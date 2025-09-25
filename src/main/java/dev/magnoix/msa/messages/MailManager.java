package dev.magnoix.msa.messages;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages per-player mail storage backed by YAML files.
 * Thread-safe for write operations; reads are non-blocking.
 */
public class MailManager {

    private final File playerDataFolder;
    private final Object mailLock = new Object(); // ensures thread-safe operations

    /**
     * Create a new MailManager using the given root data folder.
     *
     * @param playerDataFolder base folder where each player's subfolder will be created and mail.yml stored
     */
    public MailManager(File playerDataFolder) {
        this.playerDataFolder = playerDataFolder;
        if (!playerDataFolder.exists()) playerDataFolder.mkdirs();
    }

    /**
     * Mail categories and their YAML paths.
     */
    public enum MailType {
        /**
         * Player-to-player mails. YAML path: "mail.players"
         */
        PLAYERS("mail.players"),
        /**
         * Server/system mails. YAML path: "mail.server"
         */
        SERVER("mail.server");

        private final String path;
        MailType(String path) { this.path = path; }

        /**
         * Get the YAML path used to store this mail type.
         *
         * @return YAML path string (e.g., "mail.players")
         */
        public String getPath() { return path; }
    }

    /**
     * Immutable mail entry consisting of a sender name and a message body.
     *
     * @param sender  non-null sender identifier (e.g., player uuid or "Server")
     * @param message non-null message content
     */
    public record MailEntry(String sender, String message) {} // TODO: Change all instances to use UUIDs

    private File getMailFile(UUID uuid) {
        File playerFolder = new File(playerDataFolder, uuid.toString());
        if (!playerFolder.exists()) playerFolder.mkdirs();
        return new File(playerFolder, "mail.yml");
    }

    private void saveConfigAtomic(FileConfiguration config, File file) { // Prevents "atomic" config changes, where multiple instances try to write at the same time, which would crash the server
        try {
            File temp = new File(file.getAbsolutePath() + ".tmp");
            config.save(temp);
            if (!temp.renameTo(file)) {
                throw new IOException("Failed to replace original mail file");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getMapsFromConfig(FileConfiguration config, MailType type) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<?, ?> map : config.getMapList(type.getPath())) {
            list.add((Map<String, Object>) map); // unchecked cast, works with YAML
        }
        return list;
    }

    /**
     * Append a mail entry for a player under the specified mail type.
     * This operation is synchronized to ensure file consistency.
     *
     * @param uuid       player's UUID whose mailbox to modify
     * @param type       mail category to add to (PLAYERS or SERVER)
     * @param mailEntry  entry to add; fields sender and message are persisted
     */
    public void addMail(UUID uuid, MailType type, MailEntry mailEntry) {
        synchronized (mailLock) {
            File file = getMailFile(uuid);
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            List<Map<String, Object>> list = getMapsFromConfig(config, type);

            Map<String, Object> entryMap = new HashMap<>();
            entryMap.put("sender", mailEntry.sender());
            entryMap.put("message", mailEntry.message());
            list.add(entryMap);

            config.set(type.getPath(), list);
            saveConfigAtomic(config, file);
        }
    }

    /**
     * Read all mail entries for a player and type.
     * Returns an immutable snapshot list; does not modify on-disk data.
     *
     * @param uuid player's UUID whose mail to read
     * @param type mail category to read (PLAYERS or SERVER)
     * @return list of MailEntry in stored order (may be empty, never null)
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

    /**
     * Remove the first mail entry matching the given sender and message for a player and type.
     * This operation is synchronized to ensure file consistency.
     *
     * @param uuid   player's UUID whose mail to modify
     * @param type   mail category to remove from (PLAYERS or SERVER)
     * @param target MailEntry to match by equality of sender and message
     * @return true if an entry was removed and persisted; false if no matching entry existed
     */
    public boolean removeMail(UUID uuid, MailType type, MailEntry target) {
        synchronized (mailLock) {
            File file = getMailFile(uuid);
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            List<Map<String, Object>> list = getMapsFromConfig(config, type);

            boolean removed = list.removeIf(entry ->
                    Objects.equals(entry.get("sender"), target.sender()) &&
                            Objects.equals(entry.get("message"), target.message())
            );

            if (removed) {
                config.set(type.getPath(), list);
                saveConfigAtomic(config, file);
            }

            return removed;
        }
    }

    /**
     * Remove a mail entry at the given index for a player and type.
     * Index is based on the order returned by getMail(uuid, type).
     *
     * @param uuid  player's UUID whose mail to modify
     * @param type  mail category to remove from (PLAYERS or SERVER)
     * @param index zero-based index in the current mailbox list
     * @return true if the entry existed and was removed; false if index was out of bounds
     */
    public boolean removeMail(UUID uuid, MailType type, int index) {
        List<MailEntry> mail = getMail(uuid, type);
        if (index < 0 || index >= mail.size()) return false;
        return removeMail(uuid, type, mail.get(index));
    }
}
