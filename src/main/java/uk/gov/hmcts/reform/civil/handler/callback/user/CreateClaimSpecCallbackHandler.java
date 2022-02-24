package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.handler.callback.user.createclaim.CreateClaimConfirmationBuilder;
import uk.gov.hmcts.reform.civil.handler.callback.user.createclaim.CreateClaimFeeCalculator;
import uk.gov.hmcts.reform.civil.handler.callback.user.createclaim.CreateClaimSharedDataExtractor;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.ClaimAmountBreakup;
import uk.gov.hmcts.reform.civil.model.Party;
import uk.gov.hmcts.reform.civil.model.StatementOfTruth;
import uk.gov.hmcts.reform.civil.model.TimelineOfEvents;
import uk.gov.hmcts.reform.civil.repositories.SpecReferenceNumberRepository;
import uk.gov.hmcts.reform.civil.utils.MonetaryConversions;
import uk.gov.hmcts.reform.civil.validation.DateOfBirthValidator;
import uk.gov.hmcts.reform.civil.validation.OrgPolicyValidator;
import uk.gov.hmcts.reform.civil.validation.PostcodeValidator;
import uk.gov.hmcts.reform.civil.validation.ValidateEmailService;
import uk.gov.hmcts.reform.civil.validation.interfaces.ParticularsOfClaimValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CREATE_CLAIM_SPEC;
import static uk.gov.hmcts.reform.civil.enums.SuperClaimType.SPEC_CLAIM;
import static uk.gov.hmcts.reform.civil.enums.SuperClaimType.UNSPEC_CLAIM;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateClaimSpecCallbackHandler extends CallbackHandler
    implements ParticularsOfClaimValidator {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(
        CaseEvent.CREATE_CLAIM_SPEC
    );
    public static final String CONFIRMATION_SUMMARY = "<br/>[Download the sealed claim form](%s)"
        + "%n%nYour claim will not be issued until payment is confirmed. Once payment is confirmed you will "
        + "receive an email. The email will also include the date when you eed to notify the Defendant legal "
        + "representative of the claim.%n%nYou must notify the Defendant legal representative of the claim within 4 "
        + "months of the claim being issued. The exact date when you must notify the claim details will be provided "
        + "when you first notify the Defendant legal representative of the claim.";

    public static final String LIP_CONFIRMATION_BODY = "<br />Your claim will not be issued until payment is confirmed."
        + " Once payment is confirmed you will receive an email. The claim will then progress offline."
        + "%n%nTo continue the claim you need to send the <a href=\"%s\" target=\"_blank\">sealed claim form</a>, "
        + "a <a href=\"%s\" target=\"_blank\">response pack</a> and any supporting documents to "
        + "the defendant within 4 months. "
        + "%n%nOnce you have served the claim, send the Certificate of Service and supporting documents to the County"
        + " Court Claims Centre.";

    public static final String SPEC_CONFIRMATION_SUMMARY = "<br/>[Download the sealed claim form](%s)"
        + "%n%nYour claim will not be issued until payment is confirmed. Once payment is confirmed you will "
        + "receive an email. The email will also include the date when you need to notify the defendant "
        + "of the claim.%n%nYou must notify the defendant of the claim within 4 months of the claim being issued. "
        + "The exact date when you must notify the claim details will be provided when you first notify "
        + "the defendant of the claim.";

    public static final String SPEC_LIP_CONFIRMATION_BODY = "<br />When payment is confirmed your claim will be issued "
        + "and you'll be notified by email. The claim will then progress offline."
        + "%n%nTo continue the claim you need to send the:<ul> <li> <a href=\"%s\" target=\"_blank\">sealed claim form</a> "
        + "</li><li><a href=\"%s\" target=\"_blank\">response pack</a></li><li> and any supporting documents </li></ul>to "
        + "the defendant within 4 months. "
        + "%n%nOnce you have served the claim, send the Certificate of Service and supporting documents to the County"
        + " Court Claims Centre.";

    private final SpecReferenceNumberRepository specReferenceNumberRepository;
    private final DateOfBirthValidator dateOfBirthValidator;
    private final OrgPolicyValidator orgPolicyValidator;
    private final ObjectMapper objectMapper;
    private final ValidateEmailService validateEmailService;
    private final PostcodeValidator postcodeValidator;
    private final CreateClaimFeeCalculator createClaimFeeCalculator;
    private final CreateClaimSharedDataExtractor createClaimSharedDataExtractor;
    private final CreateClaimConfirmationBuilder createClaimConfirmationBuilder;

    @Override
    protected Map<String, Callback> callbacks() {
        return new ImmutableMap.Builder<String, Callback>()
            .put(callbackKey(ABOUT_TO_START), this::emptyCallbackResponse)
            .put(callbackKey(MID, "eligibilityCheck"), this::eligibilityCheck)
            .put(callbackKey(MID, "applicant"), this::validateClaimantDetails)
            .put(callbackKey(MID, "applicant2"), this::validateApplicant2DateOfBirth)
            .put(callbackKey(MID, "fee"), this::calculateFee)
            .put(callbackKey(MID, "idam-email"), this::getIdamEmail)
            .put(callbackKey(MID, "validate-defendant-legal-rep-email"), this::validateRespondentRepEmail)
            .put(callbackKey(MID, "validate-claimant-legal-rep-email"), this::validateClaimantRepEmail)
            .put(callbackKey(MID, "particulars-of-claim"), this::validateParticularsOfClaim)
            .put(callbackKey(MID, "appOrgPolicy"), this::validateApplicantSolicitorOrgPolicy)
            .put(callbackKey(MID, "repOrgPolicy"), this::validateRespondentSolicitorOrgPolicy)
            .put(callbackKey(MID, "rep2OrgPolicy"), this::validateRespondentSolicitor2OrgPolicy)
            .put(callbackKey(MID, "statement-of-truth"), this::resetStatementOfTruth)
            .put(callbackKey(ABOUT_TO_SUBMIT), this::submitClaim)
            .put(callbackKey(SUBMITTED), this::buildConfirmation)
            .put(callbackKey(MID, "respondent1"), this::validateRespondent1Address)
            .put(callbackKey(MID, "amount-breakup"), this::calculateTotalClaimAmount)
            .put(callbackKey(MID, "respondentSolicitor1"), this::validateRespondentSolicitorAddress)
            .put(callbackKey(MID, "interest-calc"), this::calculateInterest)
            .put(callbackKey(MID, "ClaimInterest"), this::specCalculateInterest)
            .put(callbackKey(MID, "spec-fee"), this::calculateSpecFee)
            .put(callbackKey(MID, "ValidateClaimInterestDate"), this::specValidateClaimInterestDate)
            .put(callbackKey(MID, "ValidateClaimTimelineDate"), this::specValidateClaimTimelineDate)
            .put(callbackKey(MID, "specCorrespondenceAddress"), this::validateCorrespondenceApplicantAddress)
            .put(
                callbackKey(MID, "specRespondentCorrespondenceAddress"),
                this::validateCorrespondenceRespondentAddress
            )
            .put(callbackKey(MID, "validate-spec-defendant-legal-rep-email"), this::validateSpecRespondentRepEmail)
            .build();
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse eligibilityCheck(CallbackParams callbackParams) {
        List<String> errors = new ArrayList<>();
        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .build();
    }

    private CallbackResponse validateClaimantDetails(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        Party applicant = caseData.getApplicant1();
        List<String> errors = dateOfBirthValidator.validate(applicant);
        caseDataBuilder.superClaimType(UNSPEC_CLAIM);
        if (errors.size() == 0 && callbackParams.getRequest().getEventId() != null) {
            errors = postcodeValidator.validatePostCodeForDefendant(
                caseData.getApplicant1().getPrimaryAddress().getPostCode());
            caseDataBuilder.superClaimType(SPEC_CLAIM);
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .data(errors.size() == 0
                      ? caseDataBuilder.build().toMap(objectMapper) : null)
            .build();
    }

    private CallbackResponse validateApplicant2DateOfBirth(CallbackParams callbackParams) {
        Party applicant = callbackParams.getCaseData().getApplicant2();
        List<String> errors = dateOfBirthValidator.validate(applicant);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .build();
    }

    private CallbackResponse validateApplicantSolicitorOrgPolicy(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        OrganisationPolicy applicant1OrganisationPolicy = caseData.getApplicant1OrganisationPolicy();
        List<String> errors = orgPolicyValidator.validate(applicant1OrganisationPolicy, YES);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .build();
    }

    private CallbackResponse validateRespondentSolicitorOrgPolicy(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        OrganisationPolicy respondent1OrganisationPolicy = caseData.getRespondent1OrganisationPolicy();
        YesOrNo respondent1OrgRegistered = caseData.getRespondent1OrgRegistered();
        List<String> errors = orgPolicyValidator.validate(respondent1OrganisationPolicy, respondent1OrgRegistered);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .build();
    }

    private CallbackResponse specValidateClaimInterestDate(CallbackParams callbackParams) {
        if (callbackParams.getRequest().getEventId().equals("CREATE_CLAIM_SPEC")) {
            CaseData caseData = callbackParams.getCaseData();
            List<String> errors = new ArrayList<>();
            if (caseData.getInterestFromSpecificDate() != null) {
                if (caseData.getInterestFromSpecificDate().isAfter(LocalDate.now())) {
                    errors.add("Correct the date. You can’t use a future date.");
                }
            }

            return AboutToStartOrSubmitCallbackResponse.builder()
                .errors(errors)
                .build();
        }
        return AboutToStartOrSubmitCallbackResponse.builder()
            .build();
    }

    private CallbackResponse specValidateClaimTimelineDate(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        List<String> errors = new ArrayList<>();
        if (caseData.getTimelineOfEvents() != null) {
            List<TimelineOfEvents> timelineOfEvent = caseData.getTimelineOfEvents();
            timelineOfEvent.forEach(timelineOfEvents -> {
                if (timelineOfEvents.getValue().getTimelineDate().isAfter(LocalDate.now())) {
                    errors.add("Correct the date. You can’t use a future date.");
                }
            });
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .build();
    }

    private CallbackResponse validateRespondentSolicitor2OrgPolicy(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        OrganisationPolicy respondent2OrganisationPolicy = caseData.getRespondent2OrganisationPolicy();
        YesOrNo respondent2OrgRegistered = caseData.getRespondent2OrgRegistered();
        List<String> errors = orgPolicyValidator.validate(respondent2OrganisationPolicy, respondent2OrgRegistered);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .build();
    }

    private CallbackResponse calculateFee(CallbackParams callbackParams) {
        return createClaimFeeCalculator.calculateFee(callbackParams);
    }

    private CallbackResponse getIdamEmail(CallbackParams callbackParams) {
        return createClaimSharedDataExtractor.getIdamEmail(callbackParams);
    }

    //WARNING! below function getPbaAccounts is being used by both damages and specified claims,
    // changes to this code may break one of the claim journeys, check with respective teams before changing it
    private CallbackResponse validateClaimantRepEmail(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();

        if (!caseData.getApplicantSolicitor1CheckEmail().isCorrect()) {
            return AboutToStartOrSubmitCallbackResponse.builder()
                .errors(validateEmailService.validate(caseData.getApplicantSolicitor1UserDetails().getEmail()))
                .build();
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseData.toMap(objectMapper))
            .build();
    }

    private CallbackResponse validateRespondentRepEmail(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(validateEmailService.validate(caseData.getRespondentSolicitor1EmailAddress()))
            .build();
    }

    private CallbackResponse resetStatementOfTruth(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();

        // resetting statement of truth field, this resets in the page, but the data is still sent to the db.
        // must be to do with the way XUI cache data entered through the lifecycle of an event.
        CaseData updatedCaseData = caseData.toBuilder()
            .uiStatementOfTruth(null)
            .build();

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(updatedCaseData.toMap(objectMapper))
            .build();
    }

    private CallbackResponse submitClaim(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        // second idam call is workaround for null pointer when hiding field in getIdamEmail callback
        CaseData.CaseDataBuilder dataBuilder = getSharedData(callbackParams);

        // moving statement of truth value to correct field, this was not possible in mid event.
        // resetting statement of truth to make sure it's empty the next time it appears in the UI.
        StatementOfTruth statementOfTruth = caseData.getUiStatementOfTruth();
        dataBuilder.uiStatementOfTruth(StatementOfTruth.builder().build());
        dataBuilder.applicantSolicitor1ClaimStatementOfTruth(statementOfTruth);
        if (callbackParams.getRequest().getEventId() != null) {
            var respondent1Represented = caseData.getSpecRespondent1Represented();
            dataBuilder.respondent1Represented(respondent1Represented);
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(dataBuilder.build().toMap(objectMapper))
            .build();
    }

    private CaseData.CaseDataBuilder getSharedData(CallbackParams callbackParams) {
        CaseData.CaseDataBuilder dataBuilder = createClaimSharedDataExtractor.getSharedData(callbackParams);
        if (null != callbackParams.getRequest().getEventId()) {
            log.debug(" inside if condition ");
            dataBuilder.legacyCaseReference(specReferenceNumberRepository.getSpecReferenceNumber());
            dataBuilder.businessProcess(BusinessProcess.ready(CREATE_CLAIM_SPEC));
        }
        return dataBuilder;
    }

    private SubmittedCallbackResponse buildConfirmation(CallbackParams callbackParams) {
        return createClaimConfirmationBuilder.buildSpecConfirmation(callbackParams
        );
    }

    private CallbackResponse validateRespondent1Address(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        List<String> errors = postcodeValidator.validatePostCodeForDefendant(
            caseData.getRespondent1().getPrimaryAddress().getPostCode());

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .build();

    }

    private CallbackResponse validateRespondentSolicitorAddress(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        List<String> errors = postcodeValidator.validatePostCodeForDefendant(
            caseData.getRespondentSolicitor1OrganisationDetails().getAddress().getPostCode());

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .build();
    }

    private CallbackResponse validateCorrespondenceRespondentAddress(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        if (caseData.getSpecRespondentCorrespondenceAddressRequired().equals(YES)) {
            List<String> errors = postcodeValidator.validatePostCodeForDefendant(
                caseData.getSpecRespondentCorrespondenceAddressdetails().getPostCode());

            return AboutToStartOrSubmitCallbackResponse.builder()
                .errors(errors)
                .build();
        } else {
            return AboutToStartOrSubmitCallbackResponse.builder()
                .build();
        }
    }

    private CallbackResponse validateCorrespondenceApplicantAddress(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        if (caseData.getSpecApplicantCorrespondenceAddressRequired().equals(YES)) {
            List<String> errors = postcodeValidator.validatePostCodeForDefendant(
                caseData.getSpecApplicantCorrespondenceAddressdetails().getPostCode());

            return AboutToStartOrSubmitCallbackResponse.builder()
                .errors(errors)
                .build();
        } else {
            return AboutToStartOrSubmitCallbackResponse.builder()
                .build();
        }
    }

    //calculate total amount for specified claim by adding up the claim break up amounts
    private CallbackResponse calculateTotalClaimAmount(CallbackParams callbackParams) {

        CaseData caseData = callbackParams.getCaseData();
        BigDecimal totalClaimAmount = new BigDecimal(0);
        List<ClaimAmountBreakup> claimAmountBreakups = caseData.getClaimAmountBreakup();

        String totalAmount = " | Description | Amount | \n |---|---| \n | ";
        StringBuilder stringBuilder = new StringBuilder();
        for (ClaimAmountBreakup claimAmountBreakup : claimAmountBreakups) {
            totalClaimAmount =
                totalClaimAmount.add(claimAmountBreakup.getValue().getClaimAmount());

            stringBuilder.append(claimAmountBreakup.getValue().getClaimReason())
                .append(" | ")
                .append("£ ")
                .append(MonetaryConversions.penniesToPounds(claimAmountBreakup.getValue().getClaimAmount()))
                .append(" |\n ");
        }
        totalAmount = totalAmount.concat(stringBuilder.toString());

        List<String> errors = new ArrayList<>();
        if (MonetaryConversions.penniesToPounds(totalClaimAmount).doubleValue() > 25000) {
            errors.add("Total Claim Amount cannot exceed £ 25,000");
            return AboutToStartOrSubmitCallbackResponse.builder()
                .errors(errors)
                .build();
        }
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        caseDataBuilder.totalClaimAmount(
            MonetaryConversions.penniesToPounds(totalClaimAmount));

        totalAmount = totalAmount.concat(" | **Total** | £ " + MonetaryConversions
            .penniesToPounds(totalClaimAmount) + " | ");

        caseDataBuilder.claimAmountBreakupSummaryObject(totalAmount);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    //calculate interest for specified claim
    private CallbackResponse calculateInterest(CallbackParams callbackParams) {
        return createClaimFeeCalculator.calculateInterest(callbackParams);
    }

    private CallbackResponse specCalculateInterest(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        BigDecimal totalAmountWithInterest = caseData.getTotalClaimAmount();

        String calculateInterest = " | Description | Amount | \n |---|---| \n | Claim amount | £ "
            + caseData.getTotalClaimAmount()
            + " | \n | Interest amount | £ " + "0" + " | \n | Total amount | £ " + totalAmountWithInterest + " |";
        caseDataBuilder.calculatedInterest(calculateInterest);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    //calculate fee for specified claim
    private CallbackResponse calculateSpecFee(CallbackParams callbackParams) {
        return createClaimFeeCalculator.calculateFee(callbackParams, true);
    }

    private CallbackResponse validateSpecRespondentRepEmail(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(validateEmailService.validate(caseData.getRespondentSolicitor1EmailAddress()))
            .build();
    }
}
