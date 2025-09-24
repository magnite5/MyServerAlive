package dev.magnoix.msa.messages;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;


public class MessageCommand {

    // Java
    public static LiteralCommandNode<CommandSourceStack> playerArgument() {
        return Commands.literal("message")
            .then(Commands.argument("target", ArgumentTypes.playerProfiles())
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        PlayerProfileListResolver profilesResolver =
                                ctx.getArgument("target", PlayerProfileListResolver.class);
                        String message = ctx.getArgument("message", String.class);

                        Collection<PlayerProfile> targets = profilesResolver.resolve(ctx.getSource());

                        UUID id = null;
                        String name = null;
                        for (PlayerProfile profile : targets) {
                            // send your message here
//                            ctx.getSource().getSender().sendPlainMessage(
//                                    "Sent message to " + profile.getName() + ": " + message
//                            );
                            id = profile.getId();
                            name = profile.getName();
                        }
                        boolean online = false;
                        if (id != null) {
                            online = Bukkit.getPlayer(id) != null;
                        } else if (name != null) {
                            online = Bukkit.getPlayer(name) != null;
                        } else {
                            Msg.log(Level.SEVERE,"Failed to send message to player: " + name + ". Skipping.");
                            return Command.SINGLE_SUCCESS;
                        }
                        if (online) {
                            ctx.getSource().getSender().sendPlainMessage(
                                    "Sent message to " + name + ": " + message
                            );
                            Msg.msg(message, Bukkit.getPlayer(id));
                        }
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
                .build();
    }

}
