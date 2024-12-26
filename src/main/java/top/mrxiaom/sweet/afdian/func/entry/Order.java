package top.mrxiaom.sweet.afdian.func.entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.afdian.SweetAfdian;

import java.util.List;
import java.util.function.Function;

public class Order {
    public final Function<Double, String> pointTransformer;
    public final List<String> commands;

    public Order(Function<Double, String> pointTransformer, List<String> commands) {
        this.pointTransformer = pointTransformer;
        this.commands = commands;
    }

    public void execute(SweetAfdian plugin, String player, Double money, String point, int times, String person, String phone, String address) {
        Pair<String, Object>[] array = Pair.array(6);
        array[0] = Pair.of("%player%", player);
        array[1] = Pair.of("%money%", money);
        array[2] = Pair.of("%point%", point);
        array[3] = Pair.of("%person%", person);
        array[4] = Pair.of("%phone%", phone);
        array[5] = Pair.of("%address%", address);
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (int i = 0; i < times; i++) {
                plugin.runCommands(player, commands, array);
            }
        });
    }

    public static Order load(ConfigurationSection section, String key) {
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
        List<String> commands = section.getStringList(key + ".commands");
        return new Order(transformer, commands);
    }
}
