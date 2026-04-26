package ga.uob.iam.local.compte.domain.model;

/**
 * Cycle de vie d'un CompteLocal.
 *
 * ACTIF      : compte créé et opérationnel, profils actifs.
 * SUSPENDU   : compte temporairement bloqué (décision administrative).
 * DESACTIVE  : compte définitivement fermé (départ, fin de scolarité, décès...).
 *
 * Seul l'IAM Central a le droit de changer ce statut via un event Kafka.
 * L'IAM Local ne peut PAS modifier le statut de sa propre initiative.
 */
public enum CompteStatus {
    ACTIF,
    SUSPENDU,
    DESACTIVE
}
