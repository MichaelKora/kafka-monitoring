package de.tuberlin.dos.monitoring.consumer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Consumer {

	private static final String BOOTSTRAP_SERVERS = "cluster-kafka-bootstrap.kafka:9092";
	private static final String TOPIC = Objects.requireNonNullElse(System.getenv("TOPIC_NAME"), "topic1");
	private static final String GROUP_ID = "group1";
	private static final Logger log = LoggerFactory.getLogger(Consumer.class);
	private static final int MESSAGE_DUMP_CAPACITY = 1_000_000;
	private static final List<String> messageDump = new ArrayList<>(MESSAGE_DUMP_CAPACITY);
	private static final Timer latencyTimer = LatencyExporter.create()
															 .getMsgLatencyTimer(UUID.randomUUID().toString());

	public static void main(String[] args) {

		WorkloadStrategy workloadStrategy = pickWorkloadStrategy(args);

		try (KafkaConsumer<String, String> consumer = createConsumer()) {
			createShutdownHook(consumer);
			consumer.subscribe(List.of(TOPIC));
			runPollingLoop(consumer, workloadStrategy);
		}
	}

	private static WorkloadStrategy pickWorkloadStrategy(String[] args) {
		if (args.length != 1) {
			throw new RuntimeException("You have to pick a workload strategy: Choose 'CPU' or 'MEM'.");
		}
		if (Objects.equals(args[0], "CPU")) {
			return Consumer::cpuIntenseStrategy;
		}
		if (Objects.equals(args[0], "MEM")) {
			return Consumer::memoryIntenseStrategy;
		}
		throw new RuntimeException("Unknown workload strategy: %s%n".formatted(args[0]));
	}

	private static KafkaConsumer<String, String> createConsumer() {
		// create consumer configs
		Properties properties = new Properties();
		properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
		properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
		properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
		return new KafkaConsumer<>(properties);
	}

	private static void runPollingLoop(KafkaConsumer<String, String> consumer, WorkloadStrategy workloadStrategy) {
		try (consumer) {
			while (true) {

				for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(100))) {

					log.debug("Got message: K<>: <%s> V<>: <%s> Partition: %s Offset: %s".formatted(
							record.key(), record.value(), record.partition(), record.offset())
					);

					latencyTimer.record(() -> workloadStrategy.apply(record));
				}

				consumer.commitSync();
			}
		}
		catch (WakeupException e) {
			log.info("Wake up exception! Gonna shutdown consumer.");
		}
		catch (Exception e) {
			log.error("Unexpected exception", e);
		}
		finally {
			log.info("Consumer closed.");
		}
	}

	private static void createShutdownHook(KafkaConsumer<String, String> consumer) {
		// get a reference to the current thread
		final Thread mainThread = Thread.currentThread();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				log.info("Detected a shutdown, let's exit by calling consumer.wakeup()...");
				consumer.wakeup();

				try {
					mainThread.join();
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@FunctionalInterface
	private interface WorkloadStrategy {
		void apply(ConsumerRecord<String, String> record);
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

}
