package ga.uob.iam.local.compte.application.service;

import ga.uob.iam.local.compte.application.command.CreerCompteLocalCommand;
import ga.uob.iam.local.compte.application.command.ModifierCompteLocalCommand;
import ga.uob.iam.local.compte.application.command.SynchroniserCompteLocalCommand;
import ga.uob.iam.local.compte.application.mapper.CompteLocalMapper;
import ga.uob.iam.local.compte.domain.exception.CompteLocalIntrouvableException;
import ga.uob.iam.local.compte.domain.model.CompteLocal;
import ga.uob.iam.local.compte.domain.model.ContexteScolaire;
import ga.uob.iam.local.compte.domain.model.IdentifiantNational;
import ga.uob.iam.local.compte.domain.model.TypeProfil;
import ga.uob.iam.local.compte.domain.port.in.CreerCompteLocalUseCase;
import ga.uob.iam.local.compte.domain.port.in.DesactiverCompteLocalUseCase;
import ga.uob.iam.local.compte.domain.port.in.ModifierCompteLocalUseCase;
import ga.uob.iam.local.compte.domain.port.in.RechercherCompteLocalUseCase;
import ga.uob.iam.local.compte.domain.port.in.SuspendreCompteLocalUseCase;
import ga.uob.iam.local.compte.domain.port.in.SynchroniserCompteLocalUseCase;
import ga.uob.iam.local.compte.domain.port.out.CompteLocalEventPublisher;
import ga.uob.iam.local.compte.domain.port.out.CompteLocalKeycloakPort;
import ga.uob.iam.local.compte.domain.port.out.CompteLocalRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;
import java.util.UUID;

/**
 * Service applicatif du module Compte.
 *
 * Implémente tous les use cases du CompteLocal :
 *   - Créer          (depuis IAM Central, idempotent)
 *   - Synchroniser   (depuis IAM Central, idempotent)
 *   - Modifier       (manuel, cas extrême, avec motif obligatoire)
 *   - Suspendre      (blocage temporaire réversible)
 *   - Réactiver      (remise en état ACTIF depuis SUSPENDU)
 *   - Désactiver     (irréversible localement)
 *   - Rechercher     (lecture pure, read-only)
 */
