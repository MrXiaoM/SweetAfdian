package top.mrxiaom.sweet.afdian.database;

import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.sweet.afdian.SweetAfdian;
import top.mrxiaom.sweet.afdian.func.AbstractPluginHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProceedOrderDatabase extends AbstractPluginHolder implements IDatabase {
    private String TABLE_NAME;
    public ProceedOrderDatabase(SweetAfdian plugin) {
        super(plugin);
    }
    @Override
    public void reload(Connection connection, String s) throws SQLException {
        TABLE_NAME = (s + "orders").toUpperCase();
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_NAME + "`(" +
                        "`out_trade_no` VARCHAR(36) PRIMARY KEY," +
                        "`order` LONGTEXT" +
                ");"
        )) {
            ps.execute();
        }
    }

    public void put(String outTradeNo, String json) {
        String sentence;
        if (plugin.options.database().isSQLite()) {
            sentence = "INSERT OR REPLACE INTO `" + TABLE_NAME + "`(`out_trade_no`, `order`) VALUES(?, ?);";
        } else if (plugin.options.database().isMySQL()) {
            sentence = "INSERT INTO `" + TABLE_NAME + "`(`out_trade_no`, `order`) VALUES(?, ?) on duplicate key update `order`=?;";
        } else return;
        try (Connection conn = plugin.getConnection();
            PreparedStatement ps = conn.prepareStatement(sentence)) {
            ps.setString(1, outTradeNo);
            ps.setString(2, json);
            if (plugin.options.database().isMySQL()) {
                ps.setString(3, json);
            }
            ps.execute();
        } catch (SQLException e) {
            warn(e);
        }
    }

    public List<String> filterOrders(List<String> outTradeNos) {
        List<String> list = new ArrayList<>();
        try (Connection conn = plugin.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TEMPORARY TABLE if NOT EXISTS `SWEET_AFDIAN_TEMP_TABLE`(`out_trade_no` VARCHAR(36));"
            )) { ps.execute(); }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO `SWEET_AFDIAN_TEMP_TABLE`(`out_trade_no`) VALUES(?);"
            )) {
                for (String outTradeNo : outTradeNos) {
                    ps.setString(1, outTradeNo);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT t.`out_trade_no` from `SWEET_AFDIAN_TEMP_TABLE` t " +
                    "LEFT JOIN `" + TABLE_NAME + "` o " +
                    "ON t.`out_trade_no` = o.`out_trade_no` " +
                    "WHERE o.`out_trade_no` IS NULL;")) {
                ResultSet resultSet = ps.executeQuery();
                while(resultSet.next()) {
                    String outTradeNo = resultSet.getString("out_trade_no");
                    list.add(outTradeNo);
                }
            }
            // 因为使用了连接池，以免连接被回收未关闭，导致临时表未删除
            try (PreparedStatement ps = conn.prepareStatement(
                    "DROP TABLE if EXISTS `SWEET_AFDIAN_TEMP_TABLE`;"
            )) { ps.execute(); }
        } catch (SQLException e) {
            warn(e);
        }
        return list;
    }
}
