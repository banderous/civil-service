package uk.gov.hmcts.reform.ordnancesurvey.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.ordnancesurvey.model.AddressSearchResults;

@FeignClient(name = "ordnance-survey-api", url = "${ordnance_survey.api.url}")
public interface OrdnanceSurveyApi {

    @GetMapping("find")
    AddressSearchResults findAddress(
        @RequestParam("key") String apiKey,
        @RequestParam("query") String address,
        @RequestParam("minmatch") double minMatch
    );
}
