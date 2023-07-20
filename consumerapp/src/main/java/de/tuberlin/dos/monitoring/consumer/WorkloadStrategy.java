package de.tuberlin.dos.monitoring.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;

@FunctionalInterface
public interface WorkloadStrategy {
	void apply(ConsumerRecord<String, String> record);
}
