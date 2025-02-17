package uk.gov.hmcts.reform.civil.handler.callback.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.config.ExitSurveyConfiguration;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.DeadlinesCalculator;
import uk.gov.hmcts.reform.civil.service.ExitSurveyContentService;
import uk.gov.hmcts.reform.civil.service.Time;

import java.time.LocalDateTime;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CallbackVersion.V_1;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NOTIFY_DEFENDANT_OF_CLAIM;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.DATE_TIME_AT;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.formatLocalDateTime;
import static uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder.DEADLINE;
import static uk.gov.hmcts.reform.civil.service.DeadlinesCalculator.END_OF_BUSINESS_DAY;

@SpringBootTest(classes = {
    NotifyClaimCallbackHandler.class,
    ExitSurveyConfiguration.class,
    ExitSurveyContentService.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotifyClaimCallbackHandlerTest extends BaseCallbackHandlerTest {

    @MockBean
    private DeadlinesCalculator deadlinesCalculator;

    @MockBean
    private Time time;

    @MockBean
    private FeatureToggleService featureToggleService;

    @Autowired
    private NotifyClaimCallbackHandler handler;

    @Autowired
    private ExitSurveyContentService exitSurveyContentService;

    private final LocalDateTime notificationDate = LocalDateTime.now();
    private final LocalDateTime deadline = notificationDate.toLocalDate().atTime(END_OF_BUSINESS_DAY);

    @Nested
    class AboutToStartCallback {

        @Test
        void shouldPrepopulateDynamicListWithOptions_whenInvoked() {
            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimNotified_1v2_andNotifyBothSolicitors()
                .build();

            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_START);
            AboutToStartOrSubmitCallbackResponse response =
                (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertTrue(response.getData().containsKey("defendantSolicitorNotifyClaimOptions"));
        }
    }

    @Nested
    class MidEventValidateOptionsCallback {

        private static final String PAGE_ID = "validateNotificationOption";
        public static final String WARNING_ONLY_NOTIFY_ONE_DEFENDANT_SOLICITOR =
            "Your claim will progress offline if you only notify one Defendant of the claim details.";

        @Test
        void shouldThrowWarning_whenNotifyingOnlyOneRespondentSolicitorAndMultipartyToggleOn() {

            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimNotified_1v2_andNotifyOnlyOneSolicitor()
                .build();

            CallbackParams params = callbackParamsOf(caseData, MID, PAGE_ID);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            assertThat(response.getWarnings()).contains(WARNING_ONLY_NOTIFY_ONE_DEFENDANT_SOLICITOR);
        }

        @Test
        void shouldNotThrowWarning_whenNotifyingBothRespondentSolicitors() {
            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimNotified_1v2_andNotifyBothSolicitors()
                .build();

            CallbackParams params = callbackParamsOf(caseData, MID, PAGE_ID);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            assertThat(response.getWarnings()).isEmpty();
        }
    }

    @Nested
    class AboutToSubmit {

        @Nested
        class SubmittedAtCurrentTime {

            @BeforeEach
            void setup() {
                when(time.now()).thenReturn(notificationDate);
                when(deadlinesCalculator.plus14DaysAt4pmDeadline(any(LocalDateTime.class))).thenReturn(deadline);
            }

            @Test
            void shouldUpdateBusinessProcessAndAddNotificationDeadline_when14DaysIsBeforeThe4MonthDeadline() {
                LocalDateTime claimNotificationDeadline = notificationDate.plusMonths(4);
                CaseData caseData = CaseDataBuilder.builder().atStateClaimNotified_1v1()
                    .claimNotificationDeadline(claimNotificationDeadline)
                    .build();
                CallbackParams params = CallbackParamsBuilder.builder().of(
                    CallbackType.ABOUT_TO_SUBMIT,
                    caseData
                ).build();
                var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

                assertThat(response.getData())
                    .extracting("businessProcess")
                    .extracting("camundaEvent", "status")
                    .containsOnly(NOTIFY_DEFENDANT_OF_CLAIM.name(), "READY");

                assertThat(response.getData())
                    .containsEntry("claimNotificationDate", notificationDate.format(ISO_DATE_TIME))
                    .containsEntry("claimDetailsNotificationDeadline", deadline.format(ISO_DATE_TIME));
            }

            @Test
            void shouldSetClaimDetailsNotificationAsNotificationDeadlineAt_when14DaysIsAfterThe4MonthDeadline() {
                LocalDateTime claimNotificationDeadline = notificationDate.minusDays(5);
                CaseData caseData = CaseDataBuilder.builder().atStateClaimNotified_1v1()
                    .claimNotificationDeadline(claimNotificationDeadline)
                    .build();
                CallbackParams params = CallbackParamsBuilder.builder().of(
                    CallbackType.ABOUT_TO_SUBMIT,
                    caseData
                ).build();
                var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

                LocalDateTime expectedTime = claimNotificationDeadline.toLocalDate().atTime(END_OF_BUSINESS_DAY);

                assertThat(response.getData())
                    .containsEntry("claimDetailsNotificationDeadline", expectedTime.format(ISO_DATE_TIME));
            }

            @Test
            void shouldSetClaimDetailsNotificationAsClaimNotificationDeadline_when14DaysIsSameDayAs4MonthDeadline() {
                CaseData caseData = CaseDataBuilder.builder().atStateClaimNotified_1v1()
                    .claimNotificationDeadline(deadline)
                    .build();
                CallbackParams params = CallbackParamsBuilder.builder().of(
                    CallbackType.ABOUT_TO_SUBMIT,
                    caseData
                ).build();
                var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

                assertThat(response.getData())
                    .containsEntry("claimDetailsNotificationDeadline", deadline.format(ISO_DATE_TIME));
            }
        }

        @Nested
        class SubmittedOnDeadlineDay {

            LocalDateTime claimNotificationDeadline = LocalDateTime.of(2021, 4, 6, 23, 59, 59);
            LocalDateTime claimDetailsNotificationDeadline = LocalDateTime.of(2021, 4, 5, 15, 15, 59);
            LocalDateTime expectedDeadline = claimDetailsNotificationDeadline;

            @BeforeEach
            void setup() {
                when(deadlinesCalculator.plus14DaysAt4pmDeadline(any(LocalDateTime.class)))
                    .thenReturn(claimDetailsNotificationDeadline);
            }

            @Test
            void shouldSetDetailsNotificationDeadlineTo4pmDeadline_whenNotifyClaimBefore4pm() {
                LocalDateTime notifyClaimDateTime = LocalDateTime.of(2021, 4, 5, 10, 0);
                when(time.now()).thenReturn(notifyClaimDateTime);

                CaseData caseData = CaseDataBuilder.builder().atStateClaimNotified_1v1()
                    .claimNotificationDeadline(claimNotificationDeadline)
                    .build();
                CallbackParams params = CallbackParamsBuilder.builder().of(
                    CallbackType.ABOUT_TO_SUBMIT,
                    caseData
                ).build();
                var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

                assertThat(response.getData())
                    .containsEntry("claimDetailsNotificationDeadline", expectedDeadline.format(ISO_DATE_TIME));
            }

            @Test
            void shouldSetDetailsNotificationDeadlineTo4pmDeadline_whenNotifyClaimAfter4pm() {
                LocalDateTime notifyClaimDateTime = LocalDateTime.of(2021, 4, 5, 17, 0);
                when(time.now()).thenReturn(notifyClaimDateTime);

                CaseData caseData = CaseDataBuilder.builder().atStateClaimNotified_1v1()
                    .claimNotificationDeadline(claimNotificationDeadline)
                    .build();
                CallbackParams params = CallbackParamsBuilder.builder().of(
                    CallbackType.ABOUT_TO_SUBMIT,
                    caseData
                ).build();
                var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

                assertThat(response.getData())
                    .containsEntry("claimDetailsNotificationDeadline", expectedDeadline.format(ISO_DATE_TIME));
            }
        }
    }

    @Nested
    class SubmittedCallback {

        private static final String CONFIRMATION_BODY = "<br />The defendant legal representative's organisation has "
            + "been notified and granted access to this claim.%n%n"
            + "You must notify the defendant with the claim details by %s";
        public static final String CONFIRMATION_NOTIFICATION_ONE_PARTY_SUMMARY = "<br />Notification of claim sent to "
            + "1 Defendant legal representative only.%n%n"
            + "Your claim will proceed offline.";

        @Test
        void shouldReturnExpectedSubmittedCallbackResponse_whenInvoked() {
            CaseData caseData = CaseDataBuilder.builder().atStateClaimNotified_1v1().build();
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);
            SubmittedCallbackResponse response = (SubmittedCallbackResponse) handler.handle(params);

            String formattedDeadline = formatLocalDateTime(DEADLINE, DATE_TIME_AT);
            String confirmationBody = String.format(CONFIRMATION_BODY, formattedDeadline)
                + exitSurveyContentService.applicantSurvey();

            assertThat(response).usingRecursiveComparison().isEqualTo(
                SubmittedCallbackResponse.builder()
                    .confirmationHeader(format("# Notification of claim sent%n## Claim number: 000DC001"))
                    .confirmationBody(confirmationBody)
                    .build());
        }

        @Test
        void shouldReturnExpectedSubmittedCallbackResponse_whenNotifyingBothParties_whenInvoked() {

            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimNotified_1v2_andNotifyBothSolicitors()
                .build();

            CallbackParams params = callbackParamsOf(V_1, caseData, SUBMITTED);
            SubmittedCallbackResponse response = (SubmittedCallbackResponse) handler.handle(params);

            String formattedDeadline = formatLocalDateTime(DEADLINE, DATE_TIME_AT);
            String confirmationBody = String.format(CONFIRMATION_BODY, formattedDeadline)
                + exitSurveyContentService.applicantSurvey();

            assertThat(response).usingRecursiveComparison().isEqualTo(
                SubmittedCallbackResponse.builder()
                    .confirmationHeader(format("# Notification of claim sent%n## Claim number: 000DC001"))
                    .confirmationBody(confirmationBody)
                    .build());
        }

        @Test
        void shouldReturnExpectedSubmittedCallbackResponse_whenNotifyingOneParty_whenInvoked() {

            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimNotified_1v2_andNotifyOnlyOneSolicitor()
                .build();

            CallbackParams params = callbackParamsOf(V_1, caseData, SUBMITTED);
            SubmittedCallbackResponse response = (SubmittedCallbackResponse) handler.handle(params);

            String formattedDeadline = formatLocalDateTime(DEADLINE, DATE_TIME_AT);
            String confirmationBody = String.format(
                CONFIRMATION_NOTIFICATION_ONE_PARTY_SUMMARY,
                formattedDeadline
            ) + exitSurveyContentService.applicantSurvey();

            assertThat(response).usingRecursiveComparison().isEqualTo(
                SubmittedCallbackResponse.builder()
                    .confirmationHeader(format("# Notification of claim sent%n## Claim number: 000DC001"))
                    .confirmationBody(confirmationBody)
                    .build());
        }
    }
}
