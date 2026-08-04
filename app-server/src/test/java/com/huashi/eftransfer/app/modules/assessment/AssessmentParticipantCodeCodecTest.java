package com.huashi.eftransfer.app.modules.assessment;

import com.huashi.eftransfer.app.modules.assessment.support.AssessmentParticipantCodeCodec;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;

class AssessmentParticipantCodeCodecTest {

    @Test
    void generatesExportCodeButOnlyStableDigestIsPersistable() {
        AssessmentParticipantCodeCodec codec = new AssessmentParticipantCodeCodec(
                "test-secret-with-at-least-32-characters", new SecureRandom());

        String code = codec.generate();
        String digest = codec.digest(code);

        assertThat(code).matches("^[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$");
        assertThat(digest).doesNotContain(code).hasSize(64);
        assertThat(codec.matches(code.toLowerCase(), digest)).isTrue();
        assertThat(codec.matches("AAAA-BBBB-CCCC", digest)).isFalse();
    }
}
