package dev.magnoix.msa.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.messages.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/*
    TODO:
        - Leaderboard command, including:
            - Get a leaderboard for any leaderboard type
            - Specify a leaderboard size
            - Display 10 leaderboard entries per page
 */
public class LeaderboardCommand {
    private final StatisticsManager statisticsManager;
    private final Set<String> VALID_STATISTICS;

    public LeaderboardCommand(StatisticsManager statisticsManager) {
        this.statisticsManager = statisticsManager;
        Set<String> validStats;
        try {
            validStats = statisticsManager.getValidColumns();
            Msg.log("Loaded statistics: " + validStats);
        } catch (Exception e) {
            Msg.log(Level.SEVERE, "Failed to load valid statistics: " + e.getMessage());
            e.printStackTrace();
            validStats = Set.of(); // Empty set as a fallback
        }
        this.VALID_STATISTICS = validStats;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("leaderboard")
            .then(Commands.argument("type", StringArgumentType.word())
                .executes(ctx -> {
                    return handleLeaderboard(ctx, 1);
                })
                .then(Commands.argument("page", IntegerArgumentType.integer())
                    .executes(ctx -> {
                        int page = ctx.getArgument("page", Integer.class);
                        return handleLeaderboard(ctx, page);
                    })))
            .build();
    }

    private int handleLeaderboard(CommandContext<CommandSourceStack> ctx, int page) {
        String type = ctx.getArgument("type", String.class);
        CommandSender sender = ctx.getSource().getSender();

        if (!VALID_STATISTICS.contains(type.toLowerCase())) {
            Msg.miniMsg("<red>Invalid Statistic Type \"<yellow>" + type + "<red>\".", sender);
            return 0;
        }
        try {
            int limit = 10;
            int offset = (page - 1) * limit;
            List<StatisticsManager.LeaderboardEntry> leaderboard = statisticsManager.getTopPlayers(type, limit, offset);
            for (StatisticsManager.LeaderboardEntry entry : leaderboard) {
                String playerName = Bukkit.getOfflinePlayer(entry.player()).getName();
                Msg.miniMsg("<gold>" + playerName + "<dark_aqua>: <aqua>" + entry.value(), sender);
            }
            return 1;

        } catch (Exception e) {
            e.printStackTrace();
            Msg.miniMsg("<red>An error occurred while retrieving the leaderboard data.", sender);
            return 0;
        }
    }
}
