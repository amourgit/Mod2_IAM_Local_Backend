package ga.uob.iam.local.keycloak.spi;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Keycloak Protocol Mapper SPI — Compte Attribute Mapper (UOB).
 *
 * Ce mapper lit les CHAMPS NATIFS du UserModel :
 *   - user.getIdCompteLocal()      → stocké colonne ID_COMPTE_LOCAL dans USER_ENTITY
 *   - user.getIdentifiantNational() → stocké colonne IDENTIFIANT_NATIONAL dans USER_ENTITY
 *
 * Et les injecte dans les tokens JWT :
 *   {
 *     "id_compte_local":      "550e8400-e29b-41d4-a716-446655440000",
 *     "identifiant_national": "ETU-2025-00412"
 *   }
 *
 * POURQUOI champs natifs et non pas getFirstAttribute("id_compte") ?
 *   Les champs natifs sont dans USER_ENTITY (accès direct, 0 jointure).
 *   getFirstAttribute() lirait USER_ATTRIBUTE (jointure lente, champ absent ici).
 *   Avec la solution native, seuls getIdCompteLocal() et getIdentifiantNational()
 *   accèdent aux bonnes colonnes.
 *
 * DÉPLOIEMENT :
 *   mvn package -pl iam-compte-keycloak-spi -DskipTests
 *   cp iam-compte-keycloak-spi/target/iam-compte-keycloak-spi.jar /opt/keycloak/providers/
 *   /opt/keycloak/bin/kc.sh build
 *   /opt/keycloak/bin/kc.sh start
 *
 * CONFIGURATION (Admin Console) :
 *   Clients → [votre client] → Client Scopes → [client]-dedicated
 *   → Mappers → Add Mapper → By Configuration
 *   → "Compte Attribute Mapper (UOB)"
 *   → Cocher "Add to access token" + "Add to ID token" → Save
 */
public class CompteAttributeProtocolMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    // =========================================================================
    // Identifiant du provider — doit être unique dans le realm Keycloak
    // =========================================================================

    public static final String PROVIDER_ID = "iam-compte-attribute-mapper";

    // =========================================================================
    // Noms des claims JWT produits par ce mapper
    // =========================================================================

    /** Claim JWT pour l'UUID du CompteLocal. */
    private static final String JWT_CLAIM_ID_COMPTE_LOCAL      = "id_compte_local";

    /** Claim JWT pour le numéro national. */
    private static final String JWT_CLAIM_IDENTIFIANT_NATIONAL = "identifiant_national";

    // =========================================================================
    // Propriétés configurables dans l'admin console
    // =========================================================================

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        // Permet à l'admin de choisir quels tokens reçoivent les claims
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(
            CONFIG_PROPERTIES,
            CompteAttributeProtocolMapper.class
        );
    }

    // =========================================================================
    // Métadonnées du provider
    // =========================================================================

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Compte Attribute Mapper (UOB)";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Injecte les champs natifs id_compte_local et identifiant_national " +
               "depuis USER_ENTITY directement dans les tokens JWT OIDC. " +
               "Nécessite que le CompteLocal soit lié au profil via le module iam-compte.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    // =========================================================================
    // Injection dans le token JWT — appelé à chaque émission de token
    // =========================================================================

    @Override
    protected void setClaim(
            IDToken              token,
            ProtocolMapperModel  mappingModel,
            UserSessionModel     userSession,
            KeycloakSession      keycloakSession,
            ClientSessionContext clientSessionCtx
    ) {
        UserModel user = userSession.getUser();

        // ── Lecture via les méthodes natives du UserModel (pas getFirstAttribute) ──
        // user.getIdCompteLocal()       lit directement la colonne ID_COMPTE_LOCAL
        // user.getIdentifiantNational() lit directement la colonne IDENTIFIANT_NATIONAL
        // Ces méthodes sont définies dans UserModel (server-spi) et
        // implémentées nativement dans UserAdapter (model/jpa).

        String idCompteLocal = user.getIdCompteLocal();
        String identifiantNational = user.getIdentifiantNational();

        // Injection dans le token — seulement si la valeur est présente
        // (un user sans CompteLocal associé ne doit pas avoir ces claims)
        if (idCompteLocal != null && !idCompteLocal.isBlank()) {
            token.getOtherClaims().put(JWT_CLAIM_ID_COMPTE_LOCAL, idCompteLocal);
        }

        if (identifiantNational != null && !identifiantNational.isBlank()) {
            token.getOtherClaims().put(JWT_CLAIM_IDENTIFIANT_NATIONAL, identifiantNational);
        }
    }
}
