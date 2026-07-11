package cn.owonya.aradcore;

import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

final class MythicNbtBridge {
    Object wrap(ItemStack item) {
        return NBTItem.get(item);
    }

    boolean has(Object nbt, String key) {
        return ((NBTItem) nbt).hasTag(key);
    }

    double number(Object nbt, String key) {
        return ((NBTItem) nbt).getDouble(key);
    }

    void number(Object nbt, String key, double value) {
        ((NBTItem) nbt).setDouble(key, value);
    }

    ItemStack item(Object nbt) {
        return ((NBTItem) nbt).toItem();
    }

    Set<String> tags(Object nbt) {
        return ((NBTItem) nbt).getTags();
    }
}
