package ga.uob.iam.local.keycloak.rest.provider;

import ga.uob.iam.local.keycloak.rest.resource.CompteLocalResource;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

/**
 * Provider JAX-RS : lie la session Keycloak à la ressource REST.
 *
 * Keycloak appelle getResource() à chaque requête entrante.
 * La session est fraîche et request-scoped — pas de problème de concurrence.
 */
public class CompteResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public CompteResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Retourne la ressource JAX-RS racine.
     * Toutes les sous-ressources sont déclarées dans CompteLocalResource.
     */
    @Override
    public Object getResource() {
        return new CompteLocalResource(session);
    }

    @Override
    public void close() {
        // La session Keycloak est gérée par le runtime — pas à fermer ici.
    }
}
