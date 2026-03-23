package main.io.github.itshaithamn.teamsandmore.nametag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.UUID;
import java.util.logging.Logger;

public class NametagManager {

    private final Scoreboard scoreboard;
    private final Logger logger;

    /** Prefix priority for team prefixes — high enough to override default groups. */
    private static final int TEAM_PREFIX_PRIORITY = 50;

    public NametagManager(Scoreboard scoreboard, Logger logger) {
        this.scoreboard = scoreboard;
        this.logger = logger;
    }

    /**
     * Creates a LuckPerms group for a new team with a colored prefix.
     * Called when a team is finalized.
     */
    public void createTeamGroup(String teamName) {
        try {
            LuckPerms lp = LuckPermsProvider.get();
            String groupName = teamGroupName(teamName);

            lp.getGroupManager().createAndLoadGroup(groupName).thenAcceptAsync(group -> {
                // Set the prefix: [TeamName]
                String prefix = "&f[" + teamName + "] ";
                PrefixNode prefixNode = PrefixNode.builder(prefix, TEAM_PREFIX_PRIORITY).build();
                group.data().add(prefixNode);

                lp.getGroupManager().saveGroup(group).thenRun(() ->
                        logger.info("[Nametag] Created LuckPerms group '" + groupName + "' with prefix [" + teamName + "]")
                );
            });
        } catch (IllegalStateException e) {
            logger.warning("[Nametag] LuckPerms API not available — could not create team group for " + teamName);
        }
    }

    /**
     * Deletes the LuckPerms group for a disbanded team.
     */
    public void deleteTeamGroup(String teamName) {
        try {
            LuckPerms lp = LuckPermsProvider.get();
            String groupName = teamGroupName(teamName);

            lp.getGroupManager().loadGroup(groupName).thenAccept(optionalGroup -> {
                if (optionalGroup.isPresent()) {
                    lp.getGroupManager().deleteGroup(optionalGroup.get()).thenRun(() ->
                            logger.info("[Nametag] Deleted LuckPerms group '" + groupName + "'")
                    );
                }
            });
        } catch (IllegalStateException e) {
            logger.warning("[Nametag] LuckPerms API not available — could not delete team group for " + teamName);
        }
    }

    /**
     * Adds a player to their team's LuckPerms group.
     */
    public void onPlayerAddedToTeam(String teamName, String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) return;

        try {
            LuckPerms lp = LuckPermsProvider.get();
            String groupName = teamGroupName(teamName);

            lp.getUserManager().modifyUser(player.getUniqueId(), user -> {
                InheritanceNode node = InheritanceNode.builder(groupName).build();
                user.data().add(node);
            }).thenRun(() -> {
                // Refresh scoreboard nametag after LP data updates
                Bukkit.getScheduler().runTaskLater(
                        Bukkit.getPluginManager().getPlugin("TeamsAndMore"),
                        () -> applyNametag(player), 5L
                );
            });
        } catch (IllegalStateException e) {
            logger.warning("[Nametag] LuckPerms API not available — could not add " + playerName + " to team group");
        }
    }

    /**
     * Removes a player from their team's LuckPerms group.
     */
    public void onPlayerRemovedFromTeam(String teamName, String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        UUID uuid = player != null ? player.getUniqueId() : null;

        // Try to get UUID from Bukkit's offline player if they're not online
        if (uuid == null) {
            uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        }

        try {
            LuckPerms lp = LuckPermsProvider.get();
            String groupName = teamGroupName(teamName);

            lp.getUserManager().modifyUser(uuid, user -> {
                InheritanceNode node = InheritanceNode.builder(groupName).build();
                user.data().remove(node);
            }).thenRun(() -> {
                if (player != null && player.isOnline()) {
                    Bukkit.getScheduler().runTaskLater(
                            Bukkit.getPluginManager().getPlugin("TeamsAndMore"),
                            () -> applyNametag(player), 5L
                    );
                }
            });
        } catch (IllegalStateException e) {
            logger.warning("[Nametag] LuckPerms API not available — could not remove " + playerName + " from team group");
        }
    }

    /**
     * Applies the LuckPerms prefix/suffix to a player's scoreboard team.
     */
    public void applyNametag(Player player) {
        Team team = scoreboard.getEntryTeam(player.getName());
        if (team == null) return;

        try {
            LuckPerms lp = LuckPermsProvider.get();
            CachedMetaData meta = lp.getPlayerAdapter(Player.class).getMetaData(player);

            String prefix = meta.getPrefix();
            String suffix = meta.getSuffix();

            if (prefix != null) {
                Component prefixComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix);
                team.prefix(prefixComponent);
            }

            if (suffix != null) {
                Component suffixComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(suffix);
                team.suffix(suffixComponent);
            }

        } catch (IllegalStateException e) {
            logger.warning("[Nametag] LuckPerms API not available yet — skipping nametag for " + player.getName());
        }
    }

    /**
     * Refreshes nametags for all online players who are on a team.
     */
    public void refreshAllNametags() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyNametag(player);
        }
    }

    /**
     * Converts a team name to a valid LuckPerms group name.
     * LuckPerms group names must be lowercase alphanumeric.
     */
    private String teamGroupName(String teamName) {
        return "team-" + teamName.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}