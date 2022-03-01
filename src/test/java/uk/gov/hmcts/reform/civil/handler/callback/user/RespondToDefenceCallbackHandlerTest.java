package uk.gov.hmcts.reform.civil.handler.callback.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.config.ExitSurveyConfiguration;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.StatementOfTruth;
import uk.gov.hmcts.reform.civil.model.UnavailableDate;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.dq.Applicant1DQ;
import uk.gov.hmcts.reform.civil.model.dq.Expert;
import uk.gov.hmcts.reform.civil.model.dq.Experts;
import uk.gov.hmcts.reform.civil.model.dq.Hearing;
import uk.gov.hmcts.reform.civil.model.dq.Witness;
import uk.gov.hmcts.reform.civil.model.dq.Witnesses;
import uk.gov.hmcts.reform.civil.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDetailsBuilder;
import uk.gov.hmcts.reform.civil.service.ExitSurveyContentService;
import uk.gov.hmcts.reform.civil.service.Time;
import uk.gov.hmcts.reform.civil.service.flowstate.FlowState;
import uk.gov.hmcts.reform.civil.validation.UnavailableDateValidator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CallbackVersion.V_1;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CLAIMANT_RESPONSE;
import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.READY;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    RespondToDefenceCallbackHandler.class,
    ExitSurveyConfiguration.class,
    ExitSurveyContentService.class,
    JacksonAutoConfiguration.class,
    ValidationAutoConfiguration.class,
    UnavailableDateValidator.class,
    CaseDetailsConverter.class
})
class RespondToDefenceCallbackHandlerTest extends BaseCallbackHandlerTest {

    @MockBean
    private Time time;

    @Autowired
    private RespondToDefenceCallbackHandler handler;

    @Autowired
    private ExitSurveyContentService exitSurveyContentService;

    @MockBean
    private FeatureToggleService featureToggleService;

    @Nested
    class AboutToStartCallback {

        @Test
        void shouldReturnNoError_WhenAboutToStartIsInvoked() {
            CaseDetails caseDetails = CaseDetailsBuilder.builder().atStateRespondedToClaim().build();
            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_START, caseDetails).build();

            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler
                .handle(params);

