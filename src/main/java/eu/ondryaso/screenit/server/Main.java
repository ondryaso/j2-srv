package eu.ondryaso.screenit.server;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, ConfigurationException {
        Configuration conf = null;

        if(args.length >= 1) {
            if(!args[0].equalsIgnoreCase("-debug")) {
                conf = new XMLConfiguration(new File(args[0]));
            }
        } else {
            System.out.println("Usage: java -jar SIServer.jar <config file path>");
            System.exit(1);
        }

        Main m = (conf == null) ? new Main(80, 1337, "E:\\ScreenIt", ReturnFormat.BY_SIZE) :
                new Main(conf.getInt("port", 80), conf.getInt("tcpPort", 0),
                        conf.getString("dir", "."), Enum.valueOf(ReturnFormat.class,
                        conf.getString("returnFormat", "BY_SIZE")));


        while (System.in.available() <= 0) {
            Thread.sleep(60000);
        }

        System.out.println("Stopping");
        m.stop();
    }

    private ImageManager img;
    private HttpServer http;
    private TcpServer tcp;

    public Main(int port, int tcpPort, String dir, ReturnFormat returnFormat) throws IOException {

        this.img = new ImageManager(new File(dir));

        if(port != 0) {
            this.http = new HttpServer(img, port, returnFormat);
            this.http.start();
        }

        if(tcpPort != 0) {
            this.tcp = new TcpServer(img, tcpPort, returnFormat);
            Thread t = new Thread(this.tcp);
            t.start();
        }

        System.out.println("Running");
    }

    public void stop() throws InterruptedException {
        if(this.http != null)
            this.http.stop();
        if(this.tcp != null)
            this.tcp.stop();

        Thread.sleep(1000);
        System.exit(0);
    }


}
