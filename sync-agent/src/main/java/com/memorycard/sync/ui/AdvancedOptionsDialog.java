package com.memorycard.sync.ui;

import com.memorycard.sync.api.GameSummary;
import com.memorycard.sync.config.AgentConfig;
import com.memorycard.sync.watcher.SaveFolderDetector;
import com.memorycard.sync.watcher.SaveFolderResolver;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

/** Opções manuais — só para casos raros em que o automático não acha a pasta. */
final class AdvancedOptionsDialog extends JDialog {

    private final AgentConfig config;
    private final JComboBox<GameSummary> gameCombo;
    private final JTextField folderField = new JTextField(32);
    private boolean saved;

    AdvancedOptionsDialog(Frame owner, AgentConfig config, java.util.List<GameSummary> games) {
        super(owner, "Opções avançadas", true);
        this.config = config;
        this.gameCombo = new JComboBox<>();
        for (GameSummary g : games) {
            gameCombo.addItem(g);
        }
        gameCombo.addActionListener(e -> loadFolderForGame());
        buildUi();
        loadFolderForGame();
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 8));
        form.add(new JLabel("<html><b>Só use se o automático não funcionar.</b><br>"
                + "Escolha o jogo e a pasta de saves (RetroArch → Settings → Directory → Save Files).</html>"));

        JPanel gameRow = new JPanel(new BorderLayout(4, 0));
        gameRow.add(new JLabel("Jogo: "), BorderLayout.WEST);
        gameRow.add(gameCombo, BorderLayout.CENTER);
        form.add(gameRow);

        JPanel folderRow = new JPanel(new BorderLayout(4, 0));
        folderRow.add(folderField, BorderLayout.CENTER);
        JButton browse = new JButton("...");
        browse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                folderField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        folderRow.add(browse, BorderLayout.EAST);
        form.add(folderRow);

        JButton detectBtn = new JButton("Detectar pasta automaticamente");
        detectBtn.addActionListener(e -> runDetect(detectBtn));
        form.add(detectBtn);

        root.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("Salvar pasta");
        ok.addActionListener(e -> saveAndClose());
        JButton cancel = new JButton("Cancelar");
        cancel.addActionListener(e -> dispose());
        buttons.add(cancel);
        buttons.add(ok);
        root.add(buttons, BorderLayout.SOUTH);

        add(root);
    }

    private void loadFolderForGame() {
        GameSummary game = (GameSummary) gameCombo.getSelectedItem();
        if (game == null) {
            folderField.setText("");
            return;
        }
        folderField.setText(config.getSaveFolder(game.id()));
    }

    private void runDetect(JButton detectBtn) {
        GameSummary game = (GameSummary) gameCombo.getSelectedItem();
        if (game == null) {
            return;
        }
        detectBtn.setEnabled(false);
        new SwingWorker<SaveFolderDetector.Result, Void>() {
            @Override
            protected SaveFolderDetector.Result doInBackground() {
                return new SaveFolderDetector().detect(game.title()).orElse(null);
            }

            @Override
            protected void done() {
                detectBtn.setEnabled(true);
                try {
                    SaveFolderDetector.Result r = get();
                    if (r == null) {
                        JOptionPane.showMessageDialog(AdvancedOptionsDialog.this,
                                "Não encontramos saves para \"" + game.title() + "\".\n"
                                        + "Salve no jogo e tente de novo, ou escolha a pasta manualmente.",
                                "Não encontrado", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        folderField.setText(r.folder().toString());
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AdvancedOptionsDialog.this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void saveAndClose() {
        GameSummary game = (GameSummary) gameCombo.getSelectedItem();
        if (game == null) {
            return;
        }
        String folder = folderField.getText().trim();
        if (folder.isBlank()) {
            JOptionPane.showMessageDialog(this, "Informe a pasta de saves.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        config.setSaveFolder(game.id(), folder);
        try {
            config.save();
        } catch (Exception ignored) {
        }
        saved = true;
        dispose();
    }

    boolean wasSaved() {
        return saved;
    }
}
