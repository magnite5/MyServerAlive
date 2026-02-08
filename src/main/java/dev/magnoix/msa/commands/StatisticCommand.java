package dev.magnoix.msa.commands;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.messages.Msg;
import dev.magnoix.msa.utils.CommandUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.logging.Level;

public class StatisticCommand {
    private final StatisticsManager statisticsManager;
    private final Set<String> VALID_STATISTICS;
    private final MiniMessage mm = MiniMessage.miniMessage();
    
    private final String permissionPrefix;

    public Set<String> getValidStatistics() { return VALID_STATISTICS; }

    public StatisticCommand(String permissionPrefix, StatisticsManager statisticsManager) {
        this.permissionPrefix = permissionPrefix;
        this.statisticsManager = statisticsManager;
        Set<String> validStats;
        try {
            validStats = statisticsManager.getValidStatisticTypes();
        } catch (Exception e) {
            Msg.log(Level.SEVERE, "Failed to load valid command statistics: " + e.getMessage());
            validStats = Set.of(); // Empty set as a fallback
        }
        this.VALID_STATISTICS = validStats;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("stats")
            .executes(this::overviewCommand)
                .then(Commands.literal("help")
                        .executes(this::subCommandHelp))

                .then(Commands.literal("get")
                        .requires(CommandUtils.requirePermission(permissionPrefix, ".stats.get", ".stats.*", ".*"))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(CommandUtils.onlinePlayerSuggestions())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                                            return builder.buildFuture();
                                        })
                                        .executes(this::subCommandGet))))
                .then(Commands.literal("?")
                        .requires(CommandUtils.requirePermission(permissionPrefix, ".stats.get", ".stats.*", ".*"))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(CommandUtils.onlinePlayerSuggestions())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                                            return builder.buildFuture();
                                        })
                                        .executes(this::subCommandGet))))

                // "set" structure
                .then(Commands.literal("set")
                        .requires(CommandUtils.requirePermission(permissionPrefix, ".stats.set", ".stats.*", ".*"))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(CommandUtils.onlinePlayerSuggestions())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("value", IntegerArgumentType.integer())
                                                .executes(this::subCommandSet)))))
                .then(Commands.literal("=")
                        .requires(CommandUtils.requirePermission(permissionPrefix, ".stats.set", ".stats.*", ".*"))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(CommandUtils.onlinePlayerSuggestions())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("value", IntegerArgumentType.integer())
                                                .executes(this::subCommandSet)))))

                // "add" structure
                .then(Commands.literal("add")
                        .requires(CommandUtils.requirePermission(permissionPrefix, ".stats.add", ".stats.*", ".*"))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(CommandUtils.onlinePlayerSuggestions())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(this::subCommandAdd)))))
                .then(Commands.literal("+")
                        .requires(CommandUtils.requirePermission(permissionPrefix, ".stats.add", ".stats.*", ".*"))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(CommandUtils.onlinePlayerSuggestions())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(this::subCommandAdd)))))

                // "multiply" structure
                .then(Commands.literal("multiply")
                        .requires(CommandUtils.requirePermission(permissionPrefix, ".stats.multiply", ".stats.*", ".*"))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(CommandUtils.onlinePlayerSuggestions())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("multiplier", IntegerArgumentType.integer())
                                                .executes(this::subCommandMultiply)))))
                .then(Commands.literal("*")
                        .requires(CommandUtils.requirePermission(permissionPrefix, ".stats.multiply", ".stats.*", ".*"))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(CommandUtils.onlinePlayerSuggestions())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("multiplier", IntegerArgumentType.integer())
                                                .executes(this::subCommandMultiply)))))

                // "reset"
                .then(Commands.literal("reset")
                        .requires(CommandUtils.requirePermission(permissionPrefix, ".stats.reset", ".stats.*", ".*"))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(CommandUtils.onlinePlayerSuggestions())
                                .executes(this::subCommandReset)))
                .then(Commands.literal("//")
                        .requires(CommandUtils.requirePermission(permissionPrefix, ".stats.reset", ".stats.*", ".*"))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(CommandUtils.onlinePlayerSuggestions())
                                .executes(this::subCommandReset)))
                .build();
    }

    private static final DynamicCommandExceptionType INVALID_STATISTIC =
        new DynamicCommandExceptionType(input -> new LiteralMessage("Invalid statistic name: " + input));

    /**
     * Resolve an OfflinePlayer target from a Command Context's "target" argument
     * @param ctx The Command Context
     * @return offlinePlayer to target
     */
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

    private int overviewCommand(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            Msg.msg("You must be a player to view a statistic overview.", sender);
            return 1;
        }
        List<String> messages = new ArrayList<>(List.of(
            " <gold>Statistics Overview<u:false>",
            " <gold>| <dark_aqua>Stats for <gold>" + player.getName() + "<dark_aqua>: "
        ));
        UUID uuid = player.getUniqueId();
        VALID_STATISTICS.forEach(type -> {
            try {
                messages.add(" <dark_aqua>» <yellow>" + type + "<dark_aqua>: <aqua>" + statisticsManager.getStatistic(uuid, type));
            } catch (SQLException e) {
                messages.add(" <dark_aqua>» <red>" + type + ": ERROR");
                Msg.log(Level.SEVERE, "Failed to get a statistic for " + player.getName() + "'s overview:  " + e.getMessage());
            }
        });
        Msg.miniMsg(messages, sender);
        return 1;
    }

    private int subCommandHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        List<String> messages = new ArrayList<>();
        if (sender instanceof Player player) {
            messages.add(" <gold>Stats <u>Help<u:false>: ");
            messages.add(" <dark_aqua>» <aqua>There are " + VALID_STATISTICS.size() + " valid statistics, each with a dedicated command:");
            VALID_STATISTICS.forEach(stat -> messages.add(" <aqua>» <yellow>" + stat));
            if (player.hasPermission(".stats.self") || player.hasPermission(".stats.*") ||
                player.hasPermission(".*")          || player.isOp()) {
                messages.add(" ");
                messages.add(" <red>| <u>Admin Info<u:false>: ");
                messages.add("  <red>» Usage: <dark_aqua>/<gold>stats <dark_aqua><<dark_green>operation<dark_aqua>> " +
                    "<<gold>target<dark_aqua>> " +
                    "<<yellow>type<dark_aqua>> " +
                    "[<<aqua>amount<dark_aqua>>]");
            }
        }
        messages.add("");
        Msg.miniMsg(messages, sender);
        return 1;
    }

    private int subCommandGet(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        OfflinePlayer target = resolveTarget(ctx);

        if (!isTargetValid(target)) {
            sender.sendMessage(mm.deserialize("<red>Unknown player: <yellow>" + ctx.getArgument("target", String.class) + "<red>."));
            return 0;
        }

        String type = ctx.getArgument("type", String.class);
        if (!VALID_STATISTICS.contains(type.toLowerCase())) {
            sender.sendMessage(mm.deserialize("<red>Invalid Statistic Type \"<yellow>" + type + "<red>\"."));
            return 0;
        }

        try {
            int result = statisticsManager.getStatistic(target.getUniqueId(), type);
            sender.sendMessage(mm.deserialize("<gold>" + target.getName() + "<dark_aqua>'s <yellow>" + type + "<dark_aqua>: <aqua>" + result + "<dark_aqua>."));
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(mm.deserialize("<red>An error occurred while retrieving the statistic."));
            return 0;
        }
    }

    private int subCommandSet(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        OfflinePlayer target = resolveTarget(ctx);

        if (!isTargetValid(target)) {
            sender.sendMessage(mm.deserialize("<red>Unknown player: <yellow>" + ctx.getArgument("target", String.class) + "<red>."));
            return 0;
        }

        String type = ctx.getArgument("type", String.class);
        int value = ctx.getArgument("value", Integer.class);
        if (!VALID_STATISTICS.contains(type.toLowerCase())) {
            sender.sendMessage(mm.deserialize("<red>Invalid Statistic Type \"<yellow>" + type + "<red>\"."));
            return 0;
        }

        try {
            int oldValue = statisticsManager.getStatistic(target.getUniqueId(), type);
            statisticsManager.setStatistic(target.getUniqueId(), type, value);
            sender.sendMessage(mm.deserialize("<dark_aqua>Set <gold>" + target.getName() + "<dark_aqua>'s <yellow>" + type + "<dark_aqua> to <aqua>" + value + "<dark_aqua>, formerly <aqua>" + oldValue + "<dark_aqua>."));
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(mm.deserialize("<red>An error occurred while changing the statistic."));
            return 0;
        }
    }

    private int subCommandAdd(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        OfflinePlayer target = resolveTarget(ctx);

        if (!isTargetValid(target)) {
            sender.sendMessage(mm.deserialize("<red>Unknown player: <yellow>" + ctx.getArgument("target", String.class) + "<red>."));
            return 0;
        }

        String type = ctx.getArgument("type", String.class);
        int value = ctx.getArgument("amount", Integer.class);
        if (!VALID_STATISTICS.contains(type.toLowerCase())) {
            sender.sendMessage(mm.deserialize("<red>Invalid Statistic Type \"<yellow>" + type + "<red>\"."));
            return 0;
        }

        try {
            int oldValue = statisticsManager.getStatistic(target.getUniqueId(), type);
            statisticsManager.addToStatistic(target.getUniqueId(), type, value);
            sender.sendMessage(mm.deserialize("<dark_aqua>Set <gold>" + target.getName() + "<dark_aqua>'s <yellow>" + type + "<dark_aqua> to <aqua>" + (oldValue + value) + "<dark_aqua>, formerly <aqua>" + oldValue + "<dark_aqua>."));
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(mm.deserialize("<red>An error occurred while updating the statistic."));
            return 0;
        }
    }

    private int subCommandMultiply(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        OfflinePlayer target = resolveTarget(ctx);

        if (!isTargetValid(target)) {
            sender.sendMessage(mm.deserialize("<red>Unknown player: <yellow>" + ctx.getArgument("target", String.class) + "<red>."));
            return 0;
        }

        String type = ctx.getArgument("type", String.class);
        int value = ctx.getArgument("multiplier", Integer.class);
        if (!VALID_STATISTICS.contains(type.toLowerCase())) {
            sender.sendMessage(mm.deserialize("<red>Invalid Statistic Type \"<yellow>" + type + "<red>\"."));
            return 0;
        }

        try {
            int oldValue = statisticsManager.getStatistic(target.getUniqueId(), type);
            statisticsManager.multiplyStatistic(target.getUniqueId(), type, value);
            sender.sendMessage(mm.deserialize("<dark_aqua>Set <gold>" + target.getName() + "<dark_aqua>'s <yellow>" + type + "<dark_aqua> to <aqua>" + (oldValue * value) + "<dark_aqua>, formerly <aqua>" + oldValue + "<dark_aqua>."));
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(mm.deserialize("<red>An error occurred while updating the statistic."));
            return 0;
        }
    }

    private int subCommandReset(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        OfflinePlayer target = resolveTarget(ctx);

        if (!isTargetValid(target)) {
            sender.sendMessage(mm.deserialize("<red>Unknown player: <yellow>" + ctx.getArgument("target", String.class) + "<red>."));
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
                    sender.sendMessage(mm.deserialize("<red>An error occurred while retrieving statistics."));
                }
            }
            sender.sendMessage(mm.deserialize("<gold>" + target.getName() + "<dark_aqua>'s former statistics: " + s + "<dark_aqua>."));
            sender.sendMessage(mm.deserialize("<dark_aqua>Resetting all of <gold>" + target.getName() + "<dark_aqua>'s statistics."));
            statisticsManager.resetPlayer(target.getUniqueId());
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(mm.deserialize("<red>An error occurred while resetting the statistics."));
            return 0;
        }
    }
}
