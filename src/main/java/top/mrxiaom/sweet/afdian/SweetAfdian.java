package top.mrxiaom.sweet.afdian;
        
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.utils.PAPI;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.afdian.database.ProceedOrderDatabase;

import java.util.List;

import static top.mrxiaom.pluginbase.func.AbstractPluginHolder.t;

public class SweetAfdian extends BukkitPlugin {
    public static SweetAfdian getInstance() {
        return (SweetAfdian) BukkitPlugin.getInstance();
    }

    public SweetAfdian() {
        super(options()
                .bungee(false)
                .adventure(false)
                .database(true)
                .reconnectDatabaseWhenReloadConfig(false)
                .vaultEconomy(false)
                .scanIgnore("top.mrxiaom.sweet.afdian.libs")
        );
    }
    ProceedOrderDatabase proceedOrder;
    public boolean debug;

    public ProceedOrderDatabase getProceedOrder() {
        return proceedOrder;
    }

    @Override
    protected void beforeEnable() {
        options.registerDatabase(
                proceedOrder = new ProceedOrderDatabase(this)
        );
    }

    @Override
    protected void afterEnable() {
        getLogger().info("SweetAfdian 加载完毕");
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        debug = getConfig().getBoolean("debug", false);
    }

    @SafeVarargs
    public final void runCommands(String playerName, List<String> commands, Pair<String, Object>... replacements) {
        Player player = Util.getOnlinePlayer(playerName).orElse(null);
        for (String str : commands) {
            String s = Pair.replace(str, replacements);
            if (s.startsWith("[console]")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s.substring(9));
            }
            if (s.startsWith("[message]") && player != null) {
                t(player, PAPI.setPlaceholders(player, s.substring(9)));
            }
        }
    }
}
