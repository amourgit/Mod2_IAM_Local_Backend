package ga.uob.iam.local.compte.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object : identifiant national d'une personne.
 *
 * Format attendu : {PREFIX}-{ANNEE}-{SEQUENCE}
 *   Exemples : ETU-2025-00412  /  PERS-2020-00001
 *
 * Immuable. Comparable par valeur. Validé à la construction.
 * On ne stocke jamais une String brute pour un identifiant aussi critique.
 */
public final class IdentifiantNational {

    private static final Pattern FORMAT =
        Pattern.compile("^[A-Z]{2,10}-\\d{4}-\\d{5}$");

    private final String valeur;

    private IdentifiantNational(String valeur) {
        this.valeur = valeur;
    }

    /**
     * Factory method avec validation stricte du format.
     *
     * @throws IllegalArgumentException si le format ne correspond pas.
     */
    public static IdentifiantNational de(String valeur) {
        Objects.requireNonNull(valeur, "L'identifiant national ne peut pas être null");
        String normalise = valeur.trim().toUpperCase();
        if (!FORMAT.matcher(normalise).matches()) {
            throw new IllegalArgumentException(
                "Format d'identifiant national invalide : '" + valeur +
                "'. Attendu : PREFIX-ANNEE-SEQUENCE (ex: ETU-2025-00412)"
            );
        }
        return new IdentifiantNational(normalise);
    }

    public String getValeur() {
        return valeur;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdentifiantNational)) return false;
        return valeur.equals(((IdentifiantNational) o).valeur);
    }

    @Override
    public int hashCode() {
        return valeur.hashCode();
    }

    @Override
    public String toString() {
        return valeur;
    }
}
