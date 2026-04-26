# Event Streaming System (Kafka-like)

A simplified event streaming platform built from scratch to demonstrate the core design principles behind Kafka-style log-based messaging systems.

---

## Problem It Solves

Many services need durable, replayable event delivery with independent consumer progress. Traditional queue models often hide log semantics and replay behavior. This project makes those internals explicit using partitioned append-only logs and consumer-managed offsets.

## Key Features

- Topic-based message organization
- Partitioning model for horizontal scalability
- Producer and consumer APIs
- Consumer-controlled offsets for replay
- File-based persistence for durable storage
- Minimal core focused on educational clarity

## Architecture Overview

`Producer -> Topic/Partition Router -> Append-Only Log -> Consumer (Offset Driven)`

- Each topic has one or more partitions
- Messages are appended sequentially to partition logs
- Consumers read by `(topic, partition, offset)` and persist progress externally

## How It Works

### Append-Only Log Storage

- New records are always appended, never rewritten in place
- Sequential disk writes improve throughput and predictability
- Log structure enables deterministic replay

### Partitioning

- Partition key determines message placement
- Ordering is guaranteed within a single partition
- Load is distributed across partitions for concurrency

### Offsets and Replay

- Consumers track the last processed offset
- Restarting from stored offset enables recovery
- Rewinding offset enables reprocessing and audit use cases

## Performance / Benchmarks

Expected behavior for a local development environment:

- Sequential writes outperform random-write persistence patterns
- Throughput scales with partition count and batch size
- Consumer lag depends on processing speed and fetch strategy

Benchmark results should be collected with fixed payload sizes, producer batch settings, and controlled I/O constraints for apples-to-apples comparisons.

## Example Use Cases

- Event sourcing and audit trails
- Asynchronous integration between services
- Activity stream processing
- Offline analytics ingestion pipelines

## Trade-offs and Design Decisions

- Educational simplicity is prioritized over full Kafka feature parity
- File-based persistence improves transparency, but lacks advanced segment/index optimizations
- Consumer-managed offsets maximize flexibility while shifting responsibility to consumers

## Next Improvements

- Add segment rolling and log compaction modes
- Add replication protocol for broker fault tolerance
- Add consumer group rebalancing semantics
- Add metrics endpoint for lag, throughput, and partition utilization
