package uk.gov.hmcts.reform.civil.service.flowstate;

import static org.springframework.util.StringUtils.hasLength;

public interface FlowState {

    String fullName();

    static FlowState fromFullName(String fullName) {
        if (!hasLength(fullName)) {
            throw new IllegalArgumentException("Invalid full name:" + fullName);
        }
        int lastIndexOfDot = fullName.lastIndexOf('.');
        String flowStateName = fullName.substring(lastIndexOfDot + 1);
        String flowName = fullName.substring(0, lastIndexOfDot);
        if (flowName.equals("MAIN")) {
            return Main.valueOf(flowStateName);
        } else {
            throw new IllegalArgumentException("Invalid flow name:" + flowName);
        }
    }

    enum Main implements FlowState {
        DRAFT,
        SPEC_DRAFT,
        CLAIM_SUBMITTED,
        CLAIM_ISSUED_PAYMENT_SUCCESSFUL,
        CLAIM_ISSUED_PAYMENT_FAILED,
        PENDING_CLAIM_ISSUED,
        CLAIM_ISSUED,
        CLAIM_NOTIFIED,
        CLAIM_DETAILS_NOTIFIED_TIME_EXTENSION,
        CLAIM_DETAILS_NOTIFIED,
        TAKEN_OFFLINE_AFTER_CLAIM_NOTIFIED,
        TAKEN_OFFLINE_AFTER_CLAIM_DETAILS_NOTIFIED,
        NOTIFICATION_ACKNOWLEDGED,
        NOTIFICATION_ACKNOWLEDGED_TIME_EXTENSION,
        FULL_DEFENCE,
        FULL_ADMISSION,
        PART_ADMISSION,
        COUNTER_CLAIM,
        DIVERGENT_RESPOND_GO_OFFLINE,
        DIVERGENT_RESPOND_GENERATE_DQ_GO_OFFLINE,
        FULL_DEFENCE_PROCEED,
        FULL_DEFENCE_NOT_PROCEED,
        ALL_RESPONSES_RECEIVED,
        AWAITING_RESPONSES_FULL_DEFENCE_RECEIVED,
        AWAITING_RESPONSES_NOT_FULL_DEFENCE_RECEIVED,
        TAKEN_OFFLINE_BY_STAFF,
        TAKEN_OFFLINE_PAST_APPLICANT_RESPONSE_DEADLINE,
        PAST_APPLICANT_RESPONSE_DEADLINE_AWAITING_CAMUNDA,
        PENDING_CLAIM_ISSUED_UNREPRESENTED_DEFENDANT,
        PENDING_CLAIM_ISSUED_UNREGISTERED_DEFENDANT,
        PENDING_CLAIM_ISSUED_UNREPRESENTED_UNREGISTERED_DEFENDANT,
        TAKEN_OFFLINE_UNREPRESENTED_DEFENDANT,
        TAKEN_OFFLINE_UNREGISTERED_DEFENDANT,
        TAKEN_OFFLINE_UNREPRESENTED_UNREGISTERED_DEFENDANT,
        CLAIM_DISMISSED_PAST_CLAIM_DISMISSED_DEADLINE,
        PAST_CLAIM_DISMISSED_DEADLINE_AWAITING_CAMUNDA,
        CLAIM_DISMISSED_PAST_CLAIM_NOTIFICATION_DEADLINE,
        PAST_CLAIM_NOTIFICATION_DEADLINE_AWAITING_CAMUNDA,
        CLAIM_DISMISSED_PAST_CLAIM_DETAILS_NOTIFICATION_DEADLINE,
        PAST_CLAIM_DETAILS_NOTIFICATION_DEADLINE_AWAITING_CAMUNDA;

        public static final String FLOW_NAME = "MAIN";

        @Override
        public String fullName() {
            return FLOW_NAME + "." + name();
        }
    }
}
