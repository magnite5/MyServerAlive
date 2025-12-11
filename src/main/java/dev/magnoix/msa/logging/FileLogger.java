package dev.magnoix.msa.logging;

import dev.magnoix.msa.messages.Msg;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

public class FileLogger {
    private final JavaPlugin plugin;
    private final File file;

    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FileLogger(JavaPlugin plugin, String filename) {
        this.plugin = plugin;

        // Plugin Folder
        File folder = plugin.getDataFolder();
        if (!folder.exists()) folder.mkdirs();
        // Logs Folder
        File logsFolder = new File(folder, "logs");
        if (!logsFolder.exists()) logsFolder.mkdirs();

        this.file = new File(logsFolder, filename);

        try {
            if (!file.exists()) file.createNewFile();
        } catch (IOException e) {
            Msg.log(Level.SEVERE, "An error occurred while creating the " + file.getName() + " log file: " + e.getMessage());
        }
    }

    /**
     * Adds a line to the log file, prefixed with the current time.
     * @param message The message to add.
     */
    public void log(String message) {
        String s = String.format("[%s] %s", TF.format(LocalDateTime.now()), message);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (FileWriter fileWriter = new FileWriter(file, true)) {
                fileWriter.write(s + System.lineSeparator());
            } catch (IOException e) {
                Msg.log(Level.SEVERE, "An error occurred while writing to the " + file.getName() + " log file: "+ e.getMessage());
            }
        });
    }
}
