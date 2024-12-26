package top.mrxiaom.sweet.afdian.func.checker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ByAPI extends BukkitRunnable {
    BukkitTask task;

    public void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void run() {

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
