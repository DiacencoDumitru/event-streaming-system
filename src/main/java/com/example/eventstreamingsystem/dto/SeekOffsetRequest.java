package com.example.eventstreamingsystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SeekOffsetRequest(
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9._-]{1,128}$")
        String groupId,
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9._-]{1,128}$")
        String topic,
        @Min(0) int partition,
        @Min(0) long nextOffset
) {
}
