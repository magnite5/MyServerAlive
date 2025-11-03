package dev.magnoix.msa.databases;

import dev.magnoix.msa.messages.Msg;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class StatisticsManager { //TODO: Use Long or Double instead of int for statistic values

    private final Connection connection;

    private final Set<String> VALID_COLUMNS = Set.of("kills", "deaths", "wins", "networth");

    public StatisticsManager(Connection connection) throws SQLException {
        this.connection = connection;
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS statistics (
                uuid TEXT PRIMARY KEY,
                kills INTEGER NOT NULL DEFAULT 0,
                deaths INTEGER NOT NULL DEFAULT 0,
                wins INTEGER NOT NULL DEFAULT 0,
                networth INTEGER NOT NULL DEFAULT 0)
            """);
        }
    }

    /**
     * Returns a set of valid statistic column names.
     * @return A set of strings representing valid statistic names.
     */
    public Set<String> getValidColumns() { return VALID_COLUMNS; }

    /**
     * Adds a new player to the statistics database.
     * @param uuid The UUID of the player to add.
     * @throws SQLException If a database access error occurs.
     */
    public void addPlayer(UUID uuid) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO statistics (uuid) VALUES (?)")) {
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.executeUpdate();
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
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM statistics WHERE uuid = ?")) {
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
        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE statistics SET kills = 0, deaths = 0, wins = 0, networth = 0 WHERE uuid = ?")) {
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.executeUpdate();
            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
            Msg.log(Level.WARNING, "Player " + (playerName != null ? playerName : uuid.toString()) + "'s statistics have been reset.");
        }
    }

    /**
     * Checks if a player exists in the statistics database.
     * @param uuid The UUID of the player to check.
     * @return True if the player exists, false otherwise.
     * @throws SQLException If a database access error occurs.
     */
    public boolean playerExists(UUID uuid) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT uuid FROM statistics WHERE uuid = ?")) {
            preparedStatement.setString(1, uuid.toString());
            return preparedStatement.executeQuery().next();
        }
    }

    /**
     * Gets a specific statistic for a player.
     * @param uuid      The UUID of the player.
     * @param statistic The name of the statistic to retrieve.
     * @return The value of the statistic.
     * @throws SQLException If a database access error occurs.
     * @throws IllegalArgumentException If the statistic name is invalid.
     */
    public int getStatistic(UUID uuid, String statistic) throws SQLException {
        if (!VALID_COLUMNS.contains(statistic.trim().toLowerCase())) throw new IllegalArgumentException("Invalid Statistic: " + statistic);

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT " + statistic + " FROM statistics WHERE uuid = ?")) {
            preparedStatement.setString(1, uuid.toString());
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) return resultSet.getInt(statistic);
            else return 0;
        }
    }

    /**
     * Sets a specific statistic for a player to a given value.
     * @param uuid      The UUID of the player.
     * @param statistic The name of the statistic to set.
     * @param value     The new value for the statistic.
     * @throws SQLException If a database access error occurs.
     * @throws IllegalArgumentException If the statistic name is invalid.
     */
    public void setStatistic(UUID uuid, String statistic, int value) throws SQLException {
        if (!VALID_COLUMNS.contains(statistic.trim().toLowerCase())) throw new IllegalArgumentException("Invalid Statistic: " + statistic);
        if (!playerExists(uuid)) addPlayer(uuid);

        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE statistics SET " + statistic + " = ? WHERE uuid = ?")) {
            preparedStatement.setInt(1, value);
            preparedStatement.setString(2, uuid.toString());
            preparedStatement.executeUpdate();
        }
    }

    /**
     * Adds a value to a player's specific statistic.
     * @param uuid      The UUID of the player.
     * @param statistic The name of the statistic to modify.
     * @param value     The value to add.
     * @throws SQLException If a database access error occurs.
     */
    public void addStatistic(UUID uuid, String statistic, int value) throws SQLException {
        if (!VALID_COLUMNS.contains(statistic.trim().toLowerCase())) throw new IllegalArgumentException("Invalid Statistic: " + statistic);

        setStatistic(uuid, statistic, getStatistic(uuid, statistic) + value);
    }

    /**
     * Multiplies a player's specific statistic by a given multiplier.
     * @param uuid       The UUID of the player.
     * @param statistic  The name of the statistic to modify.
     * @param multiplier The multiplier to apply.
     * @throws SQLException If a database access error occurs.
     */
    public void multiplyStatistic(UUID uuid, String statistic, double multiplier) throws SQLException {
        if (!VALID_COLUMNS.contains(statistic.trim().toLowerCase())) throw new IllegalArgumentException("Invalid Statistic: " + statistic);

        setStatistic(uuid, statistic, (int) (getStatistic(uuid, statistic) * multiplier));
    }

    public int getStatisticCount(String statistic) throws SQLException {
        if (!VALID_COLUMNS.contains(statistic.trim().toLowerCase())) throw new IllegalArgumentException("Invalid Statistic: " + statistic);

        String sql = "SELECT COUNT(*) AS total FROM statistics WHERE " + statistic + " > 0";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) return resultSet.getInt("total");
        }
        return 0;
    }

    // Kills Methods
    public int getKills(UUID uuid) throws SQLException {
        return getStatistic(uuid, "kills");
    }

    public void setKills(UUID uuid, int kills) throws SQLException {
        setStatistic(uuid, "kills", kills);
    }

    public void addKills(UUID uuid, int kills) throws SQLException {
        addStatistic(uuid, "kills", kills);
    }

    // Deaths Convenience Methods
    public int getDeaths(UUID uuid) throws SQLException {
        return getStatistic(uuid, "deaths");
    }

    public void setDeaths(UUID uuid, int deaths) throws SQLException {
        setStatistic(uuid, "deaths", deaths);
    }

    public void addDeaths(UUID uuid, int deaths) throws SQLException {
        addStatistic(uuid, "deaths", deaths);
    }

    // Wins Convenience Methods
    public int getWins(UUID uuid) throws SQLException {
        return getStatistic(uuid, "wins");
    }

    public void setWins(UUID uuid, int wins) throws SQLException {
        setStatistic(uuid, "wins", wins);
    }

    public void addWins(UUID uuid, int wins) throws SQLException {
        addStatistic(uuid, "wins", wins);
    }

    // Networth Convenience Methods
    public int getNetworth(UUID uuid) throws SQLException {
        return getStatistic(uuid, "networth");
    }

    public void setNetworth(UUID uuid, int networth) throws SQLException {
        setStatistic(uuid, "networth", networth);
    }

    public void addNetworth(UUID uuid, int networth) throws SQLException {
        addStatistic(uuid, "networth", networth);
    }

    public void multiplyNetworth(UUID uuid, double multiplier) throws SQLException {
        multiplyStatistic(uuid, "networth", multiplier);
    }

    public record LeaderboardEntry(UUID player, double value, int position) {} //TODO: Add support for leaderboard position

    /**
     * Gets the top players for a given statistic.
     * @param statistic The name of the statistic.
     * @param limit     The maximum number of players to return.
     * @return A list of LeaderboardEntry records, sorted by the statistic.
     * @throws SQLException If a database access error occurs.
     */
    public List<LeaderboardEntry> getTopPlayers(String statistic, int limit) throws SQLException {
        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        if (!VALID_COLUMNS.contains(statistic.trim().toLowerCase())) return leaderboard;

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT uuid, " + statistic + " FROM statistics ORDER BY " + statistic + " DESC LIMIT ?")) {
            preparedStatement.setInt(1, limit);

            ResultSet resultSet = preparedStatement.executeQuery();
            int position = 1;
            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                double value = resultSet.getInt(statistic);
                leaderboard.add(new LeaderboardEntry(uuid, value, position));
                position++;
            }
        }
        return leaderboard;
    }

    /**
     * Retrieves a list of leaderboard entries for a given statistic, ordered descending.
     * Uses SQL LIMIT and OFFSET for pagination.
     *
     * @param statistic The statistic column to sort and fetch (e.g. "kills"). Must be in VALID_COLUMNS.
     * @param limit     The number of leaderboard entries to return. Must be >= 1.
     * @param offset    The number of top entries to skip (0-based). Must be >= 0.
     * @return A list of LeaderboardEntry objects for the specified range.
     * @throws IllegalArgumentException If an invalid statistic, limit, or offset is provided.
     * @throws SQLException             If a database access error occurs.
     */
    public List<LeaderboardEntry> getTopPlayers(String statistic, int limit, int offset) throws SQLException {
        if (!VALID_COLUMNS.contains(statistic.trim().toLowerCase())) throw new IllegalArgumentException("Invalid Statistic: " + statistic);
        if (limit < 1 || offset < 0) throw new IllegalArgumentException("Invalid limit or offset: " + limit + " - " + offset + ". limit must be >= 1 and offset must be >= 0.");

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT uuid, " + statistic + " FROM statistics ORDER BY " + statistic + " DESC LIMIT ? OFFSET ?")) {
            preparedStatement.setInt(1, limit);
            preparedStatement.setInt(2, offset);
            ResultSet resultSet = preparedStatement.executeQuery();
            List<LeaderboardEntry> leaderboard = new ArrayList<>();
            int position = offset + 1;
            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                double value = resultSet.getInt(statistic);
                leaderboard.add(new LeaderboardEntry(uuid, value, position));
                position++;
            }
            return leaderboard;
        }
    }

    /**
     * Retrieves a list of leaderboard entries for a given statistic from a specific rank range (inclusive).
     * Converts startIndex and endIndex to proper limit and offset, then delegates to getTopPlayers.
     *
     * @param statistic  The statistic column to sort and fetch (e.g. "kills"). Must be in VALID_COLUMNS.
     * @param startIndex The 1-based index of the first rank to fetch (e.g. 11 for 11th place). Must be >= 1.
     * @param endIndex   The 1-based index of the last rank to fetch (inclusive, e.g. 20 for 20th place). Must be >= startIndex.
     * @return A list of LeaderboardEntry objects representing ranks in the specified range.
     * @throws IllegalArgumentException If indexes are out of range.
     * @throws SQLException             If a database access error occurs.
     */
    public List<LeaderboardEntry> getTopPlayersFromRange(String statistic, int startIndex, int endIndex) throws SQLException {
        if (startIndex < 1 || endIndex < startIndex) throw new IllegalArgumentException("Invalid range: " + startIndex + "-" + endIndex + ". startIndex must be >= 1 and endIndex must be >= startIndex.");
        int limit = endIndex - startIndex + 1;
        int offset = startIndex - 1; // zero-based offset for SQL

        return getTopPlayers(statistic, limit, offset);
    }
}
