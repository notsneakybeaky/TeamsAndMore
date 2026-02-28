package main.io.github.itshaithamn.teamsandmore.teammanager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TeamDatabaseManager {

    private final HikariDataSource source;
    public TeamDatabaseManager(File dbFile) {
        this.source = createDatabase(dbFile);
    }

    private HikariDataSource createDatabase(File dbFile) {
        if (!dbFile.exists()) dbFile.mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + new File(dbFile, "test.db").getAbsolutePath());
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
            stmt.execute("PRAGMA foreign_keys = ON;");
            String teamsTable =
                    "CREATE TABLE IF NOT EXISTS teams ("
                            + "team_name TEXT PRIMARY KEY,"
                            + "description TEXT,"
                            + "way_point_location TEXT);"
                    ;
            stmt.execute(teamsTable);

            String allPlayersTable =
                    "CREATE TABLE IF NOT EXISTS players ("
                            + "uuid TEXT PRIMARY KEY,"
                            + "team_name TEXT,"
                            + "role_name TEXT,"
                            + "role_priority INTEGER,"
                            + "date_joined DATETIME,"
                            + "FOREIGN KEY(team_name) REFERENCES teams(team_name));"
                    ;
            stmt.execute(allPlayersTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /// Queries the allPlayers table
    public void addToTeam(String uuid, String teamName, String roleName, int rolePriority, Timestamp dateJoined) {
        String sql =
                "INSERT OR IGNORE INTO players (uuid, team_name, role_name, role_priority, date_joined)"
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

    /// Queries the allPlayers table
    public void removeFromTeam(String uuid) {
        String sql =
                "DELETE FROM players WHERE uuid = ?";

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

    /// Queries the allPlayers table
    public String getTeamOfPlayer(Player player) {
        String sql = "SELECT team_name FROM players WHERE uuid = ?";

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

    /// Queries the teams table
    public void createTeam(String teamName, String description, String wayPointLocation) {
        String sql =
                "INSERT OR IGNORE INTO teams (team_name, description, way_point_location)"
                        +" VALUES (?, ?, ?)";
        try (
                Connection conn = source.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, teamName);
            stmt.setString(2, description);
            stmt.setString(3, wayPointLocation);

            stmt.executeUpdate();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    /// Queries the teams table
    public List<String> getAllTeams() {
        String sql = "SELECT team_name FROM teams";
        List<String> names = new ArrayList<>();

        try (
                Connection conn = source.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                names.add(rs.getString("team_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return names;
    }

    public HikariDataSource getSource() {
        return this.source;
    }
}
