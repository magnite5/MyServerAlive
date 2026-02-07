package dev.magnoix.msa.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.messages.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.logging.Level;

public class StatisticAliases {
    public ArrayList<LiteralCommandNode<CommandSourceStack>> aliases = new ArrayList<>();
    private final StatisticsManager statisticsManager;

    public StatisticAliases(StatisticsManager statisticsManager) {
        this.statisticsManager = statisticsManager;
    }

    public static SuggestionProvider<CommandSourceStack> onlinePlayerSuggestions() {
        return (CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                builder.suggest(player.getName());
            }
            return CompletableFuture.completedFuture(builder.build());
        };
    }

    private static Predicate<CommandSourceStack> requirePermission(String... perms) {
        return src -> {
            CommandSender sender = src.getSender();
            for (String perm : perms) if (sender.hasPermission(perm)) return true;
            return sender.isOp();
        };
    }

    public void createAliases() {
        ArrayList<String> validStats;
        try {
            validStats = new ArrayList<>(statisticsManager.getValidStatisticTypes());
        } catch (Exception e) {
            Msg.log(Level.SEVERE, "Failed to load valid command statistics: " + e.getMessage());
            validStats = new ArrayList<>(); // Empty as a fallback
        }

        validStats.add("nw");

        validStats.forEach(type -> aliases.add(
                Commands.literal(type)
                        .executes(ctx -> getStatistic(ctx, type))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .requires(requirePermission("msd.stats.get", "msd.stats.*", "msd.*"))
                                .suggests(onlinePlayerSuggestions())
                                .executes(ctx -> getOtherStatistic(ctx, type)))
                        .build()
        ));
    }

    public ArrayList<LiteralCommandNode<CommandSourceStack>> getAliases(boolean generate) {
        if (generate) createAliases();
        return aliases;
    }

    private static String normalizeType(String type) {
        return "nw".equals(type) ? "networth" : type;
    }

    /**
     * @return an online player (case-insensitive) or an offline player that has played before; otherwise null.
     */
    private static OfflinePlayer resolveKnownTarget(String name) {
        Player online = Bukkit.getPlayer(name); // allows case-insensitive match for online players
        if (online != null) return online;

        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() ? offline : null;
    }

    private int getStatistic(CommandContext<CommandSourceStack> ctx, String type) {
        CommandSender sender = ctx.getSource().getSender();
        if (sender instanceof Player player) {
            type = normalizeType(type);
            try {
                int value = statisticsManager.getStatistic(player.getUniqueId(), type);
                Msg.miniMsg("<gold>Your <yellow>" + type + "<dark_aqua>: <aqua>" + value + "<dark_aqua>.", player);
                return 1;
            } catch (SQLException e) {
                Msg.miniMsg("<red>Failed to get statistic.", sender);
                Msg.log(Level.SEVERE, "Failed to get statistic for " + player.getName() + ": " + e.getMessage());
                return 0;
            }
        } else {
            sender.sendMessage("This command can only be used by players.");
            return 0;
        }
    }

    private int getOtherStatistic(CommandContext<CommandSourceStack> ctx, String type) {
        CommandSender sender = ctx.getSource().getSender();
        String targetString = ctx.getArgument("target", String.class);

        OfflinePlayer target = resolveKnownTarget(targetString);
        if (target == null) {
            Msg.miniMsg("<red>Unknown player: <yellow>" + targetString + "<red>.", sender);
            return 0;
        }

        type = normalizeType(type);

        try {
            int value = statisticsManager.getStatistic(target.getUniqueId(), type);

            String displayName = target.getName() != null ? target.getName() : targetString;
            Msg.miniMsg("<gold>" + displayName + "<dark_aqua>'s <yellow>" + type + "<dark_aqua>: <aqua>" + value + "<dark_aqua>.", sender);
            return 1;
        } catch (SQLException e) {
            Msg.miniMsg("<red>Failed to get statistic for " + targetString + ".", sender);
            Msg.log(Level.SEVERE, "Failed to get statistic for " + targetString + ": " + e.getMessage());
            return 0;
        }
    }
}
