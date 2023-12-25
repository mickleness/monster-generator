package com.pump.monster.ui;

import com.pump.desktop.DefaultAboutRunnable;
import com.pump.desktop.DesktopApplication;
import com.pump.desktop.ExitControl;
import com.pump.graphics.vector.VectorImage;
import com.pump.monster.*;
import com.pump.monster.render.MonsterRenderer;
import com.pump.plaf.QPanelUI;
import com.pump.util.JVM;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class MonsterFrame extends JFrame {

    private static final String VERSION = "1.0";

    public static void main(String[] args) throws IOException {
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        DesktopApplication app = new DesktopApplication("com.pump.MonsterGenerator",
                "Monster Generator", VERSION, "jeremy.wood@mac.com");
        app.setFrameClass(MonsterFrame.class);
        app.setCopyright(2023, "Jeremy Wood");
        app.setURL(new URL("https://github.com/mickleness/monster-generator/"));

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
    MonsterPreview monsterPreview = new MonsterPreview(doc);
    ExportPanel exportPanel = new ExportPanel(doc);
    JButton rerollButton = new JButton("Reroll");

    public MonsterFrame() {
        super("Monster Generator v" + VERSION);
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
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0; gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE; gbc.gridheight = 2;
        gbc.insets = new Insets(10, 10, 10, 10); gbc.anchor = GridBagConstraints.NORTHWEST;
        getContentPane().add(createLabeledPanel(optionsLabel, monsterInspector, rerollButton), gbc);
        gbc.gridy++; gbc.weighty = 1;
        getContentPane().add(Box.createVerticalGlue());

        gbc.gridx++; gbc.gridheight = 1; gbc.gridy = 0; gbc.weighty = 0; gbc.anchor = GridBagConstraints.NORTHWEST;
        getContentPane().add(createLabeledPanel(exportLabel, exportPanel, null), gbc);
        gbc.weightx = 1;
        gbc.gridy++; gbc.weighty = 1; gbc.gridheight = GridBagConstraints.REMAINDER; gbc.fill = GridBagConstraints.BOTH;
        getContentPane().add(monsterPreview, gbc);

        gbc.gridx++;

        monsterInspector.setUI(QPanelUI.createBoxUI());

        updateTaskbarIcon();
        updateAboutMenu();

        rerollButton.addActionListener(e -> monsterInspector.reroll());
    }

    private JPanel createLabeledPanel(JLabel label, JComponent panel, JComponent rightButton) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.weightx = 1; c.weighty = 0;
        c.insets = new Insets(3,3,3,3);
        c.anchor = GridBagConstraints.WEST;
        p.add(label, c);
        c.gridx++;
        c.anchor = GridBagConstraints.EAST;
        if (rightButton != null)
            p.add(rightButton, c);
        c.gridy++; c.gridx = 0; c.gridwidth = GridBagConstraints.REMAINDER; c.weightx = 1; c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        p.add(panel, c);

        label.setLabelFor(panel);
        return p;
    }

    private void updateAboutMenu() {
        if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_ABOUT)) {
            DefaultAboutRunnable aboutRunnable = new DefaultAboutRunnable();
            Desktop.getDesktop().setAboutHandler(e -> aboutRunnable.run());
        }
    }

    private void updateTaskbarIcon() {
        if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
            Taskbar.getTaskbar().setIconImage(getMonsterImage());
        }
    }

    private BufferedImage getMonsterImage() {
        Monster monster = new Monster(BodyShape.TRAPEZOID,
                Monster.ORANGE,
                Hair.SHAGGY,
                false,
                EyeNumber.ONE,
                EyePlacement.NORMAL,
                Eyelid.BOTTOM,
                MouthShape.SMIRK,
                MouthFill.NONE,
                Horn.NONE,
                Legs.NONE );
        MonsterRenderer renderer = new MonsterRenderer(monster);
        VectorImage vectorImage = renderer.getImage();
        BufferedImage bi = vectorImage.toBufferedImage();
        return bi;
    }
}