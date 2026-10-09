package com.hbm_m.platform;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Blockentitaetsdaten am Gegenstand (Phase C, P3).
 * 1.20.1: Unter-Tag {@code BlockEntityTag} im Item-NBT (Vanilla-BlockItem laedt ihn beim Setzen).
 * 1.21.1: Komponente {@code DataComponents.BLOCK_ENTITY_DATA} (mit "id"; Vanilla-BlockItem laedt sie beim Setzen).
 * Alte 1.21.1-Stacks mit {@code BlockEntityTag} in CUSTOM_DATA werden beim Lesen weiter erkannt.
 */
public final class BlockEntityItemData {
    private BlockEntityItemData() {}

    public static final String KEY = "BlockEntityTag";

    /**
     * BE-Daten oder {@code null}. 1.20.1: lebender Unter-Tag ({@code getTagElement}); 1.21.1: Kopie -
     * nach Aenderungen {@link #writeBack} aufrufen.
     */
    @Nullable
    public static CompoundTag read(ItemStack stack) {
        //? if < 1.21.1 {
        return stack.getTagElement(KEY);
        //?} else {
        /*net.minecraft.world.item.component.CustomData data = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        if (data != null) return data.copyTag();
        CompoundTag custom = StackNbt.read(stack);
        return custom != null && custom.contains(KEY, net.minecraft.nbt.Tag.TAG_COMPOUND) ? custom.getCompound(KEY).copy() : null;
        *///?}
    }

    public static boolean has(ItemStack stack) {
        return read(stack) != null;
    }

    /** Nach Aenderung eines mit {@link #read} geholten Tags. 1.20.1: nichts (Tag ist lebend). */
    public static void writeBack(ItemStack stack, CompoundTag tag) {
        //? if >= 1.21.1 {
        /*net.minecraft.world.item.component.CustomData old = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        if (old != null) {
            CompoundTag t = tag.copy();
            if (!t.contains("id")) {
                CompoundTag o = old.copyTag();
                if (o.contains("id")) t.putString("id", o.getString("id"));
            }
            stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(t));
        } else {
            CompoundTag custom = StackNbt.read(stack);
            if (custom != null && custom.contains(KEY)) StackNbt.orCreate(stack).put(KEY, tag.copy());
        }
        *///?}
    }

    /**
     * BE-Daten setzen, die beim Setzen des Blocks in die Blockentitaet {@code type} geladen werden.
     * 1.20.1: {@code BlockEntityTag} im Item-NBT (wie bisher {@code StackNbt.orCreate(stack).put(..)}).
     */
    public static void write(ItemStack stack, BlockEntityType<?> type, CompoundTag tag) {
        //? if < 1.21.1 {
        StackNbt.orCreate(stack).put(KEY, tag);
        //?} else {
        /*net.minecraft.world.item.BlockItem.setBlockEntityData(stack, type, tag);
        *///?}
    }
}
