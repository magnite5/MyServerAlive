package dev.magnoix.msa.databases;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class PluginDatabase {

    private final Connection connection;
    private final StatisticsManager statisticsManager;
    private final TitleManager titleManager;

    public PluginDatabase(String path) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
        }

        statisticsManager = new StatisticsManager(connection);
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
