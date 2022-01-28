package uk.gov.hmcts.reform.civil.handler.callback.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;

@Service
@RequiredArgsConstructor
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

        /*
        TODO check if the case can enter breathing space.
        State conditions should be in ccd-definition,
        but we need to check here that there is not already a breathing space

        if (claim already in breathingSpace) {
          errors.add "Can't enter breathing space. Claim is already there."
        }
         */
        if (!errors.isEmpty()) {
            responseBuilder.errors(errors);
        }

        return responseBuilder.build();
    }

    CallbackResponse aboutToSubmit(CallbackParams params) {
        AboutToStartOrSubmitCallbackResponse.AboutToStartOrSubmitCallbackResponseBuilder responseBuilder = AboutToStartOrSubmitCallbackResponse.builder();
        List<String> errors = new ArrayList<>();

        /*
        TODO check if the case can enter breathing space.
        State conditions should be in ccd-definition,
        but we need to check here that there is not already a breathing space
        JIC, so that the caseworker and the applicant are in two different computers at the same time
         */
        if (!errors.isEmpty()) {
            responseBuilder.errors(errors);
        }

        return responseBuilder.build();
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
