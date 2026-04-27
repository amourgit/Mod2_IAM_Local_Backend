package ga.uob.iam.local.keycloak.rest.resource;

import ga.uob.iam.local.keycloak.rest.dto.CompteLocalRepresentation;
import ga.uob.iam.local.keycloak.rest.dto.ModifierCompteRepresentation;
import ga.uob.iam.local.keycloak.rest.dto.MotifRepresentation;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ressource JAX-RS — API native Keycloak pour les CompteLocal.
 *
 * URL de base : /realms/{realm}/compte-local
 *
 * ARCHITECTURE :
 *   - Authentification centralisée dans le constructeur via AdminRoot.authenticateRealmAdminRequest()
 *     (identique aux modules natifs UsersResource, RealmAdminResource, etc.)
 *   - Permissions via AdminPermissionEvaluator (manage-users / view-users natifs Keycloak)
 *   - Audit via AdminEventBuilder (persisté en base, visible dans Admin Console → Events)
 *
 * Endpoints :
 *   GET    /realms/{realm}/compte-local/{userIdNational}
 *   GET    /realms/{realm}/compte-local/by-national/{identifiantNational}
 *   PATCH  /realms/{realm}/compte-local/{userIdNational}
 *   POST   /realms/{realm}/compte-local/{userIdNational}/suspendre
 *   POST   /realms/{realm}/compte-local/{userIdNational}/reactiver
 *   DELETE /realms/{realm}/compte-local/{userIdNational}
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CompteLocalResource {

    private static final Logger log = Logger.getLogger(CompteLocalResource.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminAuth adminAuth;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    /**
     * Constructeur — authentification et initialisation centralisées.
     *
     * AdminRoot.authenticateRealmAdminRequest(session) :
     *   - Lit le token Bearer depuis les headers HTTP
     *   - Valide la signature contre le realm issu du token
     *   - Retourne un AdminAuth (realm + token + UserModel + ClientModel)
     *   - Lève NotAuthorizedException (401) si token absent ou invalide
     *
     * AdminPermissions.evaluator(session, realm, adminAuth) :
     *   - Construit un MgmtPermissions qui évalue les permissions Keycloak natives
     *   - Supporte le Fine-Grained Admin Authorization si activé
     *   - Les méthodes requireView() / requireManage() lèvent ForbiddenException (403)
     *
     * AdminEventBuilder :
     *   - Écrit les événements dans ADMIN_EVENT_ENTITY (base de données)
     *   - Visible dans Admin Console → Events → Admin Events
     *   - Queryable via l'Admin REST API
     */
    public CompteLocalResource(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();

        // Authentification centralisée — une seule fois pour toutes les méthodes
        // Lève NotAuthorizedException (401) si token absent/invalide
        this.adminAuth = AdminRoot.authenticateRealmAdminRequest(session);

        // Evaluator de permissions natif Keycloak
        // Utilise manage-users / view-users (rôles du realm-management client)
        this.auth = AdminPermissions.evaluator(session, realm, adminAuth);

        // Builder d'audit natif — persisté en base, pas juste dans les logs
        ClientConnection connection = session.getContext().getConnection();
        this.adminEvent = new AdminEventBuilder(realm, adminAuth, session, connection)
                .resource(ResourceType.USER);
    }

    // =========================================================================
    // LECTURE
    // =========================================================================

    /**
     * GET /realms/{realm}/compte-local/{userIdNational}
     *
     * Permission requise : view-users (ou manage-users) sur le realm.
     * Ces rôles sont définis dans le client "realm-management" de chaque realm.
     */
    @GET
    @Path("/{userIdNational}")
    public Response getParUserIdNational(
            @PathParam("userIdNational") String userIdNational
    ) {
        // Vérifie que l'acteur a view-users — lève ForbiddenException (403) sinon
        auth.users().requireView();

        UserModel user = session.users().getUserById(realm, userIdNational);
        if (user == null) {
            return erreur(404, "COMPTE_INTROUVABLE",
                    "Aucun profil trouvé pour userIdNational=" + userIdNational);
        }

        // Vérification fine : l'acteur peut-il voir CET utilisateur spécifique ?
        // (utile si Fine-Grained Admin Authorization est activé sur le realm)
        auth.users().requireView(user);

        adminEvent.operation(OperationType.ACTION)
                  .resourcePath(session.getContext().getUri(), userIdNational)
                  .success();

        return Response.ok(construireRepresentation(user))
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    }

    /**
     * GET /realms/{realm}/compte-local/by-national/{identifiantNational}
     *
     * Permission requise : view-users (ou manage-users) sur le realm.
     */
    @GET
    @Path("/by-national/{identifiantNational}")
    public Response getParIdentifiantNational(
            @PathParam("identifiantNational") String identifiantNational
    ) {
        auth.users().requireView();

        UserModel user = session.users()
                .searchForUserByUserAttributeStream(
                        realm,
                        UserModel.IDENTIFIANT_NATIONAL,
                        identifiantNational
                )
                .findFirst()
                .orElse(null);

        if (user == null) {
            return erreur(404, "COMPTE_INTROUVABLE",
                    "Aucun profil trouvé pour identifiantNational=" + identifiantNational);
        }

        auth.users().requireView(user);

        adminEvent.operation(OperationType.ACTION)
                  .resourcePath(session.getContext().getUri(), user.getId())
                  .success();

        return Response.ok(construireRepresentation(user))
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    }

    // =========================================================================
    // MODIFICATION MANUELLE (CAS EXTRÊME — motif obligatoire min 20 chars)
    // =========================================================================

    /**
     * PATCH /realms/{realm}/compte-local/{userIdNational}
     *
     * Permission requise : manage-users sur le realm.
     * Motif administratif obligatoire (≥ 20 caractères).
     */
    @PATCH
    @Path("/{userIdNational}")
    public Response modifierManuellement(
            @PathParam("userIdNational") String userIdNational,
            ModifierCompteRepresentation body
    ) {
        // manage-users est plus strict que view-users
        auth.users().requireManage();

        if (body == null || body.getMotif() == null || body.getMotif().trim().length() < 20) {
            return erreur(400, "MOTIF_INVALIDE",
                    "Le motif est obligatoire (minimum 20 caractères)");
        }

        UserModel user = session.users().getUserById(realm, userIdNational);
        if (user == null) {
            return erreur(404, "COMPTE_INTROUVABLE",
                    "Aucun profil trouvé pour userIdNational=" + userIdNational);
        }

        auth.users().requireManage(user);

        if (body.getNom() != null && !body.getNom().isBlank()) {
            user.setLastName(body.getNom().trim());
        }
        if (body.getPrenom() != null && !body.getPrenom().isBlank()) {
            user.setFirstName(body.getPrenom().trim());
        }
        if (body.getEmailInstitutionnel() != null && !body.getEmailInstitutionnel().isBlank()) {
            user.setEmail(body.getEmailInstitutionnel().trim());
        }

        adminEvent.operation(OperationType.UPDATE)
                  .resourcePath(session.getContext().getUri(), userIdNational)
                  .representation(body)
                  .success();

        return Response.ok(construireRepresentation(user))
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    }

    // =========================================================================
    // CYCLE DE VIE
    // =========================================================================

    /**
     * POST /realms/{realm}/compte-local/{userIdNational}/suspendre
     *
     * Permission requise : manage-users sur le realm.
     * Suspension temporaire et RÉVERSIBLE.
     */
    @POST
    @Path("/{userIdNational}/suspendre")
    public Response suspendre(
            @PathParam("userIdNational") String userIdNational,
            MotifRepresentation body
    ) {
        auth.users().requireManage();

        if (body == null || body.getMotif() == null || body.getMotif().trim().length() < 10) {
            return erreur(400, "MOTIF_INVALIDE",
                    "Le motif de suspension est obligatoire (minimum 10 caractères)");
        }

        UserModel user = session.users().getUserById(realm, userIdNational);
        if (user == null) {
            return erreur(404, "COMPTE_INTROUVABLE",
                    "Aucun profil trouvé pour userIdNational=" + userIdNational);
        }

        auth.users().requireManage(user);

        String statut = user.getStatutCompte();
        if ("SUSPENDU".equals(statut)) {
            return erreur(422, "COMPTE_DEJA_SUSPENDU", "Ce compte est déjà suspendu");
        }
        if ("DESACTIVE".equals(statut)) {
            return erreur(422, "COMPTE_DESACTIVE",
                    "Impossible de suspendre un compte désactivé définitivement");
        }

        user.setEnabled(false);
        user.setStatutCompte("SUSPENDU");

        adminEvent.operation(OperationType.ACTION)
                  .resourcePath(session.getContext().getUri(), userIdNational)
                  .representation(body)
                  .success();

        return Response.noContent().build();
    }

    /**
     * POST /realms/{realm}/compte-local/{userIdNational}/reactiver
     *
     * Permission requise : manage-users sur le realm.
     * Réactivation d'un compte suspendu.
     */
    @POST
    @Path("/{userIdNational}/reactiver")
    public Response reactiver(
            @PathParam("userIdNational") String userIdNational,
            MotifRepresentation body
    ) {
        auth.users().requireManage();

        if (body == null || body.getMotif() == null || body.getMotif().trim().length() < 10) {
            return erreur(400, "MOTIF_INVALIDE",
                    "Le motif de réactivation est obligatoire (minimum 10 caractères)");
        }

        UserModel user = session.users().getUserById(realm, userIdNational);
        if (user == null) {
            return erreur(404, "COMPTE_INTROUVABLE",
                    "Aucun profil trouvé pour userIdNational=" + userIdNational);
        }

        auth.users().requireManage(user);

        String statut = user.getStatutCompte();
        if ("ACTIF".equals(statut)) {
            return erreur(422, "COMPTE_DEJA_ACTIF", "Ce compte est déjà actif");
        }
        if ("DESACTIVE".equals(statut)) {
            return erreur(422, "COMPTE_DESACTIVE",
                    "Impossible de réactiver un compte désactivé définitivement");
        }

        user.setEnabled(true);
        user.setStatutCompte("ACTIF");

        adminEvent.operation(OperationType.ACTION)
                  .resourcePath(session.getContext().getUri(), userIdNational)
                  .representation(body)
                  .success();

        return Response.noContent().build();
    }

    /**
     * DELETE /realms/{realm}/compte-local/{userIdNational}
     *
     * Permission requise : manage-users sur le realm.
     * Désactivation DÉFINITIVE et IRRÉVERSIBLE localement.
     */
    @DELETE
    @Path("/{userIdNational}")
    public Response desactiver(
            @PathParam("userIdNational") String userIdNational
    ) {
        auth.users().requireManage();

        UserModel user = session.users().getUserById(realm, userIdNational);
        if (user == null) {
            return erreur(404, "COMPTE_INTROUVABLE",
                    "Aucun profil trouvé pour userIdNational=" + userIdNational);
        }

        auth.users().requireManage(user);

        if ("DESACTIVE".equals(user.getStatutCompte())) {
            return erreur(422, "COMPTE_DEJA_DESACTIVE", "Ce compte est déjà désactivé");
        }

        user.setEnabled(false);
        user.setStatutCompte("DESACTIVE");

        adminEvent.operation(OperationType.DELETE)
                  .resourcePath(session.getContext().getUri(), userIdNational)
                  .success();

        return Response.noContent().build();
    }

    // =========================================================================
    // Helpers — représentation et erreurs
    // =========================================================================

    private CompteLocalRepresentation construireRepresentation(UserModel user) {
        CompteLocalRepresentation rep = new CompteLocalRepresentation();
        rep.setId(user.getIdCompteLocal());
        rep.setUserIdNational(user.getId());
        rep.setIdentifiantNational(user.getIdentifiantNational());
        rep.setNom(user.getLastName());
        rep.setPrenom(user.getFirstName());
        rep.setEmailInstitutionnel(user.getEmail());
        rep.setStatut(user.getStatutCompte());
        return rep;
    }

    private Response erreur(int statut, String code, String message) {
        return Response.status(statut)
                       .type(MediaType.APPLICATION_JSON)
                       .entity(erreurBody(statut, code, message))
                       .build();
    }

    private Map<String, Object> erreurBody(int statut, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("statut",    statut);
        body.put("code",      code);
        body.put("message",   message);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
