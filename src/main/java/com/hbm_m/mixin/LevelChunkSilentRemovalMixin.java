package com.hbm_m.mixin;

//? if forge || neoforge {
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.multiblock.ContraptionAssemblyGuard;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Глушит {@code BlockState#onRemove} для блоков мультиблоков HBM, пока идёт
 * перенос блоков движком сборки (Create / Sable / Aeronautics) — см.
 * {@link ContraptionAssemblyGuard}.
 *
 * <p><b>Проблема (дюп станка):</b> оба движка удаляют захваченные блоки из мира:
 * <ul>
 *   <li>Create: {@code Contraption.removeBlocksFromWorld} → {@code setBlock(AIR)};</li>
 *   <li>Sable: {@code SubLevelAssemblyHelper.moveBlocks} → прямая запись
 *       {@code LevelChunk.setBlockState(pos, AIR)}, которая тоже вызывает
 *       {@code onRemove} старого состояния.</li>
 * </ul>
 * Наш {@code UniversalMachinePartBlock#onRemove} при живом контроллере запускает
 * каскад: {@code destroyStructure} + {@code destroyBlock(controllerPos, true)}
 * (дроп станка лут-таблицей), а контроллерные блоки в {@code onRemove} дропают
 * содержимое инвентаря ({@code be.drops()}). При этом движок уже сохранил
 * state+NBT и вернёт всё на месте разборки → двойной набор предметов.
 *
 * <p>Решение: пока окно сборки открыто, onRemove наших блоков не вызывается.
 * Ванильные блоки и все остальные моды не затрагиваются.
 *
 * <p>Примечание по семантике: в разных версиях vanilla вызывает onRemove то ли
 * на старом состоянии (приёмник = удаляемый блок), то ли на новом (приёмник =
 * AIR). Проверяем ОБА варианта: приёмник — наш блок, ИЛИ приёмник — воздух,
 * а второй аргумент — наш блок.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkSilentRemovalMixin {

    static {
        com.hbm_m.main.MainRegistry.LOGGER.info("[HBM][Mixin] LevelChunkSilentRemovalMixin применён к LevelChunk");
    }

    @Redirect(
        method = "setBlockState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;onRemove(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V"
        )
    )
    private void hbm_m$silentRemovalDuringContraptionMove(BlockState receiver, Level level, BlockPos pos,
                                                          BlockState otherState, boolean isMoving) {
        if (ContraptionAssemblyGuard.isMoving()) {
            Block receiverBlock = receiver.getBlock();
            boolean receiverOurs = receiverBlock instanceof IMultiblockPart || receiverBlock instanceof IMultiblockController;
            if (!receiverOurs && receiver.isAir()) {
                Block otherBlock = otherState.getBlock();
                receiverOurs = otherBlock instanceof IMultiblockPart || otherBlock instanceof IMultiblockController;
            }
            if (receiverOurs) {
                com.hbm_m.main.MainRegistry.LOGGER.info(
                    "[HBM] onRemove подавлен при переносе, блок {} @ {}", receiverBlock, pos.toShortString());
                return; // Перенос блока движком сборки — разрушение подавляем.
            }
        }
        receiver.onRemove(level, pos, otherState, isMoving);
    }
}
//?}
