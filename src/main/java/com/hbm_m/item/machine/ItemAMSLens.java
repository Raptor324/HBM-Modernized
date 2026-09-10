package com.hbm_m.item.machine;

import com.hbm_m.item.ITooltipProvider;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1-Port von {@code ItemLens} (1.7.10): die Linse des Feldstabilisators.
 *
 * <p>Sie ist das Verschleissteil des Dunklen Fusionskerns. Jeder Tick, in dem der Stabilisator
 * arbeitet, kostet sie so viele Punkte, wie er gerade Watt fahrt - wer das Feld hochdreht,
 * verbraucht Linsen also im Quadrat schneller, als er denkt. Ist sie aufgebraucht, verschwindet
 * sie und das Feld faellt zusammen.</p>
 *
 * <p>Original: {@code 60 * 60 * 60 * 20 * 100} Punkte - bei einem Watt genau hundert Stunden, bei
 * hundert Watt noch eine.</p>
 */
public class ItemAMSLens extends Item implements ITooltipProvider {

    /** Original: {@code new ItemLens(60 * 60 * 60 * 20 * 100)}. */
    public static final long DEFAULT_MAX_DAMAGE = 60L * 60L * 60L * 20L * 100L;

    private static final String KEY_DAMAGE = "damage";

    private final long maxDamage;

    public ItemAMSLens(Properties properties, long maxDamage) {
        super(properties.stacksTo(1));
        this.maxDamage = maxDamage;
    }

    /** Heisst bewusst nicht getMaxDamage - das ist in Item final und meint Werkzeughaltbarkeit. */
    public long getLensCapacity() {
        return maxDamage;
    }

    public static long getLensDamage(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0L : tag.getLong(KEY_DAMAGE);
    }

    public static void setLensDamage(ItemStack stack, long damage) {
        stack.getOrCreateTag().putLong(KEY_DAMAGE, damage);
    }

    /** Die Belastungsgrenze dieses Stapels, oder 0 wenn es gar keine Linse ist. */
    public static long maxDamageOf(@Nullable ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemAMSLens lens ? lens.maxDamage : 0L;
    }

    /** Ob die Linse noch taugt - das prueft der Stabilisator vor jedem Schuss. */
    public static boolean isUsable(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        long max = maxDamageOf(stack);
        return max > 0 && getLensDamage(stack) < max;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getLensDamage(stack) > 0L;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double left = 1D - (double) getLensDamage(stack) / (double) maxDamage;
        return (int) Math.round(13D * Math.max(0D, Math.min(1D, left)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x66CCFF;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        long left = Math.max(0L, maxDamage - getLensDamage(stack));
        int percent = (int) (left * 100L / maxDamage);

        tooltip.add(Component.translatable("tooltip.hbm_m.ams_lens.durability", left, maxDamage, percent)
                .withStyle(ChatFormatting.GRAY));
    }
}
