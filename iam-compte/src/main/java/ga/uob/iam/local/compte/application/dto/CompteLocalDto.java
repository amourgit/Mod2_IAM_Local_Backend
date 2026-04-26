package ga.uob.iam.local.compte.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de lecture d'un CompteLocal.
 * Exposé via l'API REST interne du module Compte.
 * Ne contient PAS les données sensibles internes (versionHash).
 */
public record CompteLocalDto(

    UUID   id,
    String userIdNational,
    String identifiantNational,
    String nom,
    String prenom,
    String emailInstitutionnel,
    String etablissementCode,
    String typeProfilSuggere,
    String filiere,
    String composante,
    String statut,
    Instant creeLe,
    Instant misAJourLe

) {}
