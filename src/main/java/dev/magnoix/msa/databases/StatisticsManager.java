package dev.magnoix.msa.databases;

import dev.magnoix.msa.messages.Msg;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class StatisticsManager {

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
    // Class Methods
    public Set<String> getValidColumns() { return VALID_COLUMNS; }
    // General Player Methods
    public void addPlayer(UUID uuid) throws SQLException {

        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO statistics (uuid) VALUES (?)")) {
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.executeUpdate();
            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
            Msg.log(Level.WARNING, "Player " + (playerName != null ? playerName : uuid.toString()) + " has been added to the statistics database.");
        }
    }
    public void removePlayer(UUID uuid) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM statistics WHERE uuid = ?")) {
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.executeUpdate();
            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
            Msg.log(Level.WARNING, "Player " + (playerName != null ? playerName : uuid.toString()) + " has been removed from the statistics database.");
        }
    }
    public void resetPlayer(UUID uuid) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE statistics SET kills = 0, deaths = 0, wins = 0, networth = 0 WHERE uuid = ?")) {
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.executeUpdate();
            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
            Msg.log(Level.WARNING, "Player " + (playerName != null ? playerName : uuid.toString()) + "'s statistics have been reset.");
        }
    }
    public boolean playerExists(UUID uuid) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT uuid FROM statistics WHERE uuid = ?")) {
            preparedStatement.setString(1, uuid.toString());
            return preparedStatement.executeQuery().next();
        }
    }

    public int getStatistic(UUID uuid, String statistic) throws SQLException {
        if (!VALID_COLUMNS.contains(statistic.trim().toLowerCase())) throw new IllegalArgumentException("Invalid Statistic: " + statistic);

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT " + statistic + " FROM statistics WHERE uuid = ?")) {
            preparedStatement.setString(1, uuid.toString());
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) return resultSet.getInt(statistic);
            else return 0;
        }
    }
    public void setStatistic(UUID uuid, String statistic, int value) throws SQLException {
        if (!VALID_COLUMNS.contains(statistic.trim().toLowerCase())) throw new IllegalArgumentException("Invalid Statistic: " + statistic);
        if (!playerExists(uuid)) addPlayer(uuid);

        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE statistics SET " + statistic + " = ? WHERE uuid = ?")) {
            preparedStatement.setInt(1, value);
            preparedStatement.setString(2, uuid.toString());
            preparedStatement.executeUpdate();
        }
    }
    public void addStatistic(UUID uuid, String statistic, int value) throws SQLException {
        if (!VALID_COLUMNS.contains(statistic.trim().toLowerCase())) throw new IllegalArgumentException("Invalid Statistic: " + statistic);

        setStatistic(uuid, statistic, getStatistic(uuid, statistic) + value);
    }
    public void multiplyStatistic(UUID uuid, String statistic, double multiplier) throws SQLException {
        if (!VALID_COLUMNS.contains(statistic.trim().toLowerCase())) throw new IllegalArgumentException("Invalid Statistic: " + statistic);

        setStatistic(uuid, statistic, (int) (getStatistic(uuid, statistic) * multiplier));
    }

    // Kills Methods
    public int getKills(UUID uuid) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT kills FROM statistics WHERE uuid = ?")) {
            preparedStatement.setString(1, uuid.toString());
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) return resultSet.getInt("kills");
            else return 0;
        }
    }
    public void setKills(UUID uuid, int kills) throws SQLException {
        if (!playerExists(uuid)) addPlayer(uuid);

        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE statistics SET kills = ? WHERE uuid = ?")) {
            preparedStatement.setInt(1, kills);
            preparedStatement.setString(2, uuid.toString());
            preparedStatement.executeUpdate();
        }
    }
    public void addKills(UUID uuid, int kills) throws SQLException {
        setKills(uuid, getKills(uuid) + kills);
    }

    // Deaths Methods
    public int getDeaths(UUID uuid) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT deaths FROM statistics WHERE uuid = ?")) {
            preparedStatement.setString(1, uuid.toString());
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) return resultSet.getInt("deaths");
            else return 0;
        }
    }
    public void setDeaths(UUID uuid, int deaths) throws SQLException {
        if (!playerExists(uuid)) addPlayer(uuid);

        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE statistics SET deaths = ? WHERE uuid = ?")) {
            preparedStatement.setInt(1, deaths);
            preparedStatement.setString(2, uuid.toString());
            preparedStatement.executeUpdate();
        }
    }
    public void addDeaths(UUID uuid, int deaths) throws SQLException {
        setDeaths(uuid, getDeaths(uuid) + deaths);
    }

    // Wins Methods
    public int getWins(UUID uuid) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT wins FROM statistics WHERE uuid = ?")) {
            preparedStatement.setString(1, uuid.toString());
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) return resultSet.getInt("wins");
            else return 0;
        }
    }
    public void setWins(UUID uuid, int wins) throws SQLException {
        if (!playerExists(uuid)) addPlayer(uuid);

        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE statistics SET wins = ? WHERE uuid = ?")) {
            preparedStatement.setInt(1, wins);
            preparedStatement.setString(2, uuid.toString());
            preparedStatement.executeUpdate();
        }
    }
    public void addWins(UUID uuid, int wins) throws SQLException {
        setWins(uuid, getWins(uuid) + wins);
    }

    // Networth Methods
    public int getNetworth(UUID uuid) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT networth FROM statistics WHERE uuid = ?")) {
            preparedStatement.setString(1, uuid.toString());
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) return resultSet.getInt("networth");
            else return 0;
        }
    }
    public void setNetworth(UUID uuid, int networth) throws SQLException {
        if (!playerExists(uuid)) addPlayer(uuid);

        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE statistics SET networth = ? WHERE uuid = ?")) {
            preparedStatement.setInt(1, networth);
            preparedStatement.setString(2, uuid.toString());
            preparedStatement.executeUpdate();
        }
    }
    public void addNetworth(UUID uuid, int networth) throws SQLException {
        setNetworth(uuid, getNetworth(uuid) + networth);
    }
    public void multiplyNetworth(UUID uuid, double multiplier) throws SQLException {
        setNetworth(uuid, (int) (getNetworth(uuid) * multiplier));
    }
}
