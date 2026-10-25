package com.hbm_m.block.network;

import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Port of {@code api.hbm.conveyor.IEnterableBlock} (1.7.10 Original). Blocks implementing this
 * (e.g. machine input slots) can absorb a {@link MovingConveyorItemEntity} that walks into them.
 * <p>
 * Der Paket-Teil des Originals ({@code canPackageEnter}/{@code onPackageEnter} fuer {@code IConveyorPackage})
 * steht in {@link IEnterablePackageBlock}; Bloecke, die Pakete im Original ablehnen, implementieren es nicht.
 */
public interface IEnterableBlock {
    void onItemEnter(Level level, BlockPos pos, MovingConveyorItemEntity item);

    /**
     * 1:1 {@code canItemEnter}: {@code dir} ist die Seite des Blocks, durch die das Teil hereinkommt
     * ({@code null} = {@code ForgeDirection.UNKNOWN}); von oben hereinfallend {@code UP}.
     */
    default boolean canItemEnter(Level level, BlockPos pos, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction dir, MovingConveyorItemEntity item) {
        return true;
    }
}
