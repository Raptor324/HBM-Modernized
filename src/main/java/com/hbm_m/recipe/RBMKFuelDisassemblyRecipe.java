package com.hbm_m.recipe;

import com.hbm_m.item.rbmk.RBMKPelletItem;
import com.hbm_m.item.rbmk.RBMKRodItem;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 1:1 port of the original's {@code com.hbm.crafting.handlers.RBMKFuelCraftingHandler}.
 *
 * <p>A single RBMK fuel rod placed alone in the crafting grid disassembles into eight pellets,
 * provided both its hull and core are below 50 °C. The resulting pellets carry the rod's condition:
 * the enrichment tier is {@code 4 - clamp(ceil(enrichment * 5 - 1), 0, 4)} and a poison level of
 * 50 % or more adds 5, exactly matching the original's output metadata.</p>
 */
public class RBMKFuelDisassemblyRecipe extends CustomRecipe {

    //? if < 1.21.1 {
    public RBMKFuelDisassemblyRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }
    //?} else {
    /*// 1.21 fuehrt Rezepte ueber RecipeHolder; der Konstruktor bekommt keine Id mehr.
    public RBMKFuelDisassemblyRecipe(CraftingBookCategory category) {
        super(category);
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public boolean matches(CraftingContainer container, Level level) {
    //?} else {
    /*@Override
    public boolean matches(net.minecraft.world.item.crafting.CraftingInput container, Level level) {
    *///?}
        ItemStack stack = getOnlyStack(container);
        if (stack == null || !(stack.getItem() instanceof RBMKRodItem rod)) return false;
        return rod.getPellet() != null
                && RBMKRodItem.getHullHeat(stack) < 50
                && RBMKRodItem.getCoreHeat(stack) < 50;
    }

    //? if < 1.21.1 {
    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
    //?} else {
    /*@Override
    public ItemStack assemble(net.minecraft.world.item.crafting.CraftingInput container,
                              net.minecraft.core.HolderLookup.Provider registries) {
    *///?}
        ItemStack stack = getOnlyStack(container);
        if (stack == null || !(stack.getItem() instanceof RBMKRodItem rod)) return ItemStack.EMPTY;

        RBMKPelletItem pellet = rod.getPellet();
        if (pellet == null) return ItemStack.EMPTY;

        double enrichment = RBMKRodItem.getEnrichment(stack);
        if (enrichment > 0.99D) return ItemStack.EMPTY;

        if (RBMKRodItem.getHullHeat(stack) >= 50 || RBMKRodItem.getCoreHeat(stack) >= 50)
            return ItemStack.EMPTY;

        int tier = 4 - Mth.clamp((int) Math.ceil(enrichment * 5 - 1), 0, 4);
        int state = tier + (RBMKRodItem.getPoisonLevel(stack) >= 0.5D ? 5 : 0);
        // Pellets without xenon variants only have the five enrichment states.
        if (!pellet.isXenonEnabled()) state = tier;

        return pellet.withState(8, state);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    /**
     * CE's {@code ItemRBMKRod} constructor calls {@code setContainerItem(ModItems.rbmk_fuel_empty)},
     * so taking a rod apart hands the empty casing back rather than destroying it - a rod is worth
     * eight pellets <b>and</b> the casing they go back into. The port returned nothing at all, which
     * quietly deleted one casing per disassembly and made the fuel loop lossy.
     */
    //? if < 1.21.1 {
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < container.getContainerSize(); i++) {
    //?} else {
    /*@Override
    public NonNullList<ItemStack> getRemainingItems(net.minecraft.world.item.crafting.CraftingInput container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(container.size(), ItemStack.EMPTY);
        for (int i = 0; i < container.size(); i++) {
    *///?}
            if (container.getItem(i).getItem() instanceof RBMKRodItem) {
                remaining.set(i, new ItemStack(com.hbm_m.item.ModItems.RBMK_FUEL_EMPTY.get()));
                break;
            }
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.RBMK_FUEL_DISASSEMBLY_SERIALIZER.get();
    }

    /** RBMKFuelCraftingHandler.hasExactlyOneStack + getFirstStack, combined. */
    //? if < 1.21.1 {
    private static ItemStack getOnlyStack(CraftingContainer container) {
        ItemStack found = null;
        for (int i = 0; i < container.getContainerSize(); i++) {
    //?} else {
    /*private static ItemStack getOnlyStack(net.minecraft.world.item.crafting.CraftingInput container) {
        ItemStack found = null;
        for (int i = 0; i < container.size(); i++) {
    *///?}
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            if (found != null) return null;
            found = stack;
        }
        return found;
    }

    public static final SimpleCraftingRecipeSerializer<RBMKFuelDisassemblyRecipe> SERIALIZER =
            new SimpleCraftingRecipeSerializer<>(RBMKFuelDisassemblyRecipe::new);
}
