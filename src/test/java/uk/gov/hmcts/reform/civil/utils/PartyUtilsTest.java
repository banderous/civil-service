package uk.gov.hmcts.reform.civil.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.LitigationFriend;
import uk.gov.hmcts.reform.civil.model.Party;
import uk.gov.hmcts.reform.civil.model.SolicitorReferences;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.PartyBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.civil.sampledata.PartyBuilder.DATE_OF_BIRTH;

class PartyUtilsTest {

    @Nested
    class PartyNameBasedOnType {
        @Test
        void shouldThrowNullPointer_whenPartyTypeIsNull() {
            Party party = Party.builder().type(null).build();
            assertThrows(NullPointerException.class, () -> PartyUtils.getPartyNameBasedOnType(party));
        }

        @Test
        void shouldProvideName_whenPartyTypeIsIndividual() {
            Party individual = Party.builder()
                .individualTitle("Mr")
                .individualFirstName("Jacob")
                .individualLastName("Martin")
                .type(Party.Type.INDIVIDUAL).build();

            assertEquals("Mr Jacob Martin", PartyUtils.getPartyNameBasedOnType(individual));
        }

        @Test
        void shouldProvideName_whenPartyTypeIsIndividualWithoutTitle() {
            Party individual = Party.builder()
                .individualFirstName("Jacob")
                .individualLastName("Martin")
                .type(Party.Type.INDIVIDUAL).build();

            assertEquals("Jacob Martin", PartyUtils.getPartyNameBasedOnType(individual));
        }

        @Test
        void shouldProvideName_whenPartyTypeIsCompany() {
            Party individual = Party.builder()
                .companyName("XYZ Company House")
                .type(Party.Type.COMPANY).build();

            assertEquals("XYZ Company House", PartyUtils.getPartyNameBasedOnType(individual));
        }

        @Test
        void shouldProvideName_whenPartyTypeIsOrganisation() {
            Party organisation = Party.builder()
                .organisationName("ABC Solutions")
                .type(Party.Type.ORGANISATION).build();

            assertEquals("ABC Solutions", PartyUtils.getPartyNameBasedOnType(organisation));
        }

        @Test
        void shouldProvideName_whenPartyTypeIsSoleTrader() {
            Party soleTrader = Party.builder()
                .soleTraderTitle("Mr")
                .soleTraderFirstName("Jacob")
                .soleTraderLastName("Martin")
                .type(Party.Type.SOLE_TRADER).build();

            assertEquals("Mr Jacob Martin", PartyUtils.getPartyNameBasedOnType(soleTrader));
        }
    }

    @Nested
    class LitigiousPartyName {

        @Test
        void shouldThrowNullPointer_whenPartyTypeIsNull() {
            Party party = Party.builder().type(null).build();
            LitigationFriend litigationFriend = LitigationFriend.builder().build();
            assertThrows(NullPointerException.class, () -> PartyUtils.getLitigiousPartyName(party, litigationFriend));
        }

        @Test
        void shouldProvideName_whenPartyTypeIsIndividualAndNoLitigationFriend() {
            Party individual = Party.builder()
                .individualTitle("Mr")
                .individualFirstName("Jacob")
                .individualLastName("Martin")
                .type(Party.Type.INDIVIDUAL).build();

            assertEquals("Mr Jacob Martin", PartyUtils.getLitigiousPartyName(individual, null));
        }

        @Test
        void shouldProvideName_whenPartyTypeIsIndividualAndLitigationFriend() {
            Party individual = Party.builder()
                .individualTitle("Mr")
                .individualFirstName("Jacob")
                .individualLastName("Martin")
                .type(Party.Type.INDIVIDUAL).build();
            LitigationFriend litigationFriend = LitigationFriend.builder().fullName("Mr Litigious Friend").build();
            assertEquals("Mr Jacob Martin L/F Mr Litigious Friend",
                         PartyUtils.getLitigiousPartyName(individual, litigationFriend));
        }

        @Test
        void shouldProvideName_whenPartyTypeIsCompany() {
            Party individual = Party.builder()
                .companyName("XYZ Company House")
                .type(Party.Type.COMPANY).build();

            assertEquals("XYZ Company House", PartyUtils.getLitigiousPartyName(individual, null));
        }

