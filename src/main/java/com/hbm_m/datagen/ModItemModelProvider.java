package com.hbm_m.datagen;
// Провайдер генерации моделей предметов для мода.
// Здесь мы определяем, как будут выглядеть наши предметы в инвентаре и в мире.
// Используется в классе DataGenerators для регистрации.

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModIngots;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, RefStrings.MODID, existingFileHelper);
    }

    /**
     * Здесь мы регистрируем все модели для наших предметов.
     */
    @Override
    protected void registerModels() {
        // АВТОМАТИЧЕСКАЯ ГЕНЕРАЦИЯ МОДЕЛЕЙ ДЛЯ СЛИТКОВ 
        // Проходимся по всем слиткам из нашего enum'а
        for (ModIngots ingot : ModIngots.values()) {
            // Получаем объект-обертку предмета
            RegistryObject<Item> ingotObject = ModItems.getIngot(ingot);
            
            // Вызываем наш вспомогательный метод для генерации простой модели
            ingotItem(ingotObject);
        }

        withExistingParent("large_vehicle_door", 
            modLoc("block/large_vehicle_door"));

        // РЕГИСТРАЦИЯ МОДЕЛЕЙ ДЛЯ УНИКАЛЬНЫХ ПРЕДМЕТОВ 
        // Для предметов, зарегистрированных вручную, мы также можем генерировать модели.
        simpleItem(ModItems.ALLOY_SWORD);
        simpleItem(ModItems.GEIGER_COUNTER);
        simpleItem(ModItems.DOSIMETER);
        simpleItem(ModItems.HEART_PIECE);
        simpleItem(ModItems.HEART_CONTAINER);
        simpleItem(ModItems.HEART_BOOSTER);
        simpleItem(ModItems.HEART_FAB);
        simpleItem(ModItems.BLACK_DIAMOND);
        simpleItem(ModItems.GHIORSIUM_CLADDING);
        simpleItem(ModItems.DESH_CLADDING);
        simpleItem(ModItems.LEAD_CLADDING);
        simpleItem(ModItems.RUBBER_CLADDING);
        simpleItem(ModItems.PAINT_CLADDING);
        simpleItem(ModItems.RADAWAY);
        simpleItem(ModItems.CREATIVE_BATTERY);
        simpleItem(ModItems.TEMPLATE_FOLDER);
        simpleItem(ModItems.STRAWBERRY);

        simpleItem(ModItems.BATTLE_SENSOR);
        simpleItem(ModItems.BATTLE_CASING);
        simpleItem(ModItems.BATTLE_COUNTER);
        simpleItem(ModItems.BATTLE_MODULE);
        simpleItem(ModItems.METAL_ROD);


        simpleItem(ModItems.PLATE_IRON);
        simpleItem(ModItems.PLATE_STEEL);
        simpleItem(ModItems.PLATE_GOLD);
        simpleItem(ModItems.PLATE_GUNMETAL);
        simpleItem(ModItems.PLATE_TITANIUM);
        simpleItem(ModItems.PLATE_GUNSTEEL);
        simpleItem(ModItems.PLATE_KEVLAR);
        simpleItem(ModItems.PLATE_LEAD);
        simpleItem(ModItems.PLATE_MIXED);
        simpleItem(ModItems.PLATE_PAA);
        simpleItem(ModItems.PLATE_POLYMER);
        simpleItem(ModItems.PLATE_SATURNITE);
        simpleItem(ModItems.PLATE_SCHRABIDIUM);
        simpleItem(ModItems.PLATE_ADVANCED_ALLOY);
        simpleItem(ModItems.PLATE_ALUMINUM);
        simpleItem(ModItems.PLATE_COPPER);
        simpleItem(ModItems.PLATE_BISMUTH);
        simpleItem(ModItems.PLATE_ARMOR_AJR);
        simpleItem(ModItems.PLATE_ARMOR_DNT);
        simpleItem(ModItems.PLATE_ARMOR_DNT_RUSTED);
        simpleItem(ModItems.PLATE_ARMOR_FAU);
        simpleItem(ModItems.PLATE_ARMOR_HEV);
        simpleItem(ModItems.PLATE_ARMOR_LUNAR);
        simpleItem(ModItems.PLATE_ARMOR_TITANIUM);
        simpleItem(ModItems.PLATE_CAST);
        simpleItem(ModItems.PLATE_CAST_ALT);
        simpleItem(ModItems.PLATE_CAST_BISMUTH);
        simpleItem(ModItems.PLATE_CAST_DARK);
        simpleItem(ModItems.PLATE_COMBINE_STEEL);
        simpleItem(ModItems.PLATE_DURA_STEEL);
        simpleItem(ModItems.PLATE_DALEKANIUM);
        simpleItem(ModItems.PLATE_DESH);
        simpleItem(ModItems.PLATE_DINEUTRONIUM);
        simpleItem(ModItems.PLATE_EUPHEMIUM);
        simpleItem(ModItems.PLATE_FUEL_MOX);
        simpleItem(ModItems.PLATE_FUEL_PU238BE);
        simpleItem(ModItems.PLATE_FUEL_PU239);
        simpleItem(ModItems.PLATE_FUEL_RA226BE);
        simpleItem(ModItems.PLATE_FUEL_SA326);
        simpleItem(ModItems.PLATE_FUEL_U233);
        simpleItem(ModItems.PLATE_FUEL_U235);

    };

    /**
     * Вспомогательный метод для генерации простой модели предмета.
     * Он предполагает, что модель имеет родителя "item/generated" и одну текстуру "layer0".
     * Это стандарт для большинства 2D предметов в Minecraft.
     * @param itemObject RegistryObject предмета, для которого генерируется модель.
     */

    private void simpleItem(RegistryObject<Item> itemObject) {
        // Получаем имя предмета из его ID (например, "uranium_ingot")
        String name = itemObject.getId().getPath();
        
        // Генерируем .json файл модели.
        // Он будет искать текстуру по пути 'assets/hbm_m/textures/item/ИМЯ_ПРЕДМЕТА.png'
        withExistingParent(name, "item/generated")
                .texture("layer0", modLoc("item/" + name));

    }
    
    private void ingotItem(RegistryObject<Item> itemObject) {
        // 1. Получаем регистрационное имя (например, "uranium_ingot")
        String registrationName = itemObject.getId().getPath();
        
        // 2. Получаем базовое имя (например, "uranium")
        String baseName = registrationName.replace("_ingot", "");

        // 3. Формируем ИМЯ ФАЙЛА ТЕКСТУРЫ (например, "ingot_uranium")
        String textureFileName = "ingot_" + baseName;
        
        // Генерируем .json файл модели
        // Имя файла модели совпадает с регистрационным именем
        withExistingParent(registrationName, "item/generated")
                // Путь к текстуре теперь использует правильное имя файла и подпапку
                .texture("layer0", modLoc("item/ingot/" + textureFileName));
    }
}