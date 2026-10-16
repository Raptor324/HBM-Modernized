package com.hbm_m.interfaces;

import com.hbm_m.inventory.material.MaterialType;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Порт com.hbm.tileentity.IMetalCopiable (1.7.10): копирка списка металлов,
 * которыми "заряжается" селектор рецептов (тигель) или фильтруется литьё.
 * Вставка по умолчанию пустая — как в оригинале, реализор сам решает,
 * читать ли matFilter из NBT (см. MachineFoundryOutletBlockEntity).
 */
public interface IMetalCopiable extends ICopiable {

    int[] getMatsToCopy();

    @Override
    default CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        if (getMatsToCopy().length > 0) nbt.putIntArray("matFilter", getMatsToCopy());
        return nbt;
    }

    @Override
    default void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) { }

    @Override
    default String[] infoForDisplay(Level level, BlockPos pos) {
        int[] ids = getMatsToCopy();
        String[] names = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            MaterialType mat = MaterialType.byId(ids[i]);
            names[i] = mat != null ? "material.hbm_m." + mat.name : null;
        }
        return names;
    }
}
