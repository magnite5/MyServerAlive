package dev.magnoix.msa.utils;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class CommandUtils {
    public static SuggestionProvider<CommandSourceStack> onlinePlayerSuggestions() {
        return (CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                builder.suggest(player.getName());
            }
            return CompletableFuture.completedFuture(builder.build());
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
