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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт TileEntityCableGauge (вложен в BlockCableGauge 1.7.10): цельноблочный кабель-датчик.
 * Считает энергию, протекающую через сеть узла (HE/тик и HE/сек), и показывает её в HUD.
 */
public class RedCableGaugeBlockEntity extends BaseHbmBlockEntity implements PowerConductor {

    public long deltaTick;
    public long deltaLastSecond;
    private long deltaSecond;

    public RedCableGaugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RED_CABLE_GAUGE_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RedCableGaugeBlockEntity entity) {
        if (level.isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) level;

        Nodespace.PowerNode node = Nodespace.getNode(serverLevel, pos);
        if (node == null || node.expired) {
            Nodespace.createNode(serverLevel, entity.createNode(pos));
            node = Nodespace.getNode(serverLevel, pos);
        }

        if (node != null && node.net != null) {
            entity.deltaTick = node.net.energyTracker;
            entity.deltaSecond += entity.deltaTick;
        }

        if (level.getGameTime() % 20 == 0) {
            entity.deltaLastSecond = entity.deltaSecond;
            entity.deltaSecond = 0;
            entity.syncToClient();
        }
    }

    private void syncToClient() {
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
        tag.putLong("deltaTick", deltaTick);
        tag.putLong("deltaLastSecond", deltaLastSecond);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        deltaTick = Math.max(tag.getLong("deltaTick"), 0);
        deltaLastSecond = Math.max(tag.getLong("deltaLastSecond"), 0);
    }
}
