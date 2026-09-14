package com.hbm_m.effect;

import java.util.function.Consumer;

import com.hbm_m.effect.render.PotionSheetRenderer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

//? if forge {
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
//?}

/**
 * Gemeinsame Basis der aus {@code HbmPotion} portierten Effekte: Farbe, Kategorie und das Symbol
 * aus {@code potions.png}.
 *
 * <p>Die Kachelkoordinaten sind dieselben wie im {@code setIconIndex(x, y)} des Originals.
 * Standardmaessig tickt ein Effekt nie ({@code isReady} gibt im Original fuer die reinen
 * Markiereffekte {@code false} zurueck) - wer etwas tun will, ueberschreibt beides.</p>
 */
public abstract class HbmEffect extends MobEffect {

    private final int iconU;
    private final int iconV;

    protected HbmEffect(MobEffectCategory category, int color, int iconX, int iconY) {
        super(category, color);
        this.iconU = PotionSheetRenderer.u(iconX);
        this.iconV = PotionSheetRenderer.v(iconY);
    }

    /**
     * Original: {@code HbmPotion.performEffect}. Wird nur serverseitig aufgerufen und nur dann,
     * wenn {@link #isReady(int, int)} zugestimmt hat. Markiereffekte lassen die Methode leer.
     */
    protected void tick(@NotNull LivingEntity entity, int amplifier) {}

    /** Original: {@code HbmPotion.isReady} - reine Markiereffekte tun pro Tick nichts. */
    protected boolean isReady(int duration, int amplifier) {
        return false;
    }

    // Die beiden Haken heissen ab 1.21.1 anders und geben dort ein boolean zurueck; die eigentliche
    // Logik steht deshalb in tick(...) bzw. isReady(...) und wird hier nur durchgereicht.
    //? if < 1.21.1 {
    @Override
    public void applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return;
        tick(entity, amplifier);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return isReady(duration, amplifier);
    }
    //?} else {
    /*@Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return true;
        tick(entity, amplifier);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return isReady(duration, amplifier);
    }
    *///?}

    //? if forge {
    @Override
    public void initializeClient(@NotNull Consumer<IClientMobEffectExtensions> consumer) {
        consumer.accept(new IClientMobEffectExtensions() {

            @Override
            public boolean renderInventoryIcon(MobEffectInstance instance,
                                               EffectRenderingInventoryScreen<?> screen,
                                               GuiGraphics gfx, int x, int y, int blitOffset) {
                PotionSheetRenderer.renderInventory(gfx, iconU, iconV, x, y, blitOffset);
                return true;
            }

            @Override
            public boolean renderGuiIcon(MobEffectInstance instance,
                                         net.minecraft.client.gui.Gui gui,
                                         GuiGraphics gfx, int x, int y, float z, float alpha) {
                PotionSheetRenderer.renderHud(gfx, iconU, iconV, x, y, (int) z, alpha);
                return true;
            }
        });
    }
    //?}
}
