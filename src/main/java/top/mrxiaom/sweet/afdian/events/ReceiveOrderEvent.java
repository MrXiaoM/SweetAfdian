package top.mrxiaom.sweet.afdian.events;

import com.google.gson.JsonObject;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReceiveOrderEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
    private boolean cancel = false;
    private final @NotNull String playerName;
    private final @NotNull OfflinePlayer player;
    private final @NotNull String outTradeNo;
    private final @Nullable String skuId;
    private final @NotNull JsonObject order;
    public ReceiveOrderEvent(
            @NotNull String playerName,
            @NotNull OfflinePlayer player,
            @NotNull String outTradeNo,
            @Nullable String skuId,
            @NotNull JsonObject order
    ) {
        this.playerName = playerName;
        this.player = player;
        this.outTradeNo = outTradeNo;
        this.skuId = skuId;
        this.order = order.deepCopy();
    }

    /**
     * 接收订单的玩家名。
     */
    @NotNull
    public String getPlayerName() {
        return playerName;
    }

    /**
     * 插件获取到的离线玩家实例，也有可能是在线玩家。
     */
    @NotNull
    public OfflinePlayer getPlayer() {
        return player;
    }

    /**
     * 订单号。
     */
    @NotNull
    public String getOutTradeNo() {
        return outTradeNo;
    }

    /**
     * 商品型号ID，返回null代表没有选定型号。
     */
    @Nullable
    public String getSkuId() {
        return skuId;
    }

    /**
     * 原始订单数据。可参考开发者中心“返回测试数据”的 order，你可以从这里取到 out_trade_no、user_id等值。
     */
    @NotNull
    public JsonObject getOrder() {
        return order;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    /**
     * 取消此事件，则不会执行配置文件中的相关操作。
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }
}
