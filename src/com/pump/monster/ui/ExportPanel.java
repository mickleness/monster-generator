package com.pump.monster.ui;

import com.pump.awt.dnd.FileLabel;
import com.pump.desktop.temp.TempFileManager;
import com.pump.geom.TransformUtils;
import com.pump.graphics.vector.*;
import com.pump.inspector.Inspector;
import com.pump.io.IOUtils;
import com.pump.monster.Monster;
import com.pump.monster.render.MonsterRenderer;
import com.pump.plaf.QPanelUI;
import com.pump.swing.FileDialogUtils;
import com.pump.swing.ImageTransferable;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExportPanel extends JPanel {

    /**
     * This replaces the PNG/SVG files
     */
    private static ExecutorService fileExecutor = Executors.newSingleThreadExecutor();

    JLabel widthLabel = new JLabel("Width:");
    JLabel heightLabel = new JLabel("Height:");
    JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(1000, 10,10_000,10));
    JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(1000, 10,10_000,10));

    Inspector inspector = new Inspector();
    final DocumentModel documentModel;

    FileLabel pngLabel = new FileLabel(DnDConstants.ACTION_COPY);
    FileLabel svgLabel = new FileLabel(DnDConstants.ACTION_COPY);
    JLabel pngSizeLabel = new JLabel();
    JLabel svgSizeLabel = new JLabel();

    private boolean filesDirty = false;
    private VectorImage vectorImage;
    private File pngFile = new File(TempFileManager.get().getDirectory(), "monster.png");
    private File svgFile = new File(TempFileManager.get().getDirectory(), "monster.svg");

    private float widthToHeightRatio = 0;

    public ExportPanel(DocumentModel doc) {
        this.documentModel = Objects.requireNonNull(doc);

        inspector.addRow(widthLabel, widthSpinner);
        inspector.addRow(heightLabel, heightSpinner);
        inspector.addRow(createFlowLayout(pngLabel, pngSizeLabel), false);
        inspector.addRow(createFlowLayout(svgLabel, svgSizeLabel), false);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.WEST;
        add(inspector.getPanel(), gbc);

        gbc.gridx++; gbc.weightx = 1;
        add(Box.createHorizontalGlue(), gbc);

        Color c = pngSizeLabel.getForeground();
        c = new Color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() * 80 / 255);
        pngSizeLabel.setForeground(c);
        svgSizeLabel.setForeground(c);

        QPanelUI ui = new QPanelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                super.paint(g, c);
                paintWidthHeightConnector( (Graphics2D) g);
            }

            private void paintWidthHeightConnector(Graphics2D g) {
                Rectangle widthSpinnerRect = new Rectangle(new Point(0,0), widthSpinner.getSize());
                Rectangle heightSpinnerRect = new Rectangle(new Point(0,0), heightSpinner.getSize());
                widthSpinnerRect = SwingUtilities.convertRectangle(widthSpinner, widthSpinnerRect, ExportPanel.this);
                heightSpinnerRect = SwingUtilities.convertRectangle(heightSpinner, heightSpinnerRect, ExportPanel.this);
                if (widthSpinnerRect != null && heightSpinnerRect != null) {
                    int x1 = widthSpinnerRect.x + widthSpinnerRect.width;
                    int y1 = widthSpinnerRect.y + widthSpinnerRect.height / 2;
                    int x2 = heightSpinnerRect.x + heightSpinnerRect.width;
                    int y2 = heightSpinnerRect.y + heightSpinnerRect.height / 2;
                    g.setColor(new Color(0,0,0,70));
                    Path2D outerPath = new Path2D.Float();
                    outerPath.moveTo(x1 + 1, y1-1);
                    outerPath.lineTo(x1 + 6, y1-1);
                    outerPath.lineTo(x2 + 6, y2+2);
                    outerPath.lineTo(x2 + 1, y2+2);
                    Path2D innerPath = new Path2D.Float();
                    innerPath.moveTo(x1 + 1, y1+2);
                    innerPath.lineTo(x1 + 3, y1+2);
                    innerPath.lineTo(x2 + 3, y2-1);
                    innerPath.lineTo(x2 + 1, y2-1);
                    g.draw(outerPath);
                    g.draw(innerPath);
                }
            }
        };
        inspector.getPanel().setOpaque(false);
        QPanelUI.formatBoxUI(ui);
        setUI(ui);

        widthSpinner.getModel().addChangeListener(e -> {
            refreshHeightSpinnerBasedOnWidth();
        });
        heightSpinner.getModel().addChangeListener(e -> {
            refreshWidthSpinnerBasedOnHeight();
        });

        documentModel.width.addPropertyChangeListener(evt -> widthSpinner.setValue(evt.getNewValue()));
        documentModel.height.addPropertyChangeListener(evt -> heightSpinner.setValue(evt.getNewValue()));

        documentModel.monster.addPropertyChangeListener(evt -> {
            refreshAfterMonsterUpdate();
        });

        refreshAfterMonsterUpdate();
        refreshHeightSpinnerBasedOnWidth();

        // make the files exist so their FileIcon looks normal
        try {
            pngFile.createNewFile();
            svgFile.createNewFile();
        } catch(IOException e) {
            e.printStackTrace();
        }

        pngLabel.setFile(pngFile);
        svgLabel.setFile(svgFile);

        pngLabel.setToolTipText("Click and drag this file to export your monster as a PNG image.");
        svgLabel.setToolTipText("Click and drag this file to export your monster as an SVG image.");
    }

    private JPanel createFlowLayout(JComponent... components) {
        JPanel panel = new JPanel(new FlowLayout());
        panel.setOpaque(false);
        for(JComponent component : components) {
            panel.add(component);
        }
        return panel;
    }

    private void refreshHeightSpinnerBasedOnWidth() {
        if (widthToHeightRatio != 0) {
            float width = (Integer) widthSpinner.getModel().getValue();
            int height = Math.round( width / widthToHeightRatio );
            documentModel.height.setValue(height);
            queueRefreshFiles();
        }
    }

    private void refreshWidthSpinnerBasedOnHeight() {
        if (widthToHeightRatio != 0) {
            float height = (Integer) heightSpinner.getModel().getValue();
            int width = Math.round( widthToHeightRatio * height );
            documentModel.width.setValue(width);
            queueRefreshFiles();
        }
    }

    /**
     * Show a file dialog to save the current image as a PNG.
     */
    public void savePNG() {
        Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
        File destFile = FileDialogUtils.showSaveDialog(frame, "Save PNG", "png");
        if (destFile == null)
            return;

        try {
            IOUtils.copy(pngFile, destFile);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Show a file dialog to save the current image as an SVG.
     */
    public void saveSVG() {
        Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
        File destFile = FileDialogUtils.showSaveDialog(frame, "Save SVG", "svg");
        if (destFile == null)
            return;

        try {
            IOUtils.copy(svgFile, destFile);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void copyImage() {
        BufferedImage bi = createImage();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageTransferable(bi), null);
    }

    class RefreshFilesRunnable implements Runnable {
        public void run() {
            if (!filesDirty || vectorImage == null)
                return;
            filesDirty = false;

            try {
                ImageIO.write(createImage(), "png", pngFile);
                SwingUtilities.invokeLater(() -> {
                    String sizeStr = IOUtils.formatFileSize(pngFile);
                    pngLabel.setVisible(true);
                    pngSizeLabel.setText(sizeStr);
                });
            } catch(Exception e) {
                e.printStackTrace();
            }

            try {
                if (writeSVG()) {
                    SwingUtilities.invokeLater(() -> {
                        String sizeStr = IOUtils.formatFileSize(svgFile);
                        svgLabel.setVisible(true);
                        svgSizeLabel.setText(sizeStr);
                    });
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean writeSVG() throws IOException {
        if (documentModel.monster.getValue() == null ||
                documentModel.width.getValue() == null ||
                documentModel.height.getValue() == null)
            return false;

        Monster monster = documentModel.monster.getValue();
        if (monster.includeTexture)
            monster = new Monster(monster.bodyShape, monster.bodyColor, monster.hair, false, monster.eyeNumber, monster.eyePlacement, monster.eyelid, monster.mouthShape, monster.mouthFill, monster.horn, monster.legs);

        int width = documentModel.width.getValue();
        int height = documentModel.height.getValue();

        MonsterRenderer r = new MonsterRenderer(monster);
        VectorImage img = r.getImage();

        SVGWriter svgWriter = new SVGWriter();
        try (FileOutputStream fileOut = new FileOutputStream(svgFile)) {
            svgWriter.write(img, new Dimension(width, height), fileOut);
        }

        return true;
    }

    private synchronized BufferedImage createImage() {
        int width = (Integer) widthSpinner.getModel().getValue();
        int height = (Integer) heightSpinner.getModel().getValue();
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.transform(TransformUtils.createAffineTransform(vectorImage.getBounds(),
                new Rectangle(1, 1, width - 2,height - 2)));
        vectorImage.paint(g);
        g.dispose();
        return bi;
    }

    private synchronized void refreshAfterMonsterUpdate() {
        Monster monster = documentModel.monster.getValue();
        if (monster == null) {
            widthToHeightRatio = 0;
            pngLabel.setVisible(false);
            svgLabel.setVisible(false);
            pngSizeLabel.setText("");
            svgSizeLabel.setText("");
        } else {
            MonsterRenderer renderer = new MonsterRenderer(monster);
            vectorImage = renderer.getImage();
            Rectangle2D r = vectorImage.getBounds();
            widthToHeightRatio = (float)( r.getWidth() / r.getHeight() );
            refreshHeightSpinnerBasedOnWidth();

            queueRefreshFiles();
        }
    }

    private void queueRefreshFiles() {
        filesDirty = true;
        fileExecutor.execute(new RefreshFilesRunnable());
    }
}
