package ga.uob.iam.local.compte.domain.event;

import ga.uob.iam.local.compte.domain.model.IdentifiantNational;

import java.util.UUID;

/**
 * Émis lors d'une modification manuelle d'un CompteLocal (cas extrême).
 * Porte le motif de la modification pour la traçabilité complète.
 */
public final class CompteLocalModifieEvent extends DomainEvent {

    public static final String EVENT_TYPE = "compte.local.modifie.manuellement";

    private final String userIdNational;
    private final IdentifiantNational identifiantNational;
    private final String motif;

    public CompteLocalModifieEvent(
            UUID compteLocalId,
            String userIdNational,
            IdentifiantNational identifiantNational,
            String motif
    ) {
        super(compteLocalId);
        this.userIdNational      = userIdNational;
        this.identifiantNational = identifiantNational;
        this.motif               = motif;
    }

    @Override
    public String getEventType() { return EVENT_TYPE; }

    public String getUserIdNational()                   { return userIdNational; }
    public IdentifiantNational getIdentifiantNational() { return identifiantNational; }
    public String getMotif()                            { return motif; }
}
