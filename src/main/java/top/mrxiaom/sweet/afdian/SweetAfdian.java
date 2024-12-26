package top.mrxiaom.sweet.afdian;
        
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.EconomyHolder;

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


    @Override
    protected void beforeEnable() {
        options.registerDatabase(
                // 在这里添加数据库 (如果需要的话)
        );
    }

    @Override
    protected void afterEnable() {
        getLogger().info("SweetAfdian 加载完毕");
    }
}
