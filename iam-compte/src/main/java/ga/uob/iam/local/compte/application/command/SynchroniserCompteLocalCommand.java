package ga.uob.iam.local.compte.application.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Commande de synchronisation d'un CompteLocal existant.
 * Construite depuis l'event Kafka "iam.central.compte.mis_a_jour".
 */
public record SynchroniserCompteLocalCommand(

    @NotBlank String userIdNational,
    @NotBlank String nom,
    @NotBlank String prenom,
    @NotBlank @Email String emailInstitutionnel,
    @NotBlank String typeProfilSuggere,
    String filiere,
    String composante,
    @NotBlank String versionHash

) {}
