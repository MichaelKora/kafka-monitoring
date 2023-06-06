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
public class ConsumerApp 
{
    
    public static void main( String[] args )
    {
		Dotenv dotenv = Dotenv.configure()
							  .filename("properties")
							  .load();

		String bootstrapServers = dotenv.get("GROUP_ID");
		String groupId = dotenv.get("GROUP_ID");
		String topic = dotenv.get("TOPIC_NAME");

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
		try (consumer) {
			while (true) {
				ConsumerRecords<String, String> records =
						consumer.poll(Duration.ofMillis(100));
				for (ConsumerRecord<String, String> record : records) {
					log.info("Key: " + record.key() + ", Value: " + record.value());
					log.info("Partition: " + record.partition() + ", Offset:" + record.offset());
				}

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
