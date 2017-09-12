import org.rapidoid.log.Log;
import org.rapidoid.net.Server;

import java.io.*;
import java.nio.file.Paths;
import java.util.Properties;

public class Application {
    public static void main(String[] args) throws Exception {
        //((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.kafka")).setLevel(Level.INFO);
        try {
            org.apache.log4j.PropertyConfigurator.configure(getConfigStream("log4j.properties"));
            org.apache.log4j.BasicConfigurator.configure();
            Log.info("Starting");
            Properties producerProps = new Properties();
            InputStream inputStream = getConfigStream("producer.properties");
            producerProps.load(inputStream);
            Server httpServer = new HttpServer(new EventSender(producerProps)).listen(8889);
            Log.info("Server started");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(httpServer)));
            new BufferedReader(new InputStreamReader(System.in)).readLine();
            shutdown(httpServer);
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

    private static void shutdown(Server httpServer) {
        httpServer.shutdown();
        Log.info("Server stopped");
    }
}
