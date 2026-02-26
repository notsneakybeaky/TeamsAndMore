    import be.seeseemelk.mockbukkit.MockBukkit;
    import be.seeseemelk.mockbukkit.ServerMock;
    import main.io.github.itshaithamn.teamsandmore.Main;
    import main.io.github.itshaithamn.teamsandmore.teammanager.Caching;
    import org.junit.jupiter.api.AfterEach;
    import org.junit.jupiter.api.BeforeEach;
    import org.junit.jupiter.api.Test;

    import java.sql.Timestamp;

    public class databasetest {

        private ServerMock server;
        private Main plugin;

        @BeforeEach
        public void setUp() {
            server = MockBukkit.mock();
            plugin = MockBukkit.load(Main.class);
        }

        @AfterEach
        public void tearDown() {
            MockBukkit.unmock();
        }

        @Test
        public void testCacheFlushMemoryPerformance() {
            int iterations = 1_000;
            Runtime runtime = Runtime.getRuntime();

            runtime.gc();
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

            long cacheStart = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                String uuid = "test-uuid-" + i;
                Main.getCaching().cache(
                        Caching.DBAction.ADDTOTEAM,
                        uuid,
                        "TestTeam",
                        "Tester",
                        1,
                        new Timestamp(System.currentTimeMillis())
                );
            }
            long cacheEnd = System.nanoTime();
            long cacheMs = (cacheEnd - cacheStart) / 1_000_000;

            long memoryAfterCache = runtime.totalMemory() - runtime.freeMemory();

            long flushStart = System.nanoTime();
            Main.getCaching().startFlush();
            long flushEnd = System.nanoTime();
            long flushMs = (flushEnd - flushStart) / 1_000_000;

            long memoryAfterFlush = runtime.totalMemory() - runtime.freeMemory();

            System.out.println(iterations + " cache insertions took: " + cacheMs + " ms");
            System.out.println(iterations + " flush operations took: " + flushMs + " ms");
            System.out.println("TOTAL: " + (cacheMs + flushMs) + " ms");

            System.out.println("Memory before: " + memoryBefore / 1024 + " KB");
            System.out.println("Memory after cache: " + memoryAfterCache / 1024 + " KB");
            System.out.println("Memory after flush: " + memoryAfterFlush / 1024 + " KB");
            System.out.println("Memory used by cache: " + (memoryAfterCache - memoryBefore) / 1024 + " KB");
            System.out.println("Memory released after flush: " + (memoryAfterCache - memoryAfterFlush) / 1024 + " KB");
        }
    }