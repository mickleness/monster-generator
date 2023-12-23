package com.pump.monster.render;


import com.pump.awt.HSLColor;
import com.pump.geom.Clipper;
import com.pump.geom.MeasuredShape;
import com.pump.geom.TransformUtils;
import com.pump.graphics.Graphics2DContext;
import com.pump.graphics.vector.*;
import com.pump.monster.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class BodyRenderer {

    public static BodyTexture getTexture(Hair hair) {
        if (hair == Hair.NONE) {
            return BodyTexture.SPECKLED;
        } else if (hair == Hair.WOOLY) {
            return BodyTexture.CONCRETE;
        } else {
            return BodyTexture.FUR_THICK;
        }
    }

    public static int getTextureOpacity(Hair hair) {
        if (hair == Hair.NONE) {
            return 20;
        } else if (hair == Hair.WOOLY) {
            return 20;
        } else {
            return 25;
        }
    }

    /**
     * Reverse the path of a Shape. This should have no immediate effect for the user's perception of the shape.
     * For example, if a circle is drawn clockwise this will render the same circle counter-clockwise.
     * This can be useful when you need to combine shapes and/or you are interested in specific winding rules.
     * <p>
     * TODO: move to ShapeUtils
     * </p>
     *
     * @param shape
     * @return
     */
    public static Path2D reverse(Shape shape) {
        PathIterator pi = shape.getPathIterator(null);
        float[] coords = new float[6];
        float moveX = -1;
        float moveY = -1;
        float lastX = -1;
        float lastY = -1;
        boolean moved = false;
        List<Shape> segments = new LinkedList<>();
        while (!pi.isDone()) {
            int k = pi.currentSegment(coords);
            switch (k) {
                case PathIterator.SEG_MOVETO:
                    if (moved) {
                        // feel free to add support if needed
                        throw new IllegalArgumentException("this method doesn't support shapes with multiple paths");
                    }
                    moved = true;
                    lastX = coords[0];
                    lastY = coords[1];
                    break;
                case PathIterator.SEG_LINETO:
                    segments.add(0, new Line2D.Float(coords[0], coords[1], lastX, lastY));
                    lastX = coords[0];
                    lastY = coords[1];
                    break;
                case PathIterator.SEG_QUADTO:
                    segments.add(0, new QuadCurve2D.Float(coords[2], coords[3], coords[0], coords[1], lastX, lastY));
                    lastX = coords[2];
                    lastY = coords[3];
                    break;
                case PathIterator.SEG_CUBICTO:
                    segments.add(0, new CubicCurve2D.Float(coords[4], coords[5], coords[2], coords[3], coords[0], coords[1], lastX, lastY));
                    lastX = coords[4];
                    lastY = coords[5];
                    break;
                case PathIterator.SEG_CLOSE:
                    segments.add(0, new Line2D.Float(moveX, moveY, lastX, lastY));
                    lastX = moveX;
                    lastY = moveY;
                    break;
                default:
                    throw new IllegalStateException("currentSegment = " + k);
            }

            pi.next();
        }

        Path2D returnValue = new Path2D.Float();
        moved = false;
        for (Shape segment : segments) {
            if (segment instanceof Line2D) {
                Line2D line = (Line2D) segment;
                if (!moved) {
                    moved = true;
                    returnValue.moveTo(line.getX1(), line.getY1());
                }
                returnValue.lineTo(line.getX2(), line.getY2());
            } else if (segment instanceof QuadCurve2D) {
                QuadCurve2D q = (QuadCurve2D) segment;
                if (!moved) {
                    moved = true;
                    returnValue.moveTo(q.getX1(), q.getY1());
                }
                returnValue.quadTo(q.getCtrlX(), q.getCtrlY(), q.getX2(), q.getY2());
            } else if (segment instanceof CubicCurve2D) {
                CubicCurve2D c = (CubicCurve2D) segment;
                if (!moved) {
                    moved = true;
                    returnValue.moveTo(c.getX1(), c.getY1());
                }
                returnValue.curveTo(c.getCtrlX1(), c.getCtrlY1(), c.getCtrlX2(), c.getCtrlY2(), c.getX2(), c.getY2());
            } else {
                throw new IllegalStateException(String.valueOf(segment));
            }
        }

        return returnValue;
    }

    public static class Quadrilateral {
        final Point2D topLeft, topRight, bottomLeft, bottomRight;

        public Quadrilateral(int x, int y, int width, int height) {
            this(new Point2D.Double(x, y), new Point2D.Double(x + width, y),
                    new Point2D.Double(x + width, y + height), new Point2D.Double(x, y + height));
        }

        public Quadrilateral(Point2D topLeft, Point2D topRight, Point2D bottomRight, Point2D bottomLeft) {
            this.topLeft = Objects.requireNonNull(topLeft);
            this.topRight = Objects.requireNonNull(topRight);
            this.bottomRight = Objects.requireNonNull(bottomRight);
            this.bottomLeft = Objects.requireNonNull(bottomLeft);
        }

        public Point2D[] getPoints() {
            return new Point2D[]{topLeft, topRight, bottomRight, bottomLeft};
        }
    }

    public static Point2D tween(Point2D p1, Point2D p2, double fraction) {
        return new Point2D.Double(p1.getX() * ( 1- fraction) + p2.getX() * fraction,
                p1.getY() * (1 - fraction) + p2.getY() * fraction);
    }

    public static Path2D createWoolyEdge(Path2D path, int randomSeed, float minGap) {
        Path2D returnValue = new Path2D.Double();

        MeasuredShape ms = new MeasuredShape(path);

        if (ms.getOriginalDistance() == 0)
            return path;

        float pathLength = ms.getOriginalDistance();

        Random r = new Random(randomSeed);

        Point2D p = new Point2D.Float(ms.getMoveToX(), ms.getMoveToY());

        addDot(returnValue, p, r, 2);

        float d = ms.getOriginalDistance();
        float z = 0;
        while (z < d) {
            float baseWidth = r.nextFloat();
            z = Math.max(z + minGap + 4 * r.nextFloat(), baseWidth);

            if (z + baseWidth / 2 < pathLength) {
                ms.getPoint(z - baseWidth / 2, p);
                addDot(returnValue, p, r, 2);

                float height = 2 + 2 * r.nextFloat();
                double theta = ms.getTangentSlope(z) + (r.nextDouble() - .5) * Math.PI / 8;

                ms.getPoint(z - baseWidth / 4, p);
                p.setLocation(p.getX() + height / 2 * Math.cos(theta + Math.PI / 2),
                        p.getY() + height / 2 * Math.sin(theta + Math.PI / 2));
                addDot(returnValue, p, r, 1.2f);

                ms.getPoint(z, p);
                p.setLocation(p.getX() + height * Math.cos(theta + Math.PI / 2),
                        p.getY() + height * Math.sin(theta + Math.PI / 2));
                addDot(returnValue, p, r, 1f);

                ms.getPoint(z + baseWidth / 4, p);
                p.setLocation(p.getX() + height / 2 * Math.cos(theta + Math.PI / 2),
                        p.getY() + height / 2 * Math.sin(theta + Math.PI / 2));
                addDot(returnValue, p, r, 1.2f);

                ms.getPoint(z + baseWidth / 2, p);
                addDot(returnValue, p, r, 2);
            }
        }

        ms.getPoint(pathLength, p);
        addDot(returnValue, p, r, 2);

        returnValue.append(reverse(path), false);

        return returnValue;
    }

    private static void addDot(Path2D dest, Point2D p, Random r, float size) {
        Path2D path = new Path2D.Double(new Rectangle2D.Float(-size, -size, 2 * size, 2 * size));
        path.transform(AffineTransform.getRotateInstance(Math.PI * r.nextDouble()));
        path.transform(AffineTransform.getTranslateInstance(p.getX(), p.getY()));
        dest.append(path, false);
    }

    static class Body {

        /**
         * The base/plain shape of this body without any texture (hair).
         */
        final Path2D untexturedShape;

        /**
         * The actual path that is rendered -- which may include embellishments to show hair.
         * If Hair.NONE is used then this is the same as {@link #untexturedShape}
         */
        final Path2D texturedShape;

        /**
         * A list of highlights/shadows to render above the textured shape. These will be clipped
         * to the texturedShape when rendered, so you can add elements that overlap it.
         */
        final List<ShapeOperation> textureForegroundAccents = new LinkedList<>();

        /**
         * A list of ShapeOperations to render under the textured shape
         */
        final List<ShapeOperation> background = new LinkedList<>();

        final BodyShape shapeType;
        final Hair hair;
        final Color color;
        final int randomSeed;
        final boolean includeTexture;

        List<Runnable> imagePrepCallbacks = new LinkedList<>();
        transient VectorImage image;
        transient Area shapeArea;
        transient float shaggyWedgeSize = 0;
        private AffineTransform tx;

        /**
         * @param bounds the bounds this Body's plain (undecorated) shape will occupy. The textured
         *               shape may fall a few pixels outside of these bounds.
         */
        public Body(Rectangle2D bounds, Hair hair, Color color, BodyShape shapeType, boolean includeTexture, int randomSeed) {
            this.color = color;
            this.hair = hair;
            this.randomSeed = randomSeed;
            this.shapeType = shapeType;
            this.includeTexture = includeTexture;
            untexturedShape = createUntransformedShape(false);

            tx = TransformUtils.createAffineTransform(untexturedShape.getBounds2D(), bounds);

            if (hair == Hair.NONE) {
                texturedShape = untexturedShape;
            } else {
                if (hair == Hair.SHAGGY) {
                    texturedShape = createUntransformedShape(true);
                } else if (hair == Hair.WOOLY) {
                    texturedShape = createWoolyEdge(untexturedShape, randomSeed, .5f);
                } else {
                    throw new IllegalArgumentException("hair must be NONE, SHAGGY or WOOLY");
                }

                texturedShape.transform(tx);
            }
            untexturedShape.transform(tx);
        }

        private Graphics2DContext createContext(Color foreground) {
            Graphics2DContext context = new Graphics2DContext();
            context.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            context.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            if (foreground != null)
                context.setColor(foreground);
            return context;
        }

        private Quadrilateral getQuadrilateral() {
            Quadrilateral q = null;
            switch (shapeType) {
                case SQUARE:
                    q = new Quadrilateral(0, 0, 100, 100);
                    break;
                case TRAPEZOID:
                    q = new Quadrilateral(new Point2D.Double(20, 0),
                            new Point2D.Double(80, 0),
                            new Point2D.Double(105, 100),
                            new Point2D.Double(-5, 100));
                    break;
                default:
                    throw new IllegalStateException("getQuadrilateral should not be invoked for " + shapeType);
            }

            // randomize the corners a little bit
            Random random = new Random(randomSeed);
            int k = 6;
            for (Point2D p : q.getPoints()) {
                p.setLocation(p.getX() + k * random.nextDouble() - k / 2, p.getY() + k * random.nextDouble() - k / 2);
            }
            return q;
        }

        private Path2D createUntransformedShape(boolean applyShaggyEdge) {
            Shape left = getLeftPath(applyShaggyEdge);
            Shape right = getRightPath(applyShaggyEdge);

            Path2D path = new Path2D.Double();
            path.append(left, false);
            path.append(reverse(right), true);
            path.closePath();
            return path;
        }

        private Shape getLeftPath(boolean applyShaggyEdge) {
            Path2D path = new Path2D.Float();
            switch (shapeType) {
                case CIRCLE:
                    // draw the right side, then flip it:
                    path.moveTo(50, 0);
                    path.curveTo(77.614235, 0.0, 100.0, 22.385763, 100.0, 50.0);
                    path.curveTo(100.0, 77.614235, 77.614235, 100.0, 50.0, 100.0);

                    // flip it:
                    path.transform(AffineTransform.getTranslateInstance(-75, 0));
                    path.transform(AffineTransform.getScaleInstance(-1, 1));
                    path.transform(AffineTransform.getTranslateInstance(25, 0));

                    break;
                case SQUARE:
                case TRAPEZOID:
                    Quadrilateral q = getQuadrilateral();

                    Point2D p = tween(q.topLeft, q.topRight, .5);
                    path.moveTo(p.getX(), p.getY());

                    writeRoundedCorner(path, p, q.topLeft, q.bottomLeft);

                    p = tween(q.bottomLeft, q.bottomRight, .5);
                    writeRoundedCorner(path, q.topLeft, q.bottomLeft, p);

                    path.lineTo(p.getX(), p.getY());
                    break;
                default:
                    throw new IllegalStateException("shape = " + shapeType);
            }

            if (applyShaggyEdge) {
                return applyShaggyEdge(path, 1);
            }

            return path;
        }

        private Path2D getRightPath(boolean applyShaggyEdge) {
            Path2D path = new Path2D.Float();
            switch (shapeType) {
                case CIRCLE:
                    path.moveTo(50, 0);
                    path.curveTo(77.614235, 0.0, 100.0, 22.385763, 100.0, 50.0);
                    path.curveTo(100.0, 77.614235, 77.614235, 100.0, 50.0, 100.0);
                    break;
                case SQUARE:
                case TRAPEZOID:
                    Quadrilateral q = getQuadrilateral();

                    Point2D p = tween(q.topLeft, q.topRight, .5);
                    path.moveTo(p.getX(), p.getY());

                    writeRoundedCorner(path, p, q.topRight, q.bottomRight);

                    p = tween(q.bottomLeft, q.bottomRight, .5);
                    writeRoundedCorner(path, q.topRight, q.bottomRight, p);

                    path.lineTo(p.getX(), p.getY());
                    break;
                default:
                    throw new IllegalStateException("shape = " + shapeType);
            }

            if (applyShaggyEdge) {
                return applyShaggyEdge(path, -1);
            }

            return path;
        }

        private void writeRoundedCorner(Path2D path, Point2D prev, Point2D corner, Point2D next) {
            double theta1 = Math.atan2(corner.getY() - prev.getY(), corner.getX() - prev.getX());
            double theta2 = Math.atan2(next.getY() - corner.getY(), next.getX() - corner.getX());

            path.lineTo(corner.getX() - 20 * Math.cos(theta1),
                    corner.getY() - 20 * Math.sin(theta1));

            path.curveTo(corner.getX() - 10 * Math.cos(theta1), corner.getY() - 10 * Math.sin(theta1),
                    corner.getX() + 10 * Math.cos(theta2), corner.getY() + 10 * Math.sin(theta2),
                    corner.getX() + 20 * Math.cos(theta2), corner.getY() + 20 * Math.sin(theta2));
        }

        private Path2D applyShaggyEdge(Path2D path, double multiplier) {
            Path2D returnValue = new Path2D.Float();

            Random r = new Random(((int) multiplier) * randomSeed);

            if (shaggyWedgeSize == 0)
                shaggyWedgeSize = .3f + .4f * r.nextFloat();

            MeasuredShape ms = new MeasuredShape(path);
            final float maxDistance = ms.getOriginalDistance();
            float z = 0;

            Point2D p0 = new Point2D.Float(ms.getMoveToX(), ms.getMoveToY());
            Point2D p1 = new Point2D.Float();
            Point2D p2 = new Point2D.Float();

            returnValue.moveTo(p0.getX(), p0.getY());

            List<Float> spans = new ArrayList<>();

            // step 1: set up the widths of each spike, and distribute leftover capacity evenly:
            float spanSum = 0;
            while (true) {
                float span = (12 + 8 * r.nextFloat()) * 2 / 3;

                if (spanSum + span < maxDistance) {
                    spans.add(span);
                    spanSum += span;
                } else {
                    float remainder = maxDistance - spanSum;
                    remainder /= spans.size();
                    for (int a = 0; a < spans.size(); a++) {
                        spans.set(a, spans.get(a) + remainder);
                    }
                    break;
                }
            }

            Graphics2DContext shadowContext = createContext(new Color(0,0,0,45));
            Graphics2DContext highlightContext = createContext(null);

            Stroke stroke = new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

            // step 2: write the path

            for (float span : spans) {
                ms.getPoint(z, p0);
                returnValue.lineTo(p0.getX(), p0.getY());

                float spikeTipDistance_far = z + span + r.nextFloat() * span / 2;
                float spikeTipDistance_near = z + span / 2;
                float nearBottom1 = (float) Math.pow(z / maxDistance, 2);
                float spikeTipDistance = spikeTipDistance_far * (1 - nearBottom1) + spikeTipDistance_near * nearBottom1;
                float spike = (float) Math.min(maxDistance - .0000001, spikeTipDistance);
                ms.getPoint( spike, p1);

                ms.getPoint( z + span, p2);

                double theta = ms.getTangentSlope(spike);
                float height = 6 + 4 * r.nextFloat();

                double x_tip = p1.getX() + height * Math.cos(theta + multiplier * Math.PI / 2);
                double y_tip = p1.getY() + height * Math.sin(theta + multiplier * Math.PI / 2);

                double theta_mid = ms.getTangentSlope(z + span / 2);
                double x_spike = p0.getX() + height * Math.cos(theta_mid + multiplier * Math.PI / 2);
                double y_spike = p0.getY() + height * Math.sin(theta_mid + multiplier * Math.PI / 2);

                CubicCurve2D cubic = new CubicCurve2D.Double(
                        p0.getX(), p0.getY(),
                        x_spike * shaggyWedgeSize + p0.getX() * (1 - shaggyWedgeSize), y_spike * shaggyWedgeSize + p0.getY() * (1 - shaggyWedgeSize),
                        x_spike * shaggyWedgeSize + x_tip * (1 - shaggyWedgeSize), y_spike * shaggyWedgeSize + y_tip * (1 - shaggyWedgeSize),
                        x_tip, y_tip);
                Line2D line = new Line2D.Double(x_tip, y_tip, p2.getX(), p2.getY());

                Path2D wedge = new Path2D.Double();
                wedge.moveTo(p0.getX(), p0.getY());
                wedge.append(cubic, true);
                wedge.append(line, true);

                returnValue.append(wedge, true);

                float nearBottom2 = z / maxDistance;
                Runnable callback = new Runnable() {
                    public void run() {
                        if (nearBottom2 < .5) {
                            int alpha = (int)(50 - nearBottom2 / .5f * 50);
                            highlightContext.setColor(new Color(255,255,255, alpha));
                        } else {
                            int alpha = (int)( (nearBottom2 - .5f) / (.5f) * 20);
                            highlightContext.setColor(new Color(0,0,0, alpha));
                        }

                        Shape highlightShape = Clipper.intersect(.01f, wedge, stroke.createStrokedShape(cubic));
                        Area shadowShape = new Area( Clipper.intersect(.01f, wedge, stroke.createStrokedShape(line)) );
                        shadowShape.subtract(new Area(highlightShape));

                        textureForegroundAccents.add(new FillOperation(highlightContext, highlightShape));
                        textureForegroundAccents.add(new FillOperation(shadowContext, shadowShape));
                    }
                };

                // technically we could invoke callback now (no harm in it), but IF this Body object
                // is only being used for its outline shapes: then clipping shapes and focusing
                // on rendering details is a waste. Instead we execute these later in our lazy
                // renderer
                imagePrepCallbacks.add(callback);

                z += span;
            }

            ms.getPoint(maxDistance, p1);
            returnValue.lineTo(p1.getX(), p1.getY());

            return returnValue;
        }

        public synchronized VectorImage getImage() {
            if (image != null)
                return image;

            for (Runnable callback : imagePrepCallbacks) {
                callback.run();
            }
            imagePrepCallbacks.clear();

            for (ShapeOperation accent : textureForegroundAccents) {
                accent.setShape(tx.createTransformedShape(accent.getShape()));
            }

            // now add the other accent / background elements:
            if (hair == Hair.NONE) {
                createNoHairAccents();
            } else if (hair == Hair.WOOLY) {
                Path2D backgroundShape = createWoolyEdge(untexturedShape, randomSeed + 1, 0);
                backgroundShape.transform(AffineTransform.getTranslateInstance(.25, .25));

                Color shadow = HSLColor.transform(color, 0, 1, .9f);
                Graphics2DContext context = createContext(shadow);
                background.add(new FillOperation(context, backgroundShape));
            }

            // clip all accents to the textured shape:
            for (ShapeOperation accent : textureForegroundAccents) {
                Shape clippedShape = Clipper.intersect(.01f, texturedShape, accent.getShape());
                accent.setShape(clippedShape);
            }

            image = new VectorImage();
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            for (Operation op : background) {
                op.paint(g);
            }

            g.setColor(color);
            g.fill(texturedShape);

            for (Operation op : textureForegroundAccents) {
                op.paint(g);
            }

            if (includeTexture) {
                getTexture(hair).paint(g, texturedShape, getTextureOpacity(hair), randomSeed);
            }

            g.dispose();

            return image;
        }

        private void createNoHairAccents() {
            if (shapeArea == null)
                shapeArea = new Area(texturedShape);

            Shape highlight, shadow;
            if (shapeType == BodyShape.CIRCLE) {
                Area shadowArea = new Area(shapeArea);
                Area highlightArea = new Area(shapeArea);

                Random random = new Random(randomSeed);
                double dx = 1 + 2 * random.nextDouble();
                double dy = 1 + 1 * random.nextDouble();
                shadowArea.transform(AffineTransform.getTranslateInstance(dx, dy));
                shadowArea.subtract(shapeArea);
                shadowArea.transform(AffineTransform.getTranslateInstance(-dx, -dy));

                dx = 1 + 1.5 * random.nextDouble();
                dy = 1 + 2 * random.nextDouble();
                highlightArea.transform(AffineTransform.getTranslateInstance(-dx, -dy));
                highlightArea.subtract(shapeArea);
                highlightArea.transform(AffineTransform.getTranslateInstance(dx, dy));

                highlight = highlightArea;
                shadow = shadowArea;
            } else {
                Quadrilateral q = getQuadrilateral();

                // paint lower-right shadow
                Point2D p1 = new Point2D.Double(q.bottomLeft.getX(), q.bottomLeft.getY() - 5);
                Point2D p2 = new Point2D.Double(q.bottomRight.getX() - 7, q.bottomRight.getY() - 5);
                Point2D p3 = new Point2D.Double(q.topRight.getX() - 3, q.topRight.getY());
                Path2D shadowPath = new Path2D.Double();
                shadowPath.moveTo(p1.getX(), p1.getY());
                writeRoundedCorner(shadowPath, p1, p2, p3);
                shadowPath.lineTo(p3.getX(), p3.getY());
                shadowPath.lineTo(q.topRight.getX(), q.topRight.getY());
                shadowPath.lineTo(q.topRight.getX() + 50, q.topRight.getY());
                shadowPath.lineTo(q.bottomRight.getX() + 50, q.bottomRight.getY() + 50);
                shadowPath.lineTo(q.bottomLeft.getX(), q.bottomLeft.getY() + 50);
                shadow = shadowPath;

                // paint upper-left highlight:
                p1 = new Point2D.Double(q.bottomLeft.getX() + 2, q.bottomLeft.getY());
                p2 = new Point2D.Double(q.topLeft.getX() + 3, q.topLeft.getY() + 3);
                p3 = new Point2D.Double(q.topRight.getX(), q.topRight.getY() + 2);

                Path2D highlightPath = new Path2D.Double();
                highlightPath.moveTo(p1.getX(), p1.getY());
                writeRoundedCorner(highlightPath, p1, p2, p3);
                highlightPath.lineTo(p3.getX(), p3.getY());
                highlightPath.lineTo(q.topRight.getX(), q.topRight.getY());
                highlightPath.lineTo(q.topRight.getX(), q.topRight.getY() - 50);
                highlightPath.lineTo(q.topLeft.getX() - 50, q.topLeft.getY() - 50);
                highlightPath.lineTo(q.bottomLeft.getX() - 50, q.bottomLeft.getY() + 50);
                highlight = highlightPath;
            }
            Graphics2DContext shadowContext = createContext(new Color(0,0,0,20));
            textureForegroundAccents.add(new FillOperation(shadowContext, shadow));

            Graphics2DContext highlightContext = createContext(new Color(255,255,255,50));
            textureForegroundAccents.add(new FillOperation(highlightContext, highlight));
        }

        /**
         * @param textured if true then this returns the shape that includes hair, if false this includes the
         *                 simpler (and subtly smaller) base shape.
         */
        public Shape getShape(boolean textured) {
            return textured ? texturedShape : untexturedShape;
        }


        public Point2D[] getAntennaBases(EyeNumber eyeNumber) {
            Point2D[] returnValue = null;
            switch (eyeNumber) {
                case ONE:
                    returnValue = new Point2D[]{new Point(50, 20)};
                    break;
                case TWO:
                    returnValue = new Point2D[]{new Point(40, 25), new Point(60, 25)};
                    break;
                case THREE:
                    switch (shapeType) {
                        case CIRCLE:
                            returnValue = new Point2D[]{new Point(35, 25), new Point(50, 20), new Point(65, 25)};
                            break;
                        case SQUARE:
                            returnValue = new Point2D[]{new Point(22, 25), new Point(50, 25), new Point(78, 25)};
                            break;
                        case TRAPEZOID:
                            returnValue = new Point2D[]{new Point(33, 25), new Point(50, 25), new Point(66, 25)};
                            break;
                    }
                    break;
            }
            if (returnValue == null)
                throw new IllegalStateException("eyeNumber: " + eyeNumber + ", shapeType = " + shapeType);

            tx.transform(returnValue, 0, returnValue, 0, returnValue.length);

            return returnValue;
        }

        public Point2D[] getHornBases() {
            Point2D[] returnValue = null;
            Point2D p1, p2;
            switch (shapeType) {
                case CIRCLE:
                    returnValue = new Point2D[]{new Point(15, 25), new Point(85, 25)};
                    break;
                default:
                    Quadrilateral q2 = getQuadrilateral();

                    p1 = tween(q2.topLeft, q2.bottomLeft, .18);
                    p2 = tween(q2.topRight, q2.bottomRight, .18f);

                    returnValue = new Point2D[] {
                            tween(p1, p2, .08),
                            tween(p2, p1, .08),
                    };
                    break;
            }
            if (returnValue == null)
                throw new IllegalStateException("shapeType = " + shapeType);

            tx.transform(returnValue, 0, returnValue, 0, returnValue.length);

            return returnValue;
        }
    }

    Hair hair;
    Color color;
    BodyShape shapeType;
    int randomSeed;
    Body body;
    VectorImage bodyImage;
    boolean includeTexture;

    public BodyRenderer(Rectangle2D bounds, Hair hair, Color color, BodyShape shapeType, boolean includeTexture, int randomSeed) {
        this.hair = hair;
        this.color = includeTexture ? HSLColor.transform(color, 0, 1.1f, 1.03f) : color;
        this.shapeType = shapeType;
        this.randomSeed = randomSeed;
        this.includeTexture = includeTexture;
        body = createBody(bounds);
    }

    public Body getBody() {
        return body;
    }

    public void paint(VectorImage img) {
        img.getOperations().addAll(getBodyImage().getOperations());
    }

    public synchronized VectorImage getBodyImage() {
        if (bodyImage == null)
            bodyImage = body.getImage();
        return bodyImage;
    }


    public Body createBody(Rectangle2D bounds) {
        return new Body(bounds, hair, color, shapeType, includeTexture, randomSeed);
    }
}