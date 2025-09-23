package dev.magnoix.msa.helpers;


import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.UUID;

public class PlayerDataFile {

    private final UUID uuid;
    private final File file;
    private final FileConfiguration config;

    /**
     * Load or create a player data file
     *
     * @param dataFolder Plugin's data folder. (getDataFolder())
     * @param uuid Player UUID.
     * @param fileName File name.
     */
    public PlayerDataFile(File dataFolder, UUID uuid, String fileName) {
        this.uuid = uuid;

        // players/<uuid>/<filename>
        File playerFolder = new File(dataFolder, "players" + File.separator + uuid.toString());
        if (!playerFolder.exists()) playerFolder.mkdirs();

        this.file = new File(playerFolder, fileName);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    // Convenience Methods

    /**
     * Convenience: Load player mail file.
     */
    public static PlayerDataFile loadMail(File dataFolder, Player player) {
        return new PlayerDataFile(dataFolder, player.getUniqueId(), "mail.yml");
    }
    /**
     * Convenience: Load player stats file.
     */
    public static PlayerDataFile loadStats(File dataFolder, Player player) {
        return new PlayerDataFile(dataFolder, player.getUniqueId(), "stats.yml");
    }

}
