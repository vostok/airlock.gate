import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import java.io.Console;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import org.apache.http.client.fluent.Request;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.rapidoid.net.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        logProperties.setProperty("log4j.appender.CONSOLE.Target", "System.out");
        logProperties.setProperty("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout");
        logProperties.setProperty("log4j.appender.CONSOLE.layout.ConversionPattern", "%d{HH:mm:ss.SSS} %-5p %m%n");
        logProperties.setProperty("log4j.appender.CONSOLE.threshold", "debug");

        org.apache.log4j.PropertyConfigurator.configure(logProperties);

        final Properties appProperties = new Properties();
        appProperties.setProperty("bandwidthMb", "3");

        final EventSender eventSenderMock = mock(EventSender.class);
        doAnswer(inv -> {
          //Thread.sleep(100);
          return null;
        }).when(eventSenderMock).send(Mockito.any(EventGroup.class));
        final HashMap<String, String[]> hashMap = new HashMap<>();
        hashMap.put("ExactPatternApiKey", new String[]{"project.env"});
        hashMap.put("WildcardPatternApiKey", new String[]{"project.env.*"});
        hashMap.put("WildcardPatternApiKey0", new String[]{"project.env.*"});
        hashMap.put("WildcardPatternApiKey1", new String[]{"project.env.*"});
        hashMap.put("UniversalApiKey", new String[]{"*"});
        final ValidatorFactory validatorFactory = new ValidatorFactory(hashMap);
        final Properties bandwidthWeights = new Properties();
        bandwidthWeights.put("WildcardPatternApiKey0","2");
        bandwidthWeights.put("WildcardPatternApiKey1","3");
        final HttpServer httpServer = new HttpServer(eventSenderMock, validatorFactory, false,
                appProperties, bandwidthWeights);
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
        int statusCode = sendMessage(message, apiKey);
        Assert.assertEquals(expectedStatus, statusCode);
    }

    protected int sendMessage(AirlockMessage message, String apiKey) throws IOException {
        Request request = Request.Post("http://localhost:" + port + "/send")
                .connectTimeout(10000)
                .socketTimeout(5000);
        if (message != null) {
            final byte[] bytes = message.toByteArray();
            //System.out.println("message size="+bytes.length);
            request.bodyByteArray(bytes);
        }
        if (apiKey != null) {
            request.addHeader("x-apikey", apiKey);
        }
        return request.execute().returnResponse().getStatusLine().getStatusCode();
    }

}
