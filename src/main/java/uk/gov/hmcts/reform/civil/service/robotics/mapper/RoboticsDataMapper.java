package uk.gov.hmcts.reform.civil.service.robotics.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.model.Organisation;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.enums.AllocatedTrack;
import uk.gov.hmcts.reform.civil.model.Address;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.LitigationFriend;
import uk.gov.hmcts.reform.civil.model.Party;
import uk.gov.hmcts.reform.civil.model.SolicitorOrganisationDetails;
import uk.gov.hmcts.reform.civil.model.SolicitorReferences;
import uk.gov.hmcts.reform.civil.model.robotics.CaseHeader;
import uk.gov.hmcts.reform.civil.model.robotics.ClaimDetails;
import uk.gov.hmcts.reform.civil.model.robotics.LitigiousParty;
import uk.gov.hmcts.reform.civil.model.robotics.RoboticsAddresses;
import uk.gov.hmcts.reform.civil.model.robotics.RoboticsCaseData;
import uk.gov.hmcts.reform.civil.model.robotics.Solicitor;
import uk.gov.hmcts.reform.civil.service.OrganisationService;
import uk.gov.hmcts.reform.civil.utils.PartyUtils;
import uk.gov.hmcts.reform.prd.model.ContactInformation;
import uk.gov.hmcts.reform.prd.model.DxAddress;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static io.jsonwebtoken.lang.Collections.isEmpty;
import static java.time.format.DateTimeFormatter.ISO_DATE;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.MonetaryConversions.penniesToPounds;

@Service
@RequiredArgsConstructor
public class RoboticsDataMapper {

    public static final String APPLICANT_SOLICITOR_ID = "001";
    public static final String RESPONDENT_SOLICITOR_ID = "002";
    public static final String APPLICANT_ID = "001";
    public static final String RESPONDENT_ID = "002";

    public static final String RESPONDENT2_ID = "003";
    public static final String APPLICANT2_ID = "004";
    public static final String RESPONDENT2_SOLICITOR_ID = "003";

    private final RoboticsAddressMapper addressMapper;
    private final EventHistoryMapper eventHistoryMapper;
    private final OrganisationService organisationService;

    public RoboticsCaseData toRoboticsCaseData(CaseData caseData) {
        requireNonNull(caseData);
        return RoboticsCaseData.builder()
            .header(buildCaseHeader(caseData))
            .litigiousParties(buildLitigiousParties(caseData))
            .solicitors(buildSolicitors(caseData))
            .claimDetails(buildClaimDetails(caseData))
            .events(eventHistoryMapper.buildEvents(caseData))
            .build();
    }

    private ClaimDetails buildClaimDetails(CaseData caseData) {
        return ClaimDetails.builder()
            .amountClaimed(caseData.getClaimValue().toPounds())
            .courtFee(ofNullable(caseData.getClaimFee())
                          .map(fee -> penniesToPounds(fee.getCalculatedAmountInPence()))
                          .orElse(null))
            .caseIssuedDate(ofNullable(caseData.getIssueDate())
                                .map(issueDate -> issueDate.format(ISO_DATE))
                                .orElse(null))
            .caseRequestReceivedDate(caseData.getSubmittedDate().toLocalDate().format(ISO_DATE))
            .build();
    }

    private CaseHeader buildCaseHeader(CaseData caseData) {
        return CaseHeader.builder()
            .caseNumber(caseData.getLegacyCaseReference())
            .owningCourtCode("390")
            .owningCourtName("CCMCC")
            .caseType("PERSONAL INJURY")
            .preferredCourtCode(caseData.getCourtLocation().getApplicantPreferredCourt())
            .caseAllocatedTo(buildAllocatedTrack(caseData.getAllocatedTrack()))
            .build();
    }

    private String buildAllocatedTrack(AllocatedTrack allocatedTrack) {
        switch (allocatedTrack) {
            case FAST_CLAIM:
                return "FAST TRACK";
            case MULTI_CLAIM:
                return "MULTI TRACK";
            case SMALL_CLAIM:
                return "SMALL CLAIM TRACK";
            default:
                return "";
        }
    }

