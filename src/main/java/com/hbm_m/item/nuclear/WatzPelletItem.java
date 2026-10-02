package com.hbm_m.item.nuclear;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.platform.PlatformHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code ItemWatzPellet} fuer {@code watz_pellet} und {@code watz_pellet_depleted}. Die Metadaten des Originals
 * sind im Port je ein Item pro {@link WatzPelletType}; der Restertrag liegt wie dort als Double {@code yield} im NBT.
 */
public class WatzPelletItem extends Item implements com.hbm_m.item.ITooltipProvider {

    private final WatzPelletType type;
    private final boolean depleted;

    public WatzPelletItem(Properties properties, WatzPelletType type, boolean depleted) {
        super(properties.stacksTo(16));
        this.type = type;
        this.depleted = depleted;
    }

    public WatzPelletType getType() { return type; }
    public boolean isDepleted() { return depleted; }

    /** Entspricht {@code stack.getItem() == ModItems.watz_pellet}. */
    public static boolean isFresh(ItemStack stack) {
        return stack.getItem() instanceof WatzPelletItem p && !p.depleted;
    }

    /** Entspricht {@code stack.getItem() == ModItems.watz_pellet_depleted}. */
    public static boolean isDepleted(ItemStack stack) {
        return stack.getItem() instanceof WatzPelletItem p && p.depleted;
    }

    @Nullable
    public static WatzPelletType typeOf(ItemStack stack) {
        return stack.getItem() instanceof WatzPelletItem p ? p.type : null;
    }

    public static double getEnrichment(ItemStack stack) {
        WatzPelletType num = typeOf(stack);
        return num == null ? 0D : getYield(stack) / num.yield;
    }

    public static double getYield(ItemStack stack) { return getDouble(stack, "yield"); }
    public static void setYield(ItemStack stack, double yield) { setDouble(stack, "yield", yield); }

    public static void setDouble(ItemStack stack, String key, double value) {
        if (PlatformHooks.getItemTag(stack) == null) setNBTDefaults(stack);
        PlatformHooks.editItemTag(stack, t -> t.putDouble(key, value));
    }

    public static double getDouble(ItemStack stack, String key) {
        if (PlatformHooks.getItemTag(stack) == null) setNBTDefaults(stack);
        CompoundTag tag = PlatformHooks.getItemTag(stack);
        return tag == null ? 0D : tag.getDouble(key);
    }

    private static void setNBTDefaults(ItemStack stack) {
        WatzPelletType num = typeOf(stack);
        if (num == null) return;
        PlatformHooks.editItemTag(stack, t -> t.putDouble("yield", num.yield));
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        if (depleted) return;
        setNBTDefaults(stack); // Fenster fuer NBT-Fehler so klein wie moeglich halten
    }

    private double getDurabilityForDisplay(ItemStack stack) {
        return 1D - getEnrichment(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !depleted && getDurabilityForDisplay(stack) > 0D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(13.0D - getDurabilityForDisplay(stack) * 13.0D);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = (float) Math.max(0.0D, 1.0D - getDurabilityForDisplay(stack));
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (depleted) return;
        WatzPelletType num = type;

        list.add(Component.literal(ChatFormatting.GREEN + "Depletion: " + String.format(Locale.US, "%.1f", getDurabilityForDisplay(stack) * 100D) + "%"));

        String color = ChatFormatting.GOLD + "";
        String reset = ChatFormatting.RESET + "";

        if (num.passive > 0) {
            list.add(Component.literal(color + "Base fission rate: " + reset + num.passive));
            list.add(Component.literal(ChatFormatting.RED + "Self-igniting!"));
        }
        if (num.heatEmission > 0) list.add(Component.literal(color + "Heat per flux: " + reset + num.heatEmission + " TU"));
        if (num.burnFunc != null) {
            list.add(Component.literal(color + "Reaction function: " + reset + num.burnFunc.getLabelForFuel()));
            list.add(Component.literal(color + "Fuel type: " + reset + num.burnFunc.getDangerFromFuel()));
        }
        if (num.heatDiv != null) list.add(Component.literal(color + "Thermal multiplier: " + reset + num.heatDiv.getLabelForFuel() + " TU⁻¹"));
        if (num.absorbFunc != null) list.add(Component.literal(color + "Flux capture: " + reset + num.absorbFunc.getLabelForFuel()));
    }
}
