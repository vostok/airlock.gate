package ru.kontur.airlock;

import com.codahale.metrics.Gauge;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.rapidoid.log.Log;
import ru.kontur.airlock.dto.EventGroup;
import ru.kontur.airlock.dto.EventRecord;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

class EventSender {
    private final KafkaProducer<String, byte[]> kafkaProducer;

    EventSender(Properties kafkaProperties) {
        kafkaProperties.setProperty(
                "key.serializer",
                "org.apache.kafka.common.serialization.ByteArraySerializer");
        kafkaProperties.setProperty(
                "value.serializer",
                "org.apache.kafka.common.serialization.ByteArraySerializer");
        this.kafkaProducer = new KafkaProducer<>(kafkaProperties);

        for (Metric metric : this.kafkaProducer.metrics().values()) {
            Application.metricRegistry.register(name("producer", metric.metricName().name()),
                    (Gauge<Long>) () -> (long)metric.value());
        }
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