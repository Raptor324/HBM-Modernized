package com.hbm_m.block.custom.weapons;

import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;

/**
 * Гравитирующий блок селлафита
 * Падает как песок/гравий, но может быть переведен в твёрдое состояние
 *
 * Используется во время генерации кратера:
 * 1. Создаётся FallingBlockEntity с этим блоком
 * 2. Падает на ландшафт, заполняя неровности
 * 3. При приземлении автоматически преобразуется в твёрдый блок селлафита
 */
public class FallingSellafit extends FallingBlock {
    private final Block solidEquivalent; // Твёрдый блок, в который преобразуется

    /**
     * @param solidBlock Твёрдый блок, в который должен преобразоваться гравитирующий селлафит
     */
    public FallingSellafit(Block solidBlock) {
        super(Block.Properties.of()
                .mapColor(MapColor.STONE)
                .sound(SoundType.STONE)
                .strength(3.0F, 3.0F)
                .speedFactor(1.0F) // Нормальная скорость падения
        );
        this.solidEquivalent = solidBlock;
    }

    /**
     * Обработчик приземления - преобразует падающий блок в твёрдый
     * КЛЮЧЕВОЙ МОМЕНТ: Здесь происходит затвердевание!
     */
    @Override
    public void onLand(Level level, BlockPos pos, BlockState fallingState, BlockState groundState, FallingBlockEntity entity) {
        // НЕ вызываем super.onLand() - это создаст обычный блок или уничтожит сущность

        // Преобразуем в твёрдый блок при приземлении
        if (!level.isClientSide && this.solidEquivalent != null) {
            level.setBlock(pos, this.solidEquivalent.defaultBlockState(), 3);
        }

        // Удаляем сущность падающего блока
        entity.discard();
    }

    /**
     * Возвращает твёрдый эквивалент этого гравитирующего блока
     */
    public Block getSolidEquivalent() {
        return this.solidEquivalent;
    }
}