package com.hbm_m.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Versionsfassade fuer das Item-NBT (1.20.1: {@code ItemStack#getTag} usw.,
 * 1.21.1: Datenkomponente {@code minecraft:custom_data}).
 *
 * <p>Die 1.20.1-Seite jeder Methode ist exakt der bisherige Vanilla-Aufruf. Mechanische Umstellung:
 * <pre>
 *   stack.hasTag()                    -> StackNbt.has(stack)
 *   stack.getTag()   (nur lesen)      -> StackNbt.read(stack)
 *   stack.getTag()   (wird veraendert)-> StackNbt.tag(stack)
 *   stack.getOrCreateTag()            -> StackNbt.orCreate(stack)
 *   stack.setTag(t)                   -> StackNbt.set(stack, t)
 *   stack.removeTagKey(k)             -> StackNbt.removeKey(stack, k)
 *   stack.getTagElement(k)            -> StackNbt.element(stack, k)
 *   stack.getOrCreateTagElement(k)    -> StackNbt.orCreateElement(stack, k)
 *   ItemStack.isSameItemSameTags(a,b) -> StackNbt.sameItemSameTags(a, b)
 *   ItemStack.of(tag)                 -> StackNbt.parse(tag)
 *   stack.save(tag)                   -> StackNbt.save(stack, tag)
 * </pre>
 *
 * <p><b>"Lebende" Tags auf 1.21.1</b> ({@link #tag}, {@link #orCreate}, {@link #orCreateElement}):
 * jeder Aufruf legt eine frische Kopie des custom_data-Tags an, setzt sie als neue Komponente in den
 * Stack und gibt genau dieses Tag-Objekt zurueck. Aenderungen daran landen damit im Stack wie auf
 * 1.20.1. Einschraenkung: das Tag bleibt nur so lange "lebendig", bis jemand die Komponente des
 * Stacks ersetzt (anderer {@code tag/orCreate/set}-Aufruf auf DEMSELBEN Stack) - also Tag holen,
 * sofort beschreiben, nicht ueber weitere Aufrufe hinweg festhalten.
 *
 * <p>{@link #read} ist auf 1.21.1 der interne Tag OHNE Kopie - niemals veraendern.
 */
public final class StackNbt {
    private StackNbt() {}

    /** {@code stack.hasTag()} */
    public static boolean has(ItemStack stack) {
        //? if < 1.21.1 {
        return stack.hasTag();
        //?} else {
        /*return stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        *///?}
    }

    /**
     * {@code stack.getTag()} nur zum Lesen (null, wenn kein Tag). Auf 1.21.1 der interne
     * Tag der Komponente ohne Kopie: NICHT veraendern.
     */
    public static CompoundTag read(ItemStack stack) {
        //? if < 1.21.1 {
        return stack.getTag();
        //?} else {
        /*net.minecraft.world.item.component.CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data == null ? null : data.getUnsafe();
        *///?}
    }

    /** {@code stack.getTag()} mit Schreibzugriff (null, wenn kein Tag). Siehe Klassenkommentar. */
    public static CompoundTag tag(ItemStack stack) {
        //? if < 1.21.1 {
        return stack.getTag();
        //?} else {
        /*if (!stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) return null;
        return live(stack);
        *///?}
    }

    /** {@code stack.getOrCreateTag()} - Aenderungen bleiben erhalten. Siehe Klassenkommentar. */
    public static CompoundTag orCreate(ItemStack stack) {
        //? if < 1.21.1 {
        return stack.getOrCreateTag();
        //?} else {
        /*return live(stack);
        *///?}
    }

    /**
     * Read-modify-write: изменения сохраняются на ОБЕИХ версиях, паритет пустых тегов —
     * editor оставил тег пустым → тег удаляется со стека (на 1.21.1 это делает
     * {@link #set}; на 1.20.1 вручную {@code setTag(null)}).
     */
    public static void edit(ItemStack stack, java.util.function.Consumer<CompoundTag> editor) {
        if (stack.isEmpty()) return;
        //? if < 1.21.1 {
        CompoundTag tag = stack.getOrCreateTag();
        editor.accept(tag);
        if (tag.isEmpty()) stack.setTag(null);
        //?} else {
        /*// CustomData.update копирует тег РОВНО ОДИН раз (как в removeKey); copyTag()+set() делали двойное клонирование.
        net.minecraft.world.item.component.CustomData.update(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, editor);
        CompoundTag remaining = read(stack);
        if (remaining != null && remaining.isEmpty()) {
            stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        }
        *///?}
    }

    //? if >= 1.21.1 {
    /*private static CompoundTag live(ItemStack stack) {
        // ItemStack.EMPTY ist ein Singleton: nichts hineinschreiben, losgeloestes Tag zurueckgeben.
        if (stack.isEmpty()) return new CompoundTag();
        final CompoundTag[] out = new CompoundTag[1];
        // CustomData#update kopiert den Tag, reicht die Kopie an den Consumer und kapselt GENAU diese
        // Kopie ohne weitere Kopie in die neue Komponente -> das Objekt ist "lebendig".
        net.minecraft.world.item.component.CustomData data = stack.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).update(t -> out[0] = t);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, data);
        return out[0];
    }
    *///?}

    /** {@code stack.setTag(tag)}; null oder leeres Tag entfernt die Komponente. */
    public static void set(ItemStack stack, CompoundTag tag) {
        //? if < 1.21.1 {
        stack.setTag(tag);
        //?} else {
        /*if (tag == null || tag.isEmpty()) {
            stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        } else {
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(tag));
        }
        *///?}
    }

    /** {@code stack.removeTagKey(key)} */
    public static void removeKey(ItemStack stack, String key) {
        //? if < 1.21.1 {
        stack.removeTagKey(key);
        //?} else {
        /*if (!stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) return;
        net.minecraft.world.item.component.CustomData.update(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, t -> t.remove(key));
        CompoundTag remaining = read(stack);
        if (remaining != null && remaining.isEmpty()) {
            stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        }
        *///?}
    }

    /**
     * Скалярный NBT-тег в строку БЕЗ суффикса типа: {@code getAsString()} на NumericTag
     * даёт "1b"/"1s"/"1L" (StringTagVisitor), что не парсится BlockItemStateProperties.
     */
    public static String scalarToString(net.minecraft.nbt.Tag tag) {
        if (tag == null) return "";
        if (tag instanceof net.minecraft.nbt.NumericTag num) return num.getAsNumber().toString();
        return tag.getAsString();
    }

    /** {@code stack.getTagElement(key)} (null, если не существует). Read-only: стек НЕ мутируется
     * (на 1.21.1 не вешает CUSTOM_DATA и не клонирует NBT). Возвращённый тег не мутировать —
     * для записи используйте {@link #orCreateElement}. */
    public static CompoundTag element(ItemStack stack, String key) {
        //? if < 1.21.1 {
        return stack.getTagElement(key);
        //?} else {
        /*CompoundTag t = read(stack);
        if (t == null || !t.contains(key, 10)) return null;
        return t.getCompound(key);
        *///?}
    }

    /** {@code stack.getOrCreateTagElement(key)} - Aenderungen bleiben erhalten (siehe Klassenkommentar). */
    public static CompoundTag orCreateElement(ItemStack stack, String key) {
        //? if < 1.21.1 {
        return stack.getOrCreateTagElement(key);
        //?} else {
        /*CompoundTag t = live(stack);
        if (!t.contains(key, 10)) t.put(key, new CompoundTag());
        return t.getCompound(key);
        *///?}
    }

    /** {@code ItemStack.isSameItemSameTags(a, b)} (1.21.1: gleiche Komponenten). Единая точка истины. */
    public static boolean sameItemSameTags(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() || b.isEmpty()) return false;
        if (a.getItem() != b.getItem()) return false;
        //? if < 1.21.1 {
        CompoundTag tagA = a.getTag();
        CompoundTag tagB = b.getTag();
        boolean emptyA = tagA == null || tagA.isEmpty();
        boolean emptyB = tagB == null || tagB.isEmpty();
        if (emptyA && emptyB) return true;
        if (emptyA || emptyB) return false;
        return tagA.equals(tagB);
        //?} else {
        /*return ItemStack.isSameItemSameComponents(a, b)
                || (isEmptyCustomData(a) && isEmptyCustomData(b) && componentsEqualExceptCustomData(a, b));
        *///?}
    }

    //? if >= 1.21.1 {
    /*// Ваниль сравнивает компоненты строго: стек с пустым CUSTOM_DATA={} и свежий стек
    // без компонента НЕ стакаются. Платформа больше не оставляет пустых CustomData
    // (set/edit/removeKey чистят), но данные могли прийти извне — нормализуем.
    private static boolean isEmptyCustomData(ItemStack stack) {
        var d = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return d == null || d.isEmpty();
    }

    private static boolean componentsEqualExceptCustomData(ItemStack a, ItemStack b) {
        var ca = a.getComponents();
        var cb = b.getComponents();
        int sizeA = ca.size() - (ca.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA) ? 1 : 0);
        int sizeB = cb.size() - (cb.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA) ? 1 : 0);
        if (sizeA != sizeB) return false;
        for (var key : ca.keySet()) {
            if (key == net.minecraft.core.component.DataComponents.CUSTOM_DATA) continue;
            if (!java.util.Objects.equals(ca.get(key), cb.get(key))) return false;
        }
        return true;
    }
    *///?}

    /**
     * {@code ItemStack.of(tag)} с явным провайдером (на 1.21.1 обязателен; null → best-effort).
     * Невалидный/пустой тег → {@link ItemStack#EMPTY}.
     */
    public static ItemStack parse(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        if (tag == null || tag.isEmpty()) return ItemStack.EMPTY;
        //? if < 1.21.1 {
        return ItemStack.of(tag);
        //?} else {
        /*net.minecraft.core.HolderLookup.Provider p = provider != null ? provider : PlatformHooks.bestEffortProvider();
        if (p == null) return ItemStack.EMPTY;
        return ItemStack.parseOptional(p, tag);
        *///?}
    }

    /**
     * {@code stack.save(tag)} - gibt {@code tag} zurueck. Auf 1.21.1 wird ein leerer Stack wie
     * {@code saveOptional} behandelt (Tag bleibt leer, {@link #parse} liefert daraus EMPTY).
     * Modifiziert {@code tag} in-place auf beiden Versionen.
     */
    public static CompoundTag save(ItemStack stack, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        if (tag == null) tag = new CompoundTag();
        if (stack == null || stack.isEmpty()) return tag;
        //? if < 1.21.1 {
        return stack.save(tag);
        //?} else {
        /*net.minecraft.core.HolderLookup.Provider p = provider != null ? provider : PlatformHooks.bestEffortProvider();
        if (p == null) return tag;
        // ItemStack.save(provider, prefix) encodes through NbtOps, which MERGES into a NEW compound
        // and leaves the prefix untouched. Keep the 1.20.1 contract: fill `tag` in-place.
        CompoundTag saved = (CompoundTag) stack.save(p, tag);
        if (saved != tag) {
            tag.merge(saved);
        }
        return tag;
        *///?}
    }

    /** Сохраняет {@code stack} в НОВЫЙ {@link CompoundTag} (бывший safeItemSave). */
    public static CompoundTag saveNew(ItemStack stack, net.minecraft.core.HolderLookup.Provider provider) {
        return save(stack, new CompoundTag(), provider);
    }

    /**
     * ВЛАДЕЕМАЯ копия custom-NBT (null, если тега нет). На 1.20.1 — настоящий
     * {@code tag.copy()} (паритет с 1.21.1 {@code copyTag()}); для БЕСПЛАТНОГО чтения
     * без глубокого копирования — {@link #read}, для мутации — {@link #edit}.
     */
    public static CompoundTag copy(ItemStack stack) {
        //? if < 1.21.1 {
        CompoundTag tag = stack.getTag();
        return tag == null ? null : tag.copy();
        //?} else {
        /*net.minecraft.world.item.component.CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
        *///?}
    }

    // =====================================================================================
    //  Типизированные хелперы (COMMON — read-modify-write через edit/read, без gating).
    //  Цель: линейный стиль {@code stack.getOrCreateTag().putX(...)} без лямбд у call-сайтов.
    // =====================================================================================

    /** {@code stack.getTag().getInt(key)} с защитой от null (0 если тега/ключа нет). */
    public static int getInt(ItemStack stack, String key) {
        CompoundTag t = read(stack);
        return t == null ? 0 : t.getInt(key);
    }

    /** {@code stack.getTag().getLong(key)} с защитой от null. */
    public static long getLong(ItemStack stack, String key) {
        CompoundTag t = read(stack);
        return t == null ? 0L : t.getLong(key);
    }

    /** {@code stack.getTag().getBoolean(key)} с защитой от null. */
    public static boolean getBoolean(ItemStack stack, String key) {
        CompoundTag t = read(stack);
        return t != null && t.getBoolean(key);
    }

    /** {@code stack.getTag().getString(key)} с защитой от null. */
    public static String getString(ItemStack stack, String key) {
        CompoundTag t = read(stack);
        return t == null ? "" : t.getString(key);
    }

    /** {@code stack.getTag().getCompound(key)} с защитой от null (пустой тег, если нет). */
    public static CompoundTag getCompound(ItemStack stack, String key) {
        CompoundTag t = read(stack);
        return t == null ? new CompoundTag() : t.getCompound(key);
    }

    /** {@code stack.getOrCreateTag().putInt(key, val)} — read-modify-write, сохраняется на обеих версиях. */
    public static void putInt(ItemStack stack, String key, int val) { edit(stack, t -> t.putInt(key, val)); }

    /** {@code stack.getOrCreateTag().putLong(key, val)}. */
    public static void putLong(ItemStack stack, String key, long val) { edit(stack, t -> t.putLong(key, val)); }

    /** {@code stack.getOrCreateTag().putBoolean(key, val)}. */
    public static void putBoolean(ItemStack stack, String key, boolean val) { edit(stack, t -> t.putBoolean(key, val)); }

    /** {@code stack.getOrCreateTag().putString(key, val)}. */
    public static void putString(ItemStack stack, String key, String val) { edit(stack, t -> t.putString(key, val)); }

    /** {@code stack.getOrCreateTag().put(key, val)} для вложенного CompoundTag. */
    public static void put(ItemStack stack, String key, CompoundTag val) { edit(stack, t -> t.put(key, val)); }

    /** {@code stack.hasTag() && stack.getTag().contains(key)}. */
    public static boolean contains(ItemStack stack, String key) {
        CompoundTag t = read(stack);
        return t != null && t.contains(key);
    }

    /**
     * Probabilistic 64-bit fingerprint of an ItemStack for cheap per-tick input-signature
     * comparisons (MachineModuleBase input gating). Identity-hash Item — валиден только
     * within-run; для персистентных сравнений невалиден. ALWAYS distinguished: empty-vs-
     * nonempty, item identity, count, DAMAGE и NBT/CUSTOM_DATA (1.21.1: весь компонентный
     * патч). Коллизия (~2^-64) — bounded risk (stale cached decision).
     */
    public static long fingerprint(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0L;
        long h = stack.getItem().hashCode() * 31L + stack.getCount();
        h = h * 31L + stack.getDamageValue();
        //? if < 1.21.1 {
        h = h * 31L + (stack.getTag() == null ? 0L : stack.getTag().hashCode());
        //?} else {
        /*// Компонентный патч вместо голого CUSTOM_DATA: зачарования/имя/прочность живут в отдельных DataComponents — иначе fingerprint их не различал.
        h = h * 31L + stack.getComponentsPatch().hashCode();
        *///?}
        return h;
    }

    /**
     * {@code ItemStack.of(tag)}. Auf 1.21.1 mit bestmoeglichem Registry-Provider
     * ({@link PlatformHooks#bestEffortProvider}); ohne Provider -> {@link ItemStack#EMPTY}.
     */
    public static ItemStack parse(CompoundTag tag) {
        return parse(tag, null);
    }

    /**
     * {@code stack.save(tag)} - gibt {@code tag} zurueck. Best-effort провайдер (1.21.1).
     */
    public static CompoundTag save(ItemStack stack, CompoundTag tag) {
        return save(stack, tag, null);
    }

    /** {@code stack.hasCustomHoverName()} */
    public static boolean hasCustomName(ItemStack stack) {
        //? if < 1.21.1 {
        return stack.hasCustomHoverName();
        //?} else {
        /*return stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        *///?}
    }

    /** {@code stack.setHoverName(name)} - gibt den Stack zurueck wie das Original. */
    public static ItemStack setCustomName(ItemStack stack, Component name) {
        //? if < 1.21.1 {
        return stack.setHoverName(name);
        //?} else {
        /*if (name == null) stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        else stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, name);
        return stack;
        *///?}
    }
}
