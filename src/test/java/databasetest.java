import main.io.github.itshaithamn.teamsandmore.commands.Commands;
import main.io.github.itshaithamn.teamsandmore.teammanager.Caching;
import main.io.github.itshaithamn.teamsandmore.teammanager.TeamDatabaseManager;
import main.io.github.itshaithamn.teamsandmore.teammanager.TeamManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamsAndMoreTest {

    @Mock Scoreboard scoreboard;
    @Mock TeamDatabaseManager dbManager;
    @Mock Command command;
    @Mock World world;

    @Mock Player leader;
    @Mock Player member1;
    @Mock Player member2;
    @Mock Player member3;
    @Mock Player member4;
    @Mock Player member5; // out of range

    private Location leaderLoc;

    // ═════════════════════════════════════════════════════════════
    //  COMMANDS TESTS — TeamManager is mocked, pure unit tests
    // ═════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Commands — routing and input validation")
    class CommandsTests {

        private TeamManager mockTeamManager;
        private Commands commands;

        @BeforeEach
        void init() {
            mockTeamManager = mock(TeamManager.class);
            commands = new Commands(mockTeamManager);
        }

        @Test
        @DisplayName("Non-player sender is rejected")
        void nonPlayerSenderRejected() {
            CommandSender consoleSender = mock(CommandSender.class);
            boolean result = commands.onCommand(consoleSender, command, "team", new String[]{});

            verify(consoleSender).sendMessage(Component.text("Only players can use this command"));
            assertTrue(result);
            verifyNoInteractions(mockTeamManager);
        }

        @Test
        @DisplayName("No args shows usage message")
        void noArgsShowsUsage() {
            boolean result = commands.onCommand(leader, command, "team", new String[]{});

            verify(leader).sendMessage(argThat((Component c) ->
                    c.toString().contains("Usage")));
            assertTrue(result);
            verifyNoInteractions(mockTeamManager);
        }

        @Test
        @DisplayName("Unknown subcommand returns DNE")
        void unknownSubcommand() {
            boolean result = commands.onCommand(leader, command, "team",
                    new String[]{"foobar"});

            verify(leader).sendMessage(Component.text("§c§lDNE"));
            assertTrue(result);
            verifyNoInteractions(mockTeamManager);
        }

        @Test
        @DisplayName("'test' subcommand sends confirmation")
        void testSubcommand() {
            boolean result = commands.onCommand(leader, command, "team",
                    new String[]{"test"});

            verify(leader).sendMessage(Component.text("Test command executed!"));
            assertTrue(result);
            verifyNoInteractions(mockTeamManager);
        }

        @Test
        @DisplayName("'create' without name shows usage")
        void createMissingName() {
            boolean result = commands.onCommand(leader, command, "team",
                    new String[]{"create"});

            verify(leader).sendMessage(argThat((Component c) ->
                    c.toString().contains("Usage") && c.toString().contains("create")));
            assertTrue(result);
            verifyNoInteractions(mockTeamManager);
        }

        @Test
        @DisplayName("'create' with name >16 characters is rejected")
        void createNameTooLong() {
            boolean result = commands.onCommand(leader, command, "team",
                    new String[]{"create", "ThisIsWayTooLongForScoreboard"});

            verify(leader).sendMessage(argThat((Component c) ->
                    c.toString().contains("16 characters")));
            assertTrue(result);
            verifyNoInteractions(mockTeamManager);
        }

        @Test
        @DisplayName("'create' with exactly 16 characters passes validation")
        void createNameExactly16() {
            commands.onCommand(leader, command, "team",
                    new String[]{"create", "Exactly16Chars__"});

            // Should reach TeamManager — name is exactly 16
            verify(mockTeamManager).createNewTeam(leader, "Exactly16Chars__");
        }

        @Test
        @DisplayName("'create' with special characters is rejected")
        void createNameSpecialChars() {
            boolean result = commands.onCommand(leader, command, "team",
                    new String[]{"create", "bad-name!"});

            verify(leader).sendMessage(argThat((Component c) ->
                    c.toString().contains("letters, numbers")));
            assertTrue(result);
            verifyNoInteractions(mockTeamManager);
        }

        @Test
        @DisplayName("'create' with spaces in name only uses first arg")
        void createNameWithSpaces() {
            // /team create My Team → args = ["create", "My", "Team"]
            // Only "My" is passed as teamName
            commands.onCommand(leader, command, "team",
                    new String[]{"create", "My", "Team"});

            verify(mockTeamManager).createNewTeam(leader, "My");
        }

        @Test
        @DisplayName("'create' with valid alphanumeric name delegates to TeamManager")
        void createValidName() {
            commands.onCommand(leader, command, "team",
                    new String[]{"create", "AlphaSquad"});

            verify(mockTeamManager).createNewTeam(leader, "AlphaSquad");
        }

        @Test
        @DisplayName("'create' with underscores passes validation")
        void createNameWithUnderscores() {
            commands.onCommand(leader, command, "team",
                    new String[]{"create", "cool_team_1"});

            verify(mockTeamManager).createNewTeam(leader, "cool_team_1");
        }

        @Test
        @DisplayName("'CREATE' is case-insensitive routing")
        void createCaseInsensitive() {
            commands.onCommand(leader, command, "team",
                    new String[]{"CREATE", "BravoTeam"});

            verify(mockTeamManager).createNewTeam(leader, "BravoTeam");
        }

        @Test
        @DisplayName("Team name preserves original casing")
        void createPreservesCase() {
            commands.onCommand(leader, command, "team",
                    new String[]{"create", "MyTeam_XYZ"});

            // Verify exact casing is passed through, not lowercased
            verify(mockTeamManager).createNewTeam(leader, "MyTeam_XYZ");
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  TEAM MANAGER TESTS — requires mocking static Bukkit calls
    // ═════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("TeamManager — team creation logic")
    class TeamManagerTests {

        private TeamManager teamManager;

        @BeforeEach
        void init() {
            // dbManager.getAllTeams() is called in TeamManager constructor
            when(dbManager.getAllTeams()).thenReturn(Collections.emptyList());

            leaderLoc = new Location(world, 0, 64, 0);
            lenient().when(leader.getLocation()).thenReturn(leaderLoc);
            lenient().when(leader.getWorld()).thenReturn(world);
            lenient().when(leader.getName()).thenReturn("Leader");
            lenient().when(leader.getUniqueId()).thenReturn(UUID.randomUUID());

            setupMember(member1, "Member1", 5, 64, 0);
            setupMember(member2, "Member2", 10, 64, 0);
            setupMember(member3, "Member3", 15, 64, 0);
            setupMember(member4, "Member4", 20, 64, 0);
            setupMember(member5, "Member5", 100, 64, 0); // out of range
        }

        private void setupMember(Player member, String name, double x, double y, double z) {
            Location loc = new Location(world, x, y, z);
            lenient().when(member.getLocation()).thenReturn(loc);
            lenient().when(member.getWorld()).thenReturn(world);
            lenient().when(member.getName()).thenReturn(name);
            lenient().when(member.getUniqueId()).thenReturn(UUID.randomUUID());
        }

        @Test
        @DisplayName("Constructor loads existing teams from database into scoreboard")
        void constructorLoadsTeams() {
            when(dbManager.getAllTeams()).thenReturn(List.of("TeamA", "TeamB", "TeamC"));

            new TeamManager(scoreboard, dbManager);

            verify(scoreboard).registerNewTeam("TeamA");
            verify(scoreboard).registerNewTeam("TeamB");
            verify(scoreboard).registerNewTeam("TeamC");
        }

        @Test
        @DisplayName("Constructor handles empty database gracefully")
        void constructorEmptyDatabase() {
            when(dbManager.getAllTeams()).thenReturn(Collections.emptyList());

            new TeamManager(scoreboard, dbManager);

            verify(scoreboard, never()).registerNewTeam(anyString());
        }

        @Test
        @DisplayName("createNewTeam fails when <4 players nearby")
        void createTeamNotEnoughPlayers() {
            // Mock Bukkit.getOnlinePlayers() to return only 2 nearby players
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(Bukkit::getOnlinePlayers)
                        .thenReturn(List.of(leader, member1, member2));

                teamManager = new TeamManager(scoreboard, dbManager);
                teamManager.createNewTeam(leader, "SmallTeam");

                verify(leader).sendMessage("You need at least 4 players to create a team.");
                verify(scoreboard, never()).registerNewTeam("SmallTeam");
            }
        }

        @Test
        @DisplayName("createNewTeam fails when nearby players are in different world")
        void createTeamDifferentWorlds() {
            World nether = mock(World.class);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                // Put all members in a different world
                when(member1.getWorld()).thenReturn(nether);
                when(member2.getWorld()).thenReturn(nether);
                when(member3.getWorld()).thenReturn(nether);
                when(member4.getWorld()).thenReturn(nether);

                bukkit.when(Bukkit::getOnlinePlayers)
                        .thenReturn(List.of(leader, member1, member2, member3, member4));

                teamManager = new TeamManager(scoreboard, dbManager);
                teamManager.createNewTeam(leader, "LonelyTeam");

                verify(leader).sendMessage("You need at least 4 players to create a team.");
            }
        }

        @Test
        @DisplayName("createNewTeam succeeds with 4+ nearby players")
        void createTeamSuccess() {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(Bukkit::getOnlinePlayers)
                        .thenReturn(List.of(leader, member1, member2, member3, member4, member5));

                Team mockTeam = mock(Team.class);
                when(scoreboard.registerNewTeam("Warriors")).thenReturn(mockTeam);

                teamManager = new TeamManager(scoreboard, dbManager);
                teamManager.createNewTeam(leader, "Warriors");

                // Scoreboard team registered
                verify(scoreboard).registerNewTeam("Warriors");

                // Leader added
                verify(mockTeam).addEntry("Leader");

                // 4 nearest members added (member5 at 100 blocks is out of range)
                verify(mockTeam).addEntry("Member1");
                verify(mockTeam).addEntry("Member2");
                verify(mockTeam).addEntry("Member3");
                verify(mockTeam).addEntry("Member4");
                verify(mockTeam, never()).addEntry("Member5");

                // Success message
                verify(leader).sendMessage("Team 'Warriors' created with 5 members!");
            }
        }

        @Test
        @DisplayName("createNewTeam only grabs 4 members even if more are in range")
        void createTeamLimitsTo4() {
            // Put member5 within range too
            setupMember(member5, "Member5", 22, 64, 0); // 22 blocks, within 25

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(Bukkit::getOnlinePlayers)
                        .thenReturn(List.of(leader, member1, member2, member3, member4, member5));

                Team mockTeam = mock(Team.class);
                when(scoreboard.registerNewTeam("BigGroup")).thenReturn(mockTeam);

                teamManager = new TeamManager(scoreboard, dbManager);
                teamManager.createNewTeam(leader, "BigGroup");

                // Leader + exactly 4 members = 5 addEntry calls total
                verify(mockTeam, times(5)).addEntry(anyString());
                verify(leader).sendMessage("Team 'BigGroup' created with 5 members!");
            }
        }

        @Test
        @DisplayName("createNewTeam picks the 4 closest players by distance")
        void createTeamPicksClosest() {
            // Move member5 to 3 blocks — closer than everyone else
            setupMember(member5, "Member5", 3, 64, 0);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(Bukkit::getOnlinePlayers)
                        .thenReturn(List.of(leader, member1, member2, member3, member4, member5));

                Team mockTeam = mock(Team.class);
                when(scoreboard.registerNewTeam("EliteTeam")).thenReturn(mockTeam);

                teamManager = new TeamManager(scoreboard, dbManager);
                teamManager.createNewTeam(leader, "EliteTeam");

                // member5 (3), member1 (5), member2 (10), member3 (15) are the 4 closest
                // member4 (20) gets cut
                verify(mockTeam).addEntry("Member5");
                verify(mockTeam).addEntry("Member1");
                verify(mockTeam).addEntry("Member2");
                verify(mockTeam).addEntry("Member3");
                verify(mockTeam, never()).addEntry("Member4");
            }
        }

        @Test
        @DisplayName("createNewTeam excludes the leader from the nearby search")
        void createTeamExcludesLeader() {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(Bukkit::getOnlinePlayers)
                        .thenReturn(List.of(leader, member1, member2, member3, member4));

                Team mockTeam = mock(Team.class);
                when(scoreboard.registerNewTeam("NoSelfTeam")).thenReturn(mockTeam);

                teamManager = new TeamManager(scoreboard, dbManager);
                teamManager.createNewTeam(leader, "NoSelfTeam");

                // Leader is added via explicit addEntry, not via findClosestPlayers
                verify(mockTeam, times(1)).addEntry("Leader");
            }
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  CACHING TESTS
    // ═════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Caching — buffer threshold and flush routing")
    class CachingTests {

        private Caching caching;

        @BeforeEach
        void init() {
            caching = new Caching(dbManager);
        }

        @AfterEach
        void tearDown() {
            caching.stop();
        }

        @Test
        @DisplayName("Buffer does not flush before reaching threshold of 9")
        void noFlushBelowThreshold() {
            for (int i = 0; i < 8; i++) {
                caching.cache(Caching.DBAction.ADDTOTEAM,
                        UUID.randomUUID().toString(),
                        "TestTeam", "member", i,
                        new java.sql.Timestamp(System.currentTimeMillis()));
            }

            verifyNoInteractions(dbManager);
        }

        @Test
        @DisplayName("Buffer flushes at 9 entries and routes ADDTOTEAM to DB")
        void flushAtThreshold() throws InterruptedException {
            for (int i = 0; i < 9; i++) {
                caching.cache(Caching.DBAction.ADDTOTEAM,
                        UUID.randomUUID().toString(),
                        "TestTeam", "member", i,
                        new java.sql.Timestamp(System.currentTimeMillis()));
            }

            Thread.sleep(500);

            verify(dbManager, times(9)).addToTeam(
                    anyString(), eq("TestTeam"), eq("member"), anyInt(), any());
        }

        @Test
        @DisplayName("REMOVEFROMTEAM action routes to removeFromTeam")
        void removeFromTeamRoutes() throws InterruptedException {
            // Fill to threshold
            for (int i = 0; i < 8; i++) {
                caching.cache(Caching.DBAction.ADDTOTEAM,
                        UUID.randomUUID().toString(),
                        "TestTeam", "member", i,
                        new java.sql.Timestamp(System.currentTimeMillis()));
            }

            String targetUuid = UUID.randomUUID().toString();
            caching.cache(Caching.DBAction.REMOVEFROMTEAM,
                    targetUuid, "TestTeam", "", 0,
                    new java.sql.Timestamp(System.currentTimeMillis()));

            Thread.sleep(500);

            verify(dbManager, atLeastOnce()).removeFromTeam(anyString());
        }

        @Test
        @DisplayName("stop() flushes remaining entries below threshold synchronously")
        void stopFlushesRemainder() {
            for (int i = 0; i < 5; i++) {
                caching.cache(Caching.DBAction.ADDTOTEAM,
                        UUID.randomUUID().toString(),
                        "TestTeam", "member", i,
                        new java.sql.Timestamp(System.currentTimeMillis()));
            }

            verifyNoInteractions(dbManager);

            caching.stop();

            verify(dbManager, times(5)).addToTeam(
                    anyString(), eq("TestTeam"), eq("member"), anyInt(), any());
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  INTEGRATION — Command → TeamManager → Caching
    // ═════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Integration — /team create end-to-end")
    class IntegrationTests {

        @BeforeEach
        void init() {
            leaderLoc = new Location(world, 0, 64, 0);
            when(leader.getLocation()).thenReturn(leaderLoc);
            when(leader.getWorld()).thenReturn(world);
            when(leader.getName()).thenReturn("Leader");
            when(leader.getUniqueId()).thenReturn(UUID.randomUUID());

            setupMember(member1, "Member1", 5, 64, 0);
            setupMember(member2, "Member2", 10, 64, 0);
            setupMember(member3, "Member3", 15, 64, 0);
            setupMember(member4, "Member4", 20, 64, 0);
        }

        private void setupMember(Player member, String name,
                                 double x, double y, double z) {
            Location loc = new Location(world, x, y, z);
            when(member.getLocation()).thenReturn(loc);
            when(member.getWorld()).thenReturn(world);
            when(member.getName()).thenReturn(name);
            when(member.getUniqueId()).thenReturn(UUID.randomUUID());
        }

        @Test
        @DisplayName("/team create Warriors — full path from command to scoreboard")
        void fullCreateFlow() {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(Bukkit::getOnlinePlayers)
                        .thenReturn(List.of(leader, member1, member2, member3, member4));

                when(dbManager.getAllTeams()).thenReturn(Collections.emptyList());

                Team mockTeam = mock(Team.class);
                when(scoreboard.registerNewTeam("Warriors")).thenReturn(mockTeam);

                TeamManager realManager = new TeamManager(scoreboard, dbManager);
                Commands commands = new Commands(realManager);

                boolean result = commands.onCommand(leader, command, "team",
                        new String[]{"create", "Warriors"});

                assertTrue(result);

                // Scoreboard populated
                verify(scoreboard).registerNewTeam("Warriors");
                verify(mockTeam).addEntry("Leader");
                verify(mockTeam).addEntry("Member1");
                verify(mockTeam).addEntry("Member2");
                verify(mockTeam).addEntry("Member3");
                verify(mockTeam).addEntry("Member4");

                // Success message
                verify(leader).sendMessage(
                        "Team 'Warriors' created with 5 members!");

                // Cleanup
                realManager.caching.stop();
            }
        }

        @Test
        @DisplayName("/team create with invalid name never reaches TeamManager")
        void invalidNameBlockedByCommand() {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                when(dbManager.getAllTeams()).thenReturn(Collections.emptyList());

                TeamManager realManager = new TeamManager(scoreboard, dbManager);
                Commands commands = new Commands(realManager);

                commands.onCommand(leader, command, "team",
                        new String[]{"create", "no spaces!"});

                // registerNewTeam never called — command layer blocked it
                verify(scoreboard, never()).registerNewTeam("no spaces!");

                realManager.caching.stop();
            }
        }

        @Test
        @DisplayName("/team create fails gracefully with insufficient players")
        void createFailsInsufficientPlayers() {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                // Only 1 other player online
                bukkit.when(Bukkit::getOnlinePlayers)
                        .thenReturn(List.of(leader, member1));

                when(dbManager.getAllTeams()).thenReturn(Collections.emptyList());

                TeamManager realManager = new TeamManager(scoreboard, dbManager);
                Commands commands = new Commands(realManager);

                commands.onCommand(leader, command, "team",
                        new String[]{"create", "TinyTeam"});

                verify(leader).sendMessage(
                        "You need at least 4 players to create a team.");
                // No team registered
                verify(scoreboard, never()).registerNewTeam("TinyTeam");

                realManager.caching.stop();
            }
        }
    }
}