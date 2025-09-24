package dev.magnoix.msa.commands;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.messages.MailManager;
import dev.magnoix.msa.messages.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;

import java.util.Collection;
import java.util.UUID;


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
                            id = profile.getId();
                            name = profile.getName();
                        }
                            if (name != null && id != null) Msg.safeWhisper(id, new MailManager.MailEntry(name, message));
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
                .build();
    }

}
