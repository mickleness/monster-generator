package com.pump.monster.render;

import com.pump.graphics.vector.VectorImage;
import com.pump.monster.Monster;

import java.awt.*;

public class MonsterRenderer {
    private final Monster monster;

    public MonsterRenderer(Monster monster) {
        this.monster = monster;
    }

    public VectorImage getImage() {
        BodyRenderer bodyRenderer = new BodyRenderer(new Rectangle(0,0,100,100), monster.hair, monster.bodyColor, monster.bodyShape, monster.includeTexture, monster.hashCode());
        EyesRenderer eyesRenderer = new EyesRenderer(monster, bodyRenderer);
        MouthRenderer mouthRenderer = new MouthRenderer(eyesRenderer);
        HornRenderer hornRenderer = new HornRenderer(bodyRenderer, monster.horn, new Color(0xCF5C36));
        LegsRenderer legsRenderer = new LegsRenderer(bodyRenderer, monster.legs);

        VectorImage composite = new VectorImage();
        Graphics2D g = composite.createGraphics();
        hornRenderer.paint(composite);
        legsRenderer.paintUnderBody(composite);
        bodyRenderer.paint(composite);
        legsRenderer.paintAboveBody(composite);
        eyesRenderer.paint(composite);
        mouthRenderer.paint(composite);

        g.dispose();
        return composite;
    }
}
