package main.io.github.itshaithamn.teamsandmore.nametag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class NametagManager {

    private final Scoreboard scoreboard;
    private final Logger logger;

    private final Map<String, NametagColor> teamColors = new HashMap<>();

    public NametagManager(Scoreboard scoreboard, Logger logger) {
        this.scoreboard = scoreboard;
        this.logger = logger;
    }

    public void assignTeamColor(String teamName, NametagColor color) {
        teamColors.put(teamName, color);
        applyToTeam(teamName, color);
    }

    public NametagColor getOrAssignColor(String teamName) {
        return teamColors.computeIfAbsent(teamName, name -> {
            NametagColor[] colors = NametagColor.values();
            NametagColor picked = colors[Math.abs(name.hashCode()) % colors.length];
            applyToTeam(teamName, picked);
            return picked;
        });
    }

    private void applyToTeam(String teamName, NametagColor color) {
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            logger.warning("Cannot apply nametag color — team '" + teamName + "' not found on scoreboard.");
            return;
        }

        TextColor textColor = color.getTextColor();

        team.color(NamedTextColor.nearestTo(textColor));
        team.prefix(Component.text("[" + teamName + "] ", textColor));
    }

    public void refreshAllNametags() {
        for (Team team : scoreboard.getTeams()) {
            NametagColor color = getOrAssignColor(team.getName());
            applyToTeam(team.getName(), color);
        }
    }

    public void onPlayerAddedToTeam(String teamName, String playerName) {
        getOrAssignColor(teamName);
    }

    public void onPlayerRemovedFromTeam(String teamName, String playerName) {
        // No-op — team prefix stays, player is just no longer an entry.
    }
}