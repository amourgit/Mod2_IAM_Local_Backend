package ga.uob.iam.local.compte.domain.model;

import java.util.Objects;

/**
 * Value Object : contexte scolaire ou professionnel d'une personne à l'UOB.
 *
 * Peut être null pour le personnel non enseignant.
 * Immuable. Comparé par valeur structurelle.
 */
public final class ContexteScolaire {

    /** Filière d'inscription (ex: L1-Informatique, M2-Droit-Public). */
    private final String filiere;

    /** Composante / UFR / faculté (ex: FST, FDSE, FSEG). */
    private final String composante;

    private ContexteScolaire(String filiere, String composante) {
        this.filiere   = filiere;
        this.composante = composante;
    }

    public static ContexteScolaire de(String filiere, String composante) {
        Objects.requireNonNull(filiere,    "filiere ne peut pas être null");
        Objects.requireNonNull(composante, "composante ne peut pas être null");
        if (filiere.isBlank() || composante.isBlank()) {
            throw new IllegalArgumentException(
                "filiere et composante ne peuvent pas être vides"
            );
        }
        return new ContexteScolaire(filiere.trim(), composante.trim().toUpperCase());
    }

    public String getFiliere()    { return filiere; }
    public String getComposante() { return composante; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContexteScolaire)) return false;
        ContexteScolaire c = (ContexteScolaire) o;
        return Objects.equals(filiere, c.filiere) &&
               Objects.equals(composante, c.composante);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filiere, composante);
    }

    @Override
    public String toString() {
        return composante + "/" + filiere;
    }
}
