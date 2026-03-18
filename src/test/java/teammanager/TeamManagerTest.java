package teammanager;

import main.io.github.itshaithamn.teamsandmore.discord.DiscordSyncManager;
import main.io.github.itshaithamn.teamsandmore.nametag.NametagManager;
import main.io.github.itshaithamn.teamsandmore.teammanager.TeamDatabaseManager;
import main.io.github.itshaithamn.teamsandmore.teammanager.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamManagerTest {

    @Mock private Scoreboard scoreboard;
    @Mock private TeamDatabaseManager dbManager;
    @Mock private NametagManager nametagManager;
    @Mock private DiscordSyncManager discordSyncManager;
    @Mock private Player player;
    @Mock private Player target;
    @Mock private Team team;

    private TeamManager teamManager;
    private final UUID playerUUID = UUID.randomUUID();
    private final UUID targetUUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        teamManager = new TeamManager(scoreboard, dbManager);
    }

    @Nested
    class LeaveTeamTests {
        @Test
        void notInTeam_shouldReject() {
            when(player.getName()).thenReturn("Alice");
            when(scoreboard.getEntryTeam("Alice")).thenReturn(null);
            teamManager.leaveTeam(player);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertEquals("You are not in a team.", captor.getValue());
        }

        @Test
        void leader_shouldReject() {
            when(player.getName()).thenReturn("Alice");
            when(player.getUniqueId()).thenReturn(playerUUID);
            when(scoreboard.getEntryTeam("Alice")).thenReturn(team);
            when(dbManager.getRoleName(playerUUID.toString())).thenReturn("leader");
            teamManager.leaveTeam(player);
            verify(team, never()).removeEntry(anyString());
        }

        @Test
        void leader_caseInsensitive_shouldReject() {
            when(player.getName()).thenReturn("Alice");
            when(player.getUniqueId()).thenReturn(playerUUID);
            when(scoreboard.getEntryTeam("Alice")).thenReturn(team);
            when(dbManager.getRoleName(playerUUID.toString())).thenReturn("Leader");
            teamManager.leaveTeam(player);
            verify(team, never()).removeEntry(anyString());
        }

        @Test
        void member_shouldLeaveSuccessfully() {
            when(player.getName()).thenReturn("Bob");
            when(player.getUniqueId()).thenReturn(playerUUID);
            when(scoreboard.getEntryTeam("Bob")).thenReturn(team);
            when(team.getName()).thenReturn("Alpha");
            when(dbManager.getRoleName(playerUUID.toString())).thenReturn("member");
            teamManager.leaveTeam(player);
            verify(team).removeEntry("Bob");
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertEquals("You have left team Alpha.", captor.getValue());
        }

        @Test
        void nullRole_shouldLeaveSuccessfully() {
            when(player.getName()).thenReturn("Bob");
            when(player.getUniqueId()).thenReturn(playerUUID);
            when(scoreboard.getEntryTeam("Bob")).thenReturn(team);
            when(team.getName()).thenReturn("Alpha");
            when(dbManager.getRoleName(playerUUID.toString())).thenReturn(null);
            teamManager.leaveTeam(player);
            verify(team).removeEntry("Bob");
        }

        @Test
        void leave_shouldNotifyNametag_whenPresent() {
            teamManager.setNametagManager(nametagManager);
            when(player.getName()).thenReturn("Bob");
            when(player.getUniqueId()).thenReturn(playerUUID);
            when(scoreboard.getEntryTeam("Bob")).thenReturn(team);
            when(team.getName()).thenReturn("Alpha");
            when(dbManager.getRoleName(playerUUID.toString())).thenReturn("member");
            teamManager.leaveTeam(player);
            verify(nametagManager).onPlayerRemovedFromTeam("Alpha", "Bob");
        }

        @Test
        void leave_shouldNotifyDiscord_whenPresent() {
            teamManager.setDiscordSyncManager(discordSyncManager);
            when(player.getName()).thenReturn("Bob");
            when(player.getUniqueId()).thenReturn(playerUUID);
            when(scoreboard.getEntryTeam("Bob")).thenReturn(team);
            when(team.getName()).thenReturn("Alpha");
            when(dbManager.getRoleName(playerUUID.toString())).thenReturn("member");
            teamManager.leaveTeam(player);
            verify(discordSyncManager).onPlayerLeftTeam(playerUUID, "Alpha");
        }

        @Test
        void leave_shouldNotCallIntegrations_whenNotSet() {
            when(player.getName()).thenReturn("Bob");
            when(player.getUniqueId()).thenReturn(playerUUID);
            when(scoreboard.getEntryTeam("Bob")).thenReturn(team);
            when(team.getName()).thenReturn("Alpha");
            when(dbManager.getRoleName(playerUUID.toString())).thenReturn("member");
            teamManager.leaveTeam(player);
            verify(team).removeEntry("Bob");
        }
    }

    @Nested
    class AddPlayerTests {
        @Test
        void notInTeam_shouldReject() {
            when(player.getName()).thenReturn("Alice");
            when(scoreboard.getEntryTeam("Alice")).thenReturn(null);
            teamManager.addPlayerToTeam(player, "Bob");
            verify(player).sendMessage("You are not in a team.");
        }

        @Test
        void valid_shouldAddEntry() {
            when(player.getName()).thenReturn("Alice");
            when(scoreboard.getEntryTeam("Alice")).thenReturn(team);
            when(team.getName()).thenReturn("Alpha");
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayerExact("Bob")).thenReturn(target);
                when(target.getUniqueId()).thenReturn(targetUUID);
                teamManager.addPlayerToTeam(player, "Bob");
                verify(team).addEntry("Bob");
            }
        }

        @Test
        void valid_shouldNotifyNametag() {
            teamManager.setNametagManager(nametagManager);
            when(player.getName()).thenReturn("Alice");
            when(scoreboard.getEntryTeam("Alice")).thenReturn(team);
            when(team.getName()).thenReturn("Alpha");
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayerExact("Bob")).thenReturn(target);
                when(target.getUniqueId()).thenReturn(targetUUID);
                teamManager.addPlayerToTeam(player, "Bob");
                verify(nametagManager).onPlayerAddedToTeam("Alpha", "Bob");
            }
        }

        @Test
        void valid_shouldNotifyDiscord() {
            teamManager.setDiscordSyncManager(discordSyncManager);
            when(player.getName()).thenReturn("Alice");
            when(scoreboard.getEntryTeam("Alice")).thenReturn(team);
            when(team.getName()).thenReturn("Alpha");
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayerExact("Bob")).thenReturn(target);
                when(target.getUniqueId()).thenReturn(targetUUID);
                teamManager.addPlayerToTeam(player, "Bob");
                verify(discordSyncManager).onPlayerJoinedTeam(targetUUID, "Alpha");
            }
        }

        @Test
        void targetOffline_shouldSkipDiscordSync() {
            teamManager.setDiscordSyncManager(discordSyncManager);
            when(player.getName()).thenReturn("Alice");
            when(player.getUniqueId()).thenReturn(playerUUID);
            when(scoreboard.getEntryTeam("Alice")).thenReturn(team);
            when(team.getName()).thenReturn("Alpha");
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayerExact("Bob")).thenReturn(null);
                teamManager.addPlayerToTeam(player, "Bob");
                verify(discordSyncManager, never()).onPlayerJoinedTeam(any(), anyString());
            }
        }
    }

    @Nested
    class RemovePlayerTests {
        @Test
        void invokerNotInTeam_shouldReject() {
            when(player.getName()).thenReturn("Alice");
            when(scoreboard.getEntryTeam("Alice")).thenReturn(null);
            teamManager.removePlayerFromTeam(player, "Bob");
            verify(player).sendMessage("You are not in a team.");
        }

        @Test
        void targetOffline_shouldReject() {
            when(player.getName()).thenReturn("Alice");
            when(scoreboard.getEntryTeam("Alice")).thenReturn(team);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayerExact("Bob")).thenReturn(null);
                teamManager.removePlayerFromTeam(player, "Bob");
                verify(player).sendMessage("That player is not online.");
            }
        }

        @Test
        void removeSelf_shouldReject() {
            when(player.getName()).thenReturn("Alice");
            when(player.getUniqueId()).thenReturn(playerUUID);
            when(scoreboard.getEntryTeam("Alice")).thenReturn(team);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayerExact("Alice")).thenReturn(player);
                teamManager.removePlayerFromTeam(player, "Alice");
                verify(player).sendMessage("Use a leave command to remove yourself.");
            }
        }

        @Test
        void targetNotInSameTeam_shouldReject() {
            Team otherTeam = mock(Team.class);
            when(player.getName()).thenReturn("Alice");
            when(player.getUniqueId()).thenReturn(playerUUID);
            when(target.getName()).thenReturn("Bob");
            when(target.getUniqueId()).thenReturn(targetUUID);
            when(scoreboard.getEntryTeam("Alice")).thenReturn(team);
            when(team.getName()).thenReturn("Alpha");
            when(scoreboard.getEntryTeam("Bob")).thenReturn(otherTeam);
            when(otherTeam.getName()).thenReturn("Bravo");
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayerExact("Bob")).thenReturn(target);
                teamManager.removePlayerFromTeam(player, "Bob");
                verify(player).sendMessage("Bob is not in your team.");
            }
        }

        @Test
        void validRemoval_shouldRemoveAndNotify() {
            when(player.getName()).thenReturn("Alice");
            when(player.getUniqueId()).thenReturn(playerUUID);
            when(target.getName()).thenReturn("Bob");
            when(target.getUniqueId()).thenReturn(targetUUID);
            when(scoreboard.getEntryTeam("Alice")).thenReturn(team);
            when(scoreboard.getEntryTeam("Bob")).thenReturn(team);
            when(team.getName()).thenReturn("Alpha");
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayerExact("Bob")).thenReturn(target);
                teamManager.removePlayerFromTeam(player, "Bob");
                verify(team).removeEntry("Bob");
                verify(player).sendMessage("Removed Bob from team Alpha.");
                verify(target).sendMessage("You were removed from team Alpha.");
            }
        }

        @Test
        void validRemoval_shouldNotifyIntegrations() {
            teamManager.setNametagManager(nametagManager);
            teamManager.setDiscordSyncManager(discordSyncManager);
            when(player.getName()).thenReturn("Alice");
            when(player.getUniqueId()).thenReturn(playerUUID);
            when(target.getName()).thenReturn("Bob");
            when(target.getUniqueId()).thenReturn(targetUUID);
            when(scoreboard.getEntryTeam("Alice")).thenReturn(team);
            when(scoreboard.getEntryTeam("Bob")).thenReturn(team);
            when(team.getName()).thenReturn("Alpha");
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayerExact("Bob")).thenReturn(target);
                teamManager.removePlayerFromTeam(player, "Bob");
                verify(nametagManager).onPlayerRemovedFromTeam("Alpha", "Bob");
                verify(discordSyncManager).onPlayerLeftTeam(targetUUID, "Alpha");
            }
        }
    }

    @Nested
    class PreloadPlayerTests {
        @Test
        void noTeamInDb_shouldDoNothing() {
            when(dbManager.getTeamNameByUUID(playerUUID.toString())).thenReturn(null);
            teamManager.preloadPlayer(playerUUID, "Alice");
            verify(scoreboard, never()).getTeam(anyString());
            verify(scoreboard, never()).registerNewTeam(anyString());
        }

        @Test
        void teamExistsOnScoreboard_shouldAddEntry() {
            when(dbManager.getTeamNameByUUID(playerUUID.toString())).thenReturn("Alpha");
            when(scoreboard.getTeam("Alpha")).thenReturn(team);
            teamManager.preloadPlayer(playerUUID, "Alice");
            verify(team).addEntry("Alice");
            verify(scoreboard, never()).registerNewTeam(anyString());
        }

        @Test
        void teamNotOnScoreboard_shouldRegisterAndAddEntry() {
            when(dbManager.getTeamNameByUUID(playerUUID.toString())).thenReturn("Alpha");
            when(scoreboard.getTeam("Alpha")).thenReturn(null);
            when(scoreboard.registerNewTeam("Alpha")).thenReturn(team);
            teamManager.preloadPlayer(playerUUID, "Alice");
            verify(scoreboard).registerNewTeam("Alpha");
            verify(team).addEntry("Alice");
        }
    }

    @Nested
    class SetTeamColorTests {
        @Test
        void notInTeam_shouldReject() {
            when(player.getName()).thenReturn("Alice");
            when(scoreboard.getEntryTeam("Alice")).thenReturn(null);
            teamManager.setTeamColor(player, "red");
            verify(player).sendMessage("You are not in a team.");
        }

        @Test
        void notLeader_shouldReject() {
            when(player.getName()).thenReturn("Alice");
            when(player.getUniqueId()).thenReturn(playerUUID);
            when(scoreboard.getEntryTeam("Alice")).thenReturn(team);
            when(dbManager.getRoleName(playerUUID.toString())).thenReturn("member");
            teamManager.setTeamColor(player, "red");
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertTrue(captor.getValue().contains("Only the team leader"));
        }

        @Test
        void invalidColor_shouldShowOptions() {
            when(player.getName()).thenReturn("Alice");
            when(player.getUniqueId()).thenReturn(playerUUID);
            when(scoreboard.getEntryTeam("Alice")).thenReturn(team);
            when(dbManager.getRoleName(playerUUID.toString())).thenReturn("leader");
            teamManager.setTeamColor(player, "rainbow");
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertTrue(captor.getValue().contains("Invalid color"));
            assertTrue(captor.getValue().contains("red"));
        }

        @Test
        void validColor_shouldApply() {
            teamManager.setNametagManager(nametagManager);
            when(player.getName()).thenReturn("Alice");
            when(player.getUniqueId()).thenReturn(playerUUID);
            when(scoreboard.getEntryTeam("Alice")).thenReturn(team);
            when(team.getName()).thenReturn("Alpha");
            when(dbManager.getRoleName(playerUUID.toString())).thenReturn("leader");
            teamManager.setTeamColor(player, "red");
            verify(nametagManager).assignTeamColor(eq("Alpha"), any());
        }

        @Test
        void validColor_caseInsensitive() {
            teamManager.setNametagManager(nametagManager);
            when(player.getName()).thenReturn("Alice");
            when(player.getUniqueId()).thenReturn(playerUUID);
            when(scoreboard.getEntryTeam("Alice")).thenReturn(team);
            when(team.getName()).thenReturn("Alpha");
            when(dbManager.getRoleName(playerUUID.toString())).thenReturn("leader");
            teamManager.setTeamColor(player, "RED");
            verify(nametagManager).assignTeamColor(eq("Alpha"), any());
        }
    }
}