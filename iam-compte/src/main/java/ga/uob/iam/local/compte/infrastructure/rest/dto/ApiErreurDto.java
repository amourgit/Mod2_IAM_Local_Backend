package ga.uob.iam.local.compte.infrastructure.rest.dto;

import java.time.Instant;
import java.util.List;

/**
 * Format uniforme des réponses d'erreur de l'API Compte.
 * Tous les endpoints retournent ce format en cas d'erreur.
 */
public record ApiErreurDto(

    /** Code HTTP de l'erreur. */
    int statut,

    /** Code erreur métier (ex: COMPTE_INTROUVABLE, COMPTE_DESACTIVE). */
    String code,

    /** Message lisible par un humain. */
    String message,

    /** Liste des erreurs de validation (non vide seulement pour les 400). */
    List<String> details,

    /** Horodatage de l'erreur. */
    Instant timestamp

) {
    /** Factory method pour une erreur simple sans détails. */
    public static ApiErreurDto de(int statut, String code, String message) {
        return new ApiErreurDto(statut, code, message, List.of(), Instant.now());
    }

    /** Factory method pour une erreur de validation avec détails. */
    public static ApiErreurDto deValidation(String message, List<String> details) {
        return new ApiErreurDto(400, "VALIDATION_ECHOUEE", message, details, Instant.now());
    }
}
