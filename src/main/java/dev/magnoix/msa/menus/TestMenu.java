package dev.magnoix.msa.menus;

import dev.magnoix.msa.helpers.ItemCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;

import java.util.Map;

public class TestMenu extends Menu {

    public TestMenu() {
        super(9, MiniMessage.miniMessage().deserialize("<red>Test Menu"), "example");
        setItem(0, ItemCreator.create(Material.BLACK_STAINED_GLASS_PANE, Component.empty(), false, Map.of()));
    }


}
