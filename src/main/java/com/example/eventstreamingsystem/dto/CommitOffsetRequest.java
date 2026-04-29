package com.example.eventstreamingsystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CommitOffsetRequest(
        @NotBlank String groupId,
        @NotBlank String topic,
        @Min(0) int partition,
        @Min(0) long offset
) {
}
