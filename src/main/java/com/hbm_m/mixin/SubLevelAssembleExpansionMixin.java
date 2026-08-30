package com.hbm_m.mixin;

//? if forge || neoforge {
import com.hbm_m.compat.create.MultiblockExpander;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Расширяет набор блоков, собираемых в корабль (Sable sublevel), до полного
 * мультиблока HBM.
 *
 * <p><b>Проблема:</b> физический сборщик (Physics Assembler / Swivel Bearing из
 * Create Aeronautics) собирает блоки flood-fill'ом: клей + структурная
 * привязанность по правилам Create. Фантомные части мультиблока HBM «непривязаны»,
 * поэтому если игрок выделил клеем только контроллер или одну часть — в корабль
 * уходит ровно один блок. При его переносе остальная структура в мире остаётся с
 * дырой (каскад разрушения подавляется гвардом окна сборки), а на корабле машина
 * неработоспособна.
 *
 * <p><b>Решение:</b> {@code SubLevelAssemblyHelper.assembleBlocks} — единая точка,
 * через которую ВСЕ сборщики Sable-экосистемы передают готовый набор мировых
 * позиций ({@code SimAssemblyHelper.assembleFromSingleBlock} → сюда). На HEAD
 * расширяем набор через {@link MultiblockExpander#expandToFullMultiblock}: если
 * среди блоков есть хоть одна часть/контроллер мультиблока HBM — добавляем все
 * остальные части и контроллер. Мультиблок становится физичным как единое целое.
 * Для клея то же самое уже делают {@code SuperGlueSelectionHelperMixin}
 * (клиентская выборка) и {@code ContraptionMixin} (BFS самого Create).
 *
 * <p>Таргет строкой: без Sable класс не загружается и mixin не применяется
 * (1.20.1 не затронут).
 */
@Mixin(targets = "dev.ryanhcode.sable.api.SubLevelAssemblyHelper")
public abstract class SubLevelAssembleExpansionMixin {

    @Inject(method = "assembleBlocks", at = @At("HEAD"), remap = false, require = 0)
    private void hbm_m$expandToFullMultiblock(ServerLevel level, BlockPos anchor,
                                              Iterable<BlockPos> blocks, CallbackInfo ci) {
        if (!(blocks instanceof Set<BlockPos> set) || set.isEmpty()) {
            return;
        }
        // Набор — мировые позиции (ObjectOpenHashSet из SimAssemblyContraption).
        Set<BlockPos> expanded = MultiblockExpander.expandToFullMultiblock(level, set);
        if (expanded.size() > set.size()) {
            set.addAll(expanded);
        }
    }
}
//?}
