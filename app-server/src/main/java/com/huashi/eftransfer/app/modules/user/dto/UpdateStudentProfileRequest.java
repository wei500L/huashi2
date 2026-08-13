package com.huashi.eftransfer.app.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateStudentProfileRequest(
        @NotBlank(message = "gradeName must not be blank")
        @Size(max = 64, message = "gradeName must be at most 64 characters")
        String gradeName,
        @NotBlank(message = "frenchLevel must not be blank")
        @Pattern(regexp = "A1|A2|B1|B2|C1|C2", flags = Pattern.Flag.CASE_INSENSITIVE, message = "frenchLevel must be one of A1, A2, B1, B2, C1, C2")
        String frenchLevel,
        @NotBlank(message = "courseStage must not be blank")
        @Pattern(regexp = "FOUNDATION|INTERMEDIATE|ADVANCED", flags = Pattern.Flag.CASE_INSENSITIVE, message = "courseStage must be one of FOUNDATION, INTERMEDIATE, ADVANCED")
        String courseStage
) {
}
