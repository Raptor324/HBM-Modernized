package com.hbm_m.item.tool;

import com.hbm_m.platform.StackNbt;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.hbm_m.api.fluids.IFillableItem;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code com.hbm.items.tool.ItemToolAbilityFueled}: Faehigkeitswerkzeug mit Treibstofftank ("fuel").
 * Abnutzung verbraucht {@code consumption} mB, ohne genug Treibstoff arbeitet es nicht.
 */
public class ItemToolAbilityFueled extends ItemToolAbility implements IFillableItem {

    protected int fillRate;
    protected int consumption;
    protected int maxFuel;
    protected final List<Supplier<Fluid>> acceptedFuelSuppliers;
    private Set<Fluid> acceptedFuels;

    @SafeVarargs
    public ItemToolAbilityFueled(float damage, double movement, Tier material, EnumToolType type, int maxFuel, int consumption, int fillRate, Supplier<Fluid>... acceptedFuels) {
        // Original setMaxDamage(1) mit "damage > max" als Bruch; 1.20 bricht bei "damage >= max", daher 2
        super(damage, movement, material, type, new Properties().durability(2), true);
        this.maxFuel = maxFuel;
        this.consumption = consumption;
        this.fillRate = fillRate;
        this.acceptedFuelSuppliers = List.of(acceptedFuels);
    }

    /** Die Fluessigkeiten sind bei der Item-Registrierung noch nicht aufgeloest, daher erst beim ersten Zugriff. */
    protected Set<Fluid> acceptedFuels() {
        if (acceptedFuels == null) {
            Set<Fluid> set = new LinkedHashSet<>();
            for (Supplier<Fluid> s : acceptedFuelSuppliers) set.add(s.get());
            acceptedFuels = set;
        }
        return acceptedFuels;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {

        list.add(Component.literal("Fuel: " + this.getFill(stack) + "/" + this.maxFuel + "mB").withStyle(ChatFormatting.GOLD));

        for (Fluid type : acceptedFuels()) {
            list.add(Component.literal("- ").append(fluidName(type)).withStyle(ChatFormatting.YELLOW));
        }

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
        return getFill(stack) < maxFuel;
    }

    public double getDurabilityForDisplay(ItemStack stack) {
        return 1 - (double) getFill(stack) / (double) maxFuel;
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
        return getFill(stack) >= this.consumption;
    }

    //? if !fabric {
    @Override
    public void setDamage(ItemStack stack, int damage) {
        // 1.20: ItemStack.setTag ruft setDamageValue(0) auf - das darf nichts tun (sonst Endlosschleife).
        if (damage <= 0) return;
        this.setFill(stack, Math.max(this.getFill(stack) - damage * consumption, 0));
    }
    //?}

    @Override
    public int getFill(ItemStack stack) {
        if (StackNbt.read(stack) == null) {
            StackNbt.set(stack, new CompoundTag());
            setFill(stack, maxFuel);
            return maxFuel;
        }

        return StackNbt.read(stack).getInt("fuel");
    }

    public void setFill(ItemStack stack, int fill) {
        if (StackNbt.read(stack) == null) {
            StackNbt.set(stack, new CompoundTag());
        }

        StackNbt.tag(stack).putInt("fuel", fill);
    }

    @Override
    public boolean acceptsFluid(Fluid type, ItemStack stack) {
        return this.acceptedFuels().contains(type);
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
        return amount;
    }

    public static ItemStack getEmptyTool(Item item) {
        ItemToolAbilityFueled tool = (ItemToolAbilityFueled) item;
        ItemStack stack = new ItemStack(item);
        tool.setFill(stack, 0);
        return stack;
    }

    @Override
    public @Nullable Fluid getFirstFluidType(ItemStack stack) {
        return null;
    }
}
