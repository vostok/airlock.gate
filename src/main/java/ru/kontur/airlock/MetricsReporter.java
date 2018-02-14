package ru.kontur.airlock;

import com.codahale.metrics.Meter;
import java.text.DecimalFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.rapidoid.log.Log;

final class MetricsReporter {

    private double lastThroughput = 0;
    private long lastThroughputBytes = 0;

    private long lastEventCount = 0;
    private long lastRequestSizeCount = 0;

    MetricsReporter(int reportingIntervalSeconds, Meter eventMeter, Meter requestSizeMeter) {
        if (reportingIntervalSeconds <= 0) {
            throw new IllegalArgumentException(
                    "reportingIntervalSeconds cannot be less than or equal to zero");
        }
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> {
            synchronized (this) {
                long prevEventCount = lastEventCount;
                long prevRequestSizeCount = lastRequestSizeCount;
                lastEventCount = eventMeter.getCount();
                lastRequestSizeCount = requestSizeMeter.getCount();
                lastThroughput =
                        ((double) (lastEventCount - prevEventCount)) / reportingIntervalSeconds;
                lastThroughputBytes =
                        (lastRequestSizeCount - prevRequestSizeCount) / reportingIntervalSeconds;
            }
            Log.info(
                    String.format("Thr-put: %s events/sec; Thr-put: %s Kb/sec", getLastThroughput(),
                            getLastThroughputKb()));
        }, 0, reportingIntervalSeconds, TimeUnit.SECONDS);
    }

    String getLastThroughput() {
        return new DecimalFormat("#.##").format(lastThroughput);
    }

    String getLastThroughputKb() {
        return new DecimalFormat("#.##").format(lastThroughputBytes / 1024);
    }
}
