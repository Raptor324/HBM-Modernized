//? if fabric {
/*package com.hbm_m.mixin.client;

import com.hbm_m.effect.RadawayEffect;
import com.hbm_m.effect.render.RadawayEffectRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Iterator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * Fabric: кастомная иконка Radaway в списке эффектов инвентаря.
 * На Forge используется {@link com.hbm_m.effect.RadawayEffect#initializeClient}.
 *
 * <p>1.20.1: {@code @ModifyVariable} на {@code Iterator.next()}+{@code INVOKE_ASSIGN} не срабатывает — javac кладёт
 * результат сразу в {@code MobEffectInstance} через {@code checkcast}, без локала {@code Object}.
 * Иконки рисуются через {@link GuiGraphics#blit(int, int, int, int, int, TextureAtlasSprite)}, а не через {@code blit(RL,...)}.</p>
 ^/
@Mixin(EffectRenderingInventoryScreen.class)
public abstract class MixinEffectRenderingInventoryScreen {

    @Unique
    private static final ThreadLocal<MobEffectInstance> HBM_RADAWAY_CURRENT = new ThreadLocal<>();

    @Inject(
            method = "renderIcons(Lnet/minecraft/client/gui/GuiGraphics;IILjava/lang/Iterable;Z)V",
            at = @At("HEAD")
    )
    private void hbm_m$clearCurrentEffect(CallbackInfo ci) {
        HBM_RADAWAY_CURRENT.remove();
    }

    @WrapOperation(
            method = "renderIcons(Lnet/minecraft/client/gui/GuiGraphics;IILjava/lang/Iterable;Z)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;", remap = false)
    )
    private Object hbm_m$trackIconsLoopIteratorNext(Iterator<?> iterator, Operation<Object> original) {
        Object next = original.call(iterator);
        if (next instanceof MobEffectInstance inst) {
            HBM_RADAWAY_CURRENT.set(inst);
        }
        return next;
    }

    /^*
     * 1.20.1: {@code renderIcons} бьёт спрайт из атласа эффектов
     * {@code blit(x, y, blitOffset, 18, 18, TextureAtlasSprite)} — x/y это левый верх 18×18 (на 3px внутри ячейки 24×24).
     ^/
    @WrapOperation(
            method = "renderIcons(Lnet/minecraft/client/gui/GuiGraphics;IILjava/lang/Iterable;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(IIIIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"
            )
    )
    private void hbm_m$wrapEffectIconSpriteBlit(
            GuiGraphics gfx,
            int x,
            int y,
            int blitOffset,
            int width,
            int height,
            TextureAtlasSprite sprite,
            Operation<Void> original
    ) {
        MobEffectInstance inst = HBM_RADAWAY_CURRENT.get();
        if (inst != null && inst.getEffect() instanceof RadawayEffect) {
            RadawayEffectRenderer.renderInventory(gfx, x - 3, y - 3, blitOffset);
            return;
        }
        original.call(gfx, x, y, blitOffset, width, height, sprite);
    }
}
*///?}
