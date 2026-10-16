package com.hbm_m.blockentity.network;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Фиктивная часть пилона (аналог dummy-блоков BlockDummyable).
 * Хранит позицию ядра, чтобы при разрушении части разрушить весь пилон.
 */
public class PylonDummyBlockEntity extends BaseHbmBlockEntity {

    @Nullable
    private BlockPos corePos;

    public PylonDummyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PYLON_DUMMY_BE.get(), pos, state);
    }

    public void setCorePos(BlockPos corePos) {
        this.corePos = corePos.immutable();
        setChanged();
    }

    @Nullable
    public BlockPos getCorePos() {
        return corePos;
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        if (corePos != null) {
            tag.putLong("core", corePos.asLong());
        }
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        corePos = tag.contains("core") ? BlockPos.of(tag.getLong("core")) : null;
    }
}
