package dev.magnoix.msa.databases;

import dev.magnoix.msa.messages.Msg;
import dev.magnoix.msa.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class TitleManager {

    private final Connection connection;
    private final LuckPerms luckPerms;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public TitleManager(Connection connection) throws SQLException {
        this.connection = connection;
        this.luckPerms = LuckPermsProvider.get();
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS titles (
                    title_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name VARCHAR(50) NOT NULL UNIQUE,
                    prefix VARCHAR(100) NOT NULL
                );"""
            );
            statement.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    active_title INTEGER DEFAULT 1,
                    FOREIGN KEY (active_title) REFERENCES titles(title_id) ON DELETE CASCADE ON UPDATE CASCADE
                );"""
            );
            statement.execute("""
                CREATE TABLE IF NOT EXISTS title_relations (
                    prerequisite_id INTEGER,
                    successor_id INTEGER,
                    PRIMARY KEY (prerequisite_id, successor_id),
                    FOREIGN KEY (prerequisite_id) REFERENCES titles(title_id) ON DELETE CASCADE ON UPDATE CASCADE,
                    FOREIGN KEY (successor_id) REFERENCES titles(title_id) ON DELETE CASCADE ON UPDATE CASCADE
                );"""
            );
            statement.execute("""
                CREATE TABLE IF NOT EXISTS player_titles (
                    uuid VARCHAR(36),
                    title_id INTEGER,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (uuid, title_id),
                    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE ON UPDATE CASCADE,
                    FOREIGN KEY (title_id) REFERENCES titles(title_id) ON DELETE CASCADE ON UPDATE CASCADE
                );"""
            );
            confirmDefaultTitle();
        }
    }

    private void updateLuckPermsPrefix(UUID uuid, String prefix) {
        User user =  luckPerms.getUserManager().getUser(uuid);
        if (user == null) {
            luckPerms.getUserManager().loadUser(uuid).thenAcceptAsync(thisUser -> {
                updateLuckPermsPrefix(thisUser, prefix);
                luckPerms.getUserManager().saveUser(thisUser);
            });
            return;
        }
        updateLuckPermsPrefix(user, prefix);
        luckPerms.getUserManager().saveUser(user);
    }
    private void updateLuckPermsPrefix(UUID uuid, title title) {
        updateLuckPermsPrefix(uuid, title.prefix);
    }
    private void updateLuckPermsPrefix(User user, String prefix) {
        String legacyPrefix = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(mm.deserialize(prefix));
        user.data().clear(node -> node instanceof PrefixNode);
        PrefixNode prefixNode = PrefixNode.builder(legacyPrefix, 10).build();
        user.data().add(prefixNode);
    }
    private void updateLuckPermsPrefix(User user, Component prefix) {
        String legacyPrefix = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(prefix);
        user.data().clear(node -> node instanceof PrefixNode);
        PrefixNode prefixNode = PrefixNode.builder(legacyPrefix, 10).build();
        user.data().add(prefixNode);
    }

    public void syncLuckPermsPrefix(UUID uuid) throws SQLException {
        title activeTitle = getActiveTitle(uuid);
        String activePrefix = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(mm.deserialize(activeTitle.prefix));

        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) user = luckPerms.getUserManager().loadUser(uuid).join();

        String currentPrefix = user.getCachedData().getMetaData().getPrefix();
        if (!activePrefix.equals(currentPrefix)) {
            user.data().clear(node -> node instanceof PrefixNode);
            PrefixNode prefixNode = PrefixNode.builder(activePrefix, 10).build();
            user.data().add(prefixNode);
            luckPerms.getUserManager().saveUser(user);
        }
    }

    public void handlePlayerJoin(PlayerJoinEvent event, JavaPlugin plugin) {
        UUID uuid = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                title active = getActiveTitle(uuid);
                if (active == null) setActivePrefix(uuid, 1);
                if (active != null) updateLuckPermsPrefix(uuid, active);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /*
    TODO:
        - Create methods to:
            - Get relation count / depth
            - Get players who have a title
            - (Optional) Give / revoke all titles to / from a player
            - (Optional) Batch Operation support
     */

    public static class DuplicateTitleException extends Exception {
        public DuplicateTitleException(String name) {
            super("A title with the name '" + name + "' already exists.");
        }
    }

    public record title(int id, String name, String prefix) {}

    // --- Title Management ---

    public void confirmDefaultTitle() {
        try {
            title title = getTitleFromName("default");
            if (title == null) {
                title newDefault = createTitle("default", "default ");
                Msg.log(Level.WARNING, "Created a new default title with ID " + newDefault.id());
            } else {
                Msg.log("A default title exists with the ID " + title.id);
            }
        } catch (Exception e) {
            Msg.log(Level.SEVERE, "An error occurred while confirming the default title: " + e.getMessage());
        }
    }

    /**
     * Creates a new title.
     * @param name The name of the new title.
     * @param prefix The MiniMessage prefix of the new title.
     * @return The newly created title record.
     * @throws SQLException If a database access error occurs.
     */
    public title createTitle(String name, String prefix) throws SQLException, DuplicateTitleException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO titles (name, prefix) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, prefix);
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int titleId = generatedKeys.getInt(1);
                    return new title(titleId, name, prefix);
                } else {
                    throw new SQLException("Creating title failed, no ID obtained.");
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) { throw new DuplicateTitleException(name); }
    }

    /**
     * Creates a new title and establishes relations to other titles.
     * @param name The name of the new title.
     * @param prefix The MiniMessage prefix of the new title.
     * @param prerequisiteIds List of title IDs that are prerequisites for this new title.
     * @param successorIds List of title IDs that this new title is a prerequisite for.
     * @return The newly created title record.
     * @throws SQLException If a database access error occurs.
     */
    public title createTitle(String name, String prefix, List<Integer> prerequisiteIds, List<Integer> successorIds) throws SQLException, DuplicateTitleException {
        title newTitle = createTitle(name, prefix);
        int newTitleId = newTitle.id();

        if (prerequisiteIds != null) {
            for (int prerequisiteId : prerequisiteIds) {
                addRelation(prerequisiteId, newTitleId);
            }
        }
        if (successorIds != null) {
            for (int successorId : successorIds) {
                addRelation(newTitleId, successorId);
            }
        }
        return newTitle;
    }

    /**
     * Retrieves a title record by its unique ID.
     * @param id The ID of the title to retrieve.
     * @return The title record, or null if not found.
     * @throws SQLException If a database access error occurs.
     */
    public title getTitle(int id) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT name, prefix FROM titles WHERE title_id = ?")) {
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return new title(id, resultSet.getString("name"), resultSet.getString("prefix"));
            } else {
                return null;
            }
        }
    }
    public Component getFormattedPrefix(title title) throws SQLException {
        return TextUtils.parseMixedFormatting(title.prefix);
    }
    public Component getFormattedPrefix(int titleId) throws SQLException {
        title title = getTitle(titleId);
        return TextUtils.parseMixedFormatting(title.prefix);
    }

    /**
     * Retrieves a title record by its name. If there are multiple of the same name, it will return the first occurrence.
     * @param name The name of the title to retrieve.
     * @return The first matching title record, or null if not found.
     * @throws SQLException If a database access error occurs.
     */
    public title getTitleFromName(String name) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT title_id, prefix FROM titles WHERE name = ?")) {
            preparedStatement.setString(1, name);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return new title(resultSet.getInt("title_id"), name, resultSet.getString("prefix"));
                } else {
                return null;
            }
        }
    }

    /**
     * Retrieves a list of all title records.
     * @return A list of all title records.
     * @throws SQLException If a database access error occurs.
     */
    public List<title> getAllTitles() throws SQLException {
        List<title> titles = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM titles")) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    int titleId = resultSet.getInt("title_id");
                    String name = resultSet.getString("name");
                    String prefix = resultSet.getString("prefix");
                    titles.add(new title(titleId, name, prefix));
                }
            }
        }
        return titles;
    }

    public boolean getTitleExists(String name) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT name FROM titles WHERE name = ?")) {
            preparedStatement.setString(1, name);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        }
    }

    /**
     * Retrieves a list of all title names.
     * @return A list of all title names.
     * @throws SQLException If a database access error occurs.
     */
    public List<String> getAllTitleNames() throws SQLException {
        List<String> names = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT name FROM titles")) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) names.add(resultSet.getString("name"));
        }
        return names;
    }

    /**
     * Deletes a title.
     * @param titleId The id of the title to delete.
     * @return True if the title was deleted, false otherwise.
     * @throws SQLException If a database access error occurs.
     */
    public boolean deleteTitle(int titleId) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM titles WHERE title_id = ?")) {
            preparedStatement.setInt(1, titleId);
            int affectedRows = preparedStatement.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Modifies an existing title by updating name and prefix
     * @param titleId The ID of the title to modify.
     * @param name The new name of the title.
     * @param prefix The new prefix of the title.
     * @return The new title.
     * @throws SQLException If a database access error occurs.
     */
    public title modifyTitle(int titleId, String name, String prefix) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE titles SET name = ?, prefix = ? WHERE title_id = ?")) {
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, prefix);
            preparedStatement.setInt(3, titleId);
            preparedStatement.executeUpdate();
            return getTitle(titleId);
        }
    }

    public title setTitleName(int titleId, String name) throws SQLException {
        title title = getTitle(titleId);
        if (title == null) throw new SQLException("Title with ID " + titleId + " does not exist.");
        return modifyTitle(titleId, name, title.prefix);
    }

    public title setTitlePrefix(int titleId, String prefix) throws SQLException {
        title title = getTitle(titleId);
        if (title == null) { throw new SQLException("Title with ID " + titleId + " does not exist."); }
        return modifyTitle(titleId, title.name, prefix);
    }

    // --- Player Title Management ---

    /**
     * Grants a title to a player.
     * @param uuid The UUID of the player.
     * @param titleId The ID of the title to grant.
     * @param setAsActive Whether to set the title as the player's active title.
     * @throws SQLException If a database access error occurs.
     */
    public void giveTitle(UUID uuid, int titleId, boolean setAsActive) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT OR IGNORE INTO players (uuid) VALUES (?);")) {
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.executeUpdate();
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT OR IGNORE INTO player_titles (uuid, title_id) VALUES (?, ?);")) {
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.setInt(2, titleId);
            preparedStatement.executeUpdate();
        }
        if (setAsActive) {
            setActivePrefix(uuid, titleId);
        }
    }

    /**
     * Grants a title to a player without setting it as active.
     * @param uuid The UUID of the player.
     * @param titleId The ID of the title to grant.
     * @throws SQLException If a database access error occurs.
     */
    public void giveTitle(UUID uuid, int titleId) throws SQLException {
        giveTitle(uuid, titleId, false);
    }

    /**
     * Removes a title from a player.
     * @param uuid The UUID of the player.
     * @param titleId The ID of the title to revoke.
     * @throws SQLException If a database access error occurs.
     */
    public void revokeTitle(UUID uuid, int titleId) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM player_titles WHERE uuid = ? AND title_id = ?")) {
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.setInt(2, titleId);
            preparedStatement.executeUpdate();
        }
    }

    /**
     * Retrieves a list of all title records a player has unlocked.
     * @param uuid The UUID of the player.
     * @return A list of title records.
     * @throws SQLException If a database access error occurs.
     */
    public List<title> getTitles(UUID uuid) throws SQLException {
        List<title> titles = new ArrayList<>();
        String sql = """
            SELECT t.title_id, t.name, t.prefix
            FROM player_titles pt
            JOIN titles t ON pt.title_id = t.title_id
            WHERE pt.uuid = ?
        """;
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, uuid.toString());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    int titleId = resultSet.getInt("title_id");
                    String name = resultSet.getString("name");
                    String prefix = resultSet.getString("prefix");
                    titles.add(new title(titleId, name, prefix));
                }
            }
        }
        return titles;
    }

    /**
     * Counts the number of titles a player has.
     * @param uuid The UUID of the player.
     * @return The number of titles
     * @throws SQLException If a database access error occurs
     */
    public int getTitleCount(UUID uuid) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM player_titles WHERE uuid = ?")) {
            preparedStatement.setString(1, uuid.toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) return resultSet.getInt(1);
            return 0;
        }
    }

    /**
     * Checks if a player has a specific title.
     * @param uuid The UUID of the player.
     * @param titleId The ID of the title to check for.
     * @return True if the player has the title, false otherwise.
     * @throws SQLException If a database access error occurs.
     */
    public boolean hasTitle(UUID uuid, int titleId) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM player_titles WHERE uuid = ? AND title_id = ?")) {
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.setInt(2, titleId);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        }
    }

    // --- Active Title Management ---

    /**
     * Sets a player's active title.
     * @param uuid The UUID of the player.
     * @param titleId The ID of the title to set as active.
     * @throws SQLException If a database access error occurs.
     */
    public void setActiveTitle(UUID uuid, int titleId) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE players SET active_title = ? WHERE uuid = ?")) {
            preparedStatement.setInt(1, titleId);
            preparedStatement.setString(2, uuid.toString());
            preparedStatement.executeUpdate();
        }
        if (!hasTitle(uuid, titleId)) giveTitle(uuid, titleId);
    }

    public void setActivePrefix(UUID uuid, int titleId) throws SQLException {
        setActiveTitle(uuid, titleId);
        syncLuckPermsPrefix(uuid);
    }

    /**
     * Retrieves the record of a player's active title.
     * @param uuid The UUID of the player.
     * @return The active title record, or null if not set.
     * @throws SQLException If a database access error occurs.
     */
    public title getActiveTitle(UUID uuid) throws SQLException {
        int titleId = getActiveTitleId(uuid);
        if (titleId == -1) return null;
        return getTitle(titleId);
    }

    /**
     * Gets the active title ID for a specific player.
     * @param uuid The UUID of the player.
     * @return The active title ID, or -1 if no active title is set.
     * @throws SQLException If a database access error occurs.
     */
    public int getActiveTitleId(UUID uuid) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT active_title FROM players WHERE uuid = ?")) {
            preparedStatement.setString(1, uuid.toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) return resultSet.getInt("active_title");
            return -1; // indicates "None Found"
        }
    }

    // --- Title Relationships ---

    /**
     * Adds a prerequisite-successor relationship between two titles.
     * @param prerequisiteId The ID of the prerequisite title.
     * @param successorId The ID of the successor title.
     * @throws SQLException If a database access error occurs.
     */
    public void addRelation(int prerequisiteId, int successorId) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO title_relations (prerequisite_id, successor_id) VALUES (?, ?)")) {
            preparedStatement.setInt(1, prerequisiteId);
            preparedStatement.setInt(2, successorId);
            preparedStatement.executeUpdate();
        }
    }

    /**
     * Removes a relationship between two titles.
     * @param prerequisiteId The ID of the prerequisite title.
     * @param successorId The ID of the successor title.
     * @throws SQLException If a database access error occurs.
     */
    public void removeRelation(int prerequisiteId, int successorId) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM title_relations WHERE prerequisite_id = ? AND successor_id = ?")) {
            preparedStatement.setInt(1, prerequisiteId);
            preparedStatement.setInt(2, successorId);
            preparedStatement.executeUpdate();
        }
    }

    /**
     * Retrieves a list of titles that are prerequisites for a given title.
     * @param successorId The ID of the title for which to find prerequisites.
     * @return A list of prerequisite titles.
     * @throws SQLException If a database access error occurs.
     */
    public List<title> getPrerequisiteTitles(int successorId) throws SQLException {
        List<title> prerequisiteTitles = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT prerequisite_id FROM title_relations WHERE successor_id = ?")) {
            preparedStatement.setInt(1, successorId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int prerequisiteId = resultSet.getInt("prerequisite_id");
                prerequisiteTitles.add(getTitle(prerequisiteId));
            }
        }
        return prerequisiteTitles;
    }

    /**
     * Retrieves a list of titles that can be unlocked from a given prerequisite title.
     * @param prerequisiteId The ID of the prerequisite title.
     * @return A list of successor titles.
     * @throws SQLException If a database access error occurs.
     */
    public List<title> getSuccessorTitles(int prerequisiteId) throws SQLException {
        List<title> successorTitles = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT successor_id FROM title_relations WHERE prerequisite_id = ?")) {
            preparedStatement.setInt(1, prerequisiteId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int successorId = resultSet.getInt("successor_id");
                successorTitles.add(getTitle(successorId));
            }
        }
        return successorTitles;
    }

    /**
     * Checks if a relationship exists between two titles.
     * @param prerequisiteId The ID of the prerequisite title.
     * @param successorId The ID of the successor title.
     * @return True if a relationship exists, false otherwise.
     * @throws SQLException If a database access error occurs.
     */
    public boolean hasRelation(int prerequisiteId, int successorId) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM title_relations WHERE prerequisite_id = ? AND successor_id = ?")) {
            preparedStatement.setInt(1, prerequisiteId);
            preparedStatement.setInt(2, successorId);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        }
    }
}
