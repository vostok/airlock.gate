import org.rapidoid.buffer.Buf;
import org.rapidoid.data.BufRange;
import org.rapidoid.http.AbstractHttpServer;
import org.rapidoid.http.HttpStatus;
import org.rapidoid.http.MediaType;
import org.rapidoid.net.abstracts.Channel;
import org.rapidoid.net.impl.RapidoidHelper;
import scala.App;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

public class HttpServer extends AbstractHttpServer {
    private static final byte[] URI_PING = "/ping".getBytes();
    private static final byte[] URI_SEND = "/send".getBytes();
    private final EventSender eventSender;
    //private final Map<String, HashSet<String>> apiKeysToProjects = new HashMap<String, HashSet<String>>();
    private final Map<String, String> apiKeyToProject = new HashMap<String, String>();

    public HttpServer(EventSender eventSender) throws IOException {
        this.eventSender = eventSender;
        Properties apiKeysProp = Application.getProperties("apikeys.properties");
        for (String key : apiKeysProp.stringPropertyNames()) {
            apiKeyToProject.put(key, apiKeysProp.getProperty(key, "").trim());
            //String[] projects = apiKeysProp.getProperty(key, "").trim().split("\\s*,\\s*");
//            HashSet<String> projectsHashSet = new HashSet<>();
//            for (String project: projects)
//                projectsHashSet.add(project.toLowerCase());
//            apiKeysToProjects.put(key, projectsHashSet);
        }
    }

    @Override
    protected HttpStatus handle(Channel ctx, Buf buf, RapidoidHelper req) {
        boolean isKeepAlive = req.isKeepAlive.value;

        if (matches(buf, req.path, URI_PING)) {
            return ok(ctx, isKeepAlive, new byte[0], MediaType.TEXT_PLAIN);
        } else if (matches(buf, req.path, URI_SEND)) {
            String apiKey = getHeader(buf, req, "apikey");
            if (apiKey == null || apiKey.trim()=="")
                return error(ctx, isKeepAlive, "Undefined apikey", 401);
            if (req.body == null || req.body.length == 0)
            {
                return error(ctx, isKeepAlive, "Empty body", 400);
            }
            //String project = getHeader(buf, req, "project");
            String project = apiKeyToProject.get(apiKey);
            //HashSet<String> projects = apiKeysToProjects.get(apiKey).;
            if (project == null || project == "")
            //if (projects == null || projects.size())
                return error(ctx, isKeepAlive, "Access denied", 401);
            byte[] body = new byte[req.body.length];
            buf.get(req.body, body, 0);
            try {
                eventSender.SendEvent(project, body);
            } catch (IOException e) {
                Application.logEx(e);
                return error(ctx, isKeepAlive, e.getMessage(), 400);
            }
            return ok(ctx, isKeepAlive, new byte[0], MediaType.TEXT_PLAIN);
        }
        return HttpStatus.NOT_FOUND;
    }

    private String getHeader(Buf buf, RapidoidHelper req, String name) {
        String value = "";
        BufRange bufRange = req.headers.getByPrefix(buf.bytes(), name.getBytes(), false);
        if (bufRange!=null) {
            String fullHeader = buf.get(bufRange);
            int separatorPos = fullHeader.indexOf(":");
            if (separatorPos > 0) {
                value = fullHeader.substring(separatorPos+1).trim();
            }
        }
        return value;
    }

    private HttpStatus error(Channel ctx, boolean isKeepAlive, String message, int code) {
        byte[] response = message.getBytes();
        this.startResponse(ctx, code, isKeepAlive);
        this.writeBody(ctx, response, 0, response.length, MediaType.TEXT_PLAIN);
        return HttpStatus.DONE;
    }

}
