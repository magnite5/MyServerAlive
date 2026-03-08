package dev.magnoix.msa.utils;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

public class CommandUtils {
    public static SuggestionProvider<CommandSourceStack> onlinePlayerSuggestions() {
        return (context, builder) -> {
            String input = builder.getRemaining().toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                String name = player.getName();
                if (name.toLowerCase().startsWith(input)) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };
    }

    public static Predicate<CommandSourceStack> requirePermission(String prefix, String... perms) {
        return src -> {
            CommandSender sender = src.getSender();
            for (String perm : perms) {
                perm = prefix + perm;
                if (sender.hasPermission(perm)) return true;
            }
            return sender.isOp();
        };
    }
}