@Service
@Validated
@Transactional
public class CompteLocalService implements
        CreerCompteLocalUseCase,
        SynchroniserCompteLocalUseCase,
        ModifierCompteLocalUseCase,
        SuspendreCompteLocalUseCase,
        DesactiverCompteLocalUseCase,
        RechercherCompteLocalUseCase {

    private static final Logger log = LoggerFactory.getLogger(CompteLocalService.class);

    private final CompteLocalRepository     repository;
    private final CompteLocalKeycloakPort   keycloakPort;
    private final CompteLocalEventPublisher eventPublisher;
    private final CompteLocalMapper         mapper;

    public CompteLocalService(
            CompteLocalRepository     repository,
            CompteLocalKeycloakPort   keycloakPort,
            CompteLocalEventPublisher eventPublisher,
            CompteLocalMapper         mapper
    ) {
        this.repository     = repository;
        this.keycloakPort   = keycloakPort;
        this.eventPublisher = eventPublisher;
        this.mapper         = mapper;
    }

    // =========================================================================
    // USE CASE : Créer
    // =========================================================================

    @Override
    public CompteLocal creer(@Valid CreerCompteLocalCommand command) {
        MDC.put("userIdNational", command.userIdNational());
        MDC.put("identifiantNational", command.identifiantNational());
        try {
            log.info("Provisioning CompteLocal pour {}", command.identifiantNational());

            Optional<CompteLocal> existant = repository.trouverParUserIdNational(command.userIdNational());
            if (existant.isPresent()) {
                log.info("CompteLocal déjà existant — idempotence, event ignoré pour userIdNational={}",
                         command.userIdNational());
                return existant.get();
            }

            IdentifiantNational identifiantNational = mapper.extraireIdentifiantNational(command);
            TypeProfil typeProfil = mapper.extraireTypeProfil(command.typeProfilSuggere());
            ContexteScolaire contexteScolaire = mapper.extraireContexteScolaire(command.filiere(), command.composante());

            CompteLocal compte = CompteLocal.creerDepuisIamCentral(
                command.userIdNational(), identifiantNational,
                command.nom(), command.prenom(), command.emailInstitutionnel(),
                command.etablissementCode(), typeProfil, contexteScolaire, command.versionHash()
            );

            CompteLocal persiste = repository.sauvegarder(compte);
            log.info("CompteLocal créé id={}", persiste.getId());

            keycloakPort.lierCompteLocalAuProfil(
                persiste.getUserIdNational(),
                persiste.getId(),
                persiste.getIdentifiantNational().getValeur()
            );

            eventPublisher.publier(persiste.getDomainEvents());
            persiste.clearDomainEvents();
            return persiste;

        } finally {
            MDC.remove("userIdNational");
            MDC.remove("identifiantNational");
        }
    }

    // =========================================================================
    // USE CASE : Synchroniser
    // =========================================================================

    @Override
    public CompteLocal synchroniser(@Valid SynchroniserCompteLocalCommand command) {
        MDC.put("userIdNational", command.userIdNational());
        try {
            log.info("Synchronisation CompteLocal userIdNational={}", command.userIdNational());

            CompteLocal compte = repository.trouverParUserIdNational(command.userIdNational())
                .orElseThrow(() -> new CompteLocalIntrouvableException(command.userIdNational()));

            TypeProfil typeProfil = mapper.extraireTypeProfil(command.typeProfilSuggere());
            ContexteScolaire contexteScolaire = mapper.extraireContexteScolaire(command.filiere(), command.composante());

            compte.synchroniserDepuisIamCentral(
                command.nom(), command.prenom(), command.emailInstitutionnel(),
                typeProfil, contexteScolaire, command.versionHash()
            );

            CompteLocal misAJour = repository.sauvegarder(compte);
            log.info("CompteLocal synchronisé id={}", misAJour.getId());

            eventPublisher.publier(misAJour.getDomainEvents());
            misAJour.clearDomainEvents();
            return misAJour;

        } finally {
            MDC.remove("userIdNational");
        }
    }

    // =========================================================================
    // USE CASE : Modifier manuellement (cas extrême)
    // =========================================================================

    @Override
    public CompteLocal modifier(@Valid ModifierCompteLocalCommand command) {
        MDC.put("compteId", command.id().toString());
        try {
            log.warn("MODIFICATION MANUELLE CompteLocal id={} motif='{}'",
                     command.id(), command.motif());

            CompteLocal compte = repository.trouverParId(command.id())
                .orElseThrow(() -> new CompteLocalIntrouvableException(command.id()));

            TypeProfil typeProfil = command.typeProfilSuggere() != null
                ? mapper.extraireTypeProfil(command.typeProfilSuggere())
                : null;
            ContexteScolaire contexteScolaire = mapper.extraireContexteScolaire(
                command.filiere(), command.composante()
            );

            compte.modifierManuellement(
                command.nom(), command.prenom(), command.emailInstitutionnel(),
                typeProfil, contexteScolaire, command.motif()
            );

            CompteLocal modifie = repository.sauvegarder(compte);

            // Synchroniser aussi Keycloak si l'identifiant national a changé
            keycloakPort.lierCompteLocalAuProfil(
                modifie.getUserIdNational(),
                modifie.getId(),
                modifie.getIdentifiantNational().getValeur()
            );

            log.warn("Modification manuelle appliquée — id={} totalModifications={}",
                     modifie.getId(), modifie.getNombreModificationsManuelles());

            eventPublisher.publier(modifie.getDomainEvents());
            modifie.clearDomainEvents();
            return modifie;

        } finally {
            MDC.remove("compteId");
        }
    }

    // =========================================================================
    // USE CASE : Suspendre / Réactiver
    // =========================================================================

    @Override
    public void suspendre(String userIdNational, String motif) {
        MDC.put("userIdNational", userIdNational);
        try {
            log.warn("Suspension CompteLocal userIdNational={} motif='{}'", userIdNational, motif);

            CompteLocal compte = repository.trouverParUserIdNational(userIdNational)
                .orElseThrow(() -> new CompteLocalIntrouvableException(userIdNational));

            compte.suspendre(motif);
            CompteLocal suspendu = repository.sauvegarder(compte);

            // Désactiver le profil Keycloak pendant la suspension
            keycloakPort.desactiverProfil(userIdNational);

            eventPublisher.publier(suspendu.getDomainEvents());
            suspendu.clearDomainEvents();

        } finally {
            MDC.remove("userIdNational");
        }
    }

    @Override
    public void reactiver(String userIdNational, String motif) {
        MDC.put("userIdNational", userIdNational);
        try {
            log.info("Réactivation CompteLocal userIdNational={} motif='{}'", userIdNational, motif);

            CompteLocal compte = repository.trouverParUserIdNational(userIdNational)
                .orElseThrow(() -> new CompteLocalIntrouvableException(userIdNational));

            compte.reactiver(motif);
            CompteLocal reactif = repository.sauvegarder(compte);

            // Réactiver le profil Keycloak
            keycloakPort.lierCompteLocalAuProfil(
                reactif.getUserIdNational(),
                reactif.getId(),
                reactif.getIdentifiantNational().getValeur()
            );

            eventPublisher.publier(reactif.getDomainEvents());
            reactif.clearDomainEvents();

        } finally {
            MDC.remove("userIdNational");
        }
    }

    // =========================================================================
    // USE CASE : Désactiver
    // =========================================================================

    @Override
    public void desactiver(String userIdNational) {
        MDC.put("userIdNational", userIdNational);
        try {
            log.warn("Désactivation CompteLocal userIdNational={}", userIdNational);

            CompteLocal compte = repository.trouverParUserIdNational(userIdNational)
                .orElseThrow(() -> new CompteLocalIntrouvableException(userIdNational));

            compte.desactiver();
            CompteLocal desactive = repository.sauvegarder(compte);

            keycloakPort.desactiverProfil(userIdNational);

            eventPublisher.publier(desactive.getDomainEvents());
            desactive.clearDomainEvents();

        } finally {
            MDC.remove("userIdNational");
        }
    }

    // =========================================================================
    // USE CASE : Recherche (lecture pure)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Optional<CompteLocal> parId(UUID id) {
        return repository.trouverParId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompteLocal> parUserIdNational(String userIdNational) {
        return repository.trouverParUserIdNational(userIdNational);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompteLocal> parIdentifiantNational(String identifiantNational) {
        return repository.trouverParIdentifiantNational(identifiantNational);
    }
}
