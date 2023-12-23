package com.pump.monster.render;

import com.pump.awt.HSLColor;
import com.pump.geom.Clipper;
import com.pump.graphics.vector.VectorImage;
import com.pump.monster.BodyShape;
import com.pump.monster.Hair;
import com.pump.monster.Legs;

import java.awt.*;
import java.awt.geom.*;
import java.util.Random;

public class LegsRenderer {

    private static final double DEFAULT_LEG_WIDTH = 8;

    private final BodyRenderer bodyRenderer;
    private final Legs legs;
    private final Shape bodyShape;
    private final Rectangle2D bodyBounds;

    private final Path2D backLegs = new Path2D.Double();
    private final Path2D middleLegs = new Path2D.Double();
    private final Path2D frontLegs = new Path2D.Double();
    private final Path2D strokedPath = new Path2D.Double();

    private final AffineTransform flipHorizTransform;

    public LegsRenderer(BodyRenderer bodyRenderer, Legs legs) {
        this.bodyRenderer = bodyRenderer;
        this.legs = legs;

        if (legs == Legs.NONE) {
            bodyShape = null;
            bodyBounds = null;
            flipHorizTransform = null;
            return;
        }

        bodyShape = bodyRenderer.getBody().getShape(false);
        bodyBounds = bodyShape.getBounds2D();

        flipHorizTransform = new AffineTransform();
        flipHorizTransform.translate(bodyBounds.getCenterX(), bodyBounds.getCenterY());
        flipHorizTransform.scale(-1, 1);
        flipHorizTransform.translate(-bodyBounds.getCenterX(), -bodyBounds.getCenterY());

        try {
            for (boolean leftLeg : new boolean[]{true, false}) {
                AffineTransform tx = leftLeg ? new AffineTransform() : flipHorizTransform;

                if (legs == Legs.BUG) {
                    int dx = 0;

                    if (bodyRenderer.hair == Hair.WOOLY) {
                        dx = -3;
                    } else if (bodyRenderer.hair == Hair.SHAGGY) {
                        dx = -7;
                    }

                    dx -= 3;

                    backLegs.append(tx.createTransformedShape(createLeg(tx, leftLeg, 1, 0, .82, .75,
                            -11 + dx, 4, - 13 + dx, 12)), false);
                    middleLegs.append(tx.createTransformedShape(createLeg(tx, leftLeg, 1, 0, 1, 1,
                            -8 + dx, 6, - 12 + dx, 15)), false);

                    if (bodyRenderer.shapeType == BodyShape.CIRCLE) {
                        dx += 9;
                    }

                    double widthPos = bodyRenderer.shapeType == BodyShape.CIRCLE ? .25 : .3;
                    frontLegs.append(tx.createTransformedShape(createLeg(tx, leftLeg, .1, widthPos, 1, 1.2,
                            -14 + dx, 12, - 16 + dx, 24)), false);
                } else {

                    // handle both Legs.SHORT and Legs.LONG

                    double x1 = bodyBounds.getX() + bodyBounds.getWidth() * 2 / 7;
                    double y1 = bodyBounds.getY() + bodyBounds.getHeight();
                    Point2D point = new Point2D.Double(x1, y1);
                    tx.transform(point, point);
                    // move leg anchor so it is fully underneath the body
                    nudgeInside(point, 0, -1, DEFAULT_LEG_WIDTH);
                    y1 = point.getY();

                    Path2D p = new Path2D.Double();
                    // paint the left leg, starting at the top-left of the thigh:
                    p.moveTo(x1, y1);

                    // make sure we use same Random (& seed) for opposing legs
                    Random random = new Random(bodyRenderer.randomSeed);

                    double legHeight;
                    if (legs == Legs.SHORT) {
                        legHeight = 11 + 3 * random.nextDouble();
                        if (bodyRenderer.hair == Hair.SHAGGY)
                            legHeight += 4;
                    } else {
                        legHeight = 42 + 7 * random.nextDouble();
                    }
                    double y2 = y1 + legHeight;
                    double bow = -1 + 7 * random.nextDouble();
                    p.curveTo(x1 - bow, y1 + 2.0 / 5.0 * legHeight, x1 - bow, y2 - 2.0 / 5.0 * legHeight, x1, y2);

                    defineFeet(p, strokedPath, x1, y2, DEFAULT_LEG_WIDTH, tx);

                    // curve back to top-right of leg
                    p.curveTo(x1 - bow + DEFAULT_LEG_WIDTH, y2 - 2.0 / 5.0 * legHeight, x1 - bow + DEFAULT_LEG_WIDTH, y1 + 2.0 / 5.0 * DEFAULT_LEG_WIDTH, x1 + DEFAULT_LEG_WIDTH, y1);

                    frontLegs.append(tx.createTransformedShape(p), false);
                }
            }
        } catch(NoninvertibleTransformException e) {
            // this shouldn't happen
            throw new RuntimeException(e);
        }
    }

