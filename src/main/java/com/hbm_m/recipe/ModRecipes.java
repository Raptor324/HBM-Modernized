package com.hbm_m.recipe;

import com.hbm_m.lib.RefStrings;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import dev.architectury.registry.registries.RegistrySupplier;



public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(RefStrings.MODID, Registries.RECIPE_SERIALIZER);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(RefStrings.MODID, Registries.RECIPE_TYPE);




    public static final RegistrySupplier<RecipeSerializer<AssemblerRecipe>> ASSEMBLER_SERIALIZER =
            SERIALIZERS.register("assembler", () -> AssemblerRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<AssemblerRecipe>> ASSEMBLER_TYPE =
            RECIPE_TYPES.register("assembler", () -> AssemblerRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<ChemicalPlantRecipe>> CHEMICAL_PLANT_SERIALIZER =
            SERIALIZERS.register("chemical_plant", () -> ChemicalPlantRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<ChemicalPlantRecipe>> CHEMICAL_PLANT_TYPE =
            RECIPE_TYPES.register("chemical_plant", () -> ChemicalPlantRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<PressRecipe>> PRESS_SERIALIZER =
            SERIALIZERS.register("press", () -> PressRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<PressRecipe>> PRESS_TYPE =
            RECIPE_TYPES.register("press", () -> PressRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<ArcFurnaceRecipe>> ARC_FURNACE_SERIALIZER =
            SERIALIZERS.register("arc_furnace", () -> ArcFurnaceRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<ArcFurnaceRecipe>> ARC_FURNACE_TYPE =
            RECIPE_TYPES.register("arc_furnace", () -> ArcFurnaceRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<AmmoPressRecipe>> AMMO_PRESS_SERIALIZER =
            SERIALIZERS.register("ammo_press", () -> AmmoPressRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<AmmoPressRecipe>> AMMO_PRESS_TYPE =
            RECIPE_TYPES.register("ammo_press", () -> AmmoPressRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<PurexRecipe>> PUREX_SERIALIZER =
            SERIALIZERS.register("purex", () -> PurexRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<PurexRecipe>> PUREX_TYPE =
            RECIPE_TYPES.register("purex", () -> PurexRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<BlastFurnaceRecipe>> BLAST_FURNACE_SERIALIZER =
            SERIALIZERS.register("blast_furnace", () -> BlastFurnaceRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<BlastFurnaceRecipe>> BLAST_FURNACE_TYPE =
            RECIPE_TYPES.register("blast_furnace", () -> BlastFurnaceRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<ShredderRecipe>> SHREDDER_SERIALIZER =
            SERIALIZERS.register("shredding", () -> ShredderRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<ShredderRecipe>> SHREDDER_TYPE =
            RECIPE_TYPES.register("shredding", () -> ShredderRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<CentrifugeRecipe>> CENTRIFUGE_SERIALIZER =
            SERIALIZERS.register("centrifuge", () -> CentrifugeRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<CentrifugeRecipe>> CENTRIFUGE_TYPE =
            RECIPE_TYPES.register("centrifuge", () -> CentrifugeRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<AnvilRecipe>> ANVIL_SERIALIZER =
            SERIALIZERS.register("anvil", () -> AnvilRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<AnvilRecipe>> ANVIL_TYPE =
            RECIPE_TYPES.register("anvil", () -> AnvilRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<CombinationOvenRecipe>> COMBINATION_OVEN_SERIALIZER =
            SERIALIZERS.register("combination_oven", () -> CombinationOvenRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<CombinationOvenRecipe>> COMBINATION_OVEN_TYPE =
            RECIPE_TYPES.register("combination_oven", () -> CombinationOvenRecipe.Type.INSTANCE);



    public static void init() {
        SERIALIZERS.register();
        RECIPE_TYPES.register();
    }
}
