package main.io.github.itshaithamn.teamsandmore.discord;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.logging.Logger;

/**
 * Subscribes to DiscordSRV API events so that:
 * <ul>
 *   <li>When DiscordSRV finishes connecting, a full resync is triggered.</li>
 *   <li>When a player links their Discord account, their current team role
 *       is immediately granted.</li>
 * </ul>
 * <p>
 * Register this with {@code DiscordSRV.api.subscribe(listener)}.
 */
public class DiscordSyncListener {

    private final DiscordSyncManager syncManager;
    private final Scoreboard scoreboard;
    private final Logger logger;

    public DiscordSyncListener(DiscordSyncManager syncManager, Scoreboard scoreboard, Logger logger) {
        this.syncManager = syncManager;
        this.scoreboard = scoreboard;
        this.logger = logger;
    }

    /**
     * Fired when DiscordSRV's JDA instance is ready and the bot is connected.
     */
    @Subscribe
    public void onDiscordReady(DiscordReadyEvent event) {
        logger.info("DiscordSRV ready — running full team role resync.");
        syncManager.fullResync();
    }

    /**
     * Fired when a player links their Minecraft account to Discord.
     * Immediately grants the team role if they're on a team.
     */
    @Subscribe
    public void onAccountLinked(AccountLinkedEvent event) {
        Player player = event.getPlayer().getPlayer();
        if (player == null) return;

        Team team = scoreboard.getEntryTeam(player.getName());
        if (team != null) {
            logger.info(player.getName() + " linked Discord — syncing team role '" + team.getName() + "'.");
            syncManager.onPlayerJoinedTeam(player.getUniqueId(), team.getName());
        }
    }
}
