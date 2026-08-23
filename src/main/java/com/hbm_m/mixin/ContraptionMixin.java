package com.hbm_m.mixin;

//? if forge || neoforge {
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hbm_m.compat.create.MultiblockExpander;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.interfaces.IMultiblockPart;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/**
 * Миксин в {@code Contraption.searchMovedStructure}.
 * <p>
 * <b>Проблема:</b> Когда Create собирает контрапшен (механический поршень, клей,
 * подъёмник, Physics Assembler из Aeronautics/Sable), он BFS-ом обходит блоки,
 * начиная с якоря. Если якорь — часть мультиблока HBM (контроллер или фантомная часть),
 * Create захватывает только этот блок, а остальные части мультиблока остаются в мире.
 * Это разрывает мультиблок: одна часть улетает на контрапшене, остальные стоят.
 * </p>
 * <p>
 * <b>Решение:</b> После завершения BFS ({@code searchMovedStructure}) пробегаем по
 * захваченным блокам ({@code this.blocks}). Для каждого блока, который является частью
 * мультиблока HBM (IMultiblockPart или IMultiblockController), находим контроллер и
 * добавляем ВСЕ его части в карту blocks. Таким образом весь мультиблок становится
 * единым контрапшеном.
 * </p>
 * <p>
 * Важно: добавление блоков в {@code this.blocks} происходит через {@code this.addBlock},
 * но мы не можем его вызвать (он требует {@code Pair<StructureBlockInfo, BlockEntity>}).
 * Вместо этого создаём {@code StructureBlockInfo} напрямую с захваченным состоянием блока
 * и null-NBT. Это допустимо, т.к. Create сам обрабатывает NBT для BlockEntity позже.
 * </p>
 */
@Mixin(targets = "com.simibubi.create.content.contraptions.Contraption")
public abstract class ContraptionMixin {

    @Shadow
    protected java.util.Map<BlockPos, StructureBlockInfo> blocks;

    @Shadow
    protected BlockPos anchor;

    @Shadow
    protected abstract boolean customBlockPlacement(net.minecraft.world.level.LevelAccessor world, BlockPos pos, net.minecraft.world.level.block.state.BlockState state);

    @Shadow
    protected abstract boolean customBlockRemoval(net.minecraft.world.level.LevelAccessor world, BlockPos pos, net.minecraft.world.level.block.state.BlockState state);

    /**
     * Перехватывает {@code searchMovedStructure} после успешного завершения BFS.
     * Расширяет набор захваченных блоков, добавляя все части мультиблоков HBM.
     */
    @Inject(
        method = "searchMovedStructure",
        at = @At("RETURN"),
        remap = false,
        require = 1
    )
    private void hbm_m$expandToFullMultiblockAfterSearch(
            Level world,
            BlockPos pos,
            net.minecraft.core.Direction forcedDirection,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValue()) {
            return; // Сборка не удалась — не расширяем
        }

        // Собираем все контроллеры, которые уже захвачены
        Set<BlockPos> controllersToExpand = new HashSet<>();
        for (BlockPos localPos : this.blocks.keySet()) {
            BlockPos worldPos = localPos.offset(this.anchor);
            var be = world.getBlockEntity(worldPos);
            if (be instanceof IMultiblockPart part) {
                BlockPos controllerPos = part.getControllerPos();
                if (controllerPos != null && !controllersToExpand.contains(controllerPos)) {
                    // Проверяем, что controllerPos уже захвачен или будет захвачен
                    BlockPos localController = controllerPos.subtract(this.anchor);
                    if (this.blocks.containsKey(localController)) {
                        controllersToExpand.add(controllerPos);
                    }
                }
            } else if (be instanceof IMultiblockController) {
                controllersToExpand.add(worldPos);
            }
        }

        if (controllersToExpand.isEmpty()) {
            return; // Нет мультиблоков HBM среди захваченных блоков
        }

        // Для каждого найденного контроллера добавляем недостающие части
        for (BlockPos controllerPos : controllersToExpand) {
            BlockPos localController = controllerPos.subtract(this.anchor);
            Collection<BlockPos> allPartPositions = MultiblockExpander.getAllMultiblockPositions(world, controllerPos);
            if (allPartPositions == null) continue;

            for (BlockPos partWorldPos : allPartPositions) {
                BlockPos localPart = partWorldPos.subtract(this.anchor);
                if (this.blocks.containsKey(localPart)) {
                    continue; // Уже захвачен
                }

                // Захватываем блок: создаём StructureBlockInfo с текущим состоянием
                var blockState = world.getBlockState(partWorldPos);
                var blockEntity = world.getBlockEntity(partWorldPos);
                net.minecraft.nbt.CompoundTag nbt = null;
                if (blockEntity != null) {
                    //? if < 1.21 {
                    nbt = blockEntity.saveWithoutMetadata();
                    //?} else {
                    /*nbt = blockEntity.saveWithoutMetadata(world.registryAccess());
                     *///?}
                }
                StructureBlockInfo info = new StructureBlockInfo(localPart, blockState, nbt);
                this.blocks.put(localPart, info);

                // Также нужно добавить в storage если есть инвентарь
                // (для мультиблоков HBM обычно нет инвентаря на частях, но на всякий случай)
                if (blockEntity != null) {
                    this.storage.addBlock(world, blockState, partWorldPos, localPart, blockEntity);
                }
            }
        }
    }

    @Shadow
    private com.simibubi.create.content.contraptions.MountedStorageManager storage;
}
//?}