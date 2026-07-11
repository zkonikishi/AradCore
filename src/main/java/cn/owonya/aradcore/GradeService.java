package cn.owonya.aradcore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

final class GradeService {
    private static final int SCHEMA_VERSION = 1;

    private final AradCorePlugin plugin;
    private final NamespacedKey gradeKey;
    private final NamespacedKey versionKey;
    private final NamespacedKey loreKey;
    private final NamespacedKey loreIndexKey;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
    private final Set<String> warnedMessages = new HashSet<>();

    private MythicNbtBridge nbt;
    private List<String> scaledTags = List.of();
    private List<String> identityTags = List.of();
    private List<GradeBand> bands = List.of();
    private boolean enabled;
    private boolean requireMmo;
    private boolean loreEnabled;
    private int min;
    private int max;
    private String loreTemplate = "<gray>装备品级: <percent>%</gray>";
    private String lorePrefix = "装备品级:";

    GradeService(AradCorePlugin plugin) {
        this.plugin = plugin;
        gradeKey = new NamespacedKey(plugin, "grade_percent");
        versionKey = new NamespacedKey(plugin, "grade_schema");
        loreKey = new NamespacedKey(plugin, "grade_lore");
        loreIndexKey = new NamespacedKey(plugin, "grade_lore_index");
        reload();
    }

    void reload() {
        warnedMessages.clear();
        enabled = false;
        nbt = null;

        try {
            boolean configuredEnabled = plugin.getConfig().getBoolean("equipment-grade.enabled", true);
            boolean configuredRequireMmo = plugin.getConfig().getBoolean("equipment-grade.require-mmoitems-tags", true);
            int configuredMin = plugin.getConfig().getInt("equipment-grade.min-percent", 1);
            int configuredMax = plugin.getConfig().getInt("equipment-grade.max-percent", 100);
            List<String> configuredScaledTags = distinctNonBlank(plugin.getConfig().getStringList("equipment-grade.scaled-tags"));
            List<String> configuredIdentityTags = distinctNonBlank(plugin.getConfig().getStringList("equipment-grade.identity-tags"));
            boolean configuredLoreEnabled = plugin.getConfig().getBoolean("equipment-grade.lore.enabled", true);
            String configuredLoreTemplate = plugin.getConfig().getString(
                    "equipment-grade.lore.line", "<gray>装备品级: <percent>%</gray>");

            validateRange(configuredMin, configuredMax);
            if (configuredRequireMmo && configuredIdentityTags.isEmpty()) {
                throw new IllegalArgumentException("identity-tags cannot be empty when MMOItems checks are enabled");
            }
            if (configuredScaledTags.isEmpty()) {
                throw new IllegalArgumentException("scaled-tags cannot be empty");
            }
            if (configuredLoreTemplate == null || configuredLoreTemplate.isBlank()) {
                throw new IllegalArgumentException("equipment-grade.lore.line cannot be blank");
            }

            List<GradeBand> configuredBands = parseBands(configuredMin, configuredMax);
            MythicNbtBridge configuredNbt = null;
            if (configuredEnabled) {
                configuredNbt = new MythicNbtBridge();
            }

            min = configuredMin;
            max = configuredMax;
            requireMmo = configuredRequireMmo;
            scaledTags = configuredScaledTags;
            identityTags = configuredIdentityTags;
            loreEnabled = configuredLoreEnabled;
            loreTemplate = configuredLoreTemplate;
            lorePrefix = extractLorePrefix(configuredLoreTemplate);
            bands = configuredBands;
            nbt = configuredNbt;
            enabled = configuredEnabled && configuredNbt != null;

            if (!enabled && configuredEnabled) {
                warnOnce("grade-disabled-api", "MythicLib NBT API unavailable; grade processing is paused.");
            }
        } catch (RuntimeException ex) {
            warnOnce("grade-invalid-config", "Invalid equipment-grade configuration; grade processing is paused: " + ex.getMessage());
            min = 1;
            max = 100;
            bands = List.of();
            scaledTags = List.of();
            identityTags = List.of();
        }
    }

    boolean isEnabled() {
        return enabled;
    }

    int minGrade() {
        return min;
    }

    int maxGrade() {
        return max;
    }

