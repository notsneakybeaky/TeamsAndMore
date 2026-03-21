package main.io.github.itshaithamn.teamsandmore.teammanager;

import main.io.github.itshaithamn.teamsandmore.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Timestamp;
import java.util.concurrent.CompletableFuture;

public interface DBRecords {
    void apply(TeamDatabaseManager db);


    record createTeamRecord(String teamName, String wayPointLocation) implements DBRecords {
        @Override
        public void apply(TeamDatabaseManager db) {
            db.createTeam(teamName, wayPointLocation);
        }
    }

    record addToTeamRecord(String uuid, String teamName, String roleName, int rolePriority, Timestamp dateJoined) implements DBRecords {
        @Override
        public void apply(TeamDatabaseManager db) {
            db.addToTeam(uuid, teamName, roleName, rolePriority, dateJoined);
        }
    }

    record removeFromTeamRecord(String uuid) implements DBRecords {
        @Override
        public void apply(TeamDatabaseManager db) {
            db.removeFromTeam(uuid);
        }
    }

    //Everytime a read is called a flush needs to done in the caching gangy.
    record rolePriorityPlayer(String uuid) implements DBRecords {
        @Override
        public void apply(TeamDatabaseManager db) {
            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(Main.class);

            CompletableFuture.supplyAsync(() -> db.getRolePriority(uuid)).handleAsync((priority, exception) -> {
                Player player = Bukkit.getPlayer(uuid);

                if (exception != null) {
                    player.sendMessage(Component.text("Error: " + exception.getMessage()));
                    return null;
                }

                if (player == null) {
                    return null;
                }

                if(priority == null) {
                    return null;
                }

                return priority;
            }, Bukkit.getScheduler().getMainThreadExecutor(plugin));
        }
    }
}