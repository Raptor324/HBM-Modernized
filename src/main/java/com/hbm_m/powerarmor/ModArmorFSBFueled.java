package com.hbm_m.powerarmor;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFillableItem;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.util.BobMathUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code com.hbm.items.armor.ArmorFSBFueled}: FSB-Ruestung mit Treibstofftank (Dampf/Diesel).
 * Treffer verbrauchen {@code consumption} mB je Schadenspunkt, das volle Set alle 10 Ticks {@code drain}.
 */
public class ModArmorFSBFueled extends ModArmorFSB implements IFillableItem {

    protected final Supplier<Fluid> fuelType;
    public int maxFuel = 1;
    public int fillRate;
    public int consumption;
    public int drain;

    public ModArmorFSBFueled(ModArmorMaterials material, Type type, Properties properties, String texture,
                             Supplier<Fluid> fuelType, int maxFuel, int fillRate, int consumption, int drain) {
        super(material, type, properties, texture);
        this.fuelType = fuelType;
        this.fillRate = fillRate;
        this.consumption = consumption;
        this.drain = drain;
        this.maxFuel = maxFuel;
    }

    @Override
    public int getFill(ItemStack stack) {
        if (stack.getTag() == null) {
            stack.setTag(new CompoundTag());
            setFill(stack, maxFuel);
            return maxFuel;
        }
        return stack.getTag().getInt("fuel");
    }

    public void setFill(ItemStack stack, int fill) {
        if (stack.getTag() == null) {
            stack.setTag(new CompoundTag());
        }
        stack.getTag().putInt("fuel", fill);
    }

    public int getMaxFill(ItemStack stack) {
        return this.maxFuel;
    }

    public int getLoadSpeed(ItemStack stack) {
        return this.fillRate;
    }

    public int getUnloadSpeed(ItemStack stack) {
        return 0;
    }

    //? if !fabric {
    @Override
    public void setDamage(ItemStack stack, int damage) {
        // 1.20: ItemStack.setTag ruft setDamageValue(0) auf - das darf nichts tun (sonst Endlosschleife).
        if (damage <= 0) return;
        this.setFill(stack, Math.max(this.getFill(stack) - (damage * consumption), 0));
    }
    //?}

    @Override
    public boolean isArmorEnabled(ItemStack stack) {
        return getFill(stack) > 0;
    }

    //? if forge {
    @Override
    @SuppressWarnings("removal")
    public void onArmorTick(ItemStack stack, Level world, Player player) {
        super.onArmorTick(stack, world, player);
        armorTick(stack, world, player);
    }
    //?}

    protected void armorTick(ItemStack stack, Level world, Player player) {
        if (this.drain > 0 && ModArmorFSB.hasFSBArmor(player) && !player.getAbilities().instabuild && world.getGameTime() % 10 == 0) {
            this.setFill(stack, Math.max(this.getFill(stack) - this.drain, 0));
        }
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(fluidName(fuelType.get()).copy().append(": " + BobMathUtil.getShortNumber(getFill(stack)) + " / " + BobMathUtil.getShortNumber(getMaxFill(stack))));
        super.appendHbmTooltip(stack, level, list, flag);
    }

    protected static Component fluidName(Fluid fluid) {
        //? if forge {
        return Component.translatable(fluid.getFluidType().getDescriptionId());
        //?} else {
        /*var key = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid);
        return Component.translatable("fluid." + key.getNamespace() + "." + key.getPath());
        *///?}
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getFill(stack) < getMaxFill(stack);
    }

    public double getDurabilityForDisplay(ItemStack stack) {
        return 1 - (double) getFill(stack) / (double) getMaxFill(stack);
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
    public boolean acceptsFluid(Fluid type, ItemStack stack) {
        return type == this.fuelType.get();
    }

    @Override
    public int tryFill(Fluid type, int amount, ItemStack stack) {

        if (!acceptsFluid(type, stack))
            return amount;

        int toFill = Math.min(amount, this.fillRate);
        toFill = Math.min(toFill, this.maxFuel - this.getFill(stack));
        this.setFill(stack, this.getFill(stack) + toFill);

        return amount - toFill;
    }

    @Override
    public boolean providesFluid(Fluid type, ItemStack stack) {
        return false;
    }

    @Override
    public int tryEmpty(Fluid type, int amount, ItemStack stack) {
        return 0;
    }

    @Override
    public @Nullable Fluid getFirstFluidType(ItemStack stack) {
        return null;
    }
}
