package com.huashi.eftransfer.app.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterStudentRequest(
        @NotBlank(message = "username must not be blank")
        @Size(max = 64, message = "username must be at most 64 characters")
        String username,
        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        @Size(max = 128, message = "email must be at most 128 characters")
        String email,
        @NotBlank(message = "displayName must not be blank")
        @Size(max = 128, message = "displayName must be at most 128 characters")
        String displayName,
        @NotBlank(message = "password must not be blank")
        @Size(min = 8, max = 128, message = "password must be between 8 and 128 characters")
        String password,
        @NotBlank(message = "registrationToken must not be blank")
        @Size(max = 256, message = "registrationToken must be at most 256 characters")
        String registrationToken,
        @NotBlank(message = "englishLevel must not be blank")
        @Pattern(regexp = "A1|A2|B1|B2|C1|C2", message = "englishLevel must be one of A1, A2, B1, B2, C1, C2")
        String englishLevel,
        @NotBlank(message = "frenchLevel must not be blank")
        @Pattern(regexp = "A1|A2|B1|B2|C1|C2", message = "frenchLevel must be one of A1, A2, B1, B2, C1, C2")
        String frenchLevel,
        @NotBlank(message = "courseStage must not be blank")
        @Pattern(regexp = "FOUNDATION|INTERMEDIATE|ADVANCED", message = "courseStage must be one of FOUNDATION, INTERMEDIATE, ADVANCED")
        String courseStage
) {
}
