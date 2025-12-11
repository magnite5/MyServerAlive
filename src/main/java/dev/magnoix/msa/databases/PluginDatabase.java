package dev.magnoix.msa.databases;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class PluginDatabase {

    private final Connection connection;
    private final StatisticsManager statisticsManager;
    private final TitleManager titleManager;

    public PluginDatabase(JavaPlugin plugin, PluginConfig pluginConfig, String path) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
        }

        Set<String> loggedTypes = new HashSet<>(pluginConfig.getStringList("statistics.logged-types"));
        statisticsManager = new StatisticsManager(plugin, connection, loggedTypes);
        titleManager = new TitleManager(connection);
    }

    public Connection getConnection() {
        return connection;
    }

    public StatisticsManager getStatisticsManager() {
        return statisticsManager;
    }
    public TitleManager getTitleManager() { return titleManager; }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
