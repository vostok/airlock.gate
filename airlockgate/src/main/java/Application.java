import org.rapidoid.net.Server;

import java.io.*;
import java.nio.file.Paths;
import java.util.Properties;

public class Application {
    public static void main(String[] args) throws Exception {
        //((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.kafka")).setLevel(Level.INFO);
        try {
            Properties producerProps = new Properties();
            String programDataDir;
            if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
                programDataDir = System.getenv("ProgramData");
            } else {
                programDataDir = "/etc";
            }
            File producerPropertiesFile = Paths.get(programDataDir, "kontur", "airlock-gate", "producer.properties").toFile();
            InputStream inputStream = producerPropertiesFile.exists() ?
                    new FileInputStream(producerPropertiesFile) :
                    Application.class.getClassLoader().getResourceAsStream("default.producer.properties");
            //load a properties file from class path, inside static method
            producerProps.load(inputStream); //);
            Server httpServer = new HttpServer(new EventSender(producerProps)).listen(8889);
            //System.out.println(producerProps.toString());
            new BufferedReader(new InputStreamReader(System.in)).readLine();
            httpServer.shutdown();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
