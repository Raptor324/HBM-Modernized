package com.hbm_m.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Порт com.hbm.interfaces.ICopiable (1.7.10): бэкенд Устройства настройки
 * (ItemSettingsTool). Shift+ПКМ копирует {@link #getSettings}, ПКМ вставляет
 * {@link #pasteSettings}, строки {@link #infoForDisplay} рисуются на экране,
 * пока предмет в руке.
 *
 * <p>В оригинале источником копии мог быть и TileEntity, и Block (Either) —
 * на блоке его реализовывали краски-кабели 1.7.10. В этом порте все цели —
 * BE-блоки, поэтому интерфейс BE-only; "фиктивный" this - это BlockEntity.
 */
public interface ICopiable {

    /** @return NBT настроек для копирки или null, если копировать нечего. */
    @Nullable
    CompoundTag getSettings(Level level, BlockPos pos);

    /** index — выбранный копиркой параметр вставки (см. copyIndex на предмете). */
    void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos);

    /** Оригинал getSettingsSourceID: descriptionId блока, сохраняется как "tileName". */
    default String getSettingsSourceID() {
        return ((BlockEntity) this).getBlockState().getBlock().getDescriptionId();
    }

    /** Оригинал getSettingsSourceDisplay: локализованное имя блока для чата. */
    default Component getSettingsSourceDisplay() {
        return ((BlockEntity) this).getBlockState().getBlock().getName();
    }

    /** Ключи строк для экранного информера; null = список не показывать. */
    @Nullable
    default String[] infoForDisplay(Level level, BlockPos pos) {
        return null;
    }
}
