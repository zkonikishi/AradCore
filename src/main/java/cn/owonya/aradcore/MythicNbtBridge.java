package cn.owonya.aradcore;

import org.bukkit.inventory.ItemStack;
import java.lang.reflect.Method;
import java.util.Set;

/** Reflection keeps AradCore buildable while allowing MythicLib dev versions to vary. */
final class MythicNbtBridge {
    private final Method get, hasTag, getDouble, setDouble, toItem, getTags;

    MythicNbtBridge() throws ReflectiveOperationException {
        Class<?> nbt = Class.forName("io.lumine.mythic.lib.api.item.NBTItem");
        get = nbt.getMethod("get", ItemStack.class);
        hasTag = nbt.getMethod("hasTag", String.class);
        getDouble = nbt.getMethod("getDouble", String.class);
        setDouble = nbt.getMethod("setDouble", String.class, double.class);
        toItem = nbt.getMethod("toItem");
        getTags = nbt.getMethod("getTags");
    }

    Object wrap(ItemStack item) throws ReflectiveOperationException { return get.invoke(null, item); }
    boolean has(Object nbt, String tag) throws ReflectiveOperationException { return (boolean) hasTag.invoke(nbt, tag); }
    double number(Object nbt, String tag) throws ReflectiveOperationException { return ((Number) getDouble.invoke(nbt, tag)).doubleValue(); }
    void number(Object nbt, String tag, double value) throws ReflectiveOperationException { setDouble.invoke(nbt, tag, value); }
    ItemStack item(Object nbt) throws ReflectiveOperationException { return (ItemStack) toItem.invoke(nbt); }
    @SuppressWarnings("unchecked") Set<String> tags(Object nbt) throws ReflectiveOperationException { return (Set<String>) getTags.invoke(nbt); }
}
