package com.hbm_m.datagen;

// Провайдер генерации состояний блоков и моделей для блоков мода.
// Используется в классе DataGenerators для регистрации.
import com.hbm_m.block.ModBlocks;

import com.hbm_m.lib.RefStrings;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, RefStrings.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {


        // ГЕНЕРАЦИЯ МОДЕЛЕЙ ДЛЯ БЛОКОВ-РЕСУРСОВ С ПРЕФИКСОМ "block_"

        simpleBlockWithItem(ModBlocks.STRAWBERRY_BUSH.get(), models().cross(blockTexture(ModBlocks.STRAWBERRY_BUSH.get()).getPath(),
                blockTexture(ModBlocks.STRAWBERRY_BUSH.get())).renderType("cutout"));
        resourceBlockWithItem(ModBlocks.URANIUM_BLOCK);
        resourceBlockWithItem(ModBlocks.PLUTONIUM_BLOCK);
        resourceBlockWithItem(ModBlocks.PLUTONIUM_FUEL_BLOCK);
        resourceBlockWithItem(ModBlocks.POLONIUM210_BLOCK);
        
        oreWithItem(ModBlocks.URANIUM_ORE);

        blockWithItem(ModBlocks.WASTE_LEAVES);
        blockWithItem(ModBlocks.REINFORCED_STONE);
        blockWithItem(ModBlocks.CONCRETE_HAZARD);
        blockWithItem(ModBlocks.BRICK_CONCRETE);
        blockWithItem(ModBlocks.BRICK_CONCRETE_BROKEN);
        blockWithItem(ModBlocks.BRICK_CONCRETE_CRACKED);
        blockWithItem(ModBlocks.BRICK_CONCRETE_MOSSY);
        blockWithItem(ModBlocks.BRICK_CONCRETE_MARKED);

        blockWithItem(ModBlocks.CONCRETE_MARKED);
        blockWithItem(ModBlocks.CONCRETE_FAN);
        blockWithItem(ModBlocks.CONCRETE_VENT);
        blockWithItem(ModBlocks.CONCRETE_MOSSY);
        blockWithItem(ModBlocks.CONCRETE_CRACKED);
        blockWithItem(ModBlocks.CONCRETE);

        blockWithItem(ModBlocks.SELLAFIELD_SLAKED);
        blockWithItem(ModBlocks.SELLAFIELD_SLAKED1);
        blockWithItem(ModBlocks.SELLAFIELD_SLAKED2);
        blockWithItem(ModBlocks.SELLAFIELD_SLAKED3);

        blockWithItem(ModBlocks.FALLING_SELLAFIT1);
        blockWithItem(ModBlocks.FALLING_SELLAFIT2);
        blockWithItem(ModBlocks.FALLING_SELLAFIT3);
        blockWithItem(ModBlocks.FALLING_SELLAFIT4);

        resourceBlockWithItem(ModBlocks.CRATE);
        resourceBlockWithItem(ModBlocks.CRATE_LEAD);
        resourceBlockWithItem(ModBlocks.CRATE_METAL);
        resourceBlockWithItem(ModBlocks.CRATE_WEAPON);

        blockWithItem(ModBlocks.WASTE_PLANKS);

        simpleBlockWithItem(ModBlocks.WASTE_LOG.get(),
                models().cubeBottomTop(
                        ModBlocks.WASTE_LOG.getId().getPath(),
                        modLoc("block/waste_log_side"),
                        modLoc("block/waste_log_top"),
                        modLoc("block/waste_log_top")
                )
        );


        simpleBlockWithItem(ModBlocks.CRATE_IRON.get(),
                models().cubeBottomTop(
                        ModBlocks.CRATE_IRON.getId().getPath(),
                        modLoc("block/crate_iron_side"),      // Текстура боков
                        modLoc("block/crate_iron_top"),       // Текстура низа
                        modLoc("block/crate_iron_top")        // Текстура верха
                )

        );
        simpleBlockWithItem(ModBlocks.CRATE_STEEL.get(),
                models().cubeBottomTop(
                        ModBlocks.CRATE_STEEL.getId().getPath(),
                        modLoc("block/crate_steel_side"),
                        modLoc("block/crate_steel_top"),
                        modLoc("block/crate_steel_top")
                )
        );
        simpleBlockWithItem(ModBlocks.CRATE_DESH.get(),
                models().cubeBottomTop(
                        ModBlocks.CRATE_DESH.getId().getPath(),
                        modLoc("block/crate_desh_side"),
                        modLoc("block/crate_desh_top"),
                        modLoc("block/crate_desh_top")
                )
        );

        blockWithItem(ModBlocks.CINNABAR_ORE_DEEPSLATE);
        blockWithItem(ModBlocks.COBALT_ORE_DEEPSLATE);
        resourceBlockWithItem(ModBlocks.EXPLOSIVE_CHARGE);
        resourceBlockWithItem(ModBlocks.GIGA_DET);
        simpleBlockWithItem(ModBlocks.REINFORCED_GLASS.get(), 
            models().cubeAll(ModBlocks.REINFORCED_GLASS.getId().getPath(), 
                blockTexture(ModBlocks.REINFORCED_GLASS.get()))
            .renderType("cutout"));

        doorBlockWithRenderType(((DoorBlock) ModBlocks.METAL_DOOR.get()), modLoc("block/metal_door_bottom"), modLoc("block/metal_door_top"), "cutout");

        doorBlockWithRenderType(((DoorBlock) ModBlocks.DOOR_BUNKER.get()), modLoc("block/door_bunker_bottom"), modLoc("block/door_bunker_top"), "cutout");

        doorBlockWithRenderType(((DoorBlock) ModBlocks.DOOR_OFFICE.get()), modLoc("block/door_office_bottom"), modLoc("block/door_office_top"), "cutout");


        columnBlockWithItem(
            ModBlocks.WASTE_GRASS, 
            modLoc("block/waste_grass_side"), // Текстура из нашего мода
            modLoc("block/waste_grass_top"),  // Текстура из нашего мода
            mcLoc("block/dirt")               // Текстура из ВАНИЛЬНОГО Minecraft
        );

        // Блок типа "ориентируемый"
        columnBlockWithItem(
            ModBlocks.ARMOR_TABLE,
            modLoc("block/armor_table_side"),   // Север (лицо)
            modLoc("block/armor_table_top"),    // Верх
            modLoc("block/armor_table_bottom") // Низ
        );

        // Блок с кастомной OBJ моделью
        customObjBlock(ModBlocks.GEIGER_COUNTER_BLOCK);
        customObjBlock(ModBlocks.MACHINE_ASSEMBLER);
        // customObjBlock(ModBlocks.FLUID_TANK);

        customObjBlock(ModBlocks.LARGE_VEHICLE_DOOR);
        customObjBlock(ModBlocks.ROUND_AIRLOCK_DOOR);
        customObjBlock(ModBlocks.TRANSITION_SEAL);
        customObjBlock(ModBlocks.SILO_HATCH);
        customObjBlock(ModBlocks.SILO_HATCH_LARGE);
        customObjBlock(ModBlocks.QE_SLIDING);
        customObjBlock(ModBlocks.QE_CONTAINMENT);
        customObjBlock(ModBlocks.WATER_DOOR);
        customObjBlock(ModBlocks.FIRE_DOOR);
        customObjBlock(ModBlocks.SLIDE_DOOR);
        customObjBlock(ModBlocks.SLIDING_SEAL_DOOR);
        customObjBlock(ModBlocks.SECURE_ACCESS_DOOR);

        simpleBlock(ModBlocks.UNIVERSAL_MACHINE_PART.get(), models().getBuilder(ModBlocks.UNIVERSAL_MACHINE_PART.getId().getPath()));

        simpleBlockWithItem(ModBlocks.WIRE_COATED.get(), models().getExistingFile(modLoc("block/wire_coated")));
        
        orientableBlockWithItem(
            ModBlocks.MACHINE_BATTERY,
            modLoc("block/battery_side_alt"),    // Бока (юг, запад, восток)
            modLoc("block/battery_front_alt"),   // Лицо (север)
            modLoc("block/battery_top")          // Верх и низ
        );

        // Генерация моделей для ступенек
        stairsBlock((StairBlock) ModBlocks.REINFORCED_STONE_STAIRS.get(),
        modLoc("block/reinforced_stone"));
        simpleBlockItem(ModBlocks.REINFORCED_STONE_STAIRS.get(),
        models().getExistingFile(modLoc("block/reinforced_stone_stairs")));

        stairsBlock((StairBlock) ModBlocks.BRICK_CONCRETE_STAIRS.get(),
                modLoc("block/brick_concrete"));
        simpleBlockItem(ModBlocks.BRICK_CONCRETE_STAIRS.get(),
                models().getExistingFile(modLoc("block/brick_concrete_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_STAIRS.get(),
                modLoc("block/concrete"));
        simpleBlockItem(ModBlocks.CONCRETE_STAIRS.get(),
                models().getExistingFile(modLoc("block/concrete_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_CRACKED_STAIRS.get(),
                modLoc("block/concrete_cracked"));
        simpleBlockItem(ModBlocks.CONCRETE_CRACKED_STAIRS.get(),
                models().getExistingFile(modLoc("block/concrete_cracked_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_MOSSY_STAIRS.get(),
                modLoc("block/concrete_mossy"));
        simpleBlockItem(ModBlocks.CONCRETE_MOSSY_STAIRS.get(),
                models().getExistingFile(modLoc("block/concrete_mossy_stairs")));

        stairsBlock((StairBlock) ModBlocks.BRICK_CONCRETE_BROKEN_STAIRS.get(),
                modLoc("block/brick_concrete_broken"));
        simpleBlockItem(ModBlocks.BRICK_CONCRETE_BROKEN_STAIRS.get(),
                models().getExistingFile(modLoc("block/brick_concrete_broken_stairs")));

        stairsBlock((StairBlock) ModBlocks.BRICK_CONCRETE_CRACKED_STAIRS.get(),
                modLoc("block/brick_concrete_cracked"));
        simpleBlockItem(ModBlocks.BRICK_CONCRETE_CRACKED_STAIRS.get(),
                models().getExistingFile(modLoc("block/brick_concrete_cracked_stairs")));

        stairsBlock((StairBlock) ModBlocks.BRICK_CONCRETE_MOSSY_STAIRS.get(),
                modLoc("block/brick_concrete_mossy"));
        simpleBlockItem(ModBlocks.BRICK_CONCRETE_MOSSY_STAIRS.get(),
                models().getExistingFile(modLoc("block/brick_concrete_mossy_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_HAZARD_STAIRS.get(),
            modLoc("block/concrete_hazard"));
        simpleBlockItem(ModBlocks.CONCRETE_HAZARD_STAIRS.get(),
            models().getExistingFile(modLoc("block/concrete_hazard_stairs")));

        // Генерация моделей для плиты

        slabBlock((SlabBlock) ModBlocks.CONCRETE_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE.get()),
                modLoc("block/concrete"));
        simpleBlockItem(ModBlocks.CONCRETE_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_MOSSY_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_MOSSY.get()),
                modLoc("block/concrete_mossy"));
        simpleBlockItem(ModBlocks.CONCRETE_MOSSY_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_mossy_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_CRACKED_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_CRACKED.get()),
                modLoc("block/concrete_cracked"));
        simpleBlockItem(ModBlocks.CONCRETE_CRACKED_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_cracked_slab")));

        slabBlock((SlabBlock) ModBlocks.REINFORCED_STONE_SLAB.get(),
            blockTexture(ModBlocks.REINFORCED_STONE.get()),
            modLoc("block/reinforced_stone"));
        simpleBlockItem(ModBlocks.REINFORCED_STONE_SLAB.get(),
            models().getExistingFile(modLoc("block/reinforced_stone_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_HAZARD_SLAB.get(),
            blockTexture(ModBlocks.CONCRETE_HAZARD.get()),
            modLoc("block/concrete_hazard"));
        simpleBlockItem(ModBlocks.CONCRETE_HAZARD_SLAB.get(),
            models().getExistingFile(modLoc("block/concrete_hazard_slab")));

        slabBlock((SlabBlock) ModBlocks.BRICK_CONCRETE_SLAB.get(),
                blockTexture(ModBlocks.BRICK_CONCRETE.get()),
                modLoc("block/brick_concrete"));
        simpleBlockItem(ModBlocks.BRICK_CONCRETE_SLAB.get(),
                models().getExistingFile(modLoc("block/brick_concrete_slab")));

        slabBlock((SlabBlock) ModBlocks.BRICK_CONCRETE_MOSSY_SLAB.get(),
                blockTexture(ModBlocks.BRICK_CONCRETE_MOSSY.get()),
                modLoc("block/brick_concrete_mossy"));
        simpleBlockItem(ModBlocks.BRICK_CONCRETE_MOSSY_SLAB.get(),
                models().getExistingFile(modLoc("block/brick_concrete_mossy_slab")));

        slabBlock((SlabBlock) ModBlocks.BRICK_CONCRETE_CRACKED_SLAB.get(),
                blockTexture(ModBlocks.BRICK_CONCRETE_CRACKED.get()),
                modLoc("block/brick_concrete_cracked"));
        simpleBlockItem(ModBlocks.BRICK_CONCRETE_CRACKED_SLAB.get(),
                models().getExistingFile(modLoc("block/brick_concrete_cracked_slab")));

        slabBlock((SlabBlock) ModBlocks.BRICK_CONCRETE_BROKEN_SLAB.get(),
                blockTexture(ModBlocks.BRICK_CONCRETE_BROKEN.get()),
                modLoc("block/brick_concrete_broken"));
        simpleBlockItem(ModBlocks.BRICK_CONCRETE_BROKEN_SLAB.get(),
                models().getExistingFile(modLoc("block/brick_concrete_broken_slab")));

        simpleBlockWithItem(ModBlocks.SHREDDER.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/shredder")));

    }

    /**
     * Метод для блоков, у которых текстура имеет префикс "block_".
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
     * Генерирует состояние для блока с кастомной OBJ моделью.
     * ВАЖНО: Сам файл модели (.json) должен быть создан вручную в /resources!
     */
    private <T extends Block> void customObjBlock(RegistryObject<T> blockObject) {
        // Создаём только blockstate, который ссылается на JSON модель
        // JSON модель должна лежать в resources/assets/hbm_m/models/block/<название>.json
        horizontalBlock(blockObject.get(), 
            models().getExistingFile(modLoc("block/" + blockObject.getId().getPath())));
    }

    /**
     * Генерирует модель и состояние для горизонтально-ориентированного блока.
     * @param blockObject Блок
     * @param sideTexture Текстура для боковых и задней сторон
     * @param frontTexture Текстура для лицевой стороны (север)
     * @param topTexture Текстура для верха и низа
     */
    private void orientableBlockWithItem(RegistryObject<Block> blockObject, ResourceLocation sideTexture, ResourceLocation frontTexture, ResourceLocation topTexture) {
        // 1. Создаем модель блока с разными текстурами.
        //    Метод orientable использует стандартные имена: side, front, top, bottom.
        var model = models().orientable(
            blockObject.getId().getPath(),
            sideTexture,
            frontTexture,
            topTexture
        ).texture("particle", frontTexture); // Частицы при ломании будут из лицевой текстуры

        // 2. Создаем состояние блока (blockstate), которое будет вращать эту модель по горизонтали.
        horizontalBlock(blockObject.get(), model);
        
        // 3. Создаем модель для предмета-блока, которая выглядит так же, как и сам блок.
        simpleBlockItem(blockObject.get(), model);
    }

}