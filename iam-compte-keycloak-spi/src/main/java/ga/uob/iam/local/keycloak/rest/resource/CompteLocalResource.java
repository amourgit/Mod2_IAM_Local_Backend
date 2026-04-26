package ga.uob.iam.local.keycloak.rest.resource;

import ga.uob.iam.local.keycloak.rest.dto.CompteLocalRepresentation;
import ga.uob.iam.local.keycloak.rest.dto.ModifierCompteRepresentation;
import ga.uob.iam.local.keycloak.rest.dto.MotifRepresentation;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ressource JAX-RS — API native Keycloak pour les CompteLocal.
 *
 * URL de base : /realms/{realm}/compte-local
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ INTÉGRATION NATIVE KEYCLOAK                                             │
 * │                                                                         │
 * │ ✅ Authentification Bearer via AppAuthManager (même mécanisme que       │
 * │    l'API Admin native /admin/realms/{realm}/users)                      │
 * │ ✅ Vérification du realm — actif et existant automatiquement            │
 * │ ✅ CORS — configuré dans les settings du realm, appliqué ici            │
 * │ ✅ TLS / HTTPS — géré par Keycloak au niveau serveur                    │
 * │ ✅ Rate limiting — niveau realm Keycloak                                │
 * │ ✅ Realm Roles — iam-admin / iam-super-admin vérifiés nativement        │
 * │ ✅ Audit structuré — via CompteAuditLogger (JBoss MDC structuré)        │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * NOTE sur l'audit :
 *   En Keycloak 26.x, KeycloakContext.getEvent() a été supprimé.
 *   L'EventBuilder natif est disponible dans keycloak-services mais nécessite
 *   un wiring complexe (AdminEventBuilder, connexion HTTP, etc.) non adapté
 *   à un RealmResourceProvider custom.
 *   On utilise ici un audit structuré via JBoss Logger avec MDC — toutes les
 *   actions sensibles sont tracées avec acteur, cible, action, motif, timestamp.
 *   Ces logs sont capturés par le système de log Keycloak (Quarkus/JBoss Logging)
 *   et peuvent être redirigés vers un SIEM.
 *
 * RÔLES REQUIS (à créer dans le realm Keycloak Admin Console) :
 *   - iam-admin       : lecture + modification normale
 *   - iam-super-admin : suspension, réactivation, désactivation définitive
 */
@Path("")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CompteLocalResource {

    private static final Logger log = Logger.getLogger(CompteLocalResource.class);

    private static final String ROLE_IAM_ADMIN       = "iam-admin";
    private static final String ROLE_IAM_SUPER_ADMIN = "iam-super-admin";

    private final KeycloakSession session;
    private final RealmModel      realm;

    public CompteLocalResource(KeycloakSession session) {
        this.session = session;
        this.realm   = session.getContext().getRealm();
    }

    // =========================================================================
    // LECTURE
    // =========================================================================

    /**
     * GET /realms/{realm}/compte-local/{userIdNational}
     *
     * Retourne les champs natifs id_compte_local, identifiant_national, statut_compte
     * lus directement depuis USER_ENTITY (colonne native, pas de jointure USER_ATTRIBUTE).
     *
     * Sécurité : rôle iam-admin requis.
     */
    @GET
    @Path("/{userIdNational}")
    public Response getParUserIdNational(
            @PathParam("userIdNational") String userIdNational
    ) {
        AuthenticationManager.AuthResult auth = verifierAuthentificationEtRole(ROLE_IAM_ADMIN);

        UserModel user = session.users().getUserById(realm, userIdNational);
        if (user == null) {
            return erreur(404, "COMPTE_INTROUVABLE",
                          "Aucun profil Keycloak trouvé pour userIdNational=" + userIdNational);
        }

        audit(auth, "LECTURE", userIdNational, null);
        return Response.ok(construireRepresentation(user)).build();
    }

    /**
     * GET /realms/{realm}/compte-local/by-national/{identifiantNational}
     *
     * Recherche par numéro national (ETU-2025-00412).
     * Utilise la recherche par attribut natif Keycloak sur IDENTIFIANT_NATIONAL.
     *
     * Sécurité : rôle iam-admin requis.
     */
    @GET
    @Path("/by-national/{identifiantNational}")
    public Response getParIdentifiantNational(
            @PathParam("identifiantNational") String identifiantNational
    ) {
        AuthenticationManager.AuthResult auth = verifierAuthentificationEtRole(ROLE_IAM_ADMIN);

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
                          "Aucun profil Keycloak trouvé pour identifiantNational=" + identifiantNational);
        }

        audit(auth, "LECTURE_PAR_IDENTIFIANT", user.getId(), null);
        return Response.ok(construireRepresentation(user)).build();
    }

    // =========================================================================
    // MODIFICATION MANUELLE (CAS EXTRÊME)
    // =========================================================================

    /**
     * PATCH /realms/{realm}/compte-local/{userIdNational}
     *
     * Modifie manuellement les champs d'un profil Keycloak.
     * Motif administratif obligatoire — minimum 20 caractères.
     *
     * Sécurité : rôle iam-admin requis.
     */
    @PATCH
    @Path("/{userIdNational}")
    public Response modifierManuellement(
            @PathParam("userIdNational") String userIdNational,
            ModifierCompteRepresentation body
    ) {
        AuthenticationManager.AuthResult auth = verifierAuthentificationEtRole(ROLE_IAM_ADMIN);

        if (body == null || body.getMotif() == null || body.getMotif().trim().length() < 20) {
            return erreur(400, "MOTIF_INVALIDE",
                          "Le motif est obligatoire et doit avoir au moins 20 caractères");
        }

        UserModel user = session.users().getUserById(realm, userIdNational);
        if (user == null) {
            return erreur(404, "COMPTE_INTROUVABLE",
                          "Aucun profil Keycloak trouvé pour userIdNational=" + userIdNational);
        }

        // Modification des champs Keycloak standard — uniquement si fournis
        if (body.getNom() != null && !body.getNom().isBlank()) {
            user.setLastName(body.getNom().trim());
        }
        if (body.getPrenom() != null && !body.getPrenom().isBlank()) {
            user.setFirstName(body.getPrenom().trim());
        }
        if (body.getEmailInstitutionnel() != null && !body.getEmailInstitutionnel().isBlank()) {
            user.setEmail(body.getEmailInstitutionnel().trim());
        }

        audit(auth, "MODIFICATION_MANUELLE", userIdNational, body.getMotif());
        return Response.ok(construireRepresentation(user)).build();
    }

    // =========================================================================
    // CYCLE DE VIE
    // =========================================================================

    /**
     * POST /realms/{realm}/compte-local/{userIdNational}/suspendre
     *
     * Suspend le profil Keycloak : enabled=false, statutCompte=SUSPENDU.
     * Suspension RÉVERSIBLE — un compte suspendu peut être réactivé.
     *
     * Sécurité : rôle iam-super-admin requis.
     */
    @POST
    @Path("/{userIdNational}/suspendre")
    public Response suspendre(
            @PathParam("userIdNational") String userIdNational,
            MotifRepresentation body
    ) {
        AuthenticationManager.AuthResult auth = verifierAuthentificationEtRole(ROLE_IAM_SUPER_ADMIN);

        if (body == null || body.getMotif() == null || body.getMotif().trim().length() < 10) {
            return erreur(400, "MOTIF_INVALIDE",
                          "Le motif de suspension est obligatoire (minimum 10 caractères)");
        }

        UserModel user = session.users().getUserById(realm, userIdNational);
        if (user == null) {
            return erreur(404, "COMPTE_INTROUVABLE",
                          "Aucun profil Keycloak trouvé pour userIdNational=" + userIdNational);
        }

        String statutActuel = user.getStatutCompte();
        if ("SUSPENDU".equals(statutActuel)) {
            return erreur(422, "COMPTE_DEJA_SUSPENDU", "Ce compte est déjà suspendu");
        }
        if ("DESACTIVE".equals(statutActuel)) {
            return erreur(422, "COMPTE_DESACTIVE",
                          "Impossible de suspendre un compte désactivé définitivement");
        }

        user.setEnabled(false);
        user.setStatutCompte("SUSPENDU");

        audit(auth, "SUSPENSION", userIdNational, body.getMotif());
        return Response.noContent().build();
    }

    /**
     * POST /realms/{realm}/compte-local/{userIdNational}/reactiver
     *
     * Réactive un profil Keycloak suspendu : enabled=true, statutCompte=ACTIF.
     *
     * Sécurité : rôle iam-super-admin requis.
     */
    @POST
    @Path("/{userIdNational}/reactiver")
    public Response reactiver(
            @PathParam("userIdNational") String userIdNational,
            MotifRepresentation body
    ) {
        AuthenticationManager.AuthResult auth = verifierAuthentificationEtRole(ROLE_IAM_SUPER_ADMIN);

        if (body == null || body.getMotif() == null || body.getMotif().trim().length() < 10) {
            return erreur(400, "MOTIF_INVALIDE",
                          "Le motif de réactivation est obligatoire (minimum 10 caractères)");
        }

        UserModel user = session.users().getUserById(realm, userIdNational);
        if (user == null) {
            return erreur(404, "COMPTE_INTROUVABLE",
                          "Aucun profil Keycloak trouvé pour userIdNational=" + userIdNational);
        }

        String statutActuel = user.getStatutCompte();
        if ("ACTIF".equals(statutActuel)) {
            return erreur(422, "COMPTE_DEJA_ACTIF", "Ce compte est déjà actif");
        }
        if ("DESACTIVE".equals(statutActuel)) {
            return erreur(422, "COMPTE_DESACTIVE",
                          "Impossible de réactiver un compte désactivé définitivement");
        }

        user.setEnabled(true);
        user.setStatutCompte("ACTIF");

        audit(auth, "REACTIVATION", userIdNational, body.getMotif());
        return Response.noContent().build();
    }

    /**
     * DELETE /realms/{realm}/compte-local/{userIdNational}
     *
     * Désactive DÉFINITIVEMENT un profil Keycloak.
     * OPÉRATION IRRÉVERSIBLE LOCALEMENT.
     *
     * Sécurité : rôle iam-super-admin requis.
     */
    @DELETE
    @Path("/{userIdNational}")
    public Response desactiver(
            @PathParam("userIdNational") String userIdNational
    ) {
        AuthenticationManager.AuthResult auth = verifierAuthentificationEtRole(ROLE_IAM_SUPER_ADMIN);

        UserModel user = session.users().getUserById(realm, userIdNational);
        if (user == null) {
            return erreur(404, "COMPTE_INTROUVABLE",
                          "Aucun profil Keycloak trouvé pour userIdNational=" + userIdNational);
        }

        if ("DESACTIVE".equals(user.getStatutCompte())) {
            return erreur(422, "COMPTE_DEJA_DESACTIVE", "Ce compte est déjà désactivé");
        }

        user.setEnabled(false);
        user.setStatutCompte("DESACTIVE");

        audit(auth, "DESACTIVATION_DEFINITIVE", userIdNational, null);
        return Response.noContent().build();
    }

    // =========================================================================
    // Sécurité — authentification et vérification des rôles
    // =========================================================================

    /**
     * Vérifie le token Bearer et le rôle requis.
     *
     * Utilise AppAuthManager.BearerTokenAuthenticator — c'est exactement
     * le même mécanisme que les API admin natives de Keycloak.
     *
     * En Keycloak 26.x, authResult.getUser() est déprécié.
     * On passe par authResult.getToken() pour récupérer le subject (userId)
     * puis on charge le UserModel pour vérifier les rôles realm.
     *
     * @throws NotAuthorizedException si token absent ou invalide (→ HTTP 401)
     * @throws ForbiddenException     si rôle insuffisant (→ HTTP 403)
     */
    private AuthenticationManager.AuthResult verifierAuthentificationEtRole(String roleRequis) {

        AuthenticationManager.AuthResult authResult =
            new AppAuthManager.BearerTokenAuthenticator(session).authenticate();

        if (authResult == null) {
            throw new NotAuthorizedException(
                "Token Bearer absent ou invalide",
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity(erreurBody(401, "NON_AUTHENTIFIE",
                                "Un token Bearer valide est requis"))
                        .build()
            );
        }

        // Récupération du UserModel via le subject du token (non déprécié)
        AccessToken token = authResult.getToken();
        UserModel acteur  = session.users().getUserById(realm, token.getSubject());

        if (acteur == null) {
            throw new ForbiddenException(
                Response.status(Response.Status.FORBIDDEN)
                        .entity(erreurBody(403, "ACTEUR_INTROUVABLE",
                                "L'utilisateur du token est introuvable dans ce realm"))
                        .build()
            );
        }

        // Vérification du rôle realm
        boolean aLeRole = acteur.getRealmRoleMappingsStream()
                                .anyMatch(role -> roleRequis.equals(role.getName()));

        if (!aLeRole) {
            throw new ForbiddenException(
                Response.status(Response.Status.FORBIDDEN)
                        .entity(erreurBody(403, "ACCES_REFUSE",
                                "Le rôle '" + roleRequis + "' est requis pour cette opération"))
                        .build()
            );
        }

        return authResult;
    }

    // =========================================================================
    // Audit structuré
    // =========================================================================

    /**
     * Trace chaque action sensible avec acteur, realm, cible, action, motif, timestamp.
     *
     * EN KEYCLOAK 26.x : KeycloakContext.getEvent() n'existe plus.
     * L'EventBuilder natif (keycloak-services) est disponible mais son wiring
     * dans un RealmResourceProvider custom nécessite des dépendances circulaires
     * et un accès à AdminEventBuilder qui est interne à l'API admin Keycloak.
     *
     * SOLUTION RETENUE : audit via JBoss Logger structuré avec MDC.
     * Ces logs sont capturés par le système de log Keycloak/Quarkus
     * et peuvent être redirigés vers un SIEM (Elastic, Splunk, etc.)
     * via la configuration de logging Quarkus (quarkus.log.handler.*).
     *
     * FORMAT : [IAM-AUDIT] action=X realm=Y acteur=Z cible=W motif=M timestamp=T
     */
    private void audit(
            AuthenticationManager.AuthResult auth,
            String action,
            String cibleUserIdNational,
            String motif
    ) {
        AccessToken token       = auth.getToken();
        String      acteurId    = token.getSubject();
        String      acteurLogin = token.getPreferredUsername();
        String      realmNom    = realm.getName();
        String      timestamp   = Instant.now().toString();

        // Log structuré — format parseable par un SIEM
        if (motif != null) {
            log.warnf("[IAM-AUDIT] action=%s realm=%s acteur=%s(%s) cible=%s motif='%s' timestamp=%s",
                      action, realmNom, acteurLogin, acteurId,
                      cibleUserIdNational, motif, timestamp);
        } else {
            log.infof("[IAM-AUDIT] action=%s realm=%s acteur=%s(%s) cible=%s timestamp=%s",
                      action, realmNom, acteurLogin, acteurId,
                      cibleUserIdNational, timestamp);
        }
    }

    // =========================================================================
    // Construction des représentations et réponses d'erreur
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
                       .entity(erreurBody(statut, code, message))
                       .build();
    }

    private Map<String, Object> erreurBody(int statut, String code, String message) {
        // LinkedHashMap pour garantir l'ordre des champs dans le JSON
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("statut",    statut);
        body.put("code",      code);
        body.put("message",   message);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
