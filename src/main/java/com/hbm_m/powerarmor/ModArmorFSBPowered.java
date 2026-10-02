package com.hbm_m.powerarmor;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.util.BobMathUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
//? if forge {
import com.hbm_m.api.energy.EnergyCapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
//?}

/**
 * 1:1 {@code com.hbm.items.armor.ArmorFSBPowered}: FSB-Ruestung mit Akku. Treffer ziehen statt Haltbarkeit
 * {@code consumption} HE je Schadenspunkt ab, das volle Set verbraucht {@code drain} HE pro Tick, ohne
 * Ladung ist das Teil abgeschaltet ({@link #isArmorEnabled}).
 */
public class ModArmorFSBPowered extends ModArmorFSB implements com.hbm_m.api.item.IBatteryItem {

    public long maxPower = 1;
    public long chargeRate;
    public long consumption;
    public long drain;

    public ModArmorFSBPowered(ModArmorMaterials material, Type type, Properties properties, String texture,
                              long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, texture);
        this.maxPower = maxPower;
        this.chargeRate = chargeRate;
        this.consumption = consumption;
        this.drain = drain;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Charge: " + BobMathUtil.getShortNumber(getCharge(stack)) + " / " + BobMathUtil.getShortNumber(getMaxCharge(stack))));
        super.appendHbmTooltip(stack, level, list, flag);
    }

    @Override
    public boolean isArmorEnabled(ItemStack stack) {
        return getCharge(stack) > 0;
    }

    public void chargeBattery(ItemStack stack, long i) {
        if (stack.getItem() instanceof ModArmorFSBPowered) {
            if (stack.hasTag()) {
                stack.getTag().putLong("charge", stack.getTag().getLong("charge") + i);
            } else {
                stack.setTag(new CompoundTag());
                stack.getTag().putLong("charge", i);
            }
        }
    }

    public void setCharge(ItemStack stack, long i) {
        if (stack.getItem() instanceof ModArmorFSBPowered) {
            if (!stack.hasTag()) stack.setTag(new CompoundTag());
            stack.getTag().putLong("charge", i);
        }
    }

    public void dischargeBattery(ItemStack stack, long i) {
        if (stack.getItem() instanceof ModArmorFSBPowered) {
            if (stack.hasTag()) {
                stack.getTag().putLong("charge", stack.getTag().getLong("charge") - i);
            } else {
                stack.setTag(new CompoundTag());
                stack.getTag().putLong("charge", getMaxCharge(stack) - i);
            }

            if (stack.getTag().getLong("charge") < 0)
                stack.getTag().putLong("charge", 0);
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

    /** Original showDurabilityBar. */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) < getMaxCharge(stack);
    }

    /** Original getDurabilityForDisplay. */
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1 - (double) getCharge(stack) / (double) getMaxCharge(stack);
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

    public long getMaxCharge(ItemStack stack) {
        return (long) (maxPower * ArmorModificationHelper.getBatteryCapacityMultiplier(stack));
    }

    public long getChargeRate(ItemStack stack) {
        return chargeRate;
    }

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
    @SuppressWarnings("removal")
    public void onArmorTick(ItemStack stack, Level world, Player player) {
        super.onArmorTick(stack, world, player);
        armorTick(stack, world, player);
    }
    //?}

    /** Original onArmorTick-Rumpf der Unterklassen (nach dem Akku-Abzug). */
    protected void armorTick(ItemStack stack, Level world, Player player) {
        if (this.drain > 0 && ModArmorFSB.hasFSBArmor(player) && !player.getAbilities().instabuild) {
            this.dischargeBattery(stack, drain);
        }
    }

    //? if forge {
    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        long modifiedCapacity = getMaxCharge(stack);
        return new EnergyCapabilityProvider(stack, modifiedCapacity, chargeRate, modifiedCapacity);
    }
    //?}

    // Port: Schutzmaske im Curios-Gesichtsslot und Helm schliessen sich aus (siehe GasMaskCurio).
    //? if < 1.21.1 {
    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot slot, Entity entity) {
        return super.canEquip(stack, slot, entity)
                && (slot != EquipmentSlot.HEAD
                    || !(entity instanceof LivingEntity living)
                    || com.hbm_m.compat.curios.CuriosCompat.getFaceMask(living).isEmpty());
    }
    //?} else {
    /*@Override
    public boolean canEquip(ItemStack stack, EquipmentSlot slot, LivingEntity entity) {
        return super.canEquip(stack, slot, entity)
                && (slot != EquipmentSlot.HEAD
                    || com.hbm_m.compat.curios.CuriosCompat.getFaceMask(entity).isEmpty());
    }
     *///?}
}
