package dev.magnoix.msa.hooks;

import dev.magnoix.msa.MSA;
import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.messages.Msg;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.logging.Level;

public class PAPIExpansion extends PlaceholderExpansion {
    private final MSA plugin;
    private final StatisticsManager statisticsManager;

    public PAPIExpansion(MSA plugin, StatisticsManager statisticsManager) {
        this.plugin = plugin;
        this.statisticsManager = statisticsManager;
    }
    @Override
    @NotNull
    public String getIdentifier() {
        return "msa";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "magnoix";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        if (params.startsWith("stats")) { // stats_ (_ -> index 5)
            String statName = params.substring(6);
            if (statName.isEmpty()) return "";
            if (statName.equals("nw")) statName = "networth";
            try {
                return String.valueOf(statisticsManager.getStatistic(player.getUniqueId(), statName));
            } catch (SQLException e) {
                Msg.log(Level.SEVERE, "Failed PAPI SQL request for " + player.getName() + ": " + e.getMessage());
                statisticsManager.log("Database error during PAPI request for " + player.getName() + ": " + e.getMessage());
                return "0";
            }
        } else if (params.startsWith("lb") || params.startsWith("leaderboard")) {
            return "WIP";
        } else return "";
    }
}
