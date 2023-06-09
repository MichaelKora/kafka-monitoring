package de.tuberlin.dos.monitoring.producer;

import java.util.Properties;
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
	private static final String TOPIC = "topic1";
	private static final Logger log = LoggerFactory.getLogger(Producer.class);

	public static void main(String[] args) {
		KafkaProducer<String, String> producer = createProducer();
		createShutdownHook(producer);
		runMessageLoop(producer);
	}

	private static void runMessageLoop(KafkaProducer<String, String> producer) {

		try (producer) {
			while (true) {

				for (int i = 0; i < 100; i++) {
					ProducerRecord<String, String> producerRecord = new ProducerRecord<>(TOPIC, "Event: %s".formatted(i));
					producer.send(producerRecord);
				}

				producer.flush();
				log.info("Sent 100 messages to topic %s. See ya again in 1 minute.".formatted(TOPIC));
				TimeUnit.MINUTES.sleep(1);
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
		properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(64*1024));
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

}
