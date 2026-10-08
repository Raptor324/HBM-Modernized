package com.hbm_m.item.fekal_electric;

import com.hbm_m.platform.StackNbt;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.hbm_m.util.EnergyFormatter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.machine.ItemBatteryPack} ({@code battery_pack}, Meta = {@link EnumBatteryPack}; im Port je
 * ein Gegenstand {@code battery_pack_<name>}): Batterien laden in 15 Minuten, Kondensatoren in 30 Sekunden; die
 * Quantenbatterie haelt eine Stunde. Die Ladung laeuft ueber die Energie-Schnittstelle des Ports ({@link ModBatteryItem}),
 * damit Sockel und Maschinen sie direkt nutzen.
 */
public class ItemBatteryPack extends ModBatteryItem {

    public enum EnumBatteryPack {
        BATTERY_REDSTONE    ("battery_redstone",          100L, false),
        BATTERY_LEAD        ("battery_lead",            1_000L, false),
        BATTERY_LITHIUM     ("battery_lithium",        10_000L, false),
        BATTERY_SODIUM      ("battery_sodium",         50_000L, false),
        BATTERY_SCHRABIDIUM ("battery_schrabidium",   250_000L, false),
        BATTERY_QUANTUM     ("battery_quantum",     1_000_000L, 20 * 60 * 60),
        CAPACITOR_COPPER    ("capacitor_copper",        1_000L, true),
        CAPACITOR_GOLD      ("capacitor_gold",         10_000L, true),
        CAPACITOR_NIOBIUM   ("capacitor_niobium",     100_000L, true),
        CAPACITOR_TANTALUM  ("capacitor_tantalum",    500_000L, true),
        CAPACITOR_BISMUTH   ("capacitor_bismuth",   2_500_000L, true),
        CAPACITOR_SPARK     ("capacitor_spark",    10_000_000L, true);

        public final String texture;
        public final long capacity;
        public final long chargeRate;
        public final long dischargeRate;

        EnumBatteryPack(String tex, long dischargeRate, boolean capacitor) {
            this(tex,
                    capacitor ? (dischargeRate * 20 * 30) : (dischargeRate * 20 * 60 * 15),
                    capacitor ? dischargeRate : dischargeRate * 10,
                    dischargeRate);
        }

        EnumBatteryPack(String tex, long dischargeRate, long duration) {
            this(tex, dischargeRate * duration, dischargeRate * 10, dischargeRate);
        }

        EnumBatteryPack(String tex, long capacity, long chargeRate, long dischargeRate) {
            this.texture = tex;
            this.capacity = capacity;
            this.chargeRate = chargeRate;
            this.dischargeRate = dischargeRate;
        }

        public boolean isCapacitor() { return this.ordinal() > BATTERY_QUANTUM.ordinal(); }

        /** Registry-Name im Port. */
        public String id() { return "battery_pack_" + name().toLowerCase(Locale.US); }
    }

    public final EnumBatteryPack pack;

    public ItemBatteryPack(Properties properties, EnumBatteryPack pack) {
        super(properties, pack.capacity, pack.chargeRate, pack.dischargeRate);
        this.pack = pack;
    }

    public static ItemStack makeEmptyBattery(ItemStack stack) {
        setEnergy(stack, 0);
        return stack;
    }

    public static ItemStack makeFullBattery(ItemStack stack) {
        setEnergy(stack, ((ItemBatteryPack) stack.getItem()).pack.capacity);
        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        // Original showDurabilityBar: nur wenn nicht voll
        return getEnergy(stack) < pack.capacity;
    }

    @Override
    public void appendHbmTooltip(ItemStack itemstack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        long maxCharge = pack.capacity;
        long chargeRate = pack.chargeRate;
        long dischargeRate = pack.dischargeRate;
        long charge = maxCharge;
        if (StackNbt.has(itemstack)) charge = getEnergy(itemstack);

        list.add(Component.literal("Energy stored: " + EnergyFormatter.format(charge) + "/" + EnergyFormatter.format(maxCharge) + "HE (" + (charge * 1000 / maxCharge / 10D) + "%)").withStyle(ChatFormatting.GREEN));
        list.add(Component.literal("Charge rate: " + EnergyFormatter.format(chargeRate) + "HE/t").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("Discharge rate: " + EnergyFormatter.format(dischargeRate) + "HE/t").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("Time for full charge: " + (maxCharge / chargeRate / 20 / 60D) + "min").withStyle(ChatFormatting.GOLD));
        list.add(Component.literal("Charge lasts for: " + (maxCharge / dischargeRate / 20 / 60D) + "min").withStyle(ChatFormatting.GOLD));
    }
}
