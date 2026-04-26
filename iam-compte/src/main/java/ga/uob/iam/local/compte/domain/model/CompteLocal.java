package ga.uob.iam.local.compte.domain.model;

import ga.uob.iam.local.compte.domain.event.CompteLocalCreeEvent;
import ga.uob.iam.local.compte.domain.event.CompteLocalDesactiveEvent;
import ga.uob.iam.local.compte.domain.event.CompteLocalModifieEvent;
import ga.uob.iam.local.compte.domain.event.CompteLocalReactiveEvent;
import ga.uob.iam.local.compte.domain.event.CompteLocalSuspenduEvent;
import ga.uob.iam.local.compte.domain.event.CompteLocalSynchroniseEvent;
import ga.uob.iam.local.compte.domain.event.DomainEvent;
import ga.uob.iam.local.compte.domain.exception.CompteLocalException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * AGGREGATE ROOT du module Compte.
 *
 * Représente la vérité identitaire locale d'une personne à l'UOB.
 *
 * Règles métier fondamentales :
 *   1. userIdNational et identifiantNational sont immuables après création.
 *   2. Seul l'IAM Central peut modifier via sync Kafka (voie normale).
 *   3. La modification manuelle est possible mais tracée avec motif obligatoire.
 *   4. Un compte DESACTIVE est irréversible localement.
 *   5. Un compte SUSPENDU peut être réactivé (contrairement à DESACTIVE).
 *   6. Tous les changements émettent un DomainEvent (pattern outbox).
 */
public final class CompteLocal {

    // =========================================================================
    // Identifiants immuables
    // =========================================================================

    private final UUID id;
    private final String userIdNational;
    private final IdentifiantNational identifiantNational;

    // =========================================================================
    // Données nominales
    // =========================================================================

    private String nom;
    private String prenom;
    private String emailInstitutionnel;
    private String etablissementCode;
    private TypeProfil typeProfilSuggere;
    private ContexteScolaire contexteScolaire;

    // =========================================================================
    // Cycle de vie
    // =========================================================================

    private CompteStatus statut;
    private final Instant creeLe;
    private Instant misAJourLe;
    private Instant desactiveLe;
    private String versionHash;

    // =========================================================================
    // Traçabilité des modifications manuelles
    // =========================================================================

    /** Motif de la dernière modification manuelle (null si jamais modifié manuellement). */
    private String dernierMotifModification;

    /** Nombre total de modifications manuelles effectuées sur ce compte. */
    private int nombreModificationsManuelles;

    // =========================================================================
    // Domain Events
    // =========================================================================

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // =========================================================================
    // Constructeur privé
    // =========================================================================

    private CompteLocal(
            UUID id, String userIdNational, IdentifiantNational identifiantNational,
            String nom, String prenom, String emailInstitutionnel,
            String etablissementCode, TypeProfil typeProfilSuggere,
            ContexteScolaire contexteScolaire, CompteStatus statut,
            Instant creeLe, Instant misAJourLe, Instant desactiveLe,
            String versionHash, String dernierMotifModification,
            int nombreModificationsManuelles
    ) {
        this.id                           = Objects.requireNonNull(id, "id");
        this.userIdNational               = requireNonBlank(userIdNational, "userIdNational");
        this.identifiantNational          = Objects.requireNonNull(identifiantNational, "identifiantNational");
        this.nom                          = requireNonBlank(nom, "nom");
        this.prenom                       = requireNonBlank(prenom, "prenom");
        this.emailInstitutionnel          = requireNonBlank(emailInstitutionnel, "emailInstitutionnel");
        this.etablissementCode            = requireNonBlank(etablissementCode, "etablissementCode");
        this.typeProfilSuggere            = Objects.requireNonNull(typeProfilSuggere, "typeProfilSuggere");
        this.contexteScolaire             = contexteScolaire;
        this.statut                       = Objects.requireNonNull(statut, "statut");
        this.creeLe                       = Objects.requireNonNull(creeLe, "creeLe");
        this.misAJourLe                   = Objects.requireNonNull(misAJourLe, "misAJourLe");
        this.desactiveLe                  = desactiveLe;
        this.versionHash                  = versionHash;
        this.dernierMotifModification     = dernierMotifModification;
        this.nombreModificationsManuelles = nombreModificationsManuelles;
    }

    // =========================================================================
    // Factory : création depuis IAM Central
    // =========================================================================

