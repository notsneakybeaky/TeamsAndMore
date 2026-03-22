package main.io.github.itshaithamn.teamsandmore.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Page 1 of the banner editor.
 * Displays 16 banner colors for the player to pick as the base color.
 * Clicking a color sets the base and opens the pattern selection page.
 */
public class BaseColorPage {

    private static final DyeColor[] DYE_COLORS = DyeColor.values();

    private BaseColorPage() {}

    public static void open(Player player, BannerEditorState state) {
        Gui gui = Gui.gui()
                .title(Component.text("Pick a base color", NamedTextColor.GOLD, TextDecoration.BOLD))
                .rows(2)
                .disableItemTake()
                .disableItemDrop()
                .disableItemSwap()
                .create();

        int row = 1;
        int col = 1;

        for (DyeColor dyeColor : DYE_COLORS) {
            Material bannerMat = BannerEditorState.dyeColorToBannerMaterial(dyeColor);

            String colorName = formatEnumName(dyeColor.name());

            GuiItem item = ItemBuilder.from(bannerMat)
                    .name(Component.text(colorName, NamedTextColor.WHITE)
                            .decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        state.setBaseColor(dyeColor);
                        // Move to pattern selection page
                        PatternPage.open(player, state);
                    });

            gui.setItem(row, col, item);

            col++;
            if (col > 9) {
                col = 1;
                row++;
            }
        }

        gui.open(player);
    }

    private static String formatEnumName(String enumName) {
        String[] parts = enumName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
