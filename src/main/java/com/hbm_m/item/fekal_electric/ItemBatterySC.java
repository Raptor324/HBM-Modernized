package com.hbm_m.item.fekal_electric;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.hbm_m.api.item.IBatteryItem;
import com.hbm_m.util.EnergyFormatter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.machine.ItemBatterySC} ({@code battery_sc}, Meta = {@link EnumBatterySC}; im Port je
 * {@code battery_sc_<typ>}): selbstladende Radioisotopenbatterie - immer voll, gibt pro Tick ihre Leistung ab und
 * nimmt nichts auf. Ueber {@link IBatteryItem} bleibt die Ladung in jeder Maschine konstant.
 */
public class ItemBatterySC extends ModBatteryItem implements IBatteryItem {

    public enum EnumBatterySC {
        EMPTY(0),
        WASTE(150),
        RA226(200),
        TC99(500),
        CO60(750),
        PU238(1_000),
        PO210(1_250),
        AU198(1_500),
        PB209(2_000),
        AM241(2_500);

        public final long power;

        EnumBatterySC(long power) {
            this.power = power;
        }

        public String id() { return "battery_sc_" + name().toLowerCase(Locale.US); }
    }

    public final EnumBatterySC type;

    public ItemBatterySC(Properties properties, EnumBatterySC type) {
        super(properties, type.power, 0, type.power);
        this.type = type;
    }

    @Override public void chargeBattery(ItemStack stack, long i) { }
    @Override public void setCharge(ItemStack stack, long i) { }
    @Override public void dischargeBattery(ItemStack stack, long i) { }
    @Override public long getChargeRate(ItemStack stack) { return 0; }
    @Override public long getCharge(ItemStack stack) { return getMaxCharge(stack); }
    @Override public long getDischargeRate(ItemStack stack) { return getMaxCharge(stack); }
    @Override public long getMaxCharge(ItemStack stack) { return type.power; }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (type.power > 0) list.add(Component.literal("Discharge rate: " + EnergyFormatter.format(type.power) + "HE/t").withStyle(ChatFormatting.YELLOW));
        for (String line : Component.translatable("item.hbm_m.battery_sc.desc").getString().split("\\$")) {
            list.add(Component.literal(line).withStyle(ChatFormatting.RED));
        }
    }
}
