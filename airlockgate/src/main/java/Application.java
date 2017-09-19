import org.rapidoid.log.Log;
import org.rapidoid.net.Server;

import java.io.*;
import java.nio.file.Paths;
import java.util.Properties;

public class Application {

    private static Server httpServer;

    public static void main(String[] args) throws Exception {
        //((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.kafka")).setLevel(Level.INFO);
        run();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));
        new BufferedReader(new InputStreamReader(System.in)).readLine();
        shutdown();
    }

    public static void run() {
        try {
            org.apache.log4j.PropertyConfigurator.configure(getProperties("log4j.properties"));
            org.apache.log4j.BasicConfigurator.configure();
            Log.info("Starting");
            Properties producerProps = getProperties("producer.properties");
            httpServer = new HttpServer(new EventSender(producerProps)).listen(8888);
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
        Properties producerProps = new Properties();
        InputStream configStream = getConfigStream(configName);
        producerProps.load(configStream);
        return producerProps;
    }

    public static void shutdown() {
        httpServer.shutdown();
        Log.info("Server stopped");
    }
}
