package dev.magnoix.msa.menus;

import dev.magnoix.msa.databases.TitleManager;
import dev.magnoix.msa.messages.Msg;
import dev.magnoix.msa.utils.ItemCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class TitleMenu { //todo: Un-italicise everything
    private static final int MENU_SIZE = 45; // 5 rows
    private static final int PAGE_SIZE = 27;
    private static final int TITLE_START_SLOT = 9;
    private TitleManager titleManager;

    public TitleMenu(TitleManager titleManager) {
        this.titleManager = titleManager;
    }

    public void open(Player player, int page) {
        List<TitleManager.title> titles;
        UUID uuid = player.getUniqueId();
        TitleManager.title activeTitle;
        int titleCount, activeTitleId;
        try {
            titles = titleManager.getTitles(uuid);
            titleCount = titleManager.getTitleCount(uuid);
            activeTitle = titleManager.getActiveTitle(uuid);
            if (activeTitle == null) activeTitle = titleManager.getTitleFromName("default");
            activeTitleId = -1;
            if (activeTitle != null) activeTitleId = activeTitle.id();
        } catch (SQLException e) {
            Msg.miniMsg("<red>An error occurred while retrieving your titles.", player);
            e.printStackTrace();
            return;
        }
        int maxPage = (titles.isEmpty()) ? 0 : (titles.size() - 1) / PAGE_SIZE;
        int activePage = Math.max(0, Math.min(page, maxPage));
        int pageStart = activePage * PAGE_SIZE;
        int pageEnd = Math.min(pageStart + PAGE_SIZE, titles.size());
        List<TitleManager.title> pageTitles = titles.subList(pageStart, pageEnd);

        Menu menu = new Menu(MENU_SIZE, Component.text("Your Titles - Page " + (activePage + 1) + "/" + (maxPage + 1)).color(NamedTextColor.GOLD));

        for (int i = 0; i < TITLE_START_SLOT; i++) {
            menu.setItem(i, headerItem());
        }
        menu.setItem(4, infoButton(activeTitle, titleCount));
        for (int i = 0; i < pageTitles.size(); i++) {
            TitleManager.title title = pageTitles.get(i);
            Boolean isActive = (title.id() == activeTitleId);
            menu.setItem(i + TITLE_START_SLOT, getTitleItem(title, isActive), (p, event) -> {
                try {
                    if (isActive) {
                        TitleManager.title defaultTitle = titleManager.getTitleFromName("default");
                        if (defaultTitle != null) {
                            titleManager.setActiveTitle(p.getUniqueId(), defaultTitle.id());
                            Msg.miniMsg("<green>Your title has been unequipped.", p);
                        } else {
                            titleManager.setActiveTitle(p.getUniqueId(), -1);
                            Msg.miniMsg("<yellow>No default title found; no title is equipped. <u>Please notify an admin of this issue.", p);
                        }
                    } else {
                        titleManager.setActiveTitle(p.getUniqueId(), title.id());
                        Msg.miniMsg("<dark_aqua>Equipped Title: <gold>" + title.name(), p);
                    }
                    menu.Close(p);
                } catch (SQLException e) {
                    Msg.miniMsg("<red>An error occurred while setting your active title.", p);
                    e.printStackTrace();
                }
            });
        }

        // Buttons
        if (activePage > 0) menu.setItem(36, backButton(), (p, event) -> open(p, activePage - 1));
        else menu.setItem(36, fillerItem());
        if (activePage < maxPage) menu.setItem(44, nextButton(), (p, event) -> open(p, activePage + 1));
        else menu.setItem(44, fillerItem());
        menu.setItem(40, closeButton(), (p, event) -> menu.Close(p));
        // Remaining nav-bar
        for (int i = 37; i < 44; i++) {
            if (i != 40) menu.setItem(i, fillerItem());
        }

        menu.Open(player);
    }

    private ItemStack getTitleItem(TitleManager.title title, boolean isActive) {
        return ItemCreator.create(
            Material.NAME_TAG,
            isActive ? Component.text(title.name()).color(NamedTextColor.GREEN) : Component.text(title.name()),
            List.of(
                MiniMessage.miniMessage().deserialize(title.prefix()),
                Component.text(" "),
                isActive ?
                    Component.text("Currently Equipped. Click to Unequip")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.UNDERLINED) :
                    Component.text("Click to Equip")
                        .color(NamedTextColor.YELLOW)
                        .decorate(TextDecoration.UNDERLINED),
                Component.text("ID: " + title.id()).color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC)
            ),
            isActive
        );
    }

    private ItemStack headerItem() { return ItemCreator.create(Material.LIGHT_BLUE_STAINED_GLASS_PANE, Component.text(" ")); }
    private ItemStack fillerItem() { return ItemCreator.create(Material.GRAY_STAINED_GLASS_PANE, Component.text(" ")); }
    private ItemStack infoButton(TitleManager.title activeTitle, int titleCount) {
        return ItemCreator.skull(
            UUID.fromString("556e1ac9-edcb-4487-b77a-a8d87d9f7e1b"),
            Component.text("Title Info").color(NamedTextColor.GOLD),
            List.of(
                Component.text("Current Title: ").color(NamedTextColor.DARK_AQUA),
                Component.text(activeTitle.name()),
                Component.text(""),
                Component.text("Title Count: " + titleCount)
            ));
    }
    private ItemStack backButton()  { return ItemCreator.create(Material.ARROW, Component.text("Previous Page").color(NamedTextColor.YELLOW)); }
    private ItemStack nextButton()  { return ItemCreator.create(Material.ARROW, Component.text("Next Page").color(NamedTextColor.GREEN)); }
    private ItemStack closeButton() { return ItemCreator.create(Material.BARRIER, Component.text("Close").color(NamedTextColor.RED)); }
}
