package com.hbm_m.blockentity.machines.fusion;

import com.hbm_m.api.network.NodeNet;
import com.hbm_m.blockentity.BaseHbmBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gemeinsame Basis der Fusions-BlockEntities ohne Inventar
 * (Original: {@code TileEntityLoadedBase} - dort ebenfalls nur "bin ich geladen?" plus die
 * gebuendelten Sync-Pakete via {@code networkPackNT}).
 */
public abstract class FusionSyncedBlockEntity extends BaseHbmBlockEntity implements NodeNet.ILoadedEntry {

    protected FusionSyncedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Original: {@code TileEntityLoadedBase#isLoaded} - schuetzt die Netze vor Zombie-Mitgliedern. */
    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    /** Original: {@code networkPackNT(...)} - hier ueber den vanilla BlockEntity-Update-Pfad. */
    protected void sendUpdateToClient() {
        if (level != null && !level.isClientSide && !isRemoved()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
