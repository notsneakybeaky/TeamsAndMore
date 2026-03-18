package teammanager;

import main.io.github.itshaithamn.teamsandmore.teammanager.Caching;
import main.io.github.itshaithamn.teamsandmore.teammanager.DBRecords;
import main.io.github.itshaithamn.teamsandmore.teammanager.TeamDatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachingTest {

    @Mock private TeamDatabaseManager dbManager;

    private Caching caching;

    @BeforeEach
    void setUp() {
        caching = new Caching(dbManager);
    }

    @AfterEach
    void tearDown() {
        caching.stop();
    }

    @Test
    void getDbManager_returnsInjectedInstance() {
        assertSame(dbManager, caching.getDbManager());
    }

    @Test
    void cache_belowThreshold_shouldNotFlush() throws InterruptedException {
        DBRecords record = mock(DBRecords.class);

        // Add 8 records — threshold is 9
        for (int i = 0; i < 8; i++) {
            caching.cache(record);
        }

        // Give executor time to run if it was triggered
        Thread.sleep(100);

        // Records should NOT have been applied yet
        verify(record, never()).apply(dbManager);
    }

    @Test
    void cache_atThreshold_shouldAutoFlush() throws InterruptedException {
        DBRecords record = mock(DBRecords.class);

        // Add exactly 9 records — should trigger auto-flush
        for (int i = 0; i < 9; i++) {
            caching.cache(record);
        }

        // Wait for the async flush to run
        Thread.sleep(300);

        // All 9 should have been applied
        verify(record, times(9)).apply(dbManager);
    }

    @Test
    void flushNow_shouldFlushAllBuffered() throws InterruptedException {
        DBRecords record = mock(DBRecords.class);

        for (int i = 0; i < 3; i++) {
            caching.cache(record);
        }

        caching.flushNow();

        Thread.sleep(300);

        verify(record, times(3)).apply(dbManager);
    }

    @Test
    void flushNow_withEmptyBuffer_shouldNotFail() {
        assertDoesNotThrow(() -> caching.flushNow());
    }

    @Test
    void multipleFlushNow_shouldApplyAll() throws InterruptedException {
        DBRecords r1 = mock(DBRecords.class);
        DBRecords r2 = mock(DBRecords.class);

        caching.cache(r1);
        caching.flushNow();

        caching.cache(r2);
        caching.flushNow();

        Thread.sleep(300);

        verify(r1, times(1)).apply(dbManager);
        verify(r2, times(1)).apply(dbManager);
    }
}