    public static CompteLocal creerDepuisIamCentral(
            String userIdNational, IdentifiantNational identifiantNational,
            String nom, String prenom, String emailInstitutionnel,
            String etablissementCode, TypeProfil typeProfilSuggere,
            ContexteScolaire contexteScolaire, String versionHash
    ) {
        Instant now = Instant.now();
        CompteLocal c = new CompteLocal(
            UUID.randomUUID(), userIdNational, identifiantNational,
            nom, prenom, emailInstitutionnel, etablissementCode,
            typeProfilSuggere, contexteScolaire, CompteStatus.ACTIF,
            now, now, null, versionHash, null, 0
        );
        c.domainEvents.add(new CompteLocalCreeEvent(c.id, c.userIdNational, c.identifiantNational));
        return c;
    }

    // =========================================================================
    // Factory : reconstitution depuis la base
    // =========================================================================

    public static CompteLocal reconstituier(
            UUID id, String userIdNational, IdentifiantNational identifiantNational,
            String nom, String prenom, String emailInstitutionnel,
            String etablissementCode, TypeProfil typeProfilSuggere,
            ContexteScolaire contexteScolaire, CompteStatus statut,
            Instant creeLe, Instant misAJourLe, Instant desactiveLe,
            String versionHash, String dernierMotifModification,
            int nombreModificationsManuelles
    ) {
        return new CompteLocal(
            id, userIdNational, identifiantNational,
            nom, prenom, emailInstitutionnel, etablissementCode,
            typeProfilSuggere, contexteScolaire, statut,
            creeLe, misAJourLe, desactiveLe, versionHash,
            dernierMotifModification, nombreModificationsManuelles
        );
    }

    // =========================================================================
    // Comportements métier — synchronisation IAM Central (voie normale)
    // =========================================================================

    public void synchroniserDepuisIamCentral(
            String nom, String prenom, String emailInstitutionnel,
            TypeProfil typeProfilSuggere, ContexteScolaire contexteScolaire,
            String nouveauVersionHash
    ) {
        if (this.statut == CompteStatus.DESACTIVE) {
            throw new CompteLocalException(
                "Impossible de synchroniser un compte désactivé : " + this.identifiantNational);
        }
        if (this.versionHash != null && this.versionHash.equals(nouveauVersionHash)) {
            return; // Idempotence — event déjà traité
        }
        this.nom                 = requireNonBlank(nom, "nom");
        this.prenom              = requireNonBlank(prenom, "prenom");
        this.emailInstitutionnel = requireNonBlank(emailInstitutionnel, "emailInstitutionnel");
        this.typeProfilSuggere   = Objects.requireNonNull(typeProfilSuggere, "typeProfilSuggere");
        this.contexteScolaire    = contexteScolaire;
        this.versionHash         = nouveauVersionHash;
        this.misAJourLe          = Instant.now();
        domainEvents.add(new CompteLocalSynchroniseEvent(this.id, this.userIdNational, this.identifiantNational));
    }

    // =========================================================================
    // Comportements métier — modification manuelle (cas extrême)
    // =========================================================================

    /**
     * Modifie manuellement les données nominales d'un CompteLocal.
     *
     * CAS EXTRÊME uniquement : correction administrative d'urgence.
     * Chaque appel incrémente le compteur et trace le motif.
     *
     * @param nom                  nouveau nom (null = inchangé)
     * @param prenom               nouveau prénom (null = inchangé)
     * @param emailInstitutionnel  nouvel email (null = inchangé)
     * @param typeProfilSuggere    nouveau type profil (null = inchangé)
     * @param contexteScolaire     nouveau contexte (null = inchangé)
     * @param motif                raison administrative OBLIGATOIRE
     */
    public void modifierManuellement(
            String nom, String prenom, String emailInstitutionnel,
            TypeProfil typeProfilSuggere, ContexteScolaire contexteScolaire,
            String motif
    ) {
        if (this.statut == CompteStatus.DESACTIVE) {
            throw new CompteLocalException(
                "Impossible de modifier un compte désactivé : " + this.identifiantNational);
        }
        requireNonBlank(motif, "motif (obligatoire pour toute modification manuelle)");

        if (nom != null && !nom.isBlank())                   this.nom = nom.trim();
        if (prenom != null && !prenom.isBlank())             this.prenom = prenom.trim();
        if (emailInstitutionnel != null && !emailInstitutionnel.isBlank())
                                                             this.emailInstitutionnel = emailInstitutionnel.trim();
        if (typeProfilSuggere != null)                       this.typeProfilSuggere = typeProfilSuggere;
        if (contexteScolaire != null)                        this.contexteScolaire = contexteScolaire;

        this.dernierMotifModification     = motif.trim();
        this.nombreModificationsManuelles = this.nombreModificationsManuelles + 1;
        this.misAJourLe                   = Instant.now();

        domainEvents.add(new CompteLocalModifieEvent(
            this.id, this.userIdNational, this.identifiantNational, motif
        ));
    }

