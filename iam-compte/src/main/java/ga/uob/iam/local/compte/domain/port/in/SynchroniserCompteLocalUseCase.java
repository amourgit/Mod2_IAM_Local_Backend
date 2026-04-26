package ga.uob.iam.local.compte.domain.port.in;

import ga.uob.iam.local.compte.application.command.SynchroniserCompteLocalCommand;
import ga.uob.iam.local.compte.domain.model.CompteLocal;

/**
 * Port entrant : synchronisation d'un CompteLocal existant depuis l'IAM Central.
 *
 * Implémenté par : CompteLocalService.
 * Appelé par     : IamCentralCompteKafkaConsumer sur event mis_a_jour.
 */
public interface SynchroniserCompteLocalUseCase {

    /**
     * Met à jour un CompteLocal existant.
     * Idempotent via versionHash : rejette silencieusement les doublons.
     *
     * @param command données de mise à jour validées
     * @return le CompteLocal mis à jour
     */
    CompteLocal synchroniser(SynchroniserCompteLocalCommand command);
}
