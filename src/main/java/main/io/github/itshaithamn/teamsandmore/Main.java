package main.io.github.itshaithamn.teamsandmore;

import main.io.github.itshaithamn.teamsandmore.teammanager.Caching;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
    private static Caching caching;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        this.getCommand("tm").setExecutor(new Commands());

        caching = new Caching();
    }

    public static Caching getCaching() {
        return caching;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Component.text("Hello, " + event.getPlayer().getName() + "!"));
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