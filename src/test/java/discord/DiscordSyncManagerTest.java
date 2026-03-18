package discord;

import main.io.github.itshaithamn.teamsandmore.discord.DiscordSyncManager;
import main.io.github.itshaithamn.teamsandmore.teammanager.TeamDatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscordSyncManagerTest {

    @Mock private JavaPlugin plugin;
    @Mock private TeamDatabaseManager dbManager;
    @Mock private Scoreboard scoreboard;
    @Mock private PluginManager pluginManager;

    private DiscordSyncManager syncManager;

    @BeforeEach
    void setUp() {
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));
        syncManager = new DiscordSyncManager(plugin, scoreboard);
    }

    @Test
    void isAvailable_returnsFalse_whenDiscordSRVNotEnabled() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            when(pluginManager.isPluginEnabled("DiscordSRV")).thenReturn(false);

            assertFalse(syncManager.isAvailable());
        }
    }

    @Test
    void isAvailable_returnsTrue_whenDiscordSRVEnabled() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            when(pluginManager.isPluginEnabled("DiscordSRV")).thenReturn(true);

            assertTrue(syncManager.isAvailable());
        }
    }

    @Test
    void onPlayerJoinedTeam_whenNotAvailable_shouldDoNothing() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            when(pluginManager.isPluginEnabled("DiscordSRV")).thenReturn(false);

            // Should not throw, should not schedule anything
            assertDoesNotThrow(() ->
                    syncManager.onPlayerJoinedTeam(java.util.UUID.randomUUID(), "Alpha"));
        }
    }

    @Test
    void onPlayerLeftTeam_whenNotAvailable_shouldDoNothing() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            when(pluginManager.isPluginEnabled("DiscordSRV")).thenReturn(false);

            assertDoesNotThrow(() ->
                    syncManager.onPlayerLeftTeam(java.util.UUID.randomUUID(), "Alpha"));
        }
    }

    @Test
    void fullResync_whenNotAvailable_shouldDoNothing() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            when(pluginManager.isPluginEnabled("DiscordSRV")).thenReturn(false);

            assertDoesNotThrow(() -> syncManager.fullResync());
        }
    }
}
