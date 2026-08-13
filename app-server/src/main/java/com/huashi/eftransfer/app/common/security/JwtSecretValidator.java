package com.huashi.eftransfer.app.common.security;

import com.huashi.eftransfer.shared.security.SecretPolicy;

final class JwtSecretValidator {

    private JwtSecretValidator() {
    }

    static void validate(String secret, String description) {
        SecretPolicy.validateHighEntropy(secret, description);
    }
}
