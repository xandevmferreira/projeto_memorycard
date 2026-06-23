package com.memorycard.sync.ui;

import com.memorycard.sync.api.GameSummary;
import com.memorycard.sync.api.MemoryCardClient;
import com.memorycard.sync.SyncAgentApp;
import com.memorycard.sync.config.AgentConfig;
import com.memorycard.sync.watcher.AutoSyncEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Interface simplificada: abrir o programa = sync automático. Sem configuração obrigatória.
 */
public class MainFrame extends JFrame {

    private final AgentConfig config;
    private final TrayController trayController;
    private final JLabel accountLabel = new JLabel();
    private final JLabel headlineLabel = new JLabel();
    private final JTextArea logArea = new JTextArea(14, 52);
    private final JButton stopBtn = new JButton("Parar sync");
    private final JButton hideBtn = new JButton("Minimizar para bandeja");
    private final JButton advancedBtn = new JButton("Opções avançadas...");
    private final JLabel statusLabel = new JLabel("Iniciando...");

    private MemoryCardClient client;
    private List<GameSummary> libraryGames = List.of();
    private Thread workerThread;
    private AutoSyncEngine autoSyncEngine;
    private final boolean pairedMode;

    public MainFrame(AgentConfig config) {
        super("MemoryCard Sync v" + SyncAgentApp.VERSION);
        this.config = config;
        this.pairedMode = config.isPairedFromDownload() && !config.getSyncToken().isBlank();
        config.setAutoPlayMode(true);
        setIconImage(AppIcon.create(64));
        buildUi();
        trayController = new TrayController(this);
        if (!trayController.isTrayAvailable()) {
            hideBtn.setEnabled(false);
        }
        pack();
        setMinimumSize(new Dimension(520, 420));
        setLocationRelativeTo(null);

        appendLog("MemoryCard Sync v" + SyncAgentApp.VERSION);
        if (pairedMode) {
            appendLog("Conta vinculada — conectando...");
            SwingUtilities.invokeLater(this::connect);
        } else {
            headlineLabel.setText("Baixe o programa em Sync no site para vincular sua conta.");
            statusLabel.setText("Não vinculado");
        }
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel header = new JPanel(new BorderLayout(4, 6));
        if (pairedMode) {
            accountLabel.setText("<html>Conta: <b>" + escapeHtml(displayAccount()) + "</b></html>");
            header.add(accountLabel, BorderLayout.NORTH);
        }
        headlineLabel.setText("<html><b>Automático</b> — abra, jogue e salve. Não precisa configurar nada.</html>");
        header.add(headlineLabel, BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        root.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(4, 8));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        footer.add(statusLabel, BorderLayout.NORTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        stopBtn.setEnabled(false);
        actions.add(stopBtn);
        actions.add(hideBtn);
        actions.add(advancedBtn);
        footer.add(actions, BorderLayout.SOUTH);
        root.add(footer, BorderLayout.SOUTH);

        add(root, BorderLayout.CENTER);

        stopBtn.addActionListener(e -> stopSync());
        hideBtn.addActionListener(e -> trayController.hideToTray());
        advancedBtn.addActionListener(e -> openAdvanced());
    }

    private void openAdvanced() {
        if (libraryGames.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Conecte ao site primeiro (baixe o zip em /sync-app).",
                    "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        AdvancedOptionsDialog dialog = new AdvancedOptionsDialog(this, config, libraryGames);
        dialog.setVisible(true);
        if (dialog.wasSaved() && autoSyncEngine != null) {
            autoSyncEngine.refreshAll();
            appendLog("Pasta manual salva — rebuscando saves...");
        }
    }

    private String displayAccount() {
        if (config.getAccountEmail() != null && !config.getAccountEmail().isBlank()) {
            return config.getAccountEmail();
        }
        return "MemoryCard";
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void connect() {
        appendLog("Conectando ao site...");
        statusLabel.setText("Conectando...");
        new SwingWorker<List<GameSummary>, Void>() {
            @Override
            protected List<GameSummary> doInBackground() throws Exception {
                client = new MemoryCardClient(config.getBaseUrl());
                client.setSyncToken(config.getSyncToken());
                return client.connect();
            }

            @Override
            protected void done() {
                try {
                    libraryGames = get();
                    if (libraryGames.isEmpty()) {
                        statusLabel.setText("Sem jogos na biblioteca");
                        appendLog("Cadastre jogos no site e reinicie o programa.");
                        return;
                    }
                    statusLabel.setText("Sync ativo — " + libraryGames.size() + " jogo(s)");
                    trayController.updateTooltip("Sync ativo");
                    appendLog("Conectado — " + libraryGames.size() + " jogo(s) na biblioteca.");
                    startAutoSync();
                } catch (Exception e) {
                    statusLabel.setText("Erro de conexão");
                    appendLog("Falha: " + e.getMessage());
                    JOptionPane.showMessageDialog(MainFrame.this,
                            e.getMessage() + "\n\nBaixe novamente em /sync-app no site.",
                            "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void startAutoSync() {
        if (client == null || libraryGames.isEmpty()) {
            return;
        }
        stopSync();
        autoSyncEngine = new AutoSyncEngine(
                client,
                config,
                () -> libraryGames,
                msg -> SwingUtilities.invokeLater(() -> appendLog(msg)),
                () -> SwingUtilities.invokeLater(() -> {
                    appendLog("Sessão expirada — baixe o zip de novo em /sync-app.");
                    statusLabel.setText("Desconectado");
                    stopSync();
                })
        );
        workerThread = new Thread(autoSyncEngine, "memorycard-autosync");
        workerThread.setDaemon(true);
        workerThread.start();
        stopBtn.setEnabled(true);
        statusLabel.setText("Sync ativo");
    }

    public void startSyncFromTray() {
        SwingUtilities.invokeLater(() -> {
            if (autoSyncEngine == null) {
                startAutoSync();
            }
        });
    }

    public void stopSyncFromTray() {
        SwingUtilities.invokeLater(this::stopSync);
    }

    private void stopSync() {
        if (autoSyncEngine != null) {
            autoSyncEngine.stop();
            autoSyncEngine = null;
        }
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
        stopBtn.setEnabled(false);
        if (client != null) {
            statusLabel.setText("Parado — abra de novo ou use a bandeja");
            appendLog("Sync parado.");
        }
    }

    public void prepareShutdown() {
        stopSync();
    }

    private void appendLog(String message) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.append("[" + time + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
