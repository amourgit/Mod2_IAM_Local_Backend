package ga.uob.iam.local.compte.infrastructure.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO de désérialisation du payload Kafka IAM Central.
 * Topic : iam.central.compte.cree
 *
 * @JsonIgnoreProperties(ignoreUnknown = true) : on ignore les champs
 * inconnus pour la rétrocompatibilité avec les futures versions du payload.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IamCentralCompteCreatedPayload(

    @JsonProperty("user_id_national")
    String userIdNational,

    @JsonProperty("identifiant_national")
    String identifiantNational,

    @JsonProperty("nom")
    String nom,

    @JsonProperty("prenom")
    String prenom,

    @JsonProperty("email")
    String email,

    @JsonProperty("etablissement_code")
    String etablissementCode,

    @JsonProperty("type_profil_suggere")
    String typeProfilSuggere,

    @JsonProperty("contexte_scolaire")
    ContexteScolairePayload contexteScolaire

) {
    /** Sous-objet contexte scolaire dans le payload JSON. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContexteScolairePayload(
        @JsonProperty("filiere")   String filiere,
        @JsonProperty("composante") String composante
    ) {}
}
