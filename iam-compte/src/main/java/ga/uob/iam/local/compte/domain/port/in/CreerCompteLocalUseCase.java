package ga.uob.iam.local.compte.domain.port.in;

import ga.uob.iam.local.compte.application.command.CreerCompteLocalCommand;
import ga.uob.iam.local.compte.domain.model.CompteLocal;

/**
 * Port entrant : création d'un CompteLocal.
 *
 * Implémenté par : CompteLocalService (couche application).
 * Appelé par     : IamCentralCompteKafkaConsumer (infrastructure Kafka).
 */
public interface CreerCompteLocalUseCase {

    /**
     * Crée un CompteLocal depuis les données de provisioning IAM Central.
     * Idempotent : si le compte existe déjà, retourne l'existant sans erreur.
     *
     * @param command données validées du payload IAM Central
     * @return le CompteLocal créé ou retrouvé
     */
    CompteLocal creer(CreerCompteLocalCommand command);
}
