package main.io.github.itshaithamn.teamsandmore.nametag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
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

    /** Prefix priority for team prefixes — MUST be high enough to override default groups. */
    private static final int TEAM_PREFIX_PRIORITY = 1000;

    public NametagManager(Scoreboard scoreboard, Logger logger) {
        this.scoreboard = scoreboard;
        this.logger = logger;
    }

    /**
     * Creates a LuckPerms group for a new team with a white prefix.
     */
    public CompletableFuture<Void> createTeamGroup(String teamName) {
        return createTeamGroup(teamName, NametagColor.WHITE);
    }

    /**
     * Creates a LuckPerms group for a new team with the given color.
     */
    public CompletableFuture<Void> createTeamGroup(String teamName, NametagColor color) {
        LuckPerms lp = LuckPermsProvider.get();
        String groupName = teamGroupName(teamName);

        // Load group instance -> modify it directly -> manually save to guarantee execution order
        return lp.getGroupManager().createAndLoadGroup(groupName).thenCompose(group -> {
            String prefix = color.getColorCode() + "[" + teamName + "] &r";
            PrefixNode prefixNode = PrefixNode.builder(prefix, TEAM_PREFIX_PRIORITY).build();
            group.data().clear(NodeType.PREFIX::matches);
            group.data().add(prefixNode);
            return lp.getGroupManager().saveGroup(group);
        }).thenRun(() -> {
            logger.info("[Nametag] Created/Saved LuckPerms group '" + groupName + "' with prefix " + color.getColorCode() + "[" + teamName + "]");
        }).exceptionally(e -> {
            logger.warning("[Nametag] Failed to create team group for " + teamName + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        });
    }

    /**
     * Changes the prefix color of an existing team's LuckPerms group.
     */
    public CompletableFuture<Void> setTeamColor(String teamName, NametagColor color) {
        LuckPerms lp = LuckPermsProvider.get();
        String groupName = teamGroupName(teamName);

        return lp.getGroupManager().loadGroup(groupName).thenCompose(optionalGroup -> {
            if (optionalGroup.isEmpty()) return CompletableFuture.completedFuture(null);
            Group group = optionalGroup.get();

            group.data().clear(NodeType.PREFIX::matches);
            String prefix = color.getColorCode() + "[" + teamName + "] &r";
            PrefixNode prefixNode = PrefixNode.builder(prefix, TEAM_PREFIX_PRIORITY).build();
            group.data().add(prefixNode);

            return lp.getGroupManager().saveGroup(group);
        }).thenRun(() -> {
            logger.info("[Nametag] Updated team '" + teamName + "' prefix color to " + color.name());
            Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugin("TeamsAndMore"),
                    this::refreshAllNametags, 10L
            );
        }).exceptionally(e -> {
            logger.warning("[Nametag] Failed to change color for " + teamName + ": " + e.getMessage());
            return null;
        });
    }

    /**
     * Deletes the LuckPerms group for a disbanded team.
     */
    public void deleteTeamGroup(String teamName) {
        LuckPerms lp = LuckPermsProvider.get();
        String groupName = teamGroupName(teamName);

        lp.getGroupManager().loadGroup(groupName).thenAccept(optionalGroup -> {
            if (optionalGroup.isPresent()) {
                lp.getGroupManager().deleteGroup(optionalGroup.get()).thenRun(() ->
                        logger.info("[Nametag] Deleted LuckPerms group '" + groupName + "'")
                );
            }
        });
    }

    /**
     * Adds a player to their team's LuckPerms group.
     */
    public CompletableFuture<Void> onPlayerAddedToTeam(String teamName, String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) return CompletableFuture.completedFuture(null);

        LuckPerms lp = LuckPermsProvider.get();
        String groupName = teamGroupName(teamName);

        // Load user explicitly -> modify -> manually save to guarantee execution order
        return lp.getUserManager().loadUser(player.getUniqueId()).thenCompose(user -> {
            InheritanceNode node = InheritanceNode.builder(groupName).build();
            user.data().add(node);
            return lp.getUserManager().saveUser(user);
        }).thenRun(() -> {
            // Increased delay slightly to 20 ticks (1 sec) to ensure LP cache recalculates
            Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugin("TeamsAndMore"),
                    () -> applyNametag(player), 20L
            );
        }).exceptionally(e -> {
            logger.warning("[Nametag] Failed to add " + playerName + " to team group: " + e.getMessage());
            e.printStackTrace();
            return null;
        });
    }

    /**
     * Removes a player from their team's LuckPerms group.
     */
    public void onPlayerRemovedFromTeam(String teamName, String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        UUID uuid = player != null ? player.getUniqueId() : Bukkit.getOfflinePlayer(playerName).getUniqueId();

        LuckPerms lp = LuckPermsProvider.get();
        String groupName = teamGroupName(teamName);
        final Player playerRef = player;

        lp.getUserManager().loadUser(uuid).thenCompose(user -> {
            InheritanceNode node = InheritanceNode.builder(groupName).build();
            user.data().remove(node);
            return lp.getUserManager().saveUser(user);
        }).thenRun(() -> {
            if (playerRef != null && playerRef.isOnline()) {
                Bukkit.getScheduler().runTaskLater(
                        Bukkit.getPluginManager().getPlugin("TeamsAndMore"),
                        () -> applyNametag(playerRef), 10L
                );
            }
        });
    }

    /**
     * Applies the LuckPerms prefix/suffix to a player's scoreboard team.
     */
    /**
     * Applies the LuckPerms prefix/suffix to a player's scoreboard team.
     */
    public void applyNametag(Player player) {
        Team team = scoreboard.getEntryTeam(player.getName());
        if (team == null) {
            logger.info("[Nametag DEBUG] No scoreboard team found for " + player.getName());
            return;
        }

        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(player.getUniqueId());

            if (user == null) {
                logger.warning("[Nametag DEBUG] User not found in LuckPerms memory for " + player.getName());
                return;
            }

            // BYPASS CACHE: Manually resolve the live inheritance tree to find the highest prefix
            String prefix = user.resolveInheritedNodes(net.luckperms.api.query.QueryOptions.nonContextual()).stream()
                    .filter(NodeType.PREFIX::matches)
                    .map(NodeType.PREFIX::cast)
                    .max(java.util.Comparator.comparingInt(net.luckperms.api.node.types.PrefixNode::getPriority))
                    .map(net.luckperms.api.node.types.PrefixNode::getMetaValue)
                    .orElse(null);

            // Do the same for the suffix just in case you ever use them
            String suffix = user.resolveInheritedNodes(net.luckperms.api.query.QueryOptions.nonContextual()).stream()
                    .filter(NodeType.SUFFIX::matches)
                    .map(NodeType.SUFFIX::cast)
                    .max(java.util.Comparator.comparingInt(net.luckperms.api.node.types.SuffixNode::getPriority))
                    .map(net.luckperms.api.node.types.SuffixNode::getMetaValue)
                    .orElse(null);

            logger.info("[Nametag DEBUG] Player: " + player.getName()
                    + " | Team: " + team.getName()
                    + " | Live Resolved Prefix: " + prefix
                    + " | Live Resolved Suffix: " + suffix);

            if (prefix != null) {
                Component prefixComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + " ");
                team.prefix(prefixComponent);
                logger.info("[Nametag DEBUG] Successfully set team prefix for " + player.getName());
            } else {
                team.prefix(Component.empty());
            }

            if (suffix != null) {
                Component suffixComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(" " + suffix);
                team.suffix(suffixComponent);
            } else {
                team.suffix(Component.empty());
            }

        } catch (IllegalStateException e) {
            logger.warning("[Nametag] LuckPerms API not available yet — skipping nametag for " + player.getName());
        }
    }

    public void refreshAllNametags() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyNametag(player);
        }
    }

    private String teamGroupName(String teamName) {
        return "team-" + teamName.toLowerCase().replaceAll("[^a-z0-9-]", "");
    }
}