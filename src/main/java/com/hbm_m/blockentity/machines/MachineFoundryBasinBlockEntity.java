package com.hbm_m.blockentity.machines;

import com.hbm_m.api.block.ICrucibleAcceptor;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;
import com.hbm_m.item.material.ItemCastMold;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Port of the 1.7.10 TileEntityFoundryBasin / TileEntityFoundryCastingBase.
 * - capacity depends on the installed mold (no mold = nothing can be poured in)
 * - can ONLY be filled by pouring from above (crucible or outlet), never by
 *   sideways flow from channels
 * - casts once the basin is completely full, after a cool-off period
 */
public class MachineFoundryBasinBlockEntity extends BlockEntity implements ICrucibleAcceptor {

    /** Original: cooloff starts at 100 and resets to 200 after each cast. */
    public static final int COOLOFF_TIME = 200;

    @Nullable public MaterialType type = null;
    public int amount = 0;

    private ItemStack moldSlot   = ItemStack.EMPTY;
    private ItemStack outputSlot = ItemStack.EMPTY;
    private int cooloff = 100;

    private float fillLevel = 0f;
    private int   fillColor = 0xFFC18336;

    public MachineFoundryBasinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_BASIN_BE.get(), pos, state);
    }

    /* ── mold handling ──────────────────────────────────────────────────── */

    public ItemStack getMoldSlot()   { return moldSlot; }
    public ItemStack getOutputSlot() { return outputSlot; }

    public boolean insertMold(ItemStack stack) {
        if (!moldSlot.isEmpty()) return false;
        if (!(stack.getItem() instanceof ItemCastMold)) return false;
        moldSlot = stack.copy();
        moldSlot.setCount(1);
        setChanged();
        syncToClient();
        return true;
    }

    public ItemStack takeMold() {
        // original: mold can only be removed while the basin is empty
        if (amount > 0) return ItemStack.EMPTY;
        ItemStack s = moldSlot.copy();
        moldSlot = ItemStack.EMPTY;
        setChanged();
        syncToClient();
        return s;
    }

    public ItemStack takeOutput() {
        ItemStack s = outputSlot.copy();
        outputSlot = ItemStack.EMPTY;
        setChanged();
        syncToClient();
        return s;
    }

    public @Nullable ItemCastMold getInstalledMold() {
        if (moldSlot.isEmpty()) return null;
        return moldSlot.getItem() instanceof ItemCastMold mold ? mold : null;
    }

    /** Original: capacity is dictated by the installed mold, 0 without mold. */
    public int getCapacity() {
        ItemCastMold mold = getInstalledMold();
        if (mold == null) return 0;
        return com.hbm_m.recipe.MoldCastingRecipes.getCost(mold.getMoldType());
    }

    private @Nullable ItemStack getResultFor(MaterialType type, ItemCastMold.MoldType mold) {
        ItemStack out = com.hbm_m.recipe.MoldCastingRecipes.getOutput(mold, type);
        return out.isEmpty() ? null : out;
    }

    /* ── tick ───────────────────────────────────────────────────────────── */

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFoundryBasinBlockEntity be) {
        if (level.isClientSide) return;
        boolean dirty = false;

        int capacity = be.getCapacity();
        if (be.amount > capacity) be.amount = capacity;
        if (be.amount == 0) be.type = null;

        ItemCastMold mold = be.getInstalledMold();

        if (mold != null && be.type != null && capacity > 0
                && be.amount == capacity && be.outputSlot.isEmpty()) {

            ItemStack result = be.getResultFor(be.type, mold.getMoldType());
            if (result != null) {
                be.cooloff--;
                if (be.cooloff <= 0) {
                    be.amount = 0;
                    be.type = null;
                    be.outputSlot = result.copy();
                    be.cooloff = COOLOFF_TIME;
                    dirty = true;
                }
            } else {
                be.cooloff = COOLOFF_TIME;
            }
        } else {
            be.cooloff = COOLOFF_TIME;
        }

        float newFill  = capacity > 0 ? Math.min(1f, (float) be.amount / capacity) : 0f;
        int   newColor = be.type != null ? (0xFF000000 | be.type.color) : 0xFFC18336;
        if (be.fillLevel != newFill || be.fillColor != newColor) {
            be.fillLevel = newFill;
            be.fillColor = newColor;
            dirty = true;
        }
        if (dirty) {
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    /* ── ICrucibleAcceptor ──────────────────────────────────────────────── */

    private boolean standardCheck(MaterialStack stack) {
        if (this.type != null && this.type != stack.type && this.amount > 0) return false;
        if (this.amount >= getCapacity()) return false;
        if (!this.outputSlot.isEmpty()) return false;
        ItemCastMold mold = getInstalledMold();
        if (mold == null) return false;
        return getResultFor(stack.type, mold.getMoldType()) != null;
    }

    private @Nullable MaterialStack standardAdd(MaterialStack stack) {
        this.type = stack.type;

        if (stack.amount + this.amount <= getCapacity()) {
            this.amount += stack.amount;
            setChanged();
            return null;
        }

        int required = getCapacity() - this.amount;
        this.amount = getCapacity();
        stack.amount -= required;
        setChanged();
        return stack;
    }

    /* Basin can't accept sideways flowing (original behavior). */
    @Override public boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, MaterialStack stack) { return false; }
    @Override public @Nullable MaterialStack flow(Level level, BlockPos pos, Direction side, MaterialStack stack) { return stack; }

    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        if (side != Direction.UP) return false;
        return standardCheck(stack);
    }

    @Override
    public @Nullable MaterialStack pour(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return standardAdd(stack);
    }

    /* ── renderer data ──────────────────────────────────────────────────── */

    public float getFillLevel() { return fillLevel; }
    public int   getFillColor() { return fillColor; }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /* ── NBT / sync ─────────────────────────────────────────────────────── */

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (type != null) tag.putString("mat_type", type.name);
        tag.putInt("mat_amount", amount);
        if (!moldSlot.isEmpty())   tag.put("mold",   moldSlot.save(new CompoundTag()));
        if (!outputSlot.isEmpty()) tag.put("output", outputSlot.save(new CompoundTag()));
        tag.putInt("cooloff", cooloff);
        tag.putFloat("fillLevel", fillLevel);
        tag.putInt("fillColor", fillColor);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        if (type != null) tag.putString("mat_type", type.name);
        tag.putInt("mat_amount", amount);
        if (!moldSlot.isEmpty())   tag.put("mold",   moldSlot.save(new CompoundTag()));
        if (!outputSlot.isEmpty()) tag.put("output", outputSlot.save(new CompoundTag()));
        tag.putInt("cooloff", cooloff);
        tag.putFloat("fillLevel", fillLevel);
        tag.putInt("fillColor", fillColor);
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("mat_type")) type = MaterialType.byName(tag.getString("mat_type"));
        else type = null;
        amount = tag.getInt("mat_amount");
        moldSlot   = tag.contains("mold")   ? ItemStack.of(tag.getCompound("mold"))   : ItemStack.EMPTY;
        outputSlot = tag.contains("output") ? ItemStack.of(tag.getCompound("output")) : ItemStack.EMPTY;
        cooloff = tag.contains("cooloff") ? tag.getInt("cooloff") : 100;
        fillLevel = tag.getFloat("fillLevel");
        fillColor = tag.getInt("fillColor");
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);
        if (tag.contains("mat_type")) type = MaterialType.byName(tag.getString("mat_type"));
        else type = null;
        amount = tag.getInt("mat_amount");
        moldSlot   = tag.contains("mold")   ? ItemStack.of(tag.getCompound("mold"))   : ItemStack.EMPTY;
        outputSlot = tag.contains("output") ? ItemStack.of(tag.getCompound("output")) : ItemStack.EMPTY;
        cooloff = tag.contains("cooloff") ? tag.getInt("cooloff") : 100;
        fillLevel = tag.getFloat("fillLevel");
        fillColor = tag.getInt("fillColor");
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public CompoundTag getUpdateTag() { CompoundTag t = super.getUpdateTag(); saveAdditional(t); return t; }
    //?} else {
    /*@Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
 CompoundTag t = super.getUpdateTag(registries); saveAdditional(t, registries); return t; 
    }
    *///?}

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
