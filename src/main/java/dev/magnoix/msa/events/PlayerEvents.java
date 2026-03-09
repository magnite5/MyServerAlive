package dev.magnoix.msa.events;

import dev.magnoix.msa.MSA;
import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.databases.TitleManager;
import dev.magnoix.msa.messages.Msg;
import dev.magnoix.msa.utils.UpdateUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URL;
import java.sql.SQLException;
import java.util.logging.Level;

public record PlayerEvents(StatisticsManager statisticsManager, TitleManager titleManager, JavaPlugin plugin) implements Listener {

    @EventHandler
    public void playerDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        Player k = p.getKiller();
        try {
            statisticsManager.addDeaths(p.getUniqueId(), 1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (k != null) try {
            statisticsManager.addKills(k.getUniqueId(), 1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        titleManager.handlePlayerJoin(event, plugin);

        Player player =  event.getPlayer();
        if (!player.hasPlayedBefore()) {
            try {
                statisticsManager.addPlayer(player.getUniqueId());
            } catch (SQLException e) {
                Msg.log(Level.SEVERE, "Failed to add player to statistics manager: " + e.getMessage());
            }
        }

        if (player.hasPermission("msa.updates") || player.isOp()) {
            if (UpdateUtils.latestRelease == null) { return; }
            String latestReleaseUrl = UpdateUtils.latestRelease.getDownloadUrl();
            if (latestReleaseUrl == null) { return; }

            String latestReleaseName = UpdateUtils.latestRelease.getTagName().replace("v", "");
            String currentReleaseName = MSA.getInstance().getPluginMeta().getVersion();

            if (!latestReleaseName.equalsIgnoreCase(currentReleaseName)) {
                Msg.miniMsg("<gold>A new version of MSA is available. <aqua>" + currentReleaseName + "<dark_aqua> -> <gold>" + latestReleaseName, player);
                Msg.miniMsg("<gold>Download it <aqua><u><click:open_url:" + latestReleaseUrl + ">here</click></u>", player);
            }
        }
    }
}
