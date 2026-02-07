package dev.magnoix.msa.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.messages.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class SpawnCommand {
    public LiteralCommandNode<CommandSourceStack> create(JavaPlugin plugin) {
        String spawn = plugin.getConfig().getString("settings.spawn");
        plugin.getConfig().options().copyDefaults(true);

        Location spawnLocation;
        if (spawn.equals("default")) spawnLocation = Bukkit.getWorlds().getFirst().getSpawnLocation();
        else spawnLocation = parseLocation(spawn);

        if (spawnLocation == null) {
            Msg.log(Level.WARNING, "<red>Invalid spawn location: " + spawn);
            spawnLocation = Bukkit.getWorlds().getFirst().getSpawnLocation();
        }
        Location finalSpawnLocation = spawnLocation;
        return Commands.literal("spawn")
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender.hasPermission("msa.*") || sender.hasPermission("msa.spawn") || sender.isOp())) {
                        Msg.miniMsg("<red>You do not have permission to use this command.", sender);
                        return 0;
                    }
                    if (sender instanceof Player player) {
                        Msg.miniMsg("<dark_aqua>Teleporting to <gold>spawn<dark_aqua>...", sender);
                        player.teleportAsync(finalSpawnLocation);
                        return 1;
                    } else {
                        sender.sendMessage("Only players can execute this command.");
                        return 0;
                    }
                }).build();
    }

    public static Location parseLocation(String locationString) {
        if (locationString == null || locationString.isEmpty()) return null;

        // Split the string by commas
        String[] parts = locationString.split(",");

        // Basic validation: ensure we have at least world, x, y, and z
        if (parts.length < 4) return null;

        try {
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return null; // World isn't loaded or doesn't exist

            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);

            // Optional: Handle Yaw and Pitch if they exist in the string
            if (parts.length >= 6) {
                float yaw = Float.parseFloat(parts[4]);
                float pitch = Float.parseFloat(parts[5]);
                return new Location(world, x, y, z, yaw, pitch);
            }

            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            // Log error: The coordinate strings weren't valid numbers
            return null;
        }
    }
}
