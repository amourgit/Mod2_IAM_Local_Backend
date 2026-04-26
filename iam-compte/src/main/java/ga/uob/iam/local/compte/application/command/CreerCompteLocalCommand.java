package ga.uob.iam.local.compte.application.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Commande de création d'un CompteLocal.
 * Construite depuis le payload Kafka IAM Central.
 * Validée avant d'atteindre le service.
 *
 * Immuable via record Java 17.
 */
public record CreerCompteLocalCommand(

    @NotBlank(message = "userIdNational obligatoire")
    String userIdNational,

    @NotBlank(message = "identifiantNational obligatoire")
    @Pattern(
        regexp = "^[A-Z]{2,10}-\\d{4}-\\d{5}$",
        message = "Format invalide : attendu PREFIX-ANNEE-SEQUENCE (ex: ETU-2025-00412)"
    )
    String identifiantNational,

    @NotBlank(message = "nom obligatoire")
    String nom,

    @NotBlank(message = "prenom obligatoire")
    String prenom,

    @NotBlank @Email(message = "email institutionnel invalide")
    String emailInstitutionnel,

    @NotBlank(message = "etablissementCode obligatoire")
    String etablissementCode,

    @NotBlank(message = "typeProfilSuggere obligatoire")
    String typeProfilSuggere,

    /** Peut être null pour le personnel non enseignant. */
    String filiere,

    /** Peut être null pour le personnel non enseignant. */
    String composante,

    /**
     * Hash SHA-256 du payload source.
     * Garantit l'idempotence si Kafka re-livre le même message.
     */
    @NotBlank(message = "versionHash obligatoire pour l'idempotence")
    String versionHash

) {}
