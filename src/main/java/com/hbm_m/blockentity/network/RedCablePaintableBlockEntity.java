package com.hbm_m.blockentity.network;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.energy.Nodespace;
import com.hbm_m.api.energy.PowerConductor;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт TileEntityCablePaintable (1.7.10): цельноблочный кабель-камуфляж.
 * ПКМ блоком перекрашивается под этот блок; рендер — RedCablePaintableRenderer.
 */
public class RedCablePaintableBlockEntity extends BaseHbmBlockEntity implements PowerConductor, com.hbm_m.interfaces.ICopiable {

    @Nullable
    private BlockState camo;

    public RedCablePaintableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RED_CABLE_PAINTABLE_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RedCablePaintableBlockEntity entity) {
        if (level.isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) level;
        Nodespace.PowerNode node = Nodespace.getNode(serverLevel, pos);
        if (node == null || node.expired) {
            Nodespace.createNode(serverLevel, entity.createNode(pos));
        }
    }

    @Nullable
    public BlockState getCamo() {
        return camo;
    }

    public void setCamo(@Nullable BlockState camo) {
        this.camo = camo;
        setChanged();
        if (level != null && !level.isClientSide && !isRemoved()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public boolean canConnectEnergy(@Nullable Direction side) {
        return true;
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        if (camo != null) {
            CompoundTag camoTag = new CompoundTag();
            camoTag.put("state", NbtUtils.writeBlockState(camo));
            tag.put("camo", camoTag);
        }
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        camo = null;
        if (tag.contains("camo")) {
            CompoundTag stateTag = tag.getCompound("camo").getCompound("state");
            HolderLookup.Provider access = registries;
            if (access == null && level != null) access = level.registryAccess();
            if (access != null) {
                camo = NbtUtils.readBlockState(access.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), stateTag);
            }
        }
    }

    /* ── Устройство настройки: копия/вставка камуфляжа (аналог paintblock/paintmeta) ── */

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        if (camo != null) {
            CompoundTag camoTag = new CompoundTag();
            camoTag.put("state", NbtUtils.writeBlockState(camo));
            nbt.put("camo", camoTag);
        }
        return nbt;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level level, net.minecraft.world.entity.player.Player player, BlockPos pos) {
        if (!nbt.contains("camo")) return;
        HolderLookup.Provider access = level != null ? level.registryAccess()
                : (this.level != null ? this.level.registryAccess() : null);
        if (access == null) return;
        CompoundTag stateTag = nbt.getCompound("camo").getCompound("state");
        BlockState state = NbtUtils.readBlockState(access.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), stateTag);
        setCamo(state);
    }

    // Without this the node outlives the block: the grid stays merged through a position that
    // no longer conducts, and a conductor placed there later inherits the ghost node.
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel serverLevel) {
            Nodespace.destroyNode(serverLevel, getBlockPos());
        }
    }
}