    // =========================================================================
    // Comportements métier — suspension / réactivation
    // =========================================================================

    public void suspendre(String motif) {
        if (this.statut == CompteStatus.DESACTIVE) {
            throw new CompteLocalException(
                "Impossible de suspendre un compte désactivé : " + this.identifiantNational);
        }
        if (this.statut == CompteStatus.SUSPENDU) {
            throw new CompteLocalException(
                "Ce compte est déjà suspendu : " + this.identifiantNational);
        }
        requireNonBlank(motif, "motif (obligatoire pour la suspension)");
        this.statut                   = CompteStatus.SUSPENDU;
        this.dernierMotifModification = motif.trim();
        this.misAJourLe               = Instant.now();
        domainEvents.add(new CompteLocalSuspenduEvent(
            this.id, this.userIdNational, this.identifiantNational, motif
        ));
    }

    public void reactiver(String motif) {
        if (this.statut == CompteStatus.DESACTIVE) {
            throw new CompteLocalException(
                "Impossible de réactiver un compte désactivé : " + this.identifiantNational);
        }
        if (this.statut == CompteStatus.ACTIF) {
            throw new CompteLocalException(
                "Ce compte est déjà actif : " + this.identifiantNational);
        }
        requireNonBlank(motif, "motif (obligatoire pour la réactivation)");
        this.statut                   = CompteStatus.ACTIF;
        this.dernierMotifModification = motif.trim();
        this.misAJourLe               = Instant.now();
        domainEvents.add(new CompteLocalReactiveEvent(
            this.id, this.userIdNational, this.identifiantNational, motif
        ));
    }

    // =========================================================================
    // Désactivation définitive
    // =========================================================================

    public void desactiver() {
        if (this.statut == CompteStatus.DESACTIVE) {
            throw new CompteLocalException(
                "Ce compte est déjà désactivé : " + this.identifiantNational);
        }
        this.statut      = CompteStatus.DESACTIVE;
        this.desactiveLe = Instant.now();
        this.misAJourLe  = this.desactiveLe;
        domainEvents.add(new CompteLocalDesactiveEvent(
            this.id, this.userIdNational, this.identifiantNational
        ));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    public boolean estActif()                       { return this.statut == CompteStatus.ACTIF; }
    public boolean estSuspendu()                    { return this.statut == CompteStatus.SUSPENDU; }
    public boolean aDejaTraite(String hash)         { return this.versionHash != null && this.versionHash.equals(hash); }

    public List<DomainEvent> getDomainEvents()      { return Collections.unmodifiableList(domainEvents); }
    public void clearDomainEvents()                 { domainEvents.clear(); }

    // =========================================================================
    // Accesseurs (lecture seule — pas de setters publics)
    // =========================================================================

    public UUID getId()                             { return id; }
    public String getUserIdNational()               { return userIdNational; }
    public IdentifiantNational getIdentifiantNational() { return identifiantNational; }
    public String getNom()                          { return nom; }
    public String getPrenom()                       { return prenom; }
    public String getEmailInstitutionnel()          { return emailInstitutionnel; }
    public String getEtablissementCode()            { return etablissementCode; }
    public TypeProfil getTypeProfilSuggere()        { return typeProfilSuggere; }
    public ContexteScolaire getContexteScolaire()   { return contexteScolaire; }
    public CompteStatus getStatut()                 { return statut; }
    public Instant getCreeLe()                      { return creeLe; }
    public Instant getMisAJourLe()                  { return misAJourLe; }
    public Instant getDesactiveLe()                 { return desactiveLe; }
    public String getVersionHash()                  { return versionHash; }
    public String getDernierMotifModification()     { return dernierMotifModification; }
    public int getNombreModificationsManuelles()    { return nombreModificationsManuelles; }

    private static String requireNonBlank(String v, String champ) {
        if (v == null || v.isBlank())
            throw new IllegalArgumentException("Le champ '" + champ + "' ne peut pas être vide");
        return v.trim();
    }

    @Override
    public String toString() {
        return "CompteLocal{id=" + id + ", identifiantNational=" + identifiantNational +
               ", statut=" + statut + '}';
    }
}
