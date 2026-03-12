import main.io.github.itshaithamn.teamsandmore.commands.Commands;
import main.io.github.itshaithamn.teamsandmore.teammanager.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandsTest {

    @Mock private TeamManager teamManager;
    @Mock private Command command;
    @Mock private Player player;
    @Mock private Player target;
    @Mock private CommandSender consoleSender;

    private Commands commands;

    @BeforeEach
    void setUp() {
        commands = new Commands(teamManager);
    }

    @Test
    void nonPlayerSender_shouldBeRejected() {
        boolean result = commands.onCommand(consoleSender, command, "team", new String[]{"create", "Alpha"});

        assertTrue(result);
        assertComponentMessage(consoleSender, "Only players can use this command");
        verifyNoInteractions(teamManager);
    }

    @Test
    void noArgs_shouldShowUsage() {
        boolean result = commands.onCommand(player, command, "team", new String[]{});

        assertTrue(result);
        assertComponentMessage(player, "§6§lUsage: /team <create|test>");
        verifyNoInteractions(teamManager);
    }

    @Test
    void unknownSubcommand_shouldSendDne() {
        boolean result = commands.onCommand(player, command, "team", new String[]{"wat"});

        assertTrue(result);
        assertComponentMessage(player, "§c§lDNE");
        verifyNoInteractions(teamManager);
    }

    @Test
    void create_missingName_shouldShowUsage() {
        boolean result = commands.onCommand(player, command, "team", new String[]{"create"});

        assertTrue(result);
        assertComponentMessage(player, "§cUsage: /team create <name>");
        verifyNoInteractions(teamManager);
    }

    @Test
    void create_nameTooLong_shouldReject() {
        boolean result = commands.onCommand(player, command, "team",
                new String[]{"create", "abcdefghijklmnopq"}); // 17 chars

        assertTrue(result);
        assertComponentMessage(player, "§cTeam name must be 16 characters or less.");
        verifyNoInteractions(teamManager);
    }

    @Test
    void create_invalidName_shouldReject() {
        boolean result = commands.onCommand(player, command, "team",
                new String[]{"create", "bad-name!"});

        assertTrue(result);
        assertComponentMessage(player, "§cTeam name can only contain letters, numbers, and underscores.");
        verifyNoInteractions(teamManager);
    }

    @Test
    void create_valid_shouldCallTeamManager() {
        boolean result = commands.onCommand(player, command, "team",
                new String[]{"create", "Alpha_1"});

        assertTrue(result);
        verify(teamManager, times(1)).createNewTeam(player, "Alpha_1");
    }

    @Test
    void invite_missingTarget_shouldShowUsage() {
        boolean result = commands.onCommand(player, command, "team", new String[]{"invite"});

        assertTrue(result);
        assertComponentMessage(player, "§cUsage: /team invite <player name>");
        verifyNoInteractions(teamManager);
    }

    @Test
    void invite_noPermission_shouldReject() {
        when(player.hasPermission("teamsandmore.invite")).thenReturn(false);
        when(player.hasPermission("teamsandmore.admin")).thenReturn(false);

        boolean result = commands.onCommand(player, command, "team", new String[]{"invite", "Bob"});

        assertTrue(result);
        assertComponentMessage(player, "§cYou don't have permission to invite players.");
        verifyNoInteractions(teamManager);
    }

    @Test
    void invite_selfInvite_shouldReject() {
        when(player.hasPermission("teamsandmore.invite")).thenReturn(true);
        when(player.hasPermission("teamsandmore.admin")).thenReturn(false);
        when(player.getName()).thenReturn("Alice");

        boolean result = commands.onCommand(player, command, "team", new String[]{"invite", "Alice"});

        assertTrue(result);
        assertComponentMessage(player, "§cYou can't invite yourself.");
        verifyNoInteractions(teamManager);
    }

    @Test
    void invite_targetOffline_shouldReject() {
        when(player.hasPermission("teamsandmore.invite")).thenReturn(true);
        when(player.hasPermission("teamsandmore.admin")).thenReturn(false);
        when(player.getName()).thenReturn("Alice");

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Bob")).thenReturn(null);

            boolean result = commands.onCommand(player, command, "team", new String[]{"invite", "Bob"});

            assertTrue(result);
            assertComponentMessage(player, "§cPlayer not found or offline.");
            verifyNoInteractions(teamManager);
        }
    }

    @Test
    void invite_valid_shouldCallTeamManager() {
        when(player.hasPermission("teamsandmore.invite")).thenReturn(true);
        when(player.hasPermission("teamsandmore.admin")).thenReturn(false);
        when(player.getName()).thenReturn("Alice");
        when(target.getName()).thenReturn("Bob");

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Bob")).thenReturn(target);

            boolean result = commands.onCommand(player, command, "team", new String[]{"invite", "Bob"});

            assertTrue(result);
            verify(teamManager, times(1)).addPlayerToTeam(player, "Bob");
        }
    }

    private void assertComponentMessage(CommandSender sender, String expected) {
        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(sender, atLeastOnce()).sendMessage(captor.capture());

        String actual = PlainTextComponentSerializer.plainText().serialize(captor.getValue());
        assertEquals(expected, actual);
    }
}