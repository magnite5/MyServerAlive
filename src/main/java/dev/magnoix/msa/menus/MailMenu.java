package dev.magnoix.msa.menus;

import dev.magnoix.msa.helpers.ItemCreator;
import dev.magnoix.msa.helpers.TextUtils;
import dev.magnoix.msa.messages.MailManager;
import dev.magnoix.msa.messages.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MailMenu extends Menu {

    private static final int GUI_SIZE = 54;
    private static final int ENTRIES_PER_PAGE = 36;
    private static final int ENTRIES_START_SLOT = 9; // Start after top row
    private static final int NAV_PREVIOUS_SLOT = 45;
    private static final int NAV_NEXT_SLOT = 53;

    private final List<MailManager.MailEntry> mailEntries;
    private final List<ItemStack> itemEntries = new ArrayList<>();
    private int page = 0;

    public MailMenu(List<MailManager.MailEntry> mailEntries, Component title) {
        super(GUI_SIZE, title);
        this.mailEntries = mailEntries;
        buildItemEntries();
        buildPage(page);
    }

    // Convert MailEntry into ItemStack
    private void buildItemEntries() {
        itemEntries.clear();
        for (MailManager.MailEntry mail : mailEntries) {
//            ItemStack item = new ItemStack(Material.PAPER);
//            ItemMeta meta = item.getItemMeta();
//            if (meta != null) {
//                meta.displayName(Component.text(mail.getTitle()));
//                item.setItemMeta(meta);
//            }
//            itemEntries.add(item);
            ItemStack item;
            List<Component> lore = TextUtils.stringToLore(mail.message(), 30, Component.text("Message: "));
            if (mail.sender().equals("Server"))
                item = ItemCreator.create(Material.NETHER_STAR, Component.text("Server"), lore);
            else {
                item = ItemCreator.skull() // TODO: Requires refactoring of MailEntry.

            }
        }
    }

    // Build a specific page
    private void buildPage(int page) {
        this.page = page;

        getInventory().clear();

        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(itemEntries.size(), start + ENTRIES_PER_PAGE);
        int slot = ENTRIES_START_SLOT;

        for (int i = start; i < end; i++) {
            final int index = i; // For lambda capture
            setItem(slot++, itemEntries.get(i), (player, event) -> onMailClick(player, mailEntries.get(index), event));
        }

        // Navigation buttons
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            setItem(NAV_PREVIOUS_SLOT, prev, (player, event) -> buildPage(page - 1));
        }

        if (end < itemEntries.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            setItem(NAV_NEXT_SLOT, next, (player, event) -> buildPage(page + 1));
        }
    }

    // Called when a mail item is clicked
    private void onMailClick(Player player, MailManager.MailEntry mail, InventoryClickEvent event) {
        event.setCancelled(true); // prevent item pickup
        Msg.msg("You opened a Message from " + mail.sender() + ": ", player);
        Msg.msg(" » " + mail.message(), player);
        // TODO: add opening mail or whatever behavior you want
    }
}
