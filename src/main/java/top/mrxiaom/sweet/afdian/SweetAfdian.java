package top.mrxiaom.sweet.afdian;
        
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.EconomyHolder;
import top.mrxiaom.sweet.afdian.database.ProceedOrderDatabase;

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
}
