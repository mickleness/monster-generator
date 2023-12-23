package com.pump.monster.render;

import com.pump.geom.Clipper;
import com.pump.graphics.vector.VectorImage;
import com.pump.monster.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

public class MouthRenderer {

    Monster monster;
    BodyRenderer bodyRenderer;
    EyesRenderer eyesRenderer;
    VectorImage mouthImage = new VectorImage();

    public MouthRenderer(EyesRenderer eyesRenderer) {
        this.eyesRenderer = eyesRenderer;
        bodyRenderer = eyesRenderer.body;
        monster = eyesRenderer.monster;


        Graphics2D g = mouthImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);


        Stroke strokeSmall = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        Stroke strokeTiny = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        Stroke strokeBig = new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        Path2D mouthShape = new Path2D.Float();
        Path2D teethSeparatorPath = new Path2D.Float();


        if (monster.eyePlacement == EyePlacement.ANTENNA) {
            g.translate(0, -10);
        }

        double widthScaleInCircle = .88;
        if (monster.mouthFill == MouthFill.NONE) {
            switch (monster.mouthShape) {
                case GRIN:
                    mouthShape.moveTo(24, 55);
                    mouthShape.curveTo(24, 85, 76, 85, 76, 55);
                    mouthShape.transform(AffineTransform.getTranslateInstance(0, 5));
                    break;
                case SMIRK:
                    mouthShape.moveTo(40, 80);
                    mouthShape.curveTo(60, 80, 72, 72, 75, 65);
                    widthScaleInCircle = .75;
                    break;
                case FROWN:
                    mouthShape.moveTo(20, 70);
                    mouthShape.curveTo(30, 50, 70, 50, 80, 70);
                    mouthShape.transform(AffineTransform.getTranslateInstance(0, 10));
                    widthScaleInCircle = .65;
                    break;
            }

            if (monster.bodyShape == BodyShape.CIRCLE) {
                scale(mouthShape, widthScaleInCircle, 1);
            }

            g.setStroke(strokeBig);
            g.setColor(new Color(0,0,0,40));
            g.draw(mouthShape);

            g.setStroke(strokeSmall);
            g.setColor(new Color(0,0,0,60));
            g.draw(mouthShape);
        } else {
            switch (monster.mouthShape) {
                case GRIN:
                    mouthShape.moveTo(24, 55);
                    mouthShape.curveTo(24, 85, 76, 85, 76, 55);
                    mouthShape.curveTo(73, 60, 27, 60, 24, 55);
                    mouthShape.transform(AffineTransform.getTranslateInstance(0, 5));

                    teethSeparatorPath.moveTo(20, 70);
                    teethSeparatorPath.curveTo(30, 90, 70, 90, 80, 70);
                    teethSeparatorPath.lineTo(80, 100);
                    teethSeparatorPath.lineTo(20, 100);
                    teethSeparatorPath.transform(AffineTransform.getTranslateInstance(0, -10));
                    break;
                case SMIRK:
                    mouthShape.moveTo(40, 80);
                    mouthShape.curveTo(60, 80, 72, 72, 75, 70);
                    mouthShape.curveTo(75, 90, 40, 100, 40, 80);
                    mouthShape.transform(AffineTransform.getTranslateInstance(0, -10));

                    teethSeparatorPath.moveTo(40, 88);
                    teethSeparatorPath.curveTo(49, 88, 70, 80, 76, 73);
                    teethSeparatorPath.lineTo(76, 100);
                    teethSeparatorPath.lineTo(40, 100);
                    teethSeparatorPath.transform(AffineTransform.getTranslateInstance(0, -10));

                    widthScaleInCircle = .75;
                    break;
                case FROWN:
                    if (monster.bodyShape != BodyShape.CIRCLE) {
                        MirrorWriter w = new MirrorWriter(50);
                        w.start(50, 50, 69, 50);
                        w.curve(80, 60, 80, 70, 80, 80);
                        w.curve(68 + 5, 75 + 2, 68, 75, 68 - 5, 75 - 2);
                        w.end(57, 69, 50, 69);
                        mouthShape = w.toPath();
                    } else {
                        MirrorWriter w = new MirrorWriter(50);
                        w.start(50, 50, 69, 50);
                        w.curve(80, 60, 80, 70, 80, 80);
                        w.curve(68 + 5, 76 + 1, 68, 76, 68 - 5, 76 - 1);
                        w.end(57, 73, 50, 73);
                        mouthShape = w.toPath();
                    }

                    teethSeparatorPath.moveTo(15, 70);
                    teethSeparatorPath.curveTo(40, 50, 60, 50, 85, 70);
                    teethSeparatorPath.lineTo(85, 100);
                    teethSeparatorPath.lineTo(15, 100);
                    teethSeparatorPath.transform(AffineTransform.getTranslateInstance(0, 16));

                    mouthShape.transform(AffineTransform.getTranslateInstance(0, 10));
                    widthScaleInCircle = .65;
                    break;
            }

            if (monster.bodyShape == BodyShape.CIRCLE) {
                scale(mouthShape, widthScaleInCircle, 1, teethSeparatorPath);
            }

            g.setStroke(strokeBig);
            g.setColor(new Color(0,0,0,50));
            g.draw(mouthShape);

            Rectangle2D mouthBounds = mouthShape.getBounds2D();
            if (monster.mouthFill == MouthFill.BLACK) {
                g.setColor(Color.black);
                g.fill(mouthShape);
            } else if (monster.mouthFill == MouthFill.ALL_TEETH) {
                g.setColor(Color.white);
                g.fill(mouthShape);
            } else {
                g.setColor(Color.black);
                g.fill(mouthShape);

                Shape leftTooth = new Rectangle2D.Double(mouthBounds.getX() + mouthBounds.getWidth() * 1 / 4, 0, mouthBounds.getWidth() * 1/8, 100);
                Shape rightTooth = new Rectangle2D.Double(mouthBounds.getX() + mouthBounds.getWidth() * 4 / 9, 0, mouthBounds.getWidth() * 1 / 8, 100);
                leftTooth = Clipper.intersect(.01f, mouthShape, teethSeparatorPath, leftTooth);
                rightTooth = Clipper.intersect(.01f, mouthShape, teethSeparatorPath, rightTooth);

                Rectangle2D leftToothBounds = leftTooth.getBounds2D();
                Rectangle2D rightToothBounds = rightTooth.getBounds2D();

                double leftDX = 0;
                double rightDX = 0;
                double leftDY = 0;
                double rightDY = 0;

                if (monster.mouthShape == MouthShape.FROWN) {
                    leftDY += mouthBounds.getHeight() / 8 + rightToothBounds.getHeight() * 1 / 5;
                    rightDY += mouthBounds.getHeight() / 8;
                    leftDX -= mouthBounds.getWidth() / 12;
                    rightDX -= mouthBounds.getWidth() / 12;
                } else {
                    rightDY += rightToothBounds.getHeight() * 1 / 5;
                }
                leftToothBounds.setFrame(leftToothBounds.getX() + leftDX, leftToothBounds.getY() + leftDY,
                        leftToothBounds.getWidth(), leftToothBounds.getHeight());
                rightToothBounds.setFrame(rightToothBounds.getX() + rightDX, rightToothBounds.getY() + rightDY,
                        rightToothBounds.getWidth(), rightToothBounds.getHeight());

                leftTooth = new RoundRectangle2D.Double(leftToothBounds.getX(), leftToothBounds.getY(), leftToothBounds.getWidth(), leftToothBounds.getHeight(), 30, 3);
                rightTooth = new RoundRectangle2D.Double(rightToothBounds.getX(), rightToothBounds.getY(), rightToothBounds.getWidth(), rightToothBounds.getHeight(), 30, 3);

                g.setColor(Color.white);
                g.fill( Clipper.intersect(.01f, mouthShape, leftTooth));
                g.fill( Clipper.intersect(.01f, mouthShape, rightTooth));
            }

            if (monster.mouthFill == MouthFill.ALL_TEETH) {
                g.setColor(new Color(0,0,0));
                g.fill( Clipper.intersect(.01f, mouthShape, strokeTiny.createStrokedShape(teethSeparatorPath)));

                double x2 = mouthBounds.getX() + mouthBounds.getWidth() * 2 / 4;
                double x3 = mouthBounds.getX() + mouthBounds.getWidth() * 3 / 4;

                for (int a = 1; a <= 3; a++) {
                    double x = mouthBounds.getX() + mouthBounds.getWidth() * a / 4;
                    Line2D line = new Line2D.Double(x, 0, x, 100);
                    g.fill( Clipper.intersect(.01f, mouthShape, strokeTiny.createStrokedShape(line)));
                }
            }
        }

