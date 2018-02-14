import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Properties;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.mockito.Mockito;
import org.rapidoid.net.Server;
import ru.kontur.airlock.EventSender;
import ru.kontur.airlock.HttpServer;
import ru.kontur.airlock.ValidatorFactory;
import ru.kontur.airlock.dto.AirlockMessage;
import ru.kontur.airlock.dto.EventGroup;

public class HttpServerTestBase {

    protected static final int port = 12121;
    static Server server;

    @BeforeClass
    public static void init() throws Exception {
        if (server != null) {
            return;
        }
        final Properties logProperties = new Properties();
        logProperties.setProperty("log4j.rootLogger", "DEBUG,CONSOLE");
        logProperties.setProperty("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
        logProperties.setProperty("log4j.appender.CONSOLE.threshold", "debug");
        org.apache.log4j.PropertyConfigurator.configure(logProperties);
        org.apache.log4j.BasicConfigurator.configure();

        final Properties appProperties = new Properties();
        appProperties.setProperty("maxRequestsPerSecondPerApiKey", "100");

        final EventSender eventSenderMock = mock(EventSender.class);
        doNothing().when(eventSenderMock).send(Mockito.any(EventGroup.class));
        final HashMap<String, String[]> hashMap = new HashMap<>();
        hashMap.put("ExactPatternApiKey", new String[]{"project.env"});
        hashMap.put("WildcardPatternApiKey", new String[]{"project.env.*"});
        hashMap.put("UniversalApiKey", new String[]{"*"});
        final ValidatorFactory validatorFactory = new ValidatorFactory(hashMap);
        final HttpServer httpServer = new HttpServer(eventSenderMock, validatorFactory, false,
                appProperties);
        server = httpServer.listen(port);
    }

//    @AfterClass
//    public static void done() throws InterruptedException {
//        server.shutdown();
//    }

    protected void testSend(AirlockMessage message, int expectedStatus) throws Exception {
        testSend(message, expectedStatus, "WildcardPatternApiKey");
    }

    protected void testSend(AirlockMessage message, int expectedStatus, String apiKey)
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
