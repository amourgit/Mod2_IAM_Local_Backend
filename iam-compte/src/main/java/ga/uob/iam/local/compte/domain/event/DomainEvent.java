package ga.uob.iam.local.compte.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrat de base de tous les domain events du module Compte.
 *
 * Chaque event est immuable et porte :
 *   - eventId    : identifiant unique de l'event (déduplication)
 *   - occurredAt : horodatage précis de l'occurrence
 *   - aggregateId: UUID de l'aggregate root concerné
 */
public abstract class DomainEvent {

    private final UUID eventId;
    private final Instant occurredAt;
    private final UUID aggregateId;

    protected DomainEvent(UUID aggregateId) {
        this.eventId     = UUID.randomUUID();
        this.occurredAt  = Instant.now();
        this.aggregateId = aggregateId;
    }

    public UUID getEventId()     { return eventId; }
    public Instant getOccurredAt() { return occurredAt; }
    public UUID getAggregateId() { return aggregateId; }

    /** Nom technique de l'event — utilisé pour le routing Kafka/logging. */
    public abstract String getEventType();
}
