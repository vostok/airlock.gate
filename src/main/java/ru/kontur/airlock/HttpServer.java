package ru.kontur.airlock;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.SlidingTimeWindowArrayReservoir;
import com.codahale.metrics.SlidingTimeWindowReservoir;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.util.concurrent.RateLimiter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.naming.SizeLimitExceededException;
import org.rapidoid.buffer.Buf;
import org.rapidoid.data.BufRange;
import org.rapidoid.http.AbstractHttpServer;
import org.rapidoid.http.HttpStatus;
import org.rapidoid.http.MediaType;
import org.rapidoid.log.Log;
import org.rapidoid.net.abstracts.Channel;
import org.rapidoid.net.impl.RapidoidHelper;
import ru.kontur.airlock.dto.AirlockMessage;
import ru.kontur.airlock.dto.BinarySerializable;
import ru.kontur.airlock.dto.EventGroup;

public class HttpServer extends AbstractHttpServer {

    private static final byte[] URI_ROOT = "/".getBytes();
    private static final byte[] URI_PING = "/ping".getBytes();
    private static final byte[] URI_SEND = "/send".getBytes();
    private static final byte[] URI_SHUTDOWN = "/shutdown".getBytes();
    private static final byte[] URI_THROUGHPUT = "/th"
            .getBytes();      // Yandex.Tank report metrics, not used in production
    private static final byte[] URI_THROUGHPUT_KB = "/thkb"
            .getBytes(); // Yandex.Tank report metrics, not used in production

    private final EventSender eventSender;
    private final ValidatorFactory validatorFactory;
    private final Meter requestSizeMeter = Application.metricRegistry
            .meter(name( "request-size"));
    private final Meter eventMeter = Application.metricRegistry
            .meter(name( "events", "total"));
    private final Histogram requests;
    private final MetricsReporter metricsReporter;
    private final Cache<String, RateInfo> rateInfoCache;
    private final long bandwidthBytes;
    private final Map<String, Double> bandwidthWeights;
    private final RateLimiter bandwidthRateLimiter;

    class RateInfo {
        public RateLimiter rateLimiter;
        public double weight;

        public RateInfo(RateLimiter rateLimiter, double weight) {
            this.rateLimiter = rateLimiter;
            this.weight = weight;
        }
    }

    public HttpServer(EventSender eventSender, ValidatorFactory validatorFactory,
            boolean useInternalMeter, Properties appProperties,
            Properties bandwidthWeights) throws IOException {
        this.eventSender = eventSender;
        this.validatorFactory = validatorFactory;
        final int graphiteRetentionSeconds = Integer.parseInt(appProperties
                .getProperty("graphiteRetentionSeconds", "60"));
        requests = Application.metricRegistry
                //.histogram(name("requests"), () -> new Histogram(new AutoResettableUniformReservoir()));
                .histogram(name("requests"), () -> new Histogram(new SlidingTimeWindowReservoir(graphiteRetentionSeconds,TimeUnit.SECONDS)));
        metricsReporter =
                useInternalMeter ? new MetricsReporter(3, eventMeter, requestSizeMeter) : null;
        bandwidthBytes = Math.round(Double
                .parseDouble(appProperties.getProperty("bandwidthMb", "1000"))*1024*1024);
        final long rateInfoExpirationSeconds = Long
                .parseLong(appProperties.getProperty("rateInfoExpirationSeconds", "600"));
        this.bandwidthWeights = new HashMap<>();
        for (final String name: bandwidthWeights.stringPropertyNames())
            this.bandwidthWeights.put(name, Double.parseDouble(bandwidthWeights.getProperty(name)));

        int rateLimiterCacheSize = Integer
                .parseInt(appProperties.getProperty("rateLimiterCacheSize", "10000"));

        rateInfoCache = CacheBuilder.newBuilder()
                .maximumSize(rateLimiterCacheSize)
                .expireAfterAccess(rateInfoExpirationSeconds, TimeUnit.SECONDS)
                .removalListener((RemovalListener<String, RateInfo>) removal -> updateRateLimits())
                .build();
        bandwidthRateLimiter = RateLimiter.create(bandwidthBytes);
    }

    @Override
    protected HttpStatus handle(Channel ctx, Buf buf, RapidoidHelper req) {
        boolean isKeepAlive = req.isKeepAlive.value;
        if (matches(buf, req.path, URI_PING)) {
            if (!matches(buf, req.verb, "GET".getBytes())) {
                return error(ctx, isKeepAlive, "Method not allowed", 405, null);
            }
            return ok(ctx, isKeepAlive, new byte[0], MediaType.TEXT_PLAIN);
        } else if (matches(buf, req.path, URI_SEND)) {
            if (!matches(buf, req.verb, "POST".getBytes())) {
                return error(ctx, isKeepAlive, "Method not allowed", 405, null);
            }
            return send(ctx, buf, req, isKeepAlive);
        } else if (matches(buf, req.path, URI_THROUGHPUT)) {
            if (metricsReporter == null) {
                return error(ctx, isKeepAlive, "Internal meter disabled", 405, null);
            }
            return ok(ctx, isKeepAlive, metricsReporter.getLastThroughput().getBytes(),
                    MediaType.TEXT_PLAIN);
        } else if (matches(buf, req.path, URI_THROUGHPUT_KB)) {
            if (metricsReporter == null) {
                return error(ctx, isKeepAlive, "Internal meter disabled", 405, null);
            }
            return ok(ctx, isKeepAlive, metricsReporter.getLastThroughputKb().getBytes(),
                    MediaType.TEXT_PLAIN);
        } else if (matches(buf, req.path, URI_ROOT)) {
            if (!matches(buf, req.verb, "GET".getBytes())) {
                return error(ctx, isKeepAlive, "Method not allowed", 405, null);
            }
            return ok(ctx, isKeepAlive, "Gate is running".getBytes(), MediaType.TEXT_PLAIN);
        } else if (matches(buf, req.path, URI_SHUTDOWN)) {
            Log.info("Shutdown via http request");
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                Runtime.getRuntime().exit(0);
            }).start();;
            return ok(ctx, isKeepAlive, "Gate is shutting down".getBytes(), MediaType.TEXT_PLAIN);
        }
        return HttpStatus.NOT_FOUND;
    }

    private HttpStatus send(Channel ctx, Buf buf, RapidoidHelper req, boolean isKeepAlive) {
        final Clock clock = Clock.defaultClock();
        long startTime = clock.getTick();
        try {
            String apiKey = getHeader(buf, req, "x-apikey");
            if (apiKey == null || apiKey.trim().isEmpty()) {
                getErrorMeter("apikey-missing").mark();
                return error(ctx, isKeepAlive, "Apikey is not provided in x-apikey header", 401,
                        null);
            }

            if (req.body == null || req.body.length == 0) {
                getErrorMeter("empty-body").mark();
                return error(ctx, isKeepAlive, "Request body is empty", 400, apiKey);
            }
            final int bodyLength = req.body.length;
            try {
                checkLimits(apiKey, bodyLength);
            } catch (SizeLimitExceededException e) {
                return error(ctx, isKeepAlive, e.getMessage(), 429, apiKey);
            }

            byte[] body = new byte[bodyLength];
            buf.get(req.body, body, 0);
            AirlockMessage message;
            try {
                message = BinarySerializable.fromByteArray(body, AirlockMessage.class);
            } catch (IOException e) {
                getErrorMeter("deserialization").mark();
                return error(ctx, isKeepAlive, e.toString(), 400, apiKey);
            }

            Validator validator = validatorFactory.getValidator(apiKey);
            if (!validator.validateApiKey()) {
                getErrorMeter("invalid-apikey").mark();
                return error(ctx, isKeepAlive, "Invalid Apikey", 401, apiKey);
            }

            ArrayList<EventGroup> validEventGroups = filterEventGroupsForApiKey(validator, message,
                    apiKey);
            if (validEventGroups.size() == 0) {
                getErrorMeter("filtered").mark();
                return error(ctx, isKeepAlive,
                        "Request is valid, but all event groups have routing keys that are either forbidden for this apikey or contain characters other than [A-Za-z0-9.-]",
                        400, apiKey);
            }

            int eventCount = 0;
            for (EventGroup eventGroup : validEventGroups) {
                eventSender.send(eventGroup);
                int groupSize = eventGroup.eventRecords.size();
                eventCount += groupSize;
                getRoutingKeyMeter(eventGroup.eventRoutingKey).mark(groupSize);
            }
            eventMeter.mark(eventCount);
            requestSizeMeter.mark(bodyLength);

            if (validEventGroups.size() < message.eventGroups.size()) {
                getErrorMeter("filtered-partial").mark();
                return error(ctx, isKeepAlive,
                        "Request is valid, but some event groups have routing keys that are either forbidden for this apikey or contain characters other than [A-Za-z0-9.-]",
                        203, apiKey);
            } else {
                return ok(ctx, isKeepAlive, new byte[0], MediaType.TEXT_PLAIN);
            }
        } finally {
            long elapsed = clock.getTick() - startTime;
            requests.update(elapsed);
        }
    }

    private void checkLimits(String apiKey, int bodyLength) throws SizeLimitExceededException {
        final RateInfo rateInfo;
        try {
            double weight = bandwidthWeights.getOrDefault(apiKey, 1D);
            final boolean[] addedRateInfo = {false}; //see https://stackoverflow.com/questions/34865383/variable-used-in-lambda-expression-should-be-final-or-effectively-final
            rateInfo = rateInfoCache
                    .get(apiKey, () -> {
                        addedRateInfo[0] = true;
                        return new RateInfo(RateLimiter.create(bandwidthBytes), weight);
                    });
            if (addedRateInfo[0]) {
                updateRateLimits();
            }
            final boolean apikeyAcquireResult = rateInfo.rateLimiter.tryAcquire(bodyLength);
            if (!bandwidthRateLimiter.tryAcquire(bodyLength) && !apikeyAcquireResult) {
                getErrorMeter("too-much-traffic").mark();
                throw new SizeLimitExceededException("too much traffic per apikey, max rate="+rateInfo.rateLimiter.getRate());
            }
        } catch (ExecutionException e) {
            getErrorMeter("ratelimiter").mark();
            Log.warn(e.toString() + ", apikey=" + apiKey);
        }
    }

    private void updateRateLimits() {
        double weightSum = 0;
        final Collection<RateInfo> rateInfos = rateInfoCache.asMap().values();
        for (RateInfo curInfo : rateInfos) {
            weightSum += curInfo.weight;
        }
        for (RateInfo curInfo : rateInfos) {
            if (weightSum <= 0) {
                curInfo.rateLimiter.setRate(1D / rateInfos.size() * bandwidthBytes);
            } else {
                curInfo.rateLimiter.setRate(curInfo.weight / weightSum * bandwidthBytes);
            }
        }
    }

    private Meter getRoutingKeyMeter(String routingKey) {
        return Application.metricRegistry
                .meter(name( "events", routingKey.replace('.', '-')));
    }

    private Meter getErrorMeter(String errorType) {
        return Application.metricRegistry.meter(name( "errors", errorType));
    }

    private ArrayList<EventGroup> filterEventGroupsForApiKey(Validator validator,
            AirlockMessage message, String apiKey) {
        ArrayList<EventGroup> validatedEventGroups = new ArrayList<>();
        for (EventGroup eventGroup : message.eventGroups) {
            if (validator.validate(eventGroup.eventRoutingKey)) {
                validatedEventGroups.add(eventGroup);
            } else {
                Log.warn("invalid routingkey or access denied, routingKey="
                        + eventGroup.eventRoutingKey + ", apikey=" + apiKey);
            }
        }
        return validatedEventGroups;
    }

    private String getHeader(Buf buf, RapidoidHelper req, String name) {
        String value = "";
        BufRange bufRange = req.headers.getByPrefix(buf.bytes(), name.getBytes(), false);
        if (bufRange != null) {
            String fullHeader = buf.get(bufRange);
            int separatorPos = fullHeader.indexOf(":");
            if (separatorPos > 0) {
                value = fullHeader.substring(separatorPos + 1).trim();
            }
        }
        return value;
    }

    private HttpStatus error(Channel ctx, boolean isKeepAlive, String message, int httpStatusCode,
            String apiKey) {
        Log.warn(message + (apiKey == null ? "" : ", apikey=" + apiKey));
        byte[] response = message.getBytes();
        this.startResponse(ctx, httpStatusCode, isKeepAlive);
        this.writeBody(ctx, response, 0, response.length, MediaType.TEXT_PLAIN);
        return HttpStatus.DONE;
    }
}
