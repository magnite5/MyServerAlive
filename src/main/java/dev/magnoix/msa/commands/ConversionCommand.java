package dev.magnoix.msa.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.menus.ConversionMenu;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ConversionCommand {
    private final StatisticsManager statisticsManager;
    private final JavaPlugin plugin;

    public ConversionCommand(StatisticsManager statisticsManager, JavaPlugin plugin) {
        this.statisticsManager = statisticsManager;
        this.plugin = plugin;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("cv")
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (sender instanceof Player player) {
                        ConversionMenu menu = new ConversionMenu(statisticsManager, plugin);
                        menu.open(player);
                        return 1;
                    } else {
                        sender.sendMessage(Component.text("Only players can execute this command."));
                        return 0;
                    }
                })
                .build();
    }
}
