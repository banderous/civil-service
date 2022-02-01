package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.MultiPartyScenario;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GAStatementOfTruth;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@Service
@RequiredArgsConstructor
public class InitiateGeneralApplicationService {

    public CaseData buildCaseData(CaseData.CaseDataBuilder dataBuilder, CaseData caseData, UserDetails userDetails) {
        List<Element<GeneralApplication>> applications = addApplication(buildApplication(caseData, userDetails),
                                                                        caseData.getGeneralApplications());
        return dataBuilder
            .generalApplications(applications)
            .generalAppType(GAApplicationType.builder().build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().build())
            .generalAppPBADetails(GAPbaDetails.builder().build())
            .generalAppDetailsOfOrder(EMPTY)
            .generalAppReasonsOfOrder(EMPTY)
            .generalAppInformOtherParty(GAInformOtherParty.builder().build())
            .generalAppUrgencyRequirement(GAUrgencyRequirement.builder().build())
            .generalAppStatementOfTruth(GAStatementOfTruth.builder().build())
            .generalAppHearingDetails(GAHearingDetails.builder().build())
            .generalAppEvidenceDocument(java.util.Collections.emptyList())
            .build();
    }

    private GeneralApplication buildApplication(CaseData caseData, UserDetails userDetails) {
        InitiateGeneralApplicationServiceHelper helper = new InitiateGeneralApplicationServiceHelper();
        GeneralApplication.GeneralApplicationBuilder applicationBuilder = GeneralApplication.builder();
        if (caseData.getGeneralAppEvidenceDocument() != null) {
            applicationBuilder.generalAppEvidenceDocument(caseData.getGeneralAppEvidenceDocument());
        }
        if (MultiPartyScenario.isMultiPartyScenario(caseData)) {
            applicationBuilder.isMultiParty(YES);
        } else {
            applicationBuilder.isMultiParty(NO);
        }

        boolean isGAApplicantSameAsParentCaseApplicant = helper.isGA_ApplicantSameAsPC_Applicant(caseData, userDetails);

        boolean isGAApplicantSameAsParentCaseRespondent = helper
            .isGA_ApplicantSameAsPC_Respondent(caseData, userDetails);

        boolean isGAApplicantOrgSameAsPCRespondentOrg = helper
            .isGA_ApplicantOrgSameAsPC_RespondentOrg(caseData, userDetails);

        if (isGAApplicantSameAsParentCaseApplicant
            && isGAApplicantSameAsParentCaseRespondent) {
            throw new IllegalArgumentException("A General Application should have only one applicant");
        }

        if (!isGAApplicantSameAsParentCaseApplicant
            && !isGAApplicantSameAsParentCaseRespondent) {
            throw new IllegalArgumentException("A General Application should have only one applicant");
        }

        return applicationBuilder
            .businessProcess(BusinessProcess.ready(INITIATE_GENERAL_APPLICATION))
            .applicantSolicitor1UserDetails(isGAApplicantSameAsParentCaseApplicant
                                                ? caseData.getApplicantSolicitor1UserDetails()
                                                : helper.constructRespondent1SolicitorUserDetails(userDetails))
            .applicant1OrganisationPolicy(isGAApplicantSameAsParentCaseApplicant
                                              ? caseData.getApplicant1OrganisationPolicy()
                                              : caseData.getRespondent1OrganisationPolicy())
            .respondent1OrganisationPolicy(isGAApplicantOrgSameAsPCRespondentOrg
                                               ? caseData.getApplicant1OrganisationPolicy()
                                               : caseData.getRespondent1OrganisationPolicy())
            .respondentSolicitor1EmailAddress(isGAApplicantSameAsParentCaseRespondent
                                                  ? caseData.getApplicantSolicitor1UserDetails().getEmail()
                                                  : caseData.getRespondentSolicitor1EmailAddress())
            .generalAppType(caseData.getGeneralAppType())
            .generalAppRespondentAgreement(caseData.getGeneralAppRespondentAgreement())
            .generalAppPBADetails(caseData.getGeneralAppPBADetails())
            .generalAppDetailsOfOrder(caseData.getGeneralAppDetailsOfOrder())
            .generalAppReasonsOfOrder(caseData.getGeneralAppReasonsOfOrder())
            .generalAppInformOtherParty(caseData.getGeneralAppInformOtherParty())
            .generalAppUrgencyRequirement(caseData.getGeneralAppUrgencyRequirement())
            .generalAppStatementOfTruth(caseData.getGeneralAppStatementOfTruth())
            .generalAppHearingDetails(caseData.getGeneralAppHearingDetails())
            .build();
    }

    private List<Element<GeneralApplication>> addApplication(GeneralApplication application,
                                                            List<Element<GeneralApplication>>
                                                                generalApplicationDetails) {
        List<Element<GeneralApplication>> newApplication = ofNullable(generalApplicationDetails).orElse(newArrayList());
        newApplication.add(element(application));

        return newApplication;
    }
}
