package ru.kontur.airlock;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.rapidoid.log.Log;
import ru.kontur.airlock.dto.EventGroup;
import ru.kontur.airlock.dto.EventRecord;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

class EventSender {
    private final KafkaProducer<String, byte[]> kafkaProducer;

    EventSender(Properties properties) {
        properties.setProperty(
                "key.serializer",
                "org.apache.kafka.common.serialization.ByteArraySerializer");
        properties.setProperty(
                "value.serializer",
                "org.apache.kafka.common.serialization.ByteArraySerializer");
        this.kafkaProducer = new KafkaProducer<>(properties);
    }

    void send(EventGroup eventGroup) {
        for (EventRecord record : eventGroup.eventRecords) {
            ProducerRecord pr = new ProducerRecord<>(
                    eventGroup.eventRoutingKey,
                    null,
                    record.timestamp,
                    null,
                    record.data);
            EventSenderCallback callback = new EventSenderCallback(eventGroup.eventRoutingKey, record);
            kafkaProducer.send(pr, callback);
        }
    }

    void shutdown() {
        kafkaProducer.flush();
        kafkaProducer.close(10000, TimeUnit.MILLISECONDS);
    }
}

class EventSenderCallback implements Callback {

    private final String routingKey;
    private final EventRecord event;

    public EventSenderCallback(String routingKey, EventRecord event) {
        this.routingKey = routingKey;
        this.event = event;
    }

    @Override
    public void onCompletion(RecordMetadata metadata, Exception e) {
        if (e != null) {
            String message = String.format(
                    "Error occurred when sending event: %s => %s",
                    routingKey,
                    event);
            Log.error(message, e);
        }
    }
}