package dev.magnoix.msa.events;

import dev.magnoix.msa.databases.StatisticsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.sql.SQLException;

public record PlayerEvents(StatisticsManager statisticsManager) implements Listener {

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
}
