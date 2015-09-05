package eu.ondryaso.screenit.server;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, ConfigurationException {
        Configuration conf = null;

        if(args.length >= 1) {
            if(!args[0].equalsIgnoreCase("-debug"))
                conf = new DefaultConfigurationBuilder(args[0]).getConfiguration();
        } else {
            System.out.println("Usage: java -jar SIServer.jar <config file path>");
            System.exit(1);
        }

        Main m = (conf == null) ? new Main(80, 1337, "E:\\ScreenIt", ReturnFormat.BY_SIZE) :
                new Main(conf.getInt("port", 80), conf.getInt("tcpPort", 0),
                        conf.getString("dir", "."), Enum.valueOf(ReturnFormat.class,
                        conf.getString("returnFormat", "BY_SIZE")));


        while (System.in.available() <= 0) {
            Thread.sleep(10000);
        }

        m.stop();
    }

    private ImageManager img;
    private HttpServer http;

    public Main(int port, int tcpPort, String dir, ReturnFormat returnFormat) throws IOException {

        this.img = new ImageManager(new File(dir));
        this.http = new HttpServer(img, port, returnFormat);
        this.http.start();

        System.out.println("Running");
    }

    public void stop() {
        this.http.stop();
    }


}
