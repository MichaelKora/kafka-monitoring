package de.tuberlin.dos.monitoring.producer;

import java.util.Properties;

import io.github.cdimascio.dotenv.Dotenv;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Producer {

	private static final Logger log = LoggerFactory.getLogger(Producer.class);

	public static void main(String[] args) {

		Dotenv dotenv = Dotenv.configure()
							  .filename("properties")
							  .load();

		String bootstrapServers = dotenv.get("KAFKA_BOOTSTRAP_SERVERS");
		String topic = dotenv.get("KAFKA_TOPIC_NAME");

		KafkaProducer<String, String> producer = createProducer(bootstrapServers);

		for (int i = 0; i < 100; i++) {
			ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, "hello world");
			producer.send(producerRecord);
		}

		log.info("Sent 100 messages to topic %s.".formatted(topic));

		producer.flush();
		producer.close();
	}

	@NotNull
	private static KafkaProducer<String, String> createProducer(String bootstrapServers) {

		Properties properties = new Properties();
		// batch size 64KB and +20ms higher linger for higher troughput
		properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(64*1024));
		properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");
		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

		return new KafkaProducer<>(properties);
	}

}
