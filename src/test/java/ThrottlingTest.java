import com.google.common.base.Stopwatch;
import org.apache.http.HttpStatus;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.kontur.airlock.EventSender;
import ru.kontur.airlock.dto.EventGroup;
import ru.kontur.airlock.dto.EventRecord;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;
import static org.junit.runners.Parameterized.*;

@RunWith(Parameterized.class)
public class ThrottlingTest extends HttpServerTestBase {
    @Parameter(0)
    public int eventCount;
    @Parameter(1)
    public int secondsElapsed;

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] { { 150 , 1 }, { 5, 0 }, { 250, 2 } };
        return Arrays.asList(data);
    }

    public ThrottlingTest() {
        final KafkaProducer<String, byte[]> kafkaProducerMock = mock(KafkaProducer.class);
        final Properties properties = new Properties();
        properties.setProperty("maxRequestsPerSecondPerApiKey", "100");
    }

    @Test
    public void throttlingTest() throws Exception {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        for (int i=0; i<eventCount; i++) {
            testSend(AirlockMessageGenerator.generateAirlockMessage("project.env.a"), HttpStatus.SC_OK);
        }
        stopwatch.stop();
        final long elapsed = stopwatch.elapsed(TimeUnit.SECONDS);
        Assert.assertTrue( "elapsed too small: " + elapsed,elapsed == secondsElapsed);
    }

}
