package com.huashi.eftransfer.app.modules.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

public record AdminUserBatchRequest(
        @NotBlank(message = "operation must not be blank")
        @Size(max = 32, message = "operation must be less than or equal to 32 characters")
        String operation,

        @Valid
        @Size(max = 200, message = "createItems must be less than or equal to 200 items")
        List<AdminUserBatchCreateItemRequest> createItems,

        @Size(max = 200, message = "userIds must be less than or equal to 200 items")
        List<@NotNull(message = "userId must not be null") Long> userIds,

        Boolean enabled,

        Set<@NotBlank(message = "role must not be blank") String> roles
) {
}
