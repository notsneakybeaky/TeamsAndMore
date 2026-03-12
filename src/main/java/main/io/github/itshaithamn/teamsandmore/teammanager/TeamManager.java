package main.io.github.itshaithamn.teamsandmore.teammanager;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class TeamManager {
    Scoreboard scoreboard;
    TeamDatabaseManager dbManager;
    public Caching caching;

    public TeamManager(Scoreboard scoreboard, TeamDatabaseManager dbManager) {
        this.scoreboard = scoreboard;
        this.dbManager = dbManager;
        this.caching = new Caching(dbManager);
        loadTeamsFromDatabase();
    }

    public void loadTeamsFromDatabase() {
        List<String> allTeams = dbManager.getAllTeams();

        for (String team: allTeams) {
            scoreboard.registerNewTeam(team);
        }
    }

    public void createNewTeam(Player player, String teamName) {
        // Implementation, when a new team is created, the leader's location and 4 more members is grabbed,
        // a team is created before the caching is enabled. And if everything succeeds the waypoint block
        // is placed. I have no idea how tf to deal with the attributes of the waypoint block.
        Set<Player> nearbyPlayers = findClosestPlayers(player);

        if (nearbyPlayers.size() < 4) {
            player.sendMessage("You need at least 4 players to create a team.");
            return;
        }

        Team team = scoreboard.registerNewTeam(teamName);
        team.addEntry(player.getName());

        for (Player member : nearbyPlayers) {
            team.addEntry(member.getName());
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());

        caching.cache(
                Caching.DBAction.CREATETEAM,
                player.getUniqueId().toString(),
                teamName,
                "leader",
                0,
                now
        );

        for (Player member : nearbyPlayers) {
            caching.cache(
                    Caching.DBAction.ADDTOTEAM,
                    member.getUniqueId().toString(),
                    teamName,
                    "member",
                    1,
                    now
            );
        }

//        Adding the waypoint, which isnt going to be added now....
        Location loc = player.getLocation().clone();
        Block wpBlock = loc.getBlock();
        wpBlock.setType(Material.LODESTONE);

        player.sendMessage("Team '" + teamName + "' created with " + (nearbyPlayers.size() + 1) + " members!");
    }

    private Set<Player> findClosestPlayers(Player centerPlayer) {
        Set<Player> closestPlayers = new HashSet<>();

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(centerPlayer))
                .filter(p -> p.getWorld().equals(centerPlayer.getWorld()))
                .filter(p -> p.getLocation().distance(centerPlayer.getLocation()) <= 25)
                .sorted((p1, p2) -> Double.compare(
                        p1.getLocation().distance(centerPlayer.getLocation()),
                        p2.getLocation().distance(centerPlayer.getLocation())
                ))
                .limit(4)
                .forEach(closestPlayers::add);

        return closestPlayers;
    }

}
