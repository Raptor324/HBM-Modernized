package com.hbm_m.item;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    // Создаем отложенный регистратор для предметов.
    // Это стандартный способ регистрации объектов в Forge.
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, RefStrings.MODID);

    // Регистрируем наш меч.
    // RegistryObject<Item> будет содержать ссылку на зарегистрированный предмет.
    public static final RegistryObject<Item> ALLOY_SWORD = ITEMS.register("alloy_sword",
            () -> new AlloySword()); // Имя файла меча будет "alloy_sword"

    // Метод для регистрации всех предметов, вызывается в основном классе мода.
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
