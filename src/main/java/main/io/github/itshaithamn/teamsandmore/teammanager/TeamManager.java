package main.io.github.itshaithamn.teamsandmore.teammanager;

import main.io.github.itshaithamn.teamsandmore.discord.DiscordSyncManager;
import main.io.github.itshaithamn.teamsandmore.nametag.NametagColor;
import main.io.github.itshaithamn.teamsandmore.nametag.NametagManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;


public class TeamManager {
    Scoreboard scoreboard;
    public Caching caching;

    private NametagManager nametagManager;
    private DiscordSyncManager discordSyncManager;

    // Tracks pending team creations waiting for player responses
    private final Map<UUID, PendingTeamCreation> pendingCreations = new WeakHashMap<>();

    // Tracks pending single-player invites (from /team invite)
    private final Map<UUID, PendingInvite> pendingInvites = new WeakHashMap<>();

    /** Timeout in milliseconds before pending invites auto-reject. */
    private static final long INVITE_TIMEOUT_MS = 30_000;

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
        if (scoreboard.getEntryTeam(player.getName()) != null) {
            player.sendMessage("§cYou are already in a team.");
            return;
        }

        Set<Player> nearbyPlayers = findClosestPlayers(player);
        if (!player.hasPermission("teamsandmore.admin") && nearbyPlayers.size() < 4) {
            player.sendMessage("You need at least 4 players within a 25 block radius to create a team.");
            return;
        }

        if (scoreboard.getTeam(teamName) != null) {
            player.sendMessage("A team with that name already exists.");
            return;
        }

        // Check if the creator already has a pending invite out
        if (pendingCreations.values().stream().anyMatch(p -> p.leader.equals(player.getUniqueId()))) {
            player.sendMessage("§cYou already have a pending team creation. Wait for it to finish or expire.");
            return;
        }

        // Admin bypass — skip invites entirely
        if (player.hasPermission("teamsandmore.admin")) {
            finalizeTeam(player, teamName, nearbyPlayers);
            return;
        }

        // Create the pending creation and send invites
        PendingTeamCreation pending = new PendingTeamCreation(player.getUniqueId(), teamName, nearbyPlayers);
        for (Player invited : nearbyPlayers) {
            pendingCreations.put(invited.getUniqueId(), pending);
        }

        // Send clickable accept/reject messages
        Component acceptBtn = Component.text("[ACCEPT]")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/team accept"));

        Component rejectBtn = Component.text("[REJECT]")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/team reject"));

        Component inviteMessage = Component.text(player.getName() + " wants you to join team ", NamedTextColor.YELLOW)
                .append(Component.text(teamName, NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("! ", NamedTextColor.YELLOW))
                .append(acceptBtn)
                .append(Component.text(" "))
                .append(rejectBtn);

        for (Player invited : nearbyPlayers) {
            invited.sendMessage(inviteMessage);
        }

        player.sendMessage("§aTeam invites sent! Waiting for all players to respond (30s timeout)...");
    }

    /**
     * Handles a player accepting or rejecting a pending invite.
     * Checks both team creation invites and single-player invites.
     */
    public void handleInviteResponse(Player player, boolean accepted) {
        // Check for a single-player invite first
        PendingInvite invite = pendingInvites.get(player.getUniqueId());
        if (invite != null) {
            pendingInvites.remove(player.getUniqueId());

            // Check expiry
            if (System.currentTimeMillis() - invite.createdAt > INVITE_TIMEOUT_MS) {
                player.sendMessage("§cThat invite has expired.");
                Player inviter = Bukkit.getPlayer(invite.inviter);
                if (inviter != null) {
                    inviter.sendMessage("§cInvite to " + player.getName() + " expired.");
                }
                return;
            }

            if (accepted) {
                player.sendMessage("§aYou accepted the invite to team " + invite.teamName + "!");
                Player inviter = Bukkit.getPlayer(invite.inviter);
                if (inviter != null) {
                    inviter.sendMessage("§a" + player.getName() + " accepted the invite!");
                }
                finalizeAddPlayer(invite);
            } else {
                player.sendMessage("§cYou rejected the invite to team " + invite.teamName + ".");
                Player inviter = Bukkit.getPlayer(invite.inviter);
                if (inviter != null) {
                    inviter.sendMessage("§c" + player.getName() + " rejected the invite.");
                }
            }
            return;
        }

        // Check for a team creation invite
        PendingTeamCreation pending = pendingCreations.get(player.getUniqueId());
        if (pending == null || pending.resolved) {
            player.sendMessage("§cYou don't have a pending team invite.");
            return;
        }

        // Check if the invite has expired
        if (System.currentTimeMillis() - pending.createdAt > INVITE_TIMEOUT_MS) {
            cancelPendingCreation(pending, "§cTeam creation timed out — not all players responded in time.");
            return;
        }

        if (accepted) {
            pending.accepted.add(player.getUniqueId());
            player.sendMessage("§aYou accepted the invite to team " + pending.teamName + ".");

            Player leader = Bukkit.getPlayer(pending.leader);
            if (leader != null) {
                leader.sendMessage("§a" + player.getName() + " accepted the invite. ("
                        + pending.accepted.size() + "/" + pending.invited.size() + ")");
            }

            // Check if all players have accepted
            if (pending.accepted.size() == pending.invited.size()) {
                pending.resolved = true;
                cleanupPending(pending);

                if (leader != null) {
                    finalizeTeam(leader, pending.teamName, pending.invited);
                } else {
                    cancelPendingCreation(pending, "§cTeam creation failed — the leader went offline.");
                }
            }
        } else {
            pending.resolved = true;
            player.sendMessage("§cYou rejected the invite to team " + pending.teamName + ".");
            cancelPendingCreation(pending, "§c" + player.getName() + " rejected the invite. Team creation cancelled.");
        }
    }

