package top.mrxiaom.sweet.afdian.func;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.afdian.SweetAfdian;
import top.mrxiaom.sweet.afdian.events.ReceiveOrderEvent;
import top.mrxiaom.sweet.afdian.func.checker.ByAPI;
import top.mrxiaom.sweet.afdian.func.checker.ByWebhook;
import top.mrxiaom.sweet.afdian.func.checker.CheckerMode;
import top.mrxiaom.sweet.afdian.func.entry.ExecuteType;
import top.mrxiaom.sweet.afdian.func.entry.Order;
import top.mrxiaom.sweet.afdian.func.entry.ShopItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static top.mrxiaom.sweet.afdian.utils.JsonUtils.*;

@AutoRegister
public class AfdianOrderReceiver extends AbstractModule {
    private CheckerMode mode;
    ByAPI byAPI = new ByAPI(this);
    ByWebhook byWebhook = new ByWebhook(this);
    private String userId, apiToken;
    private Pattern playerNamePattern;
    private int playerNameMinLength, playerNameMaxLength;
    private Order normal;
    private final Map<String, ShopItem> electricShop = new HashMap<>();
    public AfdianOrderReceiver(SweetAfdian plugin) {
        super(plugin);
    }

    @Nullable
    public CheckerMode getMode() {
        return mode;
    }

    public String getUserId() {
        return userId;
    }

    public String getApiToken() {
        return apiToken;
    }

    public boolean configuredApi() {
        return !userId.isEmpty() && !apiToken.isEmpty();
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        mode = Util.valueOr(CheckerMode.class, config.getString("mode"), null);
        if (mode == null) {
            warn("配置文件中设定的 mode 无效");
        }
        userId = config.getString("api.user_id", "");
        apiToken = config.getString("api.token", "");
        if (!configuredApi()) {
            warn("未配置 user_id 或 API Token");
        }
        playerNamePattern = Pattern.compile(config.getString("player-name.pattern", "[a-zA-Z0-9_]*"));
        playerNameMinLength = config.getInt("player-name.min-length");
        playerNameMaxLength = config.getInt("player-name.max-length");
        normal = Order.load(config, "product-normal");
        electricShop.clear();
        ConfigurationSection section = config.getConfigurationSection("product-shop");
        if (section != null) for (String itemName : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(itemName);
            if (itemSection != null) for (String skuName : itemSection.getKeys(false)) {
                ShopItem shop = ShopItem.load(itemSection, itemName, skuName);
                String key = itemName + ":" + skuName;
                electricShop.put(key, shop);
            }
        }
        byAPI.reload(config);
        byWebhook.reload(config);
    }

    public static String firstNotEmptyLine(String s) {
        if (s.contains("\n")) {
            String[] lines = s.split("\n");
            for (String line : lines) {
                String trim = line.trim();
                if (!trim.isEmpty()) return trim;
            }
            return "";
        }
        return s.trim();
    }

    public OfflinePlayer matchPlayerName(String s) {
        int length = s.length();
        if (length < playerNameMinLength || length > playerNameMaxLength) return null;
        Matcher m = playerNamePattern.matcher(s);
        boolean match = m.matches() && m.start() == 0 && m.end() == length;
        return match ? Util.getOfflinePlayer(s).orElse(null) : null;
    }

    public void printOrder(String outTradeNo, JsonObject order) {
        printOrder("", outTradeNo, order);
    }

    public void printOrder(String prefix, String outTradeNo, JsonObject order) {
        int productType = optInt(order, "product_type", -1);
        String remark = optString(order, "remark", "");
        if (productType == 0) {
            String totalAmount = optString(order, "total_amount", null);
            info(prefix + "收到新的订单 " + outTradeNo + " ￥" + totalAmount + remark);
        }
        if (productType == 1) {
            List<String> skuList = new ArrayList<>();
            JsonArray array = optArray(order, "sku_detail");
            for (JsonElement element : array) {
                String skuName = optString(element.getAsJsonObject(), "name", null);
                skuList.add(skuName);
            }
            String skuString = skuList.size() == 1
                    ? skuList.get(0)
                    : ("[" + String.join(", ", skuList) + "]");
            info(prefix + "收到新的订单 " + outTradeNo + " " + optString(order, "plan_title", "") + " " + skuString + " " + remark);
        }
    }

