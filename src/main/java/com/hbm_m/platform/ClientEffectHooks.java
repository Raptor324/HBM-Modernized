package com.hbm_m.platform;

import java.util.function.Consumer;

import com.hbm_m.effect.RadawayEffect;
import com.hbm_m.effect.TaintEffect;
import com.hbm_m.effect.render.RadawayEffectRenderer;
import com.hbm_m.effect.render.TaintEffectRenderer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Клиентские мосты для MobEffect: кастомные иконки HUD и экрана инвентаря
 * (Radaway / Taint) через IClientMobEffectExtensions.
 *
 * <p>ТОЛЬКО КЛИЕНТ — методы вызываются из {@code MobEffect.initializeClient},
 * который сам дергается только клиентом. До выноса сюда ветка neoforge
 * отсутствовала, и на 1.21.1 иконки radaway/taint не рисовались вовсе.
 * Класс эквивалентен {@link ClientEffectHooks}-паттерну PlatformHooks:
 * весь loader-gating собран здесь, эффект-классы остаются чистыми.
 */
public final class ClientEffectHooks {

    private ClientEffectHooks() {
    }

    /**
     * Регистрирует клиентские расширения иконок для эффектов мода.
     * Вызывается из переопределённого {@code initializeClient} эффектов;
     * {@code consumer} приводится к лоадер-специфичному типу внутри гейта.
     */
    public static void initializeClient(MobEffect effect, Consumer<Object> consumer) {
        //? if forge {
        if (effect instanceof RadawayEffect) {
            consumer.accept(radawayExtensionsForge());
        } else if (effect instanceof TaintEffect) {
            consumer.accept(taintExtensionsForge());
        }
        //?} elif neoforge {
        /*if (effect instanceof RadawayEffect) {
            consumer.accept(radawayExtensionsNeo());
        } else if (effect instanceof TaintEffect) {
            consumer.accept(taintExtensionsNeo());
        }
        *///?}
    }

    //? if forge {
    private static net.minecraftforge.client.extensions.common.IClientMobEffectExtensions radawayExtensionsForge() {
        return new net.minecraftforge.client.extensions.common.IClientMobEffectExtensions() {
            @Override
            public boolean renderInventoryIcon(MobEffectInstance instance,
                    EffectRenderingInventoryScreen<?> screen, GuiGraphics gfx, int x, int y, int blitOffset) {
                RadawayEffectRenderer.renderInventory(gfx, x, y, blitOffset);
                return true;
            }

            @Override
            public boolean renderGuiIcon(MobEffectInstance instance, net.minecraft.client.gui.Gui gui,
                    GuiGraphics gfx, int x, int y, float z, float alpha) {
                RadawayEffectRenderer.renderHud(gfx, x, y, (int) z, alpha);
                return true;
            }
        };
    }

    private static net.minecraftforge.client.extensions.common.IClientMobEffectExtensions taintExtensionsForge() {
        return new net.minecraftforge.client.extensions.common.IClientMobEffectExtensions() {
            @Override
            public boolean renderInventoryIcon(MobEffectInstance instance,
                    EffectRenderingInventoryScreen<?> screen, GuiGraphics gfx, int x, int y, int blitOffset) {
                TaintEffectRenderer.renderInventory(gfx, x, y, blitOffset);
                return true;
            }

            @Override
            public boolean renderGuiIcon(MobEffectInstance instance, net.minecraft.client.gui.Gui gui,
                    GuiGraphics gfx, int x, int y, float z, float alpha) {
                TaintEffectRenderer.renderHud(gfx, x, y, (int) z, alpha);
                return true;
            }
        };
    }
    //?} elif neoforge {
    /*private static net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions radawayExtensionsNeo() {
        return new net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions() {
            @Override
            public boolean renderInventoryIcon(MobEffectInstance instance,
                    EffectRenderingInventoryScreen<?> screen, GuiGraphics gfx, int x, int y, int blitOffset) {
                RadawayEffectRenderer.renderInventory(gfx, x, y, blitOffset);
                return true;
            }

            @Override
            public boolean renderGuiIcon(MobEffectInstance instance, net.minecraft.client.gui.Gui gui,
                    GuiGraphics gfx, int x, int y, float z, float alpha) {
                RadawayEffectRenderer.renderHud(gfx, x, y, (int) z, alpha);
                return true;
            }
        };
    }

    private static net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions taintExtensionsNeo() {
        return new net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions() {
            @Override
            public boolean renderInventoryIcon(MobEffectInstance instance,
                    EffectRenderingInventoryScreen<?> screen, GuiGraphics gfx, int x, int y, int blitOffset) {
                TaintEffectRenderer.renderInventory(gfx, x, y, blitOffset);
                return true;
            }

            @Override
            public boolean renderGuiIcon(MobEffectInstance instance, net.minecraft.client.gui.Gui gui,
                    GuiGraphics gfx, int x, int y, float z, float alpha) {
                TaintEffectRenderer.renderHud(gfx, x, y, (int) z, alpha);
                return true;
            }
        };
    }
     *///?}
}
