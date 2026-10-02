package com.hbm_m.armormod.item;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFillableItem;
import com.hbm_m.armormod.util.ArmorModificationHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/** 1:1 {@code com.hbm.items.armor.JetpackFueledBase}: Jetpack mit Treibstofftank (NBT "fuel"). */
public abstract class JetpackFueledBase extends JetpackBase implements IFillableItem {

    public final Supplier<Fluid> fuel;
    public int maxFuel;

    public JetpackFueledBase(Properties properties, Supplier<Fluid> fuel, int maxFuel) {
        super(properties);
        this.fuel = fuel;
        this.maxFuel = maxFuel;
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
    public void addInformation(ItemStack itemstack, @Nullable Level level, List<Component> list) {
        list.add(fluidName(fuel.get()).copy().append(": " + getFuel(itemstack) + "mB / " + this.maxFuel + "mB").withStyle(ChatFormatting.LIGHT_PURPLE));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        ItemStack jetpack = ArmorModificationHelper.pryMods(armor)[ArmorModificationHelper.plate_only];

        if (jetpack == null || jetpack.isEmpty())
            return;

        list.add(Component.literal("  ").append(stack.getHoverName()).append(" (").append(fluidName(fuel.get())).append(": " + getFuel(jetpack) + "mB / " + this.maxFuel + "mB)").withStyle(ChatFormatting.RED));
    }

    protected void useUpFuel(Player player, ItemStack stack, int rate) {

        if (player.tickCount % rate == 0) {
            setFuel(stack, getFuel(stack) - 1);
        }
    }

    public static int getFuel(ItemStack stack) {
        if (stack.getTag() == null) {
            stack.setTag(new CompoundTag());
            return 0;
        }
        return stack.getTag().getInt("fuel");
    }

    public static void setFuel(ItemStack stack, int i) {
        if (stack.getTag() == null) {
            stack.setTag(new CompoundTag());
        }
        stack.getTag().putInt("fuel", i);
    }

    public int getMaxFill(ItemStack stack) {
        return this.maxFuel;
    }

    public int getLoadSpeed(ItemStack stack) {
        return 10;
    }

    @Override
    public boolean acceptsFluid(Fluid type, ItemStack stack) {
        return type == this.fuel.get();
    }

    @Override
    public int tryFill(Fluid type, int amount, ItemStack stack) {

        if (!acceptsFluid(type, stack))
            return amount;

        int fill = getFuel(stack);
        int req = maxFuel - fill;

        int toFill = Math.min(amount, req);
        //toFill = Math.min(toFill, getLoadSpeed(stack));

        setFuel(stack, fill + toFill);

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

    @Override
    public int getFill(ItemStack stack) {
        return 0;
    }
}
