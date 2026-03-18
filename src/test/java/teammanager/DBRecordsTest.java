package teammanager;

import main.io.github.itshaithamn.teamsandmore.teammanager.DBRecords;
import main.io.github.itshaithamn.teamsandmore.teammanager.TeamDatabaseManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DBRecordsTest {

    @Mock private TeamDatabaseManager dbManager;

    @Test
    void createTeamRecord_shouldCallCreateTeam() {
        DBRecords record = new DBRecords.createTeamRecord("Alpha", "world;0;64;0");
        record.apply(dbManager);

        verify(dbManager).createTeam("Alpha", "world;0;64;0");
    }

    @Test
    void addToTeamRecord_shouldCallAddToTeam() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        DBRecords record = new DBRecords.addToTeamRecord("uuid-1", "Alpha", "leader", 0, now);
        record.apply(dbManager);

        verify(dbManager).addToTeam("uuid-1", "Alpha", "leader", 0, now);
    }

    @Test
    void removeFromTeamRecord_shouldCallRemoveFromTeam() {
        DBRecords record = new DBRecords.removeFromTeamRecord("uuid-1");
        record.apply(dbManager);

        verify(dbManager).removeFromTeam("uuid-1");
    }
}
