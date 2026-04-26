package ga.uob.iam.local.compte.domain.port.out;

import ga.uob.iam.local.compte.domain.model.CompteLocal;

import java.util.Optional;
import java.util.UUID;

/**
 * Port sortant : persistance des CompteLocal.
 *
 * Interface du domaine — aucune dépendance JPA, Hibernate ou Spring ici.
 * Implémentée par : CompteLocalPersistenceAdapter (infrastructure).
 */
public interface CompteLocalRepository {

    /** Persiste un nouveau CompteLocal ou met à jour un existant. */
    CompteLocal sauvegarder(CompteLocal compte);

    Optional<CompteLocal> trouverParId(UUID id);

    Optional<CompteLocal> trouverParUserIdNational(String userIdNational);

    Optional<CompteLocal> trouverParIdentifiantNational(String identifiantNational);

    boolean existeParUserIdNational(String userIdNational);
}
