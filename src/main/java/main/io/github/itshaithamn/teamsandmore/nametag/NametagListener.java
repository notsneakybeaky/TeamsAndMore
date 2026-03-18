package main.io.github.itshaithamn.teamsandmore.nametag;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class NametagListener implements Listener {

    private final NametagManager nametagManager;

    public NametagListener(NametagManager nametagManager) {
        this.nametagManager = nametagManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().getServer().getScheduler().runTaskLater(
                event.getPlayer().getServer().getPluginManager()
                        .getPlugin("TeamsAndMore"),
                () -> nametagManager.refreshAllNametags(),
                5L
        );
    }
}