package com.pump.monster.ui;

import com.pump.monster.Monster;
import com.pump.util.Property;

public class DocumentModel {
    public final Property<Monster> monster = new Property<>("monster");
    public final Property<Integer> width = new Property<>("width");
    public final Property<Integer> height = new Property<>("height");
}
