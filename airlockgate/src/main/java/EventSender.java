import dto.AirlockMessage;
import dto.EventGroup;
import dto.EventRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

public class EventSender {

    private final KafkaProducer<String, byte[]> kafkaProducer;

    public EventSender(Properties properties) {
        properties.setProperty("key.serializer","org.apache.kafka.common.serialization.ByteArraySerializer");
        properties.setProperty("value.serializer","org.apache.kafka.common.serialization.ByteArraySerializer");
        this.kafkaProducer = new KafkaProducer<>(properties);
    }

    public void SendEvent(byte[] data) throws IOException {
        AirlockMessage message = new AirlockMessage();
        message.read(new ByteArrayInputStream(data));
        for (EventGroup group: message.eventGroups) {
            String topic = message.project + "-" + group.eventType;
            for (EventRecord record: group.eventRecords) {
                kafkaProducer.send(new ProducerRecord<>(topic, null, record.timestamp, null, record.data));
            }
        }
    }
}
