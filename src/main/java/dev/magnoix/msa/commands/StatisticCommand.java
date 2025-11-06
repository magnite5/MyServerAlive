package dev.magnoix.msa.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.messages.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.logging.Level;

public class StatisticCommand {
    private final StatisticsManager statisticsManager;
    private final Set<String> VALID_STATISTICS;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public StatisticCommand(StatisticsManager statisticsManager) {
        this.statisticsManager = statisticsManager;
        Set<String> validStats;
        try {
            validStats = statisticsManager.getValidStatisticTypes();
            Msg.log("Loaded statistics: " + validStats);
        } catch (Exception e) {
            Msg.log(Level.SEVERE, "Failed to load valid statistics: " + e.getMessage());
            e.printStackTrace();
            validStats = Set.of(); // Empty set as a fallback
        }
        this.VALID_STATISTICS = validStats;
    }

    public static SuggestionProvider<CommandSourceStack> onlinePlayerSuggestions() {
        return (CommandContext<CommandSourceStack> context,SuggestionsBuilder builder) -> {
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
            return false;
        };
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("statistic")
            // "get" structure
            .then(Commands.literal("get")
                .requires(requirePermission("msd.stats.get", "msd.stats.*", "msd.*"))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests(onlinePlayerSuggestions())
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                            return builder.buildFuture();
                        })
                        .executes(this::subCommandGet))))
            .then(Commands.literal("?")
                .requires(requirePermission("msd.stats.get", "msd.stats.*", "msd.*"))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests(onlinePlayerSuggestions())
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                            return builder.buildFuture();
                        })
                        .executes(this::subCommandGet))))
            // "set" structure
            .then(Commands.literal("set")
                .requires(requirePermission("msd.stats.set", "msd.stats.*", "msd.*"))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests(onlinePlayerSuggestions())
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("value", IntegerArgumentType.integer())
                            .executes(this::subCommandSet)))))
            .then(Commands.literal("=")
                .requires(requirePermission("msd.stats.set", "msd.stats.*", "msd.*"))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests(onlinePlayerSuggestions())
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("value", IntegerArgumentType.integer())
                            .executes(this::subCommandSet)))))
            // "add" structure
            .then(Commands.literal("add")
                .requires(requirePermission("msd.stats.add", "msd.stats.*", "msd.*"))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests(onlinePlayerSuggestions())
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                            .executes(this::subCommandAdd)))))
            .then(Commands.literal("+")
                .requires(requirePermission("msd.stats.add", "msd.stats.*", "msd.*"))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests(onlinePlayerSuggestions())
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                            .executes(this::subCommandAdd)))))
            // "multiply" structure
            .then(Commands.literal("multiply")
                .requires(requirePermission("msd.stats.multiply", "msd.stats.*", "msd.*"))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests(onlinePlayerSuggestions())
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("multiplier", IntegerArgumentType.integer())
                            .executes(this::subCommandMultiply)))))
            .then(Commands.literal("*")
                .requires(requirePermission("msd.stats.multiply", "msd.stats.*", "msd.*"))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests(onlinePlayerSuggestions())
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("multiplier", IntegerArgumentType.integer())
                            .executes(this::subCommandMultiply)))))
            // "reset"
            .then(Commands.literal("reset")
                .requires(requirePermission("msd.stats.reset", "msd.stats.*", "msd.*"))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests(onlinePlayerSuggestions())
                    .executes(this::subCommandReset)))
            .then(Commands.literal("//")
                .requires(requirePermission("msd.stats.reset", "msd.stats.*", "msd.*"))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests(onlinePlayerSuggestions())
                    .executes(this::subCommandReset)))
            .build();
    }

    protected OfflinePlayer resolveTarget(CommandContext<CommandSourceStack> ctx) {
        String name = ctx.getArgument("target", String.class);
        if (name == null) return null;
        OfflinePlayer onlinePlayer = Bukkit.getPlayerExact(name);
        if (onlinePlayer != null) return onlinePlayer;
        return Bukkit.getOfflinePlayer(name);
    }

    private boolean isTargetValid(OfflinePlayer target) {
        return target != null && (target.hasPlayedBefore() || target.isOnline());
    }

    private int subCommandGet(CommandContext<CommandSourceStack> ctx) {
        CommandSender source = ctx.getSource().getSender();
        OfflinePlayer target = resolveTarget(ctx);

        if (!isTargetValid(target)) {
            source.sendMessage(mm.deserialize("<red>Unknown player: <yellow>" + ctx.getArgument("target", String.class) + "<red>."));
            return 0;
        }

        String type = ctx.getArgument("type", String.class);
        if (!VALID_STATISTICS.contains(type.toLowerCase())) {
            source.sendMessage(mm.deserialize("<red>Invalid Statistic Type \"<yellow>" + type + "<red>\"."));
            return 0;
        }

        try {
            int result = statisticsManager.getStatistic(target.getUniqueId(), type);
            source.sendMessage(mm.deserialize("<gold>" + target.getName() + "<dark_aqua>'s <yellow>" + type + "<dark_aqua>: <aqua>" + result + "<dark_aqua>."));
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            source.sendMessage(mm.deserialize("<red>An error occurred while retrieving the statistic."));
            return 0;
        }
    }

    private int subCommandSet(CommandContext<CommandSourceStack> ctx) {
        CommandSender source = ctx.getSource().getSender();
        OfflinePlayer target = resolveTarget(ctx);

        if (!isTargetValid(target)) {
            source.sendMessage(mm.deserialize("<red>Unknown player: <yellow>" + ctx.getArgument("target", String.class) + "<red>."));
            return 0;
        }

        String type = ctx.getArgument("type", String.class);
        int value = ctx.getArgument("value", Integer.class);
        if (!VALID_STATISTICS.contains(type.toLowerCase())) {
            source.sendMessage(mm.deserialize("<red>Invalid Statistic Type \"<yellow>" + type + "<red>\"."));
            return 0;
        }

        try {
            int oldValue = statisticsManager.getStatistic(target.getUniqueId(), type);
            statisticsManager.setStatistic(target.getUniqueId(), type, value);
            source.sendMessage(mm.deserialize("<dark_aqua>Set <gold>" + target.getName() + "<dark_aqua>'s <yellow>" + type + "<dark_aqua> to <aqua>" + value + "<dark_aqua>, formerly <aqua>" + oldValue + "<dark_aqua>."));
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            source.sendMessage(mm.deserialize("<red>An error occurred while changing the statistic."));
            return 0;
        }
    }

    private int subCommandAdd(CommandContext<CommandSourceStack> ctx) {
        CommandSender source = ctx.getSource().getSender();
        OfflinePlayer target = resolveTarget(ctx);

        if (!isTargetValid(target)) {
            source.sendMessage(mm.deserialize("<red>Unknown player: <yellow>" + ctx.getArgument("target", String.class) + "<red>."));
            return 0;
        }

        String type = ctx.getArgument("type", String.class);
        int value = ctx.getArgument("amount", Integer.class);
        if (!VALID_STATISTICS.contains(type.toLowerCase())) {
            source.sendMessage(mm.deserialize("<red>Invalid Statistic Type \"<yellow>" + type + "<red>\"."));
            return 0;
        }

        try {
            int oldValue = statisticsManager.getStatistic(target.getUniqueId(), type);
            statisticsManager.addToStatistic(target.getUniqueId(), type, value);
            source.sendMessage(mm.deserialize("<dark_aqua>Set <gold>" + target.getName() + "<dark_aqua>'s <yellow>" + type + "<dark_aqua> to <aqua>" + (oldValue + value) + "<dark_aqua>, formerly <aqua>" + oldValue + "<dark_aqua>."));
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            source.sendMessage(mm.deserialize("<red>An error occurred while updating the statistic."));
            return 0;
        }
    }

    private int subCommandMultiply(CommandContext<CommandSourceStack> ctx) {
        CommandSender source = ctx.getSource().getSender();
        OfflinePlayer target = resolveTarget(ctx);

        if (!isTargetValid(target)) {
            source.sendMessage(mm.deserialize("<red>Unknown player: <yellow>" + ctx.getArgument("target", String.class) + "<red>."));
            return 0;
        }

        String type = ctx.getArgument("type", String.class);
        int value = ctx.getArgument("multiplier", Integer.class);
        if (!VALID_STATISTICS.contains(type.toLowerCase())) {
            source.sendMessage(mm.deserialize("<red>Invalid Statistic Type \"<yellow>" + type + "<red>\"."));
            return 0;
        }

        try {
            int oldValue = statisticsManager.getStatistic(target.getUniqueId(), type);
            statisticsManager.multiplyStatistic(target.getUniqueId(), type, value);
            source.sendMessage(mm.deserialize("<dark_aqua>Set <gold>" + target.getName() + "<dark_aqua>'s <yellow>" + type + "<dark_aqua> to <aqua>" + (oldValue * value) + "<dark_aqua>, formerly <aqua>" + oldValue + "<dark_aqua>."));
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            source.sendMessage(mm.deserialize("<red>An error occurred while updating the statistic."));
            return 0;
        }
    }

    private int subCommandReset(CommandContext<CommandSourceStack> ctx) {
        CommandSender source = ctx.getSource().getSender();
        OfflinePlayer target = resolveTarget(ctx);

        if (!isTargetValid(target)) {
            source.sendMessage(mm.deserialize("<red>Unknown player: <yellow>" + ctx.getArgument("target", String.class) + "<red>."));
            return 0;
        }

        try {
            StringBuilder s = new StringBuilder();
            for (String statistic : VALID_STATISTICS) {
                try {
                    s.append("<yellow>")
                        .append(statistic)
                        .append("<dark_aqua>: <aqua>")
                        .append(statisticsManager.getStatistic(target.getUniqueId(), statistic))
                        .append("<dark_aqua>. ");
                } catch (SQLException e) {
                    e.printStackTrace();
                    source.sendMessage(mm.deserialize("<red>An error occurred while retrieving statistics."));
                }
            }
            source.sendMessage(mm.deserialize("<gold>" + target.getName() + "<dark_aqua>'s former statistics: " + s + "<dark_aqua>."));
            source.sendMessage(mm.deserialize("<dark_aqua>Resetting all of <gold>" + target.getName() + "<dark_aqua>'s statistics."));
            statisticsManager.resetPlayer(target.getUniqueId());
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            source.sendMessage(mm.deserialize("<red>An error occurred while resetting the statistics."));
            return 0;
        }
    }
}
