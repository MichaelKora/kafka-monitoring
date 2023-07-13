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

	//-----------------Variables------------------------
	private static final String BOOTSTRAP_SERVERS ="cluster-kafka-bootstrap.kafka:9092"; // "localhost:9092";   //
	private static final String TOPIC = Objects.requireNonNullElse(System.getenv("TOPIC_NAME"), "topic1");
	private static final Logger log = LoggerFactory.getLogger(Producer.class);
	private static int sleeptimeSeconds = 5;  //Change for debugging
	private static int seed = 1;
	private static int messagesMinute = 5000;
	private static int patternWindow = 10; // amount of times data is sent

	//--------------Interfaces---------------------
	@FunctionalInterface
	private interface WorkloadStrategy {
		void apply(KafkaProducer<String, String> producer);
	}
	
	//--------------MAIN------------------------
	public static void main(String[] args) {

		WorkloadStrategy workloadStrategy = pickWorkloadStrategy(args);

		KafkaProducer<String, String> producer = createProducer();
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
			log.info("Producer closed.");
		}
	}

	// -----------------Read in passed arguments-------------------
	private static WorkloadStrategy pickWorkloadStrategy(String[] args) {

		if (args.length != 4) {
			throw new RuntimeException(
					"You have to pick: \n 1. A workload pattern (String) \n 2. A random seed/patttern Window (int) \n 3. Messages per Minute (int) \n 4. Sleeptime (int): \n Choose 'STATIC'/'PATTERN'/'RANDOM'/'STAIR', <Number>(default 1 (10)), <Number>(default 5000).<Number>(default 5)");
		}
		try {
			messagesMinute = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Sorry, the third argument (Message Amount) couldn't be parsed as an Integer: " + e);
		}
		try {
			sleeptimeSeconds = Integer.parseInt(args[3]);
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Sorry, the fourth (Sleep Time) argument couldn't be parsed as an Integer: " + e);
		}
		// Pick Strategy according to 1st cli arg
		if (Objects.equals(args[0].toLowerCase(), "static")){
			setArgument(false, args[1]);
			return Producer::staticStrategy;
		}
		if (Objects.equals(args[0].toLowerCase(), "pattern")){
			setArgument(false, args[1]);
			return Producer::patternStrategy;
		}
		if (Objects.equals(args[0].toLowerCase(), "random")){
			setArgument(true, args[1]);
			return Producer::randomStrategy;
		}
		if (Objects.equals(args[0].toLowerCase(), "stair")){
			setArgument(false, args[1]);
			return Producer::stairStrategy;
		}
		throw new RuntimeException("Unknown workload strategy: %s%n".formatted(args[0]));
	}
	private static void setArgument(boolean randomPattern, String arg1) {
		if (randomPattern) {
			try {
				seed = Integer.parseInt(arg1);
			} catch (NumberFormatException e) {
				throw new NumberFormatException("Sorry, the second argument (Seed) couldn't be parsed as an Integer: " + e);
			}
		} else {
			try {
				patternWindow = Integer.parseInt(arg1);
			} catch (NumberFormatException e) {
				throw new NumberFormatException("Sorry, the second argument (Pattern Window Size) couldn't be parsed as an Integer: " + e);
			}
		}
	}
	
// ----------------Basic Kafka SetUp--------------------------
	@NotNull
	private static KafkaProducer<String, String> createProducer() {

		Properties properties = new Properties();
		properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString( 1024));
		properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "5");
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

//----------------Message Producing Strategies-------------------------------

	private static void staticStrategy(KafkaProducer<String, String> producer) {

			for (int i = 0; i < 2000; i++) {
				sendMessages(producer, messagesMinute);
				sleep(TimeUnit.SECONDS, sleeptimeSeconds);
			}
		}
	
	private static void patternStrategy(KafkaProducer<String, String> producer) {

		//Cycle: One cycle consists of two periods, one with lower amount of messages and one with higher amount of messages being sent
		for (int cycle = 0; cycle < 40; cycle++) {
			
			for(int messagePackages = 0; messagePackages < patternWindow; messagePackages++){
				// start with sending 1/2 of amount of messages per Minute for <patternWindow> times
				sendMessages(producer, messagesMinute / 2);
				sleep(TimeUnit.SECONDS, sleeptimeSeconds);
			}
			for(int messagePackages = 0; messagePackages < patternWindow; messagePackages++){
				// continue with sending 2 times of the amount of messages per Minute for <patternWindow> times
				sendMessages(producer, messagesMinute * 2);
				sleep(TimeUnit.SECONDS, sleeptimeSeconds);
			}
		}
	}

	private static void randomStrategy(KafkaProducer<String, String> producer) {

		Random random = new Random();
		random.setSeed(seed);

		for(int i = 0; i < 4000; i++){

			int messagesAmount = random.nextInt(messagesMinute);
			for (int cycle = 1; cycle <= patternWindow; cycle++) {
				sendMessages(producer, messagesAmount);
				sleep(TimeUnit.SECONDS, sleeptimeSeconds);
	  		}
		}
	}

	private static void stairStrategy(KafkaProducer<String, String> producer) {

		// scale up to 40k messages
		for (int i = 1; i <= 4; i++) {
			sendMessagesConstant(producer, messagesMinute * i, patternWindow);
		}

		// scale down to 10k messages
		for (int i = 3; i > 1; i--) {
			sendMessagesConstant(producer, messagesMinute * i, patternWindow);
		}
	}

	//-----------------Send Messages out-----------------------
	private static void sendMessages(KafkaProducer<String, String> producer, int amount) {

		for (int k = 0; k < amount; k++) {
			String key = randomString();
			String value = randomString();
			ProducerRecord<String, String> producerRecord = new ProducerRecord<>(TOPIC, key, value);
			producer.send(producerRecord);
		}

		producer.flush();
		log.info("Sent %s messages to topic %s.".formatted(amount, TOPIC));
	}

	private static void sendMessagesConstant(KafkaProducer<String, String> producer, int messagesPerMinute, int repeatTimes) {
		int messagesPerSendCall = messagesPerMinute / 45;
		for (int j = 0; j < repeatTimes; j++) {
			for (int i = 0; i < 45; i++) {
				sendMessages(producer, messagesPerSendCall);
				sleep(TimeUnit.SECONDS, 1);
			}
		}
	}
	
	//------------Random message------------------
	private static String randomString() {
		byte[] array = new byte[8];
		new Random().nextBytes(array);
		return new String(array, StandardCharsets.UTF_8);
	}

	//------------------_Sleep--------------------
	private static void sleep(TimeUnit timeUnit, int val) {
		try {
			timeUnit.sleep(val);
		}
		catch (InterruptedException e) {
			throw new RuntimeException("Could not send the producer to bed...", e);
		}
	}

}
