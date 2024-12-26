package top.mrxiaom.sweet.afdian.func;
        
import top.mrxiaom.sweet.afdian.SweetAfdian;

@SuppressWarnings({"unused"})
public abstract class AbstractPluginHolder extends top.mrxiaom.pluginbase.func.AbstractPluginHolder<SweetAfdian> {
    public AbstractPluginHolder(SweetAfdian plugin) {
        super(plugin);
    }

    public AbstractPluginHolder(SweetAfdian plugin, boolean register) {
        super(plugin, register);
    }
}
