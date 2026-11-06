package com.hbm_m.effect;

import java.util.function.Consumer;


import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

/**
 * Gemeinsame Basis der aus {@code HbmPotion} portierten Effekte: Farbe, Kategorie und das Symbol
 * aus {@code potions.png}.
 *
 * <p>Die Kachelkoordinaten sind dieselben wie im {@code setIconIndex(x, y)} des Originals.
 * Standardmaessig tickt ein Effekt nie ({@code isReady} gibt im Original fuer die reinen
 * Markiereffekte {@code false} zurueck) - wer etwas tun will, ueberschreibt beides.</p>
 */
public abstract class HbmEffect extends MobEffect {

    /** Same numbers as PotionSheetRenderer.ICON_SIZE / SHEET_ORIGIN_V (potions.png, mod rows from 198). */
    private static final int SHEET_ICON_SIZE = 18;
    private static final int SHEET_ORIGIN_V = 198;


    private final int iconU;
    private final int iconV;

    protected HbmEffect(MobEffectCategory category, int color, int iconX, int iconY) {
        super(category, color);
        // Sheet geometry inlined: PotionSheetRenderer is a client class (GuiGraphics) and this
        // constructor runs on the dedicated server too.
        this.iconU = iconX * SHEET_ICON_SIZE;
        this.iconV = SHEET_ORIGIN_V + iconY * SHEET_ICON_SIZE;
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

    public int getIconU() { return iconU; }
    public int getIconV() { return iconV; }

    // Клиентские иконки с общего листа (HUD + инвентарь), обе версии; расширение
    // живёт здесь — платформенный слой не знает про конкретные эффекты мода.
    @Override
@SuppressWarnings("removal")
    public void initializeClient(@NotNull Consumer<IClientMobEffectExtensions> consumer) {
        consumer.accept(new IClientMobEffectExtensions() {
            @Override
            public boolean renderInventoryIcon(net.minecraft.world.effect.MobEffectInstance instance,
                    net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen<?> screen,
                    net.minecraft.client.gui.GuiGraphics gfx, int x, int y, int blitOffset) {
                com.hbm_m.effect.render.PotionSheetRenderer.renderInventory(gfx, iconU, iconV, x, y, blitOffset);
                return true;
            }

            @Override
            public boolean renderGuiIcon(net.minecraft.world.effect.MobEffectInstance instance, net.minecraft.client.gui.Gui gui,
                    net.minecraft.client.gui.GuiGraphics gfx, int x, int y, float z, float alpha) {
                com.hbm_m.effect.render.PotionSheetRenderer.renderHud(gfx, iconU, iconV, x, y, (int) z, alpha);
                return true;
            }
        });
    }
}
