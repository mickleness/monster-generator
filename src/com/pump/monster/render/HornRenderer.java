package com.pump.monster.render;

import com.pump.awt.HSLColor;
import com.pump.geom.Clipper;
import com.pump.geom.MeasuredShape;
import com.pump.graphics.vector.VectorImage;
import com.pump.monster.BodyTexture;
import com.pump.monster.Horn;

import java.awt.*;
import java.awt.geom.*;

public class HornRenderer {
    VectorImage img = new VectorImage();

    public HornRenderer(BodyRenderer bodyRenderer, Horn horn, Color color) {
        if (horn == Horn.NONE || color == null || color.getAlpha() == 0)
            return;

        Point2D[] p = bodyRenderer.getBody().getHornBases();

        double theta = Math.atan2(p[1].getY() - p[0].getY(), p[1].getX() - p[0].getX());

        paintHorn(color, p[0], theta, false, bodyRenderer.randomSeed, bodyRenderer.includeTexture);
        paintHorn(color, p[1], theta, true, bodyRenderer.randomSeed, bodyRenderer.includeTexture);
    }

    private void paintHorn(Color color, Point2D base, double theta, boolean flipHorizontal, int randomSeed, boolean includeTexture) {

        double x0 = 0;
        double y0 = 0;
        double ctrlx1 = -10;
        double ctrly1 = -5;
        double ctrlx2 = -5;
        double ctrly2 = -18;
        double x2 = 0;
        double y2 = -20;

        CubicCurve2D curve = new CubicCurve2D.Double(x0, y0, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);

        Path2D horn = new Path2D.Double();
        horn.moveTo(-5,5);
        horn.curveTo(-10, -5, -10, -15, x2, y2);
        horn.curveTo(-5, -15, -5, -5, 5, -5);
        horn.closePath();

        Path2D hornHalf = new Path2D.Double();
        hornHalf.moveTo(-5,5);
        hornHalf.curveTo(-10, -5, -10, -15, x2, y2);
        hornHalf.curveTo(curve.getCtrlX2(), curve.getCtrlY2(), curve.getCtrlX1(), curve.getCtrlY1(), curve.getX1(), curve.getY1());
        hornHalf.closePath();

        AffineTransform tx = new AffineTransform();
        if (flipHorizontal) {
            tx.rotate(-theta + .5, base.getX(), base.getY());
        } else {
            tx.rotate(theta - .5, base.getX(), base.getY());
        }

        tx.translate(base.getX(), base.getY());
        if (flipHorizontal) {
            tx.scale(-1, 1);
        }
        tx.scale(1.5, 1.5);

        transform(curve, tx);
        horn.transform(tx);
        hornHalf.transform(tx);

        Color[] colors = new Color[] { color, HSLColor.transform(color, 0, 1.1f, 1.2f)};

        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MeasuredShape ms = new MeasuredShape(curve);
        int colorCtr = 0;
        Stroke stroke = new BasicStroke(10);
        for (float d = 0; d < ms.getOriginalDistance(); d += 4) {
            double theta1 = ms.getTangentSlope(d);
            g.setColor(colors[colorCtr]);

            Line2D line = new Line2D.Double(
                    ms.getPoint(d, null).getX() + 10 * Math.cos(theta1 + Math.PI / 2), ms.getPoint(d, null).getY() + 10 * Math.sin(theta1 + Math.PI / 2),
                    ms.getPoint(d, null).getX() + 10 * Math.cos(theta1 - Math.PI / 2), ms.getPoint(d, null).getY() + 10 * Math.sin(theta1 - Math.PI / 2) );
            g.fill( Clipper.intersect(.01f, horn, stroke.createStrokedShape(line)));

            colorCtr = (colorCtr + 1) % colors.length;
        }

        g.setPaint(new GradientPaint( (float) base.getX(), (float) base.getY(), new Color(0,0,0,40),
                (float) curve.getX2(), (float) curve.getY2(), new Color(0,0,0,0)
                ));
        g.fill(horn);
        g.setPaint(new GradientPaint( (float) base.getX(), (float) base.getY(), new Color(0,0,0,20),
                (float) curve.getX2(), (float) curve.getY2(), new Color(0,0,0,0)
        ));
        g.fill(hornHalf);

        if (includeTexture)
            BodyTexture.CONCRETE.paint(g, horn, 60, randomSeed);

        g.dispose();
    }

    private static void transform(CubicCurve2D curve, AffineTransform tx) {
        Point2D p1 = new Point2D.Double(curve.getX1(), curve.getY1());
        Point2D p2 = new Point2D.Double(curve.getCtrlX1(), curve.getCtrlY1());
        Point2D p3 = new Point2D.Double(curve.getCtrlX2(), curve.getCtrlY2());
        Point2D p4 = new Point2D.Double(curve.getX2(), curve.getY2());
        tx.transform(p1, p1);
        tx.transform(p2, p2);
        tx.transform(p3, p3);
        tx.transform(p4, p4);
        curve.setCurve(p1, p2, p3, p4);
    }

    public void paint(VectorImage composite) {
        composite.getOperations().addAll(img.getOperations());
    }
}
