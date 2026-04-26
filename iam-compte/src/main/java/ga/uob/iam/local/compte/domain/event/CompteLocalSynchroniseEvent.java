package ga.uob.iam.local.compte.domain.event;

import ga.uob.iam.local.compte.domain.model.IdentifiantNational;

import java.util.UUID;

/**
 * Émis quand un CompteLocal existant est mis à jour
 * suite à un event de synchronisation depuis l'IAM Central.
 *
 * Consommé par : module User (pour propager les changements aux profils).
 */
public final class CompteLocalSynchroniseEvent extends DomainEvent {

    public static final String EVENT_TYPE = "compte.local.synchronise";

    private final String userIdNational;
    private final IdentifiantNational identifiantNational;

    public CompteLocalSynchroniseEvent(
            UUID compteLocalId,
            String userIdNational,
            IdentifiantNational identifiantNational
    ) {
        super(compteLocalId);
        this.userIdNational      = userIdNational;
        this.identifiantNational = identifiantNational;
    }

    @Override
    public String getEventType() { return EVENT_TYPE; }

    public String getUserIdNational()               { return userIdNational; }
    public IdentifiantNational getIdentifiantNational() { return identifiantNational; }
}
