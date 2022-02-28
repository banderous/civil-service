package uk.gov.hmcts.reform.civil.handler.callback.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.GetCaseCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.metadatafields.CaseViewField;
import uk.gov.hmcts.reform.ccd.client.model.metadatafields.definition.FieldTypeDefinition;
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
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        caseViewField.setId("[INJECTED_DATA.totalClaimAmount]");
        caseViewField.setValue("12333");
        caseViewField.setMetadata(true);
        fieldTypeDefinition.setId("Text");
        fieldTypeDefinition.setType("Label");
        caseViewField.setFieldTypeDefinition(fieldTypeDefinition);
        List<CaseViewField> caseViewFields = new ArrayList<>();
        caseViewFields.add(caseViewField);

        getCaseCallbackResponse.setMetadataFields(caseViewFields);

        return getCaseCallbackResponse;
    }
}
