package com.hbm_m.powerarmor;

import com.hbm_m.api.energy.EnergyCapabilityProvider;
import com.hbm_m.block.custom.machines.armormod.item.ItemModBattery;
import com.hbm_m.block.custom.machines.armormod.item.ItemModBatteryMk2;
import com.hbm_m.block.custom.machines.armormod.item.ItemModBatteryMk3;
import com.hbm_m.block.custom.machines.armormod.util.ArmorModificationHelper;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.util.EnergyFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;
import java.util.List;

// Full Set Bonus Powered armor - combines FSB functionality with battery system
public class ModArmorFSBPowered extends ModArmorFSB {

    public long maxPower = 1;
    public long chargeRate;
    public long consumption; // Energy cost when armor takes damage (via setDamage)
    public long drain;       // Passive energy drain per tick

    public ModArmorFSBPowered(ArmorMaterial material, Type type, Properties properties, 
                              String texture, long maxPower, long chargeRate, 
                              long consumption, long drain) {
        super(material, type, properties, texture);
        this.maxPower = maxPower;
        this.chargeRate = chargeRate;
        this.consumption = consumption;
        this.drain = drain;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, 
                               List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Charge: " + 
            EnergyFormatter.format(getCharge(stack)) + " / " + 
            EnergyFormatter.format(getMaxCharge(stack)))
            .withStyle(ChatFormatting.AQUA));

        ArmorTooltipHandler.getFSBTooltip(stack).ifPresent(tooltip::addAll);
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public boolean isArmorEnabled(ItemStack stack) {
        return getCharge(stack) > 0;
    }

    public void chargeBattery(ItemStack stack, long amount) {
        if (stack.getItem() instanceof ModArmorFSBPowered) {
            if (stack.hasTag()) {
                stack.getTag().putLong("charge", stack.getTag().getLong("charge") + amount);
            } else {
                stack.setTag(new CompoundTag());
                stack.getTag().putLong("charge", amount);
            }
        }
    }

    public void setCharge(ItemStack stack, long amount) {
        if (stack.getItem() instanceof ModArmorFSBPowered) {
            if (stack.hasTag()) {
                stack.getTag().putLong("charge", amount);
            } else {
                stack.setTag(new CompoundTag());
                stack.getTag().putLong("charge", amount);
            }
        }
    }

    public void dischargeBattery(ItemStack stack, long amount) {
        if (stack.getItem() instanceof ModArmorFSBPowered) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putLong("charge", tag.getLong("charge") - amount);
            if (tag.getLong("charge") < 0) {
                tag.putLong("charge", 0);
            }
        }
    }

    public long getCharge(ItemStack stack) {
        if (stack.getItem() instanceof ModArmorFSBPowered) {
            if (stack.hasTag()) {
                return Math.min(stack.getTag().getLong("charge"), getMaxCharge(stack));
            } else {
                stack.setTag(new CompoundTag());
                stack.getTag().putLong("charge", getMaxCharge(stack));
                return stack.getTag().getLong("charge");
            }
        }
        return 0;
    }

    public boolean showDurabilityBar(ItemStack stack) {
        return getCharge(stack) < getMaxCharge(stack);
    }

    public double getDurabilityForDisplay(ItemStack stack) {
        return 1 - ((double) getCharge(stack) / (double) getMaxCharge(stack));
    }

    public long getMaxCharge(ItemStack stack) {
        if (ArmorModificationHelper.hasMods(stack)) {
            ItemStack mod = ArmorModificationHelper.pryMod(stack, ArmorModificationHelper.battery);
            if (!mod.isEmpty()) {
                if (mod.getItem() instanceof ItemModBatteryMk3) {
                    return (long) (maxPower * 2.0D); // MK3: 100%
                } else if (mod.getItem() instanceof ItemModBatteryMk2) {
                    return (long) (maxPower * 1.5D); // MK2: 50%
                } else if (mod.getItem() instanceof ItemModBattery battery) {
                    return (long) (maxPower * battery.getCapacityMultiplier()); // 25%
                }
            }
        }
        return maxPower;
    }

    public long getChargeRate(ItemStack stack) {
        return chargeRate;
    }

    public long getDischargeRate(ItemStack stack) {
        return 0;
    }

    /**
     * MATCHES 1.7.10 BEHAVIOR
     * This is called by Minecraft when armor would take damage.
     * Instead of damaging the item, we drain energy based on consumption.
     */
    @Override
    public void setDamage(ItemStack stack, int damage) {
        // Don't damage the item - power armor doesn't break
        // Instead, drain energy if consumption is configured
        if (this.consumption > 0) {
            this.dischargeBattery(stack, (long) damage * this.consumption);
        }
    }

    /**
     * MATCHES 1.7.10 BEHAVIOR
     * Passive energy drain per tick when wearing full FSB armor set.
     */
    @Override
    public void onArmorTick(ItemStack stack, Level world, Player player) {
        super.onArmorTick(stack, world, player);

        // Passive drain - matches 1.7.10 exactly
        if (this.drain > 0 && ModArmorFSB.hasFSBArmor(player) 
            && !player.getAbilities().instabuild && !player.isSpectator()) {
            this.dischargeBattery(stack, drain);
        }
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        long modifiedCapacity = getMaxCharge(stack);
        return new EnergyCapabilityProvider(stack, modifiedCapacity, chargeRate, modifiedCapacity);
    }
}