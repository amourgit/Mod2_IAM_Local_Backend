package ga.uob.iam.local.keycloak.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Corps de requête pour les opérations nécessitant uniquement un motif.
 * Utilisé pour : suspendre, réactiver.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MotifRepresentation {

    private String motif;

    public MotifRepresentation() {}

    public String getMotif()       { return motif; }
    public void setMotif(String v) { this.motif = v; }
}
