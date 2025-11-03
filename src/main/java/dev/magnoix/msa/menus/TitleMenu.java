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
import java.util.logging.Level;

public class TitleMenu { //todo: Un-italicise everything
    private static final int MENU_SIZE = 45; // 5 rows
    private static final int PAGE_SIZE = 27;
    private static final int TITLE_START_SLOT = 9;
    private TitleManager titleManager;

    private MiniMessage mm = MiniMessage.miniMessage();

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

        Menu menu = new Menu(MENU_SIZE, Component.text("Titles - Page " + (activePage + 1) + "/" + (maxPage + 1)).color(NamedTextColor.DARK_AQUA));
        menu.setBackground(backgroundItem());

        for (int i = 0; i < TITLE_START_SLOT; i++) {
            menu.setItem(i, headerItem());
        }
        menu.setItem(4, infoButton(activeTitle, titleCount, uuid));
        for (int i = 0; i < pageTitles.size(); i++) {
            TitleManager.title title = pageTitles.get(i);
            Boolean isActive = (title.id() == activeTitleId);
            menu.setItem(i + TITLE_START_SLOT, getTitleItem(title, isActive), (p, event) -> {
                try {
                    if (isActive) {
                        TitleManager.title defaultTitle = titleManager.getTitleFromName("default");
                        if (defaultTitle != null) {
                            titleManager.setActivePrefix(p.getUniqueId(), defaultTitle.id());
                            Msg.miniMsg("<green>Your title has been unequipped.", p);
                        } else {
                            titleManager.setActivePrefix(p.getUniqueId(), -1);
                            Msg.miniMsg("<yellow>No default title found; no title is equipped. <u>Please notify an admin of this issue.", p);
                        }
                    } else {
                        titleManager.setActivePrefix(p.getUniqueId(), title.id());
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
        try {
            return ItemCreator.create(
                Material.NAME_TAG,
                isActive
                    ? Component.text(title.name()).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
                    : Component.text(title.name()).decoration(TextDecoration.ITALIC, false),
                List.of(
                    mm.deserialize("<!i><gray>").append(titleManager.getFormattedPrefix(title)),
                    Component.text(""),
                    (isActive)
                        ? mm.deserialize("<u><!i><red>Click</u> <!i><red>to Unequip")
                        : mm.deserialize("<u><!i><yellow>Click</u> <!i><yellow>to Equip"),
                    mm.deserialize("<dark_gray><i>ID: " + title.id())
                ),
                isActive
            );
        } catch (SQLException e) {
            Msg.log(Level.SEVERE, "An error occurred while accessing the database to build a title item.");
            return ItemCreator.errorItem();
        }
    }
    private ItemStack backgroundItem() { return ItemCreator.create(Material.GRAY_STAINED_GLASS_PANE, Component.text(" ").decoration(TextDecoration.ITALIC, false)); }
    private ItemStack headerItem() { return ItemCreator.create(Material.BLUE_STAINED_GLASS_PANE, Component.text(" ").decoration(TextDecoration.ITALIC, false)); }
    private ItemStack fillerItem() { return ItemCreator.create(Material.BLUE_STAINED_GLASS_PANE, Component.text(" ").decoration(TextDecoration.ITALIC, false)); }
    private ItemStack infoButton(TitleManager.title activeTitle, int titleCount, UUID uuid) {
        return ItemCreator.skull(
            uuid,
            mm.deserialize("<!i><gold>Title Info"),
            List.of(
                mm.deserialize("<!i><dark_aqua>Current Title: "),
                mm.deserialize("<!i><dark_aqua> » <gold>" + activeTitle.name()),
                Component.text(""),
                mm.deserialize("<!i><dark_aqua>You have <aqua>" + titleCount + " <dark_aqua>title" + (titleCount == 1 ? "." : "s."))

            ));
    }
    private ItemStack backButton()  { return ItemCreator.create(Material.ARROW, Component.text("Previous Page").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)); }
    private ItemStack nextButton()  { return ItemCreator.create(Material.ARROW, Component.text("Next Page").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)); }
    private ItemStack closeButton() { return ItemCreator.create(Material.BARRIER, Component.text("Close").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)); }
}
