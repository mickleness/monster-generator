package com.pump.monster.build;

import com.pump.awt.Dimension2D;
import com.pump.geom.TransformUtils;
import com.pump.graphics.vector.VectorImage;
import com.pump.io.AdjacentFile;
import com.pump.io.FileTreeIterator;
import com.pump.io.IOUtils;
import com.pump.monster.Monster;
import com.pump.monster.render.MonsterRenderer;
import com.pump.monster.ui.MonsterFrame;
import com.pump.monster.ui.SVGWriter;
import com.pump.release.Project;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Random;

public class JarBuilder {
    static int EXAMPLE_SIZE = 20;

    public static void main(String[] args) throws IOException {
        File userDir = new File(System.getProperty("user.dir"));
        File libDir = FileTreeIterator.find(userDir, "lib");
        File[] jarFiles = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
        Project project = new Project(userDir, MonsterFrame.class, "monster-generator.jar");

        File versionedJarFile = project.buildJar(true, Arrays.asList(jarFiles),
                "release", "jars", MonsterFrame.VERSION);

        System.out.println("Created: " + versionedJarFile.getAbsolutePath());

        File jarDir = versionedJarFile.getParentFile().getParentFile();
        File genericJarFile = new File(jarDir, "monster-generator.jar");
        IOUtils.copy(versionedJarFile, genericJarFile);

        File releaseDir = new File(userDir, "release");
        File examplesDir = new File(releaseDir, "examples");
        File examplesVerDir = new File(examplesDir, MonsterFrame.VERSION);
        examplesVerDir.mkdirs();

        Random random = new Random();
        for (int a = 0; a < EXAMPLE_SIZE; a++) {
            Monster m = new Monster(random);
            writePNG(m, examplesVerDir, a, 300);
        }

        int svgCtr = 0;
        random = new Random(456789);
        while (svgCtr < EXAMPLE_SIZE) {
            Monster m = new Monster(random);
            if (m.includeTexture)
                continue;
            writeSVG(m, examplesVerDir, svgCtr, 300);
            svgCtr++;
        }
    }

    static DecimalFormat format = new DecimalFormat("00");
    private static void writePNG(Monster monster, File dir, int index, int width) throws IOException {
        File pngFile = new File(dir, "monster-png-" + format.format(index + 1) + ".png");
        MonsterRenderer renderer = new MonsterRenderer(monster);
        VectorImage img = renderer.getImage();
        Rectangle2D imgBounds = img.getBounds();
        Dimension scaled = Dimension2D.scaleProportionally(imgBounds.getBounds().getSize(),
                new Dimension(width, width * 10));
        BufferedImage bi = new BufferedImage(scaled.width, scaled.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.transform(TransformUtils.createAffineTransform(imgBounds,
                new Rectangle(1,1,scaled.width - 2,scaled.height - 2)));
        img.paint(g);

        try (OutputStream out = new AdjacentFile(pngFile).createOutputStream()) {
            ImageIO.write(bi, "png", out);
        }

        System.out.println("Wrote: " + pngFile);
    }

    private static void writeSVG(Monster monster, File dir, int index, int width) throws IOException {
        File svgFile = new File(dir, "monster-svg-" + format.format(index + 1) + ".svg");
        MonsterRenderer renderer = new MonsterRenderer(monster);
        VectorImage img = renderer.getImage();
        Rectangle2D imgBounds = img.getBounds();
        Dimension scaled = Dimension2D.scaleProportionally(imgBounds.getBounds().getSize(),
                new Dimension(width, width * 10));

        SVGWriter svgWriter = new SVGWriter();
        try (OutputStream out = new AdjacentFile(svgFile).createOutputStream()) {
            svgWriter.write(img, scaled, out);
        }

        System.out.println("Wrote: " + svgFile);
    }
}
