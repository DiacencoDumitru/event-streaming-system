package com.example.eventstreamingsystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateTopicRequest(
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9._-]{1,128}$")
        String name,
        @Min(1) int partitions
) {
}
