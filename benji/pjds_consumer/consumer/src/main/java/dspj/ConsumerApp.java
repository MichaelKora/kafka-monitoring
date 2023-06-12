package dspj;


import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * Hello world!
 *
 */
public class ConsumerApp 
{
    
    public static void main( String[] args )
    {
		if(args.length != 2) {
			System.out.println("Amount of passed arguments don't match, should be exactly 2");
		}
		String groupId = args[0]; //"localhost:9092";
        String topicName = args[1]; //"dspj-topic";

		String bootstrapServers = groupId;
		String topic = topicName;

		// create consumer configs
		Properties properties = new Properties();
		properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

		try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
			consumer.subscribe(Arrays.asList(topic));
			runPollingLoop(consumer);
		}

	}

	private static void runPollingLoop(KafkaConsumer<String, String> consumer) {
			while (true) {
				ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
						
				for (ConsumerRecord<String, String> record : records){
					System.out.print(record.value()+"\n");
				}
			}
		}

}
