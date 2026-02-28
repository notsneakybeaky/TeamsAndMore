package main.io.github.itshaithamn.teamsandmore;

import main.io.github.itshaithamn.teamsandmore.commands.Commands;
import main.io.github.itshaithamn.teamsandmore.teammanager.Caching;
import main.io.github.itshaithamn.teamsandmore.teammanager.TeamDatabaseManager;
import main.io.github.itshaithamn.teamsandmore.teammanager.TeamManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;

public class Main extends JavaPlugin implements Listener {
    private Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    private static TeamDatabaseManager dbManager;
    private static TeamManager teamManager;

    //Need to also cache all team names on start up, shouldnt affect the giga wam.
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        this.getCommand("team").setExecutor(new Commands());
        dbManager = new TeamDatabaseManager(new File("build/test-db"));
        teamManager = new TeamManager(scoreboard, dbManager);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Component.text("Hello, " + event.getPlayer().getName() + "!"));
    }

    @EventHandler
    public void preLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        //Do the team logic shit here that I was talking about
    }

//    @Override
//    public void onDisable() {
//        getLogger().info("Server shutting down! Forcing final database flush...");
//
//        // 1. Swap the maps one last time
//        ConcurrentHashMap<UUID, TeamData> finalFlush = activeCache;
//
//        // 2. We DO NOT run this asynchronously here.
//        // We run it synchronously on the main thread to intentionally hang the server.
//        flushToDatabase(finalFlush);
//
//        getLogger().info("All team data safely secured in SQLite.");
//
//        // 3. Now it is safe to close the HikariCP pool
//        if (source != null) {
//            source.close();
//        }
//    }
}