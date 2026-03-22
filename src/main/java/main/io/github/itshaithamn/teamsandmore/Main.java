package main.io.github.itshaithamn.teamsandmore;

import io.papermc.paper.event.player.AsyncChatEvent;
import main.io.github.itshaithamn.teamsandmore.commands.Commands;
import main.io.github.itshaithamn.teamsandmore.commands.TeamTabCompleter;
import main.io.github.itshaithamn.teamsandmore.discord.DiscordSyncListener;
import main.io.github.itshaithamn.teamsandmore.discord.DiscordSyncManager;
import main.io.github.itshaithamn.teamsandmore.gui.bannerui.BannerEditorManager;
import main.io.github.itshaithamn.teamsandmore.nametag.NametagListener;
import main.io.github.itshaithamn.teamsandmore.nametag.NametagManager;
import main.io.github.itshaithamn.teamsandmore.teammanager.TeamDatabaseManager;
import main.io.github.itshaithamn.teamsandmore.teammanager.TeamManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;

public class Main extends JavaPlugin implements Listener {
    private static TeamManager teamManager;
    private NametagManager nametagManager;
    Scoreboard scoreboard;


    @Override
    public void onEnable() {
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        Bukkit.getPluginManager().registerEvents(this, this);

        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();

        teamManager = new TeamManager(scoreboard, new TeamDatabaseManager(dataFolder));
        this.getCommand("team").setExecutor(new Commands(teamManager));
        this.getCommand("team").setTabCompleter(new TeamTabCompleter());
        // ── Nametag coloring (uses Bukkit Team API, no extra dependencies) ──
        nametagManager = new NametagManager(scoreboard, getLogger());
        Bukkit.getPluginManager().registerEvents(new NametagListener(nametagManager), this);
        teamManager.setNametagManager(nametagManager);
        Bukkit.getScheduler().runTaskLater(this, () -> nametagManager.refreshAllNametags(), 20L);

        // ── DiscordSRV sync (soft dependency) ──
        if (Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) {
            getLogger().info("DiscordSRV found — enabling team role sync.");
            DiscordSyncManager discordSyncManager = new DiscordSyncManager(this, scoreboard);
            teamManager.setDiscordSyncManager(discordSyncManager);

            // Register DiscordSRV API listener
            github.scarsz.discordsrv.DiscordSRV.api.subscribe(
                    new DiscordSyncListener(discordSyncManager, scoreboard, getLogger())
            );
        } else {
            getLogger().info("DiscordSRV not found — Discord role sync disabled.");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Component.text("Hello, " + event.getPlayer().getName() + "!"));
    }

    @EventHandler
    public void preLogin(AsyncPlayerPreLoginEvent event) {
        teamManager.preloadPlayer(event.getUniqueId(), event.getName());
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down — flushing database cache...");

        if (teamManager != null) {
            teamManager.caching.flushNow();
            teamManager.caching.stop();
            teamManager.caching.getDbManager().close();
        }

        getLogger().info("TeamsAndMore disabled cleanly.");
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Team team = scoreboard.getEntryTeam(player.getName());

        if (team == null) return;

        event.renderer((source, sourceDisplayName, message, viewer) ->
                team.prefix()
                        .append(sourceDisplayName)
                        .append(Component.text(": "))
                        .append(message)
        );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BannerEditorManager.cleanup(event.getPlayer());
    }
}