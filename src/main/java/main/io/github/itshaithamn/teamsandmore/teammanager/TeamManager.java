package main.io.github.itshaithamn.teamsandmore.teammanager;

import main.io.github.itshaithamn.teamsandmore.discord.DiscordSyncManager;
import main.io.github.itshaithamn.teamsandmore.nametag.NametagColor;
import main.io.github.itshaithamn.teamsandmore.nametag.NametagManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class TeamManager {
    Scoreboard scoreboard;
    public Caching caching;

    // Optional integrations — null when the dependency is not present
    private NametagManager nametagManager;
    private DiscordSyncManager discordSyncManager;

    public TeamManager(Scoreboard scoreboard, TeamDatabaseManager dbManager) {
        this.scoreboard = scoreboard;
        this.caching = new Caching(dbManager);
    }

    public void setNametagManager(NametagManager nametagManager) {
        this.nametagManager = nametagManager;
    }

    public void setDiscordSyncManager(DiscordSyncManager discordSyncManager) {
        this.discordSyncManager = discordSyncManager;
    }

    /**
     * Sets the nametag color for the player's team. Only the team leader can change it.
     */
    public void setTeamColor(Player player, String colorName) {
        Team team = scoreboard.getEntryTeam(player.getName());
        if (team == null) {
            player.sendMessage("You are not in a team.");
            return;
        }

        String roleName = caching.getDbManager().getRoleName(player.getUniqueId().toString());
        if (!"leader".equalsIgnoreCase(roleName)) {
            player.sendMessage("§cOnly the team leader can change the team color.");
            return;
        }

        NametagColor color;
        try {
            color = NametagColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid color. Options: " + String.join(", ",
                    java.util.Arrays.stream(NametagColor.values())
                            .map(c -> c.name().toLowerCase())
                            .toArray(String[]::new)));
            return;
        }

        if (nametagManager == null) {
            player.sendMessage("§cNametag coloring is not available (ProtocolLib not installed).");
            return;
        }

        nametagManager.assignTeamColor(team.getName(), color);
        nametagManager.refreshAllNametags();
        player.sendMessage("§aTeam color set to " + color.getChatColor() + colorName.toLowerCase() + "§a.");
    }

    /**
     * Pre-loads a player into their scoreboard team on login.
     * Flushes pending writes first so the DB read is consistent.
     */
    public void preloadPlayer(Player player) {
        caching.flushNow();

        String uuid = player.getUniqueId().toString();
        String teamName = caching.getDbManager().getTeamNameByUUID(uuid);
        if (teamName == null) return;

        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        team.addEntry(player.getName());
    }

    public void createNewTeam(Player player, String teamName) {
        // Need a team check here.

        Set<Player> nearbyPlayers = findClosestPlayers(player);
        if (nearbyPlayers.size() < 4) {
            player.sendMessage("You need at least 4 players within a 25 block radius to create a team.");
            return;
        }

        if (scoreboard.getTeam(teamName) != null) {
            player.sendMessage("A team with that name already exists.");
            return;
        }

        Team team;
        try {
            team = scoreboard.registerNewTeam(teamName);
        } catch (IllegalArgumentException ex) {
            player.sendMessage("Failed to create team: invalid or duplicate team name.");
            return;
        }

        team.addEntry(player.getName());
        for (Player member : nearbyPlayers) {
            team.addEntry(member.getName());
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());

        // Use block location for waypoint
        Location wpLoc = player.getLocation().getBlock().getLocation();
        String waypoint = serializeLocation(wpLoc);

        try {
            // 4) Queue DB writes in order (important for FK integrity)
            caching.cache(new DBRecords.createTeamRecord(teamName, waypoint));
            caching.cache(new DBRecords.addToTeamRecord(
                    player.getUniqueId().toString(), teamName, "leader", 0, now
            ));

            for (Player member : nearbyPlayers) {
                caching.cache(new DBRecords.addToTeamRecord(
                        member.getUniqueId().toString(), teamName, "member", 1, now
                ));
            }

            // 5) Place waypoint block after successful queueing
            wpLoc.getBlock().setType(Material.LODESTONE);

            player.sendMessage("Team '" + teamName + "' created with " + (nearbyPlayers.size() + 1) + " members!");

            // Notify nametag system
            if (nametagManager != null) {
                nametagManager.refreshAllNametags();
            }

            // Notify Discord system
            if (discordSyncManager != null) {
                discordSyncManager.onPlayerJoinedTeam(player.getUniqueId(), teamName);
                for (Player member : nearbyPlayers) {
                    discordSyncManager.onPlayerJoinedTeam(member.getUniqueId(), teamName);
                }
            }
        } catch (Exception e) {
            // rollback scoreboard team if queueing fails
            Team created = scoreboard.getTeam(teamName);
            if (created != null) created.unregister();

            player.sendMessage("Team creation failed. Please try again.");
            e.printStackTrace();
        }
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }

    private Set<Player> findClosestPlayers(Player centerPlayer) {
        Set<Player> closestPlayers = new HashSet<>();

        centerPlayer.getNearbyEntities(25, 25, 25).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
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

    public void addPlayerToTeam(Player player, String targetPlayerName) {
        // Need to add permissions check here.

        Team team = scoreboard.getEntryTeam(player.getName());
        if (team == null) {
            player.sendMessage("You are not in a team.");
            return;
        }

        team.addEntry(targetPlayerName);
        try {
            Timestamp now = new Timestamp(System.currentTimeMillis());

            Player target = Bukkit.getPlayerExact(targetPlayerName);
            caching.cache(new DBRecords.addToTeamRecord(
                    target != null ? target.getUniqueId().toString() : targetPlayerName,
                    team.getName(), "member", 1, now));

            // Notify nametag system
            if (nametagManager != null) {
                nametagManager.onPlayerAddedToTeam(team.getName(), targetPlayerName);
            }

            // Notify Discord system
            if (discordSyncManager != null && target != null) {
                discordSyncManager.onPlayerJoinedTeam(target.getUniqueId(), team.getName());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void removePlayerFromTeam(Player player, String targetPlayerName) {
//        if (!player.hasPermission("teamsandmore.invite") &&
//                !player.hasPermission("teamsandmore.admin")) {
//            player.sendMessage("You don't have permission to remove players.");
//            return;
//        }

        Team inviterTeam = scoreboard.getEntryTeam(player.getName());
        if (inviterTeam == null) {
            player.sendMessage("You are not in a team.");
            return;
        }

        Player target = Bukkit.getPlayerExact(targetPlayerName);
        if (target == null) {
            player.sendMessage("That player is not online.");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("Use a leave command to remove yourself.");
            return;
        }

        Team targetTeam = scoreboard.getEntryTeam(target.getName());
        if (targetTeam == null || !targetTeam.getName().equals(inviterTeam.getName())) {
            player.sendMessage(target.getName() + " is not in your team.");
            return;
        }

        inviterTeam.removeEntry(target.getName());
        caching.cache(new DBRecords.removeFromTeamRecord(target.getUniqueId().toString()));
        player.sendMessage("Removed " + target.getName() + " from team " + inviterTeam.getName() + ".");
        target.sendMessage("You were removed from team " + inviterTeam.getName() + ".");

        // Notify nametag system
        if (nametagManager != null) {
            nametagManager.onPlayerRemovedFromTeam(inviterTeam.getName(), target.getName());
        }

        // Notify Discord system
        if (discordSyncManager != null) {
            discordSyncManager.onPlayerLeftTeam(target.getUniqueId(), inviterTeam.getName());
        }
    }

    public void leaveTeam(Player player) {
        Team team = scoreboard.getEntryTeam(player.getName());

        if (team == null) {
            player.sendMessage("You are not in a team.");
            return;
        }

        String roleName = caching.getDbManager().getRoleName(player.getUniqueId().toString());
        if ("leader".equalsIgnoreCase(roleName)) {
            player.sendMessage("§cYou are the team leader. Transfer leadership or disband the team before leaving.");
            return;
        }

        String teamName = team.getName();
        team.removeEntry(player.getName());
        caching.cache(new DBRecords.removeFromTeamRecord(player.getUniqueId().toString()));
        player.sendMessage("You have left team " + teamName + ".");

        // Notify nametag system
        if (nametagManager != null) {
            nametagManager.onPlayerRemovedFromTeam(teamName, player.getName());
        }

        // Notify Discord system
        if (discordSyncManager != null) {
            discordSyncManager.onPlayerLeftTeam(player.getUniqueId(), teamName);
        }
    }

    public void preloadPlayer(UUID uuid, String playerName) {
        caching.flushNow();

        String teamName = caching.getDbManager().getTeamNameByUUID(uuid.toString());
        if (teamName == null) return;

        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        team.addEntry(playerName);
    }
}