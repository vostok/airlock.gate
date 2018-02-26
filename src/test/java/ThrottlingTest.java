import static org.mockito.Mockito.mock;

import com.google.common.base.Stopwatch;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
public class ThrottlingTest extends HttpServerTestBase {

    @Parameter(0)
    public int eventCount;
    @Parameter(1)
    public int minSuccessCount;

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{{120, 99}, {5, 5}, {220, 150}};
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
        int successCount = 0;
        for (int i = 0; i < eventCount; i++) {
            int statusCode = sendMessage(
                    AirlockMessageGenerator.generateAirlockMessage("project.env.a"), "WildcardPatternApiKey");
            if (statusCode == HttpStatus.SC_OK)
                successCount++;
            //testSend(AirlockMessageGenerator.generateAirlockMessage("project.env.a"),
            //        HttpStatus.SC_OK);
        }
        stopwatch.stop();
        Assert.assertTrue("unexpected successCount: " + successCount + ", minSuccessCount=" + minSuccessCount, successCount > 0 && successCount <= minSuccessCount );
        final long elapsed = stopwatch.elapsed(TimeUnit.SECONDS);
        Assert.assertTrue("unexpected elapsed: " + elapsed + ", expected 1 sec or lower", elapsed <= 1);
        Thread.sleep(3000);
    }

}
