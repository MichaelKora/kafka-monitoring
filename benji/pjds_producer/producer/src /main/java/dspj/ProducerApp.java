package dspj;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Hello world!
 *
 */
public class ProducerApp 
{
    public static void main( String[] args )
    {
		System.out.println("Hello Producer");
		String groupId = args[0]; //"localhost:9092";
        	String topicName = args[0]; //"dspj-topic";

		String bootstrapServers = groupId;
		String topic = topicName;

		KafkaProducer<String, String> producer = createProducer(bootstrapServers);

		for (int i = 0; i < 10; i++) {
			ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, "Event: "+ i);
			producer.send(producerRecord);
		}

		producer.flush();
		producer.close();
	}

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
