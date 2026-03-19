package com.huashi.eftransfer.app.modules.lexicon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLexicalListRequest(
        @NotBlank(message = "listName must not be blank")
        @Size(max = 128, message = "listName must be less than or equal to 128 characters")
        String listName,
        @Size(max = 255, message = "description must be less than or equal to 255 characters")
        String description,
        Boolean active
) {
}
