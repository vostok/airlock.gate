package ru.kontur.airlock;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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
            org.apache.log4j.BasicConfigurator.configure();

            JmxReporter.forRegistry(metricRegistry).build().start();

            Properties producerProps = getProperties("producer.properties");
            Properties appProperties = getProperties("app.properties");
            int port = Integer.parseInt(appProperties.getProperty("port", "6306"));
            boolean useInternalMeter =
                    Integer.parseInt(appProperties.getProperty("useInternalMeter", "0")) > 0;

            eventSender = new EventSender(producerProps);
            httpServer = new HttpServer(eventSender, getValidatorFactory(), useInternalMeter,appProperties).listen(port);

            Log.info("Application started");
        } catch (Exception ex) {
            Log.error("Main error", ex);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(Application::shutdown));
        new BufferedReader(new InputStreamReader(System.in)).readLine();
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
