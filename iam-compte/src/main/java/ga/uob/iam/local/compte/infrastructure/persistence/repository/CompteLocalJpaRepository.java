package ga.uob.iam.local.compte.infrastructure.persistence.repository;

import ga.uob.iam.local.compte.infrastructure.persistence.entity.CompteLocalJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository Spring Data JPA pour CompteLocalJpaEntity.
 *
 * Ce repository manipule uniquement des JpaEntity.
 * Le mapping vers/depuis le domain model est fait par CompteLocalPersistenceAdapter.
 */
@Repository
public interface CompteLocalJpaRepository extends JpaRepository<CompteLocalJpaEntity, UUID> {

    Optional<CompteLocalJpaEntity> findByUserIdNational(String userIdNational);

    Optional<CompteLocalJpaEntity> findByIdentifiantNational(String identifiantNational);

    boolean existsByUserIdNational(String userIdNational);
}
