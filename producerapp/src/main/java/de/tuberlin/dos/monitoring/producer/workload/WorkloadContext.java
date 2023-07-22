package de.tuberlin.dos.monitoring.producer.workload;

import lombok.Value;

@Value
public class WorkloadContext {

	String topic;
	int sleeptimeSeconds;
	int seed;
	int messagesPerMinute;
	int patternWindow;

}
