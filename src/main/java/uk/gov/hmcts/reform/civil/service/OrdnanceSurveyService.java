package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.config.ExitSurveyConfiguration;
import uk.gov.hmcts.reform.civil.config.OrdnanceSurveyConfiguration;
import uk.gov.hmcts.reform.ordnancesurvey.client.OrdnanceSurveyApi;
import uk.gov.hmcts.reform.ordnancesurvey.model.AddressSearchResults;

@Service
@RequiredArgsConstructor
public class OrdnanceSurveyService {

    private final OrdnanceSurveyConfiguration ordnanceSurveyConfiguration;
    private final OrdnanceSurveyApi ordnanceSurveyApi;

    public AddressSearchResults findAddress(String address) {
        return ordnanceSurveyApi.findAddress(ordnanceSurveyConfiguration.getApiKey(), address, 1.0);
    }
}
