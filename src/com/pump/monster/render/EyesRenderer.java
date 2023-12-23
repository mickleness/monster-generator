package com.pump.monster.render;

import com.pump.graphics.Graphics2DContext;
import com.pump.graphics.vector.Operation;
import com.pump.graphics.vector.VectorImage;
import com.pump.monster.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

public class EyesRenderer {

    class Eye {

        float x, y, radius, antennaWidth;
        Point2D antennaBase;

        public Eye(double x, double y, int radius, Point2D antennaBase, int antennaWidth) {
            this.x = (float) x;
            this.y = (float) y;
            this.radius = radius;
            this.antennaBase = antennaBase;
            this.antennaWidth = antennaWidth;
        }

        public void paintBackground(Graphics2D g) {
            if (antennaBase != null) {
                g.setColor(body.color);
                g.setStroke(new BasicStroke(antennaWidth));
                drawWithShadow(g, new Line2D.Double(antennaBase.getX(), antennaBase.getY(), x, y));

                Ellipse2D antennaTip = getShape(5);

                // use a BodyRenderer to paint the antenna circle in the same style as the monster itself
                // (shaggy, wooly, etc.)
                int randomSeed = monster.hashCode() + (int)(x * 10  + y * 100 + radius);
                AffineTransform tx = AffineTransform.getScaleInstance(2,2);
                BodyRenderer r = new BodyRenderer( tx.createTransformedShape(antennaTip.getBounds2D()).getBounds2D(), monster.hair,
                        monster.bodyColor, BodyShape.CIRCLE, monster.includeTexture, randomSeed);
                for (Operation op : r.getBodyImage().getOperations()) {
                    Graphics2DContext context = op.getContext();
                    context.scale(.5, .5);
                    op.setContext(context);
                }
                r.bodyImage.paint(g);
            }

            Shape eyeSocket = getShape(3);
            g.setPaint(new GradientPaint(0, y - radius, new Color(0, 0, 0, 20), 0, y + radius, new Color(0, 0, 0, 0)));
            g.fill(eyeSocket);
        }

        public void paintForeground(Graphics2D g) {
            Area eyelid;
            int pupilDY = 0;
            if (monster.eyelid == Eyelid.SQUINT) {
                eyelid = new Area(new Ellipse2D.Float(x - 1.5f * radius, y -  radius - radius * 6 / 5, 3 * radius, 2 * radius));
                eyelid.add(new Area(new Ellipse2D.Float(x - 1.5f * radius, y - radius + radius * 8 / 5, 3 * radius, 2 * radius)));
                pupilDY += radius / 4;
            } else if (monster.eyelid == Eyelid.BOTTOM) {
                eyelid = new Area(new Ellipse2D.Float(x - 1.5f * radius, y - radius + radius * 6 / 5, 3 * radius, 2 * radius));
                pupilDY -= radius / 4;
            } else {
                eyelid = new Area();
            }

            Area white = new Area(getShape(0));
            white.subtract(eyelid);
            g.setColor(Color.white);
            g.fill(white);

            float pupilDX = new Random(monster.hashCode()).nextFloat() * 4 - 2;
            float pupilRadius = radius / 2;

            // the white speck in the corner of the pupil
            float pupilGleamRadius = pupilRadius * .3f;
            Area pupilGleam = new Area(new Ellipse2D.Float(x - pupilGleamRadius + pupilDX + pupilRadius * 2/3,
                    y - pupilGleamRadius + pupilDY - pupilRadius * 2/3,
                    pupilGleamRadius * 2, pupilGleamRadius * 2));

            Area pupilOuter = new Area(new Ellipse2D.Float(x - pupilRadius + pupilDX, y - pupilRadius + pupilDY, pupilRadius * 2, pupilRadius * 2));
            pupilOuter.subtract(eyelid);
            g.setColor(monster.bodyColor.darker().darker());
            g.fill(pupilOuter);
            if (body.includeTexture)
                BodyTexture.CONCRETE.paint(g, pupilOuter, 50, body.randomSeed);

            pupilRadius = pupilRadius * .55f;
            Area pupilInner = new Area(new Ellipse2D.Float(x - pupilRadius + pupilDX, y - pupilRadius + pupilDY, pupilRadius * 2, pupilRadius * 2));
            pupilInner.subtract(eyelid);
            g.setColor(Color.black);
            g.fill(pupilInner);

            g.setColor(new Color(255,255,255,155));

            pupilGleam.subtract(eyelid);
            g.fill(pupilGleam);
        }

