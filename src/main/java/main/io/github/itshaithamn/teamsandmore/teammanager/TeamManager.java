package main.io.github.itshaithamn.teamsandmore.teammanager;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;

public class TeamManager {
    Scoreboard scoreboard;
    TeamDatabaseManager dbManager;
    Caching caching;

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

        scoreboard.registerNewTeam(teamName);
    }


}
