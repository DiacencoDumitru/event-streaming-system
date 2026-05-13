package com.example.eventstreamingsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PublishBatchEventItem(
        @Pattern(regexp = "^[a-zA-Z0-9._-]{0,128}$")
        String key,
        @NotBlank String payload
) {
}
