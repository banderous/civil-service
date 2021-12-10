package uk.gov.hmcts.reform.civil.model.genapp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;

import java.util.List;

@Setter
@Data
@Builder(toBuilder = true)
public class ApplicationType {

    private final List<GeneralApplicationTypes> childApplicationType;

    @JsonCreator
    ApplicationType(@JsonProperty("childApplicationType") List<GeneralApplicationTypes> childApplicationType) {
        this.childApplicationType = childApplicationType;
    }
}
