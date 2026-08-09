package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.constraints.NotNull;

public record PublicReleaseUpdateRequest(@NotNull Boolean qrEntryEnabled) {
}
