package dev.magnoix.msa.menus;

import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.messages.Msg;
import dev.magnoix.msa.utils.ItemCreator;
import dev.magnoix.msa.utils.TextUtils;
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

public class ConversionMenu {
    private final JavaPlugin plugin;

    private final StatisticsManager statisticsManager;

    private final NamespacedKey ITEM_TYPE_KEY;

    private final static MiniMessage mm = MiniMessage.miniMessage();

    public ConversionMenu(StatisticsManager statisticsManager, JavaPlugin plugin) {
        this.plugin = plugin;
        this.statisticsManager = statisticsManager;
        this.ITEM_TYPE_KEY = new NamespacedKey(plugin, "item_type");
    }

    public void open(Player player) {
        UUID uuid = player.getUniqueId();
        int nw = 0;
        try {
            nw = statisticsManager.getStatistic(uuid, "networth");
        } catch (SQLException e) {
            Msg.miniMsg("An error occurred while retrieving your networth.", player);
        }
        Menu menu = new Menu(36, Component.text("Networth Conversion Menu"));
        // Divider Items
        for (int i = 0; i < 4; i++) {
            menu.setColumn(i, greenBackgroundItem());
            menu.setColumn(i + 5, redBackgroundItem());
        }
        menu.setColumn(4, borderItem());
        // Display Items
        menu.setItem(4, playerSkullItem(player, nw));

        // Interaction Items
        menu.setItem(0, depositAllItem(player), (p, e) -> { // TODO: Reduce repeated operations
            depositAll(p);
            open(p);
        });
        menu.setItem(31, closeButton(), (p, e) -> p.closeInventory());
        // Deposit
        menu.setItem(10, depositItem(64, "KELP", Material.DRIED_KELP_BLOCK, false), (p, e) -> {
            if (e.getClick().isLeftClick()) depositItems(p, "KELP", 1);
            else depositItems(p, "KELP", 64);
            open(p);
        });
        menu.setItem(11, depositItem(64, "COMPRESSED_KELP", Material.DRIED_KELP_BLOCK, true), (p, e) -> {
            if (e.getClick().isLeftClick()) depositItems(p, "COMPRESSED_KELP", 1);
            else depositItems(p, "COMPRESSED_KELP", 64);
            open(p);
        });
        menu.setItem(19, depositItem(64, "PICKLE", Material.SEA_PICKLE, false), (p, e) -> {
            if (e.getClick().isLeftClick()) depositItems(p, "PICKLE", 1);
            else depositItems(p, "PICKLE", 64);
            open(p);
        });
        menu.setItem(20, depositItem(64, "COMPRESSED_PICKLE", Material.SEA_PICKLE, true), (p, e) -> {
            if (e.getClick().isLeftClick()) depositItems(p, "COMPRESSED_PICKLE", 1);
            else depositItems(p, "COMPRESSED_PICKLE", 64);
            open(p);
        });
        // Withdraw
        menu.setItem(15, withdrawItem(64, "KELP", Material.DRIED_KELP_BLOCK, false), (p, e) -> {
            if (e.getClick().isLeftClick()) withdrawItems(p, "KELP", 1);
            else withdrawItems(p, "KELP", 64);
            open(p);
        });
        menu.setItem(16, withdrawItem(64, "COMPRESSED_KELP", Material.DRIED_KELP_BLOCK, true), (p, e) -> {
            if (e.getClick().isLeftClick()) withdrawItems(p, "COMPRESSED_KELP", 1);
            else withdrawItems(p, "COMPRESSED_KELP", 64);
            open(p);
        });
        menu.setItem(24, withdrawItem(64, "PICKLE", Material.SEA_PICKLE, false), (p, e) -> {
            if (e.getClick().isLeftClick()) withdrawItems(p, "PICKLE", 1);
            else withdrawItems(p, "PICKLE", 64);
            open(p);
        });
        menu.setItem(25, withdrawItem(64, "COMPRESSED_PICKLE", Material.SEA_PICKLE, true), (p, e) -> {
            if (e.getClick().isLeftClick()) withdrawItems(p, "COMPRESSED_PICKLE", 1);
            else withdrawItems(p, "COMPRESSED_PICKLE", 64);
            open(p);
        });

        menu.Open(player);
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
    private int getItemValue(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        
        return getTypeValue(meta.getPersistentDataContainer().get(ITEM_TYPE_KEY, PersistentDataType.STRING)) * item.getAmount();
    }
    
    private int countItems(Player player, String type) {
        if (type == null) return 0;

        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;

            String itemType = getItemType(item);
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
            Msg.miniMsg("<red>Insufficient Items.", player);
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
        int profit = getTypeValue(type) * amount;
        try {
            int oldBalance = statisticsManager.addToStatistic(uuid, "networth", profit);
            int newBalance = oldBalance + profit;

            Msg.miniMsg("<dark_aqua>- <aqua>" + amount + "<dark_aqua> Item " + (amount == 1 ? " " : "s ") +
                "; + <aqua>" + profit + "<dark_aqua>nw" +
                "; New Balance: <aqua>" + newBalance + "<dark_aqua>.", player);
            return newBalance;
        } catch (SQLException e) {
            // Revert changes
            modifiedStacks.forEach((index, itemStack) -> player.getInventory().setItem(index, itemStack));

            Msg.miniMsg("<red>An error occurred while updating your networth. Your items have been returned.", player);
            statisticsManager.logIfLogged("networth", "Failed a " + amount + " " + type + " deposit for " + player.getName() + ", worth " + profit);
            Msg.log(Level.SEVERE, "Failed nw deposit for " + player.getName() + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * Read the total value of all valuable items in a target player's inventory
     * @param player Player whose inventory to read
     * @return An int array with the total value and number of items counted
     */
    public int[] getInventoryValue(Player player) {
        int value = 0; // total inventory value
        int count = 0; // # of valued items
        
        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack content : contents) {
            int itemValue = getItemValue(content);
            if (itemValue != 0) {
                value += itemValue;
                count++;
            }
        }

        return new int[]{value, count};
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
            if (item == null || item.getType() == Material.AIR) continue;

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
                    "; + <aqua>" + profit + " <dark_aqua>nw" +
                    "; New Balance: <aqua>" + newBalance + "<dark_aqua>.", player);
                return newBalance;
            } catch (SQLException e) {
                // Rollback to prevent item loss
                removedItems.forEach((slot, stack) -> player.getInventory().setItem(slot, stack));

                Msg.miniMsg("<red>An error occurred while updating your networth. Your items have been returned.", player);
                statisticsManager.logIfLogged("networth", "Failed a " + count + " item deposit for " + player.getName() + ", worth " + profit);
                Msg.log(Level.SEVERE, "Failed nw deposit for " + player.getName() + ": " + e.getMessage());
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
            Msg.miniMsg("<dark_aqua>You cannot afford <aqua>" + amount + " <yellow>" + getCapitalizedTypeDisplay(type, amount) , player);
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

        String displayType = getCapitalizedTypeDisplay(type, amount);
        Msg.miniMsg("<dark_aqua>+ <aqua>" + amount + " <yellow>" + displayType +
            "<dark_aqua>; - <aqua>" + cost + " <dark_aqua>nw" +
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
            Msg.miniMsg("<dark_aqua>You cannot afford any " + getCapitalizedTypeDisplay(type, 2) + ".", player);
            return 0;
        }
        // Calculate # of items that can fit in the player's inventory
        int canFit = getFreeSpace(player, type);
        if (canFit <= 0) {
            Msg.miniMsg("<dark_aqua>Your inventory is full. Please make some space.", player);
            return 0;
        }
        // Withdraw bottleneck amount, return the new balance
        int max = Math.min(canAfford, canFit);
        return withdrawItems(player, type, max);
    }

    private int getTypeValue(String type) {
        if (type == null) return 0;
        return switch (type.toUpperCase()) {
            case "KELP" -> 1024;
            case "PICKLE" -> 2048;
            case "COMPRESSED_KELP" -> 4096;
            case "COMPRESSED_PICKLE" -> 8192;
            default -> 0;
        };
    }

    private String getTypeDisplay(String type, int amount) {
        String typeDisplay = type.toLowerCase().replaceAll("_", " ");
        if (type.equalsIgnoreCase("kelp") || type.equalsIgnoreCase("compressed_kelp")) {
            return typeDisplay;
        } else {
            return typeDisplay += (amount == 1 ? "" : "s");
        }
    }
    private String getCapitalizedTypeDisplay(String type, int amount) {
        return TextUtils.capitalizeEach(getTypeDisplay(type, amount));
    }
    private String getTypeDisplay(String type) {
        return type.toLowerCase().replaceAll("_", " ");
    }
    private String getCapitalizedTypeDisplay(String type) {
        return TextUtils.capitalizeEach(getTypeDisplay(type));
    }

    private int getFreeSpace(Player player, String type) {
        if (type == null) return 0;

        int count = 0;
        ItemStack[] contents = player.getInventory().getContents();

        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR) {
                count += 64;
                continue;
            }

            String itemType = getItemType(item);
            if (type.equalsIgnoreCase(itemType)) {
                count += item.getMaxStackSize() - item.getAmount();
            }
        }
        return count;
    }

