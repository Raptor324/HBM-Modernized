package com.hbm_m.inventory.material;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public class MaterialStack {

    public static final int MB_PER_INGOT  = 1_000;
    public static final int MB_PER_PLATE  = 2_000;
    public static final int BUCKET        = 9_000;

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
    public String toString() { return amount + "mb " + type.name; }
}
