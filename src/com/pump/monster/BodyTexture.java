package com.pump.monster;

import com.pump.data.converter.ConverterUtils;
import com.pump.image.pixel.Scaling;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

// TODO: it'd be great if we could replace these with vector textures. That would
// reduce both the size of the jar used to render everything *and* the output vector graphics.
public class BodyTexture {

    private static final Map<String, BodyTexture> namedTextures = new TreeMap<>();

    public static final BodyTexture NONE = new BodyTexture(new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB), 1, "NONE") {
        @Override
        public void paint(Graphics2D g, Shape shape, int opacity, int randomSeed) {
            // intentionally empty
        }
    };

    /**
     * Derived from https://www.pexels.com/photo/white-and-gray-rough-surface-6104788/
     */
    public static BodyTexture ROCK = new BodyTexture(BodyTexture.class.getResource("resources/pexels-eva-bronzini-6104788.png"), 8.0, "ROCK");

    /**
     * Derived from https://www.pexels.com/photo/concrete-wall-with-rough-surface-3874048/
     */
    public static BodyTexture CONCRETE = new BodyTexture(BodyTexture.class.getResource("resources/pexels-ready-made-3874048.png"), 8.0, "CONCRETE");

    /**
     * Derived from https://www.pexels.com/photo/close-up-photograph-of-an-asphalt-surface-11254992/
     */
    public static BodyTexture ASPHALT = new BodyTexture(BodyTexture.class.getResource("resources/pexels-seamlesstextures-11254992.png"), 8.0, "ASPHALT_DARK");

    /**
     * Derived from https://www.pexels.com/photo/white-painted-wall-1939485/
     */
    public static BodyTexture WALL = new BodyTexture(BodyTexture.class.getResource("resources/pexels-henry-&-co-1939485.png"), 8.0, "WALL");

    /**
     * Derived from https://www.pexels.com/photo/black-and-white-colors-painting-9175713/
     */
    public static BodyTexture PAINTING = new BodyTexture(BodyTexture.class.getResource("resources/pexels-kseniya-lapteva-9175713.png"), 8.0, "PAINTING");

    /**
     * Derived from https://www.pexels.com/photo/texture-of-brown-rough-linen-7794364/
     */
    public static BodyTexture LINEN = new BodyTexture(BodyTexture.class.getResource("resources/pexels-monstera-7794364.png"), 8.0, "LINEN");

    /**
     * Derived from https://www.pexels.com/photo/full-frame-of-brown-animal-fur-5840850/
     */
    public static BodyTexture FUR_WIRY = new BodyTexture(BodyTexture.class.getResource("resources/pexels-karolina-grabowska-5840850.png"), 8.0, "FUR_WIRY");

    /**
     * Derived from https://www.pexels.com/photo/brown-furry-surface-11891461/
     */
    public static BodyTexture FUR_THICK = new BodyTexture(BodyTexture.class.getResource("resources/pexels-jennifer-dridiger-11891461.png"), 8.0, "FUR_THICK");

    /**
     * Derived from https://www.pexels.com/photo/texture-of-fluffy-curly-sheep-wool-4073926/
     */
    public static BodyTexture SHEEP = new BodyTexture(BodyTexture.class.getResource("resources/pexels-peter-holmboe-4073926.png"), 8.0, "SHEEP");


    // TODO: manually create tile of https://www.pexels.com/photo/black-and-white-cheetah-pattern-8767366/

    /**
     * Derived from https://www.pexels.com/photo/moss-11255695/
     */
    public static BodyTexture MOSS = new BodyTexture(BodyTexture.class.getResource("resources/pexels-seamlesstextures-11255695.png"), 8.0, "MOSS");

    /**
     * Derived from https://www.pexels.com/photo/leaves-11255718/
     */
    public static BodyTexture SPECKLED = new BodyTexture(BodyTexture.class.getResource("resources/pexels-seamlesstextures-11255718.png"), 8.0, "SPECKLED");

    public static Map<String, BodyTexture> getTextures() {
        return Collections.unmodifiableMap(namedTextures);
    }

    URL url;
    TexturePaint texturePaint;
    double resolution;
    String name;
    BufferedImage image;

    /**
     *
     * @param url
     * @param resolution the resolution this image should be scaled. This should be at least 2 for
     *                   the texture to look good on high-resolution monitors (some monitors can offer 350+% resolution),
     *                   or it should be at least 4 to look good on printed paper.
     */
    public BodyTexture(URL url, double resolution) {
        this(url, resolution, null);
    }

    private BodyTexture(BufferedImage image, double resolution, String name) {
        this.image = Objects.requireNonNull(image);
        this.resolution = resolution;
        this.name = name;
        if (name != null && namedTextures.put(name, this) != null)
            throw new IllegalArgumentException("Multiple textures named \""+name+"\"");
    }

    private BodyTexture(URL url, double resolution, String name) {
        this.url = Objects.requireNonNull(url);
        this.resolution = resolution;
        this.name = name;
        if (name != null && namedTextures.put(name, this) != null)
            throw new IllegalArgumentException("Multiple textures named \""+name+"\"");
    }

    @Override
    public String toString() {
        if (name != null)
            return name;

        return "BodyTexture[ "+url.toString()+" ]";
    }

    /**
     *
     * @param g
     * @param shape
     * @param opacity an int between [0, 255], where 0 = transparent adn 255 = opaque. The recommended
     *                values here are about 10-50, depending on how visible this texture should be.
     * @param randomSeed
     */
    public void paint(Graphics2D g, Shape shape, int opacity, int randomSeed) {
        Random r = new Random(randomSeed);
        double dx = r.nextDouble() * 1000;
        double dy = r.nextDouble() * 1000;

        Composite c = g.getComposite();
        if (c instanceof AlphaComposite) {
            AlphaComposite ac = (AlphaComposite) c;
            if (ac.getRule() == AlphaComposite.SRC_OVER) {
                g = (Graphics2D) g.create();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity * ac.getAlpha() / 255f));
            } else {
                AlphaComposite myComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
                throw new IllegalArgumentException("cannot combine two different AlphaComposite rules: " + ConverterUtils.toString(c) + " vs " + ConverterUtils.toString(myComposite));
            }
        } else {
            AlphaComposite myComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
            throw new IllegalArgumentException("cannot combine two different Composite types: " + ConverterUtils.toString(c) + " vs " + ConverterUtils.toString(myComposite));
        }

        TexturePaint p = null;
        try {
            p = getPaint();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // translate our rect. The texture should be a seamless tile, so where it starts shouldn't matter:
        Rectangle2D translatedRect = p.getAnchorRect();
        translatedRect.setRect(dx, dy, translatedRect.getWidth(), translatedRect.getHeight());
        p = new TexturePaint(p.getImage(), translatedRect);

        g.setPaint(p);
        g.fill(shape);
    }

    public TexturePaint getPaint() throws IOException {
        if (texturePaint == null) {
            texturePaint = createPaint();
        }
        return texturePaint;
    }

    protected TexturePaint createPaint() throws IOException {

        String name = url == null ? "none" : url.toString();
        name = name.substring(name.lastIndexOf("/") + 1);

        BufferedImage image = this.image != null ? this.image : ImageIO.read(url);
//        if (name.endsWith(".jpg")) {
//            image = createAndResaveTile(image, name + ".png");
//        }

        Rectangle2D rect = new Rectangle2D.Double(0,0,image.getWidth() / resolution, image.getHeight() / resolution);
        return new TexturePaint(image, rect);
    }

