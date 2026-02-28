package main.io.github.itshaithamn.teamsandmore.teammanager;

import java.io.File;
import java.sql.Timestamp;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Caching {

    //Implementation
    //An async i/o thread is triggered when a ConcurrentHashmap, named flushMap is flushed to by a ConcurrentHashmap.
    //named bufferMap, when bufferMap reaches n-1 out of n elements. The flushMap can have an unbounded size, where as
    //bufferMap has a bounded size of 10.
    public enum DBAction {
        ADDTOTEAM,
        REMOVEFROMTEAM,
        GETTEAM,

    }


    private record TMTeams (DBAction dbAction, String teamName, String roleName, int rolePriority, Timestamp dateJoined) {}
    private final ConcurrentHashMap<String, TMTeams> bufferTM = new ConcurrentHashMap<>(10);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ConcurrentHashMap<String, TMTeams> flushTM = new ConcurrentHashMap<>();
    File testDir = new File("build/test-db");
    private final TeamDatabaseManager dbManager = new TeamDatabaseManager(testDir);

    public void cache(DBAction dbAction, String uuid, String teamName, String roleName, int rolePriority, Timestamp dateJoined) {
        TMTeams team = new TMTeams(dbAction, teamName, roleName, rolePriority, dateJoined);
        bufferTM.put(uuid, team);

        if (bufferTM.size() == 9) {
            //putAll is apparently the method to append ConcurrentHashMaps -\_o_/- ~sigh
            flushTM.putAll(bufferTM);
            bufferTM.clear();
            startFlush();
        }
    }

    public void startFlush() {
        executor.submit(() -> {
            var iterator = flushTM.entrySet().iterator();

            while(iterator.hasNext()) {
                var entry = iterator.next();

//                DBAction dbAction, String teamName, String roleName, int rolePriority, Timestamp dateJoined
                String teamName = entry.getValue().teamName();
                String roleName = entry.getValue().roleName();
                int rolePriority = entry.getValue().rolePriority();
                Timestamp dateJoined = entry.getValue().dateJoined();

                switch (entry.getValue().dbAction) {
                    case DBAction.ADDTOTEAM -> dbManager.addToTeam(entry.getKey(), teamName, roleName, rolePriority, dateJoined);
                    case DBAction.REMOVEFROMTEAM -> dbManager.removeFromTeam(entry.getKey());
                    default -> iterator.remove();
                }
            }

            flushTM.clear();
        });
    }

    public void stop() {
        executor.shutdownNow();
    }
}