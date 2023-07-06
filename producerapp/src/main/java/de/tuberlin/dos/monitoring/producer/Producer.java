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
	private static final String BOOTSTRAP_SERVERS ="localhost:9092";   //"cluster-kafka-bootstrap.kafka:9092"; //
	private static final String TOPIC = Objects.requireNonNullElse(System.getenv("TOPIC_NAME"), "topic1");
	private static final Logger log = LoggerFactory.getLogger(Producer.class);
	private static final int sleeptimeSeconds = 5;  //Change for debugging
	private static int seed = 1;
	private static int messagesMinute = 10000;
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

		if (args.length != 3) {
			throw new RuntimeException(
					"You have to pick a workload pattern, a random seed & messages per Minute: Choose 'STATIC'/'PATTERN'/'RANDOM'/'STAIR', <Number>(default 1 (10)), <Number>(default 10000).");
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
				throw new NumberFormatException("Sorry, the second argument couldn't be parsed as an Integer: " + e);
			}
		} else {
			try {
				patternWindow = Integer.parseInt(arg1);
			} catch (NumberFormatException e) {
				throw new NumberFormatException("Sorry, the second argument couldn't be parsed as an Integer: " + e);
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

			for (int i = 0; i < 45; i++) {
				sendMessages(producer, messagesMinute);
				sleepSeconds(sleeptimeSeconds);
			}
		}
	
	private static void patternStrategy(KafkaProducer<String, String> producer) {

		//Cycle: One cycle consists of two periods, one with lower amount of messages and one with higher amount of messages being sent
		for (int cycle = 0; cycle < 4; cycle++) {
			
			for(int messagePackages = 0; messagePackages < patternWindow; messagePackages++){
				// start with sending 1/2 of amount of messages per Minute for <patternWindow> times
				sendMessages(producer, messagesMinute / 2);
				sleepSeconds(sleeptimeSeconds);
			}
			for(int messagePackages = 0; messagePackages < patternWindow; messagePackages++){
				// continue with sending 2 times of the amount of messages per Minute for <patternWindow> times
				sendMessages(producer, messagesMinute * 2);
				sleepSeconds(sleeptimeSeconds);
			}
		}
	}

	private static void randomStrategy(KafkaProducer<String, String> producer) {

		Random random = new Random();
		random.setSeed(seed);

		for(int i = 0; i < 45; i++){

			int messagesAmount = random.nextInt(messagesMinute);
			for (int cycle = 1; cycle <= patternWindow; cycle++) {
				sendMessages(producer, messagesAmount);
				sleepSeconds(sleeptimeSeconds);
	  		}
		}
	}

	private static void stairStrategy(KafkaProducer<String, String> producer) {

		// scale up to 30k messages
		for (int stairstep = 1; stairstep <= 4; stairstep++) {
			for(int steplength=0; steplength < patternWindow; steplength++){
				sendMessages(producer, messagesMinute * stairstep);
				sleepSeconds(sleeptimeSeconds);
			}	
		}

		// scale down to 10k messages
		for (int stairstep = 3; stairstep > 1; stairstep--) {
			for(int steplength=0; steplength < patternWindow; steplength++){
				sendMessages(producer, messagesMinute * stairstep);
				sleepSeconds(sleeptimeSeconds);
			}	
		}
	}


	//-----------------Send Messages out-----------------------
	private static void sendMessages(KafkaProducer<String, String> producer, int messagesPerMinute) {

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
		}
	
	//------------Random message------------------
	private static String randomString() {
		byte[] array = new byte[8];
		new Random().nextBytes(array);
		return new String(array, StandardCharsets.UTF_8);
	}

	//------------------_Sleep--------------------
	private static void sleepSeconds(int seconds) {
		try {
			TimeUnit.SECONDS.sleep(seconds);
		}
		catch (InterruptedException e) {
			throw new RuntimeException("Could not send the producer to bed...", e);
		}
	}
}
