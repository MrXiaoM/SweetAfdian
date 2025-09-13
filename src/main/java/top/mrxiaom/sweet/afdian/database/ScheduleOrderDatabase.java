package top.mrxiaom.sweet.afdian.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.sweet.afdian.SweetAfdian;
import top.mrxiaom.sweet.afdian.func.AbstractPluginHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ScheduleOrderDatabase extends AbstractPluginHolder implements IDatabase {
    private final Gson gson = new GsonBuilder().create();
    private String TABLE_NAME;
    public ScheduleOrderDatabase(SweetAfdian plugin) {
        super(plugin);
    }

    @Override
    public void reload(Connection conn, String s) throws SQLException {
        TABLE_NAME = (s + "schedule").toUpperCase();
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_NAME + "`(" +
                        "`name` VARCHAR(64)," +
                        "`data` LONGTEXT" +
                        ");"
        )) {
            ps.execute();
        }
    }

    public void put(String name, JsonObject data) {
        try (Connection conn = plugin.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO `" + TABLE_NAME + "`(`name`,`data`) VALUES(?, ?);"
            )) {
                ps.setString(1, name);
                ps.setString(2, data.toString());
        } catch (SQLException e) {
            warn(e);
        }
    }

    public List<JsonObject> fetch(String name) {
        try (Connection conn = plugin.getConnection()) {
            List<JsonObject> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM `" + TABLE_NAME + "` WHERE `name`=?;"
            )) {
                ps.setString(1, name);
                try (ResultSet result = ps.executeQuery()) {
                    while (result.next()) {
                        String dataStr = result.getString("data");
                        try {
                            list.add(gson.fromJson(dataStr, JsonObject.class));
                        } catch (JsonSyntaxException ignored) {
                        }
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM `" + TABLE_NAME + "` WHERE `name`=?;"
            )) {
                ps.setString(1, name);
                ps.execute();
            }
            return list;
        } catch (SQLException e) {
            warn(e);
            return null;
        }
    }
}
