package uk.gov.hmcts.reform.civil.service.flowstate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.stateflow.StateFlow;
import uk.gov.hmcts.reform.civil.stateflow.StateFlowBuilder;
import uk.gov.hmcts.reform.civil.stateflow.model.State;

import java.util.Map;

import static java.util.function.Predicate.not;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.applicantOutOfTime;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.applicantOutOfTimeProcessedByCamunda;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.caseDismissedAfterClaimAcknowledged;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.caseDismissedAfterClaimAcknowledgedExtension;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.caseDismissedAfterDetailNotified;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.caseDismissedAfterDetailNotifiedExtension;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.claimDetailsNotified;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.claimDismissedByCamunda;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.claimIssued;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.claimNotified;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.claimSubmittedOneRespondentRepresentative;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.claimSubmittedTwoRespondentRepresentatives;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.counterClaim;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.fullAdmission;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.fullDefence;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.fullDefenceNotProceed;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.fullDefenceProceed;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.notificationAcknowledged;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.partAdmission;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.pastClaimDetailsNotificationDeadline;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.pastClaimNotificationDeadline;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.paymentFailed;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.paymentSuccessful;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.pendingClaimIssued;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.respondent1NotRepresented;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.respondent1OrgNotRegistered;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.respondent1TimeExtension;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.takenOfflineByStaff;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.takenOfflineByStaffAfterClaimDetailsNotified;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.takenOfflineByStaffAfterClaimDetailsNotifiedExtension;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.takenOfflineByStaffAfterClaimIssue;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.takenOfflineByStaffAfterClaimNotified;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.takenOfflineByStaffAfterNotificationAcknowledged;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.takenOfflineByStaffAfterNotificationAcknowledgedTimeExtension;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.takenOfflineBySystem;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.CLAIM_DETAILS_NOTIFIED;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.CLAIM_DETAILS_NOTIFIED_TIME_EXTENSION;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.CLAIM_DISMISSED_PAST_CLAIM_DETAILS_NOTIFICATION_DEADLINE;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.CLAIM_DISMISSED_PAST_CLAIM_DISMISSED_DEADLINE;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.CLAIM_DISMISSED_PAST_CLAIM_NOTIFICATION_DEADLINE;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.CLAIM_ISSUED;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.CLAIM_ISSUED_PAYMENT_FAILED;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.CLAIM_ISSUED_PAYMENT_SUCCESSFUL;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.CLAIM_NOTIFIED;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.CLAIM_SUBMITTED;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.COUNTER_CLAIM;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.DRAFT;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.FLOW_NAME;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.FULL_ADMISSION;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.FULL_DEFENCE;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.FULL_DEFENCE_NOT_PROCEED;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.FULL_DEFENCE_PROCEED;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.NOTIFICATION_ACKNOWLEDGED;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.NOTIFICATION_ACKNOWLEDGED_TIME_EXTENSION;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.PART_ADMISSION;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.PAST_APPLICANT_RESPONSE_DEADLINE_AWAITING_CAMUNDA;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.PAST_CLAIM_DETAILS_NOTIFICATION_DEADLINE_AWAITING_CAMUNDA;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.PAST_CLAIM_DISMISSED_DEADLINE_AWAITING_CAMUNDA;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.PAST_CLAIM_NOTIFICATION_DEADLINE_AWAITING_CAMUNDA;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.PENDING_CLAIM_ISSUED;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.PENDING_CLAIM_ISSUED_UNREGISTERED_DEFENDANT;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.PENDING_CLAIM_ISSUED_UNREPRESENTED_DEFENDANT;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.SPEC_DRAFT;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.TAKEN_OFFLINE_BY_STAFF;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.TAKEN_OFFLINE_PAST_APPLICANT_RESPONSE_DEADLINE;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.TAKEN_OFFLINE_UNREGISTERED_DEFENDANT;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.TAKEN_OFFLINE_UNREPRESENTED_DEFENDANT;

@Component
@RequiredArgsConstructor
public class StateFlowEngine {

    private final CaseDetailsConverter caseDetailsConverter;
    private final FeatureToggleService featureToggleService;

    public StateFlow build() {
        return StateFlowBuilder.<FlowState.Main>flow(FLOW_NAME)
            .initial(DRAFT)
                .transitionTo(CLAIM_SUBMITTED).onlyIf(claimSubmittedOneRespondentRepresentative)
                    .set(flags -> flags.putAll(Map.of(FlowFlag.ONE_RESPONDENT_REPRESENTATIVE.name(), true,
                        FlowFlag.RPA_CONTINUOUS_FEED.name(), featureToggleService.isRpaContinuousFeedEnabled())))
                .transitionTo(CLAIM_SUBMITTED).onlyIf(claimSubmittedTwoRespondentRepresentatives)
                    .set(flags -> flags.putAll(Map.of(FlowFlag.TWO_RESPONDENT_REPRESENTATIVES.name(), true,
                        FlowFlag.RPA_CONTINUOUS_FEED.name(), featureToggleService.isRpaContinuousFeedEnabled())))
            .state(CLAIM_SUBMITTED)
                .transitionTo(CLAIM_ISSUED_PAYMENT_SUCCESSFUL).onlyIf(paymentSuccessful)
                .transitionTo(CLAIM_ISSUED_PAYMENT_FAILED).onlyIf(paymentFailed)
            .state(CLAIM_ISSUED_PAYMENT_FAILED)
                .transitionTo(CLAIM_ISSUED_PAYMENT_SUCCESSFUL).onlyIf(paymentSuccessful)
            .state(CLAIM_ISSUED_PAYMENT_SUCCESSFUL)
                .transitionTo(PENDING_CLAIM_ISSUED).onlyIf(pendingClaimIssued)
                .transitionTo(PENDING_CLAIM_ISSUED_UNREPRESENTED_DEFENDANT).onlyIf(respondent1NotRepresented)
                .transitionTo(PENDING_CLAIM_ISSUED_UNREGISTERED_DEFENDANT).onlyIf(respondent1OrgNotRegistered)
            .state(PENDING_CLAIM_ISSUED)
                .transitionTo(CLAIM_ISSUED).onlyIf(claimIssued)
            .state(PENDING_CLAIM_ISSUED_UNREPRESENTED_DEFENDANT)
                .transitionTo(TAKEN_OFFLINE_UNREPRESENTED_DEFENDANT).onlyIf(takenOfflineBySystem)
            .state(PENDING_CLAIM_ISSUED_UNREGISTERED_DEFENDANT)
                .transitionTo(TAKEN_OFFLINE_UNREGISTERED_DEFENDANT).onlyIf(takenOfflineBySystem)
            .state(CLAIM_ISSUED)
                .transitionTo(CLAIM_NOTIFIED).onlyIf(claimNotified)
                .transitionTo(TAKEN_OFFLINE_BY_STAFF).onlyIf(takenOfflineByStaffAfterClaimIssue)
                .transitionTo(PAST_CLAIM_NOTIFICATION_DEADLINE_AWAITING_CAMUNDA).onlyIf(pastClaimNotificationDeadline)
            .state(CLAIM_NOTIFIED)
                .transitionTo(CLAIM_DETAILS_NOTIFIED).onlyIf(claimDetailsNotified)
                .transitionTo(TAKEN_OFFLINE_BY_STAFF).onlyIf(takenOfflineByStaffAfterClaimNotified)
                .transitionTo(PAST_CLAIM_DETAILS_NOTIFICATION_DEADLINE_AWAITING_CAMUNDA)
                    .onlyIf(pastClaimDetailsNotificationDeadline)
            .state(CLAIM_DETAILS_NOTIFIED)
                .transitionTo(CLAIM_DETAILS_NOTIFIED_TIME_EXTENSION)
                    .onlyIf(respondent1TimeExtension.and(not(notificationAcknowledged)))
                .transitionTo(NOTIFICATION_ACKNOWLEDGED).onlyIf(notificationAcknowledged)
                .transitionTo(FULL_DEFENCE)
                    .onlyIf(fullDefence.and(not(notificationAcknowledged.or(respondent1TimeExtension))))
                .transitionTo(FULL_ADMISSION)
                    .onlyIf(fullAdmission.and(not(notificationAcknowledged.or(respondent1TimeExtension))))
                .transitionTo(PART_ADMISSION)
                    .onlyIf(partAdmission.and(not(notificationAcknowledged.or(respondent1TimeExtension))))
                .transitionTo(COUNTER_CLAIM)
                    .onlyIf(counterClaim.and(not(notificationAcknowledged.or(respondent1TimeExtension))))
                .transitionTo(TAKEN_OFFLINE_BY_STAFF).onlyIf(takenOfflineByStaffAfterClaimDetailsNotified)
                .transitionTo(PAST_CLAIM_DISMISSED_DEADLINE_AWAITING_CAMUNDA).onlyIf(caseDismissedAfterDetailNotified)
            .state(CLAIM_DETAILS_NOTIFIED_TIME_EXTENSION)
                .transitionTo(NOTIFICATION_ACKNOWLEDGED).onlyIf(notificationAcknowledged)
                .transitionTo(FULL_DEFENCE)
                    .onlyIf(respondent1TimeExtension.and(not(notificationAcknowledged)).and(fullDefence))
                .transitionTo(FULL_ADMISSION)
                    .onlyIf(respondent1TimeExtension.and(not(notificationAcknowledged)).and(fullAdmission))
                .transitionTo(PART_ADMISSION)
                    .onlyIf(respondent1TimeExtension.and(not(notificationAcknowledged)).and(partAdmission))
                .transitionTo(COUNTER_CLAIM)
                    .onlyIf(respondent1TimeExtension.and(not(notificationAcknowledged)).and(counterClaim))
                .transitionTo(TAKEN_OFFLINE_BY_STAFF).onlyIf(takenOfflineByStaffAfterClaimDetailsNotifiedExtension)
                .transitionTo(PAST_CLAIM_DISMISSED_DEADLINE_AWAITING_CAMUNDA)
                    .onlyIf(caseDismissedAfterDetailNotifiedExtension)
            .state(NOTIFICATION_ACKNOWLEDGED)
                .transitionTo(NOTIFICATION_ACKNOWLEDGED_TIME_EXTENSION)
                    .onlyIf(notificationAcknowledged.and(respondent1TimeExtension))
                .transitionTo(FULL_DEFENCE)
                    .onlyIf(notificationAcknowledged.and(not(respondent1TimeExtension)).and(fullDefence))
                .transitionTo(FULL_ADMISSION)
                    .onlyIf(notificationAcknowledged.and(not(respondent1TimeExtension)).and(fullAdmission))
                .transitionTo(PART_ADMISSION)
                    .onlyIf(notificationAcknowledged.and(not(respondent1TimeExtension)).and(partAdmission))
                .transitionTo(COUNTER_CLAIM)
                    .onlyIf(notificationAcknowledged.and(not(respondent1TimeExtension)).and(counterClaim))
                .transitionTo(TAKEN_OFFLINE_BY_STAFF)
                    .onlyIf(takenOfflineByStaffAfterNotificationAcknowledged)
                .transitionTo(PAST_CLAIM_DISMISSED_DEADLINE_AWAITING_CAMUNDA)
                    .onlyIf(caseDismissedAfterClaimAcknowledged)
            .state(NOTIFICATION_ACKNOWLEDGED_TIME_EXTENSION)
                .transitionTo(FULL_DEFENCE)
                    .onlyIf(respondent1TimeExtension.and(notificationAcknowledged).and(fullDefence))
                .transitionTo(FULL_ADMISSION)
                    .onlyIf(respondent1TimeExtension.and(notificationAcknowledged).and(fullAdmission))
                .transitionTo(PART_ADMISSION)
                    .onlyIf(respondent1TimeExtension.and(notificationAcknowledged).and(partAdmission))
                .transitionTo(COUNTER_CLAIM)
                    .onlyIf(respondent1TimeExtension.and(notificationAcknowledged).and(counterClaim))
                .transitionTo(TAKEN_OFFLINE_BY_STAFF)
                    .onlyIf(takenOfflineByStaffAfterNotificationAcknowledgedTimeExtension)
                .transitionTo(PAST_CLAIM_DISMISSED_DEADLINE_AWAITING_CAMUNDA)
                    .onlyIf(caseDismissedAfterClaimAcknowledgedExtension)
            .state(FULL_DEFENCE)
                .transitionTo(FULL_DEFENCE_PROCEED).onlyIf(fullDefenceProceed)
                .transitionTo(FULL_DEFENCE_NOT_PROCEED).onlyIf(fullDefenceNotProceed)
                .transitionTo(TAKEN_OFFLINE_BY_STAFF).onlyIf(takenOfflineByStaff)
                .transitionTo(PAST_APPLICANT_RESPONSE_DEADLINE_AWAITING_CAMUNDA)
                    .onlyIf(applicantOutOfTime)
            .state(PAST_CLAIM_NOTIFICATION_DEADLINE_AWAITING_CAMUNDA)
                .transitionTo(CLAIM_DISMISSED_PAST_CLAIM_NOTIFICATION_DEADLINE).onlyIf(claimDismissedByCamunda)
            .state(CLAIM_DISMISSED_PAST_CLAIM_NOTIFICATION_DEADLINE)
                .transitionTo(TAKEN_OFFLINE_BY_STAFF).onlyIf(takenOfflineByStaff)
            .state(PAST_APPLICANT_RESPONSE_DEADLINE_AWAITING_CAMUNDA)
                .transitionTo(TAKEN_OFFLINE_PAST_APPLICANT_RESPONSE_DEADLINE)
                    .onlyIf(applicantOutOfTimeProcessedByCamunda)
            .state(PAST_CLAIM_DETAILS_NOTIFICATION_DEADLINE_AWAITING_CAMUNDA)
                .transitionTo(CLAIM_DISMISSED_PAST_CLAIM_DETAILS_NOTIFICATION_DEADLINE).onlyIf(claimDismissedByCamunda)
            .state(CLAIM_DISMISSED_PAST_CLAIM_DETAILS_NOTIFICATION_DEADLINE)
                .transitionTo(TAKEN_OFFLINE_BY_STAFF).onlyIf(takenOfflineByStaff)
            .state(PAST_CLAIM_DISMISSED_DEADLINE_AWAITING_CAMUNDA)
                .transitionTo(CLAIM_DISMISSED_PAST_CLAIM_DISMISSED_DEADLINE).onlyIf(claimDismissedByCamunda)
            .state(CLAIM_DISMISSED_PAST_CLAIM_DISMISSED_DEADLINE)
            .state(FULL_ADMISSION)
            .state(PART_ADMISSION)
            .state(COUNTER_CLAIM)
            .state(FULL_DEFENCE_PROCEED)
            .state(FULL_DEFENCE_NOT_PROCEED)
            .state(TAKEN_OFFLINE_BY_STAFF)
            .state(PENDING_CLAIM_ISSUED_UNREPRESENTED_DEFENDANT)
            .state(PENDING_CLAIM_ISSUED_UNREGISTERED_DEFENDANT)
            .state(TAKEN_OFFLINE_UNREGISTERED_DEFENDANT)
            .state(TAKEN_OFFLINE_UNREPRESENTED_DEFENDANT)
            .state(TAKEN_OFFLINE_PAST_APPLICANT_RESPONSE_DEADLINE)
            .build();
    }

    public StateFlow buildSpec() {
        return StateFlowBuilder.<FlowState.Main>flow(FLOW_NAME)
            .initial(SPEC_DRAFT)
                .transitionTo(CLAIM_SUBMITTED).onlyIf(claimSubmittedOneRespondentRepresentative)
            .state(CLAIM_SUBMITTED)
                .transitionTo(CLAIM_ISSUED_PAYMENT_SUCCESSFUL).onlyIf(paymentSuccessful)
                .transitionTo(CLAIM_ISSUED_PAYMENT_FAILED).onlyIf(paymentFailed)
            .state(CLAIM_ISSUED_PAYMENT_FAILED)
                .transitionTo(CLAIM_ISSUED_PAYMENT_SUCCESSFUL).onlyIf(paymentSuccessful)
            .state(CLAIM_ISSUED_PAYMENT_SUCCESSFUL)
                .transitionTo(PENDING_CLAIM_ISSUED).onlyIf(pendingClaimIssued)
                .transitionTo(PENDING_CLAIM_ISSUED_UNREPRESENTED_DEFENDANT).onlyIf(respondent1NotRepresented)
                .transitionTo(PENDING_CLAIM_ISSUED_UNREGISTERED_DEFENDANT).onlyIf(respondent1OrgNotRegistered)
            .state(PENDING_CLAIM_ISSUED)
                .transitionTo(CLAIM_ISSUED).onlyIf(claimIssued)
            .state(PENDING_CLAIM_ISSUED_UNREPRESENTED_DEFENDANT)
                .transitionTo(TAKEN_OFFLINE_UNREPRESENTED_DEFENDANT).onlyIf(takenOfflineBySystem)
            .state(PENDING_CLAIM_ISSUED_UNREGISTERED_DEFENDANT)
                .transitionTo(TAKEN_OFFLINE_UNREGISTERED_DEFENDANT).onlyIf(takenOfflineBySystem)
            .state(CLAIM_ISSUED)
                .transitionTo(CLAIM_NOTIFIED).onlyIf(claimNotified)
                .transitionTo(TAKEN_OFFLINE_BY_STAFF).onlyIf(takenOfflineByStaffAfterClaimIssue)
                .transitionTo(CLAIM_DISMISSED_PAST_CLAIM_NOTIFICATION_DEADLINE).onlyIf(pastClaimNotificationDeadline)
            .state(CLAIM_NOTIFIED)
                .transitionTo(CLAIM_DETAILS_NOTIFIED).onlyIf(claimDetailsNotified)
                .transitionTo(TAKEN_OFFLINE_BY_STAFF).onlyIf(takenOfflineByStaffAfterClaimNotified)
                .transitionTo(CLAIM_DISMISSED_PAST_CLAIM_DETAILS_NOTIFICATION_DEADLINE)
                    .onlyIf(pastClaimDetailsNotificationDeadline)
            .state(CLAIM_DETAILS_NOTIFIED)
                .transitionTo(CLAIM_DETAILS_NOTIFIED_TIME_EXTENSION).onlyIf(claimDetailsNotifiedTimeExtension)
                .transitionTo(NOTIFICATION_ACKNOWLEDGED).onlyIf(notificationAcknowledged)
                .transitionTo(FULL_DEFENCE).onlyIf(fullDefenceAfterNotifyDetails)
                .transitionTo(FULL_ADMISSION).onlyIf(fullAdmissionAfterNotifyDetails)
                .transitionTo(PART_ADMISSION).onlyIf(partAdmissionAfterNotifyDetails)
                .transitionTo(COUNTER_CLAIM).onlyIf(counterClaimAfterNotifyDetails)
                .transitionTo(TAKEN_OFFLINE_BY_STAFF).onlyIf(takenOfflineByStaffAfterClaimDetailsNotified)
                .transitionTo(CLAIM_DISMISSED_PAST_CLAIM_DISMISSED_DEADLINE).onlyIf(caseDismissedAfterDetailNotified)
            .state(CLAIM_DETAILS_NOTIFIED_TIME_EXTENSION)
                .transitionTo(NOTIFICATION_ACKNOWLEDGED).onlyIf(notificationAcknowledgedTimeExtension)
                .transitionTo(FULL_DEFENCE).onlyIf(fullDefence)
                .transitionTo(FULL_ADMISSION).onlyIf(fullAdmission)
                .transitionTo(PART_ADMISSION).onlyIf(partAdmission)
                .transitionTo(COUNTER_CLAIM).onlyIf(counterClaim)
                .transitionTo(TAKEN_OFFLINE_BY_STAFF).onlyIf(takenOfflineByStaffAfterClaimDetailsNotifiedExtension)
                .transitionTo(CLAIM_DISMISSED_PAST_CLAIM_DISMISSED_DEADLINE)
                    .onlyIf(caseDismissedAfterDetailNotifiedExtension)
            .state(NOTIFICATION_ACKNOWLEDGED)
                .transitionTo(NOTIFICATION_ACKNOWLEDGED_TIME_EXTENSION)
                    .onlyIf(notificationAcknowledgedTimeExtension)
                .transitionTo(FULL_DEFENCE).onlyIf(fullDefenceAfterAcknowledge)
                .transitionTo(FULL_ADMISSION).onlyIf(fullAdmissionAfterAcknowledge)
                .transitionTo(PART_ADMISSION).onlyIf(partAdmissionAfterAcknowledge)
                .transitionTo(COUNTER_CLAIM).onlyIf(counterClaimAfterAcknowledge)
                .transitionTo(TAKEN_OFFLINE_BY_STAFF)
                    .onlyIf(takenOfflineByStaffAfterNotificationAcknowledged)
                .transitionTo(CLAIM_DISMISSED_PAST_CLAIM_DISMISSED_DEADLINE).onlyIf(caseDismissedAfterClaimAcknowledged)
            .state(NOTIFICATION_ACKNOWLEDGED_TIME_EXTENSION)
                .transitionTo(FULL_DEFENCE).onlyIf(fullDefence)
                .transitionTo(FULL_ADMISSION).onlyIf(fullAdmission)
                .transitionTo(PART_ADMISSION).onlyIf(partAdmission)
                .transitionTo(COUNTER_CLAIM).onlyIf(counterClaim)
                .transitionTo(TAKEN_OFFLINE_BY_STAFF)
                    .onlyIf(takenOfflineByStaffAfterNotificationAcknowledgedTimeExtension)
                .transitionTo(CLAIM_DISMISSED_PAST_CLAIM_DISMISSED_DEADLINE)
                    .onlyIf(caseDismissedAfterClaimAcknowledgedExtension)
            .state(FULL_DEFENCE)
                .transitionTo(FULL_DEFENCE_PROCEED).onlyIf(fullDefenceProceed)
                .transitionTo(FULL_DEFENCE_NOT_PROCEED).onlyIf(fullDefenceNotProceed)
                .transitionTo(TAKEN_OFFLINE_BY_STAFF).onlyIf(takenOfflineByStaff)
                .transitionTo(TAKEN_OFFLINE_PAST_APPLICANT_RESPONSE_DEADLINE).onlyIf(applicantOutOfTime)
            .state(CLAIM_DISMISSED_PAST_CLAIM_NOTIFICATION_DEADLINE)
                .transitionTo(TAKEN_OFFLINE_BY_STAFF).onlyIf(takenOfflineByStaff)
            .state(CLAIM_DISMISSED_PAST_CLAIM_DETAILS_NOTIFICATION_DEADLINE)
                .transitionTo(TAKEN_OFFLINE_BY_STAFF).onlyIf(takenOfflineByStaff)
            .state(CLAIM_DISMISSED_PAST_CLAIM_DISMISSED_DEADLINE)
            .state(FULL_ADMISSION)
            .state(PART_ADMISSION)
            .state(COUNTER_CLAIM)
            .state(FULL_DEFENCE_PROCEED)
            .state(FULL_DEFENCE_NOT_PROCEED)
            .state(TAKEN_OFFLINE_BY_STAFF)
            .state(PENDING_CLAIM_ISSUED_UNREPRESENTED_DEFENDANT)
            .state(PENDING_CLAIM_ISSUED_UNREGISTERED_DEFENDANT)
            .state(TAKEN_OFFLINE_UNREGISTERED_DEFENDANT)
            .state(TAKEN_OFFLINE_UNREPRESENTED_DEFENDANT)
            .state(TAKEN_OFFLINE_PAST_APPLICANT_RESPONSE_DEADLINE)
            .build();
    }

    public StateFlow evaluate(CaseDetails caseDetails) {
        return evaluate(caseDetailsConverter.toCaseData(caseDetails));
    }

    public StateFlow evaluate(CaseData caseData) {
        return build().evaluate(caseData);
    }

    public StateFlow evaluateSpec(CaseDetails caseDetails) {
        return evaluateSpec(caseDetailsConverter.toCaseData(caseDetails));
    }

    public StateFlow evaluateSpec(CaseData caseData) {
        return buildSpec().evaluate(caseData);
    }

    public boolean hasTransitionedTo(CaseDetails caseDetails, FlowState.Main state) {
        return evaluate(caseDetails).getStateHistory().stream()
            .map(State::getName)
            .anyMatch(name -> name.equals(state.fullName()));
    }

    public boolean hasSpecTransitionedTo(CaseDetails caseDetails, FlowState.Main state) {
        return evaluateSpec(caseDetails).getStateHistory().stream()
            .map(State::getName)
            .anyMatch(name -> name.equals(state.fullName()));
    }
}
