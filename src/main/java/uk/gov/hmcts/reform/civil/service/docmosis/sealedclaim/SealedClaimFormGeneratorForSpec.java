package uk.gov.hmcts.reform.civil.service.docmosis.sealedclaim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.ClaimAmountBreakup;
import uk.gov.hmcts.reform.civil.model.ClaimAmountBreakupDetails;
import uk.gov.hmcts.reform.civil.model.SolicitorReferences;
import uk.gov.hmcts.reform.civil.model.TimelineOfEventDetails;
import uk.gov.hmcts.reform.civil.model.TimelineOfEvents;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.common.SpecifiedParty;
import uk.gov.hmcts.reform.civil.model.docmosis.sealedclaim.SealedClaimFormForSpec;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.service.DeadlinesCalculator;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.RepresentativeService;
import uk.gov.hmcts.reform.civil.service.docmosis.TemplateDataGenerator;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;
import uk.gov.hmcts.reform.civil.utils.DocmosisTemplateDataUtils;
import uk.gov.hmcts.reform.civil.utils.InterestCalculator;
import uk.gov.hmcts.reform.civil.utils.MonetaryConversions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.DATE;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.formatLocalDate;
import static uk.gov.hmcts.reform.civil.model.interestcalc.InterestClaimOptions.BREAK_DOWN_INTEREST;
import static uk.gov.hmcts.reform.civil.model.interestcalc.InterestClaimOptions.SAME_RATE_INTEREST;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.N1;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.N2;

@Service
@RequiredArgsConstructor
public class SealedClaimFormGeneratorForSpec implements TemplateDataGenerator<SealedClaimFormForSpec> {

    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;
    private final RepresentativeService representativeService;
    private final InterestCalculator interestCalculator;
    public LocalDateTime localDateTime = LocalDateTime.now();
    private static final String END_OF_BUSINESS_DAY = "4pm, ";
    private final DeadlinesCalculator deadlinesCalculator;

    public CaseDocument generate(CaseData caseData, String authorisation) {
        SealedClaimFormForSpec templateData = getTemplateData(caseData);

        DocmosisDocument docmosisDocument = documentGeneratorService.generateDocmosisDocument(templateData, N2);
        return documentManagementService.uploadDocument(
            authorisation,
            new PDF(getFileName(caseData), docmosisDocument.getBytes(), DocumentType.SEALED_CLAIM)
        );
    }

    private String getFileName(CaseData caseData) {
        return String.format(N1.getDocumentTitle(), caseData.getLegacyCaseReference());
    }

    @Override
    public SealedClaimFormForSpec getTemplateData(CaseData caseData) {
        Optional<SolicitorReferences> solicitorReferences = ofNullable(caseData.getSolicitorReferences());
        BigDecimal interest = interestCalculator.calculateInterest(caseData);
        return SealedClaimFormForSpec.builder()
            .referenceNumber(caseData.getLegacyCaseReference())
            .caseName(DocmosisTemplateDataUtils.toCaseName.apply(caseData))
            .applicantExternalReference(solicitorReferences
                                            .map(SolicitorReferences::getApplicantSolicitor1Reference)
                                            .orElse(""))
            .respondentExternalReference(solicitorReferences
                                             .map(SolicitorReferences::getRespondentSolicitor1Reference)
                                             .orElse(""))
            .issueDate(caseData.getIssueDate())
            .submittedOn(caseData.getSubmittedDate().toLocalDate())
            .applicants(getApplicants(caseData))
            .respondents(getRespondents(caseData))
            .timeline(getTimeLine(caseData))
            .sameInterestRate(caseData.getInterestClaimOptions() != null
                                  ? caseData.getInterestClaimOptions().equals(SAME_RATE_INTEREST) + "" : null)
            .breakdownInterestRate(caseData.getInterestClaimOptions() != null
                                       ? caseData.getInterestClaimOptions().equals(BREAK_DOWN_INTEREST) + "" : null)
            .totalInterestAmount(interest != null ? interest.toString() : null)
            .howTheInterestWasCalculated(caseData.getInterestClaimOptions() != null
                                             ? caseData.getInterestClaimOptions().getDescription() : null)
            .interestRate(caseData.getSameRateInterestSelection() != null
                ? caseData.getSameRateInterestSelection().getDifferentRate() != null
                ? caseData.getSameRateInterestSelection().getDifferentRate() + "" :
                "8" : null)
            .interestExplanationText(caseData.getSameRateInterestSelection() != null
                                         ? caseData.getSameRateInterestSelection().getDifferentRate() != null
                ? caseData.getSameRateInterestSelection().getDifferentRateReason()
                : "The claimant reserves the right to claim interest under "
                + "Section 69 of the County Courts Act 1984" : null)
            .interestFromDate(caseData.getInterestFromSpecificDate() != null
                                  ? caseData.getInterestFromSpecificDate() :
                                  (isAfterFourPM()
                                      ? localDateTime.toLocalDate().plusDays(1) : localDateTime.toLocalDate()))
            .whenAreYouClaimingInterestFrom(caseData.getInterestClaimFrom() != null
                                                ? caseData.getInterestClaimFrom().name()
                .equals("FROM_CLAIM_SUBMIT_DATE")
                ? "From the date the claim was issued"
                : caseData.getInterestFromSpecificDateDescription() : null)
            .interestEndDate(isAfterFourPM() ? localDateTime.toLocalDate().plusDays(1) : localDateTime.toLocalDate())
            .interestEndDateDescription(caseData.getBreakDownInterestDescription() != null
                                            ? caseData.getBreakDownInterestDescription() + "" : null)
            .totalClaimAmount(caseData.getTotalClaimAmount() + "")
            .interestAmount(interest != null ? interest.toString() : null)
            .claimAmount(getClaimAmount(caseData))
            .claimFee(MonetaryConversions.penniesToPounds(caseData.getClaimFee().getCalculatedAmountInPence())
                          .toString())
            // Claim amount + interest + claim fees
            .totalAmountOfClaim(interest != null ? caseData.getTotalClaimAmount()
                .add(interest)
                .add(MonetaryConversions.penniesToPounds(caseData.getClaimFee()
                                                             .getCalculatedAmountInPence())).toString()
                                    : caseData.getTotalClaimAmount()
                .add(MonetaryConversions.penniesToPounds(caseData.getClaimFee()
                                                             .getCalculatedAmountInPence())).toString())
            .statementOfTruth(caseData.getApplicantSolicitor1ClaimStatementOfTruth())
            .descriptionOfClaim(caseData.getDetailsOfClaim())
            .applicantRepresentativeOrganisationName(representativeService.getApplicantRepresentative(caseData)
                                                         .getOrganisationName().toString())
            .defendantResponseDeadlineDate(getResponseDedline(caseData))
            .build();
    }

    private String getResponseDedline(CaseData caseData) {
        var notificationDeadline = formatLocalDate(
            deadlinesCalculator
                .calculateFirstWorkingDay(caseData.getIssueDate().plusDays(14)),
            DATE
        );
        return END_OF_BUSINESS_DAY + notificationDeadline;
    }

    private List<SpecifiedParty> getRespondents(CaseData caseData) {
        var respondent = caseData.getRespondent1();
        return List.of(SpecifiedParty.builder()
                           .name(respondent.getPartyName())
                           .primaryAddress(respondent.getPrimaryAddress())
                           .representative(representativeService.getRespondent1Representative(caseData))
                           .build());
    }

    private List<TimelineOfEventDetails> getTimeLine(CaseData caseData) {
        if (caseData.getTimelineOfEvents() != null) {
            List<TimelineOfEvents> timelineOfEvents = caseData.getTimelineOfEvents();
            List<TimelineOfEventDetails> timelineOfEventDetails = new ArrayList<TimelineOfEventDetails>();
            for (int index = 0; index < timelineOfEvents.size(); index++) {
                TimelineOfEventDetails timelineOfEventDetail
                    = new TimelineOfEventDetails(
                    timelineOfEvents.get(index).getValue()
                        .getTimelineDate(),
                    timelineOfEvents.get(index).getValue().getTimelineDescription()
                );
                timelineOfEventDetails.add(index, timelineOfEventDetail);
            }
            return timelineOfEventDetails;
        } else {
            return null;
        }
    }

    private List<ClaimAmountBreakupDetails> getClaimAmount(CaseData caseData) {
        if (caseData.getClaimAmountBreakup() != null) {
            List<ClaimAmountBreakup> claimAmountBreakup = caseData.getClaimAmountBreakup();
            List<ClaimAmountBreakupDetails> claimAmountBreakupDetails = new ArrayList<ClaimAmountBreakupDetails>();
            for (int index = 0; index < claimAmountBreakup.size(); index++) {
                ClaimAmountBreakupDetails claimAmountBreakupDetail
                    = new ClaimAmountBreakupDetails(
                    MonetaryConversions.penniesToPounds(claimAmountBreakup.get(index)
                                                            .getValue().getClaimAmount()),
                    claimAmountBreakup.get(index).getValue().getClaimReason()
                );
                claimAmountBreakupDetails.add(index, claimAmountBreakupDetail);
            }
            return claimAmountBreakupDetails;
        } else {
            return null;
        }
    }

    private List<SpecifiedParty> getApplicants(CaseData caseData) {
        var applicant = caseData.getApplicant1();
        return List.of(SpecifiedParty.builder()
                           .name(applicant.getPartyName())
                           .primaryAddress(applicant.getPrimaryAddress())
                           .representative(representativeService.getApplicantRepresentative(caseData))
                           .individualDateOfBirth(applicant.getIndividualDateOfBirth() != null
                                                      ? applicant.getIndividualDateOfBirth() : null)
                           .build());
    }

    private boolean isAfterFourPM() {
        LocalTime localTime = localDateTime.toLocalTime();
        return localTime.getHour() > 15;
    }
}
