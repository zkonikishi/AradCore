package cn.owonya.aradcore;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class AradCorePlugin extends JavaPlugin {
    private GradeService grades;
    private GradeListener listener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        grades = new GradeService(this);
        listener = new GradeListener(this, grades);
        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getPluginManager().registerEvents(new MmoItemsBuildListener(grades), this);

        PluginCommand command = getCommand("aradcore");
        if (command != null) {
            AradCommand handler = new AradCommand(this, grades);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }
        getLogger().info("AradCore 0.1 equipment-grade module enabled=" + grades.isEnabled() + ".");
    }

    @Override
    public void onDisable() {
        if (listener != null) listener.cancelPending();
    }

    public void reloadModule() {
        if (listener != null) listener.cancelPending();
        reloadConfig();
        grades.reload();
    }
}