    /**
     * @param fractionHeightPos the bodyBounds height multiplier to position the y-value of the leg. For ex,
     *                          0 = the top fo the bodyBounds, 1 = the bottom of the bodyBounds
     * @return
     */
    private Shape createLeg(AffineTransform tx, boolean leftLeg, double nudgeXIncr, double fractionWidthPos, double fractionHeightPos, double legWidthMultiplier, int dx1, int dy1, int dx2, int dy2) throws NoninvertibleTransformException {
        Point2D point;
        if (leftLeg) {
            point = new Point2D.Double(bodyBounds.getMinX() + fractionWidthPos * bodyBounds.getWidth(), bodyBounds.getY() + bodyBounds.getHeight() * fractionHeightPos);
            nudgeInside(point, nudgeXIncr, -1, DEFAULT_LEG_WIDTH / 2);
        } else {
            point = new Point2D.Double(bodyBounds.getMaxX() - fractionWidthPos * bodyBounds.getWidth(), bodyBounds.getY() + bodyBounds.getHeight() * fractionHeightPos);
            nudgeInside(point, -nudgeXIncr, -1, DEFAULT_LEG_WIDTH / 2);
        }
        tx.inverseTransform(point, point);

        double x1 = point.getX();
        double y1 = point.getY();

        Path2D returnValue = new Path2D.Double();

        Path2D foot = new Path2D.Double();

        Point2D p2 = defineFeet(foot, strokedPath, x1 + dx2, y1 + dy2, DEFAULT_LEG_WIDTH * legWidthMultiplier, tx);
        Point2D p3 = getMidPoint(p2.getX(), p2.getY(), x1 + dx2, y1 + dy2);

        float strokeWidth = (float) p2.distance(x1 + dx2, y1 + dy2);
        BasicStroke stroke = new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        Shape leg = stroke.createStrokedShape(createPath(point,
                new Point2D.Double(point.getX() + dx1, point.getY() + dy1),
                p3));
        returnValue.append(merge(foot, leg),false);

        return returnValue;
    }

    private static Shape merge(Shape... shapes) {
        Area returnValue = new Area();
        for (int a = 0; a < shapes.length; a++) {
            returnValue.add(new Area(shapes[a]));
        }
        return returnValue;
    }

    private static Shape createPath(Point2D... points) {
        Path2D p = new Path2D.Double();
        for (int a = 0; a < points.length; a++) {
            if (a == 0) {
                p.moveTo(points[a].getX(), points[a].getY());
            } else {
                p.lineTo(points[a].getX(), points[a].getY());
            }
        }
        return p;
    }

    private static Point2D getMidPoint(double x1, double y1, double x2, double y2) {
        return new Point2D.Double( (x1 + x2) / 2.0, (y1 + y2) / 2.0);
    }

    public void paintUnderBody(VectorImage img) {
        paint(img, false);
    }

    public void paintAboveBody(VectorImage img) {
        paint(img, true);
    }

