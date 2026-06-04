package com.hbm_m.block.entity.machines;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;
import com.hbm_m.item.material.ItemCastMold;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MachineFoundryBasinBlockEntity extends BlockEntity {

    public static final int CAST_TIME    = 100;
    public static final int CAPACITY     = MachineCrucibleBlockEntity.LIQUID_CAPACITY;

    @Nullable private MaterialStack material = null;
    private ItemStack moldSlot  = ItemStack.EMPTY;
    private ItemStack outputSlot = ItemStack.EMPTY;
    private int castProgress = 0;
    private float fillLevel  = 0f;
    private int   fillColor  = 0xFFC18336;

    public MachineFoundryBasinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_BASIN_BE.get(), pos, state);
    }

    public int receiveMaterial(MaterialType type, int amount) {
        if (amount <= 0) return 0;
        if (material == null) {
            int toFill = Math.min(amount, CAPACITY);
            material = new MaterialStack(type, toFill);
            setChanged();
            return toFill;
        }
        if (material.type != type) return 0;
        int space = CAPACITY - material.amount;
        int toFill = Math.min(amount, space);
        material.amount += toFill;
        setChanged();
        return toFill;
    }

    public ItemStack getMoldSlot()   { return moldSlot; }
    public ItemStack getOutputSlot() { return outputSlot; }

    public boolean insertMold(ItemStack stack) {
        if (!moldSlot.isEmpty()) return false;
        if (!(stack.getItem() instanceof ItemCastMold)) return false;
        moldSlot = stack.copy();
        setChanged();
        return true;
    }

    public ItemStack takeMold() {
        ItemStack s = moldSlot.copy(); moldSlot = ItemStack.EMPTY; setChanged(); return s;
    }

    public ItemStack takeOutput() {
        ItemStack s = outputSlot.copy(); outputSlot = ItemStack.EMPTY; setChanged(); return s;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFoundryBasinBlockEntity be) {
        if (level.isClientSide) return;
        boolean dirty = false;

        if (be.material != null && !be.moldSlot.isEmpty() && be.outputSlot.isEmpty()) {
            ItemCastMold mold = (ItemCastMold) be.moldSlot.getItem();
            ItemStack result  = be.getResultFor(be.material.type, mold.getMoldType());
            if (result != null && be.material.amount >= MaterialStack.MB_PER_PLATE) {
                be.castProgress++;
                if (be.castProgress >= CAST_TIME) {
                    be.material.amount -= MaterialStack.MB_PER_PLATE;
                    if (be.material.isEmpty()) be.material = null;
                    be.outputSlot  = result.copy();
                    be.castProgress = 0;
                    dirty = true;
                }
            } else {
                be.castProgress = 0;
            }
        } else {
            be.castProgress = 0;
        }

        float newFill = be.material != null ? Math.min(1f, (float) be.material.amount / CAPACITY) : 0f;
        int   newColor = be.material != null ? (0xFF000000 | be.material.type.color) : 0xFFC18336;
        if (be.fillLevel != newFill || be.fillColor != newColor) {
            be.fillLevel = newFill; be.fillColor = newColor; dirty = true;
        }
        if (dirty) { be.setChanged(); level.sendBlockUpdated(pos, state, state, 3); }
    }

    private @Nullable ItemStack getResultFor(MaterialType type, ItemCastMold.MoldType mold) {
        if (mold == ItemCastMold.MoldType.PLATE && type.hasCastPlate())
            return type.getCastPlate(1);
        return null;
    }

    public float getFillLevel() { return fillLevel; }
    public int   getFillColor() { return fillColor; }
    public int   getCastProgress() { return castProgress; }
    public @Nullable MaterialStack getMaterial() { return material; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (material != null) { CompoundTag m = new CompoundTag(); material.writeToNBT(m); tag.put("material", m); }
        if (!moldSlot.isEmpty())   tag.put("mold",   moldSlot.save(new CompoundTag()));
        if (!outputSlot.isEmpty()) tag.put("output", outputSlot.save(new CompoundTag()));
        tag.putInt("castProgress", castProgress);
        tag.putFloat("fillLevel", fillLevel); tag.putInt("fillColor", fillColor);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("material")) material = MaterialStack.readFromNBT(tag.getCompound("material"));
        if (tag.contains("mold"))     moldSlot   = ItemStack.of(tag.getCompound("mold"));
        if (tag.contains("output"))   outputSlot = ItemStack.of(tag.getCompound("output"));
        castProgress = tag.getInt("castProgress");
        fillLevel = tag.getFloat("fillLevel"); fillColor = tag.getInt("fillColor");
    }

    @Override
    public CompoundTag getUpdateTag() { CompoundTag t = super.getUpdateTag(); saveAdditional(t); return t; }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
