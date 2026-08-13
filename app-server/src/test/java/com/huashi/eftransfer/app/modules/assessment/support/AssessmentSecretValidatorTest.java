package com.huashi.eftransfer.app.modules.assessment.support;

import com.huashi.eftransfer.shared.security.SecretPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssessmentSecretValidatorTest {

    @Test
    void shouldAllowInRepoDefaultsOnLocalAndTestProfiles() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        AssessmentSecretValidator validator = new AssessmentSecretValidator(
                "local-only-change-this-participant-code-secret",
                "local-only-change-this-profile-key",
                environment
        );

        assertThatCode(validator::validateConfiguration).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectInRepoDefaultsOutsideLocalAndTest() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        AssessmentSecretValidator validator = new AssessmentSecretValidator(
                "local-only-change-this-participant-code-secret",
                "x7Pq2Lk9Vd4Nc8Rs1Tf6Yh3Jm5Bw0QeZ",
                environment
        );

        assertThatThrownBy(validator::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_ASSESSMENT_CODE_HMAC_SECRET");
    }

    @Test
    void shouldRejectMissingHighEntropyProfileKeyOnProd() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        AssessmentSecretValidator validator = new AssessmentSecretValidator(
                "x7Pq2Lk9Vd4Nc8Rs1Tf6Yh3Jm5Bw0QeZ",
                "replace-me-with-a-32-byte-minimum-profile-key",
                environment
        );

        assertThatThrownBy(validator::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_ASSESSMENT_SENSITIVE_PROFILE_KEY");
    }

    @Test
    void shouldAcceptHighEntropySecretsOnProd() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        AssessmentSecretValidator validator = new AssessmentSecretValidator(
                "x7Pq2Lk9Vd4Nc8Rs1Tf6Yh3Jm5Bw0QeZ",
                "m4Cs8Wy1Qp6Jh2Vr9Tk3Nz7Lb5Dx0FuG",
                environment
        );

        assertThatCode(validator::validateConfiguration).doesNotThrowAnyException();
    }
}
