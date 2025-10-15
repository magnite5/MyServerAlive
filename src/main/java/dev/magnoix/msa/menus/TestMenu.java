package dev.magnoix.msa.menus;

import dev.magnoix.msa.utils.ItemCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TestMenu extends Menu {

    public TestMenu() {
        super(9, MiniMessage.miniMessage().deserialize("<red>Test Menu"), "example");
        setBackground(ItemCreator.create(Material.CYAN_STAINED_GLASS_PANE, Component.empty(), List.of(Component.empty())));

        setItem(0, ItemCreator.create(Material.BLACK_STAINED_GLASS_PANE, MiniMessage.miniMessage().deserialize("<red><bold>Cool Item</bold>"), List.of(Component.text("Line One"), Component.text("Line Two")), true));
        setItem(1, ItemCreator.tool(Material.NETHERITE_SWORD, Component.text("Sword"), List.of(Component.text("Click to say 'Sword'")), true), (p, e) -> {
            p.sendMessage(Component.text("Sword Clicked!"));
            ItemStack item = e.getCurrentItem();
            if (item.getType() == Material.NETHERITE_SWORD) item = ItemCreator.updateMaterial(Material.DIAMOND_SWORD, item);

            for (Player viewer : getViewers()) {
                Menu m = Menu.getMenu(viewer);
                if (m == null) continue;
                if (viewer.equals(p)) {
                    m.setItem(e.getRawSlot(), ItemCreator.create(Material.DIRT));
                    continue;
                }
                m.setItem(e.getRawSlot(), item);
            }
        });

        setOpenAction((p) -> p.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1));
        setCloseAction((p) -> p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1));
    }


}
