package main.io.github.itshaithamn.teamsandmore.nametag;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum NametagColor {

    RED(NamedTextColor.RED),
    BLUE(NamedTextColor.BLUE),
    GREEN(NamedTextColor.GREEN),
    YELLOW(NamedTextColor.YELLOW),
    AQUA(NamedTextColor.AQUA),
    LIGHT_PURPLE(NamedTextColor.LIGHT_PURPLE),
    GOLD(NamedTextColor.GOLD),
    WHITE(NamedTextColor.WHITE),
    DARK_GREEN(NamedTextColor.DARK_GREEN),
    DARK_AQUA(NamedTextColor.DARK_AQUA),
    DARK_RED(NamedTextColor.DARK_RED),
    DARK_PURPLE(NamedTextColor.DARK_PURPLE),
    DARK_BLUE(NamedTextColor.DARK_BLUE),
    GRAY(NamedTextColor.GRAY);

    private final TextColor textColor;

    NametagColor(TextColor textColor) {
        this.textColor = textColor;
    }

    public TextColor getTextColor() {
        return textColor;
    }

    public String getChatColor() {
        NamedTextColor named = NamedTextColor.nearestTo(textColor);
        return "§" + legacyCode(named);
    }

    private static char legacyCode(NamedTextColor color) {
        if (color.equals(NamedTextColor.BLACK)) return '0';
        if (color.equals(NamedTextColor.DARK_BLUE)) return '1';
        if (color.equals(NamedTextColor.DARK_GREEN)) return '2';
        if (color.equals(NamedTextColor.DARK_AQUA)) return '3';
        if (color.equals(NamedTextColor.DARK_RED)) return '4';
        if (color.equals(NamedTextColor.DARK_PURPLE)) return '5';
        if (color.equals(NamedTextColor.GOLD)) return '6';
        if (color.equals(NamedTextColor.GRAY)) return '7';
        if (color.equals(NamedTextColor.DARK_GRAY)) return '8';
        if (color.equals(NamedTextColor.BLUE)) return '9';
        if (color.equals(NamedTextColor.GREEN)) return 'a';
        if (color.equals(NamedTextColor.AQUA)) return 'b';
        if (color.equals(NamedTextColor.RED)) return 'c';
        if (color.equals(NamedTextColor.LIGHT_PURPLE)) return 'd';
        if (color.equals(NamedTextColor.YELLOW)) return 'e';
        if (color.equals(NamedTextColor.WHITE)) return 'f';
        return 'f';
    }
}