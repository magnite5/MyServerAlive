package dev.magnoix.msa.databases;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class TitleManager {
    /*
    End goal: 3 separate tables;
    1. players -> Stores player uuid and the id of the active title
    2. player_titles -> Stores players and title ids. Will contain a single player many times. (maybe also include a timestamp of when the player was given the title?)
    3. titles -> Stores title ids, with their display names (prefixes) and other metadata
     */

    private final Connection connection;

    public TitleManager(Connection connection) throws SQLException {
        this.connection = connection;
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS titles (
                    title_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name VARCHAR(50) NOT NULL,
                    prefix VARCHAR(100) NOT NULL
                );"""
            );
            statement.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    selected_title INTEGER,
                    FOREIGN KEY (selected_title) REFERENCES titles(title_id) ON DELETE CASCADE ON UPDATE CASCADE
                );"""
            );
            statement.execute("""
                CREATE TABLE IF NOT EXISTS player_titles (
                    uuid VARCHAR(36),
                    title_id INTEGER,
                    PRIMARY KEY (uuid, title_id),
                    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE ON UPDATE CASCADE,
                    FOREIGN KEY (title_id) REFERENCES titles(title_id) ON DELETE CASCADE ON UPDATE CASCADE
                );"""
            );
        }
    }
}
