package com.huashi.eftransfer.app.common.security;

import com.huashi.eftransfer.app.common.config.InternalApiProperties;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalApiTokenAuthenticatorTest {

    @Test
    void shouldAllowRequestsWhenInternalApiProtectionIsDisabled() {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setEnabled(false);

        InternalApiTokenAuthenticator authenticator = new InternalApiTokenAuthenticator(properties);

        assertThatCode(authenticator::validateConfiguration).doesNotThrowAnyException();
        assertThatCode(() -> authenticator.authenticate(null)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectMissingTokenConfigurationWhenProtectionIsEnabled() {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setEnabled(true);
        properties.setToken("");

        InternalApiTokenAuthenticator authenticator = new InternalApiTokenAuthenticator(properties);

        assertThatThrownBy(authenticator::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("platform.internal-api.token");
    }

    @Test
    void shouldAuthenticateWhenTokenMatches() {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setEnabled(true);
        properties.setToken("expected-internal-token");

        InternalApiTokenAuthenticator authenticator = new InternalApiTokenAuthenticator(properties);

        assertThatCode(authenticator::validateConfiguration).doesNotThrowAnyException();
        assertThatCode(() -> authenticator.authenticate("expected-internal-token")).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectInvalidToken() {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setEnabled(true);
        properties.setToken("expected-internal-token");

        InternalApiTokenAuthenticator authenticator = new InternalApiTokenAuthenticator(properties);

        assertThatThrownBy(() -> authenticator.authenticate("wrong-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Invalid internal API token");
    }
}
