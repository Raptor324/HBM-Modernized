package com.hbm_m.mixin;

//? if forge || neoforge {
import com.hbm_m.multiblock.ContraptionAssemblyGuard;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Открывает {@link ContraptionAssemblyGuard}-окно на время
 * {@code SubLevelAssemblyHelper.moveBlocks} (Sable).
 *
 * <p>Это ЕДИНАЯ точка переноса блоков для всей экосистемы Sable:
 * <ul>
 *   <li>сборка корабля физическим сборщиком / swivel bearing
 *       ({@code SimAssemblyHelper.assembleFromSingleBlock} →
 *       {@code SubLevelAssemblyHelper.assembleBlocks} → {@code moveBlocks});</li>
 *   <li>разборка корабля обратно в мир
 *       ({@code SimAssemblyHelper.disassembleSubLevel} → {@code moveBlocks}).</li>
 * </ul>
 *
 * <p>Внутри окна Sable удаляет блоки из мира прямой записью чанка
 * ({@code LevelChunk.setBlockState(pos, AIR)}) — это тоже вызывает onRemove
 * старого состояния, поэтому окно необходимо и здесь (см.
 * {@link LevelChunkSilentRemovalMixin}).
 *
 * <p>Таргет строкой, без compile-зависимости от Sable.
 */
@Mixin(targets = "dev.ryanhcode.sable.api.SubLevelAssemblyHelper")
public abstract class SubLevelMoveWindowMixin {

    static {
        com.hbm_m.main.MainRegistry.LOGGER.info("[HBM][Mixin] SubLevelMoveWindowMixin применён к SubLevelAssemblyHelper");
    }

    @Inject(method = "moveBlocks", at = @At("HEAD"), remap = false)
    private void hbm_m$openWindowOnMove(CallbackInfo ci) {
        ContraptionAssemblyGuard.push();
    }

    @Inject(method = "assembleBlocks", at = @At("HEAD"), remap = false)
    private void hbm_m$openWindowOnAssemble(CallbackInfo ci) {
        ContraptionAssemblyGuard.push();
    }

    @Inject(method = "assembleBlocks", at = @At("RETURN"), remap = false)
    private void hbm_m$closeWindowOnAssemble(CallbackInfo ci) {
        ContraptionAssemblyGuard.pop();
    }

    @Inject(method = "moveBlocks", at = @At("RETURN"), remap = false)
    private void hbm_m$closeWindowOnMove(CallbackInfo ci) {
        ContraptionAssemblyGuard.pop();
    }
}
//?}
