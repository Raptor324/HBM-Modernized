//? if fabric {
package com.hbm_m.mixin.client;

import com.hbm_m.effect.RadawayEffect;
import com.hbm_m.effect.render.RadawayEffectRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Iterator;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric: иконка Radaway на HUD.
 * В 1.20.1 {@code Gui#renderEffects} собирает отложенные {@link Runnable}, иконка рисуется в
 * {@code lambda$renderEffects$0} через {@code blit(..., TextureAtlasSprite)} — без обёртки Runnable теряется
 * связь с {@link MobEffectInstance} на момент отрисовки.
 */
@Mixin(Gui.class)
public abstract class MixinGui {

    @Unique
    private static final ThreadLocal<MobEffectInstance> HBM_RADAWAY_HUD_CURRENT = new ThreadLocal<>();

    @Inject(
            method = "renderEffects(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At("HEAD")
    )
    private void hbm_m$clearHudEffect(CallbackInfo ci) {
        HBM_RADAWAY_HUD_CURRENT.remove();
    }

    @WrapOperation(
            method = "renderEffects(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;", remap = false)
    )
    private Object hbm_m$trackHudLoopIteratorNext(Iterator<?> iterator, Operation<Object> original) {
        Object next = original.call(iterator);
        if (next instanceof MobEffectInstance inst) {
            HBM_RADAWAY_HUD_CURRENT.set(inst);
        }
        return next;
    }

    /**
     * Перед {@code List.add(Runnable)} поток уже содержит текущий {@code MobEffectInstance} из {@link #hbm_m$trackHudLoopIteratorNext};
     * оборачиваем Runnable, чтобы при отложенном {@code forEach} восстановить его для {@link #hbm_m$wrapHudEffectSpriteBlit}.
     */
    @ModifyArg(
            method = "renderEffects(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false),
            index = 0
    )
    private Object hbm_m$captureEffectForDeferredHud(Object runnable) {
        if (!(runnable instanceof Runnable delegate)) {
            return runnable;
        }
        MobEffectInstance inst = HBM_RADAWAY_HUD_CURRENT.get();
        if (inst == null || !(inst.getEffect() instanceof RadawayEffect)) {
            return runnable;
        }
        return (Runnable) () -> {
            MobEffectInstance prev = HBM_RADAWAY_HUD_CURRENT.get();
            HBM_RADAWAY_HUD_CURRENT.set(inst);
            try {
                delegate.run();
            } finally {
                if (prev != null) {
                    HBM_RADAWAY_HUD_CURRENT.set(prev);
                } else {
                    HBM_RADAWAY_HUD_CURRENT.remove();
                }
            }
        };
    }

    @WrapOperation(
            method = "renderEffects(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(IIIIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"
            ),
            require = 0
    )
    private void hbm_m$wrapHudEffectSpriteBlit(
            GuiGraphics gfx,
            int x,
            int y,
            int blitOffset,
            int width,
            int height,
            TextureAtlasSprite sprite,
            Operation<Void> original
    ) {
        MobEffectInstance inst = HBM_RADAWAY_HUD_CURRENT.get();
        if (inst != null && inst.getEffect() instanceof RadawayEffect) {
            RadawayEffectRenderer.renderHud(gfx, x - 2, y - 2, blitOffset, 1f);
            return;
        }
        original.call(gfx, x, y, blitOffset, width, height, sprite);
    }

    /**
     * Некоторые маппинги/версии рисуют HUD-эффекты через overload с tint (RGBA), а не через простой blit.
     * Делаем второй перехват на всякий случай, чтобы не зависеть от конкретного ванильного пути.
     */
    @WrapOperation(
            method = "renderEffects(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(IIIIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;FFFF)V"
            ),
            require = 0
    )
    private void hbm_m$wrapHudEffectSpriteBlitTinted(
            GuiGraphics gfx,
            int x,
            int y,
            int blitOffset,
            int width,
            int height,
            TextureAtlasSprite sprite,
            float red,
            float green,
            float blue,
            float alpha,
            Operation<Void> original
    ) {
        MobEffectInstance inst = HBM_RADAWAY_HUD_CURRENT.get();
        if (inst != null && inst.getEffect() instanceof RadawayEffect) {
            RadawayEffectRenderer.renderHud(gfx, x - 2, y - 2, blitOffset, alpha);
            return;
        }
        original.call(gfx, x, y, blitOffset, width, height, sprite, red, green, blue, alpha);
    }
}
//?}
