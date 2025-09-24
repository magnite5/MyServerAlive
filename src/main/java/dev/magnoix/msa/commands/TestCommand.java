package dev.magnoix.msa.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.helpers.ItemCreator;
import dev.magnoix.msa.menus.Menu;
import dev.magnoix.msa.messages.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class TestCommand {

    public static LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("testgui")
                .then(Commands.argument("rows", IntegerArgumentType.integer()))
                .executes(ctx -> {
                    int rows = ctx.getArgument("rows", int.class);
                    if (!(rows >= 1 && rows <= 6)) {
                        Msg.msg("Invalid Rows!", (Player) ctx.getSource().getSender());
                        return 0;
                    }
                    Menu menu = new Menu(
                            rows,
                            Component.text("Test Command GUI").color(TextColor.color(255, 0, 52))
                    );
                    for (int i = 0; menu.getSize() >= i; i++) {
                        int finalI = i;
                        menu.setItem(
                                i,
                                ItemCreator.create(Material.RED_STAINED_GLASS_PANE,
                                        Component.text(Integer.toString(i))), (p, e) -> {
                                    p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                                    Msg.msg("You clicked Slot " + finalI, p);
                        });
                    }
                    return 1;
                }).build();
    }
}
