package de.tuberlin.dos.monitoring.consumer.utilization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public class Strategies {

	private static final int MESSAGE_DUMP_CAPACITY = 1_000_000;
	private static final List<String> messageDump = new ArrayList<>(MESSAGE_DUMP_CAPACITY);

	public static UtilizationStrategy pickUtilizationStrategy(String utilizationStrategyOption) {
		return switch (utilizationStrategyOption.toUpperCase()) {
			case "CPU" -> Strategies::cpuIntenseStrategy;
			case "MEM" -> Strategies::memoryIntenseStrategy;
			case "MIXED" -> Strategies::mixedStrategy;
			default -> throw new UnsupportedOperationException("You have to pick a workload strategy: Choose 'CPU', 'MEM' or 'MIXED'.");
		};
	}

	private static void cpuIntenseStrategy(ConsumerRecord<String, String> record) {
		int[] ints = new int[50_000];
		for (int i = 0; i < ints.length; i++) {
			ints[i] = ThreadLocalRandom.current().nextInt(0, 1_000_000);
		}
		Arrays.sort(ints);
	}

	private static void memoryIntenseStrategy(ConsumerRecord<String, String> record) {
		if (messageDump.size() == MESSAGE_DUMP_CAPACITY) {
			messageDump.clear();
		}
		String concatenated = "";
		for (int i = 0; i < 10; i++) {
			concatenated += record.value();
		}
		messageDump.add(concatenated);
	}

	private static void mixedStrategy(ConsumerRecord<String, String> record) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}
	
}
