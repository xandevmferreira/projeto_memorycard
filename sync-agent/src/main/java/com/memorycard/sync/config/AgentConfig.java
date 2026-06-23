package com.memorycard.sync.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class AgentConfig {

    private static final Path CONFIG_PATH = Path.of(System.getProperty("user.home"), ".memorycard", "sync-agent.properties");

    private String baseUrl = "http://localhost:8080";
    private String syncToken = "";
    private String accountEmail = "";
    private long gameId;
    private String watchFolder = "";
    private int pollSeconds = 15;
    private boolean autoPlayMode;
    private final Map<Long, String> processNamesByGameId = new HashMap<>();
    private final Map<Long, String> saveFoldersByGameId = new HashMap<>();
    private boolean pairedFromDownload;

    public static AgentConfig load() {
        AgentConfig config = new AgentConfig();
        mergeFile(config, CONFIG_PATH, false);
        mergeFile(config, Path.of(System.getProperty("user.dir"), "connection.properties"), true);
        mergeFile(config, resolveAppDir().resolve("connection.properties"), true);
        return config;
    }

    private static void mergeFile(AgentConfig config, Path path, boolean pairingFile) {
        if (!Files.isRegularFile(path)) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
            if (props.containsKey("baseUrl")) {
                config.baseUrl = props.getProperty("baseUrl", config.baseUrl);
            }
            if (props.containsKey("syncToken")) {
                config.syncToken = props.getProperty("syncToken", "");
                if (pairingFile && !config.syncToken.isBlank()) {
                    config.pairedFromDownload = true;
                }
            }
            if (props.containsKey("accountEmail")) {
                config.accountEmail = props.getProperty("accountEmail", "");
            }
            if (props.containsKey("gameId")) {
                config.gameId = Long.parseLong(props.getProperty("gameId", "0"));
            }
            if (props.containsKey("watchFolder")) {
                config.watchFolder = props.getProperty("watchFolder", "");
            }
            if (props.containsKey("pollSeconds")) {
                config.pollSeconds = Integer.parseInt(props.getProperty("pollSeconds", "15"));
            }
            if (props.containsKey("autoPlayMode")) {
                config.autoPlayMode = Boolean.parseBoolean(props.getProperty("autoPlayMode", "false"));
            }
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith("game.") && key.endsWith(".processName")) {
                    String middle = key.substring("game.".length(), key.length() - ".processName".length());
                    try {
                        long gid = Long.parseLong(middle);
                        config.processNamesByGameId.put(gid, props.getProperty(key, "").trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (key.startsWith("game.") && key.endsWith(".saveFolder")) {
                    String middle = key.substring("game.".length(), key.length() - ".saveFolder".length());
                    try {
                        long gid = Long.parseLong(middle);
                        config.saveFoldersByGameId.put(gid, props.getProperty(key, "").trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (IOException | NumberFormatException ignored) {
            // keep previous values
        }
    }

    private static Path resolveAppDir() {
        try {
            var url = AgentConfig.class.getProtectionDomain().getCodeSource().getLocation();
            if (url == null) {
                return Path.of(System.getProperty("user.dir"));
            }
            Path path = Path.of(url.toURI());
            if (Files.isRegularFile(path)) {
                return path.getParent();
            }
            return path;
        } catch (URISyntaxException e) {
            return Path.of(System.getProperty("user.dir"));
        }
    }

    public void save() throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());
        Properties props = new Properties();
        props.setProperty("baseUrl", baseUrl);
        props.setProperty("syncToken", syncToken);
        props.setProperty("gameId", Long.toString(gameId));
        props.setProperty("watchFolder", watchFolder);
        props.setProperty("pollSeconds", Integer.toString(pollSeconds));
        props.setProperty("autoPlayMode", Boolean.toString(autoPlayMode));
        for (Map.Entry<Long, String> entry : processNamesByGameId.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                props.setProperty("game." + entry.getKey() + ".processName", entry.getValue());
            }
        }
        for (Map.Entry<Long, String> entry : saveFoldersByGameId.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                props.setProperty("game." + entry.getKey() + ".saveFolder", entry.getValue());
            }
        }
        try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            props.store(out, "MemoryCard Sync Agent");
        }
    }

    public boolean isPairedFromDownload() { return pairedFromDownload; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getSyncToken() { return syncToken; }
    public void setSyncToken(String syncToken) { this.syncToken = syncToken; }
    public String getAccountEmail() { return accountEmail; }
    public long getGameId() { return gameId; }
    public void setGameId(long gameId) { this.gameId = gameId; }
    public String getWatchFolder() { return watchFolder; }
    public void setWatchFolder(String watchFolder) { this.watchFolder = watchFolder; }
    public int getPollSeconds() { return pollSeconds; }
    public void setPollSeconds(int pollSeconds) { this.pollSeconds = pollSeconds; }
    public boolean isAutoPlayMode() { return autoPlayMode; }
    public void setAutoPlayMode(boolean autoPlayMode) { this.autoPlayMode = autoPlayMode; }
    public Map<Long, String> getProcessNamesByGameId() { return processNamesByGameId; }
    public void setProcessName(long gameId, String processName) {
        if (processName == null || processName.isBlank()) {
            processNamesByGameId.remove(gameId);
        } else {
            processNamesByGameId.put(gameId, processName.trim());
        }
    }
    public String getProcessName(long gameId) {
        return processNamesByGameId.getOrDefault(gameId, "");
    }
    public void setSaveFolder(long gameId, String folder) {
        if (folder == null || folder.isBlank()) {
            saveFoldersByGameId.remove(gameId);
        } else {
            saveFoldersByGameId.put(gameId, folder.trim());
        }
    }
    public String getSaveFolder(long gameId) {
        return saveFoldersByGameId.getOrDefault(gameId, "");
    }
}
