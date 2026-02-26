package main.io.github.itshaithamn.teamsandmore.teammanager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;

public class TeamDatabaseManager {

    private final HikariDataSource source;
    public TeamDatabaseManager(File dbFile) {
        this.source = createDatabase(dbFile);
    }


    private HikariDataSource createDatabase(File dbFile) {
        if (!dbFile.exists()) dbFile.mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + new File(dbFile, "teams.db").getAbsolutePath());
        config.setMaximumPoolSize(1);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        HikariDataSource source = new HikariDataSource(config);

        setupTables(source);
        return source;
    }

    private void setupTables(HikariDataSource source) {
        try (Connection conn = source.getConnection();
             Statement stmt = conn.createStatement()) {


            stmt.execute("PRAGMA journal_mode=WAL;");
            String sql =
                    "CREATE TABLE IF NOT EXISTS teams ("
                            + "uuid TEXT PRIMARY KEY,"
                            + "team_name TEXT,"
                            + "role_name TEXT,"
                            + "role_priority INTEGER,"
                            + "date_joined DATETIME);";
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addToTeam(String uuid, String teamName, String roleName, int rolePriority, Timestamp dateJoined) {
        String sql =
                "INSERT OR IGNORE INTO teams (uuid, team_name, role_name, role_priority, date_joined)"
                +" VALUES (?, ?, ?, ?, ?)";
        try (
                Connection conn = source.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, uuid);
            stmt.setString(2, teamName);
            stmt.setString(3, roleName);
            stmt.setInt(4, rolePriority);
            stmt.setTimestamp(5, dateJoined);

            stmt.executeUpdate();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void removeFromTeam(String uuid) {
        String sql =
                "DELETE FROM teams WHERE uuid = ?";

        try (
                Connection conn = source.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, uuid);

            stmt.executeUpdate();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public String getTeam(Player player) {
        String sql = "SELECT team_name FROM teams WHERE uuid = ?";

        try (
                Connection conn = source.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, player.getUniqueId().toString());

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getString("team_name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public HikariDataSource getSource() {
        return this.source;
    }
}
