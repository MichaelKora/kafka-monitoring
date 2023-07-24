package de.tuberlin.dos.monitoring.producer.factory;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

public class ProducerFactory {

	public static KafkaProducer<String, String> create(String bootstrapServers) {

		Properties properties = new Properties();
		properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(1024));
		properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "5");
		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

		return new KafkaProducer<>(properties);
	}

}
