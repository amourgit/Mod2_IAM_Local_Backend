package ga.uob.iam.local.compte.domain.event;

import ga.uob.iam.local.compte.domain.model.IdentifiantNational;

import java.util.UUID;

/**
 * Émis quand un CompteLocal est créé pour la première fois
 * suite à un provisioning depuis l'IAM Central.
 *
 * Consommé par : module User (pour créer le profil Keycloak local),
 *                module Keycloak Adapter (pour écrire id_compte sur le User).
 */
public final class CompteLocalCreeEvent extends DomainEvent {

    public static final String EVENT_TYPE = "compte.local.cree";

    private final String userIdNational;
    private final IdentifiantNational identifiantNational;

    public CompteLocalCreeEvent(
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
