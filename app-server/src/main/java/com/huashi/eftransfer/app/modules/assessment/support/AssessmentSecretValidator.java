package com.huashi.eftransfer.app.modules.assessment.support;

import com.huashi.eftransfer.shared.security.SecretPolicy;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AssessmentSecretValidator {

    private final String hmacSecret;
    private final String sensitiveProfileKey;
    private final Environment environment;

    public AssessmentSecretValidator(
            @Value("${app.assessment.public-delivery.hmac-secret}") String hmacSecret,
            @Value("${app.assessment.sensitive-profile-key}") String sensitiveProfileKey,
            Environment environment
    ) {
        this.hmacSecret = hmacSecret;
        this.sensitiveProfileKey = sensitiveProfileKey;
        this.environment = environment;
    }

    @PostConstruct
    void validateConfiguration() {
        if (SecretPolicy.allowsInsecureDefaults(environment.getActiveProfiles())) {
            return;
        }
        SecretPolicy.validateHighEntropy(hmacSecret, "APP_ASSESSMENT_CODE_HMAC_SECRET");
        SecretPolicy.validateHighEntropy(sensitiveProfileKey, "APP_ASSESSMENT_SENSITIVE_PROFILE_KEY");
    }
}
