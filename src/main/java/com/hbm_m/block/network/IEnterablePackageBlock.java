package com.hbm_m.block.network;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.entity.conveyor.MovingConveyorPackageEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Paket-Teil von {@code api.hbm.conveyor.IEnterableBlock} ({@code canPackageEnter}/{@code onPackageEnter}). Bloecke, die
 * im Original {@code canPackageEnter} fest mit {@code false} beantworten (Aufteiler, Portionierer), implementieren dieses
 * Interface nicht. {@code dir} ist die Seite, durch die das Paket hereinkommt ({@code null} = unbekannt, {@code UP} beim
 * Hineinfallen).
 */
public interface IEnterablePackageBlock {

    default boolean canPackageEnter(Level level, BlockPos pos, @Nullable Direction dir, MovingConveyorPackageEntity entity) {
        return true;
    }

    void onPackageEnter(Level level, BlockPos pos, MovingConveyorPackageEntity item);
}
