package com.hbm_m.mixin;

//? if forge || neoforge {
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Детерминированная перепривязка частей мультиблоков HBM при переносе блоков
 * движком Sable ({@code SubLevelAssemblyHelper.moveBlocks}) — та самая
 * «перезапись ControllerPos на актуальный», но без зависимости от Sable API.
 *
 * <p><b>Зачем:</b> при сборке корабля (Create Aeronautics / физический сборщик)
 * и при разборке обратно в мир каждый блок переезжает на новые координаты,
 * а сохранённый в NBT части {@code ControllerPos} остаётся СТАРЫМ. Радиус-поиск
 * контроллера не годится: мультиблоки бывают 20x20 (двери), а при двух
 * одинаковых станках рядом часть может привязаться к ЧУЖОМУ контроллеру.
 *
 * <p><b>Как:</b> перехватываем запись блока в чанк НАЗНАЧЕНИЯ внутри
 * {@code moveBlocks}. После неё BE уже создан на новой позиции и знает свой
 * уровень — вызываем
 * {@link MultiblockStructureHelper#relinkOrphanedPartDeterministic}, который
 * вычисляет позицию контроллера по формуле
 * {@code controllerPos = partPos - rotate(localOffsetFromController, facing)}
 * без всякого радиус-поиска. Формула верна для любого размера структуры и
 * любого Y-поворота (движок вращает позицию и FACING одним поворотом).
 * Если контроллер ещё не перенесён (порядок блоков в итерации) — проверка не
 * пройдёт, и часть доедет сама: у части есть retry-механизм самолечения
 * (см. {@code UniversalMachinePartBlockEntity.tick}), который повторяет
 * детерминированную проверку первые секунды после загрузки.
 *
 * <p>Записи AIR (очистка origin) игнорируются. Таргет строкой: на сборках без
 * Sable класс не загружается и mixin не применяется (1.20.1 не затронут).
 */
@Mixin(targets = "dev.ryanhcode.sable.api.SubLevelAssemblyHelper")
public abstract class SubLevelRelinkMixin {

    @Redirect(
        method = "moveBlocks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunk;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            remap = true
        ),
        remap = false,
        require = 0
    )
    private void hbm_m$relinkPartAfterMove(LevelChunk chunk, BlockPos pos, BlockState state, boolean flag) {
        chunk.setBlockState(pos, state, flag);

        // Нас интересует только запись НАЗНАЧЕНИЯ реального блока (не очистка AIR).
        if (state.isAir()) {
            return;
        }
        if (!(chunk.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockEntity be = chunk.getBlockEntity(pos);
        if (!(be instanceof IMultiblockPart part)) {
            return;
        }
        // Окно сборки открыто (SubLevelMoveWindowMixin), поэтому каскадов не будет:
        // детерминированная проверка либо перепривяжет часть сразу (контроллер уже
        // перенесён), либо ничего не сделает — тогда сработает retry-самолечение.
        MultiblockStructureHelper.relinkOrphanedPartDeterministic(serverLevel, pos, part);
    }
}
//?}
