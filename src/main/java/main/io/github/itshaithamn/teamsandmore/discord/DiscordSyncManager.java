package main.io.github.itshaithamn.teamsandmore.discord;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.util.DiscordUtil;
import main.io.github.itshaithamn.teamsandmore.teammanager.TeamDatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Bridges team membership from the plugin database to Discord roles via DiscordSRV.
 * <p>
 * When a player joins/leaves a team, their linked Discord account is given or
 * removed from a Discord role matching the team name. Roles are created on
 * the fly if they don't exist yet.
 */
public class DiscordSyncManager {

    private final JavaPlugin plugin;
    private final Scoreboard scoreboard;
    private final Logger logger;

    public DiscordSyncManager(JavaPlugin plugin, Scoreboard scoreboard) {
        this.plugin = plugin;
        this.scoreboard = scoreboard;
        this.logger = plugin.getLogger();
    }

    /**
     * Checks whether DiscordSRV is loaded and ready.
     */
    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("DiscordSRV");
    }

    // ──────────────────────────────────────────────
    //  Public API called by TeamManager / listeners
    // ──────────────────────────────────────────────

    /**
     * Grants the Discord role for {@code teamName} to the player.
     * Runs asynchronously to avoid blocking the main thread.
     */
    public void onPlayerJoinedTeam(UUID playerUUID, String teamName) {
        if (!isAvailable()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String discordId = getDiscordId(playerUUID);
                if (discordId == null) {
                    logger.info("No linked Discord account for " + playerUUID + ", skipping role sync.");
                    return;
                }

                Guild guild = getGuild();
                if (guild == null) return;

                Role role = getOrCreateRole(guild, teamName);
                if (role == null) return;

                Member member = guild.getMemberById(discordId);
                if (member == null) {
                    member = guild.retrieveMemberById(discordId).complete();
                }
                if (member == null) {
                    logger.warning("Discord member not found for ID " + discordId);
                    return;
                }

                Member finalMember = member;
                guild.addRoleToMember(member, role).queue(
                        success -> logger.info("Granted Discord role '" + teamName + "' to " + finalMember.getEffectiveName()),
                        error -> logger.warning("Failed to grant role: " + error.getMessage())
                );
            } catch (Exception e) {
                logger.warning("Discord role sync (join) failed for " + playerUUID + ": " + e.getMessage());
            }
        });
    }

    /**
     * Removes the Discord role for {@code teamName} from the player.
     * Runs asynchronously.
     */
    public void onPlayerLeftTeam(UUID playerUUID, String teamName) {
        if (!isAvailable()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String discordId = getDiscordId(playerUUID);
                if (discordId == null) return;

                Guild guild = getGuild();
                if (guild == null) return;

                Role role = findRole(guild, teamName);
                if (role == null) return; // Nothing to remove

                Member member = guild.getMemberById(discordId);
                if (member == null) {
                    member = guild.retrieveMemberById(discordId).complete();
                }
                if (member == null) return;

                Member finalMember = member;
                guild.removeRoleFromMember(member, role).queue(
                        success -> logger.info("Removed Discord role '" + teamName + "' from " + finalMember.getEffectiveName()),
                        error -> logger.warning("Failed to remove role: " + error.getMessage())
                );
            } catch (Exception e) {
                logger.warning("Discord role sync (leave) failed for " + playerUUID + ": " + e.getMessage());
            }
        });
    }

    /**
     * Full resync: iterates all scoreboard teams and ensures every linked
     * player has the correct Discord role. Useful on plugin enable.
     */
    public void fullResync() {
        if (!isAvailable()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Guild guild = getGuild();
            if (guild == null) return;

            for (Team team : scoreboard.getTeams()) {
                String teamName = team.getName();
                Role role = getOrCreateRole(guild, teamName);
                if (role == null) continue;

                for (String entry : team.getEntries()) {
                    Player player = Bukkit.getPlayerExact(entry);
                    if (player == null) continue;

                    try {
                        String discordId = getDiscordId(player.getUniqueId());
                        if (discordId == null) continue;

                        Member member = guild.getMemberById(discordId);
                        if (member == null) {
                            member = guild.retrieveMemberById(discordId).complete();
                        }
                        if (member == null) continue;

                        if (!member.getRoles().contains(role)) {
                            guild.addRoleToMember(member, role).queue();
                        }
                    } catch (Exception e) {
                        logger.warning("Resync failed for entry " + entry + ": " + e.getMessage());
                    }
                }
            }
            logger.info("Discord full resync complete.");
        });
    }

    /**
     * Retrieves the Discord user ID linked to this Minecraft UUID via DiscordSRV.
     */
    private String getDiscordId(UUID playerUUID) {
        return DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(playerUUID);
    }

    /**
     * Gets the main guild DiscordSRV is connected to.
     */
    private Guild getGuild() {
        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        if (guild == null) {
            logger.warning("DiscordSRV main guild is null — is the bot connected?");
        }
        return guild;
    }

    /**
     * Finds an existing role by name (case-insensitive).
     */
    private Role findRole(Guild guild, String teamName) {
        return guild.getRolesByName(teamName, true).stream().findFirst().orElse(null);
    }

    /**
     * Finds or creates a Discord role matching the team name.
     */
    private Role getOrCreateRole(Guild guild, String teamName) {
        Role existing = findRole(guild, teamName);
        if (existing != null) return existing;

        try {
            Role created = guild.createRole()
                    .setName(teamName)
                    .setMentionable(true)
                    .complete();
            logger.info("Created Discord role: " + teamName);
            return created;
        } catch (Exception e) {
            logger.warning("Failed to create Discord role '" + teamName + "': " + e.getMessage());
            return null;
        }
    }
}
