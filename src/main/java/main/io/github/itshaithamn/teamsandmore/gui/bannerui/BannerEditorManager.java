package main.io.github.itshaithamn.teamsandmore.gui.bannerui;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages banner editor sessions. Each player gets their own BannerEditorState
 * while they are using the editor. State is cleared when they close the GUI or
 * finish editing.
 *
 * Usage from your command handler:
 *   BannerEditorManager.open(player, teamName);
 *
 * The manager will open Page 1 (base color picker) and track the player's
 * session through the editing flow.
 */
public class BannerEditorManager {

    private static final Map<UUID, BannerEditorState> sessions = new HashMap<>();
    private static final Map<UUID, String> teamNames = new HashMap<>();

    private BannerEditorManager() {
        // utility class
    }

    /**
     * Opens the banner editor for a player. Starts at the base color picker page.
     *
     * @param player   the player opening the editor
     * @param teamName the team this banner belongs to
     */
    public static void open(Player player, String teamName) {
        BannerEditorState state = new BannerEditorState();
        sessions.put(player.getUniqueId(), state);
        teamNames.put(player.getUniqueId(), teamName);
        BaseColorPage.open(player, state);
    }

    /**
     * Gets the current editor state for a player.
     *
     * @param player the player
     * @return the state, or null if not editing
     */
    public static BannerEditorState getState(Player player) {
        return sessions.get(player.getUniqueId());
    }

    /**
     * Gets the team name associated with the player's current editing session.
     *
     * @param player the player
     * @return the team name, or null if not editing
     */
    public static String getTeamName(Player player) {
        return teamNames.get(player.getUniqueId());
    }

    /**
     * Cleans up a player's session. Call this when they finish or close the editor.
     *
     * @param player the player
     */
    public static void cleanup(Player player) {
        sessions.remove(player.getUniqueId());
        teamNames.remove(player.getUniqueId());
    }
}
