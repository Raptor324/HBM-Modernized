package com.hbm_m.mixin;

//? if forge || neoforge {
import com.hbm_m.compat.create.MultiblockExpander;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    /**
     * Signature mirrors Sable 2.0.5 exactly: the method is {@code public static} and returns
     * {@code ServerSubLevel}, and it takes a fourth {@code BoundingBox3ic} argument. The previous
     * non-static, three-argument, CallbackInfo form could not apply at all, so the expansion never
     * ran. Sable is already a compileOnly dependency, so the types can be named here - the same
     * shape waystonessable uses for the same method.
     */
    @Inject(method = "assembleBlocks", at = @At("HEAD"), remap = false, require = 0)
    private static void hbm_m$expandToFullMultiblock(
            ServerLevel level,
            BlockPos anchor,
            Iterable<BlockPos> blocks,
            // Sable exists only on 1.21.1 and its companion jar is built for Java 21, so it cannot
            // be on the 1.20.1 classpath at all. The mixin never applies there (the target class is
            // absent), so the parameter type is irrelevant - but naming the class broke compileJava
            // on 1.20.1, and with it the whole datagen run.
            //? if >= 1.21.1 {
            /*dev.ryanhcode.sable.companion.math.BoundingBox3ic bounds,
            *///?} else {
            Object bounds,
            //?}
            CallbackInfoReturnable<?> cir) {
        if (!(blocks instanceof Set<BlockPos> set) || set.isEmpty()) {
            return;
        }
        // Набор — мировые позиции (ObjectOpenHashSet из SimAssemblyContraption).
        // The set belongs to the caller: if it ever hands us an immutable one, addAll throws and
        // would abort the assembly. Expansion is an enhancement, never a reason to fail the move.
        try {
            Set<BlockPos> expanded = MultiblockExpander.expandToFullMultiblock(level, set);
            if (expanded.size() > set.size()) {
                set.addAll(expanded);
            }
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.warn(
                    "[HBM] Не удалось расширить набор сборки до полного мультиблока", t);
        }
    }
}
//?}
