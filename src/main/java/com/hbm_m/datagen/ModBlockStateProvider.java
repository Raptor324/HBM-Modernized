package com.hbm_m.datagen;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, RefStrings.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // --- ГЕНЕРАЦИЯ МОДЕЛЕЙ ДЛЯ БЛОКОВ-РЕСУРСОВ С ПРЕФИКСОМ "block_" ---

        resourceBlockWithItem(ModBlocks.URANIUM_BLOCK);
        resourceBlockWithItem(ModBlocks.PLUTONIUM_BLOCK);
        resourceBlockWithItem(ModBlocks.PLUTONIUM_FUEL_BLOCK);
        resourceBlockWithItem(ModBlocks.POLONIUM210_BLOCK);
        
        oreWithItem(ModBlocks.URANIUM_ORE);

        blockWithItem(ModBlocks.WASTE_LEAVES);

        columnBlockWithItem(
            ModBlocks.WASTE_GRASS, 
            modLoc("block/waste_grass_side"), // Текстура из нашего мода
            modLoc("block/waste_grass_top"),  // Текстура из нашего мода
            mcLoc("block/dirt")               // Текстура из ВАНИЛЬНОГО Minecraft
        );

        // Блок типа "ориентируемый"
        directionalBlockWithItem(
            ModBlocks.ARMOR_TABLE,
            modLoc("block/armor_table_bottom"), // Низ
            modLoc("block/armor_table_top"),    // Верх
            modLoc("block/armor_table_side"),   // Север (лицо)
            modLoc("block/armor_table_side"),   // Юг (зад)
            modLoc("block/armor_table_side"),   // Запад (лево)
            modLoc("block/armor_table_side")    // Восток (право)
        );

        // Блок с кастомной OBJ моделью
        customObjBlock(ModBlocks.GEIGER_COUNTER_BLOCK);
        customObjBlock(ModBlocks.MACHINE_ASSEMBLER);

        simpleBlock(ModBlocks.MACHINE_ASSEMBLER_PART.get(), models().getBuilder(ModBlocks.MACHINE_ASSEMBLER_PART.getId().getPath()));   
    }

    /**
     * НОВЫЙ метод для блоков, у которых текстура имеет префикс "block_".
     * Например, для блока с именем "uranium_block" он будет искать текстуру "block_uranium".
     */
    private void resourceBlockWithItem(RegistryObject<Block> blockObject) {
        // 1. Получаем регистрационное имя блока (например, "uranium_block")
        String registrationName = blockObject.getId().getPath();
        
        // 2. Трансформируем его в базовое имя (удаляем "_block" -> "uranium")
        String baseName = registrationName.replace("_block", "");

        // 3. Создаем имя файла текстуры (добавляем "block_" -> "block_uranium")
        String textureName = "block_" + baseName;

        // 4. Создаем модель блока, ЯВНО указывая путь к текстуре
        //    Метод models().cubeAll() создает модель типа "block/cube_all" с указанной текстурой.
        simpleBlock(blockObject.get(), models().cubeAll(registrationName, modLoc("block/" + textureName)));

        // 5. Создаем модель для предмета-блока, как и раньше
        simpleBlockItem(blockObject.get(), models().getExistingFile(blockTexture(blockObject.get())));
    }
    private void oreWithItem(RegistryObject<Block> blockObject) {
        // 1. Получаем регистрационное имя блока (например, "uranium_block")
        String registrationName = blockObject.getId().getPath();
        
        // 2. Трансформируем его в базовое имя (удаляем "_block" -> "uranium")
        String baseName = registrationName.replace("_ore", "");

        // 3. Создаем имя файла текстуры (добавляем "ore_" -> "ore_uranium")
        String textureName = "ore_" + baseName;

        // 4. Создаем модель блока, ЯВНО указывая путь к текстуре
        //    Метод models().cubeAll() создает модель типа "block/cube_all" с указанной текстурой.
        simpleBlock(blockObject.get(), models().cubeAll(registrationName, modLoc("block/" + textureName)));

        // 5. Создаем модель для предмета-блока, как и раньше
        simpleBlockItem(blockObject.get(), models().getExistingFile(blockTexture(blockObject.get())));
    }

    /**
     * Старый метод для блоков, у которых имя текстуры СОВПАДАЕТ с именем регистрации.
     */
    private void blockWithItem(RegistryObject<Block> blockObject) {
        simpleBlock(blockObject.get());
        simpleBlockItem(blockObject.get(), models().getExistingFile(blockTexture(blockObject.get())));
    }

    /**
     * Генерирует модель и состояние для блока типа "колонна".
     * @param blockObject Блок
     * @param sideTexture Имя файла текстуры для боковых сторон
     * @param topTexture Имя файла текстуры для верха
     * @param bottomTexture Имя файла текстуры для низа
     */
    private void columnBlockWithItem(RegistryObject<Block> blockObject, ResourceLocation sideLocation, ResourceLocation topLocation, ResourceLocation bottomLocation) {
        // Создаем модель блока, передавая готовые ResourceLocation
        simpleBlock(blockObject.get(), models().cubeBottomTop(
            blockObject.getId().getPath(),
            sideLocation,
            bottomLocation,
            topLocation
        ));
        // Создаем модель предмета-блока
        simpleBlockItem(blockObject.get(), models().getExistingFile(blockTexture(blockObject.get())));
    }

    /**
     * Генерирует модель и состояние для горизонтально-ориентированного блока.
     * @param blockObject Блок
     * @param sideTexture Имя файла текстуры для боковых сторон
     * @param frontTexture Имя файла текстуры для лицевой стороны
     * @param topTexture Имя файла текстуры для верха (и низа)
     */
    // private void directionalBlockWithItemFACING(RegistryObject<Block> blockObject, ResourceLocation down, ResourceLocation up, ResourceLocation north, ResourceLocation south, ResourceLocation west, ResourceLocation east) {
    //     // 1. Создаем модель блока с 6-ю разными текстурами.
    //     var model = models().cube(blockObject.getId().getPath(), down, up, north, south, west, east);

    //     // 2. Создаем состояние блока (blockstate), которое будет вращать эту модель по горизонтали.
    //     //    Когда игрок смотрит на юг и ставит блок, блок будет повернут на 180 градусов (y=180),
    //     //    чтобы его "северная" (лицевая) сторона смотрела на игрока.
    //     horizontalBlock(blockObject.get(), model);
        
    //     // 3. Создаем модель для предмета-блока, которая выглядит так же, как и сам блок.
    //     simpleBlockItem(blockObject.get(), model);
    // }

    private void directionalBlockWithItem(RegistryObject<Block> blockObject, ResourceLocation down, ResourceLocation up, ResourceLocation north, ResourceLocation south, ResourceLocation west, ResourceLocation east) {
        // 1. Создаем модель блока с 6-ю разными текстурами. Этот шаг остается прежним.
        var model = models().cube(blockObject.getId().getPath(), down, up, north, south, west, east).texture("particle", north);

        // 2. ИСПОЛЬЗУЕМ simpleBlock ВМЕСТО horizontalBlock.
        //    simpleBlock создаст blockstate.json с одним единственным вариантом,
        //    который не зависит от facing и всегда ссылается на нашу модель.
        simpleBlock(blockObject.get(), model);
        
        // 3. Создаем модель для предмета-блока. Этот шаг остается прежним.
        simpleBlockItem(blockObject.get(), model);
    }
    
    /**
     * Генерирует состояние для блока с кастомной OBJ моделью.
     * ВАЖНО: Сам файл модели (.json) должен быть создан вручную в /resources!
     */
    private void customObjBlock(RegistryObject<Block> blockObject) {
        // Генерируем blockstate, который ссылается на УЖЕ СУЩЕСТВУЮЩИЙ файл модели.
        // Мы не создаем модель, а просто говорим: "используй вот этот файл".
        horizontalBlock(blockObject.get(), models().getExistingFile(modLoc("block/" + blockObject.getId().getPath())));
    }

}