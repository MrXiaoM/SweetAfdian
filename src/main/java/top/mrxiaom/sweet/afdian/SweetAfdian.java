package top.mrxiaom.sweet.afdian;
        
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.utils.PAPI;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.afdian.database.ProceedOrderDatabase;

import java.util.ArrayList;
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
    public final void runCommands(String playerName, @Nullable Player player, List<String> commands, Pair<String, Object>... replacements) {
        List<String> parsed = new ArrayList<>();
        for (String command : commands) {
            parsed.add(Pair.replace(command, replacements));
        }
        OfflinePlayer offline = player != null ? player : Util.getOfflinePlayer(playerName).orElse(null);
        List<String> list = offline == null ? parsed : PAPI.setPlaceholders(offline, parsed);
        for (String str : list) {
            String s = Pair.replace(str, replacements);
            if (s.startsWith("[console]")) {
                String command = s.substring(9);
                getInstance().getLogger().info("[执行命令] " + command);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
            if (player != null) {
                if (s.startsWith("[player]")) {
                    String command = s.substring(8);
                    getInstance().getLogger().info("[玩家执行][" + player.getName() + "] " + command);
                    Bukkit.dispatchCommand(player, command);
                }
                if (s.startsWith("[message]")) {
                    t(player, s.substring(9));
                }
            }
        }
    }
}
