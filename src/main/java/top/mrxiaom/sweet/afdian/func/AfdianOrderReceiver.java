package top.mrxiaom.sweet.afdian.func;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.afdian.SweetAfdian;
import top.mrxiaom.sweet.afdian.func.checker.ByAPI;
import top.mrxiaom.sweet.afdian.func.checker.ByWebhook;
import top.mrxiaom.sweet.afdian.func.checker.CheckerMode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static top.mrxiaom.sweet.afdian.utils.JsonUtils.*;
import static top.mrxiaom.sweet.afdian.utils.JsonUtils.optString;

@AutoRegister
public class AfdianOrderReceiver extends AbstractModule {
    private CheckerMode mode;
    ByAPI byAPI = new ByAPI(this);
    ByWebhook byWebhook = new ByWebhook(this);
    private String userId, apiToken;
    private Pattern playerNamePattern;
    private int playerNameMinLength, playerNameMaxLength;
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
        byAPI.reload(config);
        byWebhook.reload(config);
    }

    public boolean matchPlayerName(String s) {
        int length = s.length();
        if (length < playerNameMinLength || length > playerNameMaxLength) return false;
        Matcher m = playerNamePattern.matcher(s);
        return m.matches() && m.start() == 0 && m.end() == length;
    }

    public void handleReceiveOrder(@NotNull String outTradeNo, JsonObject obj) {
        plugin.getProceedOrder().put(outTradeNo, obj.toString());
        String userId = optString(obj, "user_id", null);
        String planId = optString(obj, "plan_id", null);
        Integer month = optInt(obj, "month", null);
        String totalAmount = optString(obj, "total_amount", null);
        String showAmount = optString(obj, "show_amount", null);
        Integer status = optInt(obj, "status", null);
        String remark = optString(obj, "remark", "");
        String redeemId = optString(obj, "redeem_id", null);
        int productType = optInt(obj, "product_type", -1);
        String discount = optString(obj, "discount", null);
        Long createTime = optLong(obj, "create_time", null);
        String planTitle = optString(obj, "plan_title", "");

        String userPrivateId = optString(obj, "user_private_id", null);
        String addressPerson = optString(obj, "address_person", null);
        String addressPhone = optString(obj, "address_phone", null);
        String addressAddress = optString(obj, "address_address", null);
        if (!matchPlayerName(remark.trim())) return;
        Double money = Util.parseDouble(totalAmount).orElse(null);
        if (money == null) return;
        if (productType == 0) { // 普通赞助
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
                if (skuId == null || name == null) continue;

            }
            return;
        }
        // TODO: 处理轮询获取的数据，比如储存已处理的订单号 outTradeNo
    }

    @Override
    public void onDisable() {
        byAPI.stopTask();
        byWebhook.stopWebHook();
    }
}
