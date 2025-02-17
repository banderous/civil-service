package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.ExitSurveyContentService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CREATE_CLAIM;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CREATE_CLAIM_SPEC;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESUBMIT_CLAIM;
import static uk.gov.hmcts.reform.civil.enums.SuperClaimType.SPEC_CLAIM;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResubmitClaimCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(RESUBMIT_CLAIM);

    private final ExitSurveyContentService exitSurveyContentService;
    private final ObjectMapper objectMapper;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_START), this::emptyCallbackResponse,
            callbackKey(ABOUT_TO_SUBMIT), this::aboutToSubmit,
            callbackKey(SUBMITTED), this::buildConfirmation
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse aboutToSubmit(CallbackParams callbackParams) {
        if ((callbackParams.getRequest().getEventId() != null
            && callbackParams.getRequest().getEventId().equals("CREATE_CLAIM_SPEC"))
            || (callbackParams.getCaseData().getSuperClaimType() != null
            && callbackParams.getCaseData().getSuperClaimType().equals(SPEC_CLAIM))) {
            CaseData caseDataUpdated = callbackParams.getCaseData().toBuilder()
                .businessProcess(BusinessProcess.ready(CREATE_CLAIM_SPEC))
                .build();
            return AboutToStartOrSubmitCallbackResponse.builder()
                .data(caseDataUpdated.toMap(objectMapper))
                .build();
        } else {
            CaseData caseDataUpdated = callbackParams.getCaseData().toBuilder()
                .businessProcess(BusinessProcess.ready(CREATE_CLAIM))
                .build();
            return AboutToStartOrSubmitCallbackResponse.builder()
                .data(caseDataUpdated.toMap(objectMapper))
                .build();
        }
    }

    private SubmittedCallbackResponse buildConfirmation(CallbackParams callbackParams) {
        return SubmittedCallbackResponse.builder()
            .confirmationHeader("# Claim pending")
            .confirmationBody(
                "<br />Your claim will be processed. Wait for us to contact you."
                    + exitSurveyContentService.applicantSurvey()
            )
            .build();
    }
}
