package uk.gov.hmcts.reform.civil.handler.callback.camunda.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.NotificationService;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NOTIFY_RESPONDENT_SOLICITOR1_FOR_CLAIMANT_CONFIRMS_TO_PROCEED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NOTIFY_RESPONDENT_SOLICITOR1_FOR_CLAIMANT_CONFIRMS_TO_PROCEED_CC;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NOTIFY_RESPONDENT_SOLICITOR2_FOR_CLAIMANT_CONFIRMS_TO_PROCEED;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.utils.PartyUtils.buildPartiesReferences;

@Service
@RequiredArgsConstructor
public class ClaimantResponseConfirmsToProceedRespondentNotificationHandler extends CallbackHandler
    implements NotificationData {

    private static final List<CaseEvent> EVENTS = List.of(
        NOTIFY_RESPONDENT_SOLICITOR1_FOR_CLAIMANT_CONFIRMS_TO_PROCEED,
        NOTIFY_RESPONDENT_SOLICITOR2_FOR_CLAIMANT_CONFIRMS_TO_PROCEED,
        NOTIFY_RESPONDENT_SOLICITOR1_FOR_CLAIMANT_CONFIRMS_TO_PROCEED_CC);

    public static final String TASK_ID = "ClaimantConfirmsToProceedNotifyRespondentSolicitor1";
    public static final String Task_ID_RESPONDENT_SOL2 = "ClaimantConfirmsToProceedNotifyRespondentSolicitor2";
    public static final String TASK_ID_CC = "ClaimantConfirmsToProceedNotifyApplicantSolicitor1CC";
    private static final String REFERENCE_TEMPLATE = "claimant-confirms-to-proceed-respondent-notification-%s";
    private static final String NP_PROCEED_REFERENCE_TEMPLATE
        = "claimant-confirms-not-to-proceed-respondent-notification-%s";

    private final NotificationService notificationService;
    private final NotificationsProperties notificationsProperties;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::notifyRespondentSolicitorForClaimantConfirmsToProceed
        );
    }

    @Override
    public String camundaActivityId(CallbackParams callbackParams) {
        if (isRespondentSolicitor2Notification(callbackParams)) {
            return Task_ID_RESPONDENT_SOL2;
        }
        return isCcNotification(callbackParams) ? TASK_ID_CC : TASK_ID;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse notifyRespondentSolicitorForClaimantConfirmsToProceed(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        var recipient = isCcNotification(callbackParams)
            ? caseData.getApplicantSolicitor1UserDetails().getEmail()
            : caseData.getRespondentSolicitor1EmailAddress();

        if (isRespondentSolicitor2Notification(callbackParams)) {
            recipient = caseData.getRespondentSolicitor2EmailAddress();
        }

        if ((isRespondentSolicitor2Notification(callbackParams)
            && NO.equals(caseData.getApplicant1ProceedWithClaimAgainstRespondent2MultiParty1v2()))
            || (!isRespondentSolicitor2Notification(callbackParams)
            && NO.equals(caseData.getApplicant1ProceedWithClaimAgainstRespondent1MultiParty1v2()))) {
            notificationService.sendMail(
                recipient,
                notificationsProperties.getClaimantSolicitorConfirmsNotToProceed(),
                addProperties(caseData),
                String.format(NP_PROCEED_REFERENCE_TEMPLATE, caseData.getLegacyCaseReference())
            );
            return AboutToStartOrSubmitCallbackResponse.builder().build();
        }

        notificationService.sendMail(
            recipient,
            notificationsProperties.getClaimantSolicitorConfirmsToProceed(),
            addProperties(caseData),
            String.format(REFERENCE_TEMPLATE, caseData.getLegacyCaseReference())
        );
        return AboutToStartOrSubmitCallbackResponse.builder().build();
    }

    @Override
    public Map<String, String> addProperties(CaseData caseData) {
        return Map.of(
            CLAIM_REFERENCE_NUMBER, caseData.getLegacyCaseReference(),
            PARTY_REFERENCES, buildPartiesReferences(caseData)
        );
    }

    private boolean isCcNotification(CallbackParams callbackParams) {
        return callbackParams.getRequest().getEventId()
            .equals(NOTIFY_RESPONDENT_SOLICITOR1_FOR_CLAIMANT_CONFIRMS_TO_PROCEED_CC.name());
    }

    private boolean isRespondentSolicitor2Notification(CallbackParams callbackParams) {
        return callbackParams.getRequest().getEventId()
            .equals(NOTIFY_RESPONDENT_SOLICITOR2_FOR_CLAIMANT_CONFIRMS_TO_PROCEED.name());
    }
}
