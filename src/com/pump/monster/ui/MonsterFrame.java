package com.pump.monster.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class MonsterFrame extends JFrame {
    public static void main(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MonsterFrame m = new MonsterFrame();
                m.pack();
                m.setVisible(true);
            }
        });
    }

    JMenuBar menuBar = new JMenuBar();
    JMenu fileMenu = new JMenu("File");
    JMenuItem rerollMenuItem = new JMenuItem("Reroll");

    MonsterInspector inspector = new MonsterInspector();

    public MonsterFrame() {
        super("Monster Generator");
        menuBar.add(fileMenu);
        fileMenu.add(rerollMenuItem);
        rerollMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        rerollMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reroll();
            }
        });
        setJMenuBar(menuBar);

        getContentPane().setLayout(new GridBagLayout());

        JPanel topLeft = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        getContentPane().add(topLeft, gbc);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 1;
        topLeft.add(inspector, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        topLeft.add(inspector.createPreview(), gbc);
    }

    private void reroll() {
        inspector.reroll();
    }
}
