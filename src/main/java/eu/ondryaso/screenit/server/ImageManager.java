package eu.ondryaso.screenit.server;

import com.sun.imageio.plugins.png.PNGImageReader;
import fi.iki.elonen.NanoHTTPD;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * A class that handles image downloading and uploading
 */
public class ImageManager {
    public boolean useCopyright;
    private File saveDir;
    private Random rnd = new Random();

    /**
     * Initializes ImageManager class
     * @param useCopyright If true, images with copyright will be provided
     * @param saveDirectory Directory for loading and saving pictures
     */
    public ImageManager(boolean useCopyright, File saveDirectory) {
        this.useCopyright = useCopyright;
        this.saveDir = saveDirectory;
    }

    /**
     * Returns an InputStream of a specified image or it's version with the copyright, depending on {@link ImageManager#useCopyright}
     * @param fileName the image file name. Be aware to not pass the image URL name that you'll get from the request, but only
     *                 the file name without the "i" on the beginning
     * @return an InputStream of the required image
     * @throws FileNotFoundException If the file required doesn't exist.
     */
    public InputStream popFile(String fileName) throws FileNotFoundException {
        if(this.useCopyright)
            fileName = fileName.replace(".png", "_copy.png");

        File toPop = new File(this.saveDir, fileName);

        if(fileName.contains("/") || fileName.contains("\\") || !toPop.exists())
            throw new FileNotFoundException("Image doesn't xizt");


        try {
            return new FileInputStream(toPop);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Takes file from POST, checks it and saves it with a random name, returns image URL name if no error happened.
     * Because of NanoHTTPD mechanisms, the POST has to be of a multipart/form-data type with proper boundary
     * containing one PNG file with proper Content-Type and Content-Disposition with field and file name set. This method
     * will save a version either with and without the copyright, so the server doesn't have to calculate it on every request.
     * @param session a HttpSession containing the request. NanoHTTPD instance has to have a working {@link NanoHTTPD.TempFileManagerFactory}
     *                set.
     * @return an image URL name (that's the one you write to the URL like /ixxxxxx.png)
     * @throws IOException When parsing or image loading exception occurs.
     * @throws NotImageException If file is not a PNG image or request doesn't contain any file.
     * @see ImageManager#makeCopyright(BufferedImage)
     * @see fi.iki.elonen.NanoHTTPD.TempFileManagerFactory
     */
    public String pushFile(NanoHTTPD.IHTTPSession session) throws IOException, NotImageException {
        String name = this.generateName() + ".png";
        File newFile = new File(this.saveDir, name);
        while (newFile.exists()) {
            name = this.generateName() + ".png";
            newFile = new File(this.saveDir, name);
        }

        Map<String, String> files = new HashMap<>();

        try {
            session.parseBody(files);
        } catch (NanoHTTPD.ResponseException e) {
            throw new IOException(e);
        }

        if(files.size() > 0) {
            File declaredImage = new File(files.values().iterator().next());
            if(!isPngImage(declaredImage))
                throw new NotImageException(null);

            BufferedImage i = ImageIO.read(declaredImage);
            this.makeCopyright(i);
            ImageIO.write(i, "png", new File(this.saveDir, name.replace(".png", "_copy.png")));

            Files.copy(declaredImage.toPath(), newFile.toPath());
        } else {
            throw new NotImageException("request doesn't contain any image");
        }

        return name;
    }

    /**
     * Draws a custom copyright on an image. Change as you wish.
     * @param i the image to put the copyright on
     */
    private void makeCopyright(BufferedImage i) {
        Graphics2D g = i.createGraphics();
        g.setColor(Color.WHITE);
        g.drawString("Kopurith LeOndrasheq 20420 plox", 0, 10);
        g.dispose();
    }

    /**
     * Checks if the file is a PNG image using {@link ImageIO#getImageReaders(Object)}.
     * @param f a file to check
     * @return True if the file is a PNG image.
     */
    private boolean isPngImage(File f) {
        try {
            ImageInputStream s = ImageIO.createImageInputStream(f);
            Iterator<ImageReader> r = ImageIO.getImageReaders(s);
            if(!r.hasNext())
                return false;

            if(!(r.next() instanceof PNGImageReader))
                return false;
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Generates a random, six-letter String that's used as a file name.
     * @return Random String
     */
    private String generateName() {
        String u = UUID.randomUUID().toString();
        String r = "";

        for (int i = 0; i < 6; i++) {
            r += u.charAt(rnd.nextInt(32));
        }

        r = r.replace('-', '6');
        return r;
    }
}