    String validationError(ItemStack item) {
        if (!enabled || nbt == null) return "品级模块当前不可用。";
        if (item == null || item.getType().isAir()) return "请先在主手拿一件物品。";
        if (!requireMmo) return null;
        try {
            Object wrapped = nbt.wrap(item.clone());
            for (String tag : identityTags) {
                if (!nbt.has(wrapped, tag)) return "手持物不是完整的 MMOItems 物品。";
            }
            return null;
        } catch (Exception ex) {
            return "无法读取 MMOItems 数据。";
        }
    }

    boolean canProcess(ItemStack item) {
        return validationError(item) == null;
    }

    ItemStack ensure(ItemStack input) {
        if (!canProcess(input)) return input;

        try {
            ItemMeta sourceMeta = input.getItemMeta();
            if (sourceMeta == null) return input;
            PersistentDataContainer sourcePdc = sourceMeta.getPersistentDataContainer();
            Integer existing = sourcePdc.get(gradeKey, PersistentDataType.INTEGER);
            if (existing != null) {
                Integer schema = sourcePdc.get(versionKey, PersistentDataType.INTEGER);
                Object existingWrapped = nbt.wrap(input.clone());
                if (schema == null || schema != SCHEMA_VERSION || existing < min || existing > max
                        || !hasCompleteBase(sourcePdc, existingWrapped)) {
                    warnOnce("incomplete-grade", "Found an outdated or incomplete AradCore grade record; use /aradcore migrate while holding the item.");
                }
                return input;
            }

            int percent = roll();
            ItemStack working = input.clone();
            Object wrapped = nbt.wrap(working);
            Map<String, Double> bases = captureBases(wrapped);
            if (bases.isEmpty()) {
                warnOnce("no-scalable-stats", "Skipped an MMOItems item because none of the configured numeric stats were present.");
                return input;
            }
            for (Map.Entry<String, Double> entry : bases.entrySet()) {
                nbt.number(wrapped, entry.getKey(), scaled(entry.getValue(), percent));
            }
            ItemStack result = nbt.item(wrapped);
            return applyManagedMeta(input, result, percent, bases);
        } catch (Exception ex) {
            warnOnce("ensure-failed", "Could not grade item; the original item was kept: " + message(ex));
            return input;
        }
    }

    ItemStack regrade(ItemStack input, int percent) {
        if (!canProcess(input)) return input;
        percent = Math.max(min, Math.min(max, percent));

        ItemStack managed = input;
        Integer existing = grade(input);
        if (existing == null) {
            managed = ensure(input);
            if (managed == input && grade(managed) == null) return input;
        }

        try {
            ItemMeta sourceMeta = managed.getItemMeta();
            if (sourceMeta == null) return input;
            PersistentDataContainer sourcePdc = sourceMeta.getPersistentDataContainer();
            Object wrapped = nbt.wrap(managed.clone());
            if (!hasCompleteBase(sourcePdc, wrapped)) {
                warnOnce("regrade-missing-base", "Refusing to regrade an item with missing immutable base values.");
                return input;
            }

            Map<String, Double> bases = readBases(sourcePdc, wrapped);
            for (Map.Entry<String, Double> entry : bases.entrySet()) {
                nbt.number(wrapped, entry.getKey(), scaled(entry.getValue(), percent));
            }
            ItemStack result = nbt.item(wrapped);
            return applyManagedMeta(managed, result, percent, bases);
        } catch (Exception ex) {
            warnOnce("regrade-failed", "Could not regrade item; the original item was kept: " + message(ex));
            return input;
        }
    }

    Integer grade(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(gradeKey, PersistentDataType.INTEGER);
    }

    Integer schema(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(versionKey, PersistentDataType.INTEGER);
    }

    int currentSchema() {
        return SCHEMA_VERSION;
    }

    int randomGrade() {
        return roll();
    }

    ItemStack migrate(ItemStack input) {
        if (!canProcess(input)) return input;
        Integer percent = grade(input);
        if (percent == null) return ensure(input);
        if (percent < min || percent > max) return input;
        try {
            ItemMeta meta = input.getItemMeta();
            if (meta == null) return input;
            Object wrapped = nbt.wrap(input.clone());
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (!hasCompleteBase(pdc, wrapped)) {
                warnOnce("migration-missing-base", "Migration refused because immutable base values are missing.");
                return input;
            }
            pdc.set(versionKey, PersistentDataType.INTEGER, SCHEMA_VERSION);
            input = input.clone();
            input.setItemMeta(meta);
            return updateLore(input, percent);
        } catch (Exception ex) {
            warnOnce("migration-failed", "Could not migrate item metadata: " + message(ex));
            return input;
        }
    }