        private void drawWithShadow(Graphics2D g, Line2D line) {
            // I'd like to replace this with something more generic, but for now this just
            // focuses on lines:
            g = (Graphics2D) g.create();

            g.draw(line);

            float theta = (float) Math.atan2(line.getY2() - line.getY1(), line.getX2() - line.getX1());
            float width = ((BasicStroke)g.getStroke()).getLineWidth();
            float shadowWidth = 1.5f;
            g.setPaint(new GradientPaint( (float) line.getX1(), (float) line.getY1(), new Color(0,0,0,0),
                    (float) (line.getX2() + line.getX1()) / 2f, (float) (line.getY2() + line.getY1()) / 2f, new Color(0,0,0,90)));

            double dx = Math.cos(theta + Math.PI/2) * (width / 2 - shadowWidth / 2 + .01);
            double dy = Math.sin(theta + Math.PI/2) * (width / 2 - shadowWidth / 2 + .01);
            Shape lineEdge = new Line2D.Double(line.getX1() + dx, line.getY1() + dy, line.getX2() + dx, line.getY2() + dy);
            g.fill(new BasicStroke(shadowWidth).createStrokedShape(lineEdge));

            if (body.includeTexture) {
                body.getTexture(body.hair).paint(g, g.getStroke().createStrokedShape(line), body.getTextureOpacity(body.hair), body.randomSeed);
            }
        }

        public Ellipse2D getShape(int padding) {
            float effectiveR = radius + padding;
            return new Ellipse2D.Float(x - effectiveR, y - effectiveR,
                    effectiveR * 2, effectiveR * 2);
        }

        public void scale(double scale, double centerX, double centerY) {
            Point2D.Float p = new Point2D.Float(x, y);

            AffineTransform tx = new AffineTransform();
            tx.translate(centerX, centerY);
            tx.scale(scale, scale);
            tx.translate(-centerX, -centerY);

            tx.transform(p, p);
            x = (float) p.getX();
            y = (float) p.getY();
            radius = (float) (radius * scale);
        }
    }

    BodyRenderer body;
    Monster monster;
    List<Eye> eyes = new LinkedList<>();

