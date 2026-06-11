package dev.magnoix.msa.utils;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.MSA;
import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.messages.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;

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

    public static void registerCommandNodes(
            ReloadableRegistrarEvent<@NotNull Commands> commands,
            ArrayList<LiteralCommandNode<CommandSourceStack>> commandNodes
    ) {
        commandNodes.forEach(node -> commands.registrar().register(node));
    }

    public static void scheduleStatisticsFlush(MSA msa, StatisticsManager statisticsManager) {
        long flushInterval = msa.getConfig().getLong("statistics.write-interval") * 20; // ticks

        Bukkit.getScheduler().runTaskTimer(msa, () -> {
            try {
                Msg.log(Level.INFO, "Flushing statistics cache...");
                statisticsManager.flushCache();
            } catch (SQLException e) {
                Msg.log(Level.SEVERE, "Failed to flush statstics cache: " + e.getMessage());
            }
        }, flushInterval, flushInterval);
    }
}
