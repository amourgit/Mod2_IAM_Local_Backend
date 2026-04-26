package ga.uob.iam.local.compte.infrastructure.rest.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de réponse complet pour un CompteLocal.
 * Exposé via l'API REST interne du module Compte.
 * Inclut toutes les informations disponibles, y compris la traçabilité.
 */
public record CompteLocalDetailDto(

    // ── Identifiants ──────────────────────────────────────────────────────
    UUID   id,
    String userIdNational,
    String identifiantNational,

    // ── Données nominales ─────────────────────────────────────────────────
    String nom,
    String prenom,
    String emailInstitutionnel,
    String etablissementCode,
    String typeProfilSuggere,

    // ── Contexte scolaire ─────────────────────────────────────────────────
    String filiere,
    String composante,

    // ── Cycle de vie ──────────────────────────────────────────────────────
    String  statut,
    Instant creeLe,
    Instant misAJourLe,
    Instant desactiveLe,

    // ── Traçabilité des modifications manuelles ────────────────────────────
    String dernierMotifModification,
    int    nombreModificationsManuelles

) {}
