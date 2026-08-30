package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.multiblock.IDummyCorePart;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared block entity for every PWR structure block that isn't the controller (fuel/control/
 * channel/heatex/heatsink/neutron_source/casing/reflector/port). 1:1 role equivalent of the
 * original's {@code BlockPWR.TileEntityBlockPWR} "dummy" delegate, but implemented via
 * {@link IDummyCorePart} instead of the original's meta-carrier-block trick (each part keeps its
 * own real block type/texture rather than being rewritten into a generic {@code pwr_block} with a
 * remembered original type - a pure implementation detail of 1.7.10's metadata system that has no
 * modern equivalent purpose; the player-visible result - each block always looks like what it is
 * - is unaffected).
 */
public class PWRPartBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements IDummyCorePart {

    public enum Kind { FUEL, CONTROL, CHANNEL, HEATEX, HEATSINK, NEUTRON_SOURCE, CASING, REFLECTOR, PORT }

    private final Kind kind;

    @Nullable
    private BlockPos corePos;

    public PWRPartBlockEntity(BlockPos pos, BlockState state, Kind kind) {
        super(ModBlockEntities.PWR_PART_BE.get(), pos, state);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    /** True for casing/reflector/port - the assembly's outer boundary, stops the flood fill. */
    public boolean isCasing() {
        return kind == Kind.CASING || kind == Kind.REFLECTOR || kind == Kind.PORT;
    }

    @Nullable
    @Override
    public BlockPos getRawCorePos() {
        return corePos;
    }

    @Override
    public void setRawCorePos(@Nullable BlockPos pos) {
        this.corePos = pos;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Called when this part is removed while pointing at an assembled controller - invalidates it. */
    public void notifyRemoved() {
        if (level == null || isCore()) return;
        BlockEntity be = level.getBlockEntity(getCorePos());
        if (be instanceof PWRControllerBlockEntity controller) {
            controller.setAssembled(false);
        }
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        if (corePos != null) {
            com.hbm_m.platform.PlatformHooks.writeBlockPos(tag, "CorePos", corePos);
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        corePos = tag.contains("CorePos") ? com.hbm_m.platform.PlatformHooks.readBlockPos(tag, "CorePos") : null;
    }
}