    public ItemStack sampleItem(String type) {
        ItemStack item = switch (type.toUpperCase()) {
            case "COMPRESSED_KELP" -> ItemCreator.create(
                Material.DRIED_KELP_BLOCK,
                mm.deserialize("<!i><color:#3f4f2d>Compressed Kelp"),
                true);
            case "PICKLE" -> ItemCreator.create(Material.SEA_PICKLE);
            case "COMPRESSED_PICKLE" -> ItemCreator.create(
                Material.SEA_PICKLE,
                mm.deserialize("<!i><color:#324f1e>Compressed Pickle"),
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
        return ItemCreator.skull(player.getUniqueId(), mm.deserialize("<!i><gold>" + player.getName()), List.of(
            mm.deserialize("<!i> <gold><b>» </b><dark_aqua>Net Worth: <aqua>" + balance)
        ));
    }

    private ItemStack depositAllItem(Player player) {
        int totalNw = 0;
        int totalItems = 0;

        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR) continue;

            int itemValue = getItemValue(item);
            if (itemValue <= 0) continue;

            totalNw += itemValue;
            totalItems += item.getAmount();
        }

        List<Component> lore = new ArrayList<>();
        lore.add(mm.deserialize("<!i> <gold><b>» </b><dark_aqua>Click: Deposit <aqua>ALL"));

        if (totalItems > 0 && totalNw > 0) {
            lore.add(mm.deserialize("<!i> <gold>└ <aqua>" + totalItems + " <dark_aqua>Item" + (totalItems == 1 ? "" : "s")
                + " <dark_aqua>for <aqua>" + totalNw + " <dark_aqua>nw"));
        } else {
            lore.add(mm.deserialize("<!i> <gold>└ <red>No valuable items found"));
        }

        return ItemCreator.create(
            Material.GLOBE_BANNER_PATTERN,
            mm.deserialize("<!i><green><b>DEPOSIT ALL</b> <gold>| Inventory"),
            lore,
            totalNw > 0
        );
    }

