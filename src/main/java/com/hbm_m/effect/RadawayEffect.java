package com.hbm_m.effect;

import com.hbm_m.effect.render.RadawayEffectRenderer;
import com.hbm_m.radiation.PlayerHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

//? if forge {
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
//?}

public class RadawayEffect extends MobEffect {

    // Amplifier 0 → ~0.583 rad/tick, Amplifier 1 → ~1.167 rad/tick
    private static final float RADAWAY_POWER = 140.0F / 120.0F;

    public RadawayEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity instanceof Player player && !entity.level().isClientSide()) {
            PlayerHandler.decrementPlayerRads(player, (amplifier + 1) * RADAWAY_POWER);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    // На Fabric рендеринг делается через Mixin (см. MixinEffectRenderingInventoryScreen,
    // MixinGui) — они вызывают RadawayEffectRenderer напрямую.
    //? if forge {
    @Override
    public void initializeClient(@NotNull Consumer<IClientMobEffectExtensions> consumer) {
        consumer.accept(new IClientMobEffectExtensions() {

            @Override
            public boolean renderInventoryIcon(MobEffectInstance instance,
                                               EffectRenderingInventoryScreen<?> screen,
                                               GuiGraphics gfx, int x, int y, int blitOffset) {
                RadawayEffectRenderer.renderInventory(gfx, x, y, blitOffset);
                return true;
            }

            @Override
            public boolean renderGuiIcon(MobEffectInstance instance,
                    net.minecraft.client.gui.Gui gui,
                    GuiGraphics gfx, int x, int y, float z, float alpha) {
                RadawayEffectRenderer.renderHud(gfx, x, y, (int) z, alpha);
                return true;
            }
        });
    }
    //?}
}