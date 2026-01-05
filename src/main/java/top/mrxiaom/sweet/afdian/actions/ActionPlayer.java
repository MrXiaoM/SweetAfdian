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

public class ActionPlayer implements IAction {
    public static final IActionProvider PROVIDER = IActionProvider.newProvider(999, s -> {
        if (s.startsWith("[player]")) {
            return new ActionPlayer(s.substring(8));
        }
        if (s.startsWith("player:")) {
            return new ActionPlayer(s.substring(7));
        }
        return null;
    });
    public final String command;
    public ActionPlayer(String command) {
        this.command = command;
    }

    @Override
    public void run(@Nullable Player player, @Nullable List<Pair<String, Object>> replacements) {
        if (player != null) {
            String s = Pair.replace(command, replacements);
            String commandLine = PAPI.setPlaceholders(player, s);
            SweetAfdian.getInstance().getLogger().info("[玩家执行][" + player.getName() + "] " + commandLine);
            Bukkit.dispatchCommand(player, commandLine);
        }
    }
}
