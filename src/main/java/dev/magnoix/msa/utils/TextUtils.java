package dev.magnoix.msa.utils;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class TextUtils {
    /**
     * Converts a long string into a list of Components for lore,
     * splitting lines at spaces so no line exceeds maxLineLength characters.
     */
    public static List<Component> stringToLore(String text, int maxLineLength, Component prefix) {
        List<Component> lore = new ArrayList<>();
        if (!(prefix == null || prefix.toString().isEmpty())) lore.add(prefix);
        String[] words = text.split(" "); // split by spaces
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (line.length() + word.length() + 1 > maxLineLength) {
                lore.add(Component.text(line.toString().trim()));
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }

        if (!line.isEmpty()) {
            lore.add(Component.text(line.toString().trim()));
        }
        return lore;
    }
    public static List<Component> stringToLore(String text, int maxLineLength) {
        return stringToLore(text, maxLineLength, null);
    }
}
