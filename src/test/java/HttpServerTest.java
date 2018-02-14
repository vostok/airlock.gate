import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.rapidoid.net.Server;
import ru.kontur.airlock.EventSender;
import ru.kontur.airlock.HttpServer;
import ru.kontur.airlock.ValidatorFactory;
import ru.kontur.airlock.dto.AirlockMessage;
import ru.kontur.airlock.dto.EventGroup;
import org.apache.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

public class HttpServerTest {

    private static Server server;
    private static final int port = 12121;

    @BeforeClass
    public static void init() throws Exception {
        final Properties properties = new Properties();
        properties.setProperty("log4j.rootLogger", "DEBUG,CONSOLE");
        properties.setProperty("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
        properties.setProperty("log4j.appender.CONSOLE.threshold", "debug");
        org.apache.log4j.PropertyConfigurator.configure(properties);
        org.apache.log4j.BasicConfigurator.configure();

        final EventSender eventSenderMock = mock(EventSender.class);
        doNothing().when(eventSenderMock).send(Mockito.any(EventGroup.class));
        final Map<String, String[]> hashMap = new HashMap<>();
        hashMap.put("ExactPatternApiKey", new String[]{"project.env"});
        hashMap.put("WildcardPatternApiKey", new String[]{"project.env.*"});
        hashMap.put("UniversalApiKey", new String[]{"*"});
        final ValidatorFactory validatorFactory = new ValidatorFactory(hashMap);
        final HttpServer httpServer = new HttpServer(eventSenderMock, validatorFactory, false);
        server = httpServer.listen(port);
    }

    @AfterClass
    public static void done() {
        server.shutdown();
    }

    @Test
    public void emptyBody() throws Exception {
        testSend(null, HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void validKey() throws Exception {
        testSend(AirlockMessageGenerator.generateAirlockMessage("project.env.a"), HttpStatus.SC_OK);
    }

    @Test
    public void invalidKey() throws Exception {
        testSend(AirlockMessageGenerator.generateAirlockMessage("project.env2.a"),
                HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void invalidMethod() throws Exception {
        Request request = Request.Get("http://localhost:" + port + "/send")
                .connectTimeout(10000)
                .socketTimeout(5000);
        HttpResponse response = request.addHeader("x-apikey", "WildcardPatternApiKey").execute()
                .returnResponse();
        Assert.assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED,
                response.getStatusLine().getStatusCode());
    }

    @Test
    public void notFound() throws Exception {
        Request request = Request.Get("http://localhost:" + port + "/helloServer")
                .connectTimeout(10000)
                .socketTimeout(5000);
        HttpResponse response = request.addHeader("x-apikey", "WildcardPatternApiKey").execute()
                .returnResponse();
        Assert.assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
    }

    @Test
    public void wrongApiKey() throws Exception {
        testSend(AirlockMessageGenerator.generateAirlockMessage(), HttpStatus.SC_UNAUTHORIZED,
                "HackerApiKey");
    }

    @Test
    public void emptyApiKey() throws Exception {
        testSend(AirlockMessageGenerator.generateAirlockMessage(), HttpStatus.SC_UNAUTHORIZED,
                null);
    }

    private void testSend(AirlockMessage message, int expectedStatus) throws Exception {
        testSend(message, expectedStatus, "WildcardPatternApiKey");
    }

    private void testSend(AirlockMessage message, int expectedStatus, String apiKey)
            throws Exception {
        Request request = Request.Post("http://localhost:" + port + "/send")
                .connectTimeout(10000)
                .socketTimeout(5000);
        if (message != null) {
            request.bodyByteArray(message.toByteArray());
        }
        if (apiKey != null) {
            request.addHeader("x-apikey", apiKey);
        }
        HttpResponse response = request.execute().returnResponse();
        Assert.assertEquals(expectedStatus, response.getStatusLine().getStatusCode());
    }
}
