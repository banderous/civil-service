package uk.gov.hmcts.reform.civil.handler.callback.user;

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
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.bs.BreathingSpaceState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;

@Service
@RequiredArgsConstructor
@Slf4j
public class BreathingSpaceEnterCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(CaseEvent.BREATHING_SPACE_ENTER);

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_START), this::aboutToStart,
            callbackKey(ABOUT_TO_SUBMIT), this::aboutToSubmit,
            callbackKey(SUBMITTED), this::submitted
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    CallbackResponse aboutToStart(CallbackParams params) {
        AboutToStartOrSubmitCallbackResponse.AboutToStartOrSubmitCallbackResponseBuilder responseBuilder = AboutToStartOrSubmitCallbackResponse.builder();
        List<String> errors = new ArrayList<>();

        CaseData caseData = params.getCaseData();

        canEnterBreathingState(caseData).ifPresent(errors::add);
        if (!errors.isEmpty()) {
            return responseBuilder.errors(errors).build();
        }

        return responseBuilder.build();
    }

    CallbackResponse aboutToSubmit(CallbackParams params) {
        AboutToStartOrSubmitCallbackResponse.AboutToStartOrSubmitCallbackResponseBuilder responseBuilder = AboutToStartOrSubmitCallbackResponse.builder();
        List<String> errors = new ArrayList<>();

        CaseData caseData = params.getCaseData();

        canEnterBreathingState(caseData).ifPresent(errors::add);
        if (!errors.isEmpty()) {
            return responseBuilder.errors(errors).build();
        } else {
            // TODO set BreathingSpace state to ENTERED and replace caseData in responseBuilder
        }

        return responseBuilder.build();
    }

    /**
     * State constraints are assumed to be checked at frontend
     *
     * <p>Assumes that front does nothing with state, that BS can't be multiple and that state will
     * be changed to "ENTERED" or "LIFTED" only through backend submit handlers.</p>
     *
     * @param caseData the case data.
     * @return an empty optional for true, a descriptive error message otherwise.
     */
    private Optional<String> canEnterBreathingState(CaseData caseData) {
        if (caseData.getBreathingSpace() != null && caseData.getBreathingSpace().getState() != null) {
            if (caseData.getBreathingSpace().getState() == BreathingSpaceState.ENTERED) {
                return Optional.of("Applicant has already entered breathing space");
            } else if (caseData.getBreathingSpace().getState() == BreathingSpaceState.LIFTED) {
                // TODO check if user can enter BS multiple times
                return Optional.of("Can't enter breathing space twice");
            } else {
                log.error(caseData.getCcdCaseReference()
                              + " Breathing Space Status unknown, prevents entering Breathing Space");
                return Optional.of("Can't enter breathing space");
            }
        } else {
            return Optional.empty();
        }
    }

    CallbackResponse submitted(CallbackParams params) {
        /*
        TODO
        notify defendant.
        If user is caseworker, notify applicant
        notifications can be done through Spring ApplicationEventListeners to avoid making the user wait

        check confirmation message
         */

        return SubmittedCallbackResponse.builder()
            .confirmationHeader("You have entered breathing space")
            .confirmationBody("<br />")
            .build();
    }
}
