package top.mrxiaom.sweet.afdian.commands;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.afdian.SweetAfdian;
import top.mrxiaom.sweet.afdian.func.AbstractModule;
import top.mrxiaom.sweet.afdian.func.AfdianOrderReceiver;
import top.mrxiaom.sweet.afdian.func.checker.ByAPI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@AutoRegister
public class CommandMain extends AbstractModule implements CommandExecutor, TabCompleter, Listener {
    public CommandMain(SweetAfdian plugin) {
        super(plugin);
        registerCommand("sweetafdian", this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1 && "query".equalsIgnoreCase(args[0]) && sender.isOp()) {
            String path = "/api/open/query-order";
            JsonObject params = new JsonObject();
            if (args.length > 1) {
                params.addProperty("per_page", 1);
                params.addProperty("out_trade_no", args[1]);
            }
            AfdianOrderReceiver parent = instanceOf(AfdianOrderReceiver.class);
            JsonObject result = ByAPI.request(path, parent.getUserId(), parent.getApiToken(), params);
            return t(sender, "/api/open/query-order: " + result);
        }
        if (args.length >= 1 && "reload".equalsIgnoreCase(args[0]) && sender.isOp()) {
            if (args.length == 2 && "database".equalsIgnoreCase(args[1])) {
                plugin.options.database().reloadConfig();
                plugin.options.database().reconnect();
                return t(sender, "&a数据库已重新连接");
            }
            plugin.reloadConfig();
            return t(sender, "&a配置文件已重载");
        }
        return true;
    }

    private static final List<String> emptyList = Lists.newArrayList();
    private static final List<String> listArg0 = Lists.newArrayList();
    private static final List<String> listArgReload = Lists.newArrayList("database");
    private static final List<String> listOpArg0 = Lists.newArrayList("query", "reload");
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return startsWith(sender.isOp() ? listOpArg0 : listArg0, args[0]);
        }
        if (args.length == 2) {
            if (sender.isOp()) {
                if ("reload".equalsIgnoreCase(args[0])) {
                    return startsWith(listArgReload, args[1]);
                }
            }
        }
        return emptyList;
    }

    public List<String> startsWith(Collection<String> list, String s) {
        return startsWith(null, list, s);
    }
    public List<String> startsWith(String[] addition, Collection<String> list, String s) {
        String s1 = s.toLowerCase();
        List<String> stringList = new ArrayList<>(list);
        if (addition != null) stringList.addAll(0, Lists.newArrayList(addition));
        stringList.removeIf(it -> !it.toLowerCase().startsWith(s1));
        return stringList;
    }
}
