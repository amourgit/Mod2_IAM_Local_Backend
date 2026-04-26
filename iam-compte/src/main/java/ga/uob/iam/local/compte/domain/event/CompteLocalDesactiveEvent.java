package ga.uob.iam.local.compte.domain.event;

import ga.uob.iam.local.compte.domain.model.IdentifiantNational;

import java.util.UUID;

/**
 * Émis quand un CompteLocal est désactivé définitivement.
 *
 * Consommé par : module User (pour désactiver tous les profils liés),
 *                module Keycloak Adapter (pour désactiver le User Keycloak).
 */
public final class CompteLocalDesactiveEvent extends DomainEvent {

    public static final String EVENT_TYPE = "compte.local.desactive";

    private final String userIdNational;
    private final IdentifiantNational identifiantNational;

    public CompteLocalDesactiveEvent(
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
