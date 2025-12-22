package dev.magnoix.msa.menus;

import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.logging.FileLogger;
import dev.magnoix.msa.messages.Msg;
import dev.magnoix.msa.utils.ItemCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConversionMenu {
    private final JavaPlugin plugin;
    private final Logger logger;

    private final StatisticsManager statisticsManager;
    private final FileLogger statsLogger;

    private final NamespacedKey ITEM_TYPE_KEY;

    private final static MiniMessage mm = MiniMessage.miniMessage();

    public ConversionMenu(StatisticsManager statisticsManager, JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.statisticsManager = statisticsManager;
        this.statsLogger = statisticsManager.getLogger();
        this.ITEM_TYPE_KEY = new NamespacedKey(plugin, "item_type");
    }

    public void open(Player player) {
        UUID uuid = player.getUniqueId();
        try {
            int nw = statisticsManager.getStatistic(uuid, "networth");
        } catch (SQLException e) {
            Msg.miniMsg("An error occurred while retrieving your networth.", player);
        }
        Menu menu = new Menu(6, Component.text("Networth Conversion Menu"));
        // Divider Items
        for (int i = 0; i < 4; i++) {
            menu.setColumn(i, greenBackgroundItem());
            menu.setColumn(i + 5, redBackgroundItem());
        }
        menu.setColumn(4, borderItem());
        menu.setRow(0, borderItem());
        menu.setRow(8, borderItem());
        // Display Items
        menu.setItem(4, ItemCreator.create())

        // Interaction Items
    }

    private NamespacedKey getNamespacedKey(String type) {
        return new NamespacedKey(plugin, type);
    }

    /// ITEM MANAGEMENT

    private String getItemType(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        return meta.getPersistentDataContainer().get(ITEM_TYPE_KEY, PersistentDataType.STRING);
    }
    private int countItems(Player player, String type) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            String itemType = item.getItemMeta().getPersistentDataContainer().get(ITEM_TYPE_KEY, PersistentDataType.STRING);
            if (type.equalsIgnoreCase(itemType)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private int depositItems(Player player, String type, int amount) {
        if (amount <= 0) return 0;
        UUID uuid = player.getUniqueId();
        // Item Logic
        Map<Integer, ItemStack> modifiedStacks = new HashMap<>(); // Index, ItemStack in case of nw errors

        int count = countItems(player, type);
        if (count < amount) {
            Msg.miniMsg("<red>You do not have enough items to perform the operation.", player);
            return 0;
        }

        int remaining = amount; // The # of items left to remove.
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            String itemType = getItemType(item);

            if (type.equalsIgnoreCase(itemType)) {
                modifiedStacks.put(i, item.clone());
                int itemAmount = item.getAmount();
                // Perform actual removal
                if (itemAmount <= remaining) { // Whole ItemStack is needed or is smaller than needed amt
                    remaining -= itemAmount;
                    player.getInventory().setItem(i, null);
                } else { // ItemStack is larger than needed
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
            if (remaining == 0) break;
        }
        // NW Logic
        int profit = getTypeValue(type) * count;
        try {
            int newBalance = statisticsManager.addToStatistic(uuid, "networth", profit);
            Msg.miniMsg("<dark_aqua>- <aqua>" + count + "<dark_aqua> Item " + (count == 1 ? " " : "s ") +
                "; + <aqua>" + profit + "<dark_aqua>NW" +
                "; New Balance: <aqua>" + newBalance + "<dark_aqua>.", player);
            return newBalance;
        } catch (SQLException e) {
            // Revert changes
            modifiedStacks.forEach((index, itemStack) -> player.getInventory().setItem(index, itemStack));

            Msg.miniMsg("<red>An error occurred while updating your networth. Your items have been returned.", player);
            statisticsManager.logIfLogged("networth", "Failed a " + count + " " + type + " deposit for " + player.getName() + ", worth " + profit);
            Msg.log(Level.SEVERE, "Failed NW deposit for " + player.getName() + ": " + e.getMessage());
            return 0;
        }
    }

    public int depositAll(Player player) {
        UUID uuid = player.getUniqueId();
        int profit = 0; // Total networth gain
        int count = 0;  // # of items deposited

        // Item Logic
        ItemStack[] contents = player.getInventory().getContents();

        Map<Integer, ItemStack> removedItems = new HashMap<>(); // Index, ItemStack in case of nw errors.

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            String itemType = getItemType(item);

            int typeValue = getTypeValue(itemType);
            if (typeValue > 0) {
                int amount = item.getAmount();
                count += amount;
                profit += (typeValue * amount);

                removedItems.put(i, item.clone());
                player.getInventory().setItem(i, null);
            }
        }
        // NW Logic
        if (profit > 0) {
            try {
                int newBalance = statisticsManager.addToStatistic(uuid, "networth", profit) + profit;
                Msg.miniMsg("<dark_aqua>- <aqua>" + count + "<dark_aqua> Item" + (count == 1 ? " " : "s ") +
                    "; + <aqua>" + profit + " <dark_aqua>NW" +
                    "; New Balance: <aqua>" + newBalance + "<dark_aqua>.", player);
                return newBalance;
            } catch (SQLException e) {
                // Rollback to prevent item loss
                removedItems.forEach((slot, stack) -> player.getInventory().setItem(slot, stack));

                Msg.miniMsg("<red>An error occurred while updating your networth. Your items have been returned.", player);
                statisticsManager.logIfLogged("networth", "Failed a " + count + " item deposit for " + player.getName() + ", worth " + profit);
                Msg.log(Level.SEVERE, "Failed NW deposit for " + player.getName() + ": " + e.getMessage());
            }
        }
        return 0;
    }

    private int withdrawItems(Player player, String type, int amount) {
        UUID uuid = player.getUniqueId();
        int typeValue = getTypeValue(type);

        int nw;
        try {
            nw = statisticsManager.getStatistic(uuid, "networth");
        } catch (SQLException e) {
            Msg.miniMsg("<red>An error occurred while retrieving your networth.", player);
            Msg.log(Level.SEVERE, "An error occurred while retrieving " + player.getName() + "'s networth: " + e.getMessage());
            return 0;
        }

        int cost = typeValue * amount;
        if (cost > nw) {
            Msg.miniMsg("<dark_aqua>You cannot afford <aqua>" + amount + " <yellow>" + getTypeDisplay(type, amount) , player);
            return 0;
        }
        int freeSpace = getFreeSpace(player, type);
        if (freeSpace < amount) {
            Msg.miniMsg("<dark_aqua>Your inventory is too full. Please make some space.", player);
            return 0;
        }

        ItemStack item = sampleItem(type);
        int itemsLeft = amount; // Amount of items left to be given
        int newBalance;
        try {
            newBalance = statisticsManager.addToStatistic(uuid, "networth", -cost) - cost;
        } catch (SQLException e) {
            Msg.miniMsg("<red>An error occurred while updating your networth.", player);
            Msg.log(Level.SEVERE, "An error occurred while retrieving " + player.getName() + "'s networth: " + e.getMessage());
            return 0;
        }

        while (itemsLeft > 0) {
            ItemStack stack = item.clone();
            int stackSize = Math.min(itemsLeft, stack.getMaxStackSize());
            stack.setAmount(stackSize);

            player.getInventory().addItem(stack);
            itemsLeft -= stackSize;
        }

        String displayType = getTypeDisplay(type, amount);
        Msg.miniMsg("<dark_aqua>+ <aqua>" + amount + " <dark_aqua> <yellow>" + displayType +
            "<dark_aqua>; - <aqua>" + cost + " <dark_aqua>NW" +
            "; New Balance: <aqua>" + newBalance,
            player);
        return newBalance;
    }

    private int withdrawMax(Player player, String type) {
        int typeValue = getTypeValue(type);

        int nw;
        try {
            nw = statisticsManager.getStatistic(player.getUniqueId(), "networth");
        } catch (SQLException e) {
            Msg.miniMsg("<red>An error occurred while retrieving your networth.", player);
            Msg.log(Level.SEVERE, "An error occurred while retrieving " + player.getName() + "'s networth: " + e.getMessage());
            return 0;
        }

        // Calculate # of items affordable by player
        int canAfford = nw / typeValue;
        if (canAfford <= 0) {
            Msg.miniMsg("<dark_aqua>You cannot afford any " + getTypeDisplay(type, 2) + ".", player);
        }
        // Calculate # of items that can fit in player's inventory
        int canFit = getFreeSpace(player, type);
        if (canFit <= 0) {
            Msg.miniMsg("<dark_aqua>Your inventory is full. Please make some space.", player);
        }
        // Withdraw bottleneck amount, return new balance
        int max = Math.min(canAfford, canFit);
        return withdrawItems(player, type, max);
    }

    private int getTypeValue(String type) {
        return switch (type.toUpperCase()) {
            case "KELP" -> 1024;
            case "PICKLE" -> 2048;
            case "CREDIT", "COMPRESSED_KELP" -> 4096;
            default -> 0;
        };
    }

    private String getTypeDisplay(String type, int amount) {
        return type.toLowerCase().replaceAll("_", " ") + (amount == 1 ? "" : "s");
    }
    private String getTypeDisplay(String type) {
        return type.toLowerCase().replaceAll("_", " ");
    }

    private int getFreeSpace(Player player, String type) {
        int count = 0;
        ItemStack[] contents = player.getInventory().getContents();

        for (ItemStack item : contents) {
            if (item.getType().equals(Material.AIR)) {
                count += 64;
            }
            if (getItemType(item).equals(type)) {
                count += item.getMaxStackSize() - item.getAmount();
            }
        }
        return count;
    }

    public ItemStack sampleItem(String type) {
        ItemStack item = switch (type.toUpperCase()) {
            case "COMPRESSED_KELP" -> ItemCreator.create(
                Material.DRIED_KELP_BLOCK,
                mm.deserialize("<green>Compressed Kelp"),
                List.of(),
                true);
            case "PICKLE" -> ItemCreator.create(Material.SEA_PICKLE);
            case "CREDIT" -> ItemCreator.create(
                Material.AMETHYST_BLOCK,
                mm.deserialize("<gold>Credit"),
                List.of(),
                true);
            default -> ItemCreator.create(Material.DRIED_KELP_BLOCK);
        };

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(ITEM_TYPE_KEY, PersistentDataType.STRING, type.toUpperCase());
        item.setItemMeta(meta);
        return item;
    }

    ///  MENU ITEMS
    private ItemStack borderItem() { return ItemCreator.create(Material.GRAY_STAINED_GLASS_PANE, Component.text(" ").decoration(TextDecoration.ITALIC, false)); }
    private ItemStack greenBackgroundItem() { return ItemCreator.create(Material.GREEN_STAINED_GLASS_PANE, Component.text(" ").decoration(TextDecoration.ITALIC, false)); }
    private ItemStack redBackgroundItem() { return ItemCreator.create(Material.RED_STAINED_GLASS_PANE, Component.text(" ").decoration(TextDecoration.ITALIC, false)); }
    private ItemStack closeButton() { return ItemCreator.create(Material.BARRIER, Component.text("Close").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)); }

    private ItemStack playerSkullItem(Player player, int balance) {
        return ItemCreator.skull(player.getUniqueId(), mm.deserialize("<gold>" + player.getName()), List.of(
            mm.deserialize(" <gold><b>» </b><dark_aqua>Net Worth: <aqua>" + balance)
        ));
    }

    private ItemStack depositItem(int maxAmount, String type, Material material, boolean isGlowing) {
        String typeDisplay = getTypeDisplay(type);
        int typeValue = getTypeValue(type);
        List<Component> lore = new ArrayList<>();

        lore.add(mm.deserialize(" <gold><b>» </b><dark_aqua>Left Click: Deposit <aqua>1"));
        lore.add(mm.deserialize(" <gold>└ For <aqua>" + typeValue));

        if (maxAmount >= 10) {
            lore.add(mm.deserialize(" <gold><b>» </b><dark_aqua>Right Click: Deposit <aqua>10"));
            lore.add(mm.deserialize(" <gold>└ For <aqua>" + typeValue * 10));
        }

        lore.add(mm.deserialize(" <gold><b>» </b><dark_aqua>Shift + Left Click: Deposit <aqua>ALL"));
        lore.add(mm.deserialize(" <gold>└ <aqua>" + maxAmount + " For <aqua>" + (typeValue * maxAmount)));

        return ItemCreator.create(material, mm.deserialize("<red><b>DEPOSIT </b><gold>| " + typeDisplay), lore, isGlowing);
    }
    private ItemStack withdrawItem(int maxAmount, String type, Material material, boolean isGlowing) {
        String typeDisplay = getTypeDisplay(type);
        int typeValue = getTypeValue(type);
        List<Component> lore = new ArrayList<>();

        lore.add(mm.deserialize(" <gold><b>» </b><dark_aqua>Left Click: Withdraw <aqua>1"));
        lore.add(mm.deserialize(" <gold>└ For <aqua>" + typeValue));

        if (maxAmount >= 10) {
            lore.add(mm.deserialize(" <gold><b>» </b><dark_aqua>Right Click: Withdraw <aqua>10"));
            lore.add(mm.deserialize(" <gold>└ For <aqua>" + typeValue * 10));
        }

        lore.add(mm.deserialize(" <gold><b>» </b><dark_aqua>Shift + Left Click: Withdraw <aqua>ALL"));
        lore.add(mm.deserialize(" <gold>└ <aqua>" + maxAmount + " For <aqua>" + (typeValue * maxAmount)));

        return ItemCreator.create(material, mm.deserialize("<red><b>WITHDRAW </b><gold>| " + typeDisplay), lore, isGlowing);
    }
}
