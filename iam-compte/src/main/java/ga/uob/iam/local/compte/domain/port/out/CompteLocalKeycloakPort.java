package ga.uob.iam.local.compte.domain.port.out;

import java.util.UUID;

/**
 * Port sortant : liaison avec les profils Keycloak.
 *
 * Après création ou mise à jour d'un CompteLocal,
 * on écrit l'attribut "id_compte" sur le User Keycloak correspondant.
 * Cela permet aux tokens JWT de porter le id_compte, et à tous
 * les services locaux de remonter à la vérité identitaire.
 *
 * Implémentée par : KeycloakCompteAdapter (infrastructure).
 */
public interface CompteLocalKeycloakPort {

    /**
     * Écrit l'attribut "id_compte" et "identifiant_national" sur le User Keycloak
     * identifié par son userIdNational (qui EST le Keycloak user ID dans le realm local).
     *
     * @param userIdNational    UUID du User dans Keycloak
     * @param idCompteLocal     UUID du CompteLocal local à associer
     * @param identifiantNational numéro national (ETU-2025-00412, ...)
     */
    void lierCompteLocalAuProfil(String userIdNational, UUID idCompteLocal, String identifiantNational);

    /**
     * Désactive le User Keycloak (enabled=false) et efface l'attribut id_compte.
     *
     * @param userIdNational UUID du User dans Keycloak
     */
    void desactiverProfil(String userIdNational);
}