//    /**
//     * This converts a megapixel image to a tiling image. It was used to set up some of the constants in this class.
//     */
//    private BufferedImage createAndResaveTile(BufferedImage image, String filename) throws IOException {
//        BufferedImage returnValue = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
//        Graphics2D g = returnValue.createGraphics();
//        g.drawImage(image, 0, 0, null);
//        g.dispose();
//
//        if (!filename.contains("seamless")) {
//            int tileWidth = (int) Math.min(image.getWidth(), 150 * resolution);
//            int tileHeight = (int) Math.min(image.getHeight(), 150 * resolution);
//
//            returnValue = tileRGBHorizontally(returnValue, tileWidth, tileHeight);
//            returnValue = invertXY(returnValue);
//            returnValue = tileRGBHorizontally(returnValue, tileHeight, tileWidth);
//            returnValue = invertXY(returnValue);
//        } else {
//            int tileWidth = (int) Math.min(image.getWidth(), 150 * resolution);
//            int tileHeight = (int) Math.min(image.getHeight(), 150 * resolution);
//            returnValue = Scaling.scale(returnValue, tileWidth, tileHeight);
//
//        }
//
//        File file1 = new File("tile-" + filename);
//        ImageIO.write(returnValue, "png", file1);
//
//        int[] row = new int[returnValue.getWidth()];
//        int minGray = -1;
//        int maxGray = -1;
//        for (int y = 0; y < returnValue.getHeight(); y++) {
//            returnValue.getRaster().getDataElements(0, y, row.length, 1, row);
//            for (int x = 0; x < row.length; x++) {
//                int rgb = row[x];
//                int red = (rgb >> 16) & 0xff;
//                int green = (rgb >> 8) & 0xff;
//                int blue = rgb & 0xff;
//                int gray = (red + green + blue) / 3;
//                if (minGray == -1) {
//                    minGray = maxGray = gray;
//                } else {
//                    minGray = Math.min(minGray, gray);
//                    maxGray = Math.max(maxGray, gray);
//                }
//                row[x] = gray;
//            }
//            returnValue.getRaster().setDataElements(0,y,row.length,1,row);
//        }
//
//        int grayRange = maxGray - minGray;
//
//        BufferedImage grayImage = new BufferedImage(returnValue.getWidth(), returnValue.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
//        byte[] byteRow = new byte[row.length];
//        // now we've populated minGray/maxGray:
//        for (int y = 0; y < returnValue.getHeight(); y++) {
//            returnValue.getRaster().getDataElements(0,y,row.length,1,row);
//            for (int x = 0; x < row.length; x++) {
//                int gray = row[x];
//                gray = (gray - minGray) * 255 / grayRange;
//                byteRow[x] = (byte) gray;
//            }
//            grayImage.getRaster().setDataElements(0,y,row.length,1,byteRow);
//        }
//        returnValue = grayImage;
//
//        File file2 = new File(filename);
//        ImageIO.write(returnValue, "png", file2);
//        System.out.println(file2.getAbsolutePath());
//
//        return returnValue;
//    }
//
//    private BufferedImage tileRGBHorizontally(BufferedImage src, int tileWidth, int tileHeight) {
//        BufferedImage tile = new BufferedImage(tileWidth, src.getHeight(), BufferedImage.TYPE_INT_ARGB);
//        int[] row1 = new int[src.getWidth()];
//        int[] row2 = new int[tileWidth];
//
//        int srcX = src.getWidth() / 2 - tileWidth / 2;
//        int seamWidth = 80;
//        int waveWidth = 80;
//
//        for (int dstY = 0; dstY < src.getHeight(); dstY++) {
//            src.getRaster().getDataElements(0, dstY, src.getWidth(), 1, row1);
//            double yF = ((double) dstY) / ((double) tileHeight);
//            double xOffset = .5 + .5 * Math.cos(Math.PI + 2 * yF * Math.PI);
//
//            int dstSeamStartX = (int)(tileWidth - xOffset * waveWidth - seamWidth);
//            int dstSeamEndX = dstSeamStartX + seamWidth;
//
//            for (int dstX = 0; dstX < tileWidth; dstX++) {
//                if (dstX < dstSeamStartX) {
//                    row2[dstX] = row1[srcX + dstX];
//                } else if (dstX > dstSeamEndX) {
//                    row2[dstX] = row1[srcX + dstX - tileWidth];
//                } else {
//                    double xF = ((double)(dstX - dstSeamStartX)/((double)seamWidth));
//
//                    int r1 = (row1[srcX + dstX] >> 16) & 0xff;
//                    int g1 = (row1[srcX + dstX] >> 8) & 0xff;
//                    int b1 = (row1[srcX + dstX] >> 0) & 0xff;
//
//                    int r2 = (row1[srcX + dstX - tileWidth] >> 16) & 0xff;
//                    int g2 = (row1[srcX + dstX - tileWidth] >> 8) & 0xff;
//                    int b2 = (row1[srcX + dstX - tileWidth] >> 0) & 0xff;
//
//                    int r = (int)(r1 * (1 - xF) + r2 * xF);
//                    int g = (int)(g1 * (1 - xF) + g2 * xF);
//                    int b = (int)(b1 * (1 - xF) + b2 * xF);
//
//                    row2[dstX] = (0xff000000) + (r << 16) + (g << 8) + (b);
//                }
//            }
//            tile.getRaster().setDataElements(0, dstY, tile.getWidth(), 1, row2);
//        }
//        return tile;
//    }
//
//    private BufferedImage invertXY(BufferedImage bi) {
//        BufferedImage returnValue = new BufferedImage(bi.getHeight(), bi.getWidth(), bi.getType());
//        for (int y = 0; y < bi.getHeight(); y++) {
//            for (int x = 0; x < bi.getWidth(); x++) {
//                returnValue.setRGB(y,x,bi.getRGB(x,y));
//            }
//        }
//        return returnValue;
//    }
}
