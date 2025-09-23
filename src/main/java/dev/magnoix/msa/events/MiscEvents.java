package dev.magnoix.msa.events;

import dev.magnoix.msa.menus.Menu;
import dev.magnoix.msa.menus.TestMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class MiscEvents implements Listener {
    @EventHandler
    private void playerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        new TestMenu().Open(event.getPlayer());
    }

    @EventHandler
    private void inventoryDrag(InventoryDragEvent event) {
        Player p = (Player) event.getWhoClicked();
        Menu menu = Menu.getMenu(p);
        if (menu != null) {
            event.setCancelled(true);
            if (menu.getGeneralDragAction() != null) menu.getGeneralDragAction().drag(p, event);
        }
    }

    @EventHandler
    private void inventoryClick(InventoryClickEvent event) {
        Player p = (Player) event.getWhoClicked();
        Menu menu = Menu.getMenu(p);
        if (menu != null) {
            event.setCancelled(true);
            if (event.getClickedInventory() != null) {
                if (event.getRawSlot() > event.getClickedInventory().getSize()) {
                    if (menu.getGeneralInvClickAction() != null) menu.getGeneralClickAction().click(p, event); // Click in own inventory.
                } else if (menu.getGeneralClickAction() != null) menu.getGeneralClickAction().click(p, event); // Click in open menu.
            }

            Menu.MenuClick menuClick = menu.getClickAction(event.getRawSlot());
            if (menuClick != null) menuClick.click(p, event);
        }
    }

    @EventHandler
    private void inventoryClose(InventoryCloseEvent event) {
        Player p = (Player) event.getPlayer();
        Menu menu = Menu.getMenu(p);
        if (menu != null) menu.remove();
    }
}
