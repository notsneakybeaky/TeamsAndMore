package main.io.github.itshaithamn.teamsandmore.gui.bannerui;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import java.util.ArrayList;
import java.util.List;

public class BannerEditorState {

    private DyeColor baseColor;
    private final List<Pattern> layers;
    private PatternType pendingPattern; // temporarily holds pattern before dye selection

    public BannerEditorState() {
        this.baseColor = DyeColor.WHITE;
        this.layers = new ArrayList<>();
        this.pendingPattern = null;
    }

    public DyeColor getBaseColor() {
        return baseColor;
    }

    public void setBaseColor(DyeColor baseColor) {
        this.baseColor = baseColor;
    }

    public List<Pattern> getLayers() {
        return layers;
    }

    public boolean addLayer(PatternType patternType, DyeColor dyeColor) {
        if (layers.size() >= 6) {
            return false; // max 6 layers like vanilla
        }
        layers.add(new Pattern(dyeColor, patternType));
        return true;
    }

    public void removeLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            layers.remove(index);
        }
    }

    public PatternType getPendingPattern() {
        return pendingPattern;
    }

    public void setPendingPattern(PatternType pendingPattern) {
        this.pendingPattern = pendingPattern;
    }

    /**
     * Converts a DyeColor to the corresponding banner Material.
     */
    public static Material dyeColorToBannerMaterial(DyeColor color) {
        return switch (color) {
            case WHITE -> Material.WHITE_BANNER;
            case ORANGE -> Material.ORANGE_BANNER;
            case MAGENTA -> Material.MAGENTA_BANNER;
            case LIGHT_BLUE -> Material.LIGHT_BLUE_BANNER;
            case YELLOW -> Material.YELLOW_BANNER;
            case LIME -> Material.LIME_BANNER;
            case PINK -> Material.PINK_BANNER;
            case GRAY -> Material.GRAY_BANNER;
            case LIGHT_GRAY -> Material.LIGHT_GRAY_BANNER;
            case CYAN -> Material.CYAN_BANNER;
            case PURPLE -> Material.PURPLE_BANNER;
            case BLUE -> Material.BLUE_BANNER;
            case BROWN -> Material.BROWN_BANNER;
            case GREEN -> Material.GREEN_BANNER;
            case RED -> Material.RED_BANNER;
            case BLACK -> Material.BLACK_BANNER;
        };
    }

    /**
     * Converts a DyeColor to the corresponding dye Material (for display in GUI).
     */
    public static Material dyeColorToDyeMaterial(DyeColor color) {
        return switch (color) {
            case WHITE -> Material.WHITE_DYE;
            case ORANGE -> Material.ORANGE_DYE;
            case MAGENTA -> Material.MAGENTA_DYE;
            case LIGHT_BLUE -> Material.LIGHT_BLUE_DYE;
            case YELLOW -> Material.YELLOW_DYE;
            case LIME -> Material.LIME_DYE;
            case PINK -> Material.PINK_DYE;
            case GRAY -> Material.GRAY_DYE;
            case LIGHT_GRAY -> Material.LIGHT_GRAY_DYE;
            case CYAN -> Material.CYAN_DYE;
            case PURPLE -> Material.PURPLE_DYE;
            case BLUE -> Material.BLUE_DYE;
            case BROWN -> Material.BROWN_DYE;
            case GREEN -> Material.GREEN_DYE;
            case RED -> Material.RED_DYE;
            case BLACK -> Material.BLACK_DYE;
        };
    }

    /**
     * Builds the full banner ItemStack from the current state.
     * This is used for both the live preview and the final output.
     */
    public ItemStack buildBanner() {
        Material bannerMat = dyeColorToBannerMaterial(baseColor);
        ItemStack banner = new ItemStack(bannerMat);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();

        for (Pattern pattern : layers) {
            meta.addPattern(pattern);
        }

        banner.setItemMeta(meta);
        return banner;
    }
}