    /**
     * Cancels a pending team creation and notifies the leader and remaining players.
     */
    private void cancelPendingCreation(PendingTeamCreation pending, String reason) {
        pending.resolved = true;

        Player leader = Bukkit.getPlayer(pending.leader);
        if (leader != null) {
            leader.sendMessage(reason);
        }

        for (Player invited : pending.invited) {
            if (invited.isOnline() && !pending.accepted.contains(invited.getUniqueId())) {
                invited.sendMessage("§cTeam creation for " + pending.teamName + " has been cancelled.");
            }
        }

        cleanupPending(pending);
    }

    /**
     * Removes all entries for a pending creation from the tracking map.
     */
    private void cleanupPending(PendingTeamCreation pending) {
        pendingCreations.values().removeIf(p -> p == pending);
    }

    /**
     * Actually creates the team on the scoreboard and in the database.
     * Called after all invited players have accepted (or by admin bypass).
     */
    private void finalizeTeam(Player leader, String teamName, Set<Player> members) {
        // Re-check team name availability (could have been taken during the invite window)
        if (scoreboard.getTeam(teamName) != null) {
            leader.sendMessage("§cA team with that name was created while waiting for responses.");
            return;
        }

        Team team;
        try {
            team = scoreboard.registerNewTeam(teamName);
        } catch (IllegalArgumentException ex) {
            leader.sendMessage("Failed to create team: invalid or duplicate team name.");
            return;
        }

        team.addEntry(leader.getName());
        for (Player member : members) {
            team.addEntry(member.getName());
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        Location locationBukkit = new Location(leader.getWorld(), leader.getX(), leader.getY(), leader.getZ());
        String location = locationBukkit.toString();
        try {
            caching.cache(new DBRecords.createTeamRecord(teamName, location));
            caching.cache(new DBRecords.addToTeamRecord(
                    leader.getUniqueId().toString(), teamName, "leader", 0, now
            ));

            for (Player member : members) {
                caching.cache(new DBRecords.addToTeamRecord(
                        member.getUniqueId().toString(), teamName, "member", 1, now
                ));
            }

            leader.sendMessage("§aTeam '" + teamName + "' created with " + (members.size() + 1) + " members!");
            for (Player member : members) {
                if (member.isOnline()) {
                    member.sendMessage("§aYou are now a member of team " + teamName + "!");
                }
            }
            caching.flushNow();

            // Notify nametag system
            if (nametagManager != null) {
                nametagManager.refreshAllNametags();
            }

            // Notify Discord system
            if (discordSyncManager != null) {
                discordSyncManager.onPlayerJoinedTeam(leader.getUniqueId(), teamName);
                for (Player member : members) {
                    discordSyncManager.onPlayerJoinedTeam(member.getUniqueId(), teamName);
                }
            }

        } catch (Exception e) {
            Team created = scoreboard.getTeam(teamName);
            if (created != null) created.unregister();

            leader.sendMessage("Team creation failed. Please try again.");
            e.printStackTrace();
        }
    }

    /**
     * Tracks the state of a pending team creation waiting for player responses.
     */
    static class PendingTeamCreation {
        final UUID leader;
        final String teamName;
        final Set<Player> invited;
        final Set<UUID> accepted = new HashSet<>();
        final long createdAt = System.currentTimeMillis();
        boolean resolved = false;

        PendingTeamCreation(UUID leader, String teamName, Set<Player> invited) {
            this.leader = leader;
            this.teamName = teamName;
            this.invited = invited;
        }
    }

    /**
     * Tracks a single-player invite to an existing team.
     */
    static class PendingInvite {
        final UUID inviter;
        final UUID target;
        final String teamName;
        final long createdAt = System.currentTimeMillis();

        PendingInvite(UUID inviter, UUID target, String teamName) {
            this.inviter = inviter;
            this.target = target;
            this.teamName = teamName;
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
        Team team = scoreboard.getEntryTeam(player.getName());
        if (team == null) {
            player.sendMessage("You are not in a team.");
            return;
        }

        String roleName = caching.getDbManager().getRoleName(player.getUniqueId().toString());
        if (!"leader".equalsIgnoreCase(roleName)) {
            player.sendMessage("§cOnly the team leader can invite players.");
            return;
        }

        Player target = Bukkit.getPlayerExact(targetPlayerName);
        if (target == null) {
            player.sendMessage("§cPlayer not found or offline.");
            return;
        }

        if (scoreboard.getEntryTeam(target.getName()) != null) {
            player.sendMessage("§c" + target.getName() + " is already in a team.");
            return;
        }

        if (pendingInvites.containsKey(target.getUniqueId())) {
            player.sendMessage("§c" + target.getName() + " already has a pending invite.");
            return;
        }

        // Store the pending invite
        PendingInvite invite = new PendingInvite(player.getUniqueId(), target.getUniqueId(), team.getName());
        pendingInvites.put(target.getUniqueId(), invite);

        // Send clickable accept/reject message to the target
        Component acceptBtn = Component.text("[ACCEPT]")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/team accept"));

        Component rejectBtn = Component.text("[REJECT]")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/team reject"));

        Component inviteMessage = Component.text(player.getName() + " invited you to join team ", NamedTextColor.YELLOW)
                .append(Component.text(team.getName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("! ", NamedTextColor.YELLOW))
                .append(acceptBtn)
                .append(Component.text(" "))
                .append(rejectBtn);

        target.sendMessage(inviteMessage);
        player.sendMessage("§aInvite sent to " + target.getName() + "! (30s timeout)");
    }

    /**
     * Finalizes adding a single invited player to an existing team.
     */
    private void finalizeAddPlayer(PendingInvite invite) {
        Player target = Bukkit.getPlayer(invite.target);
        if (target == null) return;

        Team team = scoreboard.getTeam(invite.teamName);
        if (team == null) return;

        team.addEntry(target.getName());
        try {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            caching.cache(new DBRecords.addToTeamRecord(
                    target.getUniqueId().toString(), invite.teamName, "member", 1, now));

            // Notify nametag system
            if (nametagManager != null) {
                nametagManager.onPlayerAddedToTeam(invite.teamName, target.getName());
            }

            // Notify Discord system
            if (discordSyncManager != null) {
                discordSyncManager.onPlayerJoinedTeam(target.getUniqueId(), invite.teamName);
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

        String roleName = caching.getDbManager().getRoleName(player.getUniqueId().toString());
        if (!"leader".equalsIgnoreCase(roleName)) {
            player.sendMessage("Only the team leader can remove players.");
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

    public void disbandTeam(Player player) {
        Team team = scoreboard.getEntryTeam(player.getName());

        if (team == null) {
            player.sendMessage("You are not in a team.");
            return;
        }

        String roleName = caching.getDbManager().getRoleName(player.getUniqueId().toString());
        if (!"leader".equalsIgnoreCase(roleName)) {
            player.sendMessage("§cOnly the team leader can disband the team.");
            return;
        }

        String teamName = team.getName();

        // Remove every member from DB and notify integrations
        for (String entry : new HashSet<>(team.getEntries())) {
            Player member = Bukkit.getPlayerExact(entry);

            caching.cache(new DBRecords.removeFromTeamRecord(
                    member != null ? member.getUniqueId().toString() : entry));

            if (nametagManager != null) {
                nametagManager.onPlayerRemovedFromTeam(teamName, entry);
            }

            if (discordSyncManager != null && member != null) {
                discordSyncManager.onPlayerLeftTeam(member.getUniqueId(), teamName);
            }

            if (member != null && !member.equals(player)) {
                member.sendMessage("§cTeam " + teamName + " has been disbanded.");
            }
        }

        team.unregister();
        caching.flushNow();

        player.sendMessage("§aTeam " + teamName + " has been disbanded.");
    }
}