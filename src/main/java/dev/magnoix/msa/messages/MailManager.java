package dev.magnoix.msa.messages;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MailManager {

    private final File playerDataFolder;

    public MailManager(File playerDataFolder) {
        this.playerDataFolder = playerDataFolder;
        if (!playerDataFolder.exists()) playerDataFolder.mkdirs();
    }

    public enum MailType {
        PLAYERS("mail.players"),
        SERVER("mail.server");

        private final String path;
        MailType(String path) { this.path = path; }
        public String getPath() { return path; }
    }

    public static class MailEntry {

        private final String sender; // UUID string, or "SERVER"
        private final String message;

        public MailEntry(String sender, String message) {
            this.sender = sender;
            this.message = message;
        }

        public String sender() { return sender; }
        public String message() { return message; }
    }

    private File getMailFile(UUID uuid) {
        File playerFolder = new File(playerDataFolder, uuid.toString());
        if(!playerFolder.exists()) playerFolder.mkdirs();
        return new File(playerFolder, "mail.yml");
    }

    private void saveConfig(FileConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addMail(UUID uuid, MailType type, MailEntry mailEntry) {
        File file = getMailFile(uuid);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Convert existing raw list into a safe typed list
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<?, ?> map : config.getMapList(type.getPath())) {
            Map<String, Object> entry = new HashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() instanceof String key) {
                    entry.put(key, e.getValue());
                }
            }
            list.add(entry);
        }

        // Add the new entry from MailEntry
        Map<String, Object> entry = new HashMap<>();
        entry.put("sender", mailEntry.sender());
        entry.put("message", mailEntry.message());
        list.add(entry);

        config.set(type.getPath(), list);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*
     * Get all mail of a certain type sent to a specified player.
     * @param uuid Player UUID
     * @param type Mail type
     */
    public List<MailEntry> getMail(UUID uuid, MailType type) {
        File file = getMailFile(uuid);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        List<Map<?, ?>> raw = config.getMapList(type.getPath());
        List<MailEntry> mail = new ArrayList<>();

        for (Map<?, ?> map : raw) {
            Object senderObj = map.get("sender");
            Object msgObj = map.get("message");
            if (senderObj instanceof String sender && msgObj instanceof String msg) {
                mail.add(new MailEntry(sender, msg));
            }
        }
        return mail;
    }

}
