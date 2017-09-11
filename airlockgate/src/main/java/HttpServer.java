import org.rapidoid.buffer.Buf;
import org.rapidoid.data.BufRange;
import org.rapidoid.http.AbstractHttpServer;
import org.rapidoid.http.HttpStatus;
import org.rapidoid.http.MediaType;
import org.rapidoid.net.abstracts.Channel;
import org.rapidoid.net.impl.RapidoidHelper;

import java.util.Map;

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
                this.startResponse(ctx, isKeepAlive);
                byte[] response = "Empty body".getBytes();
                this.writeBody(ctx, response, 0, response.length, MediaType.TEXT_PLAIN);
                return HttpStatus.ERROR;
            }
            byte[] body = new byte[req.body.length];
            buf.get(req.body, body, 0);
            //eventSender.SendEvent();
            System.out.println("body size:" + body.length + ", body: " + new String(body));
            return ok(ctx, isKeepAlive, new byte[0], MediaType.TEXT_PLAIN);
        }
        return HttpStatus.NOT_FOUND;
    }

}
