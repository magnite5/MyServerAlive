package dev.magnoix.msa.menus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

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

    public void Open(Player player) {
        player.openInventory(inventory);
        openMenus.put(player.getUniqueId(), this);
        if(viewerID != null) {}
        if(openAction != null) openAction.open(player);
    }

    public void addViewer(Player player) {
        if(viewerID == null) return;
        Set<UUID> list = viewers.getOrDefault(viewerID, new HashSet<>());
        list.add(player.getUniqueId());
        viewers.put(viewerID, list);
    }

    public void removeViewer(Player player) {
        if (viewerID == null) return;
        Set<UUID> list = viewers.getOrDefault(viewerID, null);
        if (list == null) return;
        list.remove(player.getUniqueId());
        if (list.isEmpty()) viewers.remove(viewerID);
        else viewers.put(viewerID, list);
    }

    public Set<Player> getViewers() {
        if (viewerID == null) return new HashSet<>();
        Set<Player> viewerList = new HashSet<>();
        for(UUID uuid : viewers.getOrDefault(viewerID, new HashSet<>())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            viewerList.add(player);
        }
        return viewerList;
    }

    public MenuClick getClickAction(int index) { return menuClickActions.getOrDefault(index, null); }

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
