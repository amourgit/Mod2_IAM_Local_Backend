package ga.uob.iam.local.compte.infrastructure.rest;

import ga.uob.iam.local.compte.application.command.ModifierCompteLocalCommand;
import ga.uob.iam.local.compte.application.mapper.CompteLocalMapper;
import ga.uob.iam.local.compte.domain.exception.CompteLocalIntrouvableException;
import ga.uob.iam.local.compte.domain.model.CompteLocal;
import ga.uob.iam.local.compte.domain.port.in.DesactiverCompteLocalUseCase;
import ga.uob.iam.local.compte.domain.port.in.ModifierCompteLocalUseCase;
import ga.uob.iam.local.compte.domain.port.in.RechercherCompteLocalUseCase;
import ga.uob.iam.local.compte.domain.port.in.SuspendreCompteLocalUseCase;
import ga.uob.iam.local.compte.infrastructure.rest.dto.CompteLocalDetailDto;
import ga.uob.iam.local.compte.infrastructure.rest.dto.CompteLocalSommaireDto;
import ga.uob.iam.local.compte.infrastructure.rest.dto.ModifierCompteRequest;
import ga.uob.iam.local.compte.infrastructure.rest.dto.SuspendreCompteRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller REST du module Compte.
 *
 * Expose les APIs de gestion des CompteLocal.
 * Base URL : /api/v1/comptes
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  LECTURE                                                            │
 * │  GET  /api/v1/comptes/{id}                    → détail complet      │
 * │  GET  /api/v1/comptes/by-national/{idNat}     → par identifiant nat.│
 * │  GET  /api/v1/comptes/by-user/{userIdNat}     → par userIdNational  │
 * ├─────────────────────────────────────────────────────────────────────┤
 * │  MODIFICATION (CAS EXTRÊME — avec motif obligatoire)                │
 * │  PATCH /api/v1/comptes/{id}                   → modif. données      │
 * ├─────────────────────────────────────────────────────────────────────┤
 * │  CYCLE DE VIE                                                       │
 * │  POST  /api/v1/comptes/{id}/suspendre         → suspension temp.    │
 * │  POST  /api/v1/comptes/{id}/reactiver         → remise en ACTIF     │
 * │  DELETE /api/v1/comptes/{id}                  → désactivation déf.  │
 * └─────────────────────────────────────────────────────────────────────┘
 *
 * Sécurité :
 *   Ces endpoints doivent être protégés par un rôle IAM_ADMIN dans
 *   la configuration Spring Security. La modification manuelle et la
 *   désactivation nécessitent le rôle IAM_SUPER_ADMIN.
 *   (La config Spring Security est dans le module iam-security.)
 */
@RestController
@RequestMapping("/api/v1/comptes")
public class CompteLocalController {

    private static final Logger log = LoggerFactory.getLogger(CompteLocalController.class);

    private final RechercherCompteLocalUseCase rechercherUseCase;
    private final ModifierCompteLocalUseCase   modifierUseCase;
    private final SuspendreCompteLocalUseCase  suspendreUseCase;
    private final DesactiverCompteLocalUseCase desactiverUseCase;
    private final CompteLocalMapper            mapper;

    public CompteLocalController(
            RechercherCompteLocalUseCase rechercherUseCase,
            ModifierCompteLocalUseCase   modifierUseCase,
            SuspendreCompteLocalUseCase  suspendreUseCase,
            DesactiverCompteLocalUseCase desactiverUseCase,
            CompteLocalMapper            mapper
    ) {
        this.rechercherUseCase = rechercherUseCase;
        this.modifierUseCase   = modifierUseCase;
        this.suspendreUseCase  = suspendreUseCase;
        this.desactiverUseCase = desactiverUseCase;
        this.mapper            = mapper;
    }

    // =========================================================================
    // LECTURE
    // =========================================================================

    /**
     * GET /api/v1/comptes/{id}
     *
     * Retourne le détail complet d'un CompteLocal par son UUID local.
     * Inclut la traçabilité des modifications manuelles.
     *
     * @param id UUID local du CompteLocal (attribué par l'UOB à la création)
     * @return 200 CompteLocalDetailDto | 404 si introuvable
     */
    @GetMapping("/{id}")
    public ResponseEntity<CompteLocalDetailDto> getParId(@PathVariable UUID id) {
        log.debug("GET /api/v1/comptes/{}", id);

        CompteLocal compte = rechercherUseCase.parId(id)
            .orElseThrow(() -> new CompteLocalIntrouvableException(id));

        return ResponseEntity.ok(mapper.versDetailDto(compte));
    }

    /**
     * GET /api/v1/comptes/by-national/{identifiantNational}
     *
     * Retourne le CompteLocal correspondant à un numéro national.
     * C'est l'identifiant que connaissent les autres systèmes (ex: ETU-2025-00412).
     *
     * @param identifiantNational numéro national (format : PREFIX-ANNEE-SEQUENCE)
     * @return 200 CompteLocalDetailDto | 404 si introuvable | 400 si format invalide
     */
    @GetMapping("/by-national/{identifiantNational}")
    public ResponseEntity<CompteLocalDetailDto> getParIdentifiantNational(
            @PathVariable String identifiantNational
    ) {
        log.debug("GET /api/v1/comptes/by-national/{}", identifiantNational);

        CompteLocal compte = rechercherUseCase.parIdentifiantNational(identifiantNational)
            .orElseThrow(() -> new CompteLocalIntrouvableException(identifiantNational));

        return ResponseEntity.ok(mapper.versDetailDto(compte));
    }

    /**
     * GET /api/v1/comptes/by-user/{userIdNational}
     *
     * Retourne le CompteLocal correspondant à un UUID Keycloak national.
     * Utilisé par les modules internes (module User, module Service)
     * pour remonter à la vérité identitaire depuis un profil Keycloak.
     *
     * @param userIdNational UUID Keycloak du realm national
     * @return 200 CompteLocalDetailDto | 404 si introuvable
     */
    @GetMapping("/by-user/{userIdNational}")
    public ResponseEntity<CompteLocalDetailDto> getParUserIdNational(
            @PathVariable String userIdNational
    ) {
        log.debug("GET /api/v1/comptes/by-user/{}", userIdNational);

        CompteLocal compte = rechercherUseCase.parUserIdNational(userIdNational)
            .orElseThrow(() -> new CompteLocalIntrouvableException(userIdNational));

        return ResponseEntity.ok(mapper.versDetailDto(compte));
    }

    /**
     * GET /api/v1/comptes/sommaire/{id}
     *
     * Version allégée du détail — sans traçabilité de modifications.
     * Pour les consommateurs qui ont juste besoin des données nominales.
     *
     * @param id UUID local du CompteLocal
     * @return 200 CompteLocalSommaireDto | 404 si introuvable
     */
    @GetMapping("/sommaire/{id}")
    public ResponseEntity<CompteLocalSommaireDto> getSommaireParId(@PathVariable UUID id) {
        log.debug("GET /api/v1/comptes/sommaire/{}", id);

        CompteLocal compte = rechercherUseCase.parId(id)
            .orElseThrow(() -> new CompteLocalIntrouvableException(id));

        return ResponseEntity.ok(mapper.versSommaireDto(compte));
    }

    // =========================================================================
    // MODIFICATION MANUELLE (CAS EXTRÊME)
    // =========================================================================

    /**
     * PATCH /api/v1/comptes/{id}
     *
     * Modifie manuellement les données nominales d'un CompteLocal.
     *
     * ⚠️  CAS EXTRÊME UNIQUEMENT ⚠️
     * En fonctionnement normal, toutes les données arrivent via Kafka depuis l'IAM Central.
     * Cette API est réservée aux corrections d'urgence dûment justifiées.
     * Chaque appel est tracé, le compteur de modifications manuelles est incrémenté,
     * et un DomainEvent de traçabilité est émis.
     *
     * Le motif doit être entre 20 et 500 caractères — une justification sérieuse est attendue.
     *
     * @param id      UUID local du CompteLocal à modifier
     * @param request données à modifier + motif obligatoire
     * @return 200 CompteLocalDetailDto mis à jour | 404 | 422 si compte désactivé | 400 validation
     */
    @PatchMapping("/{id}")
    public ResponseEntity<CompteLocalDetailDto> modifierManuellement(
            @PathVariable UUID id,
            @Valid @RequestBody ModifierCompteRequest request
    ) {
        log.warn("PATCH /api/v1/comptes/{} — modification manuelle demandée motif='{}'",
                 id, request.motif());

        ModifierCompteLocalCommand command = new ModifierCompteLocalCommand(
            id,
            request.nom(),
            request.prenom(),
            request.emailInstitutionnel(),
            request.typeProfilSuggere(),
            request.filiere(),
            request.composante(),
            request.motif()
        );

        CompteLocal modifie = modifierUseCase.modifier(command);
        return ResponseEntity.ok(mapper.versDetailDto(modifie));
    }

