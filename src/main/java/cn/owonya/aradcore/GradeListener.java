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
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class GradeListener implements Listener {
    private final AradCorePlugin plugin;
    private final GradeService grades;
    private final Map<UUID, BukkitTask> pendingScans = new HashMap<>();

    GradeListener(AradCorePlugin plugin, GradeService grades) {
        this.plugin = plugin;
        this.grades = grades;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getConfig().getBoolean("equipment-grade.scan.on-join", true)) scheduleScan(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        if (inventoryEventsEnabled()) scheduleScan(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (inventoryEventsEnabled() && event.getWhoClicked() instanceof Player player) scheduleScan(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (inventoryEventsEnabled() && event.getWhoClicked() instanceof Player player) scheduleScan(player);
    }

    void cancelPending() {
        pendingScans.values().forEach(BukkitTask::cancel);
        pendingScans.clear();
    }

    private boolean inventoryEventsEnabled() {
        return plugin.getConfig().getBoolean("equipment-grade.scan.on-inventory-events", true);
    }

    private void scheduleScan(Player player) {
        if (!grades.isEnabled()) return;
        BukkitTask previous = pendingScans.remove(player.getUniqueId());
        if (previous != null) previous.cancel();

        long delay = Math.max(1L, plugin.getConfig().getLong("equipment-grade.scan.delay-ticks", 2));
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingScans.remove(player.getUniqueId());
            scan(player);
        }, delay);
        pendingScans.put(player.getUniqueId(), task);
    }

    private void scan(Player player) {
        if (!player.isOnline() || !grades.isEnabled()) return;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack before = contents[slot];
            ItemStack after = grades.ensure(before);
            if (after != before) player.getInventory().setItem(slot, after);
        }
    }
}
