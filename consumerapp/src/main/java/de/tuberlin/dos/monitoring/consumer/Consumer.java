package de.tuberlin.dos.monitoring.consumer;

import java.time.Duration;
import java.util.Properties;

import com.sun.tools.javac.util.List;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Consumer {

	private static final Logger log = LoggerFactory.getLogger(Consumer.class);

	public static void main(String[] args) {

		// stolen from https://www.conduktor.io/kafka/complete-kafka-consumer-with-java/

		String bootstrapServers = "127.0.0.1:9092";
		String groupId = "topic1-group";
		String topic = "topic1";

		// create consumer configs
		Properties properties = new Properties();
		properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

		try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
			createShutdownHook(consumer);
			consumer.subscribe(List.of(topic));
			runPollingLoop(consumer);
		}

	}

	private static void runPollingLoop(KafkaConsumer<String, String> consumer) {
		try {
			while (true) {
				ConsumerRecords<String, String> records =
						consumer.poll(Duration.ofMillis(100));
				for (ConsumerRecord<String, String> record : records) {
					log.info("Key: " + record.key() + ", Value: " + record.value());
					log.info("Partition: " + record.partition() + ", Offset:" + record.offset());
				}
			}
		} catch (WakeupException e) {
			log.info("Wake up exception! Gonna shutdown consumer.");
		} catch (Exception e) {
			log.error("Unexpected exception", e);
		} finally {
			consumer.close();
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
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}

}
