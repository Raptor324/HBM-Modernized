package com.hbm_m.item.industrial;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/** Блок-топливо (блок металлолома): тики горения как в оригинальном FuelHandler 1.7.10. */
public class FuelBlockItem extends BlockItem {
    private final int burnTime;

    public FuelBlockItem(Block block, Properties properties, int burnTime) {
        super(block, properties);
        this.burnTime = burnTime;
    }

    // Forge/NeoForge: IForgeItem/IItemExtension; на Fabric у Item нет этого метода.
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        return this.burnTime;
    }
}
