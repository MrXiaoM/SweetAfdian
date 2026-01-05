package top.mrxiaom.sweet.afdian.func.entry;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.afdian.SweetAfdian;
import top.mrxiaom.sweet.afdian.events.ReceiveOrderEvent;

import java.util.List;
import java.util.function.Function;

public class Order {
    public final String name;
    public final Function<Double, String> pointTransformer;
    public final boolean requireOnline;
    public final boolean scheduleJoin;
    public final List<IAction> commands;

    private Order(String name, Function<Double, String> pointTransformer, boolean requireOnline, boolean scheduleJoin, List<IAction> commands) {
        this.name = name;
        this.pointTransformer = pointTransformer;
        this.requireOnline = requireOnline;
        this.scheduleJoin = scheduleJoin;
        this.commands = commands;
    }

    public void execute(
            @NotNull SweetAfdian plugin,
            @Nullable ReceiveOrderEvent event,
            @NotNull String player,
            @NotNull Double money,
            @NotNull String point,
            int times,
            @NotNull String person,
            @NotNull String phone,
            @NotNull String address
    ) {
        plugin.getScheduler().runTask(() -> {
            if (event != null) {
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) return;
            }
            Player realPlayer = Util.getOnlinePlayer(player).orElse(null);
            if (realPlayer == null || !realPlayer.isOnline()) {
                if (scheduleJoin) {
                    JsonObject data = new JsonObject();
                    data.addProperty("name", name);
                    data.addProperty("player", player);
                    data.addProperty("money", money);
                    data.addProperty("point", point);
                    data.addProperty("times", times);
                    data.addProperty("person", person);
                    data.addProperty("phone", phone);
                    data.addProperty("address", address);
                    // 提交到数据库
                    plugin.getScheduleOrder().put(player, data);
                    plugin.info("[" + name + "] 玩家 " + player + " 下单的订单将在他上线后给予奖励");
                    return;
                }
                if (requireOnline) {
                    plugin.warn("[" + name + "] 执行要求玩家在线的订单操作时，玩家 " + player + " 不在线");
                    return;
                }
            }
            execute(plugin, player, realPlayer, money, point, times, person, phone, address);
        });
    }

    public void execute(
            @NotNull SweetAfdian plugin,
            @NotNull Player player,
            @NotNull Double money,
            @NotNull String point,
            int times,
            @NotNull String person,
            @NotNull String phone,
            @NotNull String address
    ) {
        execute(plugin, player.getName(), player, money, point, times, person, phone, address);
    }

    public void execute(
            @NotNull SweetAfdian plugin,
            @NotNull String player,
            @Nullable Player realPlayer,
            @NotNull Double money,
            @NotNull String point,
            int times,
            @NotNull String person,
            @NotNull String phone,
            @NotNull String address
    ) {
        ListPair<String, Object> r = new ListPair<>();
        r.add("%player%", player);
        r.add("%money%", money);
        r.add("%point%", point);
        r.add("%person%", person);
        r.add("%phone%", phone);
        r.add("%address%", address);
        for (int i = 0; i < times; i++) {
            ActionProviders.run(plugin, realPlayer, commands, r);
        }
    }

    public static Order load(ConfigurationSection section, String key) {
        return load(section, key, key);
    }
    public static Order load(ConfigurationSection section, String key, String name) {
        double scale = section.getDouble(key + ".point.scale", 10.0);
        String rounding = section.getString(key + ".point.rounding", "floor");
        Function<Double, String> transformer;
        if (rounding.equals("ceil")) {
            transformer = d -> String.valueOf((int) Math.ceil(d * scale));
        } else if (rounding.equals("round")) {
            transformer = d -> String.valueOf((int) Math.round(d * scale));
        } else {
            Integer i = Util.parseInt(rounding).orElse(null);
            if (i != null) {
                String format = "%." + i + "f";
                transformer = d -> String.format(format, d * scale);
            } else { // floor
                transformer = d -> String.valueOf((int) Math.floor(d * scale));
            }
        }
        boolean requireOnline = section.getBoolean(key + ".require-online", false);
        boolean scheduleJoin = section.getBoolean(key + ".schedule-join", false);
        List<IAction> commands = ActionProviders.loadActions(section, key + ".commands");
        return new Order(name, transformer, requireOnline, scheduleJoin, commands);
    }
}
