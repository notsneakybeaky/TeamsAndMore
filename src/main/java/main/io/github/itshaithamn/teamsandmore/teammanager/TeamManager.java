package main.io.github.itshaithamn.teamsandmore.teammanager;

import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;

public class TeamManager {
    Scoreboard scoreboard;
    TeamDatabaseManager dbManager;

    public TeamManager(Scoreboard scoreboard, TeamDatabaseManager dbManager) {
        this.scoreboard = scoreboard;
        this.dbManager = dbManager;
        loadTeamsFromDatabase();
    }

    public void createNewTeam(String teamName) {
        //Logic about the joining a team and proximity goes here

    }


    public void loadTeamsFromDatabase() {
        List<String> allTeams = dbManager.getAllTeams();

        for (String team: allTeams) {
            scoreboard.registerNewTeam(team);
        }
    }
}
