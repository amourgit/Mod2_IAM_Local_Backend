package ga.uob.iam.local.compte.infrastructure.persistence.adapter;

import ga.uob.iam.local.compte.domain.model.*;
import ga.uob.iam.local.compte.domain.port.out.CompteLocalRepository;
import ga.uob.iam.local.compte.infrastructure.persistence.entity.CompteLocalJpaEntity;
import ga.uob.iam.local.compte.infrastructure.persistence.entity.ContexteScolaireEmbeddable;
import ga.uob.iam.local.compte.infrastructure.persistence.repository.CompteLocalJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptateur de persistance.
 *
 * Responsable du mapping bidirectionnel complet :
 *   CompteLocal (domaine) ↔ CompteLocalJpaEntity (JPA)
 *
 * Inclut les champs de traçabilité des modifications manuelles.
 */
@Component
public class CompteLocalPersistenceAdapter implements CompteLocalRepository {

    private final CompteLocalJpaRepository jpaRepository;

    public CompteLocalPersistenceAdapter(CompteLocalJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CompteLocal sauvegarder(CompteLocal compte) {
        CompteLocalJpaEntity entity = versEntity(compte);
        CompteLocalJpaEntity sauvegarde = jpaRepository.save(entity);
        return versDomaine(sauvegarde);
    }

    @Override
    public Optional<CompteLocal> trouverParId(UUID id) {
        return jpaRepository.findById(id).map(this::versDomaine);
    }

    @Override
    public Optional<CompteLocal> trouverParUserIdNational(String userIdNational) {
        return jpaRepository.findByUserIdNational(userIdNational).map(this::versDomaine);
    }

    @Override
    public Optional<CompteLocal> trouverParIdentifiantNational(String identifiantNational) {
        return jpaRepository.findByIdentifiantNational(identifiantNational).map(this::versDomaine);
    }

    @Override
    public boolean existeParUserIdNational(String userIdNational) {
        return jpaRepository.existsByUserIdNational(userIdNational);
    }

    // =========================================================================
    // Mapping domaine → JPA
    // =========================================================================

    private CompteLocalJpaEntity versEntity(CompteLocal compte) {
        CompteLocalJpaEntity entity = new CompteLocalJpaEntity();
        entity.setId(compte.getId());
        entity.setUserIdNational(compte.getUserIdNational());
        entity.setIdentifiantNational(compte.getIdentifiantNational().getValeur());
        entity.setNom(compte.getNom());
        entity.setPrenom(compte.getPrenom());
        entity.setEmailInstitutionnel(compte.getEmailInstitutionnel());
        entity.setEtablissementCode(compte.getEtablissementCode());
        entity.setTypeProfilSuggere(compte.getTypeProfilSuggere().name());
        entity.setStatut(compte.getStatut().name());
        entity.setDesactiveLe(compte.getDesactiveLe());
        entity.setVersionHash(compte.getVersionHash());
        entity.setDernierMotifModification(compte.getDernierMotifModification());
        entity.setNombreModificationsManuelles(compte.getNombreModificationsManuelles());

        if (compte.getContexteScolaire() != null) {
            ContexteScolaireEmbeddable ctx = new ContexteScolaireEmbeddable();
            ctx.setFiliere(compte.getContexteScolaire().getFiliere());
            ctx.setComposante(compte.getContexteScolaire().getComposante());
            entity.setContexteScolaire(ctx);
        }
        return entity;
    }

    // =========================================================================
    // Mapping JPA → domaine
    // =========================================================================

    private CompteLocal versDomaine(CompteLocalJpaEntity entity) {
        ContexteScolaire ctx = null;
        if (entity.getContexteScolaire() != null &&
            entity.getContexteScolaire().getFiliere() != null) {
            ctx = ContexteScolaire.de(
                entity.getContexteScolaire().getFiliere(),
                entity.getContexteScolaire().getComposante()
            );
        }
        return CompteLocal.reconstituier(
            entity.getId(),
            entity.getUserIdNational(),
            IdentifiantNational.de(entity.getIdentifiantNational()),
            entity.getNom(),
            entity.getPrenom(),
            entity.getEmailInstitutionnel(),
            entity.getEtablissementCode(),
            TypeProfil.valueOf(entity.getTypeProfilSuggere()),
            ctx,
            CompteStatus.valueOf(entity.getStatut()),
            entity.getCreeLe(),
            entity.getMisAJourLe(),
            entity.getDesactiveLe(),
            entity.getVersionHash(),
            entity.getDernierMotifModification(),
            entity.getNombreModificationsManuelles()
        );
    }
}
