package uk.gov.hmcts.reform.civil.controllers.testingsupport;

import feign.FeignException;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.CaseDefinitionConstants;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CaseEvent.*;

@Api
@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnExpression("${testing.support.enabled:false}")
public class UpdateCaseDataController {

    private final CoreCaseDataService coreCaseDataService;

    @PutMapping("/testing-support/case/{caseId}")
    public void updateCaseData(
        @PathVariable("caseId") Long caseId,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
        @RequestBody Map<String, Object> caseDataMap) {
        try {
            var startEventResponse = coreCaseDataService.startUpdate(authorisation, caseId.toString(), UPDATE_CASE_DATA);
            coreCaseDataService.submitUpdate(authorisation, caseId.toString(), caseDataContent(startEventResponse, caseDataMap));
        } catch (FeignException e) {
            log.error(String.format("Updating case data failed: %s", e.contentUTF8()));
            throw e;
        }
    }

    @PostMapping("/testing-support/case")
    public void createCaseData(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
        @RequestBody Map<String, Object> caseDataMap) {
        try {
            var startEventResponse = coreCaseDataService.startCaseForCaseworker(CREATE_CLAIM.name(), CaseDefinitionConstants.CASE_TYPE, authorisation);
            coreCaseDataService.submitForCaseWorker(caseDataContent(startEventResponse, caseDataMap), CaseDefinitionConstants.CASE_TYPE, authorisation);
        } catch (FeignException e) {
            log.error(String.format("Creating case failed: %s", e.contentUTF8()));
            throw e;
        }
    }

    @PostMapping("/testing-support/case/apps")
    public void createAppsCaseData(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
        @RequestBody Map<String, Object> caseDataMap) {
        try {
            var startEventResponse = coreCaseDataService.startCaseForCaseworker(GENERAL_APPLICATION_CREATION.name(),
                                                                                CaseDefinitionConstants.GENERAL_APPLICATION_TYPE, authorisation);
            coreCaseDataService.submitForCaseWorker(caseDataContent(startEventResponse, caseDataMap), CaseDefinitionConstants.GENERAL_APPLICATION_TYPE, authorisation);
        } catch (FeignException e) {
            log.error(String.format("Creating case failed: %s", e.contentUTF8()));
            throw e;
        }
    }

    private CaseDataContent caseDataContent(StartEventResponse startEventResponse, Map<String, Object> caseDataMap) {
        Map<String, Object> data = startEventResponse.getCaseDetails().getData();
        data.putAll(caseDataMap);

        return CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(Event.builder().id(startEventResponse.getEventId()).build())
            .data(data)
            .build();
    }
}
