package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.civil.config.TestUserConfiguration;
import uk.gov.hmcts.reform.civil.model.RoleAssignmentResponse;
import uk.gov.hmcts.reform.civil.ras.client.RoleAssignmentsApi;
import uk.gov.hmcts.reform.idam.client.IdamClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleAssignmentsService {

    Logger log = LoggerFactory.getLogger(RoleAssignmentsService.class);

    private final RoleAssignmentsApi roleAssignmentApi;
    private final IdamClient idamClient;
    private final TestUserConfiguration userConfig;
    private final AuthTokenGenerator authTokenGenerator;

    public List<RoleAssignmentResponse.RoleAssignment> getRoleAssignmentList(String actorId) throws JsonProcessingException {
        if (log.isDebugEnabled()) {
            log.debug(actorId, "Getting Role assignments for actorId {0}");
        }
        String jsonResponse = roleAssignmentApi.getRoleAssignments(
            getUserToken(),
            authTokenGenerator.generate(),
            actorId
        );
        ObjectMapper mapper = JsonMapper.builder()
            .findAndAddModules().build();
        RoleAssignmentResponse response = mapper.readValue(jsonResponse, RoleAssignmentResponse.class);
        return response.getRoleAssignmentResponse();
    }

    public List<RoleAssignmentResponse.RoleAssignment> getRoleAssignmentList() throws JsonProcessingException {
        return this.getRoleAssignmentList(getActorId());
    }


    private String getActorId() {
        return idamClient.getUserDetails(getUserToken()).getId();
    }

    private String getUserToken() {
        return idamClient.getAccessToken(userConfig.getUsername(), userConfig.getPassword());
    }
}
