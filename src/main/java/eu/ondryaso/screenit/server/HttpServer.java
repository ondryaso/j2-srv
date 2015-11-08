package eu.ondryaso.screenit.server;

import fi.iki.elonen.NanoHTTPD;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpServer extends NanoHTTPD {
    private IResponseManager defaultResponseManager;
    private HashMap<String, IResponseManager> responseManagers;
    private ImageManager img;
    private ReturnFormat returnFormat;

    public HttpServer(ImageManager imageManager, int port, ReturnFormat returnFormat) {
        super(port);

        this.setTempFileManagerFactory(eu.ondryaso.screenit.server.TempFile::new);
        this.img = imageManager;
        this.returnFormat = returnFormat;
        this.loadResponseManagers();
    }

    /**
     * Registers {@link IResponseManager} classes. Then you can make requests that will be handled by requested
     * ResponseManager using specified key. For example, http://localhost/push/json/ would return JSON in case of errors.
     * {@link DefaultResponseManager} should always be present in the list for the client library to work.
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

        if (uriParts.length > 2 && this.responseManagers.containsKey(uriParts[uriParts.length - 1])) {
            response = this.responseManagers.get(uriParts[uriParts.length - 1]);
        } else {
            response = this.defaultResponseManager;
        }

        try {
            if (uriParts.length > 1)
                try {
                    if (uriParts[1].equalsIgnoreCase("push")) {
                        return new Response(Response.Status.OK, response.getMimeForPush(),
                                response.getResponseForPush(this.pushImage(session)));
                    }

                    if (uriParts[1].startsWith("i")) {
                        file = uriParts[1].substring(uri.indexOf("i"));
                        boolean jpg;

                        if (this.returnFormat == ReturnFormat.ALWAYS_JPG) {
                            jpg = true;
                        } else if (this.returnFormat == ReturnFormat.ALWAYS_PNG) {
                            jpg = false;
                        } else {
                            boolean uepng = false, uejpg = false;

                            if (uriParts.length > 2) {
                                uepng = uriParts[2].equalsIgnoreCase("png");
                                uejpg = uriParts[2].equalsIgnoreCase("jpg") || uriParts[2].equalsIgnoreCase("jpeg");
                            }

                            if (uepng)
                                jpg = false;
                            else if (uejpg)
                                jpg = true;
                            else {
                                if (this.returnFormat == ReturnFormat.BY_SIZE)
                                    jpg = this.img.isJpgSmaller(file);
                                else
                                    jpg = this.returnFormat == ReturnFormat.PREFER_JPG;
                            }
                        }

                        return new Response(Response.Status.OK, "image/" + (jpg ? "jpeg" : "png"), this.img.popFile(file, jpg));
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
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return new Response(Response.Status.INTERNAL_ERROR, response.getMimeForPushError(),
                    response.getResponseForPushError(e));
        }
    }

    /**
     * Takes file from POST, checks it and saves it with a random name, returns image URL name if no error happened.
     * Because of NanoHTTPD mechanisms, the POST has to be of a multipart/form-data type with proper boundary
     * containing one PNG file with proper Content-Type and Content-Disposition with field and file name set.
     *
     * @param session HTTP request to work with
     * @return name of image without extension that you can use for requesting (like /ixxxxxx)
     * @throws IOException       If parsing or image loading exception occurs.
     * @throws NotImageException If the file provided is not a png image.
     * @see fi.iki.elonen.NanoHTTPD.TempFileManagerFactory
     * @see ImageManager#pushFile(BufferedImage)
     */
    private String pushImage(NanoHTTPD.IHTTPSession session) throws IOException, NotImageException {
        Map<String, String> files = new HashMap<>();

        try {
            session.parseBody(files);
        } catch (NanoHTTPD.ResponseException e) {
            throw new IOException(e);
        }

        if (files.size() > 0) {
            File declaredImage = new File(files.values().iterator().next());

            if (!this.img.isPngImage(declaredImage))
                throw new NotImageException(null);

            return this.img.pushFile(ImageIO.read(declaredImage));
        } else {
            throw new NotImageException("request doesn't contain any image");
        }
    }
}
