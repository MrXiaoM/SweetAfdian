package top.mrxiaom.sweet.afdian.actions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.depend.PAPI;
import top.mrxiaom.sweet.afdian.SweetAfdian;

import java.util.List;

public class ActionConsole implements IAction {
    public static final IActionProvider PROVIDER = IActionProvider.newProvider(999, s -> {
        if (s.startsWith("[console]")) {
            return new ActionConsole(s.substring(9));
        }
        if (s.startsWith("console:")) {
            return new ActionConsole(s.substring(8));
        }
        return null;
    });
    public final String command;
    public ActionConsole(String command) {
        this.command = command;
    }

    @Override
    public void run(@Nullable Player player, @Nullable List<Pair<String, Object>> replacements) {
        String s = Pair.replace(command, replacements);
        String commandLine = PAPI.setPlaceholders(player, s);
        SweetAfdian.getInstance().getLogger().info("[执行命令] " + commandLine);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandLine);
    }
}