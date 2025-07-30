package com.hbm_m.datagen;

import com.hbm_m.item.ModIngots;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
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
        // --- АВТОМАТИЧЕСКАЯ ГЕНЕРАЦИЯ МОДЕЛЕЙ ДЛЯ СЛИТКОВ ---
        // Проходимся по всем слиткам из нашего enum'а
        for (ModIngots ingot : ModIngots.values()) {
            // Получаем объект-обертку предмета
            RegistryObject<Item> ingotObject = ModItems.getIngot(ingot);
            
            // Вызываем наш вспомогательный метод для генерации простой модели
            ingotItem(ingotObject);
        }

        // --- РЕГИСТРАЦИЯ МОДЕЛЕЙ ДЛЯ УНИКАЛЬНЫХ ПРЕДМЕТОВ ---
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
        
        // --- КАСТОМНАЯ МОДЕЛЬ ДЛЯ ПРЕДМЕТА "СЧЕТЧИК ГЕЙГЕРА" ---
        withExistingParent("geiger_counter_block", modLoc("block/geiger_counter_block"))
            .transforms()
            // Используем ванильный ItemDisplayContext
            .transform(ItemDisplayContext.GUI)
                .rotation(30, 45, 0)
                .translation(0, 1.0f, 0)
                .scale(0.625f)
                .end()
            .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .rotation(75, 45, 0)
                .translation(0, 2.5f, 0)
                .scale(0.375f)
                .end()
            .transform(ItemDisplayContext.HEAD)
                .rotation(0, 0, 0)
                .translation(0, 13, 7)
                .end();
    }

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