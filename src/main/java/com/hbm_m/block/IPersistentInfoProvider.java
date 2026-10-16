package com.hbm_m.block;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Порт 1.7.10 {@code com.hbm.blocks.IPersistentInfoProvider}: блок добавляет строки
 * тултипа к предмету, несущему {@code persistent}-тег (см.
 * {@link com.hbm_m.blockentity.IPersistentNBT}).
 *
 * <p>Вызывается из {@code MultiblockBlockItem.appendHbmTooltip} — аналог вызова из
 * {@code ItemBlockBase.addInformation} в оригинале.</p>
 */
public interface IPersistentInfoProvider {

    void addInformation(ItemStack stack, CompoundTag persistentTag, @Nullable Level level,
                        List<Component> list, TooltipFlag flag);
}
