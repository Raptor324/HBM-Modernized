package com.hbm_m.main;

import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(RefStrings.MODID)
public class MainRegistry {

    public MainRegistry(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModItems.register(modEventBus); // Регистрация наших предметов

        // Здесь можно добавить другие регистрации, например, для блоков, сущностей и т.д.
    }
}
