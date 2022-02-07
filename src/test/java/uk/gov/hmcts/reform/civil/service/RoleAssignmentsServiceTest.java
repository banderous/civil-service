package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.civil.config.TestUserConfiguration;
import uk.gov.hmcts.reform.civil.model.RoleAssignmentResponse;
import uk.gov.hmcts.reform.civil.ras.client.RoleAssignmentsApi;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    RoleAssignmentsService.class,
    JacksonAutoConfiguration.class
})
class RoleAssignmentsServiceTest {

    private static final String USER_AUTH_TOKEN = "Bearer user-xyz";
    private static final String SERVICE_AUTH_TOKEN = "Bearer service-xyz";
    private static final String CASE_TYPE = "CIVIL";
    private static final String RAS_RESPONSE = "{\"roleAssignmentResponse\":[{\"id\":\"b034b492-d2e9-4920-8e72" +
        "-6cac42e43dfc\",\"actorIdType\":\"IDAM\",\"actorId\":\"1d70e58a-73ab-4423-83b0-9cd82a810250\"," +
        "\"roleType\":\"ORGANISATION\",\"roleName\":\"senior-tribunal-caseworker\",\"classification\":\"PUBLIC\"," +
        "\"grantType\":\"STANDARD\",\"roleCategory\":\"LEGAL_OPERATIONS\",\"readOnly\":false," +
        "\"created\":\"2022-01-17T17:52:57.478732Z\",\"attributes\":{\"substantive\":\"Y\"," +
        "\"primaryLocation\":\"123999\",\"jurisdiction\":\"IA\"},\"authorisations\":[\"QA\"]}]}";

    @MockBean
    private TestUserConfiguration userConfig;

    @MockBean
    private RoleAssignmentsApi roleAssignmentsApi;

    @MockBean
    private IdamClient idamClient;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleAssignmentsService roleAssignmentsService;

    @BeforeEach
    void init() {
        clearInvocations(authTokenGenerator);
        clearInvocations(idamClient);
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        when(idamClient.getAccessToken(userConfig.getUsername(), userConfig.getPassword())).thenReturn(USER_AUTH_TOKEN);
        when(idamClient.getUserDetails(USER_AUTH_TOKEN)).thenReturn(UserDetails.builder().id("1").build());
        when(roleAssignmentsApi.getRoleAssignments(anyString(), anyString(), anyString())).thenReturn(RAS_RESPONSE);
    }



    @Nested
    class GetRoleAssignments {

        @Rule
        public ExpectedException exceptionRule = ExpectedException.none();

        @Test
        void shouldReturnRoleAssignment_WhenGoodActorIdInvoked() throws JsonProcessingException {
            RoleAssignmentResponse.RoleAssignment roleAssignment = RoleAssignmentResponse.RoleAssignment.builder()
                .id("b034b492-d2e9-4920-8e72-6cac42e43dfc")
                .actorIdType("IDAM")
                .actorId("1d70e58a-73ab-4423-83b0-9cd82a810250")
                .roleType("ORGANISATION")
                .roleName("senior-tribunal-caseworker")
                .classification("PUBLIC")
                .grantType("STANDARD")
                .roleCategory("LEGAL_OPERATIONS")
                .readOnly(false)
                .created(OffsetDateTime.parse("2022-01-17T17:52:57.478732Z").toLocalDate())
                .attributes(RoleAssignmentResponse.RoleAssignment.Attributes.builder()
                                .substantive("Y")
                                .primaryLocation("123999")
                                .jurisdiction("IA")
                                .build())
                .authorisations(Arrays.asList("QA"))
                .build();
            when(roleAssignmentsApi.getRoleAssignments(USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, "1"))
                .thenReturn(RAS_RESPONSE);

            List<RoleAssignmentResponse.RoleAssignment> response = roleAssignmentsService.getRoleAssignmentList("1");
            RoleAssignmentResponse.RoleAssignment roleAssignment1 = response.get(0);
            assertThat(roleAssignment1).isEqualTo(roleAssignment);
            verify(idamClient).getAccessToken(userConfig.getUsername(), userConfig.getPassword());
        }

        @Test
        void shouldThrowException_WhenBadActorIdInvoked() {
            when(roleAssignmentsApi.getRoleAssignments(USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, "1"))
                .thenReturn("");

            Assert.assertThrows(JsonProcessingException.class, () -> roleAssignmentsService.getRoleAssignmentList("1"));

            verify(idamClient).getAccessToken(userConfig.getUsername(), userConfig.getPassword());
        }
    }
}
