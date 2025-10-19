package com.hbm_m.recipe;

import com.hbm_m.lib.RefStrings;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;



public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, RefStrings.MODID);

    // DeferredRegister для сериализаторов рецептов
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, RefStrings.MODID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, RefStrings.MODID);




    public static final RegistryObject<RecipeSerializer<AssemblerRecipe>> ASSEMBLER_SERIALIZER =
            SERIALIZERS.register("assembler", () -> AssemblerRecipe.Serializer.INSTANCE);

    public static final RegistryObject<RecipeType<AssemblerRecipe>> ASSEMBLER_TYPE =
            RECIPE_TYPES.register("assembler", () -> AssemblerRecipe.Type.INSTANCE);

    public static final RegistryObject<RecipeSerializer<PressRecipe>> PRESS_SERIALIZER =
            SERIALIZERS.register("press", () -> PressRecipe.Serializer.INSTANCE);

    public static final RegistryObject<RecipeType<PressRecipe>> PRESS_TYPE =
            RECIPE_TYPES.register("press", () -> PressRecipe.Type.INSTANCE);

    public static final RegistryObject<RecipeSerializer<BlastFurnaceRecipe>> BLAST_FURNACE_SERIALIZER =
            SERIALIZERS.register("blast_furnace", () -> BlastFurnaceRecipe.Serializer.INSTANCE);

    public static final RegistryObject<RecipeType<BlastFurnaceRecipe>> BLAST_FURNACE_TYPE =
            RECIPE_TYPES.register("blast_furnace", () -> BlastFurnaceRecipe.Type.INSTANCE);

    public static final RegistryObject<RecipeSerializer<ShredderRecipe>> SHREDDER_SERIALIZER =
            SERIALIZERS.register("shredding", () -> ShredderRecipe.Serializer.INSTANCE);

    public static final RegistryObject<RecipeSerializer<AnvilRecipe>> ANVIL_SERIALIZER =
            SERIALIZERS.register("anvil", () -> AnvilRecipe.Serializer.INSTANCE);

    public static final RegistryObject<RecipeType<AnvilRecipe>> ANVIL_TYPE =
            RECIPE_TYPES.register("anvil", () -> AnvilRecipe.Type.INSTANCE);



    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        RECIPE_TYPES.register(eventBus);


    }
}
