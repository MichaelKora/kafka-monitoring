package de.tuberlin.dos.monitoring.producer;

import static picocli.CommandLine.Option;

import java.util.Objects;

import de.tuberlin.dos.monitoring.producer.workload.Strategies;
import de.tuberlin.dos.monitoring.producer.workload.WorkloadContext;
import de.tuberlin.dos.monitoring.producer.workload.WorkloadStrategy;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProducerApp {

	private static final String BOOTSTRAP_SERVERS = Objects.requireNonNullElse(
			System.getenv("KAFKA_BOOTSTRAP_SERVERS"), "cluster-kafka-bootstrap.kafka:9092"
	);
	private static final String topic = Objects.requireNonNullElse(
			System.getenv("TOPIC_NAME"), "topic1"
	);
	private static final Logger log = LoggerFactory.getLogger(ProducerApp.class);
	@Option(names = "--workload-strategy", required = true)
	private static String workloadStrategyOption;
	@Option(names = "--sleeptime-seconds", defaultValue = "5")
	private static int sleeptimeSeconds;
	@Option(names = "--seed", defaultValue = "1")
	private static int seed;
	@Option(names = "--messages-per-minute", defaultValue = "5000", description = "Amount of messages sent per minute.")
	private static int messagesPerMinute;
	@Option(names = "--pattern-window", defaultValue = "10", description = "Amount of times data is sent")
	private static int patternWindow;

	public static void main(String[] args) {

		WorkloadContext workloadContext = new WorkloadContext(topic, sleeptimeSeconds, seed, messagesPerMinute, patternWindow);
		WorkloadStrategy workloadStrategy = Strategies.forWorkloadContext(workloadContext)
													  .pickWorkloadStrategy(workloadStrategyOption);

		KafkaProducer<String, String> producer = ProducerFactory.create(BOOTSTRAP_SERVERS);
		createShutdownHook(producer);
		runMessageLoop(producer, workloadStrategy);
	}
	private static void runMessageLoop(KafkaProducer<String, String> producer, WorkloadStrategy workloadStrategy) {
		try (producer) {
			while (true) {
				workloadStrategy.apply(producer);
			}
		} catch (WakeupException e) {
			log.info("Wake up exception! Gonna shutdown producer.");
		} catch (Exception e) {
			log.error("Unexpected exception", e);
		} finally {
			log.info("ProducerApp closed.");
		}
	}

	private static void createShutdownHook(KafkaProducer<String, String> producer) {
		final Thread mainThread = Thread.currentThread();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			producer.close();
			try {
				mainThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}));
	}

}