    public void handleReceiveOrder(@NotNull String outTradeNo, JsonObject obj) {
        String userId = optString(obj, "user_id", null);
        String planId = optString(obj, "plan_id", null);
        Integer month = optInt(obj, "month", null);
        String totalAmount = optString(obj, "total_amount", null);
        String showAmount = optString(obj, "show_amount", null);
        int status = optInt(obj, "status", -1);
        String remark = optString(obj, "remark", "");
        String redeemId = optString(obj, "redeem_id", null);
        int productType = optInt(obj, "product_type", -1);
        String discount = optString(obj, "discount", null);
        Long createTime = optLong(obj, "create_time", null);
        String planTitle = optString(obj, "plan_title", "");

        String userPrivateId = optString(obj, "user_private_id", null);
        String addressPerson = optString(obj, "address_person", "");
        String addressPhone = optString(obj, "address_phone", "");
        String addressAddress = optString(obj, "address_address", "");
        String player = firstNotEmptyLine(remark);
        if (status != 2) return;
        OfflinePlayer offline = matchPlayerName(player);
        if (offline == null) return;
        Double money = Util.parseDouble(totalAmount).orElse(null);
        if (money == null) return;
        if (productType == 0) { // 普通赞助
            String point = normal.pointTransformer.apply(money);
            ReceiveOrderEvent event = new ReceiveOrderEvent(player, offline, outTradeNo, null, obj);
            normal.execute(plugin, event, player, money, point, 1, addressPerson, addressPhone, addressAddress);
            return;
        }
        if (productType == 1) { // 电铺
            JsonArray array = optArray(obj, "sku_detail");
            for (JsonElement jsonElement : array) {
                JsonObject object = jsonElement.getAsJsonObject();
                String skuId = optString(object, "sku_id", null);
                String price = optString(object, "price", null);
                int count = optInt(object, "count", 1);
                String name = optString(object, "name", null);
                String albumId = optString(object, "album_id", null);
                String pic = optString(object, "pic", null);
                String stock = optString(object, "stock", null);
                String postId = optString(object, "post_id", null);
                Double priceDouble = Util.parseDouble(price).orElse(null);
                if (skuId == null || name == null || priceDouble == null) continue;
                String key = planTitle + ":" + name;
                ReceiveOrderEvent event = new ReceiveOrderEvent(player, offline, outTradeNo, skuId, obj);
                ShopItem shopItem = electricShop.get(key);
                if (shopItem == null) {
                    plugin.getScheduler().runTask(() -> Bukkit.getPluginManager().callEvent(event));
                    continue;
                }
                if (shopItem.type.equals(ExecuteType.point)) {
                    String point = normal.pointTransformer.apply(priceDouble * count);
                    shopItem.order.execute(plugin, event, player, priceDouble, point, 1, addressPerson, addressPhone, addressAddress);
                } else if (shopItem.type.equals(ExecuteType.command)) {
                    String point = normal.pointTransformer.apply(priceDouble);
                    shopItem.order.execute(plugin, event, player, priceDouble, point, count, addressPerson, addressPhone, addressAddress);
                }
            }
            return;
        }
    }

    @Override
    public void onDisable() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> future = executor.submit(() -> {
            byAPI.stopTask();
            byWebhook.stopWebHook();
            return true;
        });
        try {
            future.get(10, TimeUnit.SECONDS);
        } catch (Throwable ignored) {
            future.cancel(true);
        }
        executor.shutdown();
    }
}
