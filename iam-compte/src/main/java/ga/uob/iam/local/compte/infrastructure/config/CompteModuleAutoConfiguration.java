package ga.uob.iam.local.compte.infrastructure.config;

import ga.uob.iam.local.compte.domain.event.DomainEvent;
import ga.uob.iam.local.compte.domain.port.out.CompteLocalEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.List;

/**
 * Auto-configuration du module Compte.
 *
 * Enregistre :
 *   1. CompteLocalEventPublisher — publie les domain events via Spring ApplicationEventPublisher.
 *      Les @TransactionalEventListener des modules consommateurs les reçoivent
 *      APRÈS commit de la transaction (garantie outbox).
 *
 *   2. compteKafkaListenerContainerFactory — factory Kafka dédiée au module Compte.
 *      Acquittement MANUEL : on n'acquitte QUE si le traitement a réussi.
 *      En cas d'erreur, Kafka re-livre le message (l'idempotence du service garantit
 *      qu'un re-traitement n'a pas d'effet de bord).
 *
 * NOTE : KeycloakAdminClient N'EST PLUS configuré ici.
 * On utilise l'API interne KeycloakSession via KeycloakSessionProvider.
 */
@Configuration
@EnableKafka
public class CompteModuleAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CompteModuleAutoConfiguration.class);

    // =========================================================================
    // Publication des Domain Events
    // =========================================================================

    /**
     * Publie les DomainEvents collectés sur l'aggregate via Spring.
     * Le @TransactionalEventPublisher garantit publication APRÈS commit.
     */
    @Bean
    public CompteLocalEventPublisher compteLocalEventPublisher(
            ApplicationEventPublisher springPublisher
    ) {
        return (List<DomainEvent> events) -> events.forEach(event -> {
            log.debug("Publication domain event : type={} aggregateId={}",
                      event.getEventType(), event.getAggregateId());
            springPublisher.publishEvent(event);
        });
    }

    // =========================================================================
    // Kafka Listener Container Factory — dédiée au module Compte
    // =========================================================================

    /**
     * Factory Kafka avec acquittement MANUEL.
     * AckMode.MANUAL_IMMEDIATE : l'acquittement est fait dans le consumer,
     * uniquement si le traitement complet a réussi.
     * Concurrency 3 : 3 threads parallèles maximum par topic/partition.
     */
    @Bean("compteKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object>
    compteKafkaListenerContainerFactory(ConsumerFactory<String, Object> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(3);

        return factory;
    }
}
