package ga.uob.iam.local.keycloak.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Corps de requête pour la modification manuelle d'un CompteLocal.
 * Tous les champs nominaux sont optionnels — seul le motif est obligatoire.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModifierCompteRepresentation {

    private String nom;
    private String prenom;
    private String emailInstitutionnel;
    private String typeProfilSuggere;
    private String filiere;
    private String composante;

    /** Motif administratif — minimum 20 caractères, obligatoire. */
    private String motif;

    public ModifierCompteRepresentation() {}

    public String getNom()                       { return nom; }
    public void setNom(String v)                 { this.nom = v; }

    public String getPrenom()                    { return prenom; }
    public void setPrenom(String v)              { this.prenom = v; }

    public String getEmailInstitutionnel()       { return emailInstitutionnel; }
    public void setEmailInstitutionnel(String v) { this.emailInstitutionnel = v; }

    public String getTypeProfilSuggere()         { return typeProfilSuggere; }
    public void setTypeProfilSuggere(String v)   { this.typeProfilSuggere = v; }

    public String getFiliere()                   { return filiere; }
    public void setFiliere(String v)             { this.filiere = v; }

    public String getComposante()                { return composante; }
    public void setComposante(String v)          { this.composante = v; }

    public String getMotif()                     { return motif; }
    public void setMotif(String v)               { this.motif = v; }
}
