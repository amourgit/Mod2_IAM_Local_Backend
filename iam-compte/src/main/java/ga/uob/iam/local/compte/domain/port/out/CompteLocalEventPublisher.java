package ga.uob.iam.local.compte.domain.port.out;

import ga.uob.iam.local.compte.domain.event.DomainEvent;

import java.util.List;

/**
 * Port sortant : publication des domain events après persistance.
 *
 * Garantit que les events ne sont publiés QUE si la transaction a réussi
 * (implémentation via TransactionalEventPublisher Spring ou Kafka outbox).
 *
 * Implémentée par : CompteLocalSpringEventPublisher (infrastructure).
 */
public interface CompteLocalEventPublisher {

    /**
     * Publie tous les events collectés sur un aggregate.
     * Appelé par CompteLocalService après sauvegarder().
     */
    void publier(List<DomainEvent> events);
}
