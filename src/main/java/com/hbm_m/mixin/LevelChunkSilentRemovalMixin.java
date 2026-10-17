package com.hbm_m.mixin;

import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.ContraptionAssemblyGuard;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Collection;
import java.util.function.Consumer;

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
        com.hbm_m.main.MainRegistry.LOGGER.info("[HBM][Mixin] LevelChunkSilentRemovalMixin applied to LevelChunk");
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
            // IMultiblockPart is implemented by the part's BlockEntity, not by its Block, so the old
            // test against the Block never matched and only the controller half of this guard worked.
            Block receiverBlock = receiver.getBlock();
            boolean receiverOurs = receiverBlock instanceof IMultiblockController
                    || receiverBlock instanceof com.hbm_m.block.UniversalMachinePartBlock;
            if (!receiverOurs && receiver.isAir()) {
                Block otherBlock = otherState.getBlock();
                receiverOurs = otherBlock instanceof IMultiblockController
                        || otherBlock instanceof com.hbm_m.block.UniversalMachinePartBlock;
            }
            if (receiverOurs) {
                com.hbm_m.main.MainRegistry.LOGGER.debug(
                    "[HBM] onRemove подавлен при переносе, блок {} @ {}", receiverBlock, pos.toShortString());
                // Only the multiblock cascade is skipped. Block.onRemove is also the one place that drops
                // the block entity out of the chunk; without this the BE stayed under the air block
                // ("invalid for ticking" every tick, "Failed to create block entity ... got air" on the
                // next chunk load). Sable has already serialized it for the sub-level by now.
                if (receiver.hasBlockEntity() && !receiver.is(otherState.getBlock())) {
                    level.removeBlockEntity(pos);
                }
                return; // Перенос блока движком сборки — разрушение подавляем.
            }
        }
        receiver.onRemove(level, pos, otherState, isMoving);
    }

    /**
     * Prevents {@link java.util.ConcurrentModificationException} when clearing
     * block entities during chunk unload / bulk chunk resend (e.g. nuke crater
     * light updates).
     *
     * <p>In vanilla {@code LevelChunk#clearAllBlockEntities()}:
     * <pre>
     * this.blockEntities.values().forEach(BlockEntity::onChunkUnloaded);
     * this.blockEntities.values().forEach(BlockEntity::setRemoved);
     * this.blockEntities.clear();
     * </pre>
     * As block entities execute {@code setRemoved()}, multiblock part BEs trigger
     * controller teardown hooks (or other mods execute block queries). Any lookup
     * calling {@code chunk.getBlockEntity(pos, IMMEDIATE)} detects that a removed
     * BE is marked as removed and invokes {@code this.blockEntities.remove(pos)},
     * mutating the map mid-iteration of {@code values().forEach(...)}.
     *
     * <p>Redirecting the {@code Collection.forEach} invocation to iterate over a
     * snapshot array ({@code collection.toArray()}) makes iteration completely
     * immune to concurrent map mutations during the clear loop.
     */
    @Redirect(
        method = "clearAllBlockEntities",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Collection;forEach(Ljava/util/function/Consumer;)V"
        )
    )
    private <T> void hbm_m$safeForEachInClear(Collection<T> collection, Consumer<? super T> action) {
        for (Object item : collection.toArray()) {
            @SuppressWarnings("unchecked")
            T casted = (T) item;
            action.accept(casted);
        }
    }
}