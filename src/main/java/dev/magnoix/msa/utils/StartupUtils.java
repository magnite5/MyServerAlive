package dev.magnoix.msa.utils;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
}
