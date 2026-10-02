package com.hbm_m.item.tool;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.item.IBatteryItem;
import com.hbm_m.util.BobMathUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.tool.ItemToolAbilityPower}: Faehigkeitswerkzeug mit Akku. Abnutzung entlaedt
 * ({@code setDamage}), ohne Ladung ({@code charge < consumption}) arbeitet es nicht.
 */
public class ItemToolAbilityPower extends ItemToolAbility implements IBatteryItem {

    public long maxPower = 1;
    public long chargeRate;
    public long consumption;

    public ItemToolAbilityPower(float damage, double movement, Tier material, EnumToolType type, long maxPower, long chargeRate, long consumption) {
        // Original setMaxDamage(1) mit "damage > max" als Bruch; 1.20 bricht bei "damage >= max", daher 2
        super(damage, movement, material, type, new Properties().durability(2), true);
        this.maxPower = maxPower;
        this.chargeRate = chargeRate;
        this.consumption = consumption;
    }

    @Override
    public void chargeBattery(ItemStack stack, long i) {
        if (stack.getItem() instanceof ItemToolAbilityPower) {
            if (stack.hasTag()) {
                stack.getTag().putLong("charge", stack.getTag().getLong("charge") + i);
            } else {
                stack.setTag(new CompoundTag());
                stack.getTag().putLong("charge", i);
            }
        }
    }

    @Override
    public void setCharge(ItemStack stack, long i) {
        if (stack.getItem() instanceof ItemToolAbilityPower) {
            if (!stack.hasTag()) stack.setTag(new CompoundTag());
            stack.getTag().putLong("charge", i);
        }
    }

    @Override
    public void dischargeBattery(ItemStack stack, long i) {
        if (stack.getItem() instanceof ItemToolAbilityPower) {
            if (stack.hasTag()) {
                stack.getTag().putLong("charge", stack.getTag().getLong("charge") - i);
            } else {
                stack.setTag(new CompoundTag());
                stack.getTag().putLong("charge", this.maxPower - i);
            }

            if (stack.getTag().getLong("charge") < 0)
                stack.getTag().putLong("charge", 0);
        }
    }

    @Override
    public long getCharge(ItemStack stack) {
        if (stack.getItem() instanceof ItemToolAbilityPower) {
            if (stack.hasTag()) {
                return stack.getTag().getLong("charge");
            } else {
                stack.setTag(new CompoundTag());
                stack.getTag().putLong("charge", ((ItemToolAbilityPower) stack.getItem()).maxPower);
                return stack.getTag().getLong("charge");
            }
        }

        return 0;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Charge: " + BobMathUtil.getShortNumber(getCharge(stack)) + " / " + BobMathUtil.getShortNumber(maxPower)));
        super.appendHbmTooltip(stack, level, list, flag);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) < maxPower;
    }

    public double getDurabilityForDisplay(ItemStack stack) {
        return 1 - (double) getCharge(stack) / (double) maxPower;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(13.0D - getDurabilityForDisplay(stack) * 13.0D);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = Math.max(0.0F, (float) (1.0D - getDurabilityForDisplay(stack)));
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean canOperate(ItemStack stack) {
        return getCharge(stack) >= this.consumption;
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return maxPower;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return chargeRate;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return 0;
    }

    //? if !fabric {
    @Override
    public void setDamage(ItemStack stack, int damage) {
        // 1.20: ItemStack.setTag ruft setDamageValue(0) auf - das darf nichts tun (sonst Endlosschleife).
        if (damage <= 0) return;
        this.dischargeBattery(stack, damage * consumption);
    }
    //?}

    //? if forge {
    @Override
    public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new com.hbm_m.api.energy.EnergyCapabilityProvider(stack, maxPower, chargeRate, 0);
    }
    //?}
}
