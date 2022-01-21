package uk.gov.hmcts.reform.civil.ras.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@FeignClient(name = "ras-api", url = "${role-assignment-service.api.url}")
public interface RoleAssignmentApi {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @GetMapping(
        value = "/am/role-assignments/actors/{actorId}",
        consumes = APPLICATION_JSON_VALUE,
        headers = CONTENT_TYPE + "=" + APPLICATION_JSON_VALUE
    )
    String getRoleAssignments(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @PathVariable("actorId") String actorId);

}
