package eu.ondryaso.screenit.server;

import fi.iki.elonen.NanoHTTPD;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

public class Main extends NanoHTTPD {
    private static final boolean DEBUG = true;
    public static void main(String[] args) throws IOException, InterruptedException {
        Main m = Main.DEBUG ? new Main(80, "E:\\ScreenIt", true) :
                new Main(args.length > 1 ? Integer.parseInt(args[1]) : 6927,
                        args[0],
                        args.length > 2 && args[2].equalsIgnoreCase("-copy"));

        m.start();

        while (System.in.available() <= 0) {
            Thread.sleep(10000);
        }

        m.stop();
    }

    private ImageManager img;
    private IResponseManager defaultResponseManager;
    private HashMap<String, IResponseManager> responseManagers;

    public Main(int port, String dir, boolean useCopyright) {
        super(port);

        this.setTempFileManagerFactory(eu.ondryaso.screenit.server.TempFile::new);
        this.img = new ImageManager(useCopyright, new File(dir));
        this.loadResponseManagers();

        System.out.println("Running");
    }

    /**
     * Registers {@link IResponseManager} classes. Then you can make requests that will be handled by requested
     * ResponseManager using specified key. For example, http://localhost/push/json/ would return JSON in case of errors.
     */
    private void loadResponseManagers() {
        this.defaultResponseManager = new DefaultResponseManager();

        this.responseManagers = new HashMap<>(2);
        this.responseManagers.put("default", this.defaultResponseManager);
        this.responseManagers.put("json", new JsonResponseManager());
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        String uriParts[] = uri.split("/");
        String file = null;
        IResponseManager response;

        if(uriParts.length > 2 && this.responseManagers.containsKey(uriParts[2])) {
            response = this.responseManagers.get(uriParts[2]);
        } else {
            response = this.defaultResponseManager;
        }

        try {
            if (uriParts[1].equalsIgnoreCase("push")) {
                return new Response(Response.Status.OK, response.getMimeForPush(),
                        response.getResponseForPush("i" + this.img.pushFile(session)));
            }

            if (uriParts[1].startsWith("i") && uriParts[1].endsWith(".png")) {
                file = uriParts[1].substring(uri.indexOf("i"));
                return new Response(Response.Status.OK, "image/png", this.img.popFile(file));
            }
        } catch (FileNotFoundException e) {
            return new Response(Response.Status.NOT_FOUND, response.getMimeForImageNotFound(),
                    response.getResponseForImageNotFound(file, e));
        } catch (IOException e) {
            return new Response(Response.Status.INTERNAL_ERROR, response.getMimeForPushError(),
                    response.getResponseForPushError(e));
        } catch (NotImageException e) {
            return new Response(Response.Status.BAD_REQUEST, response.getMimeForBadImage(),
                    response.getResponseForBadImage(e));
        }

        return new Response(Response.Status.NOT_FOUND, response.getMimeForNotFound(), response.getResponseForNotFound(uri));
    }

}
