package main.io.github.itshaithamn.teamsandmore.gui.bannerui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import java.util.List;

/**
 * Page 2 of the banner editor — the main workspace.
 * Layout (6 rows):
 *   Row 1: Slots 1-6 = layer stack (click to remove), Slot 8 = live preview, Slot 9 = info
 *   Rows 2-5: All PatternType values as clickable items (click to select, then pick dye)
 *   Row 6: bannerGet (slot 1), bannerSet (slot 2), Clear All (slot 3), Back (slot 9)
 */
public class PatternPage {

    // All PatternType values — includes both basic and special patterns.
    // We skip BASE since that's just the blank banner itself.
    private static final PatternType[] PATTERNS;

    static {
        PatternType[] all = new PatternType[]{
                PatternType.BORDER,
                PatternType.BRICKS,
                PatternType.CIRCLE,
                PatternType.CROSS,
                PatternType.CREEPER,
                PatternType.CURLY_BORDER,
                PatternType.DIAGONAL_LEFT,
                PatternType.DIAGONAL_RIGHT,
                PatternType.DIAGONAL_UP_LEFT,
                PatternType.DIAGONAL_UP_RIGHT,
                PatternType.FLOW,
                PatternType.FLOWER,
                PatternType.GLOBE,
                PatternType.GRADIENT,
                PatternType.GRADIENT_UP,
                PatternType.GUSTER,
                PatternType.HALF_HORIZONTAL,
                PatternType.HALF_HORIZONTAL_BOTTOM,
                PatternType.HALF_VERTICAL,
                PatternType.HALF_VERTICAL_RIGHT,
                PatternType.MOJANG,
                PatternType.PIGLIN,
                PatternType.RHOMBUS,
                PatternType.SKULL,
                PatternType.SMALL_STRIPES,
                PatternType.SQUARE_BOTTOM_LEFT,
                PatternType.SQUARE_BOTTOM_RIGHT,
                PatternType.SQUARE_TOP_LEFT,
                PatternType.SQUARE_TOP_RIGHT,
                PatternType.STRAIGHT_CROSS,
                PatternType.STRIPE_BOTTOM,
                PatternType.STRIPE_CENTER,
                PatternType.STRIPE_DOWNLEFT,
                PatternType.STRIPE_DOWNRIGHT,
                PatternType.STRIPE_LEFT,
                PatternType.STRIPE_MIDDLE,
                PatternType.STRIPE_RIGHT,
                PatternType.STRIPE_TOP,
                PatternType.TRIANGLE_BOTTOM,
                PatternType.TRIANGLE_TOP,
                PatternType.TRIANGLES_BOTTOM,
                PatternType.TRIANGLES_TOP
        };
        PATTERNS = java.util.Arrays.stream(all)
                .toArray(PatternType[]::new);
    }

    private PatternPage() {}

    public static void open(Player player, BannerEditorState state) {
        Gui gui = Gui.gui()
                .title(Component.text("Select patterns", NamedTextColor.GOLD, TextDecoration.BOLD))
                .rows(6)
                .disableItemTake()
                .disableItemDrop()
                .disableItemSwap()
                .create();

        List<Pattern> layers = state.getLayers();
        for (int i = 0; i < 6; i++) {
            if (i < layers.size()) {
                Pattern layer = layers.get(i);
                ItemStack layerPreview = buildLayerPreview(state.getBaseColor(), layer);
                final int layerIndex = i;

                GuiItem layerItem = ItemBuilder.from(layerPreview)
                        .name(Component.text("Layer " + (i + 1), NamedTextColor.YELLOW)
                                .decoration(TextDecoration.ITALIC, false))
                        .lore(
                                Component.text(formatEnumName(layer.getPattern().toString()), NamedTextColor.GRAY)
                                        .decoration(TextDecoration.ITALIC, false),
                                Component.text("Dye: " + formatEnumName(layer.getColor().name()), NamedTextColor.GRAY)
                                        .decoration(TextDecoration.ITALIC, false),
                                Component.empty(),
                                Component.text("Click to remove", NamedTextColor.RED)
                                        .decoration(TextDecoration.ITALIC, false)
                        )
                        .asGuiItem(event -> {
                            event.setCancelled(true);
                            state.removeLayer(layerIndex);
                            open(player, state);
                        });

                gui.setItem(1, i + 1, layerItem);
            } else {
                GuiItem emptySlot = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                        .name(Component.text("Empty layer " + (i + 1), NamedTextColor.DARK_GRAY)
                                .decoration(TextDecoration.ITALIC, false))
                        .asGuiItem(event -> event.setCancelled(true));

                gui.setItem(1, i + 1, emptySlot);
            }
        }

        GuiItem separator = ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
                .name(Component.text(" "))
                .asGuiItem(event -> event.setCancelled(true));
        gui.setItem(1, 7, separator);

        ItemStack previewBanner = state.buildBanner();
        GuiItem previewItem = ItemBuilder.from(previewBanner)
                .name(Component.text("Preview", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text("Base: " + formatEnumName(state.getBaseColor().name()), NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("Layers: " + layers.size() + "/6", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem(event -> event.setCancelled(true));
        gui.setItem(1, 8, previewItem);

        GuiItem infoItem = ItemBuilder.from(Material.OAK_SIGN)
                .name(Component.text("How to use", NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text("Click a pattern below to add it", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("Click a layer above to remove it", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("Max 6 layers", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem(event -> event.setCancelled(true));
        gui.setItem(1, 9, infoItem);

        int row = 2;
        int col = 1;

        for (PatternType patternType : PATTERNS) {
            ItemStack patternDisplay = buildPatternDisplay(patternType);

            GuiItem patternItem = ItemBuilder.from(patternDisplay)
                    .name(Component.text(formatEnumName(patternType.toString()), NamedTextColor.WHITE)
                            .decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("Click to add this pattern", NamedTextColor.YELLOW)
                                    .decoration(TextDecoration.ITALIC, false)
                    )
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        if (state.getLayers().size() >= 6) {
                            player.sendMessage(Component.text("§6§l[TeamsAndMore]§r Max 6 layers reached! Remove a layer first.", NamedTextColor.RED));
                            return;
                        }
                        state.setPendingPattern(patternType);
                        DyeColorPage.open(player, state);
                    });

            gui.setItem(row, col, patternItem);

            col++;
            if (col > 9) {
                col = 1;
                row++;
            }
            if (row > 5) break;
        }

        GuiItem bannerGetItem = ItemBuilder.from(Material.CHEST)
                .name(Component.text("Get Banner", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text("Get a copy of this banner", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    ItemStack banner = state.buildBanner();
                    player.getInventory().addItem(banner);
                    player.sendMessage(Component.text("Banner added to your inventory!", NamedTextColor.GREEN));
                });
        gui.setItem(6, 2, bannerGetItem);

        // bannerSet — serializes the banner and returns the data for DB storage
        GuiItem bannerSetItem = ItemBuilder.from(Material.ANVIL)
                .name(Component.text("Set Team Banner", NamedTextColor.RED, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text("Save this as your team's banner", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    ItemStack banner = state.buildBanner();
                    String serialized = BannerSerializer.serialize(banner);

                    // TODO: Replace this with your actual DB write call
                    // Example: YourTeamDatabase.setBanner(teamName, serialized);
                    String teamName = BannerEditorManager.getTeamName(player);
                    player.sendMessage(Component.text("§6§l[TeamsAndMore]§r Banner saved for team: " + teamName, NamedTextColor.GREEN));

                    // For debugging — remove this in production
                    player.sendMessage(Component.text("§6§l[TeamsAndMore]§r Serialized (" + serialized.length() + " chars)", NamedTextColor.DARK_GRAY));
                });
        gui.setItem(6, 4, bannerSetItem);

        GuiItem clearItem = ItemBuilder.from(Material.BARRIER)
                .name(Component.text("Clear All Layers", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text("Remove all pattern layers", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    state.getLayers().clear();
                    open(player, state);
                });
        gui.setItem(6, 6, clearItem);

        GuiItem backItem = ItemBuilder.from(Material.ARROW)
                .name(Component.text("Back", NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text("Change base color", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("Warning: resets all layers!", NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    state.getLayers().clear();
                    BaseColorPage.open(player, state);
                });
        gui.setItem(6, 8, backItem);

        Material fillerMat = Material.BLACK_STAINED_GLASS_PANE;
        for (int c : new int[]{1, 3, 5, 7, 9}) {
            gui.setItem(6, c, ItemBuilder.from(fillerMat)
                    .name(Component.text(" "))
                    .asGuiItem(event -> event.setCancelled(true)));
        }

        gui.open(player);
    }

    /**
     * Builds a preview banner showing a single pattern layer on the player's base color.
     * Used for the layer stack display in row 1.
     */
    private static ItemStack buildLayerPreview(DyeColor baseColor, Pattern layer) {
        Material bannerMat = BannerEditorState.dyeColorToBannerMaterial(baseColor);
        ItemStack banner = new ItemStack(bannerMat);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        meta.addPattern(layer);
        banner.setItemMeta(meta);
        return banner;
    }

    /**
     * Builds a display banner showing what a pattern looks like.
     * Uses white banner + black dye so the pattern shape is clearly visible.
     */
    private static ItemStack buildPatternDisplay(PatternType patternType) {
        ItemStack banner = new ItemStack(Material.WHITE_BANNER);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        meta.addPattern(new Pattern(DyeColor.BLACK, patternType));
        banner.setItemMeta(meta);
        return banner;
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
