package uk.gov.hmcts.reform.ordnancesurvey.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder()
public class Address {

    @JsonProperty("ORGANISATION_NAME")
    private String organisationName;

    @JsonProperty("BUILDING_NAME")
    private String buildingName;

    @JsonProperty("BUILDING_NUMBER")
    private String buildingNumber;

    @JsonProperty("THOROUGHFARE_NAME")
    private String thoroughfareName;

    @JsonProperty("POST_TOWN")
    private String postTown;

    @JsonProperty("POSTCODE")
    private String postCode;
}