    Map<String, Object> inspect(ItemStack item) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("grade", grade(item));
        out.put("schema", schema(item));
        out.put("enabled", enabled);
        if (item == null || item.getType().isAir() || nbt == null) return out;
        try {
            Object wrapped = nbt.wrap(item.clone());
            Set<String> tags = nbt.tags(wrapped);
            out.put("tags", tags == null ? Set.of() : new TreeSet<>(tags));
            Map<String, Double> values = new LinkedHashMap<>();
            for (String tag : scaledTags) {
                if (nbt.has(wrapped, tag)) values.put(tag, nbt.number(wrapped, tag));
            }
            out.put("scaled-values", values);
        } catch (Exception ex) {
            out.put("error", message(ex));
        }
        return out;
    }

    private ItemStack applyManagedMeta(ItemStack source, ItemStack result, int percent, Map<String, Double> bases) {
        if (result == null || result.getType().isAir()) return source;
        ItemMeta resultMeta = result.getItemMeta();
        if (resultMeta == null) return source;

        ItemMeta sourceMeta = source.getItemMeta();
        if (sourceMeta != null) {
            PersistentDataContainer sourcePdc = sourceMeta.getPersistentDataContainer();
            PersistentDataContainer targetPdc = resultMeta.getPersistentDataContainer();
            Byte loreMarker = sourcePdc.get(loreKey, PersistentDataType.BYTE);
            Integer loreIndex = sourcePdc.get(loreIndexKey, PersistentDataType.INTEGER);
            if (loreMarker != null) targetPdc.set(loreKey, PersistentDataType.BYTE, loreMarker);
            if (loreIndex != null) targetPdc.set(loreIndexKey, PersistentDataType.INTEGER, loreIndex);
        }

        PersistentDataContainer targetPdc = resultMeta.getPersistentDataContainer();
        for (Map.Entry<String, Double> entry : bases.entrySet()) {
            targetPdc.set(baseKey(entry.getKey()), PersistentDataType.DOUBLE, entry.getValue());
        }
        targetPdc.set(gradeKey, PersistentDataType.INTEGER, percent);
        targetPdc.set(versionKey, PersistentDataType.INTEGER, SCHEMA_VERSION);
        result.setItemMeta(resultMeta);
        return updateLore(result, percent);
    }

    private boolean hasCompleteBase(PersistentDataContainer pdc, Object wrapped) throws ReflectiveOperationException {
        for (String tag : scaledTags) {
            if (nbt.has(wrapped, tag) && pdc.get(baseKey(tag), PersistentDataType.DOUBLE) == null) return false;
        }
        return true;
    }

    private Map<String, Double> captureBases(Object wrapped) throws ReflectiveOperationException {
        Map<String, Double> bases = new LinkedHashMap<>();
        for (String tag : scaledTags) {
            if (!nbt.has(wrapped, tag)) continue;
            double value = nbt.number(wrapped, tag);
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite MMOItems value: " + tag);
            bases.put(tag, value);
        }
        return bases;
    }

    private Map<String, Double> readBases(PersistentDataContainer pdc, Object wrapped) throws ReflectiveOperationException {
        Map<String, Double> bases = new LinkedHashMap<>();
        for (String tag : scaledTags) {
            if (!nbt.has(wrapped, tag)) continue;
            Double value = pdc.get(baseKey(tag), PersistentDataType.DOUBLE);
            if (value == null || !Double.isFinite(value)) {
                throw new IllegalArgumentException("Missing or invalid base value: " + tag);
            }
            bases.put(tag, value);
        }
        return bases;
    }

    private ItemStack updateLore(ItemStack item, int percent) {
        if (!loreEnabled || item == null || item.getType().isAir()) return item;
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return item;
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            Component line = mini.deserialize(loreTemplate
                    .replace("<percent>", Integer.toString(percent))
                    .replace("<color>", "<" + color(percent) + ">"));

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            Integer storedIndex = pdc.get(loreIndexKey, PersistentDataType.INTEGER);
            int index = storedIndex == null ? -1 : storedIndex;
            if (index < 0 || index >= lore.size() || !isManagedLoreLine(lore.get(index))) {
                index = findManagedLoreLine(lore);
            }
            if (index >= 0) lore.set(index, line);
            else {
                lore.add(0, line);
                index = 0;
            }
            pdc.set(loreKey, PersistentDataType.BYTE, (byte) 1);
            pdc.set(loreIndexKey, PersistentDataType.INTEGER, index);
            meta.lore(lore);
            item.setItemMeta(meta);
        } catch (RuntimeException ex) {
            warnOnce("lore-failed", "Could not update grade Lore; the item values were kept: " + message(ex));
        }
        return item;
    }

    private boolean isManagedLoreLine(Component line) {
        String text = plain.serialize(line);
        return (!lorePrefix.isBlank() && text.startsWith(lorePrefix))
                || text.matches(".*装备品级\\s*[:：].*\\d+%.*");
    }

    private int findManagedLoreLine(List<Component> lore) {
        for (int i = 0; i < lore.size(); i++) if (isManagedLoreLine(lore.get(i))) return i;
        return -1;
    }

    private String color(int p) {
        String key = p >= 95 ? "perfect" : p >= 80 ? "excellent" : p >= 60 ? "high" : p >= 40 ? "medium" : "low";
        return plugin.getConfig().getString("equipment-grade.lore.colors." + key, "#FFFFFF");
    }

    private int roll() {
        if (bands.isEmpty()) return min;
        long total = 0;
        for (GradeBand band : bands) total = Math.addExact(total, band.weight());
        if (total <= 0) throw new IllegalStateException("grade distribution has no positive weight");
        long hit = ThreadLocalRandom.current().nextLong(total);
        for (GradeBand band : bands) {
            hit -= band.weight();
            if (hit < 0) return ThreadLocalRandom.current().nextInt(band.min(), band.max() + 1);
        }
        return min;
    }

    private void validateRange(int configuredMin, int configuredMax) {
        if (configuredMin < 1 || configuredMax > 100 || configuredMin > configuredMax) {
            throw new IllegalArgumentException("min/max-percent must be within 1..100 and min <= max");
        }
    }

    private List<GradeBand> parseBands(int configuredMin, int configuredMax) {
        List<GradeBand> loaded = new ArrayList<>();
        for (Map<?, ?> row : plugin.getConfig().getMapList("equipment-grade.distribution")) {
            if (row == null) throw new IllegalArgumentException("distribution contains a null row");
            int bandMin = asInt(row.get("min"));
            int bandMax = asInt(row.get("max"));
            int weight = asInt(row.get("weight"));
            if (bandMin < configuredMin || bandMax > configuredMax || bandMin > bandMax || weight <= 0) {
                throw new IllegalArgumentException("invalid grade distribution band: " + row);
            }
            loaded.add(new GradeBand(bandMin, bandMax, weight));
        }
        if (loaded.isEmpty()) return List.of(new GradeBand(configuredMin, configuredMax, 1));
        loaded.sort(Comparator.comparingInt(GradeBand::min));
        for (int i = 1; i < loaded.size(); i++) {
            if (loaded.get(i - 1).max() >= loaded.get(i).min()) {
                throw new IllegalArgumentException("overlapping grade distribution bands");
            }
        }
        long total = 0;
        for (GradeBand band : loaded) total = Math.addExact(total, band.weight());
        if (total <= 0) throw new IllegalArgumentException("grade distribution total weight must be positive");
        return List.copyOf(loaded);
    }

    private static List<String> distinctNonBlank(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static int asInt(Object value) {
        if (value == null) throw new IllegalArgumentException("missing integer in grade distribution");
        long number = value instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(value));
        if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) throw new IllegalArgumentException("integer out of range");
        return (int) number;
    }

    private static String extractLorePrefix(String template) {
        final String token = "__ARADCORE_PERCENT__";
        String stripped = template.replace("<percent>", token).replaceAll("<[^>]+>", "");
        int placeholder = stripped.indexOf(token);
        return placeholder >= 0 ? stripped.substring(0, placeholder) : "";
    }

    private static String message(Exception ex) {
        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private void warnOnce(String key, String message) {
        if (warnedMessages.add(key)) plugin.getLogger().warning(message);
    }

    private NamespacedKey baseKey(String tag) {
        return new NamespacedKey(plugin, "base_" + tag.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_"));
    }

    private double scaled(double base, int percent) {
        return GradeMath.scale(base, percent);
    }
}
