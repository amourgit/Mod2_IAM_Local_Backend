package ga.uob.iam.local.compte.domain.port.in;

import ga.uob.iam.local.compte.domain.model.CompteLocal;

import java.util.Optional;
import java.util.UUID;

/**
 * Port entrant : lecture des CompteLocal.
 *
 * Implémenté par : CompteLocalService.
 * Appelé par     : API REST interne, module User, module Service.
 */
public interface RechercherCompteLocalUseCase {

    Optional<CompteLocal> parId(UUID id);

    Optional<CompteLocal> parUserIdNational(String userIdNational);

    Optional<CompteLocal> parIdentifiantNational(String identifiantNational);
}
