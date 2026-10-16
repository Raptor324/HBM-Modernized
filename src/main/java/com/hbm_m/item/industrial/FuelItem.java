package com.hbm_m.item.industrial;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.LoreTooltipItem;

/** Предмет-топливо: тики горения задаются при регистрации (значения оригинального
 *  FuelHandler 1.7.10). Работает и как печное топливо, и в машинах (пресс и т.п.),
 *  т.к. читается через IForgeItem/IItemExtension#getBurnTime. */
public class FuelItem extends LoreTooltipItem {
    private final int burnTime;

    public FuelItem(Properties pProperties, int burnTime) {
        this(null, pProperties, burnTime);
    }

    /** Топливо с lore-тултипом (как оригинальные desc-строки). */
    public FuelItem(@Nullable List<Component> lore, Properties pProperties, int burnTime) {
        super(lore == null ? List.of() : lore, pProperties);
        this.burnTime = burnTime;
    }

    // Forge/NeoForge: IForgeItem/IItemExtension; на Fabric у Item нет этого метода.
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        return this.burnTime;
    }
}
