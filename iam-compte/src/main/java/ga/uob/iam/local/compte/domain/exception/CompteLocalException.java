package ga.uob.iam.local.compte.domain.exception;

/**
 * Exception racine du module Compte.
 * Toutes les exceptions métier du module en héritent.
 * Runtime : on ne veut pas de checked exceptions dans le domaine.
 */
public class CompteLocalException extends RuntimeException {

    public CompteLocalException(String message) {
        super(message);
    }

    public CompteLocalException(String message, Throwable cause) {
        super(message, cause);
    }
}
