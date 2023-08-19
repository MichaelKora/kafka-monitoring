package de.tuberlin.dos.monitoring.consumer.utilization;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public class Strategies {

	private enum _Strategies {
		CPU,
		MEM,
		MIXED
	}

	public static UtilizationStrategy pickUtilizationStrategy(String utilizationStrategyOption) {
		UtilizationStrategy strategy;
		try {
			strategy = switch (_Strategies.valueOf(utilizationStrategyOption.toUpperCase())) {
				case CPU -> Strategies::cpuIntenseStrategy;
				case MEM -> Strategies::memoryIntenseStrategy;
				case MIXED -> Strategies::mixedStrategy;
			};
		} catch (IllegalArgumentException e) {
			throw new UnsupportedOperationException("Unknown utilization strategy: %s. You have to pick one of these: %s.".formatted(
					utilizationStrategyOption.toUpperCase(),
					_Strategies.values())
			);
		}
		return strategy;
	}

	private static void cpuIntenseStrategy(ConsumerRecord<String, String> record) {
		int[] ints = new int[50_000];
		for (int i = 0; i < ints.length; i++) {
			ints[i] = ThreadLocalRandom.current().nextInt(0, 1_000_000);
		}
		Arrays.sort(ints);
	}

	private static void memoryIntenseStrategy(ConsumerRecord<String, String> record) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	private static void mixedStrategy(ConsumerRecord<String, String> record) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}
	
}
