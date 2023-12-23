package com.pump.monster.ui;

import com.pump.awt.Dimension2D;
import com.pump.geom.TransformUtils;
import com.pump.graphics.vector.VectorImage;
import com.pump.monster.Monster;
import com.pump.monster.render.MonsterRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class MonsterPreview extends JComponent {

    private static final String PROPERTY_MONSTER = "monster";

    public MonsterPreview() {
        setPreferredSize(new Dimension(200, 200));
        setBorder(new EmptyBorder(4,4,4,4));

        addPropertyChangeListener(PROPERTY_MONSTER, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Monster monster = getMonster();

        if (monster == null)
            return;

        Graphics2D g2 = (Graphics2D) g.create();

        MonsterRenderer renderer = new MonsterRenderer(monster);

        VectorImage vi = renderer.getImage();
        Rectangle2D r = vi.getBounds();
        Dimension compSize = getSize();
        Insets i = getInsets();
        compSize.width -= i.left + i.right;
        compSize.height -= i.top + i.bottom;

        Dimension scaledSize = Dimension2D.scaleProportionally(r.getBounds().getSize(), compSize);
        AffineTransform tx = TransformUtils.createAffineTransform(r, new Rectangle(
                i.left + compSize.width / 2 - scaledSize.width / 2,
                i.top + compSize.height / 2 - scaledSize.height / 2,
                scaledSize.width, scaledSize.height));
        g2.transform(tx);
        vi.paint(g2);
    }

    public void setMonster(Monster monster) {
        putClientProperty(PROPERTY_MONSTER, monster);
    }

    public Monster getMonster() {
        return (Monster) getClientProperty(PROPERTY_MONSTER);
    }
}
