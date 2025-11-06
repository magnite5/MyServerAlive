package dev.magnoix.msa.databases;

import dev.magnoix.msa.messages.Msg;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class StatisticsManager { //TODO: Use Long or Double instead of int for statistic values

    private final Connection connection;

    private Set<String> validTypes;

    public StatisticsManager(Connection connection) throws SQLException {
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
        getValidStatisticTypes();
    }

    /**
     * Returns a set of valid statistic type names.
     * @return A set of strings representing valid statistic names.
     */
    public Set<String> getValidStatisticTypes() throws SQLException {
        if (validTypes == null || validTypes.isEmpty()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT DISTINCT stat_type FROM player_stats")) {
                Set<String> types = new HashSet<>();
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) types.add(resultSet.getString("stat_type"));
                validTypes = types;
            }
        }
        return validTypes;
    }

    /**
     * Adds a new player to the statistics database.
     * @param uuid The UUID of the player to add.
     * @throws SQLException If a database access error occurs.
     */
    public void addPlayer(UUID uuid) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT OR IGNORE INTO player_stats (uuid, stat_type, value) VALUES (?, ?, 0)")) {
            for (String type : validTypes) {
                preparedStatement.setString(1, uuid.toString());
                preparedStatement.setString(2, type);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
            Msg.log(Level.WARNING, "Player " + (playerName != null ? playerName : uuid.toString()) + " has been added to the statistics database.");
        }
    }

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
    }

    /**
     * Resets all statistics for a specific player to zero.
     * @param uuid The UUID of the player to reset.
     * @throws SQLException If a database access error occurs.
     */
    public void resetPlayer(UUID uuid) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE player_stats SET value = 0 WHERE uuid = ?")) {
            preparedStatement.setString(1, uuid.toString());
            int updated = preparedStatement.executeUpdate();
            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
            Msg.log(Level.WARNING, "Player " + (playerName != null ? playerName : uuid) + "'s " + updated + " statistic" + (updated == 1 ? "s" : "") + " has been reset.");
        }
    }

    /**
     * Checks if a player exists in the statistics database.
     * @param uuid The UUID of the player to check.
     * @return True if the player exists, false otherwise.
     * @throws SQLException If a database access error occurs.
     */
    public boolean playerExists(UUID uuid) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT uuid FROM player_stats WHERE uuid = ?")) {
            preparedStatement.setString(1, uuid.toString());
            return preparedStatement.executeQuery().next();
        }
    }

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
        if (!validTypes.contains(type)) throw new IllegalArgumentException("Invalid Statistic: " + type);

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT value FROM player_stats WHERE uuid = ? AND stat_type = ?")) {
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
     * @throws SQLException             If a database access error occurs.
     * @throws IllegalArgumentException If the statistic name is invalid.
     */
    public void setStatistic(UUID uuid, String type, int value) throws SQLException {
        type = type.trim().toLowerCase();
        if (!validTypes.contains(type)) throw new IllegalArgumentException("Invalid Statistic: " + type);
        if (!playerExists(uuid)) addPlayer(uuid);

        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE player_stats SET value = ? WHERE uuid = ? AND stat_type = ?")) {
            preparedStatement.setInt(1, value);
            preparedStatement.setString(2, uuid.toString());
            preparedStatement.setString(3, type);
            int updated = preparedStatement.executeUpdate();

            if (updated == 0) {
                try (PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO player_stats (uuid, stat_type, value) VALUES (?, ?, ?)")) {
                    insertStatement.setString(1, uuid.toString());
                    insertStatement.setString(2, type);
                    insertStatement.setInt(3, value);
                    insertStatement.executeUpdate();
                }
            }
        }
    }

    /**
     * Adds a value to a player's specific statistic.
     * @param uuid  The UUID of the player.
     * @param type  The type of statistic to modify.
     * @param value The value to add.
     * @throws SQLException If a database access error occurs.
     */
    public void addToStatistic(UUID uuid, String type, int value) throws SQLException {
        type = type.trim().toLowerCase();
        if (!validTypes.contains(type)) throw new IllegalArgumentException("Invalid Statistic: " + type);
        if (!playerExists(uuid)) addPlayer(uuid);

        try (PreparedStatement preparedStatement = connection.prepareStatement("""
            INSERT INTO player_stats (uuid, stat_type, value) VALUES (?, ?, ?)
            ON CONFLICT (uuid, stat_type) DO UPDATE SET value = player_stats.value + excluded.value""")) {
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.setString(2, type);
            preparedStatement.setInt(3, value);
            preparedStatement.executeUpdate();
        }
    }

    /**
     * Multiplies a player's specific statistic by a given multiplier.
     * @param uuid       The UUID of the player.
     * @param type       The type of statistic to modify.
     * @param multiplier The multiplier to apply.
     * @throws SQLException If a database access error occurs.
     */
    public void multiplyStatistic(UUID uuid, String type, double multiplier) throws SQLException {
        type = type.trim().toLowerCase();
        if (!validTypes.contains(type)) throw new IllegalArgumentException("Invalid Statistic: " + type);
        setStatistic(uuid, type, (int) (getStatistic(uuid, type) * multiplier));
    }

    /**
     * Retrieves the amount of entries of a given statistic type.
     * @param type The type of statistic to count.
     * @return The number of entries.
     * @throws SQLException If a database access error occurs.
     */
    public int getStatisticCount(String type) throws SQLException {
        type = type.trim().toLowerCase();
        if (!validTypes.contains(type)) throw new IllegalArgumentException("Invalid Statistic: " + type);

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT COUNT(*) AS total FROM player_stats WHERE stat_type = ? AND value > 0")) {
            preparedStatement.setString(1, type);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) return resultSet.getInt("total");
        }
        return 0;
    }

    // Kills Method
    public void addKills(UUID uuid, int kills) throws SQLException {
        addToStatistic(uuid, "kills", kills);
    }

    // Deaths Convenience Method
    public void addDeaths(UUID uuid, int deaths) throws SQLException {
        addToStatistic(uuid, "deaths", deaths);
    }

    public record LeaderboardEntry(UUID player, double value, int position) {}

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

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT uuid, value FROM player_stats WHERE stat_type = ? ORDER BY value DESC LIMIT ?")) {
            preparedStatement.setString(1, type);
            preparedStatement.setInt(2, limit);

            ResultSet resultSet = preparedStatement.executeQuery();
            int position = 1;
            while (resultSet.next()) {
                leaderboard.add(new LeaderboardEntry(
                    UUID.fromString(resultSet.getString("uuid")),
                    resultSet.getInt("value"),
                    position));
                position++;
            }
        }
        return leaderboard;
    }

    /**
     * Retrieves a list of leaderboard entries for a given statistic, ordered descending.
     * Uses SQL LIMIT and OFFSET for pagination.
     * @param type   The statistic type to sort and fetch. Must be in validColumns.
     * @param limit  The number of leaderboard entries to return. Must be >= 1.
     * @param offset The number of top entries to skip (0-based). Must be >= 0.
     * @return A list of LeaderboardEntry objects for the specified range.
     * @throws IllegalArgumentException If an invalid statistic, limit, or offset is provided.
     * @throws SQLException             If a database access error occurs.
     */
    public List<LeaderboardEntry> getTopPlayers(String type, int limit, int offset) throws SQLException {
        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        type = type.trim().toLowerCase();
        if (!validTypes.contains(type)) throw new IllegalArgumentException("Invalid Statistic: " + type);
        if (limit < 1 || offset < 0) throw new IllegalArgumentException("Invalid limit or offset: " + limit + " - " + offset + ". limit must be >= 1, and offset must be >= 0.");

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT uuid, value FROM player_stats WHERE stat_type = ? ORDER BY value DESC LIMIT ? OFFSET ?")) {
            preparedStatement.setString(1, type);
            preparedStatement.setInt(2, limit);
            preparedStatement.setInt(3, offset);
            ResultSet resultSet = preparedStatement.executeQuery();
            int position = offset + 1;
            while (resultSet.next()) {
                leaderboard.add(new LeaderboardEntry(
                    UUID.fromString(resultSet.getString("uuid")),
                    resultSet.getInt("value"),
                    position));
                position++;
            }
        }
        return leaderboard;
    }

    /**
     * Retrieves a list of leaderboard entries for a given statistic from a specific rank range (inclusive).
     * Converts startIndex and endIndex to proper limit and offset, then delegates to getTopPlayers.
     * @param type       The statistic type to sort and fetch (e.g. "kills"). Must be in validColumns.
     * @param startIndex The 1-based index of the first rank to fetch (e.g. 11 for 11th place). Must be >= 1.
     * @param endIndex   The 1-based index of the last rank to fetch (inclusive, e.g. 20 for 20th place). Must be >= startIndex.
     * @return A list of LeaderboardEntry objects representing ranks in the specified range.
     * @throws IllegalArgumentException If indexes are out of range.
     * @throws SQLException             If a database access error occurs.
     */
    public List<LeaderboardEntry> getTopPlayersFromRange(String type, int startIndex, int endIndex) throws SQLException {
        if (startIndex < 1 || endIndex < startIndex) throw new IllegalArgumentException("Invalid range: " + startIndex + "-" + endIndex + ". startIndex must be >= 1 and endIndex must be >= startIndex.");
        int limit = endIndex - startIndex + 1;
        int offset = startIndex - 1; // zero-based offset for SQL

        return getTopPlayers(type, limit, offset);
    }
}
