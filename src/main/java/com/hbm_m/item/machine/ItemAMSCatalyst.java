package com.hbm_m.item.machine;

import com.hbm_m.item.ITooltipProvider;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1-Port von {@code ItemCatalyst} (1.7.10): die Katalysatoren des Dunklen Fusionskerns.
 *
 * <p>In den Kern gehoeren <b>zwei</b> davon, und beide zusammen bestimmen, welche Farbe die
 * Reaktion annimmt - der Kern mischt schlicht die beiden Farbwerte. Ohne zwei Katalysatoren
 * bleibt die Farbe null, und ohne Farbe laeuft der Kern gar nicht erst an.</p>
 *
 * <p>Die vier Kennzahlen wirken so: {@link #powerAbs} ist ein fester Zuschlag auf die Ausbeute,
 * {@link #powerMod} ein Faktor darauf, {@link #heatMod} regelt die Hitzeentwicklung und
 * {@link #fuelMod} den Brennstoffverbrauch. Sie sind gegeneinander abgestimmt - wer mehr Leistung
 * will, zahlt mit Hitze oder Brennstoff.</p>
 *
 * <p><b>Anmerkung:</b> im Original liest der Kern zwar {@code getColor()} aus, die drei uebrigen
 * Kennzahlen aber nirgends - {@code getPowerAbs} und Geschwister haben dort keinen einzigen
 * Aufrufer. Dieser Port fuehrt sie trotzdem mit, damit sie beim Feinschliff bereitstehen.</p>
 */
public class ItemAMSCatalyst extends Item implements ITooltipProvider {

    private final int color;
    private final long powerAbs;
    private final float powerMod;
    private final float heatMod;
    private final float fuelMod;

    public ItemAMSCatalyst(Properties properties, int color) {
        this(properties, color, 0L, 1.0F, 1.0F, 1.0F);
    }

    public ItemAMSCatalyst(Properties properties, int color, long powerAbs,
                           float powerMod, float heatMod, float fuelMod) {
        super(properties.stacksTo(1));
        this.color = color;
        this.powerAbs = powerAbs;
        this.powerMod = powerMod;
        this.heatMod = heatMod;
        this.fuelMod = fuelMod;
    }

    public int getColor() { return color; }

    // ── Statische Abfragen, wie im Original ─────────────────────────────────

    public static long getPowerAbs(@Nullable ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemAMSCatalyst c ? c.powerAbs : 0L;
    }

    public static float getPowerMod(@Nullable ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemAMSCatalyst c ? c.powerMod : 0F;
    }

    public static float getHeatMod(@Nullable ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemAMSCatalyst c ? c.heatMod : 0F;
    }

    public static float getFuelMod(@Nullable ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemAMSCatalyst c ? c.fuelMod : 0F;
    }

    /** Die Farbe eines Katalysators, oder 0 wenn es keiner ist. */
    public static int colorOf(@Nullable ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemAMSCatalyst c ? c.color : 0;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.ams_catalyst.spice").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hbm_m.ams_catalyst.colors").withStyle(ChatFormatting.GRAY));
    }
}
