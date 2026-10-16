package com.hbm_m.mixin;

//? if forge || neoforge {
import com.hbm_m.multiblock.ContraptionAssemblyGuard;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Открывает {@link ContraptionAssemblyGuard}-окно на время работы Create:
 * <ul>
 *   <li>{@code Contraption.removeBlocksFromWorld} — сборка: блоки уходят в
 *       контрапшен (setBlock AIR, NBT уже захвачен);</li>
 *   <li>{@code Contraption.addBlocksToWorld} — разборка: блоки ставятся обратно
 *       из сохранённого NBT. Внутри окна подавляются наши каскады разрушения,
 *       чтобы конфликтные установки ({@code destroyBlock} занятой позиции)
 *       не запускали дюп/каскад.</li>
 * </ul>
 *
 * <p>Таргет строкой, без compile-зависимости от Create (класс не загружается,
 * пока Create отсутствует — mixin просто не применяется).
 */
@Mixin(targets = "com.simibubi.create.content.contraptions.Contraption")
public abstract class ContraptionWindowMixin {

    static {
        com.hbm_m.main.MainRegistry.LOGGER.info("[HBM][Mixin] ContraptionWindowMixin применён к Contraption");
    }

    @Inject(method = "removeBlocksFromWorld", at = @At("HEAD"), remap = false)
    private void hbm_m$openWindowOnRemoval(Level world, BlockPos offset, CallbackInfo ci) {
        ContraptionAssemblyGuard.push();
    }

    @Inject(method = "removeBlocksFromWorld", at = @At("RETURN"), remap = false)
    private void hbm_m$closeWindowOnRemoval(Level world, BlockPos offset, CallbackInfo ci) {
        ContraptionAssemblyGuard.pop();
    }

    @Inject(method = "addBlocksToWorld", at = @At("HEAD"), remap = false)
    private void hbm_m$openWindowOnPlacement(CallbackInfo ci) {
        ContraptionAssemblyGuard.push();
    }

    @Inject(method = "addBlocksToWorld", at = @At("RETURN"), remap = false)
    private void hbm_m$closeWindowOnPlacement(CallbackInfo ci) {
        ContraptionAssemblyGuard.pop();
    }
}
//?}
