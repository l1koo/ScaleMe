package l1kko.scaleme;

import org.bukkit.plugin.java.JavaPlugin;

public final class ScaleMe extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ScaleCommand scaleCommand = new ScaleCommand(this);

        getCommand("scale").setExecutor(scaleCommand);
        getCommand("scale").setTabCompleter(scaleCommand);

        getLogger().info("ScaleMe успешно включен! Версия API 1.20.5+");
    }

    @Override
    public void onDisable() {
        getLogger().info("ScaleMe выключен.");
    }
}