package ga.uob.iam.local.compte.domain.port.in;

/**
 * Port entrant : désactivation définitive d'un CompteLocal.
 *
 * Implémenté par : CompteLocalService.
 * Appelé par     : IamCentralCompteKafkaConsumer sur event desactive.
 */
public interface DesactiverCompteLocalUseCase {

    /**
     * Désactive un CompteLocal identifié par son userIdNational.
     * Déclenche la désactivation de tous les profils Keycloak liés.
     *
     * @param userIdNational clé de liaison nationale
     */
    void desactiver(String userIdNational);
}
