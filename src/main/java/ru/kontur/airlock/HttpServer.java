package ru.kontur.airlock;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.rapidoid.buffer.Buf;
import org.rapidoid.data.BufRange;
import org.rapidoid.http.AbstractHttpServer;
import org.rapidoid.http.HttpStatus;
import org.rapidoid.http.MediaType;
import org.rapidoid.net.abstracts.Channel;
import org.rapidoid.net.impl.RapidoidHelper;
import ru.kontur.airlock.dto.AirlockMessage;
import ru.kontur.airlock.dto.EventGroup;

import java.io.IOException;
import java.util.ArrayList;

import static com.codahale.metrics.MetricRegistry.name;


public class HttpServer extends AbstractHttpServer {
    private static final byte[] URI_PING = "/ping".getBytes();
    private static final byte[] URI_SEND = "/send".getBytes();
    private static final byte[] URI_TH = "/th".getBytes();
    private static final byte[] URI_THKB = "/thkb".getBytes();
    private final EventSender eventSender;
    private final AuthorizerFactory authorizerFactory;
    private final Meter requestSizeMeter = Application.metricRegistry.meter(name(HttpServer.class, "request-size"));
    private final Meter eventMeter = Application.metricRegistry.meter(name(HttpServer.class, "events"));
    private final Timer requests = Application.metricRegistry.timer(name(HttpServer.class, "requests"));
    private final MetricsReporter metricsReporter;

    public HttpServer(EventSender eventSender, AuthorizerFactory authorizerFactory) throws IOException {
        this.eventSender = eventSender;
        this.authorizerFactory = authorizerFactory;
        metricsReporter = new MetricsReporter(3, eventMeter, requestSizeMeter);
    }

    @Override
    protected HttpStatus handle(Channel ctx, Buf buf, RapidoidHelper req) {
        boolean isKeepAlive = req.isKeepAlive.value;
        if (matches(buf, req.path, URI_PING)) {
            return ok(ctx, isKeepAlive, new byte[0], MediaType.TEXT_PLAIN);
        } else if (matches(buf, req.path, URI_SEND)) {
            return send(ctx, buf, req, isKeepAlive);
        } else if (matches(buf, req.path, URI_TH)) {
            return ok(ctx, isKeepAlive, metricsReporter.getLastThroughput().getBytes(), MediaType.TEXT_PLAIN);
        } else if (matches(buf, req.path, URI_THKB)) {
            return ok(ctx, isKeepAlive, metricsReporter.getLastThroughputKb().getBytes(), MediaType.TEXT_PLAIN);
        }
        return HttpStatus.NOT_FOUND;
    }

    private HttpStatus send(Channel ctx, Buf buf, RapidoidHelper req, boolean isKeepAlive) {
        final Timer.Context timerContext = requests.time();
        try {
            String apiKey = getHeader(buf, req, "apikey");
            if (apiKey == null || apiKey.trim().isEmpty())
                return error(ctx, isKeepAlive, "Apikey is not provided", 401);

            if (req.body == null || req.body.length == 0)
                return error(ctx, isKeepAlive, "Empty body", 400);
            byte[] body = new byte[req.body.length];
            buf.get(req.body, body, 0);

            AirlockMessage message;
            try {
                message = AirlockMessage.fromByteArray(body, AirlockMessage.class);
            } catch (IOException e) {
                return error(ctx, isKeepAlive, e.getMessage(), 400);
            }

            ArrayList<EventGroup> authorizedEventGroups = filterEventGroupsForApiKey(apiKey, message);
            if (authorizedEventGroups.size() == 0)
                return error(ctx, isKeepAlive, "Access denied", 403);

            int eventCount = 0;
            for (EventGroup eventGroup : authorizedEventGroups) {
                eventSender.send(eventGroup);
                eventCount += eventGroup.eventRecords.size();
            }
            eventMeter.mark(eventCount);
            requestSizeMeter.mark(req.body.length);

            if (authorizedEventGroups.size() < message.eventGroups.size())
                return error(ctx, isKeepAlive, "Access is denied for some event groups", 203);
            else
                return ok(ctx, isKeepAlive, new byte[0], MediaType.TEXT_PLAIN);
        } finally {
            timerContext.stop();
        }
    }

    private ArrayList<EventGroup> filterEventGroupsForApiKey(String apiKey, AirlockMessage message) {
        ArrayList<EventGroup> authorizedEventGroups = new ArrayList<>();
        Authorizer authorizer = authorizerFactory.getAuthorizer(apiKey);
        for (EventGroup eventGroup : message.eventGroups) {
            if (authorizer.authorize(eventGroup.eventRoutingKey)) {
                authorizedEventGroups.add(eventGroup);
            }
        }
        return authorizedEventGroups;
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

    private HttpStatus error(Channel ctx, boolean isKeepAlive, String message, int httpStatusCode) {
        byte[] response = message.getBytes();
        this.startResponse(ctx, httpStatusCode, isKeepAlive);
        this.writeBody(ctx, response, 0, response.length, MediaType.TEXT_PLAIN);
        return HttpStatus.DONE;
    }
}
