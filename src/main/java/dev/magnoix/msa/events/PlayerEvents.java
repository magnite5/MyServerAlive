package dev.magnoix.msa.events;

import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.databases.TitleManager;
import dev.magnoix.msa.messages.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

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
    }
}
