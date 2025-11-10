package dev.magnoix.msa.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.databases.PluginConfig;
import dev.magnoix.msa.utils.TextUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ToggleCommand {
    private final PluginConfig config;
    private List<String> toggleableEnchants;

    public ToggleCommand(PluginConfig config) {
        this.config = config;
        this.toggleableEnchants = config.getStringList("toggleable-enchants");
    }
    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("toggle").build();
    }

    // Enchant-Specific Helper Methods
    public void setToggleableEnchants(List<String> enchants) {
        config.setValues("toggleable-enchants", enchants);
        this.toggleableEnchants = enchants;
    }
    public void addToggleableEnchant(String enchant) {
        config.addValue("toggleable-enchants", enchant);
        if (!this.toggleableEnchants.contains(enchant)) {
            toggleableEnchants.add(enchant);
        }
    }

    /**
     * Get a map of all toggleable enchants, including both enabled and disabled toggleable enchantments.
     * @param itemStack The item to check.
     * @return A map of Enchantments and their Levels.
     */
    public Map<Enchantment, Integer> getToggleableItemEnchants(ItemStack itemStack) {
        Map<Enchantment, Integer> enchants = itemStack.getEnchantments();
        Map<Enchantment, Integer> toggleableItemEnchants = new HashMap<>();

        // Add currently active toggleable enchants
        enchants.forEach((enchant, level) -> {
            if (toggleableEnchants.contains(enchant.getKey().getKey())) {
                toggleableItemEnchants.put(enchant, level);
            }
        });

        // Also include disabled ones from lore
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.lore() != null) {
            for (Component component : meta.lore()) {
                String plain = TextUtils.getPlainText(component);

                for (String name : toggleableEnchants) {
                    String pretty = name.replace("_", " ");
                    if (plain.toLowerCase().startsWith(pretty.toLowerCase())) {
                        Enchantment e = getEnchantFromName(name);
                        if (e != null) {
                            Matcher matcher = Pattern.compile("([IVXLCDM]+)$").matcher(plain);
                            int level = matcher.find() ? TextUtils.parseRomanNumerals(matcher.group(1)) : 1;
                            toggleableItemEnchants.put(e, level);
                        }
                    }
                }
            }
        }
        return toggleableItemEnchants;
    }

    public Enchantment getEnchantFromName(String enchant) {
        return Enchantment.getByKey(NamespacedKey.minecraft(enchant));
    }
    public boolean toggleEnchant(ItemStack item, String enchantName) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        Enchantment enchant = getEnchantFromName(enchantName);
        if (enchant == null) return false;

        int level = item.getEnchantmentLevel(enchant);
        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();

        // if enchant is active -> disable it
        if (item.containsEnchantment(enchant)) {
            item.removeEnchantment(enchant);
            Component disabled = Component.text(
                TextUtils.capitalize(enchant.getKey().getKey().replace("_", " ")) + " " + TextUtils.toRomanNumerals(level),
                NamedTextColor.GRAY,
                TextDecoration.STRIKETHROUGH
            );
            lore.add(0, disabled);
            meta.lore(lore);
            item.setItemMeta(meta);
            return true;
        }

        // if enchant is disabled in lore -> re-enable
        Iterator<Component> it = lore.iterator();
        while (it.hasNext()) {
            Component component = it.next();
            String plain = TextUtils.getPlainText(component);
            if (plain.toLowerCase().startsWith(enchant.getKey().getKey().replace("_", " "))) {
                it.remove();
                item.addUnsafeEnchantment(enchant, level > 0 ? level : 1);
                meta.lore(lore);
                item.setItemMeta(meta);
                return true;
            }
        }
        return false;
    }

    private String capitalizeEnchantName(Enchantment enchant) {
        String key = enchant.getKey().getKey().replace("_", " ");
        return Character.toUpperCase(key.charAt(0)) + key.substring(1);
    }
}
