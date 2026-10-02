package com.hbm_m.interfaces;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 1:1 {@code com.hbm.interfaces.ICopiable}: Maschinen (BlockEntity) oder Bloecke, deren Einstellungen das
 * Einstellungswerkzeug kopieren und einfuegen kann. {@code Either<TileEntity, Block>} des Originals wird hier zum
 * Block an der Position.
 */
public interface ICopiable {

    @Nullable
    CompoundTag getSettings(Level world, BlockPos pos);

    void pasteSettings(CompoundTag nbt, int index, Level world, Player player, BlockPos pos);

    default String getSettingsSourceID(Block block) {
        return block.getDescriptionId();
    }

    default Component getSettingsSourceDisplay(Block block) {
        return block.getName();
    }

    @Nullable
    default String[] infoForDisplay(Level world, BlockPos pos) {
        return null;
    }
}
