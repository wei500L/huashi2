package com.huashi.eftransfer.app.modules.assessment.imports.dto;

import jakarta.validation.constraints.AssertTrue;

public record QuestionBankImportCommitRequest(
        @AssertTrue(message = "Import confirmation is required") boolean confirmed
) {
}
