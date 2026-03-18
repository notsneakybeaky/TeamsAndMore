package nametag;

import main.io.github.itshaithamn.teamsandmore.nametag.NametagColor;
import main.io.github.itshaithamn.teamsandmore.nametag.NametagManager;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NametagManagerTest {

    @Mock private Scoreboard scoreboard;
    @Mock private Team team;

    private NametagManager nametagManager;

    @BeforeEach
    void setUp() {
        nametagManager = new NametagManager(scoreboard, Logger.getLogger("test"));
    }

    @Test
    void assignTeamColor_shouldApplyToTeam() {
        when(scoreboard.getTeam("Alpha")).thenReturn(team);
        nametagManager.assignTeamColor("Alpha", NametagColor.RED);
        verify(team).color(any());
        verify(team).prefix(any());
    }

    @Test
    void assignTeamColor_teamNotFound_shouldNotThrow() {
        when(scoreboard.getTeam("Ghost")).thenReturn(null);
        assertDoesNotThrow(() -> nametagManager.assignTeamColor("Ghost", NametagColor.RED));
    }

    @Test
    void getOrAssignColor_shouldBeConsistent() {
        when(scoreboard.getTeam("Alpha")).thenReturn(team);
        NametagColor first = nametagManager.getOrAssignColor("Alpha");
        NametagColor second = nametagManager.getOrAssignColor("Alpha");
        assertSame(first, second);
    }

    @Test
    void getOrAssignColor_shouldApplyOnFirstCall() {
        when(scoreboard.getTeam("Alpha")).thenReturn(team);
        nametagManager.getOrAssignColor("Alpha");
        verify(team).color(any());
        verify(team).prefix(any());
    }

    @Test
    void assignTeamColor_shouldOverrideAutoAssigned() {
        when(scoreboard.getTeam("Alpha")).thenReturn(team);
        nametagManager.getOrAssignColor("Alpha");
        nametagManager.assignTeamColor("Alpha", NametagColor.DARK_PURPLE);
        assertEquals(NametagColor.DARK_PURPLE, nametagManager.getOrAssignColor("Alpha"));
    }

    @Test
    void refreshAllNametags_shouldApplyToAllTeams() {
        Set<Team> teams = new HashSet<>();
        Team team2 = mock(Team.class);
        teams.add(team);
        teams.add(team2);
        when(scoreboard.getTeams()).thenReturn(teams);
        when(team.getName()).thenReturn("Alpha");
        when(team2.getName()).thenReturn("Bravo");
        when(scoreboard.getTeam("Alpha")).thenReturn(team);
        when(scoreboard.getTeam("Bravo")).thenReturn(team2);

        nametagManager.refreshAllNametags();

        verify(team).color(any());
        verify(team).prefix(any());
        verify(team2).color(any());
        verify(team2).prefix(any());
    }

    @Test
    void onPlayerAddedToTeam_shouldEnsureColorAssigned() {
        when(scoreboard.getTeam("Alpha")).thenReturn(team);
        nametagManager.onPlayerAddedToTeam("Alpha", "Bob");
        verify(team).color(any());
    }

    @Test
    void onPlayerRemovedFromTeam_shouldBeNoOp() {
        nametagManager.onPlayerRemovedFromTeam("Alpha", "Bob");
        verifyNoInteractions(scoreboard);
    }
}