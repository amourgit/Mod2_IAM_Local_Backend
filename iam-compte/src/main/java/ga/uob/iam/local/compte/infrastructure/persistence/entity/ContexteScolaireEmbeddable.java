package ga.uob.iam.local.compte.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Embeddable JPA pour ContexteScolaire.
 * Stocké dans la même table que CompteLocalJpaEntity (colonnes filiere, composante).
 *
 * NOTE : On n'utilise PAS Lombok ici car le maven-compiler-plugin du parent Keycloak
 * (version 3.8.1) ne configure pas automatiquement l'annotation processor Lombok.
 * Les getters/setters sont écrits explicitement — plus lisible et debuggable.
 */
@Embeddable
public class ContexteScolaireEmbeddable {

    @Column(name = "filiere", length = 100)
    private String filiere;

    @Column(name = "composante", length = 50)
    private String composante;

    // =========================================================================
    // Constructeurs
    // =========================================================================

    /** Constructeur no-arg requis par JPA. */
    public ContexteScolaireEmbeddable() {}

    public ContexteScolaireEmbeddable(String filiere, String composante) {
        this.filiere    = filiere;
        this.composante = composante;
    }

    // =========================================================================
    // Getters / Setters explicites
    // =========================================================================

    public String getFiliere() {
        return filiere;
    }

    public void setFiliere(String filiere) {
        this.filiere = filiere;
    }

    public String getComposante() {
        return composante;
    }

    public void setComposante(String composante) {
        this.composante = composante;
    }
}
