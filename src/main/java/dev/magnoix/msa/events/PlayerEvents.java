package dev.magnoix.msa.events;

import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.databases.TitleManager;
import dev.magnoix.msa.menus.ConversionMenu;
import dev.magnoix.msa.messages.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.SeaPickle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public record PlayerEvents(StatisticsManager statisticsManager, TitleManager titleManager, JavaPlugin plugin) implements Listener {

    @EventHandler
    public void playerDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        Player k = p.getKiller();
        try {
            statisticsManager.addDeaths(p.getUniqueId(), 1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (k != null) try {
            statisticsManager.addKills(k.getUniqueId(), 1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        titleManager.handlePlayerJoin(event, plugin);

        Player player =  event.getPlayer();
        if (!player.hasPlayedBefore()) {
            try {
                statisticsManager.addPlayer(player.getUniqueId());
            } catch (SQLException e) {
                Msg.log(Level.SEVERE, "Failed to add player to statistics manager: " + e.getMessage());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // Prevent WorldGuard conflicts
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        Material type = block.getType();

        if (type == Material.DRIED_KELP_BLOCK || type == Material.SEA_PICKLE) {
            Player player = event.getPlayer();
            ItemStack tool = player.getInventory().getItemInMainHand();

            // Check for Telepathy without an API dependency
            boolean hasTelepathy = hasTelepathyLore(tool);

            event.setDropItems(false);

            int amount = 1;
            if (block.getBlockData() instanceof SeaPickle pickleData) {
                amount = pickleData.getPickles();
            }
            String typeString;
            if (type == Material.DRIED_KELP_BLOCK) typeString = "KELP";
            else typeString = "PICKLE";

            ItemStack customDrop = ConversionMenu.sampleItem(typeString, new NamespacedKey(plugin, "item_type"));
            customDrop.setAmount(amount);

            if (hasTelepathy) {
                // Direct to inventory (mimic Telepathy behavior)
                Map<Integer, ItemStack> leftOver = player.getInventory().addItem(customDrop);
                if (!leftOver.isEmpty()) {
                    leftOver.values().forEach(item ->
                            block.getWorld().dropItemNaturally(block.getLocation(), item));
                }
            } else {
                // Drop naturally on ground
                block.getWorld().dropItemNaturally(block.getLocation(), customDrop);
            }
        }
    }

    /**
     * Scans an item's lore for the Telepathy Crazy Enchantment.
     */
    private boolean hasTelepathyLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();

        // Check modern Paper Components
        List<Component> lore = meta.lore();
        if (lore == null) return false;

        for (net.kyori.adventure.text.Component line : lore) {
            // Plaintext conversion via Adventure
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
            if (plain.contains("Telepathy")) return true;
        }
        return false;
    }
}
