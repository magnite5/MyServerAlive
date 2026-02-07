package dev.magnoix.msa.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.messages.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class SpawnCommand {
    public LiteralCommandNode<CommandSourceStack> create(JavaPlugin plugin) {
        String spawnWorld = plugin.getConfig().getString("settings.spawn-world");

        return Commands.literal("spawn")
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender.hasPermission("msa.*") || sender.hasPermission("msa.spawn") || sender.isOp())) {
                        Msg.miniMsg("<red>You do not have permission to use this command.", sender);
                        return 0;
                    }
                    if (sender instanceof Player player) {
                        Msg.miniMsg("<dark_aqua>Teleporting to <gold>spawn<dark_aqua>...", sender);
                        player.teleportAsync(getSpawnLocation(spawnWorld));
                        return 1;
                    } else {
                        sender.sendMessage("Only players can execute this command.");
                        return 0;
                    }
                }).build();
    }

    public static Location getSpawnLocation(String worldName) {
        if (worldName == null || worldName.isEmpty()) worldName = "world";
        return Objects.requireNonNull(Bukkit.getWorld(worldName)).getSpawnLocation();
    }
}
