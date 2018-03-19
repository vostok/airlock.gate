package ru.kontur.airlock;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.rapidoid.log.Log;
import org.rapidoid.net.Server;

public class Application {

    private static Server httpServer;
    private static EventSender eventSender;
    static final MetricRegistry metricRegistry = new MetricRegistry();

    public static void main(String[] args) throws Exception {
        try {
            Log.info("Starting application");

            org.apache.log4j.PropertyConfigurator.configure(getProperties("log4j.properties"));

            //JmxReporter.forRegistry(metricRegistry).build().start();

            Properties producerProps = getProperties("producer.properties");
            Properties appProperties = getProperties("app.properties");
            Properties bandwidthWeights = getProperties("apikeysBandwidthWeights.properties");

            startGraphiteReporter(appProperties);

            int port = Integer.parseInt(appProperties.getProperty("port", "6306"));
            boolean useInternalMeter =
                    Integer.parseInt(appProperties.getProperty("useInternalMeter", "0")) > 0;

            eventSender = new EventSender(producerProps);
            httpServer = new HttpServer(eventSender, getValidatorFactory(), useInternalMeter,
                    appProperties, bandwidthWeights).listen(port);

            Log.info("Application started");
        } catch (Exception ex) {
            Log.error("Main error", ex);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(Application::shutdown));
        new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

    private static void startGraphiteReporter(Properties appProperties) {
        final String graphiteUrl = appProperties.getProperty("graphiteUrl", "graphite:2003");
        final String[] splittedGraphiteUrl = graphiteUrl.split(":");
        if (splittedGraphiteUrl.length < 2) {
            Log.error("invalid graphite url " + graphiteUrl);
        } else {
            final int graphitePort = Integer.parseInt(splittedGraphiteUrl[1]);
            final InetSocketAddress address = new InetSocketAddress(splittedGraphiteUrl[0], graphitePort);
            final Graphite graphite = new Graphite(address);
            final String graphitePrefix = appProperties
                    .getProperty("graphitePrefix", "vostok.test.airlock");
            final GraphiteReporter reporter = GraphiteReporter.forRegistry(metricRegistry)
                    .prefixedWith(graphitePrefix)
                    //.convertRatesTo(TimeUnit.SECONDS)
                    //.convertDurationsTo(TimeUnit.MILLISECONDS)
                    //.filter(MetricFilter.ALL)
                    .build(graphite);
            final int graphiteRetentionSeconds = Integer.parseInt(appProperties
                    .getProperty("graphiteRetentionSeconds", "60"));
            reporter.start(graphiteRetentionSeconds, TimeUnit.SECONDS);
            Log.info("Started graphite reporter at " + address.toString() + " with prefix '" + graphitePrefix + "'");
        }
    }

    private static void shutdown() {
        Log.info("Shutdown signal triggered");
        httpServer.shutdown();
        Log.info("HTTP server stopped");

        eventSender.shutdown();
        Log.info("Event sender stopped");
    }

    private static ValidatorFactory getValidatorFactory() throws IOException {
        Properties apiKeysProps = Application.getProperties("apikeys.properties");
        Map<String, String[]> apiKeysToRoutingKeyPatterns = new HashMap<>();
        for (String key : apiKeysProps.stringPropertyNames()) {
            String[] routingKeyPatterns = apiKeysProps.getProperty(key, "").trim()
                    .split("\\s*,\\s*");
            apiKeysToRoutingKeyPatterns.put(key, routingKeyPatterns);
        }
        return new ValidatorFactory(apiKeysToRoutingKeyPatterns);
    }

    private static InputStream getConfigStream(String configName) throws FileNotFoundException {
        String programDataDir;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            programDataDir = System.getenv("ProgramData");
        } else {
            programDataDir = "/etc";
        }
        File file = Paths.get(programDataDir, "vostok", "airlock-gate", configName).toFile();

        if (file.exists()) {
            Log.info("Using configuration from file " + file.getAbsolutePath());
            return new FileInputStream(file);
        } else {
            Log.info("Using configuration from resource " + configName);
            return Application.class.getClassLoader().getResourceAsStream(configName);
        }
    }

    private static Properties getProperties(String configName) throws IOException {
        Properties properties = new Properties();
        InputStream configStream = getConfigStream(configName);
        properties.load(configStream);
        return properties;
    }
}
