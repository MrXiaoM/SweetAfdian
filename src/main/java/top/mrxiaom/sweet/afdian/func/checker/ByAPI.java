package top.mrxiaom.sweet.afdian.func.checker;

import com.google.gson.*;
import org.bukkit.configuration.MemoryConfiguration;
import top.mrxiaom.pluginbase.api.IRunTask;
import top.mrxiaom.sweet.afdian.func.AfdianOrderReceiver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static top.mrxiaom.sweet.afdian.utils.JsonUtils.*;

public class ByAPI {
    private final AfdianOrderReceiver parent;
    private IRunTask task;
    private int limitOrder;
    private boolean ignoreAll;
    public ByAPI(AfdianOrderReceiver parent) {
        this.parent = parent;
    }

    public void reload(MemoryConfiguration config) {
        stopTask();
        if (CheckerMode.POLLING_API.equals(parent.getMode()) && parent.configuredApi()) {
            long periodSecond = config.getLong("polling_api.period_seconds", 30L);
            long period = periodSecond * 20L;
            limitOrder = config.getInt("polling_api.limit_order", 50);
            ignoreAll = config.getBoolean("polling_api.ignore-all", true);
            task = parent.plugin.getScheduler().runTaskTimerAsynchronously(this::run, period, period);
            if (ignoreAll) {
                parent.info("工作模式: 轮询API/" + periodSecond + "秒 忽略全部订单");
            } else {
                parent.info("工作模式: 轮询API/" + periodSecond + "秒");
            }
        }
    }

    public void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void run() {
        String path = "/api/open/query-order";
        JsonObject params = new JsonObject();
        params.addProperty("page", 1);
        JsonObject result = ByAPI.request(path, parent.getUserId(), parent.getApiToken(), params);
        Map<String, JsonObject> ordersMap = new HashMap<>();
        if (optInt(result, "ec", 0) == 200) {
            JsonObject data = optObject(result, "data");
            JsonArray list = optArray(data, "list");
            for (JsonElement element : list) {
                JsonObject order = element.getAsJsonObject();
                String outTradeNo = optString(order, "out_trade_no", null);
                if (outTradeNo == null) continue;
                ordersMap.put(outTradeNo, order);
            }
        }
        List<String> outTradeNos = new ArrayList<>(ordersMap.keySet());
        List<String> keys = parent.plugin.getProceedOrder().filterOrders(outTradeNos);
        for (String key : keys) {
            JsonObject order = ordersMap.get(key);
            if (order == null) continue;
            if (optInt(order, "status", -1) != 2) continue;
            parent.info("收到新的订单 " + key + " " + optString(order, "plan_title", "") + " " + optString(order, "remark", ""));
            if (parent.plugin.debug) parent.info(order.toString());
            parent.plugin.getProceedOrder().put(key, order.toString());
            if (!ignoreAll) {
                parent.handleReceiveOrder(key, order);
            }
        }
    }

    public static JsonObject request(String path, String userId, String token, JsonObject params) {
        try {
            String url = "https://afdian.com" + path;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            String body = buildParams(userId, token, params);
            try (PrintWriter writer = new PrintWriter(conn.getOutputStream())) {
                writer.write(body);
                writer.flush();
            }
            try (InputStream input = conn.getInputStream();
                 InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[1024];
                int len;
                while ((len = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, len);
                }
                JsonElement element = JsonParser.parseString(sb.toString());
                return element.getAsJsonObject();
            }
        } catch (IOException | JsonSyntaxException e) {
            return null;
        }
    }

    public static String buildParams(String userId, String token, JsonObject params) {
        JsonObject object = new JsonObject();
        object.addProperty("user_id", userId);
        object.addProperty("params", params.toString());
        long timestamp = System.currentTimeMillis() / 1000L;
        String sign = sign(userId, token, params.toString(), timestamp);
        object.addProperty("ts", timestamp);
        object.addProperty("sign", sign);
        return object.toString();
    }

    public static String sign(String userId, String token, String params, long timestamp) {
        String str = token + "params" + params + "ts" + timestamp + "user_id" + userId;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
