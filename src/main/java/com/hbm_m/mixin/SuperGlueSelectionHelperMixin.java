package com.hbm_m.mixin;

//? if forge || neoforge {
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hbm_m.compat.create.MultiblockExpander;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Миксин в {@code SuperGlueSelectionHelper.searchGlueGroup}.
 * <p>
 * <b>Проблема:</b> Когда игрок использует Супер-Клей (Create) на части мультиблока HBM,
 * клей захватывает только те блоки, которые соединены клеем (или находятся в bounding box'е
 * клея). Остальные части мультиблока не попадают в выборку, что разрывает мультиблок.
 * </p>
 * <p>
 * <b>Решение:</b> После завершения BFS-поиска клея расширяем полученный набор позиций,
 * добавляя ВСЕ части любого мультиблока HBM, если хотя бы одна его часть/контроллер
 * попала в результаты клея.
 * </p>
 * <p>
 * Используется {@link MultiblockExpander#expandToFullMultiblock(Level, java.util.Collection)}.
 * </p>
 */
@Mixin(targets = "com.simibubi.create.content.contraptions.glue.SuperGlueSelectionHelper")
public abstract class SuperGlueSelectionHelperMixin {

    @Inject(
        method = "searchGlueGroup",
        at = @At("RETURN"),
        remap = false,
        require = 1
    )
    private static void hbm_m$expandGlueGroupToFullMultiblock(
            Level level,
            BlockPos startPos,
            BlockPos endPos,
            boolean includeOther,
            CallbackInfoReturnable<Set<BlockPos>> cir
    ) {
        Set<BlockPos> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) {
            return;
        }

        // Расширяем набор: если хотя бы один блок — часть мультиблока HBM,
        // добавляем все остальные части + контроллер. Ошибка расширения не должна
        // ломать само выделение клеем.
        try {
            Set<BlockPos> expanded = MultiblockExpander.expandToFullMultiblock(level, original);
            if (!expanded.equals(original)) {
                cir.setReturnValue(expanded);
            }
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.warn(
                    "[HBM] Не удалось расширить выделение клея до полного мультиблока", t);
        }
    }
}
//?}