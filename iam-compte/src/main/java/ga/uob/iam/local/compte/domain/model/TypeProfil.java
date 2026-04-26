package ga.uob.iam.local.compte.domain.model;

/**
 * Type de profil suggéré par l'IAM Central lors du provisioning.
 * Ce type pilote la création du User (profil) dans Keycloak local.
 *
 * ETUDIANT       : inscrit en formation initiale ou continue.
 * PERSONNEL_ADM  : personnel administratif de l'UOB.
 * ENSEIGNANT     : enseignant permanent ou vacataire.
 * ENSEIGNANT_ADM : enseignant-chercheur avec rôle administratif.
 */
public enum TypeProfil {
    ETUDIANT,
    PERSONNEL_ADM,
    ENSEIGNANT,
    ENSEIGNANT_ADM
}
