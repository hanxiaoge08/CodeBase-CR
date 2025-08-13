#!/bin/bash

echo "Waiting for Kafka to be ready..."

# Wait for Kafka to be ready
until kafka-topics --bootstrap-server kafka:29092 --list > /dev/null 2>&1; do
    echo "Kafka is not ready yet. Waiting..."
    sleep 5
done

echo "Kafka is ready. Creating topics..."

# Create doc-generation-topic
kafka-topics --create \
    --if-not-exists \
    --topic doc-generation-topic \
    --bootstrap-server kafka:29092 \
    --partitions 4 \
    --replication-factor 1 \
    --config retention.ms=604800000

# Create doc-generation-retry-topic
kafka-topics --create \
    --if-not-exists \
    --topic doc-generation-retry-topic \
    --bootstrap-server kafka:29092 \
    --partitions 2 \
    --replication-factor 1 \
    --config retention.ms=86400000

# Create doc-generation-dlq
kafka-topics --create \
    --if-not-exists \
    --topic doc-generation-dlq \
    --bootstrap-server kafka:29092 \
    --partitions 1 \
    --replication-factor 1 \
    --config retention.ms=2592000000

echo "Topics created successfully!"

# List all topics
echo "Listing all topics:"
kafka-topics --bootstrap-server kafka:29092 --list

# Describe topics
echo "Topic details:"
kafka-topics --bootstrap-server kafka:29092 --describe --topic doc-generation-topic
kafka-topics --bootstrap-server kafka:29092 --describe --topic doc-generation-retry-topic
kafka-topics --bootstrap-server kafka:29092 --describe --topic doc-generation-dlq