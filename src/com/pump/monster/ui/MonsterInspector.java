package com.pump.monster.ui;

import com.pump.inspector.Inspector;
import com.pump.inspector.InspectorRow;
import com.pump.monster.*;
import com.pump.util.EnumProperty;
import com.pump.util.Property;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.*;

public class MonsterInspector extends JPanel {

    final int ROWS = 6;

    static boolean outputCombinations = false;

    Collection<EnumProperty<?>> properties = new HashSet<>();

    EnumProperty<BodyShape> bodyShape = new EnumProperty<BodyShape>("Body Shape", BodyShape.values(), BodyShape.CIRCLE);
    EnumProperty<Color> color = new EnumProperty<Color>("Color", new Color[] {Monster.TEAL, Monster.GREEN, Monster.ORANGE, Monster.PINK, Monster.YELLOW, Monster.PURPLE}, Monster.TEAL);
    EnumProperty<Hair> hair = new EnumProperty<Hair>("Hair", Hair.values(), Hair.NONE);
    EnumProperty<Eyelid> eyelid = new EnumProperty<Eyelid>("Eyelid", Eyelid.values(), Eyelid.NONE);
    EnumProperty<EyeNumber> eyeNumber = new EnumProperty<EyeNumber>("Eye Number", EyeNumber.values(), EyeNumber.ONE);
    EnumProperty<EyePlacement> eyePlacement = new EnumProperty<EyePlacement>("Eye Placement", EyePlacement.values(), EyePlacement.NORMAL);
    EnumProperty<MouthShape> mouthShape = new EnumProperty<MouthShape>("Mouth Shape", MouthShape.values(), MouthShape.SMIRK);
    EnumProperty<MouthFill> mouthFill = new EnumProperty<MouthFill>("Mouth Fill", MouthFill.values(), MouthFill.BLACK);
    EnumProperty<Horn> horn = new EnumProperty<Horn>("Horn", Horn.values(), Horn.NONE);
    EnumProperty<Boolean> includeTexture = new EnumProperty<Boolean>("Include Texture", new Boolean[] {Boolean.FALSE, Boolean.TRUE}, Boolean.TRUE);
    EnumProperty<Legs> legs = new EnumProperty<Legs>("Legs", Legs.values(), Legs.SHORT);

    java.util.List<Inspector> inspectors = new LinkedList<>();

    PropertyChangeListener updateMonsterPCL = new PropertyChangeListener() {

        boolean dirty = false;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!dirty)
                    return;
                dirty = false;

                Monster monster = new Monster(bodyShape.getValue(), color.getValue(), hair.getValue(), includeTexture.getValue(),
                        eyeNumber.getValue(), eyePlacement.getValue(), eyelid.getValue(), mouthShape.getValue(), mouthFill.getValue(),
                        horn.getValue(), legs.getValue());
                monsterProperty.setValue(monster);
            }
        };

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            dirty = true;
            SwingUtilities.invokeLater(runnable);
        }
    };

    JButton rerollButton = new JButton("Reroll");
    final Property<Monster> monsterProperty;

    public MonsterInspector(Property<Monster> monsterProperty) {
        super(new GridBagLayout());
        this.monsterProperty = Objects.requireNonNull(monsterProperty);

        // TODO: save properties to preferences across sessions

        addProperty(bodyShape);
        addProperty(color);
        addProperty(hair);
        addProperty(eyeNumber);
        addProperty(eyePlacement);
        addProperty(eyelid);
        addProperty(mouthShape);
        addProperty(mouthFill);
        addProperty(horn);
        addProperty(includeTexture);
        addProperty(legs);

        if (!outputCombinations) {
            outputCombinations = true;
            System.out.println(NumberFormat.getInstance().format(combinations) + " possible combinations");
        }

        addToInspector(null, rerollButton);
        rerollButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reroll();
            }
        });

        reroll();

        // TODO: when monsterProperty changes, update UI.
        // Currently this isn't necessary (or testable) because this inspector is the only way to change the monster,
        // but once we add an undo/redo feature it will be necessary.
    }

    /**
     * How many possible combinations these controls can create
     */
    private long combinations = 1;

    static Random random = new Random();

    private <T> void addProperty(EnumProperty<T> property) {
        properties.add(property);
        combinations *= property.getValues().length;
        String name = property.getName();
        JComboBox<T> comboBox = new JComboBox<>(property.getValues());

        class Listener implements ActionListener, PropertyChangeListener {

            @Override
            public void actionPerformed(ActionEvent e) {
                property.removePropertyChangeListener(this);
                try {
                    property.setValue((T) comboBox.getSelectedItem());
                } finally {
                    property.addPropertyChangeListener(this);
                }
            }

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                comboBox.removeActionListener(this);
                try {
                    comboBox.setSelectedItem(property.getValue());
                } finally {
                    comboBox.addActionListener(this);
                }
            }
        }

        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                value = toString(value);
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }

            private String toString(Object value) {
                String str = String.valueOf(value);
                StringBuilder returnValue = new StringBuilder();
                boolean newWord = true;
                for (char ch : str.toCharArray()) {
                    if (ch == '_') {
                        newWord = true;
                        returnValue.append(' ');
                    } else if (newWord) {
                        returnValue.append( Character.toUpperCase(ch) );
                        newWord = false;
                    } else {
                        returnValue.append(Character.toLowerCase(ch));
                    }
                }
                return returnValue.toString();
            }
        });

        Listener l = new Listener();
        comboBox.addActionListener(l);
        property.addPropertyChangeListener(l);

        JLabel label = new JLabel(name + ":");
        property.addPropertyChangeListener(updateMonsterPCL);

        addToInspector(label, comboBox);
    }

    private void addToInspector(JComponent label, JComponent component) {
        int inspectorIndex = (properties.size()-1) / ROWS;
        if (inspectorIndex == inspectors.size()) {
            Inspector i = new Inspector();
            inspectors.add(i);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = inspectorIndex; gbc.gridy = 0; gbc.weightx = 1; gbc.weighty = 1;
            gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.NORTHWEST;
            add(i.getPanel(), gbc);
            i.getPanel().setOpaque(false);
        }

//        if (label != null)
//            label.putClientProperty("JComponent.sizeVariant", "small");
//        if (component != null)
//            component.putClientProperty("JComponent.sizeVariant", "small");

        Inspector inspector = inspectors.get(inspectorIndex);
        if (label != null) {
            inspector.addRow(label, component);
        } else {
            // center in row:
            JPanel p = new JPanel(new GridBagLayout());
            p.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1; gbc.weighty = 1; gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.NONE;
            p.add(component, gbc);
            inspector.addRow(p, true);
        }
    }

    public void reroll() {
        for (EnumProperty property : properties) {
            Object initialValue = property.getValues()[random.nextInt(property.getValues().length)];
            property.setValue(initialValue);
        }
    }
}
