package com.pump.monster;

import java.awt.*;
import java.util.Arrays;

public class Monster {
    public static final Color PINK = new Color(0xFF7B9C);
    public static final Color YELLOW = new Color(0xFFC145);
    public static final Color TEAL = new Color(0x06AED5);
    public static final Color ORANGE = new Color(0xFAA317);
    public static final Color GREEN = new Color(0x5FAD41);
    public static final Color PURPLE = new Color(0xBAA5FF);

    public final BodyShape bodyShape;
    public final Color bodyColor;
    public final Hair hair;
    public final EyeNumber eyeNumber;
    public final EyePlacement eyePlacement;
    public final Eyelid eyelid;
    public final MouthShape mouthShape;
    public final MouthFill mouthFill;
    public final Horn horn;
    public final boolean includeTexture;
    public final Legs legs;

    private int hashcode;

    public Monster() {
        this(BodyShape.CIRCLE, Monster.PINK, Hair.NONE, false, EyeNumber.ONE, EyePlacement.NORMAL,
                Eyelid.NONE, MouthShape.GRIN, MouthFill.NONE, Horn.NONE, Legs.NONE);
    }

    public Monster(BodyShape bodyShape, Color bodyColor, Hair hair, boolean includeTexture, EyeNumber eyeNumber, EyePlacement eyePlacement,
                   Eyelid eyelid, MouthShape mouthShape, MouthFill mouthFill, Horn horn, Legs legs) {
        this.bodyShape = hash(bodyShape);
        this.bodyColor = hash(bodyColor);
        this.includeTexture = hash(includeTexture);
        this.hair = hash(hair);
        this.eyeNumber = hash(eyeNumber);
        this.eyePlacement = hash(eyePlacement);
        this.eyelid = hash(eyelid);
        this.mouthShape = hash(mouthShape);
        this.mouthFill = hash(mouthFill);
        this.horn = hash(horn);
        this.legs = hash(legs);
    }

    private <T> T hash(T value) {
        int incomingHash = 0;
        if (value != null) {
            Class z = value.getClass();
            Object[] constants = z.getEnumConstants();
            if (constants != null) {
                incomingHash = Arrays.asList(constants).indexOf(value);
            }
        }

        hashcode = (hashcode << 2) + incomingHash;

        return value;
    }

    @Override
    public int hashCode() {
        return hashcode;
    }
}
