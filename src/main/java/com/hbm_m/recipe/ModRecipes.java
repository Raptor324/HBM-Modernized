package com.hbm_m.recipe;

import com.hbm_m.lib.RefStrings;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, RefStrings.MODID);

        public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, RefStrings.MODID);

        public static final RegistryObject<RecipeSerializer<AssemblerRecipe>> ASSEMBLER_SERIALIZER =
                SERIALIZERS.register("assembler", () -> AssemblerRecipe.Serializer.INSTANCE);
                
        public static final RegistryObject<RecipeType<AssemblerRecipe>> ASSEMBLER_TYPE =
                RECIPE_TYPES.register("assembler", () -> AssemblerRecipe.Type.INSTANCE);


        public static void register(IEventBus eventBus) {
            SERIALIZERS.register(eventBus);
            RECIPE_TYPES.register(eventBus);
        }
}
