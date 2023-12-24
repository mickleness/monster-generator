package com.pump.monster.ui;

import com.pump.inspector.Inspector;
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
import java.util.function.Function;

public class MonsterInspector extends JPanel {

    final int ROWS = 6;

    /**
     * How many possible combinations these controls can create
     */
    private static long COMBINATIONS = 1;

    static final Collection<MonsterEnumProperty<?>> ALL_PROPERTIES = new LinkedHashSet<>();

    static class MonsterEnumProperty<T> extends EnumProperty<T> {
        Function<Monster, T> retrieveValueFromMonster;

        public MonsterEnumProperty(String propertyName, T[] values, T value, Function<Monster, T> retrieveValueFromMonster) {
            super(propertyName, values, value);
            this.retrieveValueFromMonster = Objects.requireNonNull(retrieveValueFromMonster);
            ALL_PROPERTIES.add(this);
            COMBINATIONS *= values.length;
        }

        public T getValue(Monster monster) {
            return retrieveValueFromMonster.apply(monster);
        }
    }

    static final MonsterEnumProperty<BodyShape> bodyShape = new MonsterEnumProperty<>("Body Shape", BodyShape.values(), BodyShape.CIRCLE, monster -> monster.bodyShape);
    static final MonsterEnumProperty<Color> color = new MonsterEnumProperty<>("Color", new Color[]{Monster.TEAL, Monster.GREEN, Monster.ORANGE, Monster.PINK, Monster.YELLOW, Monster.PURPLE},
            Monster.TEAL, monster -> monster.bodyColor);
    static final MonsterEnumProperty<Hair> hair = new MonsterEnumProperty<>("Hair", Hair.values(), Hair.NONE, monster -> monster.hair);
    static final MonsterEnumProperty<Eyelid> eyelid = new MonsterEnumProperty<>("Eyelid", Eyelid.values(), Eyelid.NONE, monster -> monster.eyelid);
    static final MonsterEnumProperty<EyeNumber> eyeNumber = new MonsterEnumProperty<>("Eye Number", EyeNumber.values(), EyeNumber.ONE, monster -> monster.eyeNumber);
    static final MonsterEnumProperty<EyePlacement> eyePlacement = new MonsterEnumProperty<>("Eye Placement", EyePlacement.values(), EyePlacement.NORMAL, monster -> monster.eyePlacement);
    static final MonsterEnumProperty<MouthShape> mouthShape = new MonsterEnumProperty<>("Mouth Shape", MouthShape.values(), MouthShape.SMIRK, monster -> monster.mouthShape);
    static final MonsterEnumProperty<MouthFill> mouthFill = new MonsterEnumProperty<>("Mouth Fill", MouthFill.values(), MouthFill.BLACK, monster -> monster.mouthFill);
    static final MonsterEnumProperty<Horn> horn = new MonsterEnumProperty<>("Horn", Horn.values(), Horn.NONE, monster -> monster.horn);
    static final MonsterEnumProperty<Boolean> includeTexture = new MonsterEnumProperty<>("Include Texture", new Boolean[]{Boolean.FALSE, Boolean.TRUE}, Boolean.TRUE, monster -> monster.includeTexture);
    static final MonsterEnumProperty<Legs> legs = new MonsterEnumProperty<>("Legs", Legs.values(), Legs.SHORT, monster -> monster.legs);

    /**
     * Each element is a column of controls
     */
    java.util.List<Inspector> inspectors = new LinkedList<>();
    Map<MonsterEnumProperty, JComboBox> propertyToControlMap = new HashMap<>();

    PropertyChangeListener updateMonsterPCL = new PropertyChangeListener() {
        boolean dirty = false;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!dirty)
                    return;
                dirty = false;

                Monster m = new Monster(bodyShape.getValue(), color.getValue(), hair.getValue(), includeTexture.getValue(),
                        eyeNumber.getValue(), eyePlacement.getValue(), eyelid.getValue(), mouthShape.getValue(), mouthFill.getValue(),
                        horn.getValue(), legs.getValue());
                monster.setValue(m);
            }
        };

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            dirty = true;
            SwingUtilities.invokeLater(runnable);
        }
    };

    JButton rerollButton = new JButton("Reroll");
    final Property<Monster> monster;

    static Random random = new Random();

    public MonsterInspector(Property<Monster> monster) {
        super(new GridBagLayout());
        this.monster = Objects.requireNonNull(monster);

        // TODO: save properties to preferences across sessions

        for (MonsterEnumProperty p : ALL_PROPERTIES)
            initializeProperty(p);

        System.out.println(NumberFormat.getInstance().format(COMBINATIONS) + " possible combinations");

        addToInspector(null, rerollButton);
        rerollButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reroll();
            }
        });

        refreshUIOptionsFromMonster();
        monster.addPropertyChangeListener(evt -> refreshUIOptionsFromMonster());

        reroll();
    }


    private void refreshUIOptionsFromMonster() {
        for (Map.Entry<MonsterEnumProperty, JComboBox> entry : propertyToControlMap.entrySet()) {
            MonsterEnumProperty p = entry.getKey();
            Object currentValue = p.getValue(monster.getValue());
            int i = Arrays.asList(p.getValues()).indexOf(currentValue);
            entry.getValue().setSelectedIndex(i);
        }
    }

    private <T> void initializeProperty(MonsterEnumProperty<T> property) {
        String name = property.getName();
        JComboBox<T> comboBox = new JComboBox<>(property.getValues());
        propertyToControlMap.put(property, comboBox);

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
                if (str.equalsIgnoreCase("true"))
                    return "Yes";
                if (str.equalsIgnoreCase("false"))
                    return "No";
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

    private int currentRow = 0;
    private int currentColumn = 0;
    private void addToInspector(JComponent label, JComponent component) {
        int inspectorIndex = currentColumn;

        currentRow++;
        if (currentRow == ROWS) {
            currentRow = 0;
            currentColumn++;
        }

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
        Monster m = new Monster(
                bodyShape.getValues()[random.nextInt(bodyShape.getValues().length)],
                color.getValues()[random.nextInt(color.getValues().length)],
                hair.getValues()[random.nextInt(hair.getValues().length)],
                includeTexture.getValues()[random.nextInt(includeTexture.getValues().length)],
                eyeNumber.getValues()[random.nextInt(eyeNumber.getValues().length)],
                eyePlacement.getValues()[random.nextInt(eyePlacement.getValues().length)],
                eyelid.getValues()[random.nextInt(eyelid.getValues().length)],
                mouthShape.getValues()[random.nextInt(mouthShape.getValues().length)],
                mouthFill.getValues()[random.nextInt(mouthFill.getValues().length)],
                horn.getValues()[random.nextInt(horn.getValues().length)],
                legs.getValues()[random.nextInt(legs.getValues().length)]
        );
        monster.setValue(m);
    }
}
