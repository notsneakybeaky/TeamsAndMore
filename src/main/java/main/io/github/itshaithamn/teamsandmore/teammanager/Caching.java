package main.io.github.itshaithamn.teamsandmore.teammanager;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Caching {

    private final TeamDatabaseManager dbManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Queue<DBRecords> bufferQueue = new ConcurrentLinkedQueue<>();
    private final Queue<DBRecords> flushQueue = new ConcurrentLinkedQueue<>();

    public Caching(TeamDatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void cache(DBRecords dbRecords) {
        bufferQueue.add(dbRecords);

        if (bufferQueue.size() >= 9) {
            while (!bufferQueue.isEmpty()) {
                DBRecords op = bufferQueue.poll();
                if (op != null) flushQueue.add(op);
            }
            startFlush();
        }
    }

    public void startFlush() {
        executor.submit(() -> {
            while (!flushQueue.isEmpty()) {
                DBRecords dbRecords = flushQueue.poll();
                if (dbRecords != null) {
                    dbRecords.apply(dbManager);
                }
            }
        });
    }

    public void flushNow() {
        flushQueue.addAll(bufferQueue);
        bufferQueue.clear();
        startFlush();
    }
    public void stop() {
        executor.shutdownNow();
    }
}