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
    static net.minecraft.world.level.block.entity.BlockEntity resolve(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos) {
        // Original Compat.getTileStandard: ungeladene Chunks liefern nichts
        if (!level.hasChunkAt(pos)) return null;
        net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof IRORInfo) && be instanceof com.hbm_m.interfaces.IMultiblockPart part && part.getControllerPos() != null) {
            return level.getBlockEntity(part.getControllerPos());
        }
        // Original RBMK-Saeulen: die oberen Zellen sind TileEntityProxyCombo und reichen an den Fuss weiter
        if (be == null && level.getBlockState(pos).getBlock() instanceof com.hbm_m.block.machines.rbmk.RBMKColumnFillerBlock) {
            net.minecraft.core.BlockPos base = com.hbm_m.block.machines.rbmk.RBMKColumnFillerBlock.findBase(level, pos);
            if (base != null) return level.getBlockEntity(base);
        }
        return be;
    }
}
