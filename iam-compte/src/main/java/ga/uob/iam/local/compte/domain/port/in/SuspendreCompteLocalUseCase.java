package ga.uob.iam.local.compte.domain.port.in;

/**
 * Port entrant : suspension temporaire d'un CompteLocal.
 *
 * Contrairement à la désactivation, la suspension est réversible.
 * Un compte suspendu ne peut plus s'authentifier mais ses données sont conservées.
 * Implémenté par : CompteLocalService.
 */
public interface SuspendreCompteLocalUseCase {

    /**
     * Suspend un compte (bloque l'authentification temporairement).
     *
     * @param userIdNational clé de liaison nationale
     * @param motif          raison administrative obligatoire
     */
    void suspendre(String userIdNational, String motif);

    /**
     * Réactive un compte suspendu.
     *
     * @param userIdNational clé de liaison nationale
     * @param motif          raison administrative obligatoire
     */
    void reactiver(String userIdNational, String motif);
}
