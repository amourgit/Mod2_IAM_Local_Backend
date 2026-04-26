package ga.uob.iam.local.compte.infrastructure.rest.handler;

import ga.uob.iam.local.compte.domain.exception.CompteLocalDejaExistantException;
import ga.uob.iam.local.compte.domain.exception.CompteLocalException;
import ga.uob.iam.local.compte.domain.exception.CompteLocalIntrouvableException;
import ga.uob.iam.local.compte.infrastructure.rest.dto.ApiErreurDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestionnaire global des exceptions du module Compte.
 *
 * Intercepte toutes les exceptions levées par les controllers du module
 * et les traduit en réponses HTTP structurées avec le format ApiErreurDto.
 *
 * Chaque type d'exception a son propre code HTTP et code métier.
 * Les erreurs sont loguées avec le niveau approprié (warn ou error).
 */
@RestControllerAdvice(basePackages = "ga.uob.iam.local.compte.infrastructure.rest")
public class CompteExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CompteExceptionHandler.class);

    // =========================================================================
    // Exceptions métier du domaine
    // =========================================================================

    @ExceptionHandler(CompteLocalIntrouvableException.class)
    public ResponseEntity<ApiErreurDto> handleIntrouvable(CompteLocalIntrouvableException ex) {
        log.warn("CompteLocal introuvable : {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiErreurDto.de(404, "COMPTE_INTROUVABLE", ex.getMessage()));
    }

    @ExceptionHandler(CompteLocalDejaExistantException.class)
    public ResponseEntity<ApiErreurDto> handleDejaExistant(CompteLocalDejaExistantException ex) {
        log.warn("CompteLocal déjà existant : {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiErreurDto.de(409, "COMPTE_DEJA_EXISTANT", ex.getMessage()));
    }

    @ExceptionHandler(CompteLocalException.class)
    public ResponseEntity<ApiErreurDto> handleCompteLocalException(CompteLocalException ex) {
        log.warn("Erreur métier CompteLocal : {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiErreurDto.de(422, "ERREUR_METIER_COMPTE", ex.getMessage()));
    }

    // =========================================================================
    // Erreurs de validation (Bean Validation)
    // =========================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErreurDto> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());

        log.warn("Validation échouée : {}", details);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiErreurDto.deValidation("Les données fournies sont invalides", details));
    }

    // =========================================================================
    // Erreurs de paramètres illégaux (Value Objects)
    // =========================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErreurDto> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Argument invalide : {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiErreurDto.de(400, "PARAMETRE_INVALIDE", ex.getMessage()));
    }

    // =========================================================================
    // Erreurs inattendues
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErreurDto> handleGeneral(Exception ex) {
        log.error("Erreur inattendue dans le module Compte : {}", ex.getMessage(), ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiErreurDto.de(500, "ERREUR_INTERNE",
                "Une erreur inattendue s'est produite. Contactez l'administrateur."));
    }
}
