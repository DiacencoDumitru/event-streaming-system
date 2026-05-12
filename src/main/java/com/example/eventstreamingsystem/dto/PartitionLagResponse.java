package com.example.eventstreamingsystem.dto;

public record PartitionLagResponse(int partition, long committedOffset, long endOffsetInclusive, long lag) {
}
