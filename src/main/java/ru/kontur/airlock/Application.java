package ru.kontur.airlock;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import org.rapidoid.log.Log;
import org.rapidoid.net.Server;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Application {
    private static Server httpServer;
    static final MetricRegistry metricRegistry = new MetricRegistry();

    private static void initMetrics() {
        final JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).build();
        reporter.start();
    }

    public static void main(String[] args) throws Exception {
        run();
        Runtime.getRuntime().addShutdownHook(new Thread(Application::shutdown));
        new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

    public static void run() {
        try {
            org.apache.log4j.PropertyConfigurator.configure(getProperties("log4j.properties"));
            org.apache.log4j.BasicConfigurator.configure();
            Log.info("Starting");
            initMetrics();
            Properties producerProps = getProperties("producer.properties");
            Properties appProperties = getProperties("app.properties");
            int port = Integer.parseInt(appProperties.getProperty("port", "8888"));
            httpServer = new HttpServer(new EventSender(producerProps), getAuthorizerFactory()).listen(port);
            Log.info("Server started");
        } catch (Exception ex) {
            logEx(ex);
        }
    }

    private static AuthorizerFactory getAuthorizerFactory() throws IOException {
        Properties apiKeysProps = Application.getProperties("apikeys.properties");
        Map<String, String[]> apiKeysToRoutingKeyPatterns = new HashMap<>();
        for (String key : apiKeysProps.stringPropertyNames()) {
            String[] routingKeyPatterns = apiKeysProps.getProperty(key, "").trim().split("\\s*,\\s*");
            apiKeysToRoutingKeyPatterns.put(key, routingKeyPatterns);
        }
        return new AuthorizerFactory(apiKeysToRoutingKeyPatterns);
    }

    private static void logEx(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        Log.error(sw.toString());
    }

    private static InputStream getConfigStream(String configName) throws FileNotFoundException {
        String programDataDir;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            programDataDir = System.getenv("ProgramData");
        } else {
            programDataDir = "/etc";
        }
        File file = Paths.get(programDataDir, "vostok", "airlock-gate", configName).toFile();
        return file.exists() ?
                new FileInputStream(file) :
                Application.class.getClassLoader().getResourceAsStream(configName);
    }

    private static Properties getProperties(String configName) throws IOException {
        Properties properties = new Properties();
        InputStream configStream = getConfigStream(configName);
        properties.load(configStream);
        return properties;
    }

    public static void shutdown() {
        httpServer.shutdown();
        Log.info("Server stopped");
    }
}
