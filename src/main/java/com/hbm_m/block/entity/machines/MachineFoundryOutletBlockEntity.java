package com.hbm_m.block.entity.machines;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MachineFoundryOutletBlockEntity extends BlockEntity {

    /** Only let this material through (null = all materials allowed). */
    @Nullable public MaterialType filter = null;

    /** When true: let everything EXCEPT the filter material through. */
    public boolean invertFilter   = false;

    /** When true: outlet is closed by default and OPENS with redstone.
     *  When false (default): outlet is open by default and CLOSES with redstone. */
    public boolean invertRedstone = false;

    public MachineFoundryOutletBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_OUTLET_BE.get(), pos, state);
    }

    public boolean isClosed(Level level, BlockPos pos) {
        boolean powered = level.hasNeighborSignal(pos);
        return invertRedstone ^ powered;
    }

    public boolean passesFilter(MaterialType type) {
        if (filter == null) return true;
        boolean matches = filter == type;
        return invertFilter ? !matches : matches;
    }

    /**
     * Called by an adjacent channel or crucible to offer material to this outlet.
     * The outlet tries to pour all offered material downward into the nearest basin.
     * Returns how much was accepted (poured).
     */
    public int receiveMaterial(Level level, BlockPos pos, BlockState state,
                                Direction incomingDir, MaterialType type, int amount) {
        if (amount <= 0) return 0;
        if (isClosed(level, pos)) return 0;
        if (!passesFilter(type)) return 0;

        Direction facing = state.hasProperty(com.hbm_m.block.machines.MachineFoundryOutletBlock.FACING)
                ? state.getValue(com.hbm_m.block.machines.MachineFoundryOutletBlock.FACING)
                : Direction.NORTH;

        // Material must arrive from the opposite of the facing direction
        if (incomingDir != facing.getOpposite()) return 0;

        return pourDownward(level, pos, type, amount);
    }

    /** Searches up to 4 blocks directly below for a FoundryBasin and pours into it. */
    private int pourDownward(Level level, BlockPos outletPos, MaterialType type, int amount) {
        for (int dy = 1; dy <= 4; dy++) {
            BlockPos target = outletPos.below(dy);
            BlockEntity te = level.getBlockEntity(target);
            if (te instanceof MachineFoundryBasinBlockEntity basin) {
                int poured = basin.receiveMaterial(type, amount);
                if (poured > 0) {
                    setChanged();
                    level.sendBlockUpdated(outletPos, level.getBlockState(outletPos), level.getBlockState(outletPos), 3);
                    return poured;
                }
                break;
            }
            if (!level.isEmptyBlock(target)) break;
        }
        return 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (filter != null) tag.putString("filter", filter.name);
        tag.putBoolean("invertFilter",   invertFilter);
        tag.putBoolean("invertRedstone", invertRedstone);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("filter")) filter = MaterialType.byName(tag.getString("filter"));
        invertFilter   = tag.getBoolean("invertFilter");
        invertRedstone = tag.getBoolean("invertRedstone");
    }

    @Override
    public CompoundTag getUpdateTag() { CompoundTag t = super.getUpdateTag(); saveAdditional(t); return t; }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
