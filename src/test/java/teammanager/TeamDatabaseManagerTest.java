package teammanager;

import main.io.github.itshaithamn.teamsandmore.teammanager.TeamDatabaseManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamDatabaseManagerTest {

    @TempDir
    File tempDir;

    private TeamDatabaseManager db;

    private final String uuid1 = UUID.randomUUID().toString();
    private final String uuid2 = UUID.randomUUID().toString();
    private final Timestamp now = new Timestamp(System.currentTimeMillis());

    @BeforeEach
    void setUp() {
        db = new TeamDatabaseManager(tempDir);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    // ── createTeam / getAllTeams ──

    @Test
    void createTeam_shouldAppearInGetAllTeams() {
        db.createTeam("Alpha", "world;0;64;0");

        List<String> teams = db.getAllTeams();
        assertEquals(1, teams.size());
        assertEquals("Alpha", teams.get(0));
    }

    @Test
    void createTeam_duplicate_shouldIgnore() {
        db.createTeam("Alpha", "world;0;64;0");
        db.createTeam("Alpha", "world;100;64;100");

        List<String> teams = db.getAllTeams();
        assertEquals(1, teams.size());
    }

    @Test
    void createMultipleTeams_shouldReturnAll() {
        db.createTeam("Alpha", "world;0;64;0");
        db.createTeam("Bravo", "world;10;64;10");
        db.createTeam("Charlie", "world;20;64;20");

        List<String> teams = db.getAllTeams();
        assertEquals(3, teams.size());
        assertTrue(teams.contains("Alpha"));
        assertTrue(teams.contains("Bravo"));
        assertTrue(teams.contains("Charlie"));
    }

    // ── addToTeam / getRoleName / getRolePriority / getTeamNameByUUID ──

    @Test
    void addToTeam_shouldBeRetrievable() {
        db.createTeam("Alpha", "world;0;64;0");
        db.addToTeam(uuid1, "Alpha", "leader", 0, now);

        assertEquals("leader", db.getRoleName(uuid1));
        assertEquals(0, db.getRolePriority(uuid1));
        assertEquals("Alpha", db.getTeamNameByUUID(uuid1));
    }

    @Test
    void addToTeam_duplicateUUID_shouldIgnore() {
        db.createTeam("Alpha", "world;0;64;0");
        db.addToTeam(uuid1, "Alpha", "leader", 0, now);
        db.addToTeam(uuid1, "Alpha", "member", 1, now); // duplicate — INSERT OR IGNORE

        // Original role should persist
        assertEquals("leader", db.getRoleName(uuid1));
    }

    @Test
    void getRoleName_unknownUUID_shouldReturnNull() {
        assertNull(db.getRoleName("nonexistent-uuid"));
    }

    @Test
    void getTeamNameByUUID_unknownUUID_shouldReturnNull() {
        assertNull(db.getTeamNameByUUID("nonexistent-uuid"));
    }

    @Test
    void getRolePriority_unknownUUID_shouldReturnNegativeOne() {
        assertEquals(-1, db.getRolePriority("nonexistent-uuid"));
    }

    // ── removeFromTeam ──

    @Test
    void removeFromTeam_shouldDeletePlayer() {
        db.createTeam("Alpha", "world;0;64;0");
        db.addToTeam(uuid1, "Alpha", "member", 1, now);

        db.removeFromTeam(uuid1);

        assertNull(db.getRoleName(uuid1));
        assertNull(db.getTeamNameByUUID(uuid1));
        assertEquals(-1, db.getRolePriority(uuid1));
    }

    @Test
    void removeFromTeam_nonexistentUUID_shouldNotFail() {
        assertDoesNotThrow(() -> db.removeFromTeam("ghost-uuid"));
    }

    @Test
    void removeFromTeam_shouldNotAffectOtherPlayers() {
        db.createTeam("Alpha", "world;0;64;0");
        db.addToTeam(uuid1, "Alpha", "leader", 0, now);
        db.addToTeam(uuid2, "Alpha", "member", 1, now);

        db.removeFromTeam(uuid1);

        assertNull(db.getRoleName(uuid1));
        assertEquals("member", db.getRoleName(uuid2));
    }

    // ── getTeamOfPlayer ──

    @Test
    void getTeamOfPlayer_shouldReturnTeamName(@Mock Player mockPlayer) {
        UUID pUUID = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(pUUID);
        db.createTeam("Alpha", "world;0;64;0");
        db.addToTeam(pUUID.toString(), "Alpha", "member", 1, now);

        String result = db.getTeamOfPlayer(mockPlayer);
        assertEquals("Alpha", result);
    }

    @Test
    void getTeamOfPlayer_notInTeam_shouldReturnNull(@Mock Player mockPlayer) {
        UUID pUUID = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(pUUID);

        assertNull(db.getTeamOfPlayer(mockPlayer));
    }

    // ── close ──

    @Test
    void close_shouldBeIdempotent() {
        assertDoesNotThrow(() -> {
            db.close();
            db.close();
        });
    }
}
