package ga.uob.iam.local.compte.infrastructure.keycloak;

import org.keycloak.models.KeycloakSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fournisseur de KeycloakSession pour les composants Spring.
 *
 * PROBLÈME : KeycloakSession est un objet request-scoped du runtime Keycloak/Quarkus.
 * Il ne peut pas être injecté directement comme un bean Spring (scope différent).
 *
 * SOLUTION : ThreadLocal.
 *   - Keycloak appelle setSession() au début de chaque traitement (via un Provider SPI).
 *   - Les beans Spring appellent get() pour récupérer la session courante.
 *   - clear() est appelé en fin de traitement pour éviter les memory leaks.
 *
 * POUR LE CONSUMER KAFKA (contexte sans requête HTTP Keycloak) :
 *   Le KeycloakSessionFactory doit être utilisé pour créer une session ad-hoc.
 *   Voir KeycloakSessionFactory dans le module keycloak-services.
 *   Exemple dans KeycloakKafkaSessionInitializer.
 *
 * POUR LES REQUÊTES HTTP (contexte Keycloak normal) :
 *   La session est automatiquement disponible via le filtre Keycloak existant.
 */
@Component
public class KeycloakSessionProvider {

    private static final Logger log = LoggerFactory.getLogger(KeycloakSessionProvider.class);

    /** ThreadLocal — chaque thread Kafka/HTTP a sa propre session. */
    private static final ThreadLocal<KeycloakSession> SESSION_HOLDER = new ThreadLocal<>();

    /**
     * Définit la session pour le thread courant.
     * Appelé par KeycloakKafkaSessionInitializer avant chaque traitement Kafka.
     */
    public static void setSession(KeycloakSession session) {
        SESSION_HOLDER.set(session);
        log.debug("KeycloakSession définie pour le thread {}", Thread.currentThread().getName());
    }

    /**
     * Retourne la session Keycloak du thread courant.
     * Peut retourner null si appelé hors contexte Keycloak.
     */
    public KeycloakSession get() {
        KeycloakSession session = SESSION_HOLDER.get();
        if (session == null) {
            log.debug("Aucune KeycloakSession disponible pour le thread {} — " +
                      "opération Keycloak sera reportée", Thread.currentThread().getName());
        }
        return session;
    }

    /**
     * Libère la session du thread courant.
     * À appeler impérativement en fin de traitement pour éviter les memory leaks.
     */
    public static void clear() {
        SESSION_HOLDER.remove();
        log.debug("KeycloakSession libérée pour le thread {}", Thread.currentThread().getName());
    }
}
