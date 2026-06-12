package dev.magnoix.msa.utils;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.MSA;
import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.messages.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class StartupUtils {
    private StartupUtils() {}

    public static void registerCommandWithAliases(
        ReloadableRegistrarEvent<@NotNull Commands> commands,
        LiteralCommandNode<CommandSourceStack> node,
        String... aliases
    ) {
        commands.registrar().register(node);
        for (String alias : aliases) {
            commands.registrar().register(Commands.literal(alias).redirect(node).build());
        }
    }

    public static void registerCommandWithAliases(
            ReloadableRegistrarEvent<@NotNull Commands> commands,
            LiteralCommandNode<CommandSourceStack> node,
            Collection<String> aliases
    ) {
        commands.registrar().register(node);
        if (aliases == null) return;
        for (String alias : aliases) {
            commands.registrar().register(Commands.literal(alias).redirect(node).build());
        }
    }

    public static void registerCommandNodes(
            ReloadableRegistrarEvent<@NotNull Commands> commands,
            ArrayList<LiteralCommandNode<CommandSourceStack>> commandNodes
    ) {
        commandNodes.forEach(node -> commands.registrar().register(node));
    }
    public static void registerCommandNodesWithAliases(
            JavaPlugin plugin,
            ReloadableRegistrarEvent<@NotNull Commands> commands,
            Map<CommandUtils.Command, List<String>> commandAliases
    ) {
        commandAliases.forEach(
                (command, aliases) -> {
                    if (aliases == null) commands.registrar().register(command.create(plugin));
                    else registerCommandWithAliases(commands, command.create(plugin), aliases);
                }
        );
    }

    public static void scheduleStatisticsFlush(MSA msa, StatisticsManager statisticsManager, boolean logEachTime) {
        long flushInterval = msa.getConfig().getLong("statistics.write-interval") * 20; // ticks
        if (flushInterval <= 0) {
            Msg.log(Level.WARNING, "Statistics flush interval is set to 0 or an invalid number. Defaulting to 60 seconds.");
            flushInterval = 60 * 20;
        }
        Bukkit.getScheduler().runTaskTimer(msa, () -> {
            try {
                if (logEachTime) Msg.log(Level.INFO, "Flushing statistics cache...");
                statisticsManager.flushCache();
            } catch (SQLException e) {
                Msg.log(Level.SEVERE, "Failed to flush statistics cache: " + e.getMessage());
            }
        }, flushInterval, flushInterval);
    }

    public static void scheduleLeaderboardRebuild(MSA msa, StatisticsManager statisticsManager, boolean logEachTime) {
        long rebuildInterval = msa.getConfig().getLong("leaderboards.rebuild-interval") * 20;
        if (rebuildInterval <= 0) {
            Msg.log(Level.WARNING, "Leaderboard rebuild interval is set to 0 or an invalid number. Defaulting to 240 seconds.");
            rebuildInterval = 240 * 20;
        }
        Bukkit.getScheduler().runTaskTimer(msa, () -> {
            try {
                if (logEachTime) Msg.log(Level.INFO, "Rebuilding leaderboard cache...");
                statisticsManager.rebuildLeaderboardCache();
            } catch (SQLException e) {
                Msg.log(Level.SEVERE, "Failed to rebuild leaderboard cache: " + e.getMessage());
            }
        }, rebuildInterval, rebuildInterval);
    }
}
