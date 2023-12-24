package com.pump.monster.ui;

import com.pump.graphics.vector.VectorImage;
import com.pump.inspector.Inspector;
import com.pump.monster.Monster;
import com.pump.monster.render.MonsterRenderer;
import com.pump.plaf.QPanelUI;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

public class ExportPanel extends JPanel {

    JLabel widthLabel = new JLabel("Width:");
    JLabel heightLabel = new JLabel("Height:");
    JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(1000, 10,10_000,10));
    JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(1000, 10,10_000,10));

    Inspector inspector = new Inspector();
    final DocumentModel documentModel;

    private float widthToHeightRatio = 0;

    public ExportPanel(DocumentModel doc) {
        this.documentModel = Objects.requireNonNull(doc);

        inspector.addRow(widthLabel, widthSpinner);
        inspector.addRow(heightLabel, heightSpinner);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1; gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.WEST;
        add(inspector.getPanel(), gbc);

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
        formatBoxUI(ui);
        setUI(ui);

        widthSpinner.getModel().addChangeListener(e -> {
            refreshHeightSpinnerBasedOnWidth();
        });
        heightSpinner.getModel().addChangeListener(e -> {
            if (widthToHeightRatio != 0) {
                float height = (Integer) heightSpinner.getModel().getValue();
                int width = Math.round( widthToHeightRatio * height );
                documentModel.width.setValue(width);
            }
        });

        documentModel.width.addPropertyChangeListener(evt -> widthSpinner.setValue(evt.getNewValue()));
        documentModel.height.addPropertyChangeListener(evt -> heightSpinner.setValue(evt.getNewValue()));

        documentModel.monster.addPropertyChangeListener(evt -> {
            refreshWidthHeightRatio();
        });

        refreshWidthHeightRatio();
        refreshHeightSpinnerBasedOnWidth();
    }

    private void refreshHeightSpinnerBasedOnWidth() {
        if (widthToHeightRatio != 0) {
            float width = (Integer) widthSpinner.getModel().getValue();
            int height = Math.round( width / widthToHeightRatio );
            documentModel.height.setValue(height);
        }
    }

    private void refreshWidthHeightRatio() {
        Monster monster = documentModel.monster.getValue();
        if (monster == null) {
            widthToHeightRatio = 0;
        } else {
            MonsterRenderer renderer = new MonsterRenderer(monster);
            VectorImage vi = renderer.getImage();
            Rectangle2D r = vi.getBounds();
            widthToHeightRatio = (float)( r.getWidth() / r.getHeight() );
            refreshHeightSpinnerBasedOnWidth();
        }
    }

    /**
     * This formats a subtly off-white UI with rounded corners and a (even more subtle) one-pixel gray border.
     * This replicates Apple's box UI. Their documentation describes a box as "a type of view that’s used to create
     * distinct, logical groupings of controls, text fields, and other interface elements."
     */
    private static void formatBoxUI(QPanelUI ui) {
        // TODO: integrate this in the pumpernickel codebase
        ui.setCornerSize(5);
        ui.setStrokeColor1(new Color(0, 0, 0, 30));
        ui.setStrokeColor2(new Color(0, 0, 0, 22));
        ui.setFillColor(new Color(0, 0, 0, 16));
    }
}
