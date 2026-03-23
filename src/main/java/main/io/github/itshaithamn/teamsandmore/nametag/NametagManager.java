package main.io.github.itshaithamn.teamsandmore.nametag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
     * Creates a LuckPerms group for a new team with a white prefix.
     * Format: "&f[TeamName] &r"
     */
    public CompletableFuture<Void> createTeamGroup(String teamName) {
        return createTeamGroup(teamName, NametagColor.WHITE);
    }

    /**
     * Creates a LuckPerms group for a new team with the given color.
     * Format: "<color>[TeamName] &r"
     */
    public CompletableFuture<Void> createTeamGroup(String teamName, NametagColor color) {
        try {
            LuckPerms lp = LuckPermsProvider.get();
            String groupName = teamGroupName(teamName);

            return lp.getGroupManager().createAndLoadGroup(groupName).thenCompose(group ->
                    lp.getGroupManager().modifyGroup(groupName, g -> {
                        String prefix = color.getColorCode() + "[" + teamName + "] &r";
                        PrefixNode prefixNode = PrefixNode.builder(prefix, TEAM_PREFIX_PRIORITY).build();
                        g.data().add(prefixNode);
                    })
            ).thenRun(() ->
                    logger.info("[Nametag] Created LuckPerms group '" + groupName + "' with prefix " + color.getColorCode() + "[" + teamName + "]")
            );
        } catch (IllegalStateException e) {
            logger.warning("[Nametag] LuckPerms API not available — could not create team group for " + teamName);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Changes the prefix color of an existing team's LuckPerms group.
     * Removes the old prefix node and adds a new one with the updated color.
     */
    public CompletableFuture<Void> setTeamColor(String teamName, NametagColor color) {
        try {
            LuckPerms lp = LuckPermsProvider.get();
            String groupName = teamGroupName(teamName);

            return lp.getGroupManager().modifyGroup(groupName, g -> {
                // Remove all existing prefix nodes at our priority
                g.data().clear(NodeType.PREFIX::matches);

                // Add the new colored prefix
                String prefix = color.getColorCode() + "[" + teamName + "] &r";
                PrefixNode prefixNode = PrefixNode.builder(prefix, TEAM_PREFIX_PRIORITY).build();
                g.data().add(prefixNode);
            }).thenRun(() -> {
                logger.info("[Nametag] Updated team '" + teamName + "' prefix color to " + color.name());
                // Refresh nametags for all online players after a short delay
                Bukkit.getScheduler().runTaskLater(
                        Bukkit.getPluginManager().getPlugin("TeamsAndMore"),
                        this::refreshAllNametags, 10L
                );
            });
        } catch (IllegalStateException e) {
            logger.warning("[Nametag] LuckPerms API not available — could not change color for " + teamName);
            return CompletableFuture.completedFuture(null);
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
    public CompletableFuture<Void> onPlayerAddedToTeam(String teamName, String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) return CompletableFuture.completedFuture(null);

        try {
            LuckPerms lp = LuckPermsProvider.get();
            String groupName = teamGroupName(teamName);

            return lp.getUserManager().modifyUser(player.getUniqueId(), user -> {
                InheritanceNode node = InheritanceNode.builder(groupName).build();
                user.data().add(node);
            }).thenRun(() -> {
                Bukkit.getScheduler().runTaskLater(
                        Bukkit.getPluginManager().getPlugin("TeamsAndMore"),
                        () -> applyNametag(player), 10L
                );
            });
        } catch (IllegalStateException e) {
            logger.warning("[Nametag] LuckPerms API not available — could not add " + playerName + " to team group");
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Removes a player from their team's LuckPerms group.
     */
    public void onPlayerRemovedFromTeam(String teamName, String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        UUID uuid = player != null ? player.getUniqueId() : null;

        if (uuid == null) {
            uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        }

        try {
            LuckPerms lp = LuckPermsProvider.get();
            String groupName = teamGroupName(teamName);

            final Player playerRef = player;
            lp.getUserManager().modifyUser(uuid, user -> {
                InheritanceNode node = InheritanceNode.builder(groupName).build();
                user.data().remove(node);
            }).thenRun(() -> {
                if (playerRef != null && playerRef.isOnline()) {
                    Bukkit.getScheduler().runTaskLater(
                            Bukkit.getPluginManager().getPlugin("TeamsAndMore"),
                            () -> applyNametag(playerRef), 10L
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
            } else {
                team.prefix(Component.empty());
            }

            if (suffix != null) {
                Component suffixComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(suffix);
                team.suffix(suffixComponent);
            } else {
                team.suffix(Component.empty());
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
     */
    private String teamGroupName(String teamName) {
        return "team-" + teamName.toLowerCase().replaceAll("[^a-z0-9-]", "");
    }
}