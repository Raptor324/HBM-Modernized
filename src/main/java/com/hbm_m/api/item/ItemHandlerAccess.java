package com.hbm_m.api.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

/**
 * Кросс-платформенный доступ к item-handler capability блока/BlockEntity.
 *
 * <p><b>Главная разница 1.20.1 ↔ 1.21.1:</b>
 * <ul>
 *   <li><b>Forge 1.20.1</b>: {@code be.getCapability(ForgeCapabilities.ITEM_HANDLER, side)} →
 *       {@code LazyOptional<IItemHandler>}</li>
 *   <li><b>NeoForge 1.21.1</b>: {@code level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side)} →
 *       {@code IItemHandler} напрямую (без LazyOptional). Запрос идёт через Level, не через BE.</li>
 *   <li><b>Fabric</b>: {@code ItemStorage.SIDED.find(level, pos, state, be, side)} (Transfer API)</li>
 * </ul>
 *
 * <p>Заменяет прямые {@code be.getCapability(ForgeCapabilities.ITEM_HANDLER, ...)} в общем коде.
 *
 * <p><b>Тип возвращаемого значения</b> {@code IItemHandler} отличается пакетом на forge vs neoforge,
 * поэтому {@link #getItemHandler} имеет платформенно-специфичную сигнатуру через {@code //? if}.
 */
public final class ItemHandlerAccess {
    private ItemHandlerAccess() {}

    /**
     * Получить IItemHandler блока по позиции и стороне.
     *
     * @param level уровень
     * @param pos   позиция блока
     * @param side  сторона (может быть {@code null} для «без стороны»)
     * @return handler или {@code null}, если блок не предоставляет item-capability
     */
    //? if forge {
    @Nullable
    public static net.minecraftforge.items.IItemHandler getItemHandler(Level level, BlockPos pos, @Nullable Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || be.isRemoved()) return null;
        return be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
    }
    //?}
    //? if neoforge {
    /*@Nullable
    public static net.neoforged.neoforge.items.IItemHandler getItemHandler(Level level, BlockPos pos, @Nullable Direction side) {
        // На NeoForge запрос идёт через Level, не через BlockEntity.getCapability.
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || be.isRemoved()) return null;
        return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, side);
    }
    *///?}
}
