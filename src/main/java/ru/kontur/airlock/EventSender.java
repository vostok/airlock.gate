package ru.kontur.airlock;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.rapidoid.log.Log;
import ru.kontur.airlock.dto.EventGroup;
import ru.kontur.airlock.dto.EventRecord;

import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class EventSender {
    private final KafkaProducer<String, byte[]> kafkaProducer;
    private final Cache<String, RateLimiter> rateLimiterCache;
    private final int maxRequestsPerSecondPerRoutingKey;

    public EventSender(KafkaProducer<String, byte[]> kafkaProducer, Properties appProperties) {
        this.kafkaProducer = kafkaProducer;
        maxRequestsPerSecondPerRoutingKey = Integer.parseInt(appProperties.getProperty("maxRequestsPerSecondPerRoutingKey", "100000"));
        int rateLimiterCacheSize = Integer.parseInt(appProperties.getProperty("rateLimiterCacheSize", "10000"));

        rateLimiterCache = CacheBuilder.newBuilder()
                .maximumSize(rateLimiterCacheSize)
                .expireAfterAccess(10,TimeUnit.MINUTES)
                .build();
    }

    public void send(EventGroup eventGroup)  {
        final RateLimiter rateLimiter;
        try {
            rateLimiter = rateLimiterCache.get(eventGroup.eventRoutingKey, () -> RateLimiter.create(maxRequestsPerSecondPerRoutingKey));
        } catch (ExecutionException e) {
            Log.error(e.toString());
            return;
        }
        rateLimiter.acquire();
        for (EventRecord record : eventGroup.eventRecords) {
            ProducerRecord<String, byte[]> pr = new ProducerRecord<>(
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