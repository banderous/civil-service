package uk.gov.hmcts.reform.civil.validation.interfaces;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.model.dq.DQ;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

public interface ExpertsValidator {

    default CallbackResponse validateExperts(DQ dq) {
        var experts = dq.getExperts();
        List<String> errors = new ArrayList<>();
        if (experts.getExpertRequired() == YES && experts.getDetails() == null) {
            errors.add("Expert details required");
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .build();
    }
}
