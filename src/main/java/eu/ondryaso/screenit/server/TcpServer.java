package eu.ondryaso.screenit.server;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class TcpServer implements Runnable {
    private IResponseManager defaultResponseManager;
    private HashMap<Byte, IResponseManager> responseManagers;
    private ImageManager img;
    private ReturnFormat returnFormat;
    private int port;
    private boolean stopped = false;

    public TcpServer(ImageManager imageManager, int port, ReturnFormat returnFormat) {
        this.img = imageManager;
        this.returnFormat = returnFormat;
        this.port = port;
        this.loadResponseManagers();
    }

    public synchronized void stop() {
        this.stopped = true;
    }

    /**
     * Registers {@link IResponseManager} classes. Then you can make requests that will be handled by requested
     * ResponseManager using specified key. For example, http://localhost/push/json/ would return JSON in case of errors.
     * {@link DefaultResponseManager} should always be present in the list for the client library to work.
     */
    private void loadResponseManagers() {
        this.defaultResponseManager = new DefaultResponseManager();

        this.responseManagers = new HashMap<>(2);
        this.responseManagers.put((byte) 0, this.defaultResponseManager);
        this.responseManagers.put((byte) 1, new JsonResponseManager());
    }

    @Override
    public void run() {
        ServerSocket socket;

        try {
            socket = new ServerSocket(this.port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while (!this.stopped) {
            try {
                Socket client = socket.accept();
                new Thread(new Worker(this, client)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    class Worker implements Runnable {
        private TcpServer server;
        private Socket client;

        public Worker(TcpServer server, Socket client) {
            this.server = server;
            this.client = client;
        }

        @Override
        public void run() {
            try {
                InputStream in = this.client.getInputStream();
                OutputStream out = this.client.getOutputStream();

                byte[] data = this.readInputStream(in);
                this.handleRequest(out, data);
                out.write(new byte[]{23, 3, 4});

                out.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //TODO: this method can't be trusted, it should be rewritten immediately (8.11.15)
        private byte[] readInputStream(InputStream inputStream) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                byte[] buf = new byte[4096];
                int n = 0;

                while ((n = inputStream.read(buf)) != -1) {
                    if (n >= 3)
                        if (buf[n - 3] == 23 && buf[n - 2] == 3 && buf[n - 1] == 4)
                            break;

                    try {
                        baos.write(buf, 0, n);
                    } catch(RuntimeException e) {
                        continue;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return baos.toByteArray();
        }

        private void handleRequest(OutputStream out, byte[] data) {
            IResponseManager response = this.server.responseManagers.containsKey(data[1])
                    ? this.server.responseManagers.get(data[1]) : this.server.defaultResponseManager;

            byte[] realData = new byte[data.length - 3];
            System.arraycopy(data, 3, realData, 0, realData.length);
            String name = null;

            try {
                if (data[0] == 0) {
                    //PUSH
                    try {
                        BufferedImage img = ImageIO.read(new ByteArrayInputStream(realData));

                        try {
                            name = this.server.img.pushFile(img);
                        } catch (IOException e) {
                            out.write(3);
                            out.write(response.getResponseForPushError(e).getBytes("UTF-8"));
                            return;
                        }
                    } catch (IOException e) {
                        out.write(2);
                        out.write(response.getResponseForBadImage(e).getBytes("UTF-8"));
                        return;
                    }

                    byte[] resp = response.getResponseForPush(name).getBytes("UTF-8");

                    out.write(0);
                    out.write(resp);
                } else if (data[0] == 1) {
                    //PULL
                    name = new String(realData, "UTF-8");
                    boolean jpg = willBeJpg(data[2], name);
                    try {
                        byte[] resp = this.pullImage(jpg, name);
                        out.write(0);
                        out.write(jpg ? 1 : 0);
                        out.write(resp);
                    } catch (FileNotFoundException e) {
                        out.write(1);
                        out.write(2);
                        out.write(response.getResponseForImageNotFound(name, e).getBytes("UTF-8"));
                    }
                } else {
                    out.write(4);
                    out.write(response.getResponseForBadProtocol().getBytes("UTF-8"));
                }
            } catch (IOException ignored) {
            }
        }

        private boolean willBeJpg(int flag, String name) {
            boolean jpg;

            if (this.server.returnFormat == ReturnFormat.ALWAYS_JPG) {
                jpg = true;
            } else if (this.server.returnFormat == ReturnFormat.ALWAYS_PNG) {
                jpg = false;
            } else {
                if (flag == 0)
                    jpg = false;
                else if (flag == 1)
                    jpg = true;
                else {
                    if (this.server.returnFormat == ReturnFormat.BY_SIZE)
                        jpg = this.server.img.isJpgSmaller(name);
                    else
                        jpg = this.server.returnFormat == ReturnFormat.PREFER_JPG;
                }
            }

            return jpg;
        }

        private byte[] pullImage(boolean jpg, String name) throws FileNotFoundException {
            return this.readInputStream(this.server.img.popFile(name, jpg));
        }
    }
}
