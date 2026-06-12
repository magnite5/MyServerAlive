package dev.magnoix.msa.databases;

import dev.magnoix.msa.logging.FileLogger;
import dev.magnoix.msa.messages.Msg;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class StatisticsManager {
    //TODO: Use Long or Double instead of int for statistic values

    private final Map<UUID, Map<String, Integer>> playerStats = new HashMap<>();
    private final Set<UUID> dirtyPlayers = new HashSet<>();

    private final Connection connection;

    private final FileLogger statsLogger;
    private final Set<String> loggedTypes;

    private Set<String> validTypes;

    public StatisticsManager(JavaPlugin plugin, Connection connection, Set<String> loggedTypes) throws SQLException {
        this.connection = connection;
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS player_stats (
                    uuid TEXT NOT NULL,
                    type TEXT NOT NULL,
                    value INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (uuid, type))
            """);
            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_stat_type_value ON player_stats (type, value DESC)
            """);
            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_uuid_stat_type ON player_stats (uuid, type)
            """);
        }
        statsLogger = new FileLogger(plugin, "stats.log");
        this.loggedTypes = loggedTypes;
    }

    /// STAT TYPE MANIPULATION

    /**
     * Updates & Returns the list of valid statistic types. Should only be called on plugin startup.
     * @param plugin The JavaPlugin instance.
     * @return The updated set of type names.
     */
    public Set<String> updateStatisticTypes(JavaPlugin plugin) {
        List<String> builtInTypes = List.of("kills", "deaths", "networth");
        Set<String> types = new HashSet<>(builtInTypes);
        plugin.getConfig().getStringList("statistics.custom-types").forEach(s ->
                types.add(s.trim().toLowerCase().replaceAll(" ", "_")));
        loggedTypes.removeIf(type -> !types.contains(type));
        validTypes = types;
        return validTypes;
    }

    /**
     * Returns a set of valid statistic type names.
     * @return A set of strings representing valid statistic names.
     */
    public Set<String> getValidStatisticTypes() {
        return validTypes;
    }

    /// CACHE MANIPULATION
    public void addPlayer(UUID uuid) throws SQLException{
        Map<String, Integer> stats = new HashMap<>();
        if (playerStats.containsKey(uuid)) return;

        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT type, value FROM player_stats WHERE uuid = ?")) {
            preparedStatement.setString(1, uuid.toString());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    stats.put(resultSet.getString("type"), resultSet.getInt("value"));
                }
            }
        }

        boolean hasStats = !stats.isEmpty();
        if (!hasStats) validTypes.forEach(type -> stats.put(type, 0));

        playerStats.put(uuid, stats);

        if (!hasStats) dirtyPlayers.add(uuid);
    }

    public void flushCache(UUID uuid) throws SQLException {
        if (!playerStats.containsKey(uuid)) return;
        updateLeaderboardCache();

        Map<String, Integer> stats = playerStats.get(uuid);
        if (dirtyPlayers.contains(uuid)) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("""
                INSERT INTO player_stats (uuid, type, value) VALUES (?, ?, ?)
                ON CONFLICT (uuid, type) DO UPDATE SET value = excluded.value
                """)) {
                for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                    preparedStatement.setString(1, uuid.toString());
                    preparedStatement.setString(2, entry.getKey());
                    preparedStatement.setInt(3, entry.getValue());
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
            }
            dirtyPlayers.remove(uuid);
        }

        playerStats.keySet().removeIf(key -> Bukkit.getPlayer(key) == null);
    }

    public void flushCache() throws SQLException {
        if (dirtyPlayers.isEmpty() || playerStats.isEmpty()) return;
        updateLeaderboardCache();

        Set<UUID> flushedPlayers = new HashSet<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement("""
                INSERT INTO player_stats (uuid, type, value) VALUES (?, ?, ?)
                ON CONFLICT (uuid, type) DO UPDATE SET value = excluded.value
                """)) {
            for (Map.Entry<UUID, Map<String, Integer>> entry : playerStats.entrySet()) {
                UUID uuid = entry.getKey();
                if (dirtyPlayers.contains(uuid)) {
                    for (Map.Entry<String, Integer> stat : entry.getValue().entrySet()) {
                        preparedStatement.setString(1, uuid.toString());
                        preparedStatement.setString(2, stat.getKey());
                        preparedStatement.setInt(3, stat.getValue());
                        preparedStatement.addBatch();
                    }
                    flushedPlayers.add(uuid);
                }
            }
            preparedStatement.executeBatch();
        }
        dirtyPlayers.removeAll(flushedPlayers);
        playerStats.keySet().removeIf(key -> Bukkit.getPlayer(key) == null);
    }

    /// PLAYER MANIPULATION

    /**
     * Removes a player from the statistics database.
     * @param uuid The UUID of the player to remove.
     * @throws SQLException If a database access error occurs.
     */
    public void removePlayer(UUID uuid) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM player_stats WHERE uuid = ?")) {
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.executeUpdate();
            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
            Msg.log(Level.WARNING, "Player " + (playerName != null ? playerName : uuid.toString()) + " has been removed from the statistics database.");
        }
        playerStats.remove(uuid);
        dirtyPlayers.remove(uuid);
        leaderboardCache.forEach((type, list) -> list.removeIf(entry -> entry.player().equals(uuid)));
    }

    /**
     * Resets all statistics for a specific player to zero.
     * @param uuid The UUID of the player to reset.
     * @throws SQLException If a database access error occurs.
     */
    public void resetPlayer(UUID uuid) throws SQLException {
        if (!playerStats.containsKey(uuid)) {
            addPlayer(uuid);
        }
        playerStats.get(uuid).replaceAll((type, value) -> 0);
        dirtyPlayers.add(uuid);

        String playerName = Bukkit.getOfflinePlayer(uuid).getName();
        Msg.log(Level.WARNING, "Player " + (playerName != null ? playerName : uuid) + "'s statistics have been reset.");
    }

    /**
     * Checks if a player exists in the statistics database.
     * @param uuid The UUID of the player to check.
     * @return True if the player exists, false otherwise.
     * @throws SQLException If a database access error occurs.
     */
    public boolean playerExists(UUID uuid) throws SQLException {
        if (playerStats.containsKey(uuid)) {
            return true;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM player_stats WHERE uuid = ? LIMIT 1")) {
            preparedStatement.setString(1, uuid.toString());
            return preparedStatement.executeQuery().next();
        }
    }

    /// STAT MANIPULATION

    /**
     * Gets a specific statistic for a player.
     * @param uuid  The UUID of the player.
     * @param type  The type the statistic to retrieve.
     * @return The value of the statistic.
     * @throws SQLException If a database access error occurs.
     * @throws IllegalArgumentException If the statistic name is invalid.
     */
    public int getStatistic(UUID uuid, String type) throws SQLException {
        type = type.trim().toLowerCase();
        if (!validTypes.contains(type)) throw new IllegalArgumentException("Unknown statistic type: " + type);

        if (playerStats.containsKey(uuid)) {
            return playerStats.get(uuid).getOrDefault(type, 0);
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT value FROM player_stats WHERE uuid = ? AND type = ?")) {
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.setString(2, type);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) return resultSet.getInt("value");
            else return 0;
        }
    }

    /**
     * Sets a specific statistic for a player to a given value.
     *
     * @param uuid  The UUID of the player.
     * @param type  The type of statistic to set.
     * @param value The new value for the statistic.
     * @return The former value, 0 if a new entry had to be created.
     * @throws SQLException             If a database access error occurs.
     * @throws IllegalArgumentException If the statistic name is invalid.
     */
    public int setStatistic(UUID uuid, String type, int value) throws SQLException {
        type = type.trim().toLowerCase();
        if (!validTypes.contains(type)) throw new IllegalArgumentException("Invalid Statistic: " + type);

        if (!playerStats.containsKey(uuid)) addPlayer(uuid);

        int oldValue = getStatistic(uuid, type);

        playerStats.get(uuid).put(type, value);
        dirtyPlayers.add(uuid);

        logIfLogged(type, "Set " + type + " for " + uuid + " from " + oldValue + " to " + value + ".");
        return oldValue;
    }

    /**
     * Adds a value to a player's specific statistic.
     * @param uuid  The UUID of the player.
     * @param type  The type of statistic to modify.
     * @param value The value to add.
     * @return The former value, 0 if a new entry had to be created.
     * @throws SQLException If a database access error occurs.
     */
    public int addToStatistic(UUID uuid, String type, int value) throws SQLException {
        type = type.trim().toLowerCase();
        if (!validTypes.contains(type)) throw new IllegalArgumentException("Invalid Statistic: " + type);
        if (!playerStats.containsKey(uuid)) addPlayer(uuid);

        int oldValue = getStatistic(uuid, type);
        playerStats.get(uuid).put(type, oldValue + value);
        dirtyPlayers.add(uuid);

        logIfLogged(type, "Added " + value + " to " + type + " for " + uuid + ", from " + oldValue + " to " + (oldValue + value) + ".");
        return oldValue;
    }

    /**
     * Multiplies a player's specific statistic by a given multiplier.
     * @param uuid       The UUID of the player.
     * @param type       The type of statistic to modify.
     * @param multiplier The multiplier to apply.
     * @return The former value, 0 if a new entry had to be created.
     * @throws SQLException If a database access error occurs.
     */
    public int multiplyStatistic(UUID uuid, String type, double multiplier) throws SQLException {
        type = type.trim().toLowerCase();
        int oldValue = getStatistic(uuid, type); // Already checking for type validity.
        setStatistic(uuid, type, (int) (oldValue * multiplier));

        logIfLogged(type, "Multiplied " + type + " for " + uuid + " by " + multiplier + " from " + oldValue + " to " + (oldValue * multiplier) + ".");
        return oldValue;
    }

    /**
     * Retrieves the number of entries of a given statistic type.
     * @param type The type of statistic to count.
     * @return The number of entries.
     * @throws SQLException If a database access error occurs.
     */
    public int getStatisticCount(String type) throws SQLException {
        type = type.trim().toLowerCase();
        if (!validTypes.contains(type)) throw new IllegalArgumentException("Invalid Statistic: " + type);

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT COUNT(*) AS total FROM player_stats WHERE type = ? AND value > 0")) {
            preparedStatement.setString(1, type);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) return resultSet.getInt("total");
        }
        return 0;
    }

    /// LEADERBOARDS

    public record LeaderboardEntry(UUID player, double value) {}

    private final Map<String, List<LeaderboardEntry>> leaderboardCache = new HashMap<>();
    private final int leaderboardCacheSize = 100;

    public int getLeaderboardCacheSize() {
        return leaderboardCacheSize;
    }

    public void updateLeaderboardCache() throws SQLException {
        if (leaderboardCache.isEmpty()) {
            for (String type : validTypes) {
                leaderboardCache.put(type, getTopPlayers(type, leaderboardCacheSize));
            }
        }
        Set<String> dirtyTypes = new HashSet<>();
        for (UUID uuid : dirtyPlayers) {
            for (String type : validTypes) {
                List<LeaderboardEntry> leaderboard = leaderboardCache.get(type);
                leaderboard.removeIf(entry -> entry.player().equals(uuid));

                int value = getStatistic(uuid, type);
                if (value > 0) leaderboard.add(new LeaderboardEntry(uuid, value));
                dirtyTypes.add(type);
            }
        }
        for (String type : dirtyTypes) {
            List<LeaderboardEntry> leaderboard = leaderboardCache.get(type);
            leaderboard.sort(Comparator.comparingDouble(LeaderboardEntry::value).reversed());
            if (leaderboard.size() > leaderboardCacheSize) leaderboard.subList(leaderboardCacheSize, leaderboard.size()).clear();
        }
    }

    public void rebuildLeaderboardCache() throws SQLException {
        leaderboardCache.clear();
        updateLeaderboardCache();
    }

    /**
     * Gets the top players for a given statistic.
     * @param type  The type of statistic.
     * @param limit The maximum number of players to return.
     * @return A list of LeaderboardEntry records, sorted by value in descending order.
     * @throws SQLException If a database access error occurs.
     */
    public List<LeaderboardEntry> getTopPlayers(String type, int limit) throws SQLException {
        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        type = type.trim().toLowerCase();
        if (!validTypes.contains(type)) return leaderboard;

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT uuid, value FROM player_stats WHERE type = ? ORDER BY value DESC LIMIT ?")) {
            preparedStatement.setString(1, type);
            preparedStatement.setInt(2, limit);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int value = resultSet.getInt("value");
                if (value <= 0) continue;
                leaderboard.add(new LeaderboardEntry(
                    UUID.fromString(resultSet.getString("uuid")),
                    value));
            }
        }
        return leaderboard;
    }

    public List<LeaderboardEntry> getLeaderboard(String type) {
        type = type.trim().toLowerCase();
        try {
            if (leaderboardCache.isEmpty()) rebuildLeaderboardCache();
            return new ArrayList<>(leaderboardCache.getOrDefault(type, new ArrayList<>()));
        } catch (SQLException e) {
            Msg.log(Level.SEVERE, "Failed to get leaderboard for type " + type + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves a list of leaderboard entries for a given statistic, ordered descending.
     * @param type        The statistic type to sort and fetch. Must be in validColumns.
     * @param startIndex  The index at which to start fetching entries (0-based).
     * @param endIndex    The index at which to stop fetching entries (exclusive).
     * @return A list of LeaderboardEntry objects for the specified range.
     * @throws IllegalArgumentException If an invalid statistic, limit, or offset is provided.
     */
    public List<LeaderboardEntry> getTopPlayers(String type, int startIndex, int endIndex) {
        type = type.trim().toLowerCase();
        if (!validTypes.contains(type)) throw new IllegalArgumentException("Invalid statistic type: " + type);

        List<LeaderboardEntry> leaderboard = getLeaderboard(type);
        if (leaderboard.size() < endIndex) throw new IllegalArgumentException("Invalid offset: " + endIndex);

        return new ArrayList<>(leaderboard.subList(startIndex, endIndex));
    }

    /// LOGGING
    public FileLogger getLogger() {
        return statsLogger;
    }

    public void logIfLogged(String type, String message) {
        if (loggedTypes.contains(type))
            statsLogger.log(message);
    }
    public void log(String message) {
        statsLogger.log(message);
    }
}
