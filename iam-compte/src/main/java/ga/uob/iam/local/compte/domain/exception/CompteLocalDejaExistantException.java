package ga.uob.iam.local.compte.domain.exception;

/**
 * Levée quand on tente de créer un CompteLocal pour un userIdNational
 * qui existe déjà en base (protection contre les doublons).
 */
public class CompteLocalDejaExistantException extends CompteLocalException {

    private final String userIdNational;

    public CompteLocalDejaExistantException(String userIdNational) {
        super("Un CompteLocal existe déjà pour userIdNational=" + userIdNational);
        this.userIdNational = userIdNational;
    }

    public String getUserIdNational() { return userIdNational; }
}
