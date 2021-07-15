package uk.gov.hmcts.reform.civil.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class OrdnanceSurveyConfiguration {

    private final String apiKey;

    public OrdnanceSurveyConfiguration(@Value("${ordnance_survey.api.key}") String apiKey) {
        this.apiKey = apiKey;
    }
}
