package ga.uob.iam.local.compte.infrastructure.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO de désérialisation du payload Kafka IAM Central.
 * Topic : iam.central.compte.mis_a_jour
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IamCentralCompteMisAJourPayload(

    @JsonProperty("user_id_national")
    String userIdNational,

    @JsonProperty("nom")
    String nom,

    @JsonProperty("prenom")
    String prenom,

    @JsonProperty("email")
    String email,

    @JsonProperty("type_profil_suggere")
    String typeProfilSuggere,

    @JsonProperty("contexte_scolaire")
    IamCentralCompteCreatedPayload.ContexteScolairePayload contexteScolaire,

    @JsonProperty("version_hash")
    String versionHash

) {}
