package dev.magnoix.msa.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.menus.Menu;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public class MailViewerCommand { //TODO: Make this work, requires MailMenu to be finished
    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("mail").executes(command -> {


            return 0;
        }).build()
    }
}
