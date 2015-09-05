package eu.ondryaso.screenit.server;

import com.sun.imageio.plugins.png.PNGImageReader;

import javax.imageio.*;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

/**
 * A class that handles image downloading and uploading
 */
public class ImageManager {
    private File saveDir;
    private Random rnd = new Random();

    /**
     * Initializes ImageManager class
     * @param saveDirectory Directory for loading and saving pictures
     */
    public ImageManager(File saveDirectory) {
        this.saveDir = saveDirectory;
    }

    /**
     * Returns an InputStream of a specified image
     * @param fileName the image file name. Be aware to not pass the image URL name that you'll get from the request, but only
     *                 the file name without the "i" on the beginning
     * @param jpg if true, jpg form will be returned.
     * @return an InputStream of the required image
     * @throws FileNotFoundException If the file required doesn't exist.
     */
    public InputStream popFile(String fileName, boolean jpg) throws FileNotFoundException {
        File toPop = new File(this.saveDir, fileName + (jpg ? ".jpeg" : ".png"));

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
     * Decides whether is JPG form of image smaller than the PNG one.
     * @param fileName the image file name.
     * @return true if the jpg is smaller
     */
    public boolean isJpgSmaller(String fileName) {
        File pngFile = new File(this.saveDir, fileName + ".png");
        File jpgFile = new File(this.saveDir, fileName + ".jpeg");

        return (jpgFile.length() < pngFile.length());
    }

    /**
     * Takes an image and saves it under a random name.
     * This method will save both jpg and png images, so the server doesn't have to calculate it on every request.
     * @param img image to save.
     * @return name of image without extension
     * @throws IOException When parsing or image loading exception occurs.

     */
    public String pushFile(BufferedImage img) throws IOException {
        String name = this.generateName();

        File newFile = new File(this.saveDir, name + ".png");
        while (newFile.exists()) {
            name = this.generateName();
            newFile = new File(this.saveDir, name + ".png");
        }

        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage newImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] rgb = img.getRGB(0, 0, w, h, null, 0, w);
        newImage.setRGB(0, 0, w, h, rgb, 0, w);

        this.saveJpg(newImage, new File(this.saveDir, name + ".jpeg"));
        ImageIO.write(img, "png", newFile);

        return name;
    }

    private void saveJpg(BufferedImage image, File file) throws IOException {
        ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
        jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpgWriteParam.setCompressionQuality(0.85f);

        jpgWriter.setOutput(new FileImageOutputStream(file));
        IIOImage outputImage = new IIOImage(image, null, null);
        jpgWriter.write(null, outputImage, jpgWriteParam);
        jpgWriter.dispose();
    }

    /**
     * Checks if the file is a PNG image using {@link ImageIO#getImageReaders(Object)}.
     * @param f a file to check
     * @return True if the file is a PNG image.
     */
    public boolean isPngImage(File f) {
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
