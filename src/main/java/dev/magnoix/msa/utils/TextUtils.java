package dev.magnoix.msa.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public static String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
    public static String capitalizeEach(String text) {
        if (text == null || text.isEmpty()) return text;
        String[] words = text.split("\\s+");
        StringBuilder stringBuilder = new StringBuilder();
        for (String word : words) {
            stringBuilder.append(capitalizeFirst(word)).append(" ");
        }
        return stringBuilder.toString().trim();
    }

    public static String getPlainText(Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static final Pattern LEGACY_PATTERN = Pattern.compile("&(#([A-Fa-f0-9]{6})|[0-9a-fk-orA-FK-OR])");

    public static Component parseMixedFormatting(String input) {
        Matcher matcher = LEGACY_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String group = matcher.group(1);
            String replacement;
            if (group.startsWith("#")) {
                replacement = "<#" + group.substring(1).toLowerCase() + ">";
            } else {
                replacement = switch (group.toLowerCase()) {
                    case "0" -> "<black>";
                    case "1" -> "<dark_blue>";
                    case "2" -> "<dark_green>";
                    case "3" -> "<dark_aqua>";
                    case "4" -> "<dark_red>";
                    case "5" -> "<dark_purple>";
                    case "6" -> "<gold>";
                    case "7" -> "<gray>";
                    case "8" -> "<dark_gray>";
                    case "9" -> "<blue>";
                    case "a" -> "<green>";
                    case "b" -> "<aqua>";
                    case "c" -> "<red>";
                    case "d" -> "<light_purple>";
                    case "e" -> "<yellow>";
                    case "f" -> "<white>";
                    case "k" -> "<obf>";
                    case "l" -> "<b>";
                    case "m" -> "<st>";
                    case "n" -> "<u>";
                    case "o" -> "<i>";
                    case "r" -> "<reset>";
                    default -> "";
                };
            }
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        return MiniMessage.miniMessage().deserialize(sb.toString());
    }

    public static String toRomanNumerals(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(num);
        };
    }

    public static int parseRomanNumerals(String roman) {
        if (roman == null || roman.isEmpty()) return 1;
        return switch (roman.toUpperCase(Locale.ROOT)) {
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
            case "VI" -> 6;
            case "VII" -> 7;
            case "VIII" -> 8;
            case "IX" -> 9;
            case "X" -> 10;
            default -> 1;
        };
    }

    public static String capitalize(String str) {
        if (str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

}
