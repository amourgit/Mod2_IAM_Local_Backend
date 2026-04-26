package ga.uob.iam.local.compte.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité JPA pour la persistance du CompteLocal.
 * Version complète incluant les champs de traçabilité des modifications manuelles.
 */
@Entity
@Table(
    name = "compte_local",
    indexes = {
        @Index(name = "idx_compte_user_id_national",     columnList = "user_id_national",     unique = true),
        @Index(name = "idx_compte_identifiant_national", columnList = "identifiant_national",  unique = true),
        @Index(name = "idx_compte_statut",               columnList = "statut")
    }
)
public class CompteLocalJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id_national", nullable = false, unique = true, length = 36)
    private String userIdNational;

    @Column(name = "identifiant_national", nullable = false, unique = true, length = 30)
    private String identifiantNational;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @Column(name = "email_institutionnel", nullable = false, length = 255)
    private String emailInstitutionnel;

    @Column(name = "etablissement_code", nullable = false, length = 20)
    private String etablissementCode;

    @Column(name = "type_profil_suggere", nullable = false, length = 30)
    private String typeProfilSuggere;

    @Embedded
    private ContexteScolaireEmbeddable contexteScolaire;

    @Column(name = "statut", nullable = false, length = 20)
    private String statut;

    @CreationTimestamp
    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe;

    @UpdateTimestamp
    @Column(name = "mis_a_jour_le", nullable = false)
    private Instant misAJourLe;

    @Column(name = "desactive_le")
    private Instant desactiveLe;

    @Column(name = "version_hash", length = 64)
    private String versionHash;

    /** Motif de la dernière modification manuelle. */
    @Column(name = "dernier_motif_modification", length = 500)
    private String dernierMotifModification;

    /** Compteur de modifications manuelles (0 = jamais touché manuellement). */
    @Column(name = "nombre_modifications_manuelles", nullable = false)
    private int nombreModificationsManuelles = 0;

    @Version
    @Column(name = "version")
    private Long version;

    // =========================================================================
    // Constructeur no-arg JPA
    // =========================================================================

    public CompteLocalJpaEntity() {}

    // =========================================================================
    // Getters / Setters — tous explicites
    // =========================================================================

    public UUID getId()                                      { return id; }
    public void setId(UUID id)                               { this.id = id; }

    public String getUserIdNational()                        { return userIdNational; }
    public void setUserIdNational(String v)                  { this.userIdNational = v; }

    public String getIdentifiantNational()                   { return identifiantNational; }
    public void setIdentifiantNational(String v)             { this.identifiantNational = v; }

    public String getNom()                                   { return nom; }
    public void setNom(String v)                             { this.nom = v; }

    public String getPrenom()                                { return prenom; }
    public void setPrenom(String v)                          { this.prenom = v; }

    public String getEmailInstitutionnel()                   { return emailInstitutionnel; }
    public void setEmailInstitutionnel(String v)             { this.emailInstitutionnel = v; }

    public String getEtablissementCode()                     { return etablissementCode; }
    public void setEtablissementCode(String v)               { this.etablissementCode = v; }

    public String getTypeProfilSuggere()                     { return typeProfilSuggere; }
    public void setTypeProfilSuggere(String v)               { this.typeProfilSuggere = v; }

    public ContexteScolaireEmbeddable getContexteScolaire()  { return contexteScolaire; }
    public void setContexteScolaire(ContexteScolaireEmbeddable v) { this.contexteScolaire = v; }

    public String getStatut()                                { return statut; }
    public void setStatut(String v)                          { this.statut = v; }

    public Instant getCreeLe()                               { return creeLe; }
    public void setCreeLe(Instant v)                         { this.creeLe = v; }

    public Instant getMisAJourLe()                           { return misAJourLe; }
    public void setMisAJourLe(Instant v)                     { this.misAJourLe = v; }

    public Instant getDesactiveLe()                          { return desactiveLe; }
    public void setDesactiveLe(Instant v)                    { this.desactiveLe = v; }

    public String getVersionHash()                           { return versionHash; }
    public void setVersionHash(String v)                     { this.versionHash = v; }

    public String getDernierMotifModification()              { return dernierMotifModification; }
    public void setDernierMotifModification(String v)        { this.dernierMotifModification = v; }

    public int getNombreModificationsManuelles()             { return nombreModificationsManuelles; }
    public void setNombreModificationsManuelles(int v)       { this.nombreModificationsManuelles = v; }

    public Long getVersion()                                 { return version; }
    public void setVersion(Long v)                           { this.version = v; }
}
