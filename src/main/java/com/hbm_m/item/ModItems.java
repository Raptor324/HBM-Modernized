//src\main\java\com\hbm_m\item\ModItems.java
package com.hbm_m.item;

import com.hbm_m.lib.RefStrings;
//import com.hbm_m.main.ModCreativeTabs;
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
            
    // Радиоактивные материалы
    public static final RegistryObject<Item> URANIUM_INGOT = ITEMS.register("uranium_ingot",
            () -> new RadioactiveItem(new Item.Properties(), 0.35F, "ingotUranium", "ingotUranium235"));

    // Инструменты
    public static final RegistryObject<Item> GEIGER_COUNTER = ITEMS.register("geiger_counter",
            () -> new GeigerCounterItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DOSIMETER = ITEMS.register("dosimeter",
            () -> new DosimeterItem(new Item.Properties().stacksTo(1)));

    // Метод для регистрации всех предметов, вызывается в основном классе мода.
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
