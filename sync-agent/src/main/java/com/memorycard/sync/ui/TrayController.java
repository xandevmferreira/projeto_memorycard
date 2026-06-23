package com.memorycard.sync.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public class TrayController {

    private final MainFrame frame;
    private TrayIcon trayIcon;
    private boolean trayAvailable;

    public TrayController(MainFrame frame) {
        this.frame = frame;
        this.trayAvailable = SystemTray.isSupported();
        if (trayAvailable) {
            installTray();
            installWindowBehavior();
        } else {
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        }
    }

    public boolean isTrayAvailable() {
        return trayAvailable;
    }

    private void installTray() {
        Image image = AppIcon.create(32);
        PopupMenu menu = new PopupMenu();

        MenuItem showItem = new MenuItem("Abrir MemoryCard Sync");
        showItem.addActionListener(e -> showFrame());

        MenuItem startItem = new MenuItem("Iniciar sync");
        startItem.addActionListener(e -> frame.startSyncFromTray());

        MenuItem stopItem = new MenuItem("Parar sync");
        stopItem.addActionListener(e -> frame.stopSyncFromTray());

        MenuItem quitItem = new MenuItem("Sair");
        quitItem.addActionListener(e -> exitApp());

        menu.add(showItem);
        menu.addSeparator();
        menu.add(startItem);
        menu.add(stopItem);
        menu.addSeparator();
        menu.add(quitItem);

        trayIcon = new TrayIcon(image, "MemoryCard Sync", menu);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> showFrame());

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            trayAvailable = false;
        }
    }

    private void installWindowBehavior() {
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                hideToTray();
            }
        });
        frame.addWindowStateListener(e -> {
            if ((e.getNewState() & Frame.ICONIFIED) != 0) {
                hideToTray();
            }
        });
    }

    public void hideToTray() {
        if (!trayAvailable) {
            frame.dispose();
            System.exit(0);
            return;
        }
        frame.setVisible(false);
        if (trayIcon != null) {
            trayIcon.displayMessage(
                    "MemoryCard Sync",
                    "Rodando em segundo plano. Clique no ícone para abrir.",
                    TrayIcon.MessageType.INFO);
        }
    }

    public void showFrame() {
        frame.setVisible(true);
        frame.setExtendedState(Frame.NORMAL);
        frame.toFront();
        frame.requestFocus();
    }

    public void updateTooltip(String status) {
        if (trayIcon != null) {
            trayIcon.setToolTip("MemoryCard Sync — " + status);
        }
    }

    public void exitApp() {
        frame.prepareShutdown();
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        frame.dispose();
        System.exit(0);
    }
}
