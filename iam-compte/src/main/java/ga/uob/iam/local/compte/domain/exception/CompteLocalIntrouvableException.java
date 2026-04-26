package ga.uob.iam.local.compte.domain.exception;

import java.util.UUID;

/**
 * Levée quand un CompteLocal est recherché mais n'existe pas.
 */
public class CompteLocalIntrouvableException extends CompteLocalException {

    public CompteLocalIntrouvableException(UUID id) {
        super("Aucun CompteLocal trouvé pour id=" + id);
    }

    public CompteLocalIntrouvableException(String userIdNational) {
        super("Aucun CompteLocal trouvé pour userIdNational=" + userIdNational);
    }
}
