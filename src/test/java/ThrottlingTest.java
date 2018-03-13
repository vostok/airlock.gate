import static org.mockito.Mockito.mock;

import com.google.common.base.Stopwatch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.rapidoid.log.Log;
import ru.kontur.airlock.dto.AirlockMessage;


@RunWith(Parameterized.class)
public class ThrottlingTest extends HttpServerTestBase {

    @Parameter(0)
    public int eventCountPerKey;
    @Parameter(1)
    public int minSuccessCount;
    @Parameter(2)
    public int maxSuccessCount;
    @Parameter(3)
    public int apikeyCount;

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{{15, 10, 14, 1}, {5, 5, 5, 1}, {50, 10, 15, 1}, {50, 10, 25, 2}, {5, 9, 10, 2}};
        return Arrays.asList(data);
    }

    //message size=336075
    final AirlockMessage message = AirlockMessageGenerator
            .generateAirlockMessage("project.env.a", 1000);
    @Test
    public void throttlingTest() throws Exception {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        AtomicInteger successCounter = new AtomicInteger(0);
        final ArrayList<Thread> threads = new ArrayList<>();
        final ArrayList<AtomicInteger> successCountersByApikey = new ArrayList<>();
        for (int i = 0; i < apikeyCount; i++) {
            successCountersByApikey.add(new AtomicInteger());
        }
        for (int i = 0; i < eventCountPerKey; i++) {
            for (int j = 0; j < apikeyCount; j++) {
                int finalJ = j;
                final Thread thread = new Thread(() -> {
                    try {
                        int statusCode = sendMessage(
                                message, "WildcardPatternApiKey"+ finalJ);
                        if (statusCode == HttpStatus.SC_OK) {
                            successCounter.incrementAndGet();
                            successCountersByApikey.get(finalJ).incrementAndGet();
                        }
                    } catch (Exception e) {
                        Log.error(e.toString());
                    }
                });
                thread.run();
                threads.add(thread);
            }
        }
        for (Thread thread: threads) {
            thread.join();
        }
        stopwatch.stop();
        final long elapsed = stopwatch.elapsed(TimeUnit.SECONDS);
        final int successEvents = successCounter.get();
        int eventCount = apikeyCount * eventCountPerKey;
        System.out.println("eventCount: " + eventCount + ", successEvents: " + successEvents + ", elapsed seconds:" + elapsed);
        for (int i = 0; i < apikeyCount; i++) {
            System.out.println("successCountByApiKey" + i + ": " + successCountersByApikey.get(i).get());
        }
        Assert.assertTrue("unexpected successCount: " + successEvents +  ", minSuccessCount=" + minSuccessCount + ", maxSuccessCount=" + maxSuccessCount, successEvents
                >= minSuccessCount && successEvents <= maxSuccessCount);
        Assert.assertTrue("unexpected elapsed: " + elapsed + ", expected 1 sec or lower", elapsed <= 1);
    }

    @After
    public void AfterTest() throws InterruptedException {
        Thread.sleep(3000);
    }

}
