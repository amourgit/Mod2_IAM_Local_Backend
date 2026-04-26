package ga.uob.iam.local.compte.infrastructure.kafka.consumer;

import ga.uob.iam.local.compte.application.command.CreerCompteLocalCommand;
import ga.uob.iam.local.compte.application.command.SynchroniserCompteLocalCommand;
import ga.uob.iam.local.compte.domain.port.in.CreerCompteLocalUseCase;
import ga.uob.iam.local.compte.domain.port.in.DesactiverCompteLocalUseCase;
import ga.uob.iam.local.compte.domain.port.in.SynchroniserCompteLocalUseCase;
import ga.uob.iam.local.compte.infrastructure.kafka.dto.IamCentralCompteCreatedPayload;
import ga.uob.iam.local.compte.infrastructure.kafka.dto.IamCentralCompteMisAJourPayload;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Consumer Kafka des events IAM Central concernant les comptes.
 *
 * Topics écoutés :
 *   - iam.central.compte.cree       → création d'un CompteLocal
 *   - iam.central.compte.mis_a_jour → synchronisation d'un CompteLocal
 *   - iam.central.compte.desactive  → désactivation d'un CompteLocal
 *
 * Stratégie d'acquittement : MANUAL (AckMode.MANUAL_IMMEDIATE)
 * → on n'acquitte QUE si le traitement a réussi.
 * → en cas d'erreur, Kafka re-livre le message (idempotence garantie côté service).
 *
 * Le versionHash est calculé ici depuis le payload brut si absent du message.
 */
@Component
public class IamCentralCompteKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(IamCentralCompteKafkaConsumer.class);

    private static final String TOPIC_CREE       = "iam.central.compte.cree";
    private static final String TOPIC_MIS_A_JOUR = "iam.central.compte.mis_a_jour";
    private static final String TOPIC_DESACTIVE  = "iam.central.compte.desactive";

    private final CreerCompteLocalUseCase       creerUseCase;
    private final SynchroniserCompteLocalUseCase synchroniserUseCase;
    private final DesactiverCompteLocalUseCase   desactiverUseCase;

    public IamCentralCompteKafkaConsumer(
            CreerCompteLocalUseCase       creerUseCase,
            SynchroniserCompteLocalUseCase synchroniserUseCase,
            DesactiverCompteLocalUseCase   desactiverUseCase
    ) {
        this.creerUseCase       = creerUseCase;
        this.synchroniserUseCase = synchroniserUseCase;
        this.desactiverUseCase   = desactiverUseCase;
    }

    // =========================================================================
    // Écoute : compte créé
    // =========================================================================

    @KafkaListener(
        topics   = TOPIC_CREE,
        groupId  = "${spring.kafka.consumer.group-id}",
        containerFactory = "compteKafkaListenerContainerFactory"
    )
    public void onCompteCreated(
            ConsumerRecord<String, IamCentralCompteCreatedPayload> record,
            Acknowledgment ack
    ) {
        IamCentralCompteCreatedPayload payload = record.value();
        MDC.put("kafkaTopic",         TOPIC_CREE);
        MDC.put("kafkaOffset",        String.valueOf(record.offset()));
        MDC.put("userIdNational",     payload.userIdNational());
        try {
            log.info("Event reçu : compte.cree pour {}", payload.identifiantNational());

            String versionHash = calculerHash(record.value().toString());

            String filiere   = payload.contexteScolaire() != null ? payload.contexteScolaire().filiere()    : null;
            String composante = payload.contexteScolaire() != null ? payload.contexteScolaire().composante() : null;

            CreerCompteLocalCommand command = new CreerCompteLocalCommand(
                payload.userIdNational(),
                payload.identifiantNational(),
                payload.nom(),
                payload.prenom(),
                payload.email(),
                payload.etablissementCode(),
                payload.typeProfilSuggere(),
                filiere,
                composante,
                versionHash
            );

            creerUseCase.creer(command);
            ack.acknowledge();
            log.info("Event compte.cree traité et acquitté — offset={}", record.offset());

        } catch (Exception e) {
            log.error("Échec traitement event compte.cree offset={} : {}",
                      record.offset(), e.getMessage(), e);
            // Pas d'ack → Kafka re-livre le message
        } finally {
            MDC.clear();
        }
    }

    // =========================================================================
    // Écoute : compte mis à jour
    // =========================================================================

    @KafkaListener(
        topics   = TOPIC_MIS_A_JOUR,
        groupId  = "${spring.kafka.consumer.group-id}",
        containerFactory = "compteKafkaListenerContainerFactory"
    )
    public void onCompteMisAJour(
            ConsumerRecord<String, IamCentralCompteMisAJourPayload> record,
            Acknowledgment ack
    ) {
        IamCentralCompteMisAJourPayload payload = record.value();
        MDC.put("kafkaTopic",     TOPIC_MIS_A_JOUR);
        MDC.put("kafkaOffset",    String.valueOf(record.offset()));
        MDC.put("userIdNational", payload.userIdNational());
        try {
            log.info("Event reçu : compte.mis_a_jour pour userIdNational={}", payload.userIdNational());

            String versionHash = payload.versionHash() != null
                ? payload.versionHash()
                : calculerHash(record.value().toString());

            String filiere    = payload.contexteScolaire() != null ? payload.contexteScolaire().filiere()    : null;
            String composante = payload.contexteScolaire() != null ? payload.contexteScolaire().composante() : null;

            SynchroniserCompteLocalCommand command = new SynchroniserCompteLocalCommand(
                payload.userIdNational(),
                payload.nom(),
                payload.prenom(),
                payload.email(),
                payload.typeProfilSuggere(),
                filiere,
                composante,
                versionHash
            );

            synchroniserUseCase.synchroniser(command);
            ack.acknowledge();
            log.info("Event compte.mis_a_jour traité et acquitté — offset={}", record.offset());

        } catch (Exception e) {
            log.error("Échec traitement event compte.mis_a_jour offset={} : {}",
                      record.offset(), e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }

    // =========================================================================
    // Écoute : compte désactivé
    // =========================================================================

    @KafkaListener(
        topics   = TOPIC_DESACTIVE,
        groupId  = "${spring.kafka.consumer.group-id}",
        containerFactory = "compteKafkaListenerContainerFactory"
    )
    public void onCompteDesactive(
            ConsumerRecord<String, String> record,
            Acknowledgment ack
    ) {
        // Le payload de désactivation est minimaliste : juste le userIdNational
        String userIdNational = record.key();
        MDC.put("kafkaTopic",     TOPIC_DESACTIVE);
        MDC.put("kafkaOffset",    String.valueOf(record.offset()));
        MDC.put("userIdNational", userIdNational);
        try {
            log.warn("Event reçu : compte.desactive pour userIdNational={}", userIdNational);
            desactiverUseCase.desactiver(userIdNational);
            ack.acknowledge();
            log.warn("Event compte.desactive traité et acquitté — offset={}", record.offset());

        } catch (Exception e) {
            log.error("Échec traitement event compte.desactive offset={} : {}",
                      record.offset(), e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }

    // =========================================================================
    // Utilitaire : calcul du versionHash
    // =========================================================================

    private String calculerHash(String contenu) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(contenu.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponible — JVM compromise", e);
        }
    }
}
