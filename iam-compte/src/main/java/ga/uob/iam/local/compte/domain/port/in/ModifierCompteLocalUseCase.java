package ga.uob.iam.local.compte.domain.port.in;

import ga.uob.iam.local.compte.application.command.ModifierCompteLocalCommand;
import ga.uob.iam.local.compte.domain.model.CompteLocal;

/**
 * Port entrant : modification manuelle d'un CompteLocal.
 *
 * CAS D'USAGE EXTRÊME UNIQUEMENT.
 * En opération normale, les données sont synchronisées automatiquement
 * depuis l'IAM Central via Kafka. Cette API existe pour les corrections
 * administratives d'urgence (erreur de saisie, rectification judiciaire...).
 *
 * Chaque modification manuelle est tracée avec le motif obligatoire.
 * Implémenté par : CompteLocalService.
 */
public interface ModifierCompteLocalUseCase {

    /**
     * Modifie manuellement les données nominales d'un CompteLocal.
     *
     * @param command données de modification avec motif obligatoire
     * @return le CompteLocal mis à jour
     */
    CompteLocal modifier(ModifierCompteLocalCommand command);
}
