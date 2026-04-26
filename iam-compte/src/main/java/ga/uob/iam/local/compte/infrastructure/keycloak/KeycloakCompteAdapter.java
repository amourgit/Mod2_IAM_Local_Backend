package ga.uob.iam.local.compte.infrastructure.keycloak;

import ga.uob.iam.local.compte.domain.port.out.CompteLocalKeycloakPort;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adaptateur Keycloak — implémente CompteLocalKeycloakPort.
 *
 * Utilise l'API INTERNE de Keycloak (KeycloakSession / UserModel) pour
 * écrire directement dans les colonnes natives USER_ENTITY :
 *   - ID_COMPTE_LOCAL      → user.setIdCompteLocal(...)
 *   - IDENTIFIANT_NATIONAL → user.setIdentifiantNational(...)
 *   - STATUT_COMPTE        → user.setStatutCompte(...)
 *
 * AVANTAGES vs REST Admin Client :
 *   ✓ Pas d'appel HTTP — même JVM, même transaction
 *   ✓ Accès direct aux colonnes natives (pas de jointure USER_ATTRIBUTE)
 *   ✓ Cohérent avec l'approche native choisie pour UserEntity
 *
 * CONTRAINTE D'INJECTION :
 *   KeycloakSession est un objet request-scoped du runtime Keycloak/Quarkus.
 *   Il doit être fourni via un KeycloakSessionProvider (voir classe ci-dessous)
 *   qui maintient la session dans un ThreadLocal alimenté par le listener Kafka
 *   ou le filtre de requête Keycloak selon le contexte d'exécution.
 *
 * GESTION DES ERREURS :
 *   Les erreurs Keycloak ne bloquent PAS la transaction Spring Boot.
 *   Le CompteLocal est déjà persisté en base. La liaison Keycloak peut être
 *   rejouée via un job de réconciliation (CompteKeycloakReconciliationJob).
 */
@Component
public class KeycloakCompteAdapter implements CompteLocalKeycloakPort {

    private static final Logger log = LoggerFactory.getLogger(KeycloakCompteAdapter.class);

    private final KeycloakSessionProvider sessionProvider;
    private final String                  realm;

    public KeycloakCompteAdapter(
            KeycloakSessionProvider sessionProvider,
            @Value("${keycloak.realm}") String realm
    ) {
        this.sessionProvider = sessionProvider;
        this.realm           = realm;
    }

    // =========================================================================
    // Liaison CompteLocal → Profil Keycloak
    // =========================================================================

    @Override
    public void lierCompteLocalAuProfil(
            String userIdNational,
            UUID   idCompteLocal,
            String identifiantNational
    ) {
        try {
            KeycloakSession session = sessionProvider.get();
            if (session == null) {
                log.warn("KeycloakSession non disponible — liaison Keycloak reportée pour userIdNational={}",
                         userIdNational);
                return;
            }

            RealmModel realmModel = session.realms().getRealm(realm);
            if (realmModel == null) {
                log.error("Realm '{}' introuvable dans Keycloak", realm);
                return;
            }

            UserModel user = session.users().getUserById(realmModel, userIdNational);
            if (user == null) {
                log.warn("Profil Keycloak introuvable pour userIdNational={} " +
                         "— id_compte_local non écrit (le profil sera lié à la prochaine synchro)",
                         userIdNational);
                return;
            }

            // Écriture dans les colonnes natives USER_ENTITY via UserModel
            // Ces méthodes sont définies dans UserModel (server-spi) et
            // implémentées nativement dans UserAdapter (model/jpa).
            user.setIdCompteLocal(idCompteLocal.toString());
            user.setIdentifiantNational(identifiantNational);
            user.setStatutCompte("ACTIF");

            log.info("Champs natifs écrits sur profil Keycloak — " +
                     "userIdNational={} id_compte_local={} identifiant_national={}",
                     userIdNational, idCompteLocal, identifiantNational);

        } catch (Exception e) {
            // Erreur non bloquante — le CompteLocal est déjà en base.
            // Un job de réconciliation doit rejouer les liaisons en échec.
            log.error("Échec écriture champs natifs Keycloak pour userIdNational={} : {}",
                      userIdNational, e.getMessage(), e);
        }
    }

    // =========================================================================
    // Désactivation du profil Keycloak
    // =========================================================================

    @Override
    public void desactiverProfil(String userIdNational) {
        try {
            KeycloakSession session = sessionProvider.get();
            if (session == null) {
                log.warn("KeycloakSession non disponible — désactivation Keycloak reportée pour userIdNational={}",
                         userIdNational);
                return;
            }

            RealmModel realmModel = session.realms().getRealm(realm);
            if (realmModel == null) {
                log.error("Realm '{}' introuvable dans Keycloak", realm);
                return;
            }

            UserModel user = session.users().getUserById(realmModel, userIdNational);
            if (user == null) {
                log.warn("Profil Keycloak introuvable pour désactivation userIdNational={}",
                         userIdNational);
                return;
            }

            // Désactivation effective du compte Keycloak
            user.setEnabled(false);

            // On conserve id_compte_local pour la traçabilité
            // mais on marque le statut comme désactivé
            user.setStatutCompte("DESACTIVE");

            log.warn("Profil Keycloak désactivé (enabled=false, statutCompte=DESACTIVE) " +
                     "pour userIdNational={}", userIdNational);

        } catch (Exception e) {
            log.error("Échec désactivation profil Keycloak pour userIdNational={} : {}",
                      userIdNational, e.getMessage(), e);
        }
    }
}
