package ga.uob.iam.local.compte.domain.event;

import ga.uob.iam.local.compte.domain.model.IdentifiantNational;

import java.util.UUID;

/** Émis lors de la réactivation d'un CompteLocal suspendu. */
public final class CompteLocalReactiveEvent extends DomainEvent {

    public static final String EVENT_TYPE = "compte.local.reactive";

    private final String userIdNational;
    private final IdentifiantNational identifiantNational;
    private final String motif;

    public CompteLocalReactiveEvent(UUID id, String userIdNational,
                                    IdentifiantNational identifiantNational, String motif) {
        super(id);
        this.userIdNational      = userIdNational;
        this.identifiantNational = identifiantNational;
        this.motif               = motif;
    }

    @Override public String getEventType()                        { return EVENT_TYPE; }
    public String getUserIdNational()                             { return userIdNational; }
    public IdentifiantNational getIdentifiantNational()           { return identifiantNational; }
    public String getMotif()                                      { return motif; }
}
