package ga.uob.iam.local.compte.infrastructure.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps de requête pour la modification manuelle d'un CompteLocal.
 *
 * Tous les champs nominaux sont optionnels.
 * Seul le motif est obligatoire — sans motif, la requête est rejetée.
 *
 * RAPPEL : cette API est réservée aux cas extrêmes.
 * En fonctionnement normal, les données arrivent via Kafka depuis l'IAM Central.
 */
public record ModifierCompteRequest(

    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    String nom,

    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
    String prenom,

    @Email(message = "L'email institutionnel doit être valide")
    @Size(max = 255)
    String emailInstitutionnel,

    String typeProfilSuggere,

    @Size(max = 100)
    String filiere,

    @Size(max = 50)
    String composante,

    /**
     * Motif administratif — OBLIGATOIRE et non vide.
     * Minimum 20 caractères pour forcer une justification sérieuse.
     */
    @NotBlank(message = "Le motif de modification est obligatoire")
    @Size(min = 20, max = 500,
          message = "Le motif doit avoir entre 20 et 500 caractères")
    String motif

) {}