        g.dispose();
    }

    static void scale(Path2D shape, double scaleX, int scaleY, Path2D... auxShapes) {
        Rectangle2D r = shape.getBounds2D();
        double cx = r.getCenterX();
        double cy = r.getCenterY();
        AffineTransform tx = new AffineTransform();
        tx.translate(cx, cy);
        tx.scale(scaleX, scaleY);
        tx.translate(-cx, -cy);

        shape.transform(tx);
        for (Path2D auxShape : auxShapes) {
            auxShape.transform(tx);
        }
    }

    public void paint(VectorImage img) {
        Graphics2D g = img.createGraphics();
        mouthImage.paint(g);
        g.dispose();
    }

    static class MirrorWriter {
        LinkedList<Point2D> points = new LinkedList<>();

        int mirrorXvalue;

        public MirrorWriter(int mirrorXvalue) {
            this.mirrorXvalue = mirrorXvalue;
        }

        public void start(int x1, int y1, int ctrlX, int ctrlY) {
            points.add(new Point2D.Float(x1, y1));
            points.add(new Point2D.Float(ctrlX, ctrlY));
        }

        public void curve(int ctrlX1, int ctrlY1, int x, int y, int ctrlX2, int ctrlY2) {
            points.add(new Point2D.Float(ctrlX1, ctrlY1));
            points.add(new Point2D.Float(x, y));
            points.add(new Point2D.Float(ctrlX2, ctrlY2));
        }

        public void end(int ctrlX1, int ctrlY1, int x, int y) {
            points.add(new Point2D.Float(ctrlX1, ctrlY1));
            points.add(new Point2D.Float(x, y));
        }

        public Path2D toPath() {
            Path2D path = new Path2D.Float();
            path.moveTo(points.get(0).getX(), points.get(0).getY());
            ListIterator<Point2D> iter = points.listIterator(1);
            while (iter.hasNext()) {
                Point2D ctrl1 = iter.next();
                Point2D ctrl2 = iter.next();
                Point2D p = iter.next();
                path.curveTo(ctrl1.getX(), ctrl1.getY(), ctrl2.getX(), ctrl2.getY(), p.getX(), p.getY());
            }

            iter.previous();

            while(iter.hasPrevious()) {
                Point2D ctrl1 = iter.previous();
                Point2D ctrl2 = iter.previous();
                Point2D p = iter.previous();
                path.curveTo(mirrorXvalue + mirrorXvalue - ctrl1.getX(), ctrl1.getY(),
                        mirrorXvalue + mirrorXvalue - ctrl2.getX(), ctrl2.getY(),
                        mirrorXvalue + mirrorXvalue - p.getX(), p.getY());
            }

            return path;
        }
    }
}
