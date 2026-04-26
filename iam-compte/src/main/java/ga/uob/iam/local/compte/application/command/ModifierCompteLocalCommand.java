package ga.uob.iam.local.compte.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Commande de modification manuelle d'un CompteLocal.
 *
 * Tous les champs nominaux sont optionnels (null = inchangé).
 * Le motif et l'id sont OBLIGATOIRES sans exception.
 *
 * Cette commande doit être utilisée UNIQUEMENT pour des corrections
 * administratives d'urgence justifiées et documentées.
 */
public record ModifierCompteLocalCommand(

    @NotNull(message = "L'id du CompteLocal est obligatoire")
    UUID id,

    /** Null = on ne modifie pas le nom */
    String nom,

    /** Null = on ne modifie pas le prénom */
    String prenom,

    /** Null = on ne modifie pas l'email */
    String emailInstitutionnel,

    /** Null = on ne modifie pas le type de profil */
    String typeProfilSuggere,

    /** Null = on ne modifie pas la filière */
    String filiere,

    /** Null = on ne modifie pas la composante */
    String composante,

    /**
     * Motif administratif de la modification — OBLIGATOIRE.
     * Ex : "Correction orthographique du nom suite à acte de naissance rectifié"
     */
    @NotBlank(message = "Le motif de modification est obligatoire")
    String motif

) {}
