package de.tuberlin.dos.monitoring.consumer.utilization;

import org.apache.kafka.clients.consumer.ConsumerRecord;

@FunctionalInterface
public interface UtilizationStrategy {
	void apply(ConsumerRecord<String, String> record);
}
