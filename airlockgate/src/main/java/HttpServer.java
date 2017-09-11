import org.rapidoid.buffer.Buf;
import org.rapidoid.data.BufRange;
import org.rapidoid.http.AbstractHttpServer;
import org.rapidoid.http.HttpStatus;
import org.rapidoid.http.MediaType;
import org.rapidoid.net.abstracts.Channel;
import org.rapidoid.net.impl.RapidoidHelper;
import scala.App;

import java.io.IOException;

public class HttpServer extends AbstractHttpServer {
    private static final byte[] URI_PING = "/ping".getBytes();
    private static final byte[] URI_SEND = "/send".getBytes();
    private final EventSender eventSender;

    public HttpServer(EventSender eventSender) {
        this.eventSender = eventSender;
    }

    @Override
    protected HttpStatus handle(Channel ctx, Buf buf, RapidoidHelper req) {
        String apiKey = "";
        BufRange apiKeyRange = req.headers.getByPrefix(buf.bytes(), "apikey".getBytes(), false);
        if (apiKeyRange!=null) {
            String apiKeyHeader = buf.get(apiKeyRange);
            int separatorPos = apiKeyHeader.indexOf(":");
            if (separatorPos > 0) {
                apiKey = apiKeyHeader.substring(separatorPos+1);
            }
        }
        System.out.println("apiKey:" + apiKey);

        boolean isKeepAlive = req.isKeepAlive.value;
        if (matches(buf, req.path, URI_PING)) {
            return ok(ctx, isKeepAlive, new byte[0], MediaType.TEXT_PLAIN);
        } else if (matches(buf, req.path, URI_SEND)) {
            if (req.body == null)
            {
                return error(ctx, isKeepAlive, "Empty body");
            }
            byte[] body = new byte[req.body.length];
            buf.get(req.body, body, 0);
            try {
                eventSender.SendEvent(body);
            } catch (IOException e) {
                Application.logEx(e);
                return error(ctx, isKeepAlive, e.getMessage());
            }
            return ok(ctx, isKeepAlive, new byte[0], MediaType.TEXT_PLAIN);
        }
        return HttpStatus.NOT_FOUND;
    }

    private HttpStatus error(Channel ctx, boolean isKeepAlive, String message) {
        byte[] response = message.getBytes();
        this.startResponse(ctx, isKeepAlive);
        this.writeBody(ctx, response, 0, response.length, MediaType.TEXT_PLAIN);
        return HttpStatus.ERROR;
    }

}
