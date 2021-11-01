package uk.gov.hmcts.reform.civil.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;


@RequiredArgsConstructor
public class LaunchDarklyLRSpecToggle implements Condition {


    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        //FeatureToggleService featureToggleService = context.getBeanFactory().getBean(FeatureToggleService.class);
        return true;
    }
}
