package ga.uob.iam.local.compte.application.mapper;

import ga.uob.iam.local.compte.application.command.CreerCompteLocalCommand;
import ga.uob.iam.local.compte.application.dto.CompteLocalDto;
import ga.uob.iam.local.compte.domain.model.CompteLocal;
import ga.uob.iam.local.compte.domain.model.ContexteScolaire;
import ga.uob.iam.local.compte.domain.model.IdentifiantNational;
import ga.uob.iam.local.compte.domain.model.TypeProfil;
import ga.uob.iam.local.compte.infrastructure.rest.dto.CompteLocalDetailDto;
import ga.uob.iam.local.compte.infrastructure.rest.dto.CompteLocalSommaireDto;
import org.springframework.stereotype.Component;

/**
 * Mapper manuel entre domain model, commandes et DTOs.
 *
 * Centralisé ici pour éviter la dispersion de la logique de transformation.
 * Toutes les conversions domaine ↔ DTO passent par cette classe unique.
 */
@Component
public class CompteLocalMapper {

    // =========================================================================
    // Domaine → DTO de lecture (application layer)
    // =========================================================================

    public CompteLocalDto versDto(CompteLocal compte) {
        ContexteScolaire ctx = compte.getContexteScolaire();
        return new CompteLocalDto(
            compte.getId(),
            compte.getUserIdNational(),
            compte.getIdentifiantNational().getValeur(),
            compte.getNom(),
            compte.getPrenom(),
            compte.getEmailInstitutionnel(),
            compte.getEtablissementCode(),
            compte.getTypeProfilSuggere().name(),
            ctx != null ? ctx.getFiliere()    : null,
            ctx != null ? ctx.getComposante() : null,
            compte.getStatut().name(),
            compte.getCreeLe(),
            compte.getMisAJourLe()
        );
    }

    // =========================================================================
    // Domaine → DTO détaillé (REST layer — inclut traçabilité manuelle)
    // =========================================================================

    public CompteLocalDetailDto versDetailDto(CompteLocal compte) {
        ContexteScolaire ctx = compte.getContexteScolaire();
        return new CompteLocalDetailDto(
            compte.getId(),
            compte.getUserIdNational(),
            compte.getIdentifiantNational().getValeur(),
            compte.getNom(),
            compte.getPrenom(),
            compte.getEmailInstitutionnel(),
            compte.getEtablissementCode(),
            compte.getTypeProfilSuggere().name(),
            ctx != null ? ctx.getFiliere()    : null,
            ctx != null ? ctx.getComposante() : null,
            compte.getStatut().name(),
            compte.getCreeLe(),
            compte.getMisAJourLe(),
            compte.getDesactiveLe(),
            compte.getDernierMotifModification(),
            compte.getNombreModificationsManuelles()
        );
    }

    // =========================================================================
    // Domaine → DTO sommaire (REST layer — liste allégée)
    // =========================================================================

    public CompteLocalSommaireDto versSommaireDto(CompteLocal compte) {
        return new CompteLocalSommaireDto(
            compte.getId(),
            compte.getIdentifiantNational().getValeur(),
            compte.getNom(),
            compte.getPrenom(),
            compte.getEmailInstitutionnel(),
            compte.getEtablissementCode(),
            compte.getTypeProfilSuggere().name(),
            compte.getStatut().name(),
            compte.getCreeLe()
        );
    }

    // =========================================================================
    // Extraction des value objects depuis les commandes
    // =========================================================================

    public TypeProfil extraireTypeProfil(String typeProfilSuggere) {
        try {
            return TypeProfil.valueOf(typeProfilSuggere.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "typeProfilSuggere inconnu : '" + typeProfilSuggere +
                "'. Valeurs acceptées : ETUDIANT, PERSONNEL_ADM, ENSEIGNANT, ENSEIGNANT_ADM"
            );
        }
    }

    public ContexteScolaire extraireContexteScolaire(String filiere, String composante) {
        if (filiere == null || filiere.isBlank() || composante == null || composante.isBlank()) {
            return null;
        }
        return ContexteScolaire.de(filiere, composante);
    }

    public IdentifiantNational extraireIdentifiantNational(CreerCompteLocalCommand command) {
        return IdentifiantNational.de(command.identifiantNational());
    }
}
