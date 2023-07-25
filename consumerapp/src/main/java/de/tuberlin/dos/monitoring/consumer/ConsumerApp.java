package de.tuberlin.dos.monitoring.consumer;

import static picocli.CommandLine.Option;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import de.tuberlin.dos.monitoring.consumer.factory.ConsumerFactory;
import de.tuberlin.dos.monitoring.consumer.metrics.MetricsExporter;
import de.tuberlin.dos.monitoring.consumer.utilization.Strategies;
import de.tuberlin.dos.monitoring.consumer.utilization.UtilizationStrategy;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

public class ConsumerApp implements Runnable {

	private static final String BOOTSTRAP_SERVERS = "cluster-kafka-bootstrap.kafka:9092";
	private static final String TOPIC = Objects.requireNonNullElse(System.getenv("TOPIC_NAME"), "topic1");
	private static final String GROUP_ID = "group1";
	private static final Logger log = LoggerFactory.getLogger(ConsumerApp.class);
	private static final Timer latencyTimer = MetricsExporter.create()
															 .getMsgLatencyTimer(UUID.randomUUID().toString());

	@Option(names = "--utilization-strategy", required = true)
	private String utilizationStrategyOption;

	public static void main(String[] args) {
		new CommandLine(new ConsumerApp()).execute(args);
	}

	@Override
	public void run() {

		UtilizationStrategy utilizationStrategy = Strategies.pickUtilizationStrategy(utilizationStrategyOption);

		try (KafkaConsumer<String, String> consumer = ConsumerFactory.create(BOOTSTRAP_SERVERS, GROUP_ID)) {
			createShutdownHook(consumer);
			consumer.subscribe(List.of(TOPIC));
			runPollingLoop(consumer, utilizationStrategy);
		}
	}

	private static void runPollingLoop(KafkaConsumer<String, String> consumer, UtilizationStrategy utilizationStrategy) {
		try (consumer) {
			while (true) {

				for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(100))) {

					log.debug("Got message: K<>: <%s> V<>: <%s> Partition: %s Offset: %s".formatted(
							record.key(), record.value(), record.partition(), record.offset())
					);

					latencyTimer.record(() -> utilizationStrategy.apply(record));
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
			log.info("ConsumerApp closed.");
		}
	}

	private static void createShutdownHook(KafkaConsumer<String, String> consumer) {

		final Thread mainThread = Thread.currentThread();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {

			log.info("Detected a shutdown, let's exit by calling consumer.wakeup()...");
			consumer.wakeup();

			try {
				mainThread.join();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}));
	}

}
