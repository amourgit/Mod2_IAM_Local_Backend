package ga.uob.iam.local.compte.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps de requête pour la suspension ou la réactivation d'un CompteLocal.
 */
public record SuspendreCompteRequest(

    @NotBlank(message = "Le motif est obligatoire")
    @Size(min = 10, max = 500, message = "Le motif doit avoir entre 10 et 500 caractères")
    String motif

) {}
