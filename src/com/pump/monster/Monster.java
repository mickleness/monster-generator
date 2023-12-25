package com.pump.monster;

import java.awt.*;
import java.util.Arrays;
import java.util.Random;

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

    /**
     * Create a random Monster
     */
    public Monster() {
        this(new Random());
    }

    /**
     * Create a random Monster
     */
    public Monster(Random random) {
        this(BodyShape.values()[random.nextInt(BodyShape.values().length)],
                new Color[] {PINK, YELLOW, TEAL, ORANGE, GREEN, PURPLE}[random.nextInt(6)],
                Hair.values()[random.nextInt(Hair.values().length)],
                random.nextBoolean(),
                EyeNumber.values()[random.nextInt(EyeNumber.values().length)],
                EyePlacement.values()[random.nextInt(EyePlacement.values().length)],
                Eyelid.values()[random.nextInt(Eyelid.values().length)],
                MouthShape.values()[random.nextInt(MouthShape.values().length)],
                MouthFill.values()[random.nextInt(MouthFill.values().length)],
                Horn.values()[random.nextInt(Horn.values().length)],
                Legs.values()[random.nextInt(Legs.values().length)] );
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
