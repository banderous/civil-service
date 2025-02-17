package uk.gov.hmcts.reform.civil.validation.interfaces;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.model.dq.DQ;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

public interface WitnessesValidator {

    default CallbackResponse validateWitnesses(DQ dq) {
        var experts = dq.getWitnesses();
        List<String> errors = new ArrayList<>();
        if (experts.getWitnessesToAppear() == YES && experts.getDetails() == null) {
            errors.add("Witness details required");
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .build();
    }
}
