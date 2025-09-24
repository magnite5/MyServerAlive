package dev.magnoix.msa.messages;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("ClassCanBeRecord")
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

    public record MailEntry(String sender, String message) {}

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private File getMailFile(UUID uuid) {
        File playerFolder = new File(playerDataFolder, uuid.toString());
        if (!playerFolder.exists()) playerFolder.mkdirs();
        return new File(playerFolder, "mail.yml");
    }

    private void saveConfig(FileConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getMapsFromConfig(FileConfiguration config, MailType type) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<?, ?> map : config.getMapList(type.getPath())) {
            list.add((Map<String, Object>) map); // unchecked cast, but works with YAML
        }
        return list;
    }

    public void addMail(UUID uuid, MailType type, MailEntry mailEntry) {
        File file = getMailFile(uuid);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        List<Map<String, Object>> list = getMapsFromConfig(config, type);

        Map<String, Object> entryMap = new HashMap<>();
        entryMap.put("sender", mailEntry.sender());
        entryMap.put("message", mailEntry.message());
        list.add(entryMap);

        config.set(type.getPath(), list);
        saveConfig(config, file);
    }

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

    public boolean removeMail(UUID uuid, MailType type, MailEntry target) {
        File file = getMailFile(uuid);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        List<Map<String, Object>> list = getMapsFromConfig(config, type);

        boolean removed = list.removeIf(entry ->
                Objects.equals(entry.get("sender"), target.sender()) &&
                        Objects.equals(entry.get("message"), target.message())
        );

        if (removed) {
            config.set(type.getPath(), list);
            saveConfig(config, file);
        }

        return removed;
    }

    public boolean removeMail(UUID uuid, MailType type, int index) {
        List<MailEntry> mail = getMail(uuid, type);
        if (index < 0 || index >= mail.size()) return false;
        return removeMail(uuid, type, mail.get(index));
    }
}
