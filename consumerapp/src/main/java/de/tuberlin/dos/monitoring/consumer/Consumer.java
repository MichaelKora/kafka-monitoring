package de.tuberlin.dos.monitoring.consumer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Consumer {

	private static final String BOOTSTRAP_SERVERS = "192.168.49.2:30534,192.168.49.2:30223";
	private static final String TOPIC = "topic1";
	private static final String GROUP_ID = "group1";

	private static final Logger log = LoggerFactory.getLogger(Consumer.class);

	public static void main(String[] args) {
		try (KafkaConsumer<String, String> consumer = createConsumer()) {
			createShutdownHook(consumer);
			consumer.subscribe(List.of(TOPIC));
			runPollingLoop(consumer);
		}
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

	private static void runPollingLoop(KafkaConsumer<String, String> consumer) {
		try (consumer) {
			while (true) {
				ConsumerRecords<String, String> records =
						consumer.poll(Duration.ofMillis(1000));
				for (ConsumerRecord<String, String> record : records) {
					log.info("Got message: K<>: <%s> V<>: <%s> Partition: %s Offset: %s".formatted(
							record.key(), record.value(), record.partition(), record.offset())
					);
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
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}

}
