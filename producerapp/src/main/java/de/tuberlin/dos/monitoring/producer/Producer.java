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

	private static final String BOOTSTRAP_SERVERS ="localhost:9092";   //"cluster-kafka-bootstrap.kafka:9092";
	private static final String TOPIC = Objects.requireNonNullElse(System.getenv("TOPIC_NAME"), "topic1");
	private static final Logger log = LoggerFactory.getLogger(Producer.class);
	private static int seed = 0;
	private static int messagesMinute = 10000;
	private static int patternWindow = 300; // in seconds

	public static void main(String[] args) {

		WorkloadStrategy workloadStrategy = pickWorkloadStrategy(args);

		KafkaProducer<String, String> producer = createProducer();
		createShutdownHook(producer);
		runMessageLoop(producer, workloadStrategy);
	}

	private static WorkloadStrategy pickWorkloadStrategy(String[] args) {
		/*
		 * Possible Scenarios:
		 * 1. CPU intense ~konstant
		 * 2. MEM intense ~konstant
		 * 3. CPU/MEM intense ~konstant
		 * 4. CPU intense ~pattern
		 * 5. MEM intense ~pattern
		 * 6. CPU/MEM intense ~pattern
		 * 7. CPU intense ~random
		 * 8. MEM intense ~random
		 * 9. CPU/MEM intense ~random
		 * 
		 * To consider:
		 * Partitions ~ key structure of different messages
		 * Replication Factors
		 * Consumer Groups
		 * When Consumers fall behind, the data could not be in-memory of the brokers
		 * anymore
		 * Broker size
		 * Encryption of data
		 * Pattern: #Messages at low rate, #Messages at high rate, ratio of low ~ high
		 * rate, Time of one Cycle, #Cycles
		 */
		if (args.length != 3) {
			throw new RuntimeException(
					"You have to pick a workload pattern, a random seed & messages per Minute: Choose 'STATIC'/'PATTERN'/'RANDOM', <Number>(default 0), <Number>(default 10000).");
		}
		try {
			seed = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Sorry, the second argument couldn't be parsed as an Integer: " + e);
		}
		try {
			messagesMinute = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Sorry, the third argument couldn't be parsed as an Integer: " + e);
		}
		if (Objects.equals(args[0].toLowerCase(), "static"))
			return Producer::staticStrategy;
		if (Objects.equals(args[0].toLowerCase(), "pattern"))
			return Producer::patternStrategy;
		if (Objects.equals(args[0].toLowerCase(), "random"))
			return Producer::randomStrategy;
		throw new RuntimeException("Unknown workload strategy: %s%n".formatted(args[0]));
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
			log.info("Producer closed.");
		}
	}

	@NotNull
	private static KafkaProducer<String, String> createProducer() {

		Properties properties = new Properties();
		// batch size 64KB and +20ms higher linger for higher troughput
		properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(64 * 1024));
		properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");
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
		void apply(KafkaProducer<String, String> producer);
	}

	private static void staticStrategy(KafkaProducer<String, String> producer) {

		// scale up to 30k messages
		for (int i = 1; i <= 4; i++) {
			sendMessages(producer, messagesMinute * i, 30);
		}

		// scale down to 10k messages
		for (int i = 3; i > 1; i--) {
			sendMessages(producer, messagesMinute * i, 30);
		}
	}

	private static void patternStrategy(KafkaProducer<String, String> producer) {

		for (int cycle = 0; cycle < 4; cycle++) {

			// start with 1/2 of #messages for <patternWindow> seconds(?)
			sendMessages(producer, messagesMinute / 2, patternWindow);

			// scale up to 2 * #messages for <patternWindow> seconds(?)
			sendMessages(producer, messagesMinute * 2, patternWindow);

		}
	}

	private static void randomStrategy(KafkaProducer<String, String> producer) {

		Random random = new Random();
		random.setSeed(seed);
		int scaleOneFactor, scaleTwoFactor, window;

		for(int cycle = 0; cycle < 4; cycle++){
			
			scaleOneFactor = random.nextInt(5);
			scaleTwoFactor = random.nextInt(5);
			window = random.nextInt(random.nextInt(40));
			
			// scale up to random #messages
			for (int i = 1; i <= scaleOneFactor; i++) {
				sendMessages(producer, messagesMinute * i, window);
			}

			//scale down to random #messages
			for (int i = 5; i > scaleTwoFactor; i--) {
				sendMessages(producer, messagesMinute * i, window);
			}
	  }
	}

	private static void sendMessages(KafkaProducer<String, String> producer, int messagesPerMinute, int count) {

		for (int j = 0; j < count; j++) {
			int debugmessages = 0;
			for (int i = 0; i < messagesPerMinute; i++) {
				String key = randomString();
				String value = randomString();
				ProducerRecord<String, String> producerRecord = new ProducerRecord<>(TOPIC, key, value);
				producer.send(producerRecord);
				debugmessages++;
			}
			System.out.println("Messages sent: " + Integer.toString(debugmessages));

			producer.flush();
			log.info("Sent %s messages to topic %s.".formatted(messagesPerMinute, TOPIC));

			sleep(5);

		}
	}

	private static String randomString() {
		byte[] array = new byte[8];
		new Random().nextBytes(array);
		return new String(array, StandardCharsets.UTF_8);
	}

	private static void sleep(int seconds) {
		try {
			TimeUnit.SECONDS.sleep(seconds);
		} catch (InterruptedException e) {
			throw new RuntimeException("Could not send the producer to bed...", e);
		}
	}

}
