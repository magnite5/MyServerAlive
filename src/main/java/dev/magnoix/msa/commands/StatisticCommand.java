package dev.magnoix.msa.commands;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.messages.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
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

    private static final List<String> SUB_COMMANDS = new ArrayList<>(List.of(
        "get", "add", "set", "multiply", "reset"
    )); // List of subcommands. Does not include "help", as it is always available

    public StatisticCommand(StatisticsManager statisticsManager) {
        this.statisticsManager = statisticsManager;
        Set<String> validStats;
        try {
            validStats = statisticsManager.getValidStatisticTypes();
        } catch (Exception e) {
            Msg.log(Level.SEVERE, "Failed to load valid command statistics: " + e.getMessage());
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
        //TODO:
        // Switch operation to an argument, for more suggestion display control

        return Commands.literal("stats")

            .executes(this::overviewCommand)

            .then(Commands.literal("help")
                .executes(this::subCommandHelp))

            .then(Commands.argument("type", StringArgumentType.word())
                .suggests((context, builder) -> {
                    CommandSender sender = context.getSource().getSender();
                    if (sender instanceof Player player) { // Don't show shortcuts to admins
                        if (player.hasPermission("msd.stats.self") || player.hasPermission("msd.stats.*") ||
                            player.hasPermission("msd.*")          || player.isOp()) {
                            return builder.buildFuture();
                        }
                    } else {
                        return builder.buildFuture();
                    }
                    for (String stat : VALID_STATISTICS) builder.suggest(stat);
                    return builder.buildFuture();
                })
                .executes(this::subCommand))

            .then(Commands.argument("operation", StringArgumentType.word())
                .suggests((context, builder) -> {
                    CommandSender sender = context.getSource().getSender();
                    if (sender.hasPermission("msa.*") || sender.hasPermission("msa.stats.*")) {
                        SUB_COMMANDS.forEach(builder::suggest);
                        return builder.buildFuture();
                    }
                    SUB_COMMANDS.forEach(subCommand -> {
                        if (sender.hasPermission("msa.stats." + subCommand)) {
                            builder.suggest(subCommand);
                        }
                    });

                    if (sender instanceof Player player) { // Don't show shortcuts to admins
                        if (player.hasPermission("msd.stats.self") || player.hasPermission("msd.stats.*") ||
                            player.hasPermission("msd.*")          || player.isOp()) {
                            return builder.buildFuture();
                        }
                    } else {
                        return builder.buildFuture();
                    }
                    for (String stat : VALID_STATISTICS) builder.suggest(stat);

                    return builder.buildFuture();
                })
                .executes(this::subCommand)
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests(onlinePlayerSuggestions())
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                            return builder.buildFuture();
                        })
                        .executes(this::subCommandGet)
                        .then(Commands.argument("value", IntegerArgumentType.integer())
                            .executes(this::evaluateModification)))))
            .build();
    }

    private static final DynamicCommandExceptionType INVALID_STATISTIC =
        new DynamicCommandExceptionType(input -> new LiteralMessage("Invalid statistic name: " + input));

    /**
     * Resolve an OfflinePlayer target from a Command Context's "target" argument
     * @param ctx The Command Context
     * @return the OfflinePlayer to target
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

    private int evaluateModification(CommandContext<CommandSourceStack> ctx) {
        switch (ctx.getArgument("operation", String.class).toLowerCase()) {
            case "set":
            case "=": subCommandSet(ctx);
            break;

            case "add":
            case "+": subCommandAdd(ctx);
            break;

            case "multiply":
            case "*": subCommandMultiply(ctx);
            break;

            case "reset":
            case "//": subCommandReset(ctx);
            break;

            default: {
                CommandSender sender = ctx.getSource().getSender();
                sender.sendMessage(mm.deserialize("<red>Invalid Operation or Type."));
            }
        }
        return 1;
    }

    private int overviewCommand(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            Msg.msg("You must be a player to view a statistic overview.", sender);
            return 1;
        }
        List<String> messages = new ArrayList<>(List.of(
            " <gold>| <u>Statistics Overview<u:false>",
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

    private int subCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String type = ctx.getArgument("type", String.class).toLowerCase();
        if (!VALID_STATISTICS.contains(type)) {
            // Switch to checking for <operation> argument if invalid
            throw INVALID_STATISTIC.create(type);
        }
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            Msg.msg("You must be a player to use this command.", sender);
            return 1;
        }
        try {
            int amount = statisticsManager.getStatistic(player.getUniqueId(), type);
            Msg.miniMsg("<dark_aqua>You have <aqua>" + amount + " <yellow>" + type + "<dark_aqua>.", player);
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            Msg.miniMsg("An error occurred while retrieving your statistics.", player);
            return 0;
        }
    }

    private int subCommandHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        List<String> messages = new ArrayList<>(List.of(
            " <gold>| <gold><u>Statistics Help Menu<u:false>",
            " <gold>| <dark_aqua>The statistics command lets you view your statistics.",
            " <gold>| <dark_aqua>Usage: /<gold>stats <dark_aqua>[<<gold>type<dark_aqua>>]",
            " <gold>| <dark_aqua>Valid Statistics:"
        ));
        VALID_STATISTICS.forEach(type -> messages.add("  <dark_aqua>» <yellow>" + type));
        if (sender instanceof Player player) {
            if (player.hasPermission("msd.stats.self") || player.hasPermission("msd.stats.*") ||
                player.hasPermission("msd.*")          || player.isOp()) {
                messages.add(" <dark_gray>|");
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
