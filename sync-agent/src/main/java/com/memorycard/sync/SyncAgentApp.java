package com.memorycard.sync;

import com.memorycard.sync.config.AgentConfig;
import com.memorycard.sync.ui.MainFrame;

import javax.swing.*;

public class SyncAgentApp {

    public static final String VERSION = "0.0.11";

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> {
            AgentConfig config = AgentConfig.load();
            MainFrame frame = new MainFrame(config);
            frame.setVisible(true);
        });
    }
}
