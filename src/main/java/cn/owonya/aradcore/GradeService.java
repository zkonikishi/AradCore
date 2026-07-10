package cn.owonya.aradcore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

final class GradeService {
    private final AradCorePlugin plugin;
    private final NamespacedKey gradeKey, versionKey, loreKey;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private MythicNbtBridge nbt;
    private List<String> scaledTags = List.of(), identityTags = List.of();
    private List<GradeBand> bands = List.of();
    private boolean enabled, requireMmo, loreEnabled;
    private int min, max;
    private String loreTemplate;

    GradeService(AradCorePlugin plugin) {
        this.plugin = plugin;
        gradeKey = new NamespacedKey(plugin, "grade_percent");
        versionKey = new NamespacedKey(plugin, "grade_schema");
        loreKey = new NamespacedKey(plugin, "grade_lore");
        reload();
    }

    void reload() {
        enabled = plugin.getConfig().getBoolean("equipment-grade.enabled", true);
        requireMmo = plugin.getConfig().getBoolean("equipment-grade.require-mmoitems-tags", true);
        min = plugin.getConfig().getInt("equipment-grade.min-percent", 1);
        max = plugin.getConfig().getInt("equipment-grade.max-percent", 100);
        scaledTags = List.copyOf(plugin.getConfig().getStringList("equipment-grade.scaled-tags"));
        identityTags = List.copyOf(plugin.getConfig().getStringList("equipment-grade.identity-tags"));
        loreEnabled = plugin.getConfig().getBoolean("equipment-grade.lore.enabled", true);
        loreTemplate = plugin.getConfig().getString("equipment-grade.lore.line", "<gray>装备品级: <percent>%</gray>");
        List<GradeBand> loaded = new ArrayList<>();
        for (Map<?, ?> row : plugin.getConfig().getMapList("equipment-grade.distribution")) {
            loaded.add(new GradeBand(asInt(row.get("min")), asInt(row.get("max")), asInt(row.get("weight"))));
        }
        bands = loaded.isEmpty() ? List.of(new GradeBand(min, max, 1)) : List.copyOf(loaded);
        try { nbt = new MythicNbtBridge(); }
        catch (ReflectiveOperationException ex) {
            nbt = null;
            plugin.getLogger().warning("MythicLib NBT API unavailable; grade processing is paused: " + ex.getMessage());
        }
    }

    ItemStack ensure(ItemStack input) {
        if (!enabled || input == null || input.getType().isAir() || nbt == null) return input;
        try {
            Object wrapped = nbt.wrap(input);
            if (requireMmo && identityTags.stream().noneMatch(t -> safeHas(wrapped, t))) return input;
            ItemMeta meta = input.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            Integer existing = pdc.get(gradeKey, PersistentDataType.INTEGER);
            if (existing != null) return input;

            int percent = roll();
            // Immutable base values are namespaced per stat. Regrades always use these values.
            for (String tag : scaledTags) {
                if (!nbt.has(wrapped, tag)) continue;
                double base = nbt.number(wrapped, tag);
                pdc.set(baseKey(tag), PersistentDataType.DOUBLE, base);
                nbt.number(wrapped, tag, scaled(base, percent));
            }
            pdc.set(gradeKey, PersistentDataType.INTEGER, percent);
            pdc.set(versionKey, PersistentDataType.INTEGER, 1);
            ItemStack result = nbt.item(wrapped);
            result.setItemMeta(meta);
            return updateLore(result, percent);
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning("Could not grade item: " + ex.getMessage());
            return input;
        }
    }

    ItemStack regrade(ItemStack input, int percent) {
        if (input == null || nbt == null) return input;
        percent = Math.max(min, Math.min(max, percent));
        try {
            Object wrapped = nbt.wrap(input);
            ItemMeta meta = input.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            for (String tag : scaledTags) {
                Double base = pdc.get(baseKey(tag), PersistentDataType.DOUBLE);
                if (base == null && nbt.has(wrapped, tag)) {
                    base = nbt.number(wrapped, tag);
                    pdc.set(baseKey(tag), PersistentDataType.DOUBLE, base);
                }
                if (base != null) nbt.number(wrapped, tag, scaled(base, percent));
            }
            pdc.set(gradeKey, PersistentDataType.INTEGER, percent);
            pdc.set(versionKey, PersistentDataType.INTEGER, 1);
            ItemStack result = nbt.item(wrapped);
            result.setItemMeta(meta);
            return updateLore(result, percent);
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning("Could not regrade item: " + ex.getMessage());
            return input;
        }
    }

    Integer grade(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(gradeKey, PersistentDataType.INTEGER);
    }

    int randomGrade() { return roll(); }

    Map<String, Object> inspect(ItemStack item) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("grade", grade(item));
        if (item == null || nbt == null) return out;
        try {
            Object wrapped = nbt.wrap(item);
            out.put("tags", new TreeSet<>(nbt.tags(wrapped)));
            Map<String, Double> values = new LinkedHashMap<>();
            for (String tag : scaledTags) if (nbt.has(wrapped, tag)) values.put(tag, nbt.number(wrapped, tag));
            out.put("scaled-values", values);
        } catch (ReflectiveOperationException ex) { out.put("error", ex.getMessage()); }
        return out;
    }

    private ItemStack updateLore(ItemStack item, int percent) {
        if (!loreEnabled || item == null) return item;
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        // Our line is always first; replacing it makes the operation idempotent.
        Component line = mini.deserialize(loreTemplate.replace("<percent>", Integer.toString(percent)).replace("<color>", "<" + color(percent) + ">"));
        if (meta.getPersistentDataContainer().has(loreKey, PersistentDataType.BYTE) && !lore.isEmpty()) lore.set(0, line);
        else lore.add(0, line);
        meta.getPersistentDataContainer().set(loreKey, PersistentDataType.BYTE, (byte) 1);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String color(int p) {
        String key = p >= 95 ? "perfect" : p >= 80 ? "excellent" : p >= 60 ? "high" : p >= 40 ? "medium" : "low";
        return plugin.getConfig().getString("equipment-grade.lore.colors." + key, "#FFFFFF");
    }
    private boolean safeHas(Object wrapped, String tag) { try { return nbt.has(wrapped, tag); } catch (Exception ignored) { return false; } }
    private NamespacedKey baseKey(String tag) { return new NamespacedKey(plugin, "base_" + tag.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_")); }
    private double scaled(double base, int percent) { return BigDecimal.valueOf(base * percent / 100d).setScale(4, RoundingMode.HALF_UP).doubleValue(); }
    private static int asInt(Object value) { return value instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(value)); }
    private int roll() {
        int total = bands.stream().mapToInt(GradeBand::weight).sum();
        int hit = ThreadLocalRandom.current().nextInt(Math.max(1, total));
        for (GradeBand band : bands) {
            hit -= band.weight();
            if (hit < 0) return ThreadLocalRandom.current().nextInt(band.min(), band.max() + 1);
        }
        return min;
    }
}
