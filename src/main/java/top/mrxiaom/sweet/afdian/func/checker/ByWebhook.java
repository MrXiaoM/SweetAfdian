package top.mrxiaom.sweet.afdian.func.checker;

import com.google.gson.*;
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
import java.util.HashSet;
import java.util.Set;

import static top.mrxiaom.sweet.afdian.utils.JsonUtils.*;

public class ByWebhook {
    private final AfdianOrderReceiver parent;
    private HttpServer server;
    private boolean ignoreAll;
    private final Set<String> whitelist = new HashSet<>();
    private final Set<String> blocked = new HashSet<>();
    private final Set<String> handledOrders = new HashSet<>();
    public ByWebhook(AfdianOrderReceiver parent) {
        this.parent = parent;
    }

    private void setupWebHookServer(int port, String hookPath) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            createContext(hookPath, exchange -> {
                if (!exchange.getRequestMethod().equals("POST")) {
                    // 隐藏自己，我就不说 400 Bad Request，我就说 404 Not Found
                    byte[] message = "<h1>404 Not Found</h1>No context found for request"
                            .getBytes("ISO8859_1");
                    exchange.getResponseHeaders().add("Content-Type", "text/html");
                    exchange.sendResponseHeaders(404, message.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(message);
                    }
                    return;
                }
                String hostName = exchange.getRemoteAddress().getHostName();
                if (!whitelist.isEmpty()) {
                    if (blocked.contains(hostName)) return;
                    if (!whitelist.contains(hostName)) {
                        blocked.add(hostName);
                        parent.warn("[" + hostName + "] 不在 WebHook 白名单中，禁止访问");
                        byte[] message = "<h1>403 Forbidden</h1>Authorize failed"
                                .getBytes("ISO8859_1");
                        exchange.getResponseHeaders().add("Content-Type", "text/html");
                        exchange.sendResponseHeaders(403, message.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(message);
                        }
                        return;
                    }
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
                        if (parent.plugin.debug) parent.info(order.toString());
                        if (outTradeNo != null) {
                            boolean isTestOrder = outTradeNo.equals("202106232138371083454010626");
                            JsonObject leakCheck = null;
                            if (!ignoreAll && !isTestOrder) {
                                String path = "/api/open/query-order";
                                JsonObject params = new JsonObject();
                                params.addProperty("per_page", 1);
                                params.addProperty("out_trade_no", outTradeNo);
                                JsonObject result = ByAPI.request(path, parent.getUserId(), parent.getApiToken(), params);
                                if (optInt(result, "ec", 0) == 200) {
                                    JsonObject data1 = optObject(result, "data");
                                    JsonArray list = optArray(data1, "list");
                                    for (JsonElement element1 : list) {
                                        leakCheck = element1.getAsJsonObject();
                                        break;
                                    }
                                }
                            }
                            if (isTestOrder) {
                                parent.info("[" + hostName + "] 成功收到爱发电测试订单 " + optString(order, "plan_title", ""));
                            } else if (leakCheck == null) {
                                parent.warn("[" + hostName + "] WebHook 收到了异常的订单号 " + outTradeNo + "，无法通过爱发电接口查询到其信息");
                            } else {
                                if (handledOrders.add(outTradeNo)) {
                                    parent.printOrder("[" + hostName + "] ", outTradeNo, leakCheck, ignoreAll);
                                    parent.plugin.getProceedOrder().put(outTradeNo, leakCheck.toString());
                                    if (!ignoreAll) {
                                        parent.handleReceiveOrder(outTradeNo, leakCheck);
                                    }
                                } else {
                                    parent.printOrder("[" + hostName + "][已处理订单] ", outTradeNo, leakCheck, ignoreAll);
                                }
                            }
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
            whitelist.clear();
            blocked.clear();
            whitelist.addAll(config.getStringList("web_hook.whitelist"));
            ignoreAll = config.getBoolean("web_hook.ignore-all", true);
            int port = config.getInt("web_hook.port", 8087);
            String path = config.getString("web_hook.path", "/api/afdian/hook");
            setupWebHookServer(port, path);
            if (ignoreAll) {
                parent.info("工作模式: WebHook http://<ip>:" + port + path + " 忽略全部订单");
            } else {
                parent.info("工作模式: WebHook http://<ip>:" + port + path);
            }
        }
    }

}
