package main.io.github.itshaithamn.teamsandmore.nametag;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public class NametagListener implements Listener {

    private final NametagManager nametagManager;

    public NametagListener(NametagManager nametagManager) {
        this.nametagManager = nametagManager;
    }

    /**
     * Register the LuckPerms event listener.
     * Call this from onEnable() after confirming LuckPerms is available.
     */
    public void registerLuckPermsListener(Plugin plugin) {
        try {
            LuckPerms lp = LuckPermsProvider.get();
            lp.getEventBus().subscribe(plugin, UserDataRecalculateEvent.class, event -> {
                // This fires whenever LP recalculates a player's cached data
                // (e.g. after group inheritance changes)
                Player player = Bukkit.getPlayer(event.getUser().getUniqueId());
                if (player != null && player.isOnline()) {
                    // Run on main thread since scoreboard ops need it
                    Bukkit.getScheduler().runTask(plugin, () -> nametagManager.applyNametag(player));
                }
            });
        } catch (IllegalStateException e) {
            // LuckPerms not available — fall back to delay-based refresh on join
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Backup: refresh on join with a delay in case LP event doesn't fire
        event.getPlayer().getServer().getScheduler().runTaskLater(
                event.getPlayer().getServer().getPluginManager()
                        .getPlugin("TeamsAndMore"),
                () -> nametagManager.refreshAllNametags(),
                40L
        );
    }
}