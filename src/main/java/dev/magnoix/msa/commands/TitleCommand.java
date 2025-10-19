package dev.magnoix.msa.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.databases.TitleManager;
import dev.magnoix.msa.menus.TitleMenu;
import dev.magnoix.msa.messages.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TitleCommand {
    private TitleManager titleManager;
    private TitleMenu titleMenu;
    // TODO: Add command logic here (minimum include: create, delete titles, and open title viewer)

    public CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestTitleNames(
        CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder
    ) {
        List<String> names;
        try {
            names = titleManager.getAllTitleNames();
        } catch (Exception e) {
            Msg.miniMsg("<red>An error occurred while retrieving title names for suggestions: <yellow>" + e.getMessage(), ctx.getSource().getSender());
            return builder.buildFuture();
        }
        String partial = builder.getRemaining().toLowerCase();
        names.stream()
            .filter(name -> name.toLowerCase().contains(partial))
            .forEach(builder::suggest);
        return builder.buildFuture();
    }

    public LiteralCommandNode<CommandSourceStack> create(TitleManager titleManager) {
        this.titleManager = titleManager;
        this.titleMenu = new TitleMenu(titleManager);
        return Commands.literal("titles")
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (sender instanceof Player) {
                    titleMenu.open((Player) sender, 0);
                } else {
                    sender.sendMessage("This GUI can only be opened by a player.");
                }
                return 1;
            })
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.argument("prefix", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            String name = ctx.getArgument("name", String.class);
                            String prefix = ctx.getArgument("prefix", String.class);
                            try {
                                TitleManager.title newTitle = titleManager.createTitle(name, prefix);
                                Msg.miniMsg("<dark_aqua>Successfully created the new <gold>\"<yellow>" + newTitle.name() + "<gold>\" <dark_aqua>title with an ID of <i><dark_gray>" + newTitle.id() + "</i>", sender);
                                return 1;
                            } catch (TitleManager.DuplicateTitleException e) {
                                Msg.miniMsg("<red>A title with the name <gold>\"<yellow>" + name + "<gold>\" <red> already exists.", sender);
                                return 0;
                            } catch (Exception e) {
                                Msg.miniMsg("<red>An error occurred while creating the title: <yellow>" + e.getMessage(), sender);
                                return 0;
                            }
                        }))))
            .then(Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests(this::suggestTitleNames)
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        String name = ctx.getArgument("name", String.class);
                        try {
                            TitleManager.title title = titleManager.getTitleFromName(name);
                            if (title != null) {
                                titleManager.deleteTitle(title.id());
                                Msg.miniMsg("<dark_aqua>Successfully deleted the <gold>\"<yellow>" + name + "<gold>\" <dark_aqua>title, with an ID of <i><dark_gray>" + title.id() + "</i>", sender);
                            } else Msg.miniMsg("<yellow>No title with the name <gold>\"<yellow>" + name + "<gold>\" <yellow>exists.", sender);
                            return 1;
                        } catch (Exception e) {
                            Msg.miniMsg("<red>An error occurred while retrieving the title: <yellow>" + e.getMessage(), sender);
                            return 0;
                        }
                    })))
            .then(Commands.literal("player")
                .then(Commands.argument("target", StringArgumentType.word())
                    .then(Commands.literal("setActive")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .executes(ctx -> {
                                CommandSender sender = ctx.getSource().getSender();
                                OfflinePlayer target = resolveTarget(ctx);
                                if (!isTargetValid(target)) {
                                    sender.sendMessage("<red>Unknown player: <yellow>" + ctx.getArgument("target", String.class));
                                    return 0;
                                }

                                String name = ctx.getArgument("name", String.class);
                                try {
                                    TitleManager.title title = titleManager.getTitleFromName(name);
                                    if (title != null) {
                                        titleManager.setActiveTitle(target.getUniqueId(), title.id());
                                        Msg.miniMsg("<dark_aqua>Successfully set <gold>" + target.getName() + "<dark_aqua>'s active title to <gold>\"<yellow>" + name + "<gold>\" <dark_aqua>with an ID of <i><dark_gray>" + title.id() + "</i>", sender);
                                    } else {
                                        Msg.miniMsg("<yellow>No title with the name <gold>\"<yellow>" + name + "<gold>\"<yellow>exists.", sender);
                                    }
                                    return 1;
                                } catch (Exception e) {
                                    Msg.miniMsg("<red>An error occurred while retrieving the title: <yellow>" + e.getMessage(), sender);
                                    return 0;
                                }
                            }))))).build();
    }

    private boolean isTargetValid(OfflinePlayer target) {
        return target != null && (target.hasPlayedBefore() || target.isOnline());
    }

    protected OfflinePlayer resolveTarget(CommandContext<CommandSourceStack> ctx) {
        String name = ctx.getArgument("target", String.class);
        if (name == null) return null;
        OfflinePlayer onlinePlayer = Bukkit.getPlayerExact(name);
        if (onlinePlayer != null) return onlinePlayer;
        return Bukkit.getOfflinePlayer(name);
    }

}
