package ru.kontur.airlock;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import ru.kontur.airlock.dto.EventGroup;
import ru.kontur.airlock.dto.EventRecord;

import java.util.Properties;

class EventSender {

    private final KafkaProducer<String, byte[]> kafkaProducer;

    EventSender(Properties properties) {
        properties.setProperty("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        properties.setProperty("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        this.kafkaProducer = new KafkaProducer<>(properties);
    }

    void send(EventGroup eventGroup) {
        String topic = eventGroup.eventRoutingKey + "|" + eventGroup.eventType;
        for (EventRecord record : eventGroup.eventRecords) {
            kafkaProducer.send(new ProducerRecord<>(topic, null, record.timestamp, null, record.data));
        }
    }
}