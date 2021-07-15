package uk.gov.hmcts.reform.ordnancesurvey.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder()
public class Dpa {

    private final Address dpa;

    @JsonCreator
    public Dpa (@JsonProperty("DPA") Address dpa) {
        this.dpa = dpa;
    }
}