    private ItemStack depositItem(int maxAmount, String type, Material material, boolean isGlowing) {
        String typeDisplay = getCapitalizedTypeDisplay(type);
        int typeValue = getTypeValue(type);
        List<Component> lore = new ArrayList<>();

        lore.add(mm.deserialize("<!i> <gold><b>» </b><dark_aqua>Left Click: Deposit <aqua>1"));
        lore.add(mm.deserialize("<!i> <gold>└ For <aqua>" + typeValue));

        if (maxAmount >= 10) {
            lore.add(mm.deserialize("<!i> <gold><b>» </b><dark_aqua>Right Click: Deposit <aqua>" + maxAmount));
            lore.add(mm.deserialize("<!i> <gold>└ For <aqua>" + typeValue * maxAmount));
        }

//        lore.add(mm.deserialize("<!i> <gold><b>» </b><dark_aqua>Shift + Left Click: Deposit <aqua>ALL"));
//        lore.add(mm.deserialize("<!i> <gold>└ <aqua>" + maxAmount + " For <aqua>" + (typeValue * maxAmount)));

        ItemStack item = ItemCreator.create(material, mm.deserialize("<!i><green><b>DEPOSIT </b><gold>| " + typeDisplay), lore, isGlowing);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(ITEM_TYPE_KEY, PersistentDataType.STRING, type.toUpperCase());
        item.setItemMeta(meta);
        return item;
    }
    private ItemStack withdrawItem(int maxAmount, String type, Material material, boolean isGlowing) {
        String typeDisplay = getCapitalizedTypeDisplay(type);
        int typeValue = getTypeValue(type);
        List<Component> lore = new ArrayList<>();

        lore.add(mm.deserialize("<!i> <gold><b>» </b><dark_aqua>Left Click: Withdraw <aqua>1"));
        lore.add(mm.deserialize("<!i> <gold>└ For <aqua>" + typeValue));

        if (maxAmount >= 10) {
            lore.add(mm.deserialize("<!i> <gold><b>» </b><dark_aqua>Right Click: Withdraw <aqua>" + maxAmount));
            lore.add(mm.deserialize("<!i> <gold>└ For <aqua>" + typeValue * maxAmount));
        }

//        lore.add(mm.deserialize("<!i> <gold><b>» </b><dark_aqua>Shift + Left Click: Withdraw <aqua>ALL"));
//        lore.add(mm.deserialize("<!i> <gold>└ <aqua>" + maxAmount + " For <aqua>" + (typeValue * maxAmount)));

        ItemStack item = ItemCreator.create(material, mm.deserialize("<!i><red><b>WITHDRAW </b><gold>| " + typeDisplay), lore, isGlowing);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(ITEM_TYPE_KEY, PersistentDataType.STRING, type.toUpperCase());
        item.setItemMeta(meta);
        return item;
    }
}
