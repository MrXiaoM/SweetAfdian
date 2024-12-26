package top.mrxiaom.sweet.afdian.func.checker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.configuration.MemoryConfiguration;
import top.mrxiaom.sweet.afdian.func.AfdianOrderReceiver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static top.mrxiaom.sweet.afdian.utils.JsonUtils.optObject;
import static top.mrxiaom.sweet.afdian.utils.JsonUtils.optString;

public class ByWebhook {
    AfdianOrderReceiver parent;
    HttpServer server;
    public ByWebhook(AfdianOrderReceiver parent) {
        this.parent = parent;
    }

    private void setupWebHookServer(int port, String path) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            createContext(path, exchange -> {
                if (!exchange.getRequestMethod().equals("POST")) {
                    exchange.sendResponseHeaders(404, 0);
                    exchange.getRequestBody().close();
                    return;
                }
                try (InputStream input = exchange.getRequestBody();
                     InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                    StringBuilder sb = new StringBuilder();
                    char[] buffer = new char[1024];
                    int len;
                    while ((len = reader.read(buffer)) != -1) {
                        sb.append(buffer, 0, len);
                    }
                    try {
                        JsonElement element = JsonParser.parseString(sb.toString());
                        JsonObject json = element.getAsJsonObject();
                        JsonObject data = optObject(json, "data");
                        JsonObject order = optObject(data, "order");
                        String outTradeNo = optString(order, "out_trade_no", null);
                        if (outTradeNo != null) {
                            parent.handleReceiveOrder(outTradeNo, order);
                        }
                    } catch (JsonSyntaxException | IllegalStateException ignored) {
                    }
                }
                byte[] response = "{\"ec\":200,\"em\":\"成功\"}".getBytes();
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            });
            server.start();
        } catch (IOException e) {
            parent.warn(e);
        }
    }
    public interface Handler {
        void run(HttpExchange exchange) throws IOException;
    }
    private void createContext(String path, Handler handler) {
        if (server == null) return;
        server.createContext(path, exchange -> {
            try {
                handler.run(exchange);
            } catch (IOException t) {
                parent.warn(t);
            }
        });
    }

    public void stopWebHook() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public void reload(MemoryConfiguration config) {
        stopWebHook();
        if (CheckerMode.WEB_HOOK.equals(parent.getMode())) {
            int port = config.getInt("web_hook.port", 8087);
            String path = config.getString("web_hook.path", "/api/afdian/hook");
            setupWebHookServer(port, path);
        }
    }

}
