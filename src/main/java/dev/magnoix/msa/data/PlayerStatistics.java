package dev.magnoix.msa.data;

import org.bukkit.entity.Player;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Deprecated
public class PlayerStatistics {

    public enum StatisticType {
        KILLS("statistic.kills"),
        EVENT_WINS("statistic.eventwins"),
        DEATHS("statistic.deaths");

        private final String path;

        StatisticType(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    // PlayerStatistics implementation

    private final PlayerDataFile dataFile;
    private final Map<StatisticType, Integer> cachedStats;

    /**
     * Create a new Statistic instance for a player
     *
     * @param dataFolder Plugin's data folder
     * @param player The player
     */
    public PlayerStatistics(File dataFolder, Player player) {
        this.dataFile = PlayerDataFile.loadStats(dataFolder, player);
        this.cachedStats = new HashMap<>();
        loadStats();
    }

    /**
     * Load all statistics from a file into a cache
     */
    private void loadStats() {
        for (StatisticType type : StatisticType.values()) {
            int value = dataFile.getConfig().getInt(type.getPath(), 0);
            cachedStats.put(type, value);
        }
    }

    /**
     * Get a statistic value
     *
     * @param type The statistic type
     * @return The current value
     */
    public int getStat(StatisticType type) {
        return cachedStats.getOrDefault(type, 0);
    }

    /**
     * Set a statistic value
     *
     * @param type The statistic type
     * @param value The new value
     */
    public void setStat(StatisticType type, int value) {
        cachedStats.put(type, value);
        dataFile.getConfig().set(type.getPath(), value);
    }

    /**
     * Increment a statistic by 1
     *
     * @param type The statistic type
     */
    public void incrementStat(StatisticType type) {
        incrementStat(type, 1);
    }

    /**
     * Increment a statistic by a specific amount
     *
     * @param type The statistic type
     * @param amount The amount to increment by
     */
    public void incrementStat(StatisticType type, int amount) {
        int current = getStat(type);
        setStat(type, current + amount);
    }

    /**
     * Reset a statistic to 0
     *
     * @param type The statistic type
     */
    public void resetStat(StatisticType type) {
        setStat(type, 0);
    }

    /**
     * Reset all statistics to 0
     */
    public void resetAllStats() {
        for (StatisticType type : StatisticType.values()) {
            resetStat(type);
        }
    }

    /**
     * Save all statistics to file
     */
    public void save() {
        dataFile.save();
    }

    /**
     * Get all statistics as a map
     *
     * @return Map of all statistics
     */
    public Map<StatisticType, Integer> getAllStats() {
        return new HashMap<>(cachedStats);
    }
}