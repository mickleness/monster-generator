package com.pump.monster.ui;

import com.pump.desktop.ExitControl;
import com.pump.desktop.temp.TempFileManager;
import com.pump.plaf.QPanelUI;
import com.pump.util.JVM;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class MonsterFrame extends JFrame {
    public static void main(String[] args) throws IOException {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        TempFileManager.initialize("pump-monster-gen-v1.0");

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
    JMenu editMenu = new JMenu("Edit");
    JMenuItem rerollMenuItem = new JMenuItem("Reroll");
    JMenuItem savePNGMenuItem = new JMenuItem("Save PNG As\u2026");
    JMenuItem saveSVGMenuItem = new JMenuItem("Save SVG As\u2026");
    JMenuItem copyMenuItem = new JMenuItem("Copy Image");

    // TODO: add undo / redo ?

    DocumentModel doc = new DocumentModel();
    MonsterInspector monsterInspector = new MonsterInspector(doc.monster);
    MonsterPreview monsterPreview = new MonsterPreview(doc.monster);
    ExportPanel exportPanel = new ExportPanel(doc);

    public MonsterFrame() {
        super("Monster Generator");
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        editMenu.add(copyMenuItem);

        fileMenu.add(savePNGMenuItem);
        fileMenu.add(saveSVGMenuItem);
        fileMenu.add(rerollMenuItem);
        copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        savePNGMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        saveSVGMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() + InputEvent.SHIFT_DOWN_MASK));
        rerollMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        rerollMenuItem.addActionListener(e -> monsterInspector.reroll());
        savePNGMenuItem.addActionListener(e -> exportPanel.savePNG());
        saveSVGMenuItem.addActionListener(e -> exportPanel.saveSVG());
        copyMenuItem.addActionListener(e -> exportPanel.copyImage());
        setJMenuBar(menuBar);

        if (!JVM.isMac)
            fileMenu.add(new ExitControl(true).getExitMenuItem());

        getContentPane().setLayout(new GridBagLayout());

        JLabel optionsLabel = new JLabel("Options");
        JLabel exportLabel = new JLabel("Export");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(20,20,20,20);
        getContentPane().add(monsterPreview, gbc);

        gbc.gridx++;
        gbc.weightx = 0; gbc.gridheight = 1; gbc.weighty = 0;
        gbc.insets = new Insets(20,20,3,20);
        getContentPane().add(optionsLabel, gbc);
        gbc.insets = new Insets(3,20,3,20);
        gbc.gridy++;
        getContentPane().add(monsterInspector, gbc);
        gbc.insets = new Insets(20,20,3,20);
        gbc.gridy++;
        getContentPane().add(exportLabel, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(3,20,20,20);
        getContentPane().add(exportPanel, gbc);
        gbc.gridy++; gbc.weighty = 1;
        gbc.insets = new Insets(0,0,0,0);
        getContentPane().add(Box.createVerticalBox(), gbc);

        optionsLabel.setLabelFor(monsterInspector);

        gbc.gridx++;

        monsterInspector.setUI(QPanelUI.createBoxUI());
    }
}