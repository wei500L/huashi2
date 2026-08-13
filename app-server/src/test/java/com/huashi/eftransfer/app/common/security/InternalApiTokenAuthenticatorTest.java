package com.huashi.eftransfer.app.common.security;

import com.huashi.eftransfer.app.common.config.InternalApiProperties;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalApiTokenAuthenticatorTest {

    @Test
    void shouldAllowRequestsWhenInternalApiProtectionIsDisabledOnRelaxedProfile() {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setEnabled(false);

        InternalApiTokenAuthenticator authenticator = new InternalApiTokenAuthenticator(properties, environment("test"));

        assertThatCode(authenticator::validateConfiguration).doesNotThrowAnyException();
        assertThatCode(() -> authenticator.authenticate(null)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectDisabledProtectionOutsideLocalAndTest() {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setEnabled(false);

        InternalApiTokenAuthenticator authenticator = new InternalApiTokenAuthenticator(properties, environment("prod"));

        assertThatThrownBy(authenticator::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("platform.internal-api.enabled");
    }

    @Test
    void shouldRejectMissingTokenConfigurationWhenProtectionIsEnabled() {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setEnabled(true);
        properties.setToken("");

        InternalApiTokenAuthenticator authenticator = new InternalApiTokenAuthenticator(properties, environment("test"));

        assertThatThrownBy(authenticator::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("platform.internal-api.token");
    }

    @Test
    void shouldRejectPlaceholderTokenOutsideLocalAndTest() {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setEnabled(true);
        properties.setToken("replace-with-shared-internal-token");

        InternalApiTokenAuthenticator authenticator = new InternalApiTokenAuthenticator(properties, environment("prod"));

        assertThatThrownBy(authenticator::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("platform.internal-api.token");
    }

    @Test
    void shouldAuthenticateWhenTokenMatches() {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setEnabled(true);
        properties.setToken("expected-internal-token");

        InternalApiTokenAuthenticator authenticator = new InternalApiTokenAuthenticator(properties, environment("test"));

        assertThatCode(authenticator::validateConfiguration).doesNotThrowAnyException();
        assertThatCode(() -> authenticator.authenticate("expected-internal-token")).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectInvalidToken() {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setEnabled(true);
        properties.setToken("expected-internal-token");

        InternalApiTokenAuthenticator authenticator = new InternalApiTokenAuthenticator(properties, environment("test"));

        assertThatThrownBy(() -> authenticator.authenticate("wrong-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Invalid internal API token");
    }

    private static MockEnvironment environment(String profile) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        return environment;
    }
}
