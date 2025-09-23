package dev.magnoix.msa.helpers;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.List;
import java.util.Map;

public class ItemCreator {

    public static ItemStack create(Material material) { return new ItemStack(material); }
    public static ItemStack create(Material material, Component name) {
        ItemStack item = create(material);
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return item;
        itemMeta.displayName(name);
        item.setItemMeta(itemMeta);
        return item;
    }
    public static ItemStack create(Material material, Component name, List<Component> lore) {
        ItemStack item = create(material, name);
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return item;
        if (lore != null) itemMeta.lore(lore);
        item.setItemMeta(itemMeta);
        return item;
    }
    public static ItemStack create(Material material, Component name, List<Component> lore, boolean isGlowing) {
        ItemStack item = create(material, name, lore);
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return item;
        itemMeta.setEnchantmentGlintOverride(isGlowing);
        item.setItemMeta(itemMeta);
        return item;
    }

    public static ItemStack updateMaterial(Material newMaterial, ItemStack oldItem) {
        ItemMeta itemMeta = oldItem.getItemMeta();
        ItemStack newItem = new ItemStack(newMaterial);
        newItem.setItemMeta(itemMeta);
        return newItem;
    }

    public static ItemStack tool(Material material, Component name, List<Component> lore, boolean unbreakable) {
        ItemStack item = create(material, name, lore);
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return item;
        itemMeta.setUnbreakable(unbreakable);
        return item;
    }
    public static ItemStack tool(Material material, Component name, List<Component> lore, boolean unbreakable, Map<Enchantment, Integer> enchants) {
        ItemStack item = tool(material, name, lore, unbreakable);
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return item;
        if (enchants != null && !enchants.isEmpty()) {
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                Enchantment enchantment = entry.getKey();
                Integer level = entry.getValue();
                if (enchantment != null && level != null) {
                    itemMeta.addEnchant(enchantment, Math.max(1, level), true);
                }
            }
        }
        item.setItemMeta(itemMeta);
        return item;
    }
    public static ItemStack tool(Material material, Component name, List<Component> lore, boolean unbreakable, Map<Enchantment, Integer> enchants, List<ItemFlag> flags) {
        ItemStack item = tool(material, name, lore, unbreakable, enchants);
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return item;
        if (flags != null && !flags.isEmpty()) itemMeta.addItemFlags(flags.toArray(new ItemFlag[0]));
        item.setItemMeta(itemMeta);
        return item;
    }
    public static ItemStack tool(Material material, Component name, List<Component> lore, boolean unbreakable, Map<Enchantment, Integer> enchants, List<ItemFlag> flags, Integer customModelData) {
        ItemStack item = create(material, name, lore);
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null || customModelData == null) return item;
        if (flags != null && !flags.isEmpty()) itemMeta.addItemFlags(flags.toArray(new ItemFlag[0]));
        itemMeta.setCustomModelData(customModelData);
        item.setItemMeta(itemMeta);
        return item;
    }
    public static ItemStack tool(Material material, Component name, List<Component> lore, boolean unbreakable, Map<Enchantment, Integer> enchants, List<ItemFlag> flags, CustomModelDataComponent customModelDataComponent) {
        ItemStack item = create(material, name, lore);
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null || customModelDataComponent == null) return item;
        if (flags != null && !flags.isEmpty()) itemMeta.addItemFlags(flags.toArray(new ItemFlag[0]));
        itemMeta.setCustomModelDataComponent(customModelDataComponent);
        item.setItemMeta(itemMeta);
        return item;
    }

}
