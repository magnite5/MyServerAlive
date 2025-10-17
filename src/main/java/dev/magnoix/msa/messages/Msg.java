package dev.magnoix.msa.messages;

import dev.magnoix.msa.MSA;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Msg {
    static MiniMessage mm = MiniMessage.miniMessage();
    private static Logger logger;

    public static void init(Logger pluginLogger) {
        logger = pluginLogger;
    }

    public static void log(String message) {
        if (logger != null) {
            logger.info(message);
        } else {
            Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "[LOG]" + message);
        }
    }

    public static void log(Level level, String message) {
        if (logger != null) {
            logger.log(level, message);
        } else {
            Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "[" + level + "] " + message);
        }
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
