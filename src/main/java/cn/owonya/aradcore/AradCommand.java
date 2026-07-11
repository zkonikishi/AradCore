package cn.owonya.aradcore;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class AradCommand implements CommandExecutor, TabCompleter {
    private final AradCorePlugin plugin;
    private final GradeService grades;

    AradCommand(AradCorePlugin plugin, GradeService grades) {
        this.plugin = plugin;
        this.grades = grades;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e/aradcore inspect|roll|set <百分比>|migrate|reload");
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        String permission = switch (subcommand) {
            case "inspect" -> "aradcore.inspect";
            case "roll" -> "aradcore.grade.roll";
            case "set" -> "aradcore.grade.set";
            case "migrate" -> "aradcore.migrate";
            case "reload" -> "aradcore.reload";
            default -> "aradcore.admin";
        };
        if (!sender.hasPermission(permission) && !sender.hasPermission("aradcore.admin")) {
            sender.sendMessage("§c没有权限。");
            return true;
        }

        if (subcommand.equals("reload")) {
            plugin.reloadModule();
            sender.sendMessage(grades.isEnabled() ? "§aAradCore 配置已重载。" : "§e配置已重载，但品级模块处于停用状态，请检查控制台。" );
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令需要玩家手持物品。");
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (subcommand.equals("inspect")) {
            grades.inspect(hand).forEach((key, value) -> sender.sendMessage("§7" + key + ": §f" + value));
            return true;
        }

        String validationError = grades.validationError(hand);
        if (validationError != null) {
            sender.sendMessage("§c" + validationError);
            return true;
        }

        switch (subcommand) {
            case "roll" -> {
                ItemStack result = grades.regrade(grades.ensure(hand), grades.randomGrade());
                player.getInventory().setItemInMainHand(result);
                sender.sendMessage("§a已重新随机品级。");
            }
            case "set" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c用法: /aradcore set <" + grades.minGrade() + "-" + grades.maxGrade() + ">");
                    return true;
                }
                try {
                    int requested = Integer.parseInt(args[1]);
                    if (requested < grades.minGrade() || requested > grades.maxGrade()) {
                        sender.sendMessage("§c百分比必须在 " + grades.minGrade() + " 到 " + grades.maxGrade() + " 之间。");
                        return true;
                    }
                    ItemStack result = grades.regrade(grades.ensure(hand), requested);
                    player.getInventory().setItemInMainHand(result);
                    sender.sendMessage("§a品级已设置为 " + requested + "% 。");
                } catch (NumberFormatException ex) {
                    sender.sendMessage("§c百分比必须是整数。");
                }
            }
            case "migrate" -> {
                ItemStack result = grades.migrate(hand);
                player.getInventory().setItemInMainHand(result);
                Integer schema = grades.schema(result);
                if (schema != null && schema == grades.currentSchema()) sender.sendMessage("§a物品品级数据已迁移到 schema " + schema + "。" );
                else sender.sendMessage("§c迁移被拒绝，请先使用 /aradcore inspect 检查基础值。" );
            }
            default -> sender.sendMessage("§e/aradcore inspect|roll|set <百分比>|migrate|reload");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        List<String> available = new ArrayList<>();
        addIfAllowed(sender, available, "inspect", "aradcore.inspect");
        addIfAllowed(sender, available, "roll", "aradcore.grade.roll");
        addIfAllowed(sender, available, "set", "aradcore.grade.set");
        addIfAllowed(sender, available, "migrate", "aradcore.migrate");
        addIfAllowed(sender, available, "reload", "aradcore.reload");
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return available.stream().filter(value -> value.startsWith(prefix)).toList();
    }

    private void addIfAllowed(CommandSender sender, List<String> values, String value, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("aradcore.admin")) values.add(value);
    }
}
