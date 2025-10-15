package dev.magnoix.msa.databases;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PluginDatabase {

    private final Connection connection;
    private final StatisticsManager statisticsManager;

    public PluginDatabase(String path) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        statisticsManager = new StatisticsManager(connection);
    }

    public Connection getConnection() {
        return connection;
    }

    public StatisticsManager getStatisticsManager() {
        return statisticsManager;
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
