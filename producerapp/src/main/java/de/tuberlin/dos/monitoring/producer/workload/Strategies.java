package de.tuberlin.dos.monitoring.producer.workload;

import static java.time.chrono.JapaneseEra.values;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Strategies {

	private static final Logger log = LoggerFactory.getLogger(Strategies.class);

	private final WorkloadContext workloadContext;

	public static Strategies forWorkloadContext(WorkloadContext workloadContext) {
		return new Strategies(workloadContext);
	}

	public WorkloadStrategy pickWorkloadStrategy(String workload) {
		return switch (workload.toUpperCase()) {
			case "STATIC" -> this::staticStrategy;
			case "PATTERN" -> this::patternStrategy;
			case "RANDOM" -> this::randomStrategy;
			case "STAIR" -> this::stairStrategy;
			default -> throw new IllegalStateException("Unknown workload: %s. Pick one of %s".formatted(workload.toUpperCase(), values()));
		};
	}

	private void staticStrategy(KafkaProducer<String, String> producer) {

		for (int i = 0; i < 2000; i++) {
			sendMessages(producer, workloadContext.getMessagesPerMinute());
			sleep(TimeUnit.SECONDS, workloadContext.getSleeptimeSeconds());
		}
	}

	private void patternStrategy(KafkaProducer<String, String> producer) {

		//Cycle: One cycle consists of two periods, one with lower amount of messages and one with higher amount of messages being sent
		for (int cycle = 0; cycle < 40; cycle++) {

			for(int messagePackages = 0; messagePackages < workloadContext.getPatternWindow(); messagePackages++){
				// start with sending 1/2 of amount of messages per Minute for <patternWindow> times
				sendMessages(producer, workloadContext.getMessagesPerMinute() / 2);
				sleep(TimeUnit.SECONDS, workloadContext.getSleeptimeSeconds());
			}
			for(int messagePackages = 0; messagePackages < workloadContext.getPatternWindow(); messagePackages++){
				// continue with sending 2 times of the amount of messages per Minute for <patternWindow> times
				sendMessages(producer, workloadContext.getMessagesPerMinute() * 2);
				sleep(TimeUnit.SECONDS, workloadContext.getSleeptimeSeconds());
			}
		}
	}

	private void randomStrategy(KafkaProducer<String, String> producer) {

		Random random = new Random();
		random.setSeed(workloadContext.getSeed());

		for(int i = 0; i < 4000; i++){

			int messagesAmount = random.nextInt(workloadContext.getMessagesPerMinute());
			for (int cycle = 1; cycle <= workloadContext.getPatternWindow(); cycle++) {
				sendMessages(producer, messagesAmount);
				sleep(TimeUnit.SECONDS, workloadContext.getSleeptimeSeconds());
			}
		}
	}

	private void stairStrategy(KafkaProducer<String, String> producer) {

		// scale up to 40k messages
		for (int i = 1; i <= 4; i++) {
			sendMessagesConstant(producer, workloadContext.getMessagesPerMinute() * i, workloadContext.getPatternWindow());
		}

		// scale down to 10k messages
		for (int i = 3; i > 1; i--) {
			sendMessagesConstant(producer, workloadContext.getMessagesPerMinute() * i, workloadContext.getPatternWindow());
		}
	}

	private void sendMessages(KafkaProducer<String, String> producer, int amount) {

		String topic = workloadContext.getTopic();

		for (int k = 0; k < amount; k++) {
			String key = randomString();
			String value = randomString();
			ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, value);
			producer.send(producerRecord);
		}

		producer.flush();
		log.info("Sent %s messages to topic %s.".formatted(amount, topic));
	}

	private void sendMessagesConstant(KafkaProducer<String, String> producer, int messagesPerMinute, int repeatTimes) {
		int messagesPerSendCall = messagesPerMinute / 45;
		for (int j = 0; j < repeatTimes; j++) {
			for (int i = 0; i < 45; i++) {
				sendMessages(producer, messagesPerSendCall);
				sleep(TimeUnit.SECONDS, 1);
			}
		}
	}

	private static String randomString() {
		byte[] array = new byte[8];
		new Random().nextBytes(array);
		return new String(array, StandardCharsets.UTF_8);
	}

	private static void sleep(TimeUnit timeUnit, int val) {
		try {
			timeUnit.sleep(val);
		}
		catch (InterruptedException e) {
			throw new RuntimeException("Could not send the producer to bed...", e);
		}
	}

}
