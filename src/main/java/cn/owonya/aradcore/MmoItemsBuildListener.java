package cn.owonya.aradcore;

import net.Indyuce.mmoitems.api.event.ItemBuildEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

final class MmoItemsBuildListener implements Listener {
    private final GradeService grades;

    MmoItemsBuildListener(GradeService grades) {
        this.grades = grades;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemBuild(ItemBuildEvent event) {
        if (!grades.isEnabled()) return;
        ItemStack original = event.getItemStack();
        ItemStack graded = grades.ensure(original);
        if (graded != original) event.setItemStack(graded);
    }
}
