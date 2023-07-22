package de.tuberlin.dos.monitoring.producer.workload;

import org.apache.kafka.clients.producer.KafkaProducer;

@FunctionalInterface
public interface WorkloadStrategy {
	void apply(KafkaProducer<String, String> producer);
}
