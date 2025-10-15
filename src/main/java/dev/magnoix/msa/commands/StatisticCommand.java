package dev.magnoix.msa.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.messages.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Level;

public class StatisticCommand { // TODO: Allow Offline Players
    private final  StatisticsManager statisticsManager;
    private Set<String> VALID_STATISTICS;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public StatisticCommand(StatisticsManager statisticsManager) {
        this.statisticsManager = statisticsManager;
        try {
            this.VALID_STATISTICS = statisticsManager.getValidColumns();
            Msg.log("Loaded statistics: " + VALID_STATISTICS);
        } catch (Exception e) {
            Msg.log(Level.SEVERE, "Failed to load valid statistics: " + e.getMessage());
            e.printStackTrace();
            this.VALID_STATISTICS = Set.of(); // Empty set as fall back
        }
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("statistic")
            // "get" structure
            .then(Commands.literal("get")
                .then(Commands.argument("target", ArgumentTypes.player())
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                            return builder.buildFuture();
                        })
                        .executes(this::subCommandGet))))
            .then(Commands.literal("?")
                .then(Commands.argument("target", ArgumentTypes.player())
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                            return builder.buildFuture();
                        })
                        .executes(this::subCommandGet))))
            // "set" structure
            .then(Commands.literal("set")
                .then(Commands.argument("target", ArgumentTypes.player())
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("value", IntegerArgumentType.integer())
                            .executes(this::subCommandSet)))))
            .then(Commands.literal("=")
                .then(Commands.argument("target", ArgumentTypes.player())
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("value", IntegerArgumentType.integer())
                            .executes(this::subCommandSet)))))
            // "add" structure
            .then(Commands.literal("add")
                .then(Commands.argument("target", ArgumentTypes.player())
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                            .executes(this::subCommandAdd)))))
            .then(Commands.literal("+")
                .then(Commands.argument("target", ArgumentTypes.player())
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                            .executes(this::subCommandAdd)))))
            // "multiply" structure
            .then(Commands.literal("multiply")
                .then(Commands.argument("target", ArgumentTypes.player())
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("multiplier", IntegerArgumentType.integer())
                            .executes(this::subCommandMultiply)))))
            .then(Commands.literal("*")
                .then(Commands.argument("target", ArgumentTypes.player())
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String stat : VALID_STATISTICS) builder.suggest(stat);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("multiplier", IntegerArgumentType.integer())
                            .executes(this::subCommandMultiply)))))
            // "reset"
            .then(Commands.literal("reset")
                .then(Commands.argument("target", ArgumentTypes.player())
                    .executes(this::subCommandReset)))
            .then(Commands.literal("//")
                .then(Commands.argument("target", ArgumentTypes.player())
                    .executes(this::subCommandReset)))
            .build();

    }

    private int subCommandGet(CommandContext<CommandSourceStack> ctx) {
        CommandSender source = ctx.getSource().getSender();
        
        try {
            PlayerSelectorArgumentResolver targetResolver = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
            Player target = targetResolver.resolve(ctx.getSource()).getFirst();
            
            String type = ctx.getArgument("type", String.class);

            if (!VALID_STATISTICS.contains(type.toLowerCase())) {
                source.sendMessage(mm.deserialize("<red> Invalid Statistic Type \"<yellow>" + type + "<red>\"."));
                return 0;
            }
            
            int result = statisticsManager.getStatistic(target, type);
            source.sendMessage(mm.deserialize("<gold>" + target.getName() + "<dark_aqua>'s <yellow>" + type + "<dark_aqua>: <aqua>" + result + "<dark_aqua>."));
            return 1;
            
        } catch (CommandSyntaxException e) {
            source.sendMessage(mm.deserialize("<red> Invalid player specified: <yellow>" + e.getMessage() + "<red>."));
            return 0;
        } catch (SQLException e) {
            e.printStackTrace();
            source.sendMessage(mm.deserialize("<red> An error occurred while retrieving the statistic."));
            return 0;
        }
    }
    private int subCommandSet(CommandContext<CommandSourceStack> ctx) {
        CommandSender source = ctx.getSource().getSender();
        
        try {
            PlayerSelectorArgumentResolver targetResolver = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
            Player target = targetResolver.resolve(ctx.getSource()).getFirst();
            
            String type = ctx.getArgument("type", String.class);
            int value = ctx.getArgument("value", Integer.class);

            if (!VALID_STATISTICS.contains(type.toLowerCase())) {
                source.sendMessage(mm.deserialize("<red> Invalid Statistic Type \"<yellow>" + type + "<red>\"."));
                return 0;
            }
            
            int oldValue = statisticsManager.getStatistic(target, type);
            statisticsManager.setStatistic(target, type, value);
            source.sendMessage(mm.deserialize("<dark_aqua>Set <gold>" + target.getName() + "<dark_aqua>'s <yellow>" + type + "<dark_aqua> to <aqua>" + value + "<dark_aqua>, formerly <aqua>" + oldValue + "<dark_aqua>."));
            return 1;
            
        } catch (CommandSyntaxException e) {
            source.sendMessage(mm.deserialize("<red>Invalid player specified: <yellow>" + e.getMessage() + "<red>."));
            return 0;
        } catch (SQLException e) {
            e.printStackTrace();
            source.sendMessage(mm.deserialize("<red>An error occurred while changing the statistic."));
            return 0;
        }
    }
    
    private int subCommandAdd(CommandContext<CommandSourceStack> ctx) {
        CommandSender source = ctx.getSource().getSender();
        
        try {
            PlayerSelectorArgumentResolver targetResolver = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
            Player target = targetResolver.resolve(ctx.getSource()).getFirst();
            
            String type = ctx.getArgument("type", String.class);
            int value = ctx.getArgument("amount", Integer.class);

            if (!VALID_STATISTICS.contains(type.toLowerCase())) {
                source.sendMessage(mm.deserialize("<red> Invalid Statistic Type \"<yellow>" + type + "<red>\"."));
                return 0;
            }
            
            int oldValue = statisticsManager.getStatistic(target, type);
            statisticsManager.addStatistic(target, type, value);
            source.sendMessage(mm.deserialize("<dark_aqua>Set <gold>" + target.getName() + "<dark_aqua>'s <yellow>" + type + "<dark_aqua> to <aqua>" + (oldValue + value) + "<dark_aqua>, formerly <aqua>" + oldValue + "<dark_aqua>."));
            return 1;
            
        } catch (CommandSyntaxException e) {
            source.sendMessage(mm.deserialize("<red>Invalid player specified: <yellow>" + e.getMessage() + "<red>."));
            return 0;
        } catch (SQLException e) {
            e.printStackTrace();
            source.sendMessage(mm.deserialize("<red>An error occurred while updating the statistic."));
            return 0;
        }
    }
    
    private int subCommandMultiply(CommandContext<CommandSourceStack> ctx) {
        CommandSender source = ctx.getSource().getSender();
        
        try {
            PlayerSelectorArgumentResolver targetResolver = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
            Player target = targetResolver.resolve(ctx.getSource()).getFirst();
            
            String type = ctx.getArgument("type", String.class);
            int value = ctx.getArgument("multiplier", Integer.class);

            if (!VALID_STATISTICS.contains(type.toLowerCase())) {
                source.sendMessage(mm.deserialize("<red> Invalid Statistic Type \"<yellow>" + type + "<red>\"."));
                return 0;
            }
            
            int oldValue = statisticsManager.getStatistic(target, type);
            statisticsManager.multiplyStatistic(target, type, value);
            source.sendMessage(mm.deserialize("<dark_aqua>Set <gold>" + target.getName() + "<dark_aqua>'s <yellow>" + type + "<dark_aqua> to <aqua>" + (oldValue * value) + "<dark_aqua>, formerly <aqua>" + oldValue + "<dark_aqua>."));
            return 1;
            
        } catch (CommandSyntaxException e) {
            source.sendMessage(mm.deserialize("<red>Invalid player specified: <yellow>" + e.getMessage() + "<red>."));
            return 0;
        } catch (SQLException e) {
            e.printStackTrace();
            source.sendMessage(mm.deserialize("<red>An error occurred while updating the statistic."));
            return 0;
        }
    }
    
    private int subCommandReset(CommandContext<CommandSourceStack> ctx) {
        CommandSender source = ctx.getSource().getSender();

        try {
            PlayerSelectorArgumentResolver targetResolver = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
            Player target = targetResolver.resolve(ctx.getSource()).getFirst();
            
            StringBuilder s = new StringBuilder();
            VALID_STATISTICS.forEach(statistic -> {
                try {
                    s.append("<yellow>")
                        .append(statistic)
                        .append("<dark_aqua>: <aqua>")
                        .append(statisticsManager.getStatistic(target, statistic))
                        .append("<dark_aqua>. ");
                } catch (SQLException e) {
                    e.printStackTrace();
                    source.sendMessage(mm.deserialize("<red>An error occurred while retrieving the statistics."));
                }
            });
            source.sendMessage(mm.deserialize("<gold>" + target.getName() + "<dark_aqua>'s former statistics: " + s + "<dark_aqua>."));
            source.sendMessage(mm.deserialize("<dark_aqua>Resetting all of <gold>" + target.getName() + "<dark_aqua>'s statistics."));

            statisticsManager.resetPlayer(target);
            return 1;
            
        } catch (CommandSyntaxException e) {
            source.sendMessage(mm.deserialize("<red>Invalid player specified: <yellow>" + e.getMessage() + "<red>."));
            return 0;
        } catch (SQLException e) {
            e.printStackTrace();
            source.sendMessage(mm.deserialize("<red>An error occurred while resetting the statistics."));
            return 0;
        }
    }
}
