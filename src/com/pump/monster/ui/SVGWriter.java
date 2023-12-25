package com.pump.monster.ui;

import com.pump.geom.ShapeStringUtils;
import com.pump.geom.ShapeUtils;
import com.pump.geom.TransformUtils;
import com.pump.graphics.vector.*;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * This converts VectorImages to SVGs. It is only intended to support the bare minimum for
 * this monster-generator project. We can add to it as needed, but we don't want to overengineer it.
 */
class SVGWriter {
    public void write(VectorImage vectorImage, Dimension exportSize, OutputStream out) {
        try (PrintStream ps = new PrintStream(out)) {
            ps.println("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>");
            ps.println("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
            ps.println("<svg version=\"1.1\" id=\"buildings_1\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"");
            ps.println("\tx=\"0px\" y=\"0px\" width=\"" + exportSize.width + "px\" height=\"" + exportSize.height + "px\" viewBox=\"0 0 " + exportSize.width + " " + exportSize.height + "\"");
            ps.println("\t xml:space=\"preserve\">");

            AffineTransform masterTX = TransformUtils.createAffineTransform(vectorImage.getBounds(),
                    new Rectangle(0,0,exportSize.width,exportSize.height));

            java.util.List<Operation> operations = new LinkedList<>();
            for (Operation op : vectorImage.getOperations()) {
                if (op instanceof DrawOperation drawOperation)
                    op = drawOperation.toFillOperation();
                if (op instanceof ShapeOperation shapeOp && ShapeUtils.isEmpty(shapeOp.getShape()))
                    continue;
                operations.add(op);
            }

            ps.println("\t<defs>");

            // I think (?) gradients have to be defined at the top of the SVG, and they can't be
            // defined inline.
            Map<Paint, String> paintDefinitions = new IdentityHashMap<>();
            for (Operation op : operations) {
                if (op instanceof FillOperation fillOperation) {
                    Paint paint = fillOperation.getContext().getPaint();
                    paint = applyAlphaComposite(paint, fillOperation.getContext().getComposite());
                    if (paint instanceof GradientPaint gradientPaint) {
                        String id = "paint-id-" + paintDefinitions.size();
                        paintDefinitions.put(paint, id);

                        Point2D p1 = gradientPaint.getPoint1();
                        Point2D p2 = gradientPaint.getPoint1();

                        fillOperation.getContext().getTransform().transform(p1, p1);
                        masterTX.transform(p1, p1);
                        fillOperation.getContext().getTransform().transform(p2, p2);
                        masterTX.transform(p2, p2);

                        ps.println("\t\t<linearGradient id=\"" + id  +"\" " +
                                "x1=\"" + p1.getX() + "\" y1=\"" + p1.getY() + "\" x2=\"" + p2.getX() + "\" y2=\"" + p2.getY() + "\">");
                        String color1str = toRGBHexString(gradientPaint.getColor1());
                        String color2str = toRGBHexString(gradientPaint.getColor2());
                        float alpha1 = gradientPaint.getColor1().getAlpha();
                        float alpha2 = gradientPaint.getColor2().getAlpha();
                        String opacity1str = alpha1 == 255f ? "1" : Float.toString( alpha1 / 255f );
                        String opacity2str = alpha2 == 255f ? "1" : Float.toString( alpha2 / 255f );
                        ps.println("\t\t\toffset=\"0%\" style=\"stop-color:" + color1str + ";stop-opacity:" + opacity1str +";\"/>");
                        ps.println("\t\t\toffset=\"100%\" style=\"stop-color:" + color2str + ";stop-opacity:" + opacity2str +";\"/>");
                        ps.println("\t\t</linearGradient>");
                    }
                }
            }

            ps.println("\t</defs>");
            ps.println("\t<g>");

            for (Operation op : operations) {
                if (op instanceof FillOperation fillOperation) {
                    Shape shape = fillOperation.getShape();
                    shape = fillOperation.getContext().getTransform().createTransformedShape(shape);
                    shape = masterTX.createTransformedShape(shape);

                    Paint paint = fillOperation.getContext().getPaint();
                    paint = applyAlphaComposite(paint, fillOperation.getContext().getComposite());

                    String pathStr = ShapeStringUtils.toString(shape).toUpperCase();
                    String fillruleStr = shape.getPathIterator(null).getWindingRule() == PathIterator.WIND_EVEN_ODD ? "evenodd" : "nonzero";
                    String fillStr;
                    String paintID = paintDefinitions.get(paint);
                    String fillOpacityStr = "1";
                    if (paintID != null) {
                        fillStr = "url(#" + paintID + ")";
                    } else if (paint instanceof Color color) {
                        fillStr = toRGBHexString(color);
                        if (color.getAlpha() < 255)
                            fillOpacityStr = Float.toString( ((float)(color.getAlpha())) / 255f );
                    } else {
                        throw new UnsupportedOperationException(paint.toString());
                    }
                    ps.println("\t\t<path style=\"fill-rule:" + fillruleStr + ";fill:" + fillStr + ";fill-opacity:" + fillOpacityStr + ";\" d=\"" + pathStr + "\"/>");
                } else {
                    throw new UnsupportedOperationException(op.getClass().getName() + " " + op);
                }
            }

            ps.println("\t</g>");
            ps.println("</svg>");
        }
    }

    private Paint applyAlphaComposite(Paint paint, Composite composite) {
        if (composite instanceof AlphaComposite alphaComposite) {
            if (alphaComposite.getRule() == AlphaComposite.SRC_OVER) {
                if (alphaComposite.getAlpha() == 1)
                    return paint;

                if (paint instanceof Color color) {
                    int alpha = Math.max(0, Math.min(255, Math.round(alphaComposite.getAlpha() * color.getAlpha())));
                    return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                } else if (paint instanceof GradientPaint gradientPaint) {
                    return new GradientPaint(gradientPaint.getPoint1(),
                            (Color) applyAlphaComposite(gradientPaint.getColor1(), composite),
                            gradientPaint.getPoint2(),
                            (Color) applyAlphaComposite(gradientPaint.getColor2(), composite),
                            gradientPaint.isCyclic());
                } else {
                    throw new RuntimeException("Unsupported paint: " + paint);
                }
            } else {
                throw new RuntimeException("Unsupported AlphaComposite rule: " + alphaComposite.getRule());
            }
        } else {
            throw new RuntimeException("Unsupported composite: " + composite);
        }
    }

    /**
     * @return an RGB hex code like "#fe10da" or "#001fc8"
     */
    private String toRGBHexString(Color color) {
        String str = Integer.toUnsignedString(color.getRGB(), 16);
        while (str.length() < 8)
            str = "0" + str;
        // put alpha at end
        return "#" + str.substring(2, 8);
    }
}
