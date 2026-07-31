package com.hbm_m.recipe;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

/**
 * Port of the 1.7.10 Gas Centrifuge's inner {@code PseudoFluidTank} class: a simple
 * fill/capacity pair typed with a {@link PseudoFluidType} instead of a real Forge fluid.
 */
public class PseudoFluidTank {

    private PseudoFluidType type;
    private int fill;
    private final int maxFill;

    public PseudoFluidTank(PseudoFluidType type, int maxFill) {
        this.type = type;
        this.maxFill = maxFill;
    }

    public int getFill() { return fill; }
    public int getMaxFill() { return maxFill; }
    public PseudoFluidType getTankType() { return type; }

    public void setFill(int amount) {
        this.fill = Mth.clamp(amount, 0, maxFill);
    }

    public void setTankType(PseudoFluidType newType) {
        PseudoFluidType resolved = newType == null ? PseudoFluidType.NONE : newType;
        if (this.type == resolved) return;
        this.type = resolved;
        setFill(0);
    }

    public void writeToNBT(CompoundTag nbt, String prefix) {
        nbt.putInt(prefix + "_amount", fill);
        nbt.putString(prefix + "_type", type.getId());
    }

    public void readFromNBT(CompoundTag nbt, String prefix) {
        fill = nbt.getInt(prefix + "_amount");
        PseudoFluidType t = PseudoFluidType.byId(nbt.getString(prefix + "_type"));
        type = t != null ? t : PseudoFluidType.NONE;
    }

    public void serialize(FriendlyByteBuf buf) {
        buf.writeInt(fill);
        buf.writeUtf(type.getId());
    }

    public void deserialize(FriendlyByteBuf buf) {
        fill = buf.readInt();
        PseudoFluidType t = PseudoFluidType.byId(buf.readUtf());
        type = t != null ? t : PseudoFluidType.NONE;
    }
}