    // =========================================================================
    // CYCLE DE VIE
    // =========================================================================

    /**
     * POST /api/v1/comptes/{id}/suspendre
     *
     * Suspend temporairement un CompteLocal.
     * Le profil Keycloak est désactivé (enabled=false).
     * La personne ne peut plus s'authentifier jusqu'à réactivation.
     * La suspension est RÉVERSIBLE (contrairement à la désactivation).
     *
     * @param id      UUID local du CompteLocal à suspendre
     * @param request motif de suspension obligatoire
     * @return 204 No Content | 404 | 422 si déjà suspendu ou désactivé
     */
    @PostMapping("/{id}/suspendre")
    public ResponseEntity<Void> suspendre(
            @PathVariable UUID id,
            @Valid @RequestBody SuspendreCompteRequest request
    ) {
        log.warn("POST /api/v1/comptes/{}/suspendre motif='{}'", id, request.motif());

        CompteLocal compte = rechercherUseCase.parId(id)
            .orElseThrow(() -> new CompteLocalIntrouvableException(id));

        suspendreUseCase.suspendre(compte.getUserIdNational(), request.motif());
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/comptes/{id}/reactiver
     *
     * Réactive un CompteLocal suspendu.
     * Le profil Keycloak est réactivé (enabled=true, id_compte_local réécrit).
     * La personne peut à nouveau s'authentifier.
     *
     * @param id      UUID local du CompteLocal à réactiver
     * @param request motif de réactivation obligatoire
     * @return 204 No Content | 404 | 422 si déjà actif ou désactivé
     */
    @PostMapping("/{id}/reactiver")
    public ResponseEntity<Void> reactiver(
            @PathVariable UUID id,
            @Valid @RequestBody SuspendreCompteRequest request
    ) {
        log.info("POST /api/v1/comptes/{}/reactiver motif='{}'", id, request.motif());

        CompteLocal compte = rechercherUseCase.parId(id)
            .orElseThrow(() -> new CompteLocalIntrouvableException(id));

        suspendreUseCase.reactiver(compte.getUserIdNational(), request.motif());
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/v1/comptes/{id}
     *
     * Désactive DÉFINITIVEMENT un CompteLocal.
     *
     * ⚠️  OPÉRATION IRRÉVERSIBLE LOCALEMENT ⚠️
     * Un compte désactivé ne peut pas être réactivé depuis l'UOB.
     * Seul un nouvel event de l'IAM Central pourrait le recréer.
     * Le profil Keycloak est désactivé (enabled=false, statutCompte=DESACTIVE).
     *
     * En pratique, cette action ne devrait être déclenchée que par
     * un event Kafka iam.central.compte.desactive depuis le national,
     * pas manuellement. Cette route existe pour les cas exceptionnels
     * (décès, fraude grave, exclusion définitive validée).
     *
     * @param id UUID local du CompteLocal à désactiver définitivement
     * @return 204 No Content | 404 | 422 si déjà désactivé
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactiver(@PathVariable UUID id) {
        log.warn("DELETE /api/v1/comptes/{} — désactivation définitive demandée", id);

        CompteLocal compte = rechercherUseCase.parId(id)
            .orElseThrow(() -> new CompteLocalIntrouvableException(id));

        desactiverUseCase.desactiver(compte.getUserIdNational());
        return ResponseEntity.noContent().build();
    }
}
