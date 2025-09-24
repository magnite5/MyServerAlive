package dev.magnoix.msa.menus;

import dev.magnoix.msa.messages.MailManager;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MailMenu extends Menu { //TODO: Finish everything ;-;
    private static final int GUI_SIZE = 54;
    private static final int ENTRIES_PER_PAGE = 36;

    private static final int ENTRIES_START_SLOT = 9;
    private static final int ENTRIES_END_SLOT = ENTRIES_START_SLOT + ENTRIES_PER_PAGE;

    private final List<ItemStack> itemEntries;
    private final List<MailManager.MailEntry> mailEntries;
    private int page = 0;

    public MailMenu(List<MailManager.MailEntry> mailEntries, Component title) {
        super(GUI_SIZE, title);
        this.mailEntries = mailEntries;
    }

    private void buildPage(int page) {
        this.page = page;

        getInventory().clear();

        int start = page * ENTRIES_PER_PAGE; // Where the page begins
        int end = Math.min(itemEntries.size(), start + ENTRIES_PER_PAGE); // Where the page ends
        int slot = ENTRIES_START_SLOT; // Where the page's first entry is

        for (int i = start; i < end; i++) {
            setItem(slot++, itemEntries.get(i), (player, event) -> {

            });
        }

    }
}
