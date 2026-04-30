package com.hbm_m.item.tools_and_armor;

import com.hbm_m.item.ModItems;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

//? if forge {
/*import com.hbm_m.item.tags_and_tiers.ModTags;
import com.hbm_m.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;

import java.util.List;
*///?}

public class ModToolTiers {
    public static final Tier ALLOY =
            //? if forge {
            /*TierSortingRegistry.registerTier(
                    new ForgeTier(4, 1500, 6f, 6f, 25,
                            ModTags.Blocks.NEEDS_ALLOY_TOOL, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),
                    new ResourceLocation(MainRegistry.MOD_ID, "alloy"), List.of(Tiers.NETHERITE), List.of()
            );
            *///?} else {

            new SimpleTier(4, 1500, 6f, 6f, 25, () -> Ingredient.of(ModItems.PLATE_STEEL.get()));
            //?}

    public static final Tier STEEL =
            //? if forge {
            /*TierSortingRegistry.registerTier(
                    new ForgeTier(3, 600, 4f, 4f, 18,
                            ModTags.Blocks.NEEDS_STEEL_TOOL, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),
                    new ResourceLocation(MainRegistry.MOD_ID, "steel"), List.of(Tiers.IRON), List.of()
            );
            *///?} else {

            new SimpleTier(3, 600, 4f, 4f, 18, () -> Ingredient.of(ModItems.PLATE_STEEL.get()));
            //?}


    public static final Tier STARMETAL =
            //? if forge {
            /*TierSortingRegistry.registerTier(
                    new ForgeTier(5, 19000, 9f, 8f, 25,
                            ModTags.Blocks.NEEDS_STARMETAL_TOOL, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),
                    new ResourceLocation(MainRegistry.MOD_ID, "starmetal"), List.of(Tiers.IRON), List.of()
            );
            *///?} else {

            new SimpleTier(5, 19000, 9f, 8f, 25, () -> Ingredient.of(ModItems.PLATE_STEEL.get()));
            //?}


    public static final Tier TITANIUM =
            //? if forge {
            /*TierSortingRegistry.registerTier(
                    new ForgeTier(3, 750, 3.25f, 3f, 15,
                            ModTags.Blocks.NEEDS_TITANIUM_TOOL, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),
                    new ResourceLocation(MainRegistry.MOD_ID, "titanium"), List.of(Tiers.IRON), List.of()
            );
            *///?} else {

            new SimpleTier(3, 750, 3.25f, 3f, 15, () -> Ingredient.of(ModItems.PLATE_STEEL.get()));
            //?}

    /**
     * Fabric/NeoForge: {@link ModItems} регистрирует инструменты до {@code plate_steel}; Architectury при
     * {@code ITEMS.register()} сразу создаёт предметы → нельзя звать {@code PLATE_STEEL.get()} в {@code <clinit>}.
     * Ингредиент ремонта откладываем до первого {@link #getRepairIngredient()} (как {@link ForgeTier} на Forge).
     */
    private static final class SimpleTier implements Tier {
        private final int level;
        private final int uses;
        private final float speed;
        private final float attackDamageBonus;
        private final int enchantmentValue;
        private final Supplier<Ingredient> repairIngredient;
        private Ingredient resolvedRepair;

        private SimpleTier(int level, int uses, float speed, float attackDamageBonus, int enchantmentValue,
                Supplier<Ingredient> repairIngredient) {
            this.level = level;
            this.uses = uses;
            this.speed = speed;
            this.attackDamageBonus = attackDamageBonus;
            this.enchantmentValue = enchantmentValue;
            this.repairIngredient = repairIngredient;
        }

        @Override
        public int getUses() {
            return uses;
        }

        @Override
        public float getSpeed() {
            return speed;
        }

        @Override
        public float getAttackDamageBonus() {
            return attackDamageBonus;
        }

        @Override
        public int getLevel() {
            return level;
        }

        @Override
        public int getEnchantmentValue() {
            return enchantmentValue;
        }

        @Override
        public Ingredient getRepairIngredient() {
            if (resolvedRepair == null) {
                resolvedRepair = repairIngredient.get();
            }
            return resolvedRepair;
        }
    }
}