        @Test
        void shouldProvideName_whenPartyTypeIsOrganisation() {
            Party organisation = Party.builder()
                .organisationName("ABC Solutions")
                .type(Party.Type.ORGANISATION).build();

            assertEquals("ABC Solutions", PartyUtils.getLitigiousPartyName(organisation, null));
        }

        @Test
        void shouldProvideName_whenPartyTypeIsSoleTrader() {
            Party soleTrader = Party.builder()
                .soleTraderTitle("Mr")
                .soleTraderFirstName("Jacob")
                .soleTraderLastName("Martin")
                .type(Party.Type.SOLE_TRADER).build();

            assertEquals("Mr Jacob Martin", PartyUtils.getLitigiousPartyName(soleTrader, null));
        }

        @Test
        void shouldProvideName_whenPartyTypeIsSoleTraderAndTradingAs() {
            Party soleTrader = Party.builder()
                .soleTraderTitle("Mr")
                .soleTraderFirstName("Jacob")
                .soleTraderLastName("Martin")
                .soleTraderTradingAs("Trading Co")
                .type(Party.Type.SOLE_TRADER).build();

            assertEquals("Mr Jacob Martin T/A Trading Co",
                         PartyUtils.getLitigiousPartyName(soleTrader, null));
        }
    }

    @Nested
    class PartyDateOfBirth {

        @ParameterizedTest
        @EnumSource(value = Party.Type.class, names = {"SOLE_TRADER", "INDIVIDUAL"})
        void shouldReturnDateOfBirth_whenPartyTypeIsIndividualOrSoleTrader(Party.Type type) {
            Party party = PartyBuilder.builder().ofType(type).build();

            assertThat(PartyUtils.getDateOfBirth(party)).contains(DATE_OF_BIRTH);
        }

        @ParameterizedTest
        @EnumSource(value = Party.Type.class, mode = EnumSource.Mode.EXCLUDE, names = {"SOLE_TRADER", "INDIVIDUAL"})
        void shouldReturnEmpty_whenPartyTypeIsNotIndividualOrSoleTrader(Party.Type type) {
            Party party = PartyBuilder.builder().ofType(type).build();

            assertThat(PartyUtils.getDateOfBirth(party)).isEmpty();
        }
    }

    @Nested
    class PartyReferences {

        @Test
        void shouldReturnReferences_whenNoRefsAvailable() {
            CaseData caseData = CaseDataBuilder.builder().build();

            String partyReferences = PartyUtils.buildPartiesReferences(caseData);

            assertEquals("", partyReferences);
        }

        @Test
        void shouldReturnReferences_whenCaseHasOneDefendantSolicitorAndBothRefsAvailable() {
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().build();

            String partyReferences = PartyUtils.buildPartiesReferences(caseData);

            assertEquals("Claimant reference: 12345\nDefendant reference: 6789", partyReferences);
        }

        @Test
        void shouldReturnReferences_when1v2DiffSolicitorAndOnlyClaimantRefAndSol2RefAvailable() {
            CaseData caseData = CaseDataBuilder.builder().build().toBuilder()
                .solicitorReferences(SolicitorReferences.builder().applicantSolicitor1Reference("App One").build())
                .respondentSolicitor2Reference("Def Two")
                .build();

            String partyReferences = PartyUtils.buildPartiesReferences(caseData);

            assertEquals("Claimant reference: App One\nDefendant 2 reference: Def Two", partyReferences);
        }

        @Test
        void shouldReturnReferences_when1v2DiffSolicitorCase() {
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().build().toBuilder()
                .respondentSolicitor2Reference("defendant sol 2")
                .build();

            String partyReferences = PartyUtils.buildPartiesReferences(caseData);

            assertEquals("Claimant reference: 12345\nDefendant 1 reference: 6789\nDefendant 2 reference: "
                             + "defendant sol 2",
                         partyReferences);
        }

    }

    @Nested
    class ClaimantReferences {

        @Test
        void shouldReturnEmptyReferences_whenNoRefsAvailable() {
            CaseData caseData = CaseDataBuilder.builder().build();

            String partyReferences = PartyUtils.buildClaimantReference(caseData);

            assertEquals("", partyReferences);
        }

        @Test
        void shouldReturnClaimaintReferences_whenCaseHasRefsAvailable() {
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().build();

            String partyReferences = PartyUtils.buildClaimantReference(caseData);

            assertEquals("Claimant reference: 12345", partyReferences);
        }
    }
}
