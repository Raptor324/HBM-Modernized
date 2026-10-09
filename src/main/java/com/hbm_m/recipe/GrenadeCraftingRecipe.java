package com.hbm_m.recipe;

import java.util.function.Function;

import com.hbm_m.item.weapon.grenade.GrenadeItems;
import com.hbm_m.item.weapon.grenade.ItemGrenadeExtra;
import com.hbm_m.item.weapon.grenade.ItemGrenadeExtra.EnumGrenadeExtra;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFilling;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFuze;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm_m.item.weapon.grenade.ItemGrenadeShell;
import com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm_m.item.weapon.grenade.ItemGrenadeUniversal;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code GrenadeCraftingHandler}: genau eine Huelle, eine passende Fuellung, ein Zuender und hoechstens ein Extra
 * (keine fremden Gegenstaende, hoechstens 4 Teile) ergeben die entsprechende Baukastengranate.
 */
public class GrenadeCraftingRecipe extends CustomRecipe {

    //? if < 1.21.1 {
    public GrenadeCraftingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }
    //?} else {
    /*public GrenadeCraftingRecipe(CraftingBookCategory category) {
        super(category);
    }
    *///?}

    @Override
    //? if < 1.21.1 {
    public boolean matches(CraftingContainer inv, Level world) {
    //?} else {
    /*public boolean matches(net.minecraft.world.item.crafting.CraftingInput inv, Level world) {
    *///?}
        if (hasForeignObject(inv)) return false; // can't be non-grenade items and can't be more than 4 items total
        EnumGrenadeShell shell = getFirst(inv, ItemGrenadeShell.class, i -> ((ItemGrenadeShell) i).type); // only one shell, null otherwise
        EnumGrenadeFilling filling = getFirst(inv, ItemGrenadeFilling.class, i -> ((ItemGrenadeFilling) i).type); // only one filling, null otherwise
        if (filling != null && shell != null && !filling.compatibleShells.contains(shell)) return false;
        EnumGrenadeFuze fuze = getFirst(inv, ItemGrenadeFuze.class, i -> ((ItemGrenadeFuze) i).type); // only one fuze, null otherwise
        // this leaves the extra unaccounted for, but the restrictions we put in place will allow exactly one without dedicated check
        return shell != null && filling != null && fuze != null;
    }

    @Override
    //? if < 1.21.1 {
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registries) {
    //?} else {
    /*public ItemStack assemble(net.minecraft.world.item.crafting.CraftingInput inv, net.minecraft.core.HolderLookup.Provider registries) {
    *///?}
        EnumGrenadeShell shell = getFirst(inv, ItemGrenadeShell.class, i -> ((ItemGrenadeShell) i).type);
        EnumGrenadeFilling filling = getFirst(inv, ItemGrenadeFilling.class, i -> ((ItemGrenadeFilling) i).type);
        EnumGrenadeFuze fuze = getFirst(inv, ItemGrenadeFuze.class, i -> ((ItemGrenadeFuze) i).type);
        EnumGrenadeExtra extra = getFirst(inv, ItemGrenadeExtra.class, i -> ((ItemGrenadeExtra) i).type); // if this is null, then we don't care, MAKE works with a null extra too
        if (shell == null || filling == null || fuze == null) return ItemStack.EMPTY;
        return ItemGrenadeUniversal.make(shell, filling, fuze, extra);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 4;
    }

    @Override
    //? if < 1.21.1 {
    public ItemStack getResultItem(RegistryAccess registries) {
    //?} else {
    /*public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) {
    *///?}
        return new ItemStack(GrenadeItems.GRENADE_UNIVERSAL.get());
    }

    // why write the same crap four times when you can just use your massive cock instead
    //? if < 1.21.1 {
    private static <T extends Enum<T>> T getFirst(CraftingContainer inv, Class<? extends Item> itemType, Function<Item, T> typeOf) {
        T first = null;

        for (int i = 0; i < inv.getContainerSize(); i++) {
    //?} else {
    /*private static <T extends Enum<T>> T getFirst(net.minecraft.world.item.crafting.CraftingInput inv, Class<? extends Item> itemType, Function<Item, T> typeOf) {
        T first = null;

        for (int i = 0; i < inv.size(); i++) {
    *///?}
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (itemType.isInstance(stack.getItem())) {
                if (first != null) return null;
                first = typeOf.apply(stack.getItem());
            }
        }

        return first;
    }

    // this should weed out non-grenade grids quickly as to not waste too much CPU time
    //? if < 1.21.1 {
    private static boolean hasForeignObject(CraftingContainer inv) {
        int itemCount = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
    //?} else {
    /*private static boolean hasForeignObject(net.minecraft.world.item.crafting.CraftingInput inv) {
        int itemCount = 0;
        for (int i = 0; i < inv.size(); i++) {
    *///?}
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (!(item instanceof ItemGrenadeShell) && !(item instanceof ItemGrenadeFilling) && !(item instanceof ItemGrenadeFuze) && !(item instanceof ItemGrenadeExtra)) return true;
            itemCount++;
            if (itemCount > 4) return true;
        }
        return false;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GRENADE_CRAFTING_SERIALIZER.get();
    }

    public static final SimpleCraftingRecipeSerializer<GrenadeCraftingRecipe> SERIALIZER =
            new SimpleCraftingRecipeSerializer<>(GrenadeCraftingRecipe::new);
}
