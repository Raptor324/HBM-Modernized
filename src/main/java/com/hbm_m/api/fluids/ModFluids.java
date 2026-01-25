package com.hbm_m.api.fluids;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.main.MainRegistry; // Твой главный класс
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;

import java.util.function.Consumer;

public class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, MainRegistry.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, MainRegistry.MOD_ID);
    // Не забудь добавить регистр блоков и предметов, если их еще нет

    //=====================================================================================//
    // 1. Тип Жидкости (свойства: звук, вязкость, плотность)
    //=====================================================================================//

    // 1. Тип Жидкости (свойства: звук, вязкость, плотность)
    public static final RegistryObject<FluidType> CRUDE_OIL_TYPE = FLUID_TYPES.register("crude_oil", () -> new FluidType(
            FluidType.Properties.create()
                    .descriptionId("fluid.hbm_m.crude_oil")
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .density(900)
                    .viscosity(2000)







    ) {
        // ВОТ ЗДЕСЬ МАГИЯ: Мы сразу указываем клиентские свойства (текстуры)
        @Override
        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                // Пути к текстурам
                private static final ResourceLocation OIL_STILL = new ResourceLocation(MainRegistry.MOD_ID, "block/crude_oil");
                private static final ResourceLocation OIL_FLOW = new ResourceLocation(MainRegistry.MOD_ID, "block/crude_oil");

                @Override
                public ResourceLocation getStillTexture() {
                    return OIL_STILL;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return OIL_FLOW;
                }

                @Override
                public int getTintColor() {
                    return 0xFFFFFFFF; // Белый (без окраски)
                }
            });
        }
    });

    // 2. Сама жидкость (Source - источник, Flowing - течение)
    // Свойства для ForgeFlowingFluid
    private static final ForgeFlowingFluid.Properties OIL_PROPERTIES = new ForgeFlowingFluid.Properties(
            CRUDE_OIL_TYPE,
            ModFluids.CRUDE_OIL_SOURCE,
            ModFluids.CRUDE_OIL_FLOWING
    )
            .bucket(ModItems.CRUDE_OIL_BUCKET);

    public static final RegistryObject<FlowingFluid> CRUDE_OIL_SOURCE = FLUIDS.register("crude_oil_source",
            () -> new ForgeFlowingFluid.Source(OIL_PROPERTIES));

    public static final RegistryObject<FlowingFluid> CRUDE_OIL_FLOWING = FLUIDS.register("crude_oil_flowing",
            () -> new ForgeFlowingFluid.Flowing(OIL_PROPERTIES));

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}
