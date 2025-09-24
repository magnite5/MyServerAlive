package dev.magnoix.msa.messages;

import dev.magnoix.msa.MSA;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Msg {
    private static MailManager mailManager;
    private static Logger logger;

    public static void init(MailManager manager, Logger pluginLogger) {
        mailManager = manager;
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
    /*
     * Send a message to a player or add it to the mail if the player is offline.
     */
    public static void safeWhisper(UUID target, MailManager.MailEntry mailEntry) {
        Player player = Bukkit.getPlayer(target);
        if (player != null) {
            player.sendMessage("" + mailEntry.message());
        } else {
            mailManager.addMail(target, MailManager.MailType.PLAYERS, mailEntry);
        }
    }
}
