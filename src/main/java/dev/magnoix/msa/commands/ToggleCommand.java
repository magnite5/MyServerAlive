package dev.magnoix.msa.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.messages.Msg;
import dev.magnoix.msa.utils.TextUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ToggleCommand {
    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private List<String> toggleableEnchants;

    public ToggleCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();

        this.toggleableEnchants = new ArrayList<>();
        for (String s : config.getStringList("toggleable-enchants")) {
            this.toggleableEnchants.add(s.toLowerCase().replace(" ", "_"));
        }
    }

    public SuggestionProvider<CommandSourceStack> toggleableEnchantSuggestions(ItemStack item) {
        return (CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) -> {
            Map<Enchantment, Integer> toggleableItemEnchants = getToggleableItemEnchants(item);
            toggleableItemEnchants.forEach((enchantment, level) -> {
                builder.suggest(enchantment.getKey().getKey());
            });
            return CompletableFuture.completedFuture(builder.build());
        };
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("toggle")
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (!(sender instanceof Player player)) {
                    Msg.msg("You must be a player to use this command.", sender);
                    return 1;
                }
                ItemStack item = player.getInventory().getItemInMainHand();
                Msg.miniMsg("<dark_aqua>Toggle an enchantment on your held item.", sender);
                getToggleableItemEnchants(item).forEach((enchantment, level) -> {
                    Msg.miniMsg("  <gold>-> " + enchantment.getKey().getKey(), sender);
                });
                return 1;
            })
            .then(Commands.argument("enchant", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player player)) return CompletableFuture.completedFuture(builder.build());
                    return toggleableEnchantSuggestions(player.getInventory().getItemInMainHand()).getSuggestions(ctx, builder);
                })
                .then(Commands.literal("help")
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        List<String> messages = new ArrayList<>(List.of(
                            " <gold>| <dark_aqua><u>Toggle Help Menu<u:false>",
                            " <gold>| <dark_aqua>Toggle an enchantment on the item in your main hand.",
                            " <gold>| <dark_aqua>Only enabled enchantments can be toggled.",
                            " <gold>| <dark_aqua>Allowed Enchantments:"
                        ));
                        for (String enchant : toggleableEnchants) {
                            messages.add(" <dark_aqua>└ <gold>" + enchant);
                        }
                        Msg.miniMsg(messages, sender);
                        return 1;
                    }))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player player)) {
                        Msg.msg("You must be a player to use this command.", sender);
                        return 1;
                    }
                    String enchant = ctx.getArgument("enchant", String.class);

                    if (!toggleableEnchants.contains(enchant.toLowerCase().replaceAll(" ", "_"))) {
                        Msg.miniMsg("<red>The specified enchant is not valid. Please ensure that the enchantment can be toggled.", sender);
                        return 1;
                    }
                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (toggleEnchant(item, enchant)) {
                        Msg.miniMsg("<dark_aqua>Successfully toggled the <gold>" + enchant + " <dark_aqua>enchantment.", sender);
                    } else {
                        Msg.miniMsg("<red>An error occurred while toggling the " + enchant + " enchantment.", sender);
                    }
                    return 1;
                }))
            .build();
    }

    // Enchant-Specific Helper Methods
    public void setToggleableEnchants(List<String> enchants) {
        // Update the config with the updated list
        config.set("toggleable-enchants", enchants);
        this.toggleableEnchants = enchants;
        // Write changes
        plugin.saveConfig();
    }
    public void addToggleableEnchant(String enchant) {
        // Get the current list
        if (!toggleableEnchants.contains(enchant)) {
            toggleableEnchants.add(enchant);
            // Update the config with the updated list
            config.set("toggleable-enchants", toggleableEnchants);
            // Write changes
            plugin.saveConfig();
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
                String raw = TextUtils.getPlainText(component);

                for (String name : toggleableEnchants) {
                    String formatted = name.replace("_", " ");
                    if (raw.toLowerCase().startsWith(formatted.toLowerCase())) {
                        Enchantment e = getEnchantFromName(name);
                        if (e != null) {
                            Matcher matcher = Pattern.compile("([IVXLCDM]+)$").matcher(raw);
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

        Enchantment enchant = getEnchantFromName(enchantName.toLowerCase(Locale.ROOT));
        if (enchant == null) return false;

        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();

        // CASE 1: Enchant is active -> Disable it
        if (item.containsEnchantment(enchant)) {
            int level = item.getEnchantmentLevel(enchant);

            // Remove the enchantment
            meta.removeEnchant(enchant);

            // Add the "disabled" visual to lore
            Component disabledLore = Component.text(
                    TextUtils.capitalize(enchant.getKey().getKey().replace("_", " ")) + " " + TextUtils.toRomanNumerals(level),
                    NamedTextColor.GRAY,
                    TextDecoration.STRIKETHROUGH
            );
            lore.addFirst(disabledLore);

            meta.lore(lore);
            item.setItemMeta(meta);
            return true;
        }

        // CASE 2: Enchant is NOT active -> Check lore to re-enable it
        else {
            Iterator<Component> it = lore.iterator();
            while (it.hasNext()) {
                Component component = it.next();
                String plain = TextUtils.getPlainText(component);
                String pretty = enchant.getKey().getKey().replace("_", " ");

                if (plain.toLowerCase().startsWith(pretty.toLowerCase())) {
                    // 1. Remove the lore line
                    it.remove();

                    // 2. Parse the level from the lore line
                    int storedLevel = 1;
                    Matcher romanMatcher = Pattern.compile("([IVXLCDM]+)$").matcher(plain);
                    if (romanMatcher.find()) {
                        storedLevel = TextUtils.parseRomanNumerals(romanMatcher.group(1));
                    }

                    // 3. Apply changes
                    meta.lore(lore);
                    item.setItemMeta(meta);
                    item.addUnsafeEnchantment(enchant, storedLevel);
                    return true;
                }
            }
        }

        // Enchantment wasn't active or disabled
        return false;
    }

    private String capitalizeEnchantName(Enchantment enchant) {
        String key = enchant.getKey().getKey().replace("_", " ");
        return Character.toUpperCase(key.charAt(0)) + key.substring(1);
    }
}
