package uk.gov.hmcts.reform.civil.callback;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static uk.gov.hmcts.reform.civil.callback.UserType.CAMUNDA;
import static uk.gov.hmcts.reform.civil.callback.UserType.TESTING_SUPPORT;
import static uk.gov.hmcts.reform.civil.callback.UserType.USER;

@Getter
@RequiredArgsConstructor
public enum CaseEvent {
    CREATE_CLAIM(USER),
    NOTIFY_DEFENDANT_OF_CLAIM(USER),
    NOTIFY_DEFENDANT_OF_CLAIM_DETAILS(USER),
    ADD_OR_AMEND_CLAIM_DOCUMENTS(USER),
    ACKNOWLEDGE_CLAIM(USER),
    ADD_DEFENDANT_LITIGATION_FRIEND(USER),
    INFORM_AGREED_EXTENSION_DATE(USER),
    DEFENDANT_RESPONSE(USER),
    CLAIMANT_RESPONSE(USER),
    WITHDRAW_CLAIM(USER),
    DISCONTINUE_CLAIM(USER),
    DISMISS_CLAIM(USER),
    CASE_PROCEEDS_IN_CASEMAN(USER),
    RESUBMIT_CLAIM(USER),
    AMEND_PARTY_DETAILS(USER),
    TAKE_CASE_OFFLINE(USER),
    ADD_CASE_NOTE(USER),

    ASSIGN_CASE_TO_APPLICANT_SOLICITOR1(CAMUNDA),
    NOTIFY_RESPONDENT_SOLICITOR1_FOR_CLAIM_ISSUE(CAMUNDA),
    NOTIFY_RESPONDENT_SOLICITOR2_FOR_CLAIM_ISSUE(CAMUNDA),
    NOTIFY_RESPONDENT_SOLICITOR1_FOR_CLAIM_ISSUE_CC(CAMUNDA),
    NOTIFY_RESPONDENT_SOLICITOR1_FOR_CLAIM_DETAILS(CAMUNDA),
    NOTIFY_RESPONDENT_SOLICITOR2_FOR_CLAIM_DETAILS(CAMUNDA),
    NOTIFY_RESPONDENT_SOLICITOR1_FOR_CLAIM_DETAILS_CC(CAMUNDA),
    NOTIFY_APPLICANT_SOLICITOR1_FOR_REQUEST_FOR_EXTENSION(CAMUNDA),
    NOTIFY_RESPONDENT_SOLICITOR1_FOR_EXTENSION_RESPONSE(CAMUNDA),
    NOTIFY_APPLICANT_SOLICITOR1_FOR_CLAIM_ACKNOWLEDGEMENT(CAMUNDA),
    NOTIFY_APPLICANT_SOLICITOR1_FOR_CLAIM_ACKNOWLEDGEMENT_CC(CAMUNDA),
    NOTIFY_APPLICANT_SOLICITOR1_FOR_DEFENDANT_RESPONSE(CAMUNDA),
    NOTIFY_APPLICANT_SOLICITOR1_FOR_DEFENDANT_RESPONSE_CC(CAMUNDA),
    NOTIFY_RESPONDENT_SOLICITOR1_FOR_CASE_HANDED_OFFLINE(CAMUNDA),
    NOTIFY_APPLICANT_SOLICITOR1_FOR_CASE_HANDED_OFFLINE(CAMUNDA),
    NOTIFY_APPLICANT_SOLICITOR1_FOR_FAILED_PAYMENT(CAMUNDA),
    NOTIFY_APPLICANT_SOLICITOR1_FOR_RESPONDENT_LITIGANT_IN_PERSON(CAMUNDA),
    NOTIFY_APPLICANT_SOLICITOR1_FOR_AGREED_EXTENSION_DATE(CAMUNDA),
    NOTIFY_APPLICANT_SOLICITOR1_FOR_AGREED_EXTENSION_DATE_CC(CAMUNDA),
    NOTIFY_APPLICANT_SOLICITOR1_FOR_CLAIM_CONTINUING_ONLINE(CAMUNDA),
    NOTIFY_RESPONDENT_SOLICITOR1_FOR_CLAIMANT_CONFIRMS_TO_PROCEED(CAMUNDA),
    NOTIFY_RESPONDENT_SOLICITOR1_FOR_CLAIMANT_CONFIRMS_TO_PROCEED_CC(CAMUNDA),
    NOTIFY_RESPONDENT_SOLICITOR1_FOR_CLAIMANT_CONFIRMS_NOT_TO_PROCEED(CAMUNDA),
    NOTIFY_RESPONDENT_SOLICITOR1_FOR_CLAIMANT_CONFIRMS_NOT_TO_PROCEED_CC(CAMUNDA),
    NOTIFY_RESPONDENT_SOLICITOR1_FOR_CASE_TAKEN_OFFLINE(CAMUNDA),
    NOTIFY_APPLICANT_SOLICITOR1_FOR_CASE_TAKEN_OFFLINE(CAMUNDA),
    NOTIFY_RESPONDENT_SOLICITOR1_FOR_LITIGATION_FRIEND_ADDED(CAMUNDA),
    NOTIFY_APPLICANT_SOLICITOR1_FOR_LITIGATION_FRIEND_ADDED(CAMUNDA),
    NOTIFY_RESPONDENT_SOLICITOR1_FOR_CASE_PROCEEDS_IN_CASEMAN(CAMUNDA),
    NOTIFY_APPLICANT_SOLICITOR1_FOR_CASE_PROCEEDS_IN_CASEMAN(CAMUNDA),
    NOTIFY_APPLICANT_SOLICITOR1_FOR_CLAIM_DISMISSED(CAMUNDA),
    NOTIFY_RESPONDENT_SOLICITOR1_FOR_CLAIM_DISMISSED(CAMUNDA),

    DISPATCH_BUSINESS_PROCESS(CAMUNDA),
    START_BUSINESS_PROCESS(CAMUNDA),
    END_BUSINESS_PROCESS(CAMUNDA),

    MAKE_PBA_PAYMENT(CAMUNDA),
    VALIDATE_FEE(CAMUNDA),

    GENERATE_CLAIM_FORM(CAMUNDA),

    PROCESS_FULL_DEFENCE(CAMUNDA),
    PROCESS_CLAIM_ISSUE(CAMUNDA),
    PROCESS_PAYMENT_FAILED(CAMUNDA),

    PROCEEDS_IN_HERITAGE_SYSTEM(CAMUNDA),
    GENERATE_ACKNOWLEDGEMENT_OF_CLAIM(CAMUNDA),
    GENERATE_DIRECTIONS_QUESTIONNAIRE(CAMUNDA),
    NOTIFY_RPA_ON_CASE_HANDED_OFFLINE(CAMUNDA),
    NOTIFY_RPA_ON_CONTINUOUS_FEED(CAMUNDA),
    RETRY_NOTIFY_RPA_ON_CASE_HANDED_OFFLINE(CAMUNDA),
    RESET_RPA_NOTIFICATION_BUSINESS_PROCESS(CAMUNDA),

    UPDATE_CASE_DATA(TESTING_SUPPORT);

    private final UserType userType;

    public boolean isCamundaEvent() {
        return this.getUserType() == CAMUNDA;
    }
}
