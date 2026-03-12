package main.io.github.itshaithamn.teamsandmore.teammanager;

import java.sql.Timestamp;

public interface DBRecords {
    void apply(TeamDatabaseManager db);


    record createTeamRecord(String teamName, String wayPointLocation) implements DBRecords {
        @Override
        public void apply(TeamDatabaseManager db) {
            db.createTeam(teamName, wayPointLocation);
        }
    }

    record addToTeamRecord(String uuid, String teamName, String roleName, int rolePriority, Timestamp dateJoined) implements DBRecords {
        @Override
        public void apply(TeamDatabaseManager db) {
            db.addToTeam(uuid, teamName, roleName, rolePriority, dateJoined);
        }
    }

    record removeFromTeamRecord(String uuid) implements DBRecords {
        @Override
        public void apply(TeamDatabaseManager db) {
            db.removeFromTeam(uuid);
        }
    }

    record rolePriorityPlayer(String uuid) implements DBRecords {
        //Bruh what :sob:
        @Override
        public void apply(TeamDatabaseManager db) {
            db
        }
    }
}