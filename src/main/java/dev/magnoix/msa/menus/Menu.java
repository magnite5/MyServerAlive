package dev.magnoix.msa.menus;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Menu {
    private static final Map<UUID, Menu> openMenus = new HashMap<>();
    private static final Map<String, Set<UUID>> viewers = new HashMap<>();

    private final Map<Integer, MenuClick> menuClickActions = new HashMap<>(); // Int: Slot index, MenuClick: Action to perform

    private MenuClick generalClickAction;
    private MenuClick generalInvClickAction;
    private MenuDrag generalDragAction;

    private MenuOpen openAction;
    private MenuClose closeAction;

    public final UUID uuid;
    private final Inventory inventory;
    private final String viewerID;

    public Menu(int size, Component name) {
        uuid = UUID.randomUUID();
        inventory = Bukkit.createInventory(null, size, name);
        viewerID = null;
    }
    public Menu(int size, Component name, String viewerID) {
        uuid = UUID.randomUUID();
        inventory = Bukkit.createInventory(null, size, name);
        this.viewerID = viewerID;
    }

    public MenuClick getGeneralClickAction() { return generalClickAction; }
    protected void setGeneralClickAction(MenuClick generalClickAction) { this.generalClickAction = generalClickAction; }

    public MenuClick getGeneralInvClickAction() { return generalInvClickAction; }
    protected void setGeneralInvClickAction(MenuClick generalInvClickAction) { this.generalInvClickAction = generalInvClickAction; }

    public MenuDrag getGeneralDragAction() { return generalDragAction; }
    protected void setGeneralDragAction(MenuDrag generalDragAction) { this.generalDragAction = generalDragAction; }

    protected void setOpenAction(MenuOpen openAction) { this.openAction = openAction; }
    protected void setCloseAction(MenuClose closeAction) { this.closeAction = closeAction; }

    public interface MenuClick { void click(Player p, InventoryClickEvent event); }
    public interface MenuDrag { void drag(Player p, InventoryDragEvent event); }
    public interface MenuOpen { void open(Player p); }
    public interface MenuClose { void close(Player p); }

    public void setItem(int index, ItemStack item) { inventory.setItem(index, item); }
    public void setItem(int index, ItemStack item, MenuClick action) {
        inventory.setItem(index, item);
        if (action == null) menuClickActions.remove(item);
        else menuClickActions.put(index,action);
    }
}
