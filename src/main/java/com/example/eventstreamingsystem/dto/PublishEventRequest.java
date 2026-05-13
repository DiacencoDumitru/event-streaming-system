package com.example.eventstreamingsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PublishEventRequest(
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9._-]{1,128}$")
        String topic,
        String key,
        @NotBlank String payload
) {
}
