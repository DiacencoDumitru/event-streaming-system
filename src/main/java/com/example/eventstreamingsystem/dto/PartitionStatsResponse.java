package com.example.eventstreamingsystem.dto;

public record PartitionStatsResponse(int partition, long eventCount) {
}
