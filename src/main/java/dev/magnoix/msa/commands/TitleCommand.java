package dev.magnoix.msa.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.databases.TitleManager;
import dev.magnoix.msa.menus.TitleMenu;
import dev.magnoix.msa.messages.Msg;
import dev.magnoix.msa.utils.TextUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class TitleCommand {
    private TitleManager titleManager;
    private TitleMenu titleMenu;

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

    public LiteralCommandNode<CommandSourceStack> create(TitleManager titleManager) {
        this.titleManager = titleManager;
        this.titleMenu = new TitleMenu(titleManager);
        return Commands.literal("titles")
            .executes(this::commandGui)
            .then(Commands.literal("create")
                .requires(requirePermission("msd.titles.create", "msd.titles.*", "msd.*"))
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.argument("prefix", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            String name = ctx.getArgument("name", String.class);
                            String prefix = normalizePrefixArg(ctx.getArgument("prefix", String.class));
                            try {
                                TitleManager.title newTitle = titleManager.createTitle(name, prefix);
                                titleManager.syncTitleUpdate(newTitle.id(), true);
                                Msg.miniMsg("<dark_aqua>Created the new <gold>\"<yellow>" + newTitle.name() + "<gold>\" <dark_aqua>title ID <i><dark_gray>" + newTitle.id() + "</i>", sender);
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
                .requires(requirePermission("msd.titles.delete", "msd.titles.*", "msd.*"))
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests(this::suggestTitleNames)
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        String name = ctx.getArgument("name", String.class);
                        try {
                            TitleManager.title title = titleManager.getTitleFromName(name);
                            if (title != null) {
                                titleManager.deleteTitle(title.id());
                                Msg.miniMsg("<dark_aqua>Deleted the <gold>\"<yellow>" + name + "<gold>\" <dark_aqua>title, ID <i><dark_gray>" + title.id() + "</i>", sender);

                                for (UUID uuid : titleManager.getPlayersWithActiveTitle(title.id())) {
                                    titleManager.setActivePrefix(uuid, 1);
                                }
                            } else Msg.miniMsg("<yellow>No title with the name <gold>\"<yellow>" + name + "<gold>\" <yellow>exists.", sender);
                            return 1;
                        } catch (Exception e) {
                            Msg.miniMsg("<red>An error occurred while retrieving the title: <yellow>" + e.getMessage(), sender);
                            return 0;
                        }
                    })))
            .then(Commands.literal("edit")
                .requires(requirePermission("msd.titles.edit", "msd.titles.*", "msd.*"))
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests(this::suggestTitleNames)
                    .then(Commands.literal("prefix")
                        .then(Commands.argument("newPrefix", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                CommandSender sender = ctx.getSource().getSender();
                                String name = ctx.getArgument("name", String.class);

                                String newPrefix = normalizePrefixArg(ctx.getArgument("newPrefix", String.class));

                                try {
                                    TitleManager.title title = titleManager.getTitleFromName(name);
                                    TitleManager.title newTitle = titleManager.setTitlePrefix(title.id(), newPrefix);
                                    titleManager.syncTitleUpdate(newTitle.id(), true);
                                    Msg.miniMsg("<dark_aqua>Updated the <gold>\"<yellow>" + newTitle.name() + "<gold>\" <dark_aqua>title's prefix.", sender);
                                    Msg.miniMsg("<dark_aqua>Old Prefix: <gold>\"" + title.prefix() + "<gold>\"<dark_aqua>. New Prefix: <gold>\"" + newTitle.prefix() + "<gold>\"<dark_aqua>.", sender);
                                    return 1;
                                } catch (Exception e) {
                                    Msg.miniMsg("<red>An error occurred while retrieving the title: <yellow>" + e.getMessage(), sender);
                                    return 0;
                                }
                            })))
                    .then(Commands.literal("name")
                        .then(Commands.argument("newName", StringArgumentType.string())
                            .executes(ctx -> {
                                CommandSender sender = ctx.getSource().getSender();
                                String name = ctx.getArgument("name", String.class);
                                String newName = ctx.getArgument(("newName"), String.class);
                                try {
                                    TitleManager.title title = titleManager.getTitleFromName(name);
                                    TitleManager.title newTitle = titleManager.setTitleName(title.id(), newName);
                                    Msg.miniMsg("<dark_aqua>Changed the <gold>\"<yellow>" + title.name() + "<gold>\" <dark_aqua>title's name to <gold>\"<yellow>" + newTitle.name() + "<gold>\"<dark_aqua>.", sender);
                                    return 1;
                                } catch (Exception e) {
                                    Msg.miniMsg("<red>An error occurred while retrieving the title: <yellow>" + e.getMessage(), sender);
                                    return 0;
                                }
                            })))))
            .then(Commands.literal("sync")
                .requires(requirePermission("msd.titles.sync", "msd.titles.*", "msd.*"))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests(onlinePlayerSuggestions())
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        OfflinePlayer target = resolveTarget(ctx);
                        if (!isTargetValid(target)) {
                            sender.sendMessage("<red>Unknown player: <yellow>" + ctx.getArgument("target", String.class));
                            return 0;
                        } try {
                            titleManager.syncLuckPermsPrefixAsync(target.getUniqueId());
                            Msg.miniMsg("<dark_aqua>Synced <gold>" + target.getName() + "<dark_aqua>'s <yellow>LuckPerms Prefix <dark_aqua>with their <yellow>Active Title.", sender);
                            return 1;
                        } catch (Exception e) {
                            Msg.miniMsg("<red>An error occurred while retrieving the title: <yellow>" + e.getMessage(), sender);
                            return 0;
                        }
                    })))
            .then(Commands.literal("player")
                .requires(requirePermission("msd.titles.player", "msd.titles.*", "msd.*"))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests(onlinePlayerSuggestions())
                    .then(Commands.literal("active")
                        .requires(requirePermission("msd.titles.player.active", "msd.titles.player.*", "msd.titles.*", "msd.*"))
                        .then(Commands.argument("name", StringArgumentType.string())
                            .suggests(this::suggestTitleNames)
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
                                        titleManager.setActivePrefix(target.getUniqueId(), title.id());
                                        Msg.miniMsg("<dark_aqua>Successfully set <gold>" + target.getName() + "<dark_aqua>'s active title to <gold>\"<yellow>" + name + "<gold>\" <dark_aqua>ID <i><dark_gray>" + title.id() + "</i>", sender);
                                    } else {
                                        Msg.miniMsg("<yellow>No title with the name <gold>\"<yellow>" + name + "<gold>\"<yellow>exists.", sender);
                                    }
                                    return 1;
                                } catch (Exception e) {
                                    Msg.miniMsg("<red>An error occurred while retrieving the title: <yellow>" + e.getMessage(), sender);
                                    return 0;
                                }
                            })))
                    .then(Commands.literal("give")
                        .requires(requirePermission("msd.titles.player.give", "msd.titles.player.*", "msd.titles.*", "msd.*"))
                        .then(Commands.argument("name", StringArgumentType.string())
                            .suggests(this::suggestTitleNames)
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
                                        titleManager.giveTitle(target.getUniqueId(), title.id());
                                        Msg.miniMsg("<dark_aqua>Gave the <gold>\"<yellow>" + name + "<gold>\" <dark_aqua>title to <gold>" + target.getName() + "<dark_aqua>, ID <i><dark_gray>" + title.id() + "</i>", sender);
                                    } else {
                                        Msg.miniMsg("<yellow>No title with the name <gold>\"<yellow>" + name + "<gold>\"<yellow>exists.", sender);
                                    }
                                    return 1;
                                } catch (SQLException e) {
                                    Msg.miniMsg("<red>An error occurred while giving the title: <yellow>" + e.getMessage(),sender);
                                    return 0;
                                }
                            })))
                    .then(Commands.literal("revoke")
                        .requires(requirePermission("msd.titles.player.give", "msd.titles.player.*", "msd.titles.*", "msd.*"))
                        .then(Commands.argument("name", StringArgumentType.string())
                            .suggests(this::suggestTitleNames)
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
                                        titleManager.revokeTitle(target.getUniqueId(), title.id());
                                        Msg.miniMsg("<dark_aqua>Revoked the <gold>\"<yellow>" + name + "<gold>\" <dark_aqua>title from <gold>" + target.getName() + "<dark_aqua>, ID <i><dark_gray>" + title.id() + "</i>",sender);
                                    } else {
                                        Msg.miniMsg("<yellow>No title with the name <gold>\"<yellow>" + name + "<gold>\"<yellow>exists.", sender);
                                    }
                                    return 1;
                                } catch (SQLException e) {
                                    Msg.miniMsg("<red>An error occurred while revoking the title: <yellow>" + e.getMessage(), sender);
                                    return 0;
                                }
                            }))))).build();
    }

    private static String normalizePrefixArg(String raw) {
        if (raw == null) return null;

        String s = raw;

        // Strip wrapping quotes: "..." or '...'
        if (s.length() >= 2) {
            char first = s.charAt(0);
            char last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                s = s.substring(1, s.length() - 1);
            }
        }

        // Explicit trailing space token
        if (s.endsWith("\\s")) {
            s = s.substring(0, s.length() - 2) + " ";
        }

        return s;
    }

    private boolean isTargetValid(OfflinePlayer target) {
        return target != null && (target.hasPlayedBefore() || target.isOnline());
    }

    private int commandGui(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (sender instanceof Player) {
            titleMenu.open((Player) sender, 0);
        } else {
            sender.sendMessage("This GUI can only be opened by a player.");
        }
        return 1;
    }

    protected OfflinePlayer resolveTarget(CommandContext<CommandSourceStack> ctx) {
        String name = ctx.getArgument("target", String.class);
        if (name == null) return null;
        OfflinePlayer onlinePlayer = Bukkit.getPlayerExact(name);
        if (onlinePlayer != null) return onlinePlayer;
        return Bukkit.getOfflinePlayer(name);
    }
}
