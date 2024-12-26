package top.mrxiaom.sweet.afdian.func.entry;

import org.bukkit.configuration.ConfigurationSection;
import top.mrxiaom.pluginbase.utils.Util;

public class ShopItem {
    public final String itemName;
    public final String skuName;
    public final ExecuteType type;
    public final Order order;

    public ShopItem(String itemName, String skuName, ExecuteType type, Order order) {
        this.itemName = itemName;
        this.skuName = skuName;
        this.type = type;
        this.order = order;
    }

    public static ShopItem load(ConfigurationSection itemSection, String itemName, String skuName) {
        Order order = Order.load(itemSection, skuName);
        ExecuteType type = Util.valueOr(ExecuteType.class, itemSection.getString(skuName + ".type"), ExecuteType.command);
        return new ShopItem(itemName, skuName, type, order);
    }
}
