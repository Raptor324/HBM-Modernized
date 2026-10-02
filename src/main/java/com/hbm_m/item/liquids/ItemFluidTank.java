package com.hbm_m.item.liquids;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * 1:1 {@code com.hbm.items.machine.ItemFluidTank} ({@code fluid_tank_full}, {@code fluid_tank_lead_full},
 * {@code fluid_barrel_full}, {@code fluid_pack_full}). Die Fluessigkeit, im Original die Metadaten-ID, steht im NBT
 * {@code "type"} (Registry-Name). Pass 1 (Overlay) wird in der Fluessigkeitsfarbe eingefaerbt, das Zurueckgeben beim
 * Crafting liefert den leeren Behaelter ({@code setContainerItem}).
 */
public class ItemFluidTank extends Item {

    public static final String NBT_TYPE = "type";

    private final Supplier<Item> empty;

    public ItemFluidTank(Properties properties, Supplier<Item> empty) {
        super(properties);
        this.empty = empty;
    }

    public static ItemStack make(Item item, Fluid fluid, int count) {
        ItemStack stack = new ItemStack(item, count);
        setFluid(stack, fluid);
        return stack;
    }

    public static void setFluid(ItemStack stack, Fluid fluid) {
        stack.getOrCreateTag().putString(NBT_TYPE, BuiltInRegistries.FLUID.getKey(fluid).toString());
    }

    /** Fluids.fromID(meta): ohne NBT {@code NONE}. */
    public static Fluid getFluid(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(NBT_TYPE)) return ModFluids.NONE.getSource();
        ResourceLocation id = ResourceLocation.tryParse(stack.getTag().getString(NBT_TYPE));
        Fluid f = id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(id);
        return f == null || f == Fluids.EMPTY ? ModFluids.NONE.getSource() : f;
    }

    public static Component fluidName(Fluid fluid) {
        return FluidType.forFluid(fluid).getLocalizedName();
    }

    /** getSubItems: alle Fluessigkeiten in Anzeigereihenfolge, die diesen Behaelter haben. */
    public List<ItemStack> getSubItems() {
        List<ItemStack> list = new ArrayList<>();
        for (ModFluids.FluidEntry entry : HbmFluidRegistry.getOrderedFluids()) {
            Fluid f = entry.getSource();
            if (f == ModFluids.NONE.getSource()) continue;
            if (accepts(f)) list.add(make(this, f, 1));
        }
        return list;
    }

    protected boolean accepts(Fluid f) {
        FluidType type = FluidType.forFluid(f);
        if (type.hasNoContainer()) return false;
        if (type.needsLeadContainer()) return this == com.hbm_m.item.ModItems.FLUID_TANK_LEAD_FULL.get();
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack), fluidName(getFluid(stack)));
    }

    /** getColorFromItemStack: Pass 0 weiss, Pass 1 Fluessigkeitsfarbe. */
    public int getColor(ItemStack stack, int pass) {
        if (pass == 0) return 0xFFFFFF;
        int j = HbmFluidRegistry.getTintColor(getFluid(stack));
        if (j < 0) j = 0xFFFFFF;
        return j;
    }

    @Nullable
    public Item getEmpty() {
        return empty == null ? null : empty.get();
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return getEmpty() != null;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        Item e = getEmpty();
        return e == null ? ItemStack.EMPTY : new ItemStack(e);
    }

    /** Ersatz fuer den Metadaten-Zugriff aus dem Original. */
    public static CompoundTag tagFor(Fluid fluid) {
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_TYPE, BuiltInRegistries.FLUID.getKey(fluid).toString());
        return tag;
    }
}
