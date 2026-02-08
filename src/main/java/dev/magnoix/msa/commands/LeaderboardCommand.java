package dev.magnoix.msa.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.messages.Msg;
import dev.magnoix.msa.utils.TextUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class LeaderboardCommand {
    private final StatisticsManager statisticsManager;
    private final Set<String> VALID_STATISTICS;

    public LeaderboardCommand(StatisticsManager statisticsManager) {
        this.statisticsManager = statisticsManager;
        Set<String> validStats;
        try {
            validStats = statisticsManager.getValidStatisticTypes();
        } catch (Exception e) {
            Msg.log(Level.SEVERE, "Leaderboard Command: Failed to load valid statistics: " + e.getMessage());
            e.printStackTrace();
            validStats = Set.of(); // Empty set as a fallback
        }
        this.VALID_STATISTICS = validStats;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("leaderboard")
            .then(Commands.argument("type", StringArgumentType.word())
                .suggests((context, builder) -> {
                    for (String stat : VALID_STATISTICS) {
                        builder.suggest(stat);
                    }
                    return builder.buildFuture();
                })
                .executes(ctx -> handleLeaderboard(ctx, 1))
                .then(Commands.argument("page", IntegerArgumentType.integer())
                    .executes(ctx -> {
                        int page = ctx.getArgument("page", Integer.class);
                        return handleLeaderboard(ctx, page);
                    })))
            .build();
    }

    private int handleLeaderboard(CommandContext<CommandSourceStack> ctx, int page) {
        String type = ctx.getArgument("type", String.class);
        String typeCapitalized = TextUtils.capitalizeFirst(type);
        CommandSender sender = ctx.getSource().getSender();

        if (!VALID_STATISTICS.contains(type.toLowerCase())) {
            Msg.miniMsg("<red>Invalid Statistic Type \"<yellow>" + type + "<red>\".", sender);
            return 0;
        }
        try {
            int limit = 10;
            int totalEntries = statisticsManager.getStatisticCount(type);
            int totalPages = (int) Math.ceil((double) totalEntries / limit);

            if (totalPages == 0) {
                Msg.miniMsg("<dark_aqua>There are no pages in the <yellow>" + type + " <dark_aqua>leaderboard.", sender);
                return 1;
            }

            if (page < 1) page = 1;
            if (page > totalPages) page = totalPages;
            int offset = (page - 1) * limit;

            List<StatisticsManager.LeaderboardEntry> leaderboard = statisticsManager.getTopPlayers(type, limit, offset);
            if (leaderboard.isEmpty()) {
                Msg.miniMsg("<dark_aqua>There are no entries in the <yellow>" + type + " <dark_aqua>leaderboard.", sender);
                return 1;
            }
            Msg.msg("", sender);
            Msg.miniMsg(" <red>--- <gold>| <dark_aqua><u>" + typeCapitalized + "</u> <gold>(<red>" + page + "<gold> / <red>" + totalPages + "<gold>)", sender);
            for (StatisticsManager.LeaderboardEntry entry : leaderboard) {
                String playerName = Bukkit.getOfflinePlayer(entry.player()).getName();
                Msg.miniMsg("  <red>" + entry.position() + ". <gold>" + playerName + "<dark_aqua>: <aqua>" + String.format("%,.0f", entry.value()), sender);
            }
            Msg.miniMsg(" <red>--- <gold>| <dark_aqua><u>" + typeCapitalized + "</u> <gold>(<red>" + page + "<gold> / <red>" + totalPages + "<gold>)", sender);
            // Button Logic
            String previousButton = page > 1
                ? "<click:run_command:'/leaderboard " + type + " " + (page - 1) + "'><hover:show_text:'Go to previous page'><dark_aqua><u>«</u></hover></click>"
                : "<gray>«</gray>";
            String nextButton = page < totalPages
                ? "<click:run_command:'/leaderboard " + type + " " + (page + 1) + "'><hover:show_text:'Go to next page'><dark_aqua><u>»</u></hover></red></click>"
                : "<gray>»</gray>";
            Msg.msg("", sender);
            Msg.miniMsg(" <gold>--- | " + previousButton + " <dark_aqua>| " + nextButton, sender);
            Msg.msg("", sender);
            return 1;

        } catch (Exception e) {
            e.printStackTrace();
            Msg.miniMsg("<red>An error occurred while retrieving the leaderboard data.", sender);
            return 0;
        }
    }
}
