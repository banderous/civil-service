package uk.gov.hmcts.reform.civil.handler.callback.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.GetCaseCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.metadatafields.CaseViewField;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCaseUrlCallbackHandler {

    public GetCaseCallbackResponse getMetaData(CallbackParams callbackParams) {
        GetCaseCallbackResponse getCaseCallbackResponse
            = new GetCaseCallbackResponse();
        CaseViewField caseViewField = new CaseViewField();
        caseViewField.setId("[INJECTED_DATA.totalClaimAmount]");
        caseViewField.setValue("200");
        caseViewField.setMetadata(true);
        List<CaseViewField> caseViewFields = new ArrayList<>();
        caseViewFields.add(caseViewField);

        getCaseCallbackResponse.setMetadataFields(caseViewFields);

        return getCaseCallbackResponse;
    }
}
