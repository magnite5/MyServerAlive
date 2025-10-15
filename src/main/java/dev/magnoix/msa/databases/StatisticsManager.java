package dev.magnoix.msa.databases;

import dev.magnoix.msa.messages.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.sql.*;
import java.util.Map;
import java.util.Set;
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
    public void addPlayer(Player p) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO statistics (uuid) VALUES (?)")) {
            preparedStatement.setString(1, p.getUniqueId().toString());
            preparedStatement.executeUpdate();
            Msg.log(Level.WARNING, "Player " + p.getName() + " has been added to the statistics database.");
        }
    }
    public void removePlayer(Player p) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM statistics WHERE uuid = ?")) {
            preparedStatement.setString(1, p.getUniqueId().toString());
            preparedStatement.executeUpdate();
            Msg.log(Level.WARNING, "Player " + p.getName() + " has been removed from the statistics database.");
        }
    }
    public void resetPlayer(Player p) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE statistics SET kills = 0, deaths = 0, wins = 0, networth = 0 WHERE uuid = ?")) {
            preparedStatement.setString(1, p.getUniqueId().toString());
            preparedStatement.executeUpdate();
            Msg.log(Level.WARNING, "Player " + p.getName() + "'s statistics have been reset.");
        }
    }
    public boolean playerExists(Player p) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT uuid FROM statistics WHERE uuid = ?")) {
            preparedStatement.setString(1, p.getUniqueId().toString());
            return preparedStatement.executeQuery().next();
        }
    }

    public int getStatistic(Player p, String statistic) throws SQLException {
        if (!VALID_COLUMNS.contains(statistic.trim().toLowerCase())) throw new IllegalArgumentException("Invalid Statistic: " + statistic);

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT " + statistic + " FROM statistics WHERE uuid = ?")) {
            preparedStatement.setString(1, p.getUniqueId().toString());
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) return resultSet.getInt(statistic);
            else return 0;
        }
    }
    public void setStatistic(Player p, String statistic, int value) throws SQLException {
        if (!VALID_COLUMNS.contains(statistic.trim().toLowerCase())) throw new IllegalArgumentException("Invalid Statistic: " + statistic);
        if (!playerExists(p)) addPlayer(p);

        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE statistics SET " + statistic + " = ? WHERE uuid = ?")) {
            preparedStatement.setInt(1, value);
            preparedStatement.setString(2, p.getUniqueId().toString());
            preparedStatement.executeUpdate();
        }
    }
    public void addStatistic(Player p, String statistic, int value) throws SQLException {
        if (!VALID_COLUMNS.contains(statistic.trim().toLowerCase())) throw new IllegalArgumentException("Invalid Statistic: " + statistic);

        setStatistic(p, statistic, getStatistic(p, statistic) + value);
    }
    public void multiplyStatistic(Player p, String statistic, double multiplier) throws SQLException {
        if (!VALID_COLUMNS.contains(statistic.trim().toLowerCase())) throw new IllegalArgumentException("Invalid Statistic: " + statistic);

        setStatistic(p, statistic, (int) (getStatistic(p, statistic) * multiplier));
    }

    // Kills Methods
    public int getKills(Player p) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT kills FROM statistics WHERE uuid = ?")) {
            preparedStatement.setString(1, p.getUniqueId().toString());
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) return resultSet.getInt("kills");
            else return 0;
        }
    }
    public void setKills(Player p, int kills) throws SQLException {
        if (!playerExists(p)) addPlayer(p);

        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE statistics SET kills = ? WHERE uuid = ?")) {
            preparedStatement.setInt(1, kills);
            preparedStatement.setString(2, p.getUniqueId().toString());
            preparedStatement.executeUpdate();
        }
    }
    public void addKills(Player p, int kills) throws SQLException {
        setKills(p, getKills(p) + kills);
    }

    // Deaths Methods
    public int getDeaths(Player p) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT deaths FROM statistics WHERE uuid = ?")) {
            preparedStatement.setString(1, p.getUniqueId().toString());
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) return resultSet.getInt("deaths");
            else return 0;
        }
    }
    public void setDeaths(Player p, int deaths) throws SQLException {
        if (!playerExists(p)) addPlayer(p);

        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE statistics SET deaths = ? WHERE uuid = ?")) {
            preparedStatement.setInt(1, deaths);
            preparedStatement.setString(2, p.getUniqueId().toString());
            preparedStatement.executeUpdate();
        }
    }
    public void addDeaths(Player p, int deaths) throws SQLException {
        setDeaths(p, getDeaths(p) + deaths);
    }

    // Wins Methods
    public int getWins(Player p) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT wins FROM statistics WHERE uuid = ?")) {
            preparedStatement.setString(1, p.getUniqueId().toString());
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) return resultSet.getInt("wins");
            else return 0;
        }
    }
    public void setWins(Player p, int wins) throws SQLException {
        if (!playerExists(p)) addPlayer(p);

        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE statistics SET wins = ? WHERE uuid = ?")) {
            preparedStatement.setInt(1, wins);
            preparedStatement.setString(2, p.getUniqueId().toString());
            preparedStatement.executeUpdate();
        }
    }
    public void addWins(Player p, int wins) throws SQLException {
        setWins(p, getWins(p) + wins);
    }

    // Networth Methods
    public int getNetworth(Player p) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT networth FROM statistics WHERE uuid = ?")) {
            preparedStatement.setString(1, p.getUniqueId().toString());
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) return resultSet.getInt("networth");
            else return 0;
        }
    }
    public void setNetworth(Player p, int networth) throws SQLException {
        if (!playerExists(p)) addPlayer(p);

        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE statistics SET networth = ? WHERE uuid = ?")) {
            preparedStatement.setInt(1, networth);
            preparedStatement.setString(2, p.getUniqueId().toString());
            preparedStatement.executeUpdate();
        }
    }
    public void addNetworth(Player p, int networth) throws SQLException {
        setNetworth(p, getNetworth(p) + networth);
    }
    public void multiplyNetworth(Player p, double multiplier) throws SQLException {
        setNetworth(p, (int) (getNetworth(p) * multiplier));
    }
}