    private List<Solicitor> buildSolicitors(CaseData caseData) {
        List<Solicitor> solicitorsList = new ArrayList<>();
        solicitorsList.add(buildApplicantSolicitor(caseData, APPLICANT_SOLICITOR_ID));
        ofNullable(buildRespondentSolicitor(caseData, RESPONDENT_SOLICITOR_ID))
            .ifPresent(solicitorsList::add);

        if (caseData.getRespondent2Represented() == YES) {
            String respondent2SolicitorId = caseData.getRespondent2SameLegalRepresentative() == YES
                ? RESPONDENT_SOLICITOR_ID : RESPONDENT2_SOLICITOR_ID;
            ofNullable(buildRespondent2Solicitor(caseData, respondent2SolicitorId))
                .ifPresent(solicitorsList::add);
        }
        return solicitorsList;
    }

    private Solicitor buildRespondentSolicitor(CaseData caseData, String id) {
        Solicitor.SolicitorBuilder solicitorBuilder = Solicitor.builder();
        Optional<String> organisationId = getOrganisationId(caseData.getRespondent1OrganisationPolicy());

        var organisationDetails = ofNullable(
            caseData.getRespondentSolicitor1OrganisationDetails()
        );
        if (organisationId.isEmpty() && organisationDetails.isEmpty()) {
            return null;
        }
        solicitorBuilder
            .id(id)
            .isPayee(false)
            .organisationId(organisationId.orElse(null))
            .reference(ofNullable(caseData.getSolicitorReferences())
                           .map(SolicitorReferences::getRespondentSolicitor1Reference)
                           .orElse(null)
            );

        organisationId
            .flatMap(organisationService::findOrganisationById)
            .ifPresent(buildOrganisation(solicitorBuilder, caseData.getRespondentSolicitor1ServiceAddress()));

        organisationDetails.ifPresent(buildOrganisationDetails(solicitorBuilder));

        return solicitorBuilder.build();
    }

    private Consumer<uk.gov.hmcts.reform.prd.model.Organisation> buildOrganisation(
        Solicitor.SolicitorBuilder solicitorBuilder, Address providedServiceAddress
    ) {
        return organisation -> {
            List<ContactInformation> contactInformation = organisation.getContactInformation();
            solicitorBuilder
                .name(organisation.getName())
                .addresses(fromProvidedAddress(contactInformation, providedServiceAddress))
                .contactDX(getContactDX(contactInformation));
        };
    }

    private RoboticsAddresses fromProvidedAddress(List<ContactInformation> contactInformation, Address provided) {
        return Optional.ofNullable(provided)
            .map(address -> addressMapper.toRoboticsAddresses(provided))
            .orElse(addressMapper.toRoboticsAddresses(contactInformation));
    }

    private String getContactDX(List<ContactInformation> contactInformation) {
        if (isEmpty(contactInformation)) {
            return null;
        }
        List<DxAddress> dxAddresses = contactInformation.get(0).getDxAddress();
        return isEmpty(dxAddresses) ? null : dxAddresses.get(0).getDxNumber();
    }

    private Optional<String> getOrganisationId(OrganisationPolicy respondent1OrganisationPolicy) {
        return ofNullable(respondent1OrganisationPolicy)
            .map(OrganisationPolicy::getOrganisation)
            .map(Organisation::getOrganisationID);
    }

    private Consumer<SolicitorOrganisationDetails> buildOrganisationDetails(
        Solicitor.SolicitorBuilder solicitorBuilder
    ) {
        return organisationDetails ->
            solicitorBuilder
                .name(organisationDetails.getOrganisationName())
                .contactTelephoneNumber(organisationDetails.getPhoneNumber())
                .contactFaxNumber(organisationDetails.getFax())
                .contactDX(organisationDetails.getDx())
                .contactEmailAddress(organisationDetails.getEmail())
                .addresses(ofNullable(organisationDetails.getAddress())
                               .map(addressMapper::toRoboticsAddresses)
                               .orElse(null)
                );
    }

    private Solicitor buildApplicantSolicitor(CaseData caseData, String id) {
        Optional<String> organisationId = getOrganisationId(caseData.getApplicant1OrganisationPolicy());

        Solicitor.SolicitorBuilder solicitorBuilder = Solicitor.builder()
            .id(id)
            .isPayee(true)
            .organisationId(organisationId.orElse(null))
            .contactEmailAddress(caseData.getApplicantSolicitor1UserDetails().getEmail())
            .reference(ofNullable(caseData.getSolicitorReferences())
                           .map(SolicitorReferences::getApplicantSolicitor1Reference)
                           .orElse(null)
            );

        organisationId
            .flatMap(organisationService::findOrganisationById)
            .ifPresent(buildOrganisation(solicitorBuilder, caseData.getApplicantSolicitor1ServiceAddress()));

        return solicitorBuilder.build();
    }

