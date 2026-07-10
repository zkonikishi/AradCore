package cn.owonya.aradcore;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;

final class AradCommand implements CommandExecutor, TabCompleter {
    private final AradCorePlugin plugin;
    private final GradeService grades;
    AradCommand(AradCorePlugin plugin, GradeService grades) { this.plugin = plugin; this.grades = grades; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aradcore.admin")) { sender.sendMessage("§c没有权限。"); return true; }
        if (args.length == 0) { sender.sendMessage("§e/aradcore inspect|roll|set <1-100>|reload"); return true; }
        if (args[0].equalsIgnoreCase("reload")) { plugin.reloadModule(); sender.sendMessage("§aAradCore 配置已重载。"); return true; }
        if (!(sender instanceof Player player)) { sender.sendMessage("§c此命令需要玩家手持物品。"); return true; }
        ItemStack hand = player.getInventory().getItemInMainHand();
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "inspect" -> grades.inspect(hand).forEach((k, v) -> sender.sendMessage("§7" + k + ": §f" + v));
            case "roll" -> { player.getInventory().setItemInMainHand(grades.regrade(grades.ensure(hand), grades.randomGrade())); sender.sendMessage("§a已重新随机品级。"); }
            case "set" -> {
                if (args.length < 2) { sender.sendMessage("§c用法: /aradcore set <1-100>"); return true; }
                try { player.getInventory().setItemInMainHand(grades.regrade(grades.ensure(hand), Integer.parseInt(args[1]))); sender.sendMessage("§a品级已设置。"); }
                catch (NumberFormatException ex) { sender.sendMessage("§c百分比必须是数字。"); }
            }
            default -> sender.sendMessage("§e/aradcore inspect|roll|set <1-100>|reload");
        }
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? List.of("inspect", "roll", "set", "reload") : List.of();
    }
}
