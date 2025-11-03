package dev.magnoix.msa.utils;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.NotNull;

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
}
