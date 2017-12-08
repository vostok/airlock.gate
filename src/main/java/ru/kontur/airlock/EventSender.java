package ru.kontur.airlock;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.rapidoid.log.Log;
import ru.kontur.airlock.dto.EventGroup;
import ru.kontur.airlock.dto.EventRecord;

public class EventSender {

    private final KafkaProducer<String, byte[]> kafkaProducer;

    EventSender(Properties kafkaProperties) {
        kafkaProperties.setProperty(
                "key.serializer",
                "org.apache.kafka.common.serialization.ByteArraySerializer");
        kafkaProperties.setProperty(
                "value.serializer",
                "org.apache.kafka.common.serialization.ByteArraySerializer");
        this.kafkaProducer = new KafkaProducer<>(kafkaProperties);
    }

    public void send(EventGroup eventGroup) {
        Log.info("Send event group. Size=" + eventGroup.eventRecords.size() + ", key=" + eventGroup.eventRoutingKey);
        for (EventRecord record : eventGroup.eventRecords) {
            ProducerRecord<String, byte[]> pr = new ProducerRecord<>(
                    eventGroup.eventRoutingKey,
                    null,
                    record.timestamp,
                    null,
                    record.data);
            EventSenderCallback callback = new EventSenderCallback(eventGroup.eventRoutingKey,
                    record);
            kafkaProducer.send(pr, callback);
        }
    }

    void shutdown() {
        kafkaProducer.flush();
        kafkaProducer.close(10000, TimeUnit.MILLISECONDS);
    }

    private final static class EventSenderCallback implements Callback {

        private final String routingKey;
        private final EventRecord event;

        EventSenderCallback(String routingKey, EventRecord event) {
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
}