package ga.uob.iam.local.keycloak.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Représentation JSON d'un CompteLocal pour les réponses JAX-RS.
 *
 * Suit la convention Keycloak : classes de représentation suffixées "Representation"
 * (ex: UserRepresentation, RealmRepresentation, ClientRepresentation...).
 *
 * @JsonInclude(NON_NULL) : les champs null ne sont pas sérialisés —
 * cohérent avec les représentations natives Keycloak.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompteLocalRepresentation {

    private String id;
    private String userIdNational;
    private String identifiantNational;
    private String nom;
    private String prenom;
    private String emailInstitutionnel;
    private String etablissementCode;
    private String typeProfilSuggere;
    private String filiere;
    private String composante;
    private String statut;
    private String creeLe;
    private String misAJourLe;
    private String desactiveLe;
    private String dernierMotifModification;
    private Integer nombreModificationsManuelles;

    // Constructeur no-arg pour Jackson
    public CompteLocalRepresentation() {}

    // Getters / Setters explicites — convention Keycloak, pas de Lombok
    public String getId()                           { return id; }
    public void setId(String id)                    { this.id = id; }

    public String getUserIdNational()               { return userIdNational; }
    public void setUserIdNational(String v)         { this.userIdNational = v; }

    public String getIdentifiantNational()          { return identifiantNational; }
    public void setIdentifiantNational(String v)    { this.identifiantNational = v; }

    public String getNom()                          { return nom; }
    public void setNom(String v)                    { this.nom = v; }

    public String getPrenom()                       { return prenom; }
    public void setPrenom(String v)                 { this.prenom = v; }

    public String getEmailInstitutionnel()          { return emailInstitutionnel; }
    public void setEmailInstitutionnel(String v)    { this.emailInstitutionnel = v; }

    public String getEtablissementCode()            { return etablissementCode; }
    public void setEtablissementCode(String v)      { this.etablissementCode = v; }

    public String getTypeProfilSuggere()            { return typeProfilSuggere; }
    public void setTypeProfilSuggere(String v)      { this.typeProfilSuggere = v; }

    public String getFiliere()                      { return filiere; }
    public void setFiliere(String v)                { this.filiere = v; }

    public String getComposante()                   { return composante; }
    public void setComposante(String v)             { this.composante = v; }

    public String getStatut()                       { return statut; }
    public void setStatut(String v)                 { this.statut = v; }

    public String getCreeLe()                       { return creeLe; }
    public void setCreeLe(String v)                 { this.creeLe = v; }

    public String getMisAJourLe()                   { return misAJourLe; }
    public void setMisAJourLe(String v)             { this.misAJourLe = v; }

    public String getDesactiveLe()                  { return desactiveLe; }
    public void setDesactiveLe(String v)            { this.desactiveLe = v; }

    public String getDernierMotifModification()     { return dernierMotifModification; }
    public void setDernierMotifModification(String v) { this.dernierMotifModification = v; }

    public Integer getNombreModificationsManuelles() { return nombreModificationsManuelles; }
    public void setNombreModificationsManuelles(Integer v) { this.nombreModificationsManuelles = v; }
}
