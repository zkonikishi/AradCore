package cn.owonya.aradcore;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class AradCorePlugin extends JavaPlugin {
    private GradeService grades;

    @Override public void onEnable() {
        saveDefaultConfig();
        grades = new GradeService(this);
        GradeListener listener = new GradeListener(this, grades);
        getServer().getPluginManager().registerEvents(listener, this);
        PluginCommand command = getCommand("aradcore");
        if (command != null) {
            AradCommand handler = new AradCommand(this, grades);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }
        getLogger().info("AradCore 0.1 equipment-grade module enabled.");
    }

    public void reloadModule() {
        reloadConfig();
        grades.reload();
    }
}
