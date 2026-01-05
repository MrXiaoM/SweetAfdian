package top.mrxiaom.sweet.afdian;
        
import com.google.common.collect.Lists;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.resolver.DefaultLibraryResolver;
import top.mrxiaom.pluginbase.utils.ClassLoaderWrapper;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.scheduler.FoliaLibScheduler;
import top.mrxiaom.sweet.afdian.actions.ActionConsole;
import top.mrxiaom.sweet.afdian.actions.ActionPlayer;
import top.mrxiaom.sweet.afdian.database.ProceedOrderDatabase;
import top.mrxiaom.sweet.afdian.database.ScheduleOrderDatabase;

import java.io.File;
import java.net.URL;
import java.util.List;

public class SweetAfdian extends BukkitPlugin {
    public static SweetAfdian getInstance() {
        return (SweetAfdian) BukkitPlugin.getInstance();
    }

    public SweetAfdian() throws Exception {
        super(options()
                .database(true)
                .scanIgnore("top.mrxiaom.sweet.afdian.libs")
        );
        scheduler = new FoliaLibScheduler(this);

        getLogger().info("正在检查依赖库状态");
        File librariesDir = ClassLoaderWrapper.isSupportLibraryLoader
                ? new File("libraries")
                : new File(this.getDataFolder(), "libraries");
        DefaultLibraryResolver resolver = new DefaultLibraryResolver(getLogger(), librariesDir);

        resolver.addResolvedLibrary(BuildConstants.RESOLVED_LIBRARIES);

        List<URL> libraries = resolver.doResolve();
        getLogger().info("正在添加 " + libraries.size() + " 个依赖库到类加载器");
        for (URL library : libraries) {
            this.classLoader.addURL(library);
        }
    }
    ProceedOrderDatabase proceedOrder;
    ScheduleOrderDatabase scheduleOrder;
    public boolean debug;

    public ProceedOrderDatabase getProceedOrder() {
        return proceedOrder;
    }

    public ScheduleOrderDatabase getScheduleOrder() {
        return scheduleOrder;
    }

    @Override
    protected void beforeEnable() {
        ActionProviders.registerActionProviders(ActionConsole.PROVIDER, ActionPlayer.PROVIDER);
        options.registerDatabase(
                proceedOrder = new ProceedOrderDatabase(this),
                scheduleOrder = new ScheduleOrderDatabase(this)
        );
    }

    @Override
    protected void afterEnable() {
        getLogger().info("SweetAfdian 加载完毕");
    }

    @Override
    protected void beforeReloadConfig(FileConfiguration config) {
        debug = getConfig().getBoolean("debug", false);
    }

    @SafeVarargs
    @Deprecated
    public final void runCommands(String playerName, @Nullable Player player, List<String> commands, Pair<String, Object>... replacements) {
        List<IAction> actions = ActionProviders.loadActions(commands);
        ActionProviders.run(getInstance(), player, actions, Lists.newArrayList(replacements));
    }
}
