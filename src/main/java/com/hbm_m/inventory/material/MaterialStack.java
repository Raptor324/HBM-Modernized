package com.hbm_m.inventory.material;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * Порция расплавленного материала: {@link MaterialType} + количество в квантах.
 * Порт {@code Mats.MaterialStack} 1.7.10; единица измерения — квант
 * ({@link MaterialShapes#QUANTUM} = 1/72 слитка), все операции целочисленны.
 */
public class MaterialStack {

    public final MaterialType type;
    public int amount;

    public MaterialStack(MaterialType type, int amount) {
        this.type   = type;
        this.amount = amount;
    }

    public MaterialStack copy() { return new MaterialStack(type, amount); }

    public boolean isEmpty() { return amount <= 0; }

    public void writeToNBT(CompoundTag tag) {
        tag.putInt("mat_id", type.id);
        tag.putInt("mat_amount", amount);
    }

    public static @Nullable MaterialStack readFromNBT(CompoundTag tag) {
        if (!tag.contains("mat_id")) return null;
        MaterialType t = MaterialType.byId(tag.getInt("mat_id"));
        if (t == null) return null;
        return new MaterialStack(t, tag.getInt("mat_amount"));
    }

    @Override
    public String toString() { return amount + "q " + type.name; }
}
