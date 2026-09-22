package com.hbm_m.mixin;

//? if forge || neoforge {
import com.hbm_m.multiblock.ContraptionAssemblyGuard;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        com.hbm_m.main.MainRegistry.LOGGER.info("[HBM][Mixin] SubLevelMoveWindowMixin applied to SubLevelAssemblyHelper");
    }

    // All four handlers are static and the assembleBlocks pair takes CallbackInfoReturnable:
    // Sable declares both methods as `public static`, and assembleBlocks returns ServerSubLevel.
    // Mixin requires a static handler for a static target and CallbackInfoReturnable for a
    // non-void one, so the previous non-static/CallbackInfo forms could not apply at all - the
    // assembly guard never opened, and the multiblock destruction cascade ran during ship
    // assembly and disassembly. Verified against SubLevelAssemblyHelper in Sable 2.0.5.
    @Inject(method = "moveBlocks", at = @At("HEAD"), remap = false, require = 0)
    private static void hbm_m$openWindowOnMove(CallbackInfo ci) {
        ContraptionAssemblyGuard.push();
    }

    @Inject(method = "assembleBlocks", at = @At("HEAD"), remap = false, require = 0)
    private static void hbm_m$openWindowOnAssemble(CallbackInfoReturnable<?> cir) {
        ContraptionAssemblyGuard.push();
    }

    @Inject(method = "assembleBlocks", at = @At("RETURN"), remap = false, require = 0)
    private static void hbm_m$closeWindowOnAssemble(CallbackInfoReturnable<?> cir) {
        ContraptionAssemblyGuard.pop();
    }

    @Inject(method = "moveBlocks", at = @At("RETURN"), remap = false, require = 0)
    private static void hbm_m$closeWindowOnMove(CallbackInfo ci) {
        ContraptionAssemblyGuard.pop();
    }
}
//?}
