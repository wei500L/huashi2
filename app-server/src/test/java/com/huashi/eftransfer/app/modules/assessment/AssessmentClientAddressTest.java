package com.huashi.eftransfer.app.modules.assessment;

import com.huashi.eftransfer.app.common.security.ClientRequestContextResolver;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentClientIpNormalizer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AssessmentClientAddressTest {

    @Test
    void shouldResolveTheFirstTrustedProxyAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.9, 172.22.0.1");
        request.addHeader("X-Real-IP", "172.22.0.1");
        request.setRemoteAddr("127.0.0.1");

        assertThat(ClientRequestContextResolver.resolveIpAddress(request)).isEqualTo("203.0.113.9");
    }

    @Test
    void shouldNormalizeIpv4AndIpv6AndRejectUnknownAddresses() {
        assertThat(AssessmentClientIpNormalizer.normalize("203.000.113.009")).isEqualTo("203.0.113.9");
        assertThat(AssessmentClientIpNormalizer.normalize("2001:db8::42")).isEqualTo("2001:db8:0:0:0:0:0:42");
        assertThat(AssessmentClientIpNormalizer.normalize("unknown")).isNull();
        assertThat(AssessmentClientIpNormalizer.normalize("999.1.1.1")).isNull();
        assertThat(AssessmentClientIpNormalizer.normalize("example.com")).isNull();
    }
}
