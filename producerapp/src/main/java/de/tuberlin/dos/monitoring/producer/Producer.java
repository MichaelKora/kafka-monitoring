package de.tuberlin.dos.monitoring.producer;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Producer {

	private static final String BOOTSTRAP_SERVERS = "cluster-kafka-bootstrap.kafka:9092";
	private static final String TOPIC = Objects.requireNonNullElse(System.getenv("TOPIC_NAME"), "topic1");
	private static final Logger log = LoggerFactory.getLogger(Producer.class);

	public static void main(String[] args) {

		WorkloadStrategy workloadStrategy = Producer::stairStrategy;
		MessageStrategy messageStrategy = Producer::sendMessagesSpiky;

		KafkaProducer<String, String> producer = createProducer();
		createShutdownHook(producer);
		runMessageLoop(producer, workloadStrategy, messageStrategy);
	}

	private static void runMessageLoop(
			KafkaProducer<String, String> producer,
			WorkloadStrategy workloadStrategy,
			MessageStrategy messageStrategy
	) {

		try (producer) {
			while (true) {
				workloadStrategy.apply(producer, messageStrategy);
			}
		}
		catch (WakeupException e) {
			log.info("Wake up exception! Gonna shutdown producer.");
		}
		catch (Exception e) {
			log.error("Unexpected exception", e);
		}
		finally {
			log.info("Producer closed.");
		}
	}

	@NotNull
	private static KafkaProducer<String, String> createProducer() {

		Properties properties = new Properties();
		// batch size 64KB and +20ms higher linger for higher troughput
		properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(8*1024));
		properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "10");
		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

		return new KafkaProducer<>(properties);
	}

	private static void createShutdownHook(KafkaProducer<String, String> producer) {
		final Thread mainThread = Thread.currentThread();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				producer.close();
				try {
					mainThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@FunctionalInterface
	private interface WorkloadStrategy {
		void apply(KafkaProducer<String, String> producer, MessageStrategy messageStrategy);
	}

	private static void stairStrategy(KafkaProducer<String, String> producer, MessageStrategy messageStrategy) {

		final int MESSAGES_PER_MINUTE = 10_000;

		// scale up to 30k messages
		for (int i = 1; i <= 4; i++) {
			messageStrategy.apply(producer, MESSAGES_PER_MINUTE * i, 15);
		}

		// scale down to 10k messages
		for (int i = 3; i > 1; i--) {
			messageStrategy.apply(producer, MESSAGES_PER_MINUTE * i, 15);
		}
	}

	@FunctionalInterface
	private interface MessageStrategy {
		void apply(KafkaProducer<String, String> producer, int messagesPerMinute, int count);
	}

	private static void sendMessagesConstant(KafkaProducer<String, String> producer, int messagesPerMinute, int count) {
		int messagesPerSendCall = messagesPerMinute / 30;
		for (int j = 0; j < count; j++) {
			for (int i = 0; i < 30; i++) {
				sendMessages(producer, messagesPerSendCall);
				sleepSeconds(1);
			}
		}
	}

	private static void sendMessagesSpiky(KafkaProducer<String, String> producer, int messagesPerMinute, int count) {
		for (int j = 0; j < count; j++) {
			sendMessages(producer, messagesPerMinute);
			sleepMinutes(1);
		}
	}

	private static void sendMessages(KafkaProducer<String, String> producer, int amount) {
		int partitionIndex = 0;
		for (int k = 0; k < amount; k++) {

			String key = randomString();
			String value = randomString();
			ProducerRecord<String, String> producerRecord = new ProducerRecord<>(TOPIC, partitionIndex, key, value);
			producer.send(producerRecord);

			// we have to set the indices manually to ensure all messages are distributed equally
			if (partitionIndex < 24) {
				partitionIndex++;
			} else {
				partitionIndex = 0;
			}

		}
		producer.flush();
		log.info("Sent %s messages to topic %s.".formatted(amount, TOPIC));
	}

	private static String randomString() {
		byte[] array = new byte[8];
		new Random().nextBytes(array);
		return new String(array, StandardCharsets.UTF_8);
	}

	private static void sleepMinutes(int minutes) {
		try {
			TimeUnit.MINUTES.sleep(minutes);
		}
		catch (InterruptedException e) {
			throw new RuntimeException("Could not send the producer to bed...", e);
		}
	}

	private static void sleepSeconds(int seconds) {
		try {
			TimeUnit.SECONDS.sleep(seconds);
		}
		catch (InterruptedException e) {
			throw new RuntimeException("Could not send the producer to bed...", e);
		}
	}

}