            assertThat(response.getErrors()).isNull();
        }
    }

    @Nested
    class AboutToStartCallbackV1 {

        @Test
        void shouldPopulateRespondent1ClaimResponseDocumentCopy_WhenAboutToStartIsInvoked() {
            CaseData caseData = CaseDataBuilder.builder()
                .atStateRespondentFullDefenceAfterNotifyClaimDetails()
                .build();
            CallbackParams params = callbackParamsOf(V_1, caseData, ABOUT_TO_START);

            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler
                .handle(params);

            assertThat(response.getErrors()).isNull();
            assertThat(response.getData().get("respondent1ClaimResponseDocumentCopy"))
                .isEqualTo(response.getData().get("respondent1ClaimResponseDocument"));
        }

        @Test
        void shouldSetClaimantResponseScenarioFlagTo1V1_WhenAboutToStartIsInvokedAndMulitPatyScenariois1V1() {
            CaseData caseData = CaseDataBuilder.builder()
                .atStateRespondentFullDefenceAfterNotifyClaimDetails()
                .build();
            CallbackParams params = callbackParamsOf(V_1, caseData, ABOUT_TO_START);

            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler
                .handle(params);

            assertThat(response.getErrors()).isNull();
            assertThat(response.getData().get("claimantResponseScenarioFlag"))
                .isEqualTo("ONE_V_ONE");
        }

        @Test
        void shouldSetClaimantResponseScenarioFlagTo2V1_WhenAboutToStartIsInvoked() {
            CaseData caseData = CaseDataBuilder.builder()
                .atStateRespondentFullDefenceAfterNotifyClaimDetails()
                .multiPartyClaimTwoApplicants()
                .build();
            CallbackParams params = callbackParamsOf(V_1, caseData, ABOUT_TO_START);

            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler
                .handle(params);

            assertThat(response.getErrors()).isNull();
            assertThat(response.getData().get("claimantResponseScenarioFlag"))
                .isEqualTo("TWO_V_ONE");
        }

        @Test
        void shouldSetClaimantResponseScenarioFlagTo1V2OneSol_WhenAboutToStartIsInvoked() {
            CaseData caseData = CaseDataBuilder.builder()
                .atStateRespondentFullDefenceAfterNotifyClaimDetails()
                .multiPartyClaimOneDefendantSolicitor()
                .build();
            CallbackParams params = callbackParamsOf(V_1, caseData, ABOUT_TO_START);

            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler
                .handle(params);

            assertThat(response.getErrors()).isNull();
            assertThat(response.getData().get("claimantResponseScenarioFlag"))
                .isEqualTo("ONE_V_TWO_ONE_LEGAL_REP");
        }

        @Test
        void shouldSetClaimantResponseScenarioFlagTo1V2TwoSol_WhenAboutToStartIsInvoked() {
            CaseData caseData = CaseDataBuilder.builder()
                .atStateRespondentFullDefenceAfterNotifyClaimDetails()
                .multiPartyClaimTwoDefendantSolicitors()
                .build();
            CallbackParams params = callbackParamsOf(V_1, caseData, ABOUT_TO_START);

            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler
                .handle(params);

            assertThat(response.getErrors()).isNull();
            assertThat(response.getData().get("claimantResponseScenarioFlag"))
                .isEqualTo("ONE_V_TWO_TWO_LEGAL_REP");
        }
    }

    @Nested
    class MidEventCallbackValidateUnavailableDates {

        @Test
        void shouldReturnError_whenUnavailableDateIsMoreThanOneYearInFuture() {
            CaseData.CaseDataBuilder caseDataBuilder = CaseData.builder();
            caseDataBuilder
                .applicant1DQ(Applicant1DQ.builder()
                                  .applicant1DQHearing(Hearing.builder()
                                                           .unavailableDatesRequired(YES)
                                                           .unavailableDates(wrapElements(
                                                               UnavailableDate.builder().date(
                                                                   LocalDate.now().plusYears(5)).build()))
                                                           .build())
                                  .build())
                .build();

            CallbackParams params = callbackParamsOf(caseDataBuilder.build(), MID, "validate-unavailable-dates");

            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler
                .handle(params);

            assertThat(response.getErrors())
                .containsExactly("The date cannot be in the past and must not be more than a year in the future");
        }

        @Test
        void shouldReturnError_whenUnavailableDateIsInPast() {
            CaseData.CaseDataBuilder caseDataBuilder = CaseData.builder();
            caseDataBuilder
                .applicant1DQ(Applicant1DQ.builder()
                                  .applicant1DQHearing(Hearing.builder()
                                                           .unavailableDatesRequired(YES)
                                                           .unavailableDates(wrapElements(
                                                               UnavailableDate.builder().date(
                                                                   LocalDate.now().minusYears(5)).build()))
                                                           .build())
                                  .build())
                .build();

            CallbackParams params = callbackParamsOf(caseDataBuilder.build(), MID, "validate-unavailable-dates");

            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler
                .handle(params);

            assertThat(response.getErrors())
                .containsExactly("The date cannot be in the past and must not be more than a year in the future");
        }

        @Test
        void shouldReturnNoError_whenUnavailableDateIsValid() {
            CaseData.CaseDataBuilder caseDataBuilder = CaseData.builder();
            caseDataBuilder
                .applicant1DQ(Applicant1DQ.builder()
                                  .applicant1DQHearing(Hearing.builder()
                                                           .unavailableDatesRequired(YES)
                                                           .unavailableDates(wrapElements(
                                                               UnavailableDate.builder().date(
                                                                   LocalDate.now().plusDays(5)).build()))
                                                           .build())
                                  .build())
                .build();

            CallbackParams params = callbackParamsOf(caseDataBuilder.build(), MID, "validate-unavailable-dates");

            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler
                .handle(params);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldReturnNoError_whenNoUnavailableDate() {
            CaseData.CaseDataBuilder caseDataBuilder = CaseData.builder();
            caseDataBuilder
                .applicant1DQ(Applicant1DQ.builder().applicant1DQHearing(Hearing.builder().build()).build()).build();

            CallbackParams params = callbackParamsOf(caseDataBuilder.build(), MID, "validate-unavailable-dates");

            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler
                .handle(params);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldReturnNoError_whenUnavailableDatesNotRequired() {
            CaseData.CaseDataBuilder caseDataBuilder = CaseData.builder();
            Hearing hearing = Hearing.builder().unavailableDatesRequired(NO).build();
            caseDataBuilder
                .applicant1DQ(Applicant1DQ.builder().applicant1DQHearing(hearing).build()).build();

            CallbackParams params = callbackParamsOf(caseDataBuilder.build(), MID, "validate-unavailable-dates");

            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler
                .handle(params);

            assertThat(response.getErrors()).isEmpty();
        }
    }

    @Nested
    class MidEventCallbackValidateWitnesses {

        private static final String PAGE_ID = "witnesses";

        @Test
        void shouldReturnError_whenWitnessRequiredAndNullDetails() {
            Witnesses witnesses = Witnesses.builder().witnessesToAppear(YES).build();
            CaseData caseData = CaseDataBuilder.builder()
                .applicant1DQ(Applicant1DQ.builder().applicant1DQWitnesses(witnesses).build())
                .build();
            CallbackParams params = callbackParamsOf(caseData, MID, PAGE_ID);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).containsExactly("Witness details required");
        }

        @Test
        void shouldReturnNoError_whenWitnessRequiredAndDetailsProvided() {
            List<Element<Witness>> testWitness = wrapElements(Witness.builder().name("test witness").build());
            Witnesses witnesses = Witnesses.builder().witnessesToAppear(YES).details(testWitness).build();
            CaseData caseData = CaseDataBuilder.builder()
                .applicant1DQ(Applicant1DQ.builder().applicant1DQWitnesses(witnesses).build())
                .build();
            CallbackParams params = callbackParamsOf(caseData, MID, PAGE_ID);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldReturnNoError_whenWitnessNotRequired() {
            Witnesses witnesses = Witnesses.builder().witnessesToAppear(NO).build();
            CaseData caseData = CaseDataBuilder.builder()
                .applicant1DQ(Applicant1DQ.builder().applicant1DQWitnesses(witnesses).build())
                .build();
            CallbackParams params = callbackParamsOf(caseData, MID, PAGE_ID);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }
    }

    @Nested
    class MidEventCallbackValidateExperts {

        private static final String PAGE_ID = "experts";

        @Test
        void shouldReturnError_whenExpertRequiredAndNullDetails() {
            CaseData caseData = CaseDataBuilder.builder()
                .applicant1DQ(Applicant1DQ.builder()
                                  .applicant1DQExperts(Experts.builder()
                                                           .expertRequired(YES)
                                                           .build())
                                  .build())
                .build();
            CallbackParams params = callbackParamsOf(caseData, MID, PAGE_ID);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).containsExactly("Expert details required");
        }

        @Test
        void shouldReturnNoError_whenExpertRequiredAndDetailsProvided() {
            CaseData caseData = CaseDataBuilder.builder()
                .applicant1DQ(Applicant1DQ.builder()
                                  .applicant1DQExperts(Experts.builder()
                                                           .expertRequired(YES)
                                                           .details(wrapElements(Expert.builder()
                                                                                     .name("test expert").build()))
                                                           .build())
                                  .build())
                .build();
            CallbackParams params = callbackParamsOf(caseData, MID, PAGE_ID);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldReturnNoError_whenExpertNotRequired() {
            CaseData caseData = CaseDataBuilder.builder()
                .applicant1DQ(Applicant1DQ.builder()
                                  .applicant1DQExperts(Experts.builder()
                                                           .expertRequired(NO)
                                                           .build())
                                  .build())
                .build();
            CallbackParams params = callbackParamsOf(caseData, MID, PAGE_ID);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }
    }

    @Nested
    class MidStatementOfTruth {

        @Test
        void shouldSetStatementOfTruthFieldsToNull_whenPopulated() {
            String name = "John Smith";
            String role = "Solicitor";

            CaseData caseData = CaseDataBuilder.builder()
                .uiStatementOfTruth(StatementOfTruth.builder().name(name).role(role).build())
                .build();

            CallbackParams params = callbackParamsOf(caseData, MID, "statement-of-truth");
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getData())
                .extracting("uiStatementOfTruth")
                .extracting("name", "role")
                .containsExactly(null, null);
        }
    }

    @Nested
    class AboutToSubmitCallback {
        private final LocalDateTime localDateTime = now();

        @BeforeEach
        void setup() {
            when(time.now()).thenReturn(localDateTime);
        }

        @ParameterizedTest
        @EnumSource(value = FlowState.Main.class,
            names = {"FULL_DEFENCE_PROCEED", "FULL_DEFENCE_NOT_PROCEED"},
            mode = EnumSource.Mode.INCLUDE)
        void shouldUpdateBusinessProcess_whenAtFullDefenceState(FlowState.Main flowState) {
            var params = callbackParamsOf(
                CaseDataBuilder.builder().atState(flowState).build(),
                ABOUT_TO_SUBMIT
            );

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getData()).extracting("businessProcess")
                .extracting("status", "camundaEvent")
                .containsExactly(READY.name(), CLAIMANT_RESPONSE.name());

            assertThat(response.getData()).containsEntry("applicant1ResponseDate", localDateTime.format(ISO_DATE_TIME));
        }

        @Nested
        class ResetStatementOfTruth {

            @Test
            void shouldAddUiStatementOfTruthToApplicantStatementOfTruth_whenInvoked() {
                String name = "John Smith";
                String role = "Solicitor";

                CaseData caseData = CaseDataBuilder.builder().atStateApplicantRespondToDefenceAndProceed().build()
                    .toBuilder()
                    .uiStatementOfTruth(StatementOfTruth.builder().name(name).role(role).build())
                    .build();

                var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(
                    callbackParamsOf(
                        caseData,
                        ABOUT_TO_SUBMIT
                    ));

                assertThat(response.getData())
                    .extracting("applicant1DQStatementOfTruth")
                    .extracting("name", "role")
                    .containsExactly("John Smith", "Solicitor");

                assertThat(response.getData())
                    .extracting("uiStatementOfTruth")
                    .extracting("name", "role")
                    .containsExactly(null, null);
            }
        }
    }

    @Nested
    class SubmittedCallback {

        @Test
        void shouldReturnExpectedResponse_whenApplicantIsProceedingWithClaim() {
            CaseData caseData = CaseDataBuilder.builder()
                .atStateApplicantRespondToDefenceAndProceed()
                .build();
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            SubmittedCallbackResponse response = (SubmittedCallbackResponse) handler.handle(params);

            assertThat(response).usingRecursiveComparison().isEqualTo(
                SubmittedCallbackResponse.builder()
                    .confirmationHeader(format(
                        "# You have chosen to proceed with the claim%n## Claim number: 000DC001"))
                    .confirmationBody(format(
                        "<br />We will review the case and contact you to tell you what to do next.%n%n"
                            + "[Download directions questionnaire](http://www.google.com)"
                    ) + exitSurveyContentService.applicantSurvey())
                    .build());
        }

        @Test
        void shouldReturnExpectedResponse_whenApplicantIsNotProceedingWithClaim() {
            CaseData caseData = CaseDataBuilder.builder()
                .atStateApplicantRespondToDefenceAndProceed()
                .applicant1ProceedWithClaim(NO)
                .build();
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            SubmittedCallbackResponse response = (SubmittedCallbackResponse) handler.handle(params);

            assertThat(response).usingRecursiveComparison().isEqualTo(
                SubmittedCallbackResponse.builder()
                    .confirmationHeader(format("# You have chosen not to proceed with the claim%n## Claim number:"
                                                   + " 000DC001"))
                    .confirmationBody(exitSurveyContentService.applicantSurvey())
                    .build());
        }
    }

    @Nested
    class MidSetApplicantsProceedIntention {

        @Test
        void shouldSetToYes_whenApplicant1IntendToProceed() {
            CaseData caseData = CaseDataBuilder.builder()
                .applicant1ProceedWithClaim(YES)
                .build();

            CallbackParams params = callbackParamsOf(caseData, MID, "set-applicants-proceed-intention");
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getData()).extracting("applicantsProceedIntention").isEqualTo("Yes");
        }

        @Test
        void shouldSetToYes_whenApplicant2IntendToProceed() {
            CaseData caseData = CaseDataBuilder.builder()
                .applicant2ProceedWithClaimMultiParty2v1(YES)
                .build();

            CallbackParams params = callbackParamsOf(caseData, MID, "set-applicants-proceed-intention");
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getData()).extracting("applicantsProceedIntention").isEqualTo("Yes");
        }

        @Test
        void shouldSetToNo_whenApplicant1OrApplicant2DoNotIntendToProceed() {
            CaseData caseData = CaseDataBuilder.builder()
                .applicant1ProceedWithClaim(NO)
                .applicant2ProceedWithClaimMultiParty2v1(NO)
                .build();

            CallbackParams params = callbackParamsOf(caseData, MID, "set-applicants-proceed-intention");
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getData()).extracting("applicantsProceedIntention").isEqualTo("No");
        }
    }
}