    public EyesRenderer(Monster monster, BodyRenderer body) {
        this.body = body;
        this.monster = monster;

        Random r = new Random(monster.hashCode());

        if (monster.eyePlacement == EyePlacement.NORMAL) {
            int dy = -15;
            if (monster.eyeNumber == EyeNumber.ONE) {
                eyes.add(new Eye(50, 50 + dy, 20, null, -1));
            } else if (monster.eyeNumber == EyeNumber.TWO) {
                eyes.add(new Eye(35, 50 + dy, 12, null, -1));
                eyes.add(new Eye(65, 50 + dy, 12, null, -1));
            } else if (monster.eyeNumber == EyeNumber.THREE) {
                eyes.add(new Eye(30, 50 + dy, 12, null, -1));
                eyes.add(new Eye(50, 38 + dy, 10, null, -1));
                eyes.add(new Eye(70, 50 + dy, 12, null, -1));
            }
        } else if (monster.eyePlacement == EyePlacement.ANTENNA) {
            Point2D[] p = body.getBody().getAntennaBases(monster.eyeNumber);
            int k = 0;
            if (monster.hair == Hair.SHAGGY)
                k = 10;
            if (monster.hair == Hair.WOOLY)
                k = 4;

            if (monster.eyeNumber == EyeNumber.ONE) {
                eyes.add(new Eye(p[0].getX(), -27 - k, 15, p[0], 10));
            } else if (monster.eyeNumber == EyeNumber.TWO) {
                eyes.add(new Eye(p[0].getX() - 10, -23 - k, 12, p[0], 8));
                eyes.add(new Eye(p[1].getX() + 10, -23 - k, 12, p[1], 8));
            } else if (monster.eyeNumber == EyeNumber.THREE) {
                eyes.add(new Eye(p[0].getX() - 20, -20 - k, 10, p[0], 6));
                eyes.add(new Eye(p[1].getX(), -30 - k, 10, p[1], 6));
                eyes.add(new Eye(p[2].getX() + 20, -20 - k, 10, p[2], 6));
            }
        } else {
            throw new IllegalStateException();
        }

        // wiggle the eyes a little so most variations aren't identical:

        Collections.shuffle(eyes, r);

        for (int eyeIndex = 0; eyeIndex < eyes.size(); eyeIndex++) {
            Eye eye = eyes.get(eyeIndex);
            Shape shape = eye.getShape(1);
            nudgeEye : while (true) {
                double dx = r.nextInt(8) - 4;
                double dy = r.nextInt(8) - 4;
                AffineTransform tx = AffineTransform.getTranslateInstance(dx, dy);
                for (int otherIndex = 0; otherIndex < eyes.size(); otherIndex++) {
                    if (otherIndex == eyeIndex)
                        continue;
                    Eye otherEye = eyes.get(otherIndex);
                    if (intersects(otherEye.getShape(1), tx.createTransformedShape(shape))) {
                        continue nudgeEye;
                    }
                }

                eye.x += dx;
                eye.y += dy;
                break nudgeEye;
            }
        }

        // maybe scale them a little smaller so they don't bump into the body's edge:
        Shape bodyShape = body.getBody().getShape(false);

        if (monster.eyePlacement == EyePlacement.NORMAL) {

            Rectangle2D eyeBounds = eyes.get(0).getShape(0).getBounds2D();
            for (int a = 1; a < eyes.size(); a++) {
                eyeBounds.add(eyes.get(a).getShape(0).getBounds2D());
            }
            double centerX = eyeBounds.getCenterX();
            double centerY = eyeBounds.getCenterY();

            while (true) {
                boolean needsResize = false;

                for (int a = 0; a < eyes.size(); a++) {
                    if (!bodyShape.contains(eyes.get(a).getShape(2).getBounds2D())) {
                        needsResize = true;
                    }
                }

                if (!needsResize) {
                    break;
                }

                for (Eye eye : eyes) {
                    // squish inward (to avoid horizontal overlap with body left/right)
                    eye.scale(.95, centerX, centerY);

                    // push down (to avoid overlap with top of body)
                    eye.y += 1;
                }
            }
        }
    }

    public void paint(VectorImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Random r = new Random(monster.hashCode());

        for (Eye eye : eyes) {
            eye.radius -= r.nextInt(2);
        }

        for (Eye eye : eyes) {
            eye.paintBackground(g);
        }
        for (Eye eye : eyes) {
            eye.paintForeground(g);
        }
        g.dispose();
    }

    private boolean intersects(Shape shape1, Shape shape2) {
        LineIterator iter1 = new LineIterator(shape1);
        LineIterator iter2 = new LineIterator(shape2);

        for (Line2D line1 = iter1.next(); line1 != null; line1 = iter1.next()) {
            if(shape2.contains(line1.getX1(), line1.getY1()) || shape2.contains(line1.getX2(), line1.getY2()))
                return true;
            for (Line2D line2 = iter2.next(); line2 != null; line2 = iter2.next()) {
                if(shape1.contains(line2.getX1(), line2.getY1()) || shape1.contains(line2.getX2(), line2.getY2()))
                    return true;
            }
        }

        return false;
    }

    static class LineIterator {
        PathIterator pathIter;
        float[] coords = new float[6];
        float moveX, moveY, lastX, lastY;
        Line2D line = new Line2D.Float();

        public LineIterator(Shape shape) {
            pathIter = shape.getPathIterator(null, .01);
        }

        public Line2D next() {
            if (pathIter.isDone())
                return null;

            int k = pathIter.currentSegment(coords);
            if (!pathIter.isDone())
                pathIter.next();

            if (k == PathIterator.SEG_MOVETO) {
                moveX = coords[0];
                moveY = coords[1];
                lastX = moveX;
                lastY = moveY;
                return next();
            }

            if (k == PathIterator.SEG_LINETO) {
                line.setLine(lastX, lastY, coords[0], coords[1]);
                lastX = coords[0];
                lastY = coords[1];
                return line;
            }

            if (k == PathIterator.SEG_CLOSE) {
                line.setLine(lastX, lastY, moveX, moveY);
                return line;
            }

            throw new IllegalStateException("segment = " + k);
        }
    }
}
