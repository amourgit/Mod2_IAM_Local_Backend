package ga.uob.iam.local.keycloak.rest.provider;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * Factory SPI : enregistre le CompteResourceProvider auprès de Keycloak.
 *
 * Keycloak découvre cette factory via le ServiceLoader (META-INF/services).
 * Elle est instanciée une seule fois au démarrage du serveur.
 *
 * Le provider qu'elle crée sera monté sous :
 *   /realms/{realm}/compte-local/...
 *
 * Ces endpoints bénéficient AUTOMATIQUEMENT de :
 *   - L'authentification Keycloak (token Bearer obligatoire)
 *   - Les checks de realm (le realm doit exister et être actif)
 *   - L'audit log natif Keycloak
 *   - Le rate limiting natif
 *   - Le CORS natif configuré dans le realm
 */
public class CompteResourceProviderFactory implements RealmResourceProviderFactory {

    /**
     * Identifiant unique du provider.
     * Définit le segment d'URL : /realms/{realm}/compte-local
     */
    public static final String PROVIDER_ID = "compte-local";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new CompteResourceProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        // Pas de configuration statique nécessaire pour ce provider.
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Pas d'initialisation post-démarrage nécessaire.
    }

    @Override
    public void close() {
        // Pas de ressources à libérer.
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
