package com.hbm_m.datagen;
// Провайдер генерации моделей предметов для мода.
// Здесь мы определяем, как будут выглядеть наши предметы в инвентаре и в мире.
// Используется в классе DataGenerators для регистрации.

import java.util.LinkedHashMap;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModIngots;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItemModelProvider extends ItemModelProvider {

    private static LinkedHashMap<ResourceKey<TrimMaterial>, Float> trimMaterials = new LinkedHashMap<>();
    static {
        trimMaterials.put(TrimMaterials.QUARTZ, 0.1F);
        trimMaterials.put(TrimMaterials.IRON, 0.2F);
        trimMaterials.put(TrimMaterials.NETHERITE, 0.3F);
        trimMaterials.put(TrimMaterials.REDSTONE, 0.4F);
        trimMaterials.put(TrimMaterials.COPPER, 0.5F);
        trimMaterials.put(TrimMaterials.GOLD, 0.6F);
        trimMaterials.put(TrimMaterials.EMERALD, 0.7F);
        trimMaterials.put(TrimMaterials.DIAMOND, 0.8F);
        trimMaterials.put(TrimMaterials.LAPIS, 0.9F);
        trimMaterials.put(TrimMaterials.AMETHYST, 1.0F);
    }

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

        // Door items (flat icons like vanilla doors)
        withExistingParent(ModBlocks.METAL_DOOR.getId().getPath(), "item/generated")
            .texture("layer0", modLoc("item/" + ModBlocks.METAL_DOOR.getId().getPath()));
        withExistingParent(ModBlocks.DOOR_BUNKER.getId().getPath(), "item/generated")
            .texture("layer0", modLoc("item/" + ModBlocks.DOOR_BUNKER.getId().getPath()));
        withExistingParent(ModBlocks.DOOR_OFFICE.getId().getPath(), "item/generated")
            .texture("layer0", modLoc("item/" + ModBlocks.DOOR_OFFICE.getId().getPath()));

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


        simpleItem(ModItems.CAPACITOR_BOARD);
        simpleItem(ModItems.CAPACITOR_TANTALUM);
        simpleItem(ModItems.BISMOID_CIRCUIT);
        simpleItem(ModItems.BISMOID_CHIP);
        simpleItem(ModItems.SILICON_CIRCUIT);
        simpleItem(ModItems.CONTROLLER_ADVANCED);
        simpleItem(ModItems.CONTROLLER);
        simpleItem(ModItems.CONTROLLER_CHASSIS);
        simpleItem(ModItems.QUANTUM_COMPUTER);
        simpleItem(ModItems.QUANTUM_CIRCUIT);
        simpleItem(ModItems.QUANTUM_CHIP);
        simpleItem(ModItems.INTEGRATED_CIRCUIT);
        simpleItem(ModItems.ADVANCED_CIRCUIT);
        simpleItem(ModItems.ANALOG_CIRCUIT);
        simpleItem(ModItems.VACUUM_TUBE);
        simpleItem(ModItems.CAPACITOR);
        simpleItem(ModItems.PCB);
        simpleItem(ModItems.ATOMIC_CLOCK);
        simpleItem(ModItems.MICROCHIP);

        simpleItem(ModItems.BATTLE_GEARS);
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
        simpleItem(ModItems.INSULATOR);
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

        simpleItem(ModItems.STAMP_STONE_FLAT);
        simpleItem(ModItems.STAMP_STONE_PLATE);
        simpleItem(ModItems.STAMP_STONE_WIRE);
        simpleItem(ModItems.STAMP_STONE_CIRCUIT);
        simpleItem(ModItems.STAMP_IRON_FLAT);
        simpleItem(ModItems.STAMP_IRON_PLATE);
        simpleItem(ModItems.STAMP_IRON_WIRE);
        simpleItem(ModItems.STAMP_IRON_CIRCUIT);
        simpleItem(ModItems.STAMP_IRON_9);
        simpleItem(ModItems.STAMP_IRON_44);
        simpleItem(ModItems.STAMP_IRON_50);
        simpleItem(ModItems.STAMP_IRON_357);
        simpleItem(ModItems.STAMP_STEEL_FLAT);
        simpleItem(ModItems.STAMP_STEEL_PLATE);
        simpleItem(ModItems.STAMP_STEEL_WIRE);
        simpleItem(ModItems.STAMP_STEEL_CIRCUIT);
        simpleItem(ModItems.STAMP_TITANIUM_FLAT);
        simpleItem(ModItems.STAMP_TITANIUM_PLATE);
        simpleItem(ModItems.STAMP_TITANIUM_WIRE);
        simpleItem(ModItems.STAMP_TITANIUM_FLAT);
        simpleItem(ModItems.STAMP_TITANIUM_PLATE);
        simpleItem(ModItems.STAMP_TITANIUM_WIRE);
        simpleItem(ModItems.STAMP_TITANIUM_CIRCUIT);
        simpleItem(ModItems.STAMP_OBSIDIAN_FLAT);
        simpleItem(ModItems.STAMP_OBSIDIAN_PLATE);
        simpleItem(ModItems.STAMP_OBSIDIAN_WIRE);
        simpleItem(ModItems.STAMP_OBSIDIAN_CIRCUIT);
        simpleItem(ModItems.STAMP_DESH_FLAT);
        simpleItem(ModItems.STAMP_DESH_PLATE);
        simpleItem(ModItems.STAMP_DESH_WIRE);
        simpleItem(ModItems.STAMP_DESH_CIRCUIT);
        simpleItem(ModItems.STAMP_DESH_9);
        simpleItem(ModItems.STAMP_DESH_44);
        simpleItem(ModItems.STAMP_DESH_50);
        simpleItem(ModItems.STAMP_DESH_357);

        simpleItem(ModItems.NUGGET_SILICON);
        simpleItem(ModItems.BILLET_SILICON);
        simpleItem(ModItems.WIRE_RED_COPPER);
        simpleItem(ModItems.WIRE_COPPER);
        simpleItem(ModItems.WIRE_TUNGSTEN);
        simpleItem(ModItems.WIRE_ALUMINIUM);
        simpleItem(ModItems.WIRE_FINE);
        simpleItem(ModItems.WIRE_SCHRABIDIUM);
        simpleItem(ModItems.WIRE_ADVANCED_ALLOY);
        simpleItem(ModItems.WIRE_GOLD);
        simpleItem(ModItems.WIRE_MAGNETIZED_TUNGSTEN);
        simpleItem(ModItems.WIRE_CARBON);

        trimmedArmorItem(com.hbm_m.item.ModItems.ALLOY_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.ALLOY_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.ALLOY_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.ALLOY_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.TITANIUM_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.TITANIUM_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.TITANIUM_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.TITANIUM_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.SECURITY_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.SECURITY_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.SECURITY_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.SECURITY_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.ASBESTOS_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.ASBESTOS_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.ASBESTOS_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.ASBESTOS_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.AJR_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.AJR_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.AJR_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.AJR_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.STEEL_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.STEEL_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.STEEL_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.STEEL_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.PAA_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.PAA_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.PAA_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.PAA_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.LIQUIDATOR_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.LIQUIDATOR_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.LIQUIDATOR_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.LIQUIDATOR_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.HAZMAT_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.HAZMAT_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.HAZMAT_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.HAZMAT_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.STARMETAL_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.STARMETAL_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.STARMETAL_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.STARMETAL_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.COBALT_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.COBALT_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.COBALT_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.COBALT_BOOTS);

        evenSimplerBlockItem(ModBlocks.REINFORCED_STONE_STAIRS);
        evenSimplerBlockItem(ModBlocks.REINFORCED_STONE_SLAB);

        evenSimplerBlockItem(ModBlocks.CONCRETE_HAZARD_STAIRS);
        evenSimplerBlockItem(ModBlocks.CONCRETE_HAZARD_SLAB);
        simpleBlockItem(ModBlocks.DOOR_BUNKER);
        simpleBlockItem(ModBlocks.DOOR_OFFICE);
        simpleBlockItem(ModBlocks.METAL_DOOR);
        simpleItem(ModItems.GRENADEHE);
        simpleItem(ModItems.GRENADEFIRE);

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
    private ItemModelBuilder simpleBlockItem(RegistryObject<Block> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID,"item/" + item.getId().getPath()));
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

    private void trimmedArmorItem(RegistryObject<Item> itemRegistryObject) {
        final String MOD_ID = MainRegistry.MOD_ID; // Change this to your mod id

        if(itemRegistryObject.get() instanceof ArmorItem armorItem) {
            trimMaterials.entrySet().forEach(entry -> {

                ResourceKey<TrimMaterial> trimMaterial = entry.getKey();
                float trimValue = entry.getValue();

                String armorType = switch (armorItem.getEquipmentSlot()) {
                    case HEAD -> "helmet";
                    case CHEST -> "chestplate";
                    case LEGS -> "leggings";
                    case FEET -> "boots";
                    default -> "";
                };

                String armorItemPath = "item/" + armorItem;
                String trimPath = "trims/items/" + armorType + "_trim_" + trimMaterial.location().getPath();
                String currentTrimName = armorItemPath + "_" + trimMaterial.location().getPath() + "_trim";
                ResourceLocation armorItemResLoc = ResourceLocation.fromNamespaceAndPath(MOD_ID, armorItemPath);
                ResourceLocation trimResLoc = ResourceLocation.parse(trimPath); // minecraft namespace
                ResourceLocation trimNameResLoc = ResourceLocation.fromNamespaceAndPath(MOD_ID, currentTrimName);

                existingFileHelper.trackGenerated(trimResLoc, PackType.CLIENT_RESOURCES, ".png", "textures");

                getBuilder(currentTrimName)
                        .parent(new ModelFile.UncheckedModelFile("item/generated"))
                        .texture("layer0", armorItemResLoc)
                        .texture("layer1", trimResLoc);

                this.withExistingParent(itemRegistryObject.getId().getPath(),
                                mcLoc("item/generated"))
                        .override()
                        .model(new ModelFile.UncheckedModelFile(trimNameResLoc))
                        .predicate(mcLoc("trim_type"), trimValue).end()
                        .texture("layer0",
                                ResourceLocation.fromNamespaceAndPath(MOD_ID,
                                        "item/" + itemRegistryObject.getId().getPath()));
            });
        }
    }

    public void evenSimplerBlockItem(RegistryObject<Block> block) {
        this.withExistingParent(MainRegistry.MOD_ID + ":" + ForgeRegistries.BLOCKS.getKey(block.get()).getPath(),
                modLoc("block/" + ForgeRegistries.BLOCKS.getKey(block.get()).getPath()));
    }

}
