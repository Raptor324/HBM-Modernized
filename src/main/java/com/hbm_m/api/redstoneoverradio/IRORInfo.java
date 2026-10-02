package com.hbm_m.api.redstoneoverradio;

/** Port of {@code api.hbm.redstoneoverradio.IRORInfo} (1.7.10 Original). */
public interface IRORInfo {

    String PREFIX_VALUE = "VAL:";
    String PREFIX_FUNCTION = "FUN:";

    String[] getFunctionInfo();

    /**
     * Original {@code TileEntityProxyCombo}: die Anschlusszellen einer Mehrblockmaschine reichen Funk-Abfragen an
     * den Kern weiter. Gibt den Blockeintrag am Ort zurueck oder - bei einer Mehrblockzelle - den des Kerns.
     */
    static net.minecraft.world.level.block.entity.BlockEntity resolve(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof IRORInfo) && be instanceof com.hbm_m.interfaces.IMultiblockPart part && part.getControllerPos() != null) {
            return level.getBlockEntity(part.getControllerPos());
        }
        return be;
    }
}
