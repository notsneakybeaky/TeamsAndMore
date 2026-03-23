package main.io.github.itshaithamn.teamsandmore.nametag;

/**
 * Available team prefix colors.
 * Each entry maps to a legacy '&' color code used in LuckPerms prefix strings.
 */
public enum NametagColor {

    RED("&c"),
    BLUE("&9"),
    GREEN("&a"),
    YELLOW("&e"),
    AQUA("&b"),
    LIGHT_PURPLE("&d"),
    GOLD("&6"),
    WHITE("&f"),
    DARK_GREEN("&2"),
    DARK_AQUA("&3"),
    DARK_RED("&4"),
    DARK_PURPLE("&5"),
    DARK_BLUE("&1"),
    GRAY("&7");

    private final String colorCode;

    NametagColor(String colorCode) {
        this.colorCode = colorCode;
    }

    /**
     * Returns the legacy '&' color code (e.g. "&c" for red).
     */
    public String getColorCode() {
        return colorCode;
    }

    /**
     * Returns the '§' color code for use in chat messages (e.g. "§c" for red).
     */
    public String getChatColor() {
        return colorCode.replace('&', '§');
    }
}