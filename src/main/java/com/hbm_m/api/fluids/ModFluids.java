package com.hbm_m.api.fluids;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.hbm_m.main.MainRegistry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
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

public class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, MainRegistry.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, MainRegistry.MOD_ID);

    //=====================================================================================//
    // 1. Тип Жидкости
    //=====================================================================================//

    public static final RegistryObject<FluidType> CRUDE_OIL_TYPE = FLUID_TYPES.register("crude_oil", () -> new FluidType(
            FluidType.Properties.create()
                    .descriptionId("fluid.hbm_m.crude_oil")
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .density(900)
                    .viscosity(2000)

    ) {
        @Override
        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
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
                    return 0xFFFFFFFF;
                }
            });
        }
    });

    //=====================================================================================//
    // 2. Suppliers для Fluid (чтобы избежать ошибок инициализации)
    //=====================================================================================//

    private static final class OilFluidSuppliers {
        static final Supplier<Fluid> SOURCE = () -> OilFluids.source;
        static final Supplier<Fluid> FLOWING = () -> OilFluids.flowing;
    }

    //=====================================================================================//
    // 3. Holder для Fluid объектов
    //=====================================================================================//

    private static final class OilFluids {
        static FlowingFluid source;
        static FlowingFluid flowing;
    }

    //=====================================================================================//
    // 4. Properties (bucket добавляется в ModItems)
    //=====================================================================================//

    private static ForgeFlowingFluid.Properties createOilProperties() {
        return new ForgeFlowingFluid.Properties(
                CRUDE_OIL_TYPE,
                OilFluidSuppliers.SOURCE,
                OilFluidSuppliers.FLOWING
        );
    }

    //=====================================================================================//
    // 5. Регистрация Fluid
    //=====================================================================================//

    public static final RegistryObject<Fluid> CRUDE_OIL_SOURCE = FLUIDS.register("crude_oil_source", () -> {
        OilFluids.source = new ForgeFlowingFluid.Source(createOilProperties());
        OilFluids.flowing = new ForgeFlowingFluid.Flowing(createOilProperties());
        return OilFluids.source;
    });

    public static final RegistryObject<Fluid> CRUDE_OIL_FLOWING = FLUIDS.register("crude_oil_flowing", () -> {
        return OilFluids.flowing;
    });

    //=====================================================================================//
    // 6. Метод регистрации
    //=====================================================================================//

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}