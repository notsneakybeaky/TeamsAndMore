package main.io.github.itshaithamn.teamsandmore.gui;

import org.bukkit.inventory.ItemStack;

import java.util.Base64;

/**
 * Utility class for serializing/deserializing banner ItemStacks to Base64 strings.
 * Uses Paper's serializeAsBytes() which stores NBT with DataVersion for safe migration.
 *
 * Usage:
 *   String encoded = BannerSerializer.serialize(bannerItemStack);
 *   // store `encoded` in your database
 *
 *   ItemStack banner = BannerSerializer.deserialize(encoded);
 *   // give to player or display in GUI
 */
public class BannerSerializer {

    private BannerSerializer() {
        // utility class
    }

    /**
     * Serializes an ItemStack to a Base64-encoded NBT string.
     *
     * @param item the ItemStack to serialize
     * @return Base64-encoded string of the NBT bytes
     */
    public static String serialize(ItemStack item) {
        byte[] bytes = item.serializeAsBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Deserializes an ItemStack from a Base64-encoded NBT string.
     *
     * @param base64 the Base64-encoded string
     * @return the deserialized ItemStack
     */
    public static ItemStack deserialize(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return ItemStack.deserializeBytes(bytes);
    }
}
