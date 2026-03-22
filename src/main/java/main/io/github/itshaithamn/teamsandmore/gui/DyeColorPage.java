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
 * Page 3 of the banner editor.
 * Shows 16 dye colors. Clicking one applies the pending pattern with that
 * dye color as a new layer, then returns to the pattern page.
 */
public class DyeColorPage {

    private static final DyeColor[] DYE_COLORS = DyeColor.values();

    private DyeColorPage() {}

    public static void open(Player player, BannerEditorState state) {
        String patternName = formatEnumName(state.getPendingPattern().toString());

        Gui gui = Gui.gui()
                .title(Component.text("Pick dye for: " + patternName, NamedTextColor.GOLD, TextDecoration.BOLD))
                .rows(3)
                .disableItemTake()
                .disableItemDrop()
                .disableItemSwap()
                .create();

        int row = 1;
        int col = 1;

        for (DyeColor dyeColor : DYE_COLORS) {
            Material dyeMat = BannerEditorState.dyeColorToDyeMaterial(dyeColor);
            String colorName = formatEnumName(dyeColor.name());

            GuiItem item = ItemBuilder.from(dyeMat)
                    .name(Component.text(colorName, NamedTextColor.WHITE)
                            .decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("Apply " + patternName + " in " + colorName, NamedTextColor.YELLOW)
                                    .decoration(TextDecoration.ITALIC, false)
                    )
                    .asGuiItem(event -> {
                        event.setCancelled(true);

                        boolean added = state.addLayer(state.getPendingPattern(), dyeColor);
                        if (!added) {
                            player.sendMessage(Component.text("Max 6 layers reached!", NamedTextColor.RED));
                        }

                        state.setPendingPattern(null);
                        // Return to pattern page with updated stack
                        PatternPage.open(player, state);
                    });

            gui.setItem(row, col, item);

            col++;
            if (col > 9) {
                col = 1;
                row++;
            }
        }

        GuiItem backItem = ItemBuilder.from(Material.ARROW)
                .name(Component.text("Cancel", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text("Go back without adding a pattern", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    state.setPendingPattern(null);
                    PatternPage.open(player, state);
                });
        gui.setItem(3, 9, backItem);

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
