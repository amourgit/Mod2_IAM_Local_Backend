package ga.uob.iam.local.compte.infrastructure.rest.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de réponse allégé pour les listes et les recherches.
 * Ne contient pas les détails de traçabilité — utiliser CompteLocalDetailDto
 * pour accéder aux modifications manuelles.
 */
public record CompteLocalSommaireDto(

    UUID    id,
    String  identifiantNational,
    String  nom,
    String  prenom,
    String  emailInstitutionnel,
    String  etablissementCode,
    String  typeProfilSuggere,
    String  statut,
    Instant creeLe

) {}
