package com.hbm_m.item.industrial;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Вечное топливо (coal_eternal): не расходуется — является собственным container item,
 *  как в 1.7.10 ({@code setContainerItem(itself)}). Ванильная печь и пресс возвращают
 *  остаток крафта, поэтому предмет горит вечно; стек = 1, вне креатива. */
public class EternalFuelItem extends FuelItem {

    public EternalFuelItem(Properties pProperties, int burnTime) {
        super(pProperties, burnTime);
    }

    // Forge/NeoForge: IForgeItem/IItemExtension — stack-sensitive варианты на обоих лоадерах.
    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack itemStack) {
        return itemStack.copy();
    }
}