    private void paint(VectorImage img, boolean foreground) {
        if (legs == Legs.NONE)
            return;

        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (!foreground) {
            fillShape(g, backLegs, .96f);
            fillShape(g, middleLegs, .98f);
        } else {
            fillShape(g, frontLegs, 1);
        }

        g.setColor(new Color(0,0,0,40));
        Shape strokedShape = new BasicStroke(.5f).createStrokedShape(strokedPath);
        g.fill(Clipper.intersect(.01f, strokedShape, backLegs));
        g.fill(Clipper.intersect(.01f, strokedShape, middleLegs));
        g.fill(Clipper.intersect(.01f, strokedShape, frontLegs));

        g.dispose();
    }

    private void fillShape(Graphics2D g, Shape path, float luminanceMultiplier) {
        g.setColor(HSLColor.transform(bodyRenderer.color, 0, 1, luminanceMultiplier));
        g.fill(path);

        Rectangle2D pathBounds = path.getBounds2D();

        // paint shadow:
        Path2D p = new Path2D.Double();
        p.append(path, false);
        p.transform(AffineTransform.getTranslateInstance(0, -1.5));
        Area shadowArea = new Area(path);
        shadowArea.subtract(new Area(p));
        g.setPaint(new GradientPaint(0, (float) (pathBounds.getMinY() + pathBounds.getHeight() * .25f), new Color(0,0,0,0),
                0, (float) pathBounds.getMaxY(), new Color(0,0,0,40)));
        g.fill(shadowArea);

        // paint highlight:
        p.reset();
        p.append(path, false);
        p.transform(AffineTransform.getTranslateInstance(1,1));
        Area highlightArea = new Area(path);
        highlightArea.subtract(new Area(p));
        highlightArea.subtract(shadowArea);
        g.setPaint(new GradientPaint(0, (float) (pathBounds.getMinY() + pathBounds.getHeight() * .1f), new Color(255,255,255,0),
                0, (float) pathBounds.getMaxY(), new Color(255,255,255,20)));
        g.fill(highlightArea);

        if (bodyRenderer.includeTexture) {
            BodyRenderer.getTexture(bodyRenderer.hair).paint(g, path, 30, bodyRenderer.randomSeed);
        }
    }

    private void nudgeInside(Point2D point, double xIncr, double yIncr, double squareLength) {
        // for un-textured bodies, we need to clear an extra few pixels to cover the body's shadow
        int dy = 4;
        while(!bodyShape.contains(point.getX() - squareLength/2, point.getY() - squareLength / 2 + dy, squareLength, squareLength)) {
            point.setLocation(point.getX() + xIncr, point.getY() + yIncr);
        }
    }

    /**
     *
     * @param filledPath
     * @param strokedPath
     * @param x the x-coordinate of the top-left corner of the foot
     * @param y the y-coordinate of the top-left corner of the foot
     * @param legWidth the width of the foot
     */
    private Point2D defineFeet(Path2D filledPath, Path2D strokedPath, double x, double y, double legWidth, AffineTransform tx) {

        double scale = legWidth / DEFAULT_LEG_WIDTH;

        Path2D p = new Path2D.Double();

        p.moveTo(0,0);
        p.curveTo(- 11, 3, - 14,  1, - 13, 10);
        p.lineTo(- 9, 10);
        p.lineTo(- 9, 12);
        p.lineTo(- 5, 12);
        p.lineTo(- 5, 14);
        p.lineTo(- 1, 14);
        p.curveTo(legWidth + 3, 10, legWidth + 3,  10,  legWidth, 0);

        p.transform(AffineTransform.getScaleInstance(scale, scale));
        p.transform(AffineTransform.getTranslateInstance(x,y));
        filledPath.append(p, true);

        // the toe shadows
        p.reset();
        p.moveTo(- 9, 10);
        p.curveTo(- 9,  6, - 9, 6, - 3, 4.5f);

        p.moveTo(- 5, 12);
        p.curveTo(- 5, 8, - 4, 8, 1,  6.5f);

        p.transform(AffineTransform.getScaleInstance(scale, scale));
        p.transform(AffineTransform.getTranslateInstance(x,y));
        strokedPath.append( tx.createTransformedShape(p), false);

        return new Point2D.Double(legWidth * scale + x, 0 + y);
    }
}
