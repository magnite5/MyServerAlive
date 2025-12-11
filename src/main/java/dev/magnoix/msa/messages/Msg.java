package dev.magnoix.msa.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Msg {
    static MiniMessage mm = MiniMessage.miniMessage();
    private static Logger logger;
    private static String pluginName;

    public static void init(JavaPlugin plugin) {
        logger = plugin.getLogger();
        pluginName = plugin.getName();
    }

    public static void log(Level level, String message) {
        if (logger != null) {
            logger.log(level, message);
        } else {
            Bukkit.getConsoleSender().sendMessage(formatConsole(level, message));
        }
    }
    public static void log(String message) {
        log(Level.INFO, message);
    }

    private static String formatConsole(Level level, String message) {
        ChatColor color = switch (level.getName()) {
            case "SEVERE" -> ChatColor.RED;
            case "WARNING" -> ChatColor.YELLOW;
            case "INFO" -> ChatColor.GREEN;
            default -> ChatColor.GRAY;
        };
        return color + "[" + level + "][" + pluginName + "] " + message;
    }

    public static void msg(Component message, Player target) {
        if (target.isConnected()) {
            target.sendMessage(message);
        } else
            log(Level.SEVERE, "Tried to send a message to player " + target + ", but " + target + " is not connected. Skipping.");
    }
    public static void msg(String message, Player target) {
        if (target.isConnected()) {
            target.sendMessage(message);
        } else
            log(Level.SEVERE, "Tried to send a message to player " + target + ", but " + target + " is not connected. Skipping.");
    }
    public static void msg(String message, CommandSender target) {
        target.sendMessage(message);
    }
    public static void miniMsg(String message, Player target) {
        target.sendMessage(mm.deserialize(message));
    }
    public static void miniMsg(String message, CommandSender target) {
        target.sendMessage(mm.deserialize(message));
    }
}
