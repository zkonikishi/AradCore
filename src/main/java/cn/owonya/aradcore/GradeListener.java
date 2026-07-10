package cn.owonya.aradcore;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

final class GradeListener implements Listener {
    private final AradCorePlugin plugin;
    private final GradeService grades;
    GradeListener(AradCorePlugin plugin, GradeService grades) { this.plugin = plugin; this.grades = grades; }

    @EventHandler(priority = EventPriority.MONITOR) public void onJoin(PlayerJoinEvent e) {
        if (plugin.getConfig().getBoolean("equipment-grade.scan.on-join", true)) later(e.getPlayer());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void onPickup(PlayerAttemptPickupItemEvent e) { later(e.getPlayer()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void onClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p) later(p);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void onDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player p) later(p);
    }
    private void later(Player player) {
        if (!plugin.getConfig().getBoolean("equipment-grade.scan.on-inventory-events", true) && player.isOnline()) return;
        long delay = plugin.getConfig().getLong("equipment-grade.scan.delay-ticks", 2);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> scan(player), delay);
    }
    private void scan(Player player) {
        if (!player.isOnline()) return;
        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;
        for (int i = 0; i < contents.length; i++) {
            ItemStack before = contents[i];
            ItemStack after = grades.ensure(before);
            if (after != before) { contents[i] = after; changed = true; }
        }
        if (changed) player.getInventory().setContents(contents);
    }
}