    private List<LitigiousParty> buildLitigiousParties(CaseData caseData) {
        String respondent1SolicitorId = caseData.getRespondent1Represented() == YES
            ? RESPONDENT_SOLICITOR_ID : null;

        var respondentParties = new ArrayList<>(List.of(
            buildLitigiousParty(
                caseData.getApplicant1(),
                caseData.getApplicant1LitigationFriend(),
                caseData.getApplicant1OrganisationPolicy(),
                "Claimant",
                APPLICANT_ID,
                APPLICANT_SOLICITOR_ID,
                caseData.getClaimDetailsNotificationDate()
            ),
            buildLitigiousParty(
                caseData.getRespondent1(),
                caseData.getRespondent1LitigationFriend(),
                caseData.getRespondent1OrganisationPolicy(),
                "Defendant",
                RESPONDENT_ID,
                respondent1SolicitorId,
                caseData.getClaimDetailsNotificationDate()
            )
        ));

        if (caseData.getApplicant2() != null) {
            respondentParties.add(buildLitigiousParty(
                caseData.getApplicant2(),
                caseData.getApplicant2LitigationFriend(),
                caseData.getApplicant2OrganisationPolicy(),
                "Claimant",
                APPLICANT2_ID,
                APPLICANT_SOLICITOR_ID,
                caseData.getClaimDetailsNotificationDate()
            ));
        }

        if (caseData.getRespondent2() != null) {
            String respondent2SolicitorId = null;
            if (caseData.getRespondent2Represented() == YES
                && caseData.getRespondent2SameLegalRepresentative() == YES) {
                respondent2SolicitorId = RESPONDENT_SOLICITOR_ID;
            } else if (caseData.getRespondent2Represented() == YES
                && caseData.getRespondent2SameLegalRepresentative() == NO) {
                respondent2SolicitorId = RESPONDENT2_SOLICITOR_ID;
            }
            respondentParties.add(buildLitigiousParty(
                caseData.getRespondent2(),
                caseData.getRespondent2LitigationFriend(),
                caseData.getRespondent2OrganisationPolicy(),
                "Defendant",
                RESPONDENT2_ID,
                respondent2SolicitorId,
                caseData.getClaimDetailsNotificationDate()
            ));
        }
        return respondentParties;
    }

    private LitigiousParty buildLitigiousParty(
        Party party,
        LitigationFriend litigationFriend,
        OrganisationPolicy organisationPolicy,
        String type,
        String id,
        String solicitorId,
        LocalDateTime claimDetailsNotificationDate
    ) {
        return LitigiousParty.builder()
            .id(id)
            .solicitorID(solicitorId)
            .type(type)
            .name(PartyUtils.getLitigiousPartyName(party, litigationFriend))
            .dateOfBirth(PartyUtils.getDateOfBirth(party).map(d -> d.format(ISO_DATE)).orElse(null))
            .addresses(addressMapper.toRoboticsAddresses(party.getPrimaryAddress()))
            .dateOfService(ofNullable(claimDetailsNotificationDate)
                               .map(LocalDateTime::toLocalDate)
                               .map(d -> d.format(ISO_DATE))
                               .orElse(null))
            .solicitorOrganisationID(getOrganisationId(organisationPolicy).orElse(null))
            .build();
    }

    private Solicitor buildRespondent2Solicitor(CaseData caseData, String id) {
        Solicitor.SolicitorBuilder solicitorBuilder = Solicitor.builder();
        Optional<String> organisationId = getOrganisationId(caseData.getRespondent2OrganisationPolicy());

        var organisationDetails = ofNullable(
            caseData.getRespondentSolicitor2OrganisationDetails()
        );
        if (organisationId.isEmpty() && organisationDetails.isEmpty()) {
            return null;
        }
        solicitorBuilder
            .id(id)
            .isPayee(false)
            .organisationId(organisationId.orElse(null))
            .reference(caseData.getRespondentSolicitor2Reference());

        organisationId
            .flatMap(organisationService::findOrganisationById)
            .ifPresent(buildOrganisation(solicitorBuilder, caseData.getRespondentSolicitor2ServiceAddress()));

        organisationDetails.ifPresent(buildOrganisationDetails(solicitorBuilder));

        return solicitorBuilder.build();
    }
}
