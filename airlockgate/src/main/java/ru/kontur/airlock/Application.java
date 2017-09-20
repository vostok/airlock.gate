package ru.kontur.airlock;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import org.rapidoid.log.Log;
import org.rapidoid.net.Server;

import java.io.*;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Application {

    private static Server httpServer;
    public static final MetricRegistry metricRegistry = new MetricRegistry();

    private static void initMetrics() {
        final JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).build();
        reporter.start();
//        final ConsoleReporter consoleReporter = ConsoleReporter.forRegistry(metricRegistry)
//                .convertRatesTo(TimeUnit.SECONDS)
//                .convertDurationsTo(TimeUnit.MILLISECONDS)
//                .build();
//        consoleReporter.start(1, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws Exception {
        //((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.kafka")).setLevel(Level.INFO);
        run();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));
        new BufferedReader(new InputStreamReader(System.in)).readLine();
        //shutdown();
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
            httpServer = new HttpServer(new EventSender(producerProps)).listen(port);
            Log.info("Server started");
        } catch (Exception ex) {
            logEx(ex);
        }
    }

    public static void logEx(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        Log.error(sw.toString());
    }

    private static InputStream getConfigStream(String configName) throws FileNotFoundException {
        String programDataDir;
        if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
            programDataDir = System.getenv("ProgramData");
        } else {
            programDataDir = "/etc";
        }
        File file = Paths.get(programDataDir, "kontur", "airlock-gate", configName).toFile();
        return file.exists() ?
                new FileInputStream(file) :
                Application.class.getClassLoader().getResourceAsStream(configName);
    }

    public static Properties getProperties(String configName) throws IOException {
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
