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
 * ARCHITECTURE RealmResourceProvider (sub-resource locator pattern) :
 *
 *   1. Keycloak reçoit /realms/{realm}/compte-local/{suite...}
 *   2. Il identifie "compte-local" → CompteResourceProviderFactory
 *   3. Il appelle CompteResourceProvider.getResource() → retourne cette classe
 *   4. JAX-RS traite "/{suite...}" avec les @Path des MÉTHODES de cette classe
 *
 * IMPORTANT — PAS de @Path sur la classe :
 *   Un @Path("") ou @Path("/") au niveau classe transforme cette ressource
 *   en ressource racine JAX-RS qui capture TOUTES les URLs de Quarkus
 *   (/docs/, /favicon.ico, /q/dev/, etc.).
 *   Dans le pattern sub-resource locator, seuls les @Path des méthodes comptent.
 *   L'annotation de classe doit être ABSENTE.
 *
 * Endpoints opérationnels :
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

    private static final String ROLE_IAM_ADMIN       = "iam-admin";
    private static final String ROLE_IAM_SUPER_ADMIN = "iam-super-admin";

    private final KeycloakSession session;

    public CompteLocalResource(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Récupère le realm lazy depuis le contexte de session.
     * Retourne null si absent — chaque méthode gère ce cas proprement.
     */
    private RealmModel getRealm() {
        return session.getContext().getRealm();
    }

    /**
     * Vérifie la présence du realm. Retourne une Response d'erreur si absent,
     * null si tout est bon.
     *
     * On ne throw jamais ici — une exception non enveloppée dans une Response
     * complète (avec MediaType) casse DefaultSecurityHeadersProvider.
     */
    private Response verifierRealm(RealmModel realm) {
        if (realm == null) {
            log.warnf("getRealm() null — URL mal formée ou contexte Keycloak non initialisé");
            return Response.status(Response.Status.BAD_REQUEST)
                           .type(MediaType.APPLICATION_JSON)
                           .entity(erreurBody(400, "REALM_MANQUANT",
                                   "URL invalide. Format attendu : " +
                                   "/realms/{realm-name}/compte-local/{userIdNational}"))
                           .build();
        }
        return null;
    }

    // =========================================================================
    // LECTURE
    // =========================================================================

    /**
     * GET /realms/{realm}/compte-local/{userIdNational}
     *
     * Retourne les champs natifs du profil Keycloak (id_compte_local,
     * identifiant_national, statut_compte) lus directement depuis USER_ENTITY.
     *
     * Sécurité : rôle iam-admin requis.
     */
    @GET
    @Path("/{userIdNational}")
    public Response getParUserIdNational(
            @PathParam("userIdNational") String userIdNational
    ) {
        RealmModel realm = getRealm();
        Response realmCheck = verifierRealm(realm);
        if (realmCheck != null) return realmCheck;

        AuthenticationManager.AuthResult auth =
            verifierAuthentificationEtRole(realm, ROLE_IAM_ADMIN);

        UserModel user = session.users().getUserById(realm, userIdNational);
        if (user == null) {
            return erreur(404, "COMPTE_INTROUVABLE",
                          "Aucun profil trouvé pour userIdNational=" + userIdNational);
        }

        audit(auth, realm, "LECTURE", userIdNational, null);
        return Response.ok(construireRepresentation(user))
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    }

    /**
     * GET /realms/{realm}/compte-local/by-national/{identifiantNational}
     *
     * Recherche par numéro national (ETU-2025-00412).
     * Utilise la colonne native IDENTIFIANT_NATIONAL (index, pas de jointure).
     *
     * Sécurité : rôle iam-admin requis.
     */
    @GET
    @Path("/by-national/{identifiantNational}")
    public Response getParIdentifiantNational(
            @PathParam("identifiantNational") String identifiantNational
    ) {
        RealmModel realm = getRealm();
        Response realmCheck = verifierRealm(realm);
        if (realmCheck != null) return realmCheck;

        AuthenticationManager.AuthResult auth =
            verifierAuthentificationEtRole(realm, ROLE_IAM_ADMIN);

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

        audit(auth, realm, "LECTURE_PAR_IDENTIFIANT", user.getId(), null);
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
     * Modification administrative d'urgence des données nominales.
     * En fonctionnement normal, tout passe par Kafka depuis l'IAM Central.
     *
     * Sécurité : rôle iam-admin requis + motif ≥ 20 caractères.
     */
    @PATCH
    @Path("/{userIdNational}")
    public Response modifierManuellement(
            @PathParam("userIdNational") String userIdNational,
            ModifierCompteRepresentation body
    ) {
        RealmModel realm = getRealm();
        Response realmCheck = verifierRealm(realm);
        if (realmCheck != null) return realmCheck;

        AuthenticationManager.AuthResult auth =
            verifierAuthentificationEtRole(realm, ROLE_IAM_ADMIN);

        if (body == null || body.getMotif() == null || body.getMotif().trim().length() < 20) {
            return erreur(400, "MOTIF_INVALIDE",
                          "Le motif est obligatoire (minimum 20 caractères)");
        }

        UserModel user = session.users().getUserById(realm, userIdNational);
        if (user == null) {
            return erreur(404, "COMPTE_INTROUVABLE",
                          "Aucun profil trouvé pour userIdNational=" + userIdNational);
        }

        if (body.getNom() != null && !body.getNom().isBlank()) {
            user.setLastName(body.getNom().trim());
        }
        if (body.getPrenom() != null && !body.getPrenom().isBlank()) {
            user.setFirstName(body.getPrenom().trim());
        }
        if (body.getEmailInstitutionnel() != null && !body.getEmailInstitutionnel().isBlank()) {
            user.setEmail(body.getEmailInstitutionnel().trim());
        }

        audit(auth, realm, "MODIFICATION_MANUELLE", userIdNational, body.getMotif());
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
     * Suspension temporaire et RÉVERSIBLE.
     * enabled=false + statutCompte=SUSPENDU dans USER_ENTITY.
     *
     * Sécurité : rôle iam-super-admin requis.
     */
    @POST
    @Path("/{userIdNational}/suspendre")
    public Response suspendre(
            @PathParam("userIdNational") String userIdNational,
            MotifRepresentation body
    ) {
        RealmModel realm = getRealm();
        Response realmCheck = verifierRealm(realm);
        if (realmCheck != null) return realmCheck;

        AuthenticationManager.AuthResult auth =
            verifierAuthentificationEtRole(realm, ROLE_IAM_SUPER_ADMIN);

        if (body == null || body.getMotif() == null || body.getMotif().trim().length() < 10) {
            return erreur(400, "MOTIF_INVALIDE",
                          "Le motif de suspension est obligatoire (minimum 10 caractères)");
        }

        UserModel user = session.users().getUserById(realm, userIdNational);
        if (user == null) {
            return erreur(404, "COMPTE_INTROUVABLE",
                          "Aucun profil trouvé pour userIdNational=" + userIdNational);
        }

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

        audit(auth, realm, "SUSPENSION", userIdNational, body.getMotif());
        return Response.noContent().build();
    }

    /**
     * POST /realms/{realm}/compte-local/{userIdNational}/reactiver
     *
     * Réactivation d'un compte suspendu.
     * enabled=true + statutCompte=ACTIF dans USER_ENTITY.
     *
     * Sécurité : rôle iam-super-admin requis.
     */
    @POST
    @Path("/{userIdNational}/reactiver")
    public Response reactiver(
            @PathParam("userIdNational") String userIdNational,
            MotifRepresentation body
    ) {
        RealmModel realm = getRealm();
        Response realmCheck = verifierRealm(realm);
        if (realmCheck != null) return realmCheck;

        AuthenticationManager.AuthResult auth =
            verifierAuthentificationEtRole(realm, ROLE_IAM_SUPER_ADMIN);

        if (body == null || body.getMotif() == null || body.getMotif().trim().length() < 10) {
            return erreur(400, "MOTIF_INVALIDE",
                          "Le motif de réactivation est obligatoire (minimum 10 caractères)");
        }

        UserModel user = session.users().getUserById(realm, userIdNational);
        if (user == null) {
            return erreur(404, "COMPTE_INTROUVABLE",
                          "Aucun profil trouvé pour userIdNational=" + userIdNational);
        }

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

        audit(auth, realm, "REACTIVATION", userIdNational, body.getMotif());
        return Response.noContent().build();
    }

    /**
     * DELETE /realms/{realm}/compte-local/{userIdNational}
     *
     * Désactivation DÉFINITIVE et IRRÉVERSIBLE localement.
     * enabled=false + statutCompte=DESACTIVE dans USER_ENTITY.
     *
     * Sécurité : rôle iam-super-admin requis.
     */
    @DELETE
    @Path("/{userIdNational}")
    public Response desactiver(
            @PathParam("userIdNational") String userIdNational
    ) {
        RealmModel realm = getRealm();
        Response realmCheck = verifierRealm(realm);
        if (realmCheck != null) return realmCheck;

        AuthenticationManager.AuthResult auth =
            verifierAuthentificationEtRole(realm, ROLE_IAM_SUPER_ADMIN);

        UserModel user = session.users().getUserById(realm, userIdNational);
        if (user == null) {
            return erreur(404, "COMPTE_INTROUVABLE",
                          "Aucun profil trouvé pour userIdNational=" + userIdNational);
        }

        if ("DESACTIVE".equals(user.getStatutCompte())) {
            return erreur(422, "COMPTE_DEJA_DESACTIVE", "Ce compte est déjà désactivé");
        }

        user.setEnabled(false);
        user.setStatutCompte("DESACTIVE");

        audit(auth, realm, "DESACTIVATION_DEFINITIVE", userIdNational, null);
        return Response.noContent().build();
    }

    // =========================================================================
    // Sécurité — authentification + vérification du rôle realm
    // =========================================================================

    /**
     * Pose le realm dans le contexte de session, authentifie le token Bearer,
     * puis vérifie que l'acteur possède le rôle requis dans ce realm.
     *
     * session.getContext().setRealm(realm) est obligatoire avant authenticate() :
     * BearerTokenAuthenticator en a besoin pour construire l'issuer attendu
     * et valider la signature du token.
     *
     * @throws NotAuthorizedException (→ 401) si token absent ou invalide
     * @throws ForbiddenException     (→ 403) si rôle insuffisant
     */
    private AuthenticationManager.AuthResult verifierAuthentificationEtRole(
            RealmModel realm,
            String roleRequis
    ) {
        session.getContext().setRealm(realm);

        AuthenticationManager.AuthResult authResult =
            new AppAuthManager.BearerTokenAuthenticator(session).authenticate();

        if (authResult == null) {
            throw new NotAuthorizedException(
                "Token Bearer requis",
                Response.status(Response.Status.UNAUTHORIZED)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(erreurBody(401, "NON_AUTHENTIFIE",
                                "Un token Bearer valide est requis"))
                        .build()
            );
        }

        // getUser() est déprécié en Keycloak 26.x — on passe par le token
        AccessToken token = authResult.getToken();
        
        // Pour admin-cli, token.getSubject() et getPreferredUsername() peuvent être null.
        // On récupère l'utilisateur depuis l'authResult directement.
        UserModel acteur = authResult.getUser();
        
        // Debug temporaire
        log.infof("DEBUG: authResult=%s, acteur=%s, token=%s", 
                 authResult != null ? "OK" : "NULL", 
                 acteur != null ? acteur.getUsername() : "NULL",
                 token != null ? "OK" : "NULL");

        if (acteur == null) {
            throw new ForbiddenException(
                Response.status(Response.Status.FORBIDDEN)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(erreurBody(403, "ACTEUR_INTROUVABLE",
                                "L'utilisateur du token est introuvable dans le realm " +
                                realm.getName()))
                        .build()
            );
        }

        boolean aLeRole = acteur.getRealmRoleMappingsStream()
                                .anyMatch(r -> roleRequis.equals(r.getName()));

        if (!aLeRole) {
            throw new ForbiddenException(
                Response.status(Response.Status.FORBIDDEN)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(erreurBody(403, "ACCES_REFUSE",
                                "Le rôle '" + roleRequis + "' est requis"))
                        .build()
            );
        }

        return authResult;
    }

    // =========================================================================
    // Audit structuré [IAM-AUDIT]
    // =========================================================================

    private void audit(
            AuthenticationManager.AuthResult auth,
            RealmModel realm,
            String action,
            String cible,
            String motif
    ) {
        AccessToken token   = auth.getToken();
        String acteurId     = token.getSubject();
        String acteurLogin  = token.getPreferredUsername();
        String realmNom     = realm.getName();
        String ts           = Instant.now().toString();

        if (motif != null) {
            log.warnf("[IAM-AUDIT] action=%s realm=%s acteur=%s(%s) cible=%s motif='%s' ts=%s",
                      action, realmNom, acteurLogin, acteurId, cible, motif, ts);
        } else {
            log.infof("[IAM-AUDIT] action=%s realm=%s acteur=%s(%s) cible=%s ts=%s",
                      action, realmNom, acteurLogin, acteurId, cible, ts);
        }
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
