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

    /** {@code stack.setTag(tag)}; null entfernt das Tag. Auf 1.21.1 wird {@code tag} kopiert. */
    public static void set(ItemStack stack, CompoundTag tag) {
        //? if < 1.21.1 {
        stack.setTag(tag);
        //?} else {
        /*if (tag == null) {
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
        *///?}
    }

    /** {@code stack.getTagElement(key)} (null, wenn nicht vorhanden; Schreibzugriff wie {@link #tag}). */
    public static CompoundTag element(ItemStack stack, String key) {
        //? if < 1.21.1 {
        return stack.getTagElement(key);
        //?} else {
        /*CompoundTag t = read(stack);
        if (t == null || !t.contains(key, 10)) return null;
        return live(stack).getCompound(key);
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

    /** {@code ItemStack.isSameItemSameTags(a, b)} (1.21.1: gleiche Komponenten). */
    public static boolean sameItemSameTags(ItemStack a, ItemStack b) {
        return PlatformHooks.isSameItemSameTags(a, b);
    }

    /**
     * {@code ItemStack.of(tag)}. Auf 1.21.1 mit bestmoeglichem Registry-Provider
     * ({@link PlatformHooks#bestEffortProvider}); ohne Provider -> {@link ItemStack#EMPTY}.
     */
    public static ItemStack parse(CompoundTag tag) {
        //? if < 1.21.1 {
        return ItemStack.of(tag);
        //?} else {
        /*net.minecraft.core.HolderLookup.Provider p = PlatformHooks.bestEffortProvider();
        if (p == null || tag == null) return ItemStack.EMPTY;
        return ItemStack.parseOptional(p, tag);
        *///?}
    }

    /**
     * {@code stack.save(tag)} - gibt {@code tag} zurueck. Auf 1.21.1 wird ein leerer Stack wie
     * {@code saveOptional} behandelt (Tag bleibt leer, {@link #parse} liefert daraus EMPTY).
     */
    public static CompoundTag save(ItemStack stack, CompoundTag tag) {
        //? if < 1.21.1 {
        return stack.save(tag);
        //?} else {
        /*if (stack.isEmpty()) return tag;
        net.minecraft.core.HolderLookup.Provider p = PlatformHooks.bestEffortProvider();
        if (p == null) return tag;
        return (CompoundTag) stack.save(p, tag);
        *///?}
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
