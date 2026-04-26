package ga.uob.iam.local.compte.infrastructure.keycloak;

/**
 * Constantes des champs natifs Keycloak gérés par le module Compte.
 *
 * Ces noms correspondent aux constantes déclarées dans UserModel
 * (server-spi/src/main/java/org/keycloak/models/UserModel.java) :
 *
 *   String ID_COMPTE_LOCAL      = "idCompteLocal";
 *   String IDENTIFIANT_NATIONAL = "identifiantNational";
 *   String STATUT_COMPTE        = "statutCompte";
 *
 * Ils correspondent aussi aux colonnes natives dans USER_ENTITY :
 *   ID_COMPTE_LOCAL, IDENTIFIANT_NATIONAL, STATUT_COMPTE
 *
 * Et aux claims JWT injectés par CompteAttributeProtocolMapper :
 *   "id_compte_local", "identifiant_national", "statut_compte"
 *
 * IMPORTANT : les noms Java (camelCase) = noms des attributs UserModel.
 * Les noms JSON dans le JWT (snake_case) = ce que le Protocol Mapper injecte.
 */
public final class KeycloakUserAttributeConstants {

    private KeycloakUserAttributeConstants() {}

    /**
     * Nom de l'attribut UserModel pour l'UUID du CompteLocal.
     * Correspond à UserModel.ID_COMPTE_LOCAL.
     * Stocké en colonne native ID_COMPTE_LOCAL dans USER_ENTITY.
     */
    public static final String ID_COMPTE_LOCAL = "idCompteLocal";

    /**
     * Nom de l'attribut UserModel pour le numéro national.
     * Correspond à UserModel.IDENTIFIANT_NATIONAL.
     * Stocké en colonne native IDENTIFIANT_NATIONAL dans USER_ENTITY.
     */
    public static final String IDENTIFIANT_NATIONAL = "identifiantNational";

    /**
     * Nom de l'attribut UserModel pour le statut du compte.
     * Correspond à UserModel.STATUT_COMPTE.
     * Stocké en colonne native STATUT_COMPTE dans USER_ENTITY.
     */
    public static final String STATUT_COMPTE = "statutCompte";

    // =========================================================================
    // Noms des claims dans le JWT (injectés par le Protocol Mapper SPI)
    // =========================================================================

    /** Claim JWT correspondant à ID_COMPTE_LOCAL. */
    public static final String JWT_CLAIM_ID_COMPTE_LOCAL      = "id_compte_local";

    /** Claim JWT correspondant à IDENTIFIANT_NATIONAL. */
    public static final String JWT_CLAIM_IDENTIFIANT_NATIONAL = "identifiant_national";
}
