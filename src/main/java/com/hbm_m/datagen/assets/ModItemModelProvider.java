package com.hbm_m.datagen.assets;

import java.util.LinkedHashMap;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;
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

    @Override
    protected void registerModels() {
        // ЦИКЛ ДЛЯ СЛИТКОВ
        for (ModIngots ingot : ModIngots.values()) {
            RegistryObject<Item> ingotObject = ModItems.getIngot(ingot);
            if (ingotObject != null && ingotObject.isPresent()) {
                ingotItem(ingotObject);
            }
        }

        // ЦИКЛ ДЛЯ ModPowders
        for (ModPowders powder : ModPowders.values()) {
            RegistryObject<Item> powderObject = ModItems.getPowders(powder);
            if (powderObject != null && powderObject.isPresent()) {
                powdersItem(powderObject);
            }
        }

        // ЦИКЛ ДЛЯ ПОРОШКОВ ИЗ СЛИТКОВ
        for (ModIngots ingot : ModIngots.values()) {
            RegistryObject<Item> powder = ModItems.getPowder(ingot);
            if (powder != null && powder.isPresent() && powderTextureExists(ingot.getName())) {
                powdersItem(powder);
            }
            ModItems.getTinyPowder(ingot).ifPresent(tiny -> {
                if (tiny != null && tiny.isPresent() && powderTinyTextureExists(ingot.getName())) {
                    tinyPowderItem(tiny);
                }
            });
        }

        // БАЗОВЫЕ ПОРОШКИ (всегда существуют)
        if (ModItems.DUST != null && ModItems.DUST.isPresent()) powderTexture(ModItems.DUST, "powders/dust");
        if (ModItems.DUST_TINY != null && ModItems.DUST_TINY.isPresent()) powderTexture(ModItems.DUST_TINY, "powders/tiny/dust_tiny");

        withExistingParent("large_vehicle_door", 
            modLoc("block/doors/large_vehicle_door_modern"));

        withExistingParent("round_airlock_door", 
            modLoc("block/doors/round_airlock_door_modern"));

        withExistingParent("transition_seal", 
            modLoc("block/doors/transition_seal"));

        withExistingParent("silo_hatch", 
            modLoc("block/doors/silo_hatch"));

        withExistingParent("silo_hatch_large", 
            modLoc("block/doors/silo_hatch_large"));

        withExistingParent("qe_containment_door", 
            modLoc("block/doors/qe_containment_door_modern"));

        withExistingParent("water_door", 
            modLoc("block/doors/water_door_modern"));

        withExistingParent("fire_door", 
            modLoc("block/doors/fire_door_modern"));

        withExistingParent("sliding_blast_door", 
            modLoc("block/doors/sliding_blast_door_modern"));

        withExistingParent("sliding_seal_door", 
            modLoc("block/doors/sliding_seal_door_modern"));

        withExistingParent("secure_access_door", 
            modLoc("block/doors/secure_access_door_modern"));

        withExistingParent("qe_sliding_door", 
            modLoc("block/doors/qe_sliding_door_modern"));

        withExistingParent("vault_door", 
            modLoc("block/doors/vault_door_modern"));

        // Door items (flat icons like vanilla doors)
        withExistingParent(ModBlocks.METAL_DOOR.getId().getPath(), "item/generated")
            .texture("layer0", modLoc("item/" + ModBlocks.METAL_DOOR.getId().getPath()));
        withExistingParent(ModBlocks.DOOR_BUNKER.getId().getPath(), "item/generated")
            .texture("layer0", modLoc("item/" + ModBlocks.DOOR_BUNKER.getId().getPath()));
        withExistingParent(ModBlocks.DOOR_OFFICE.getId().getPath(), "item/generated")
            .texture("layer0", modLoc("item/" + ModBlocks.DOOR_OFFICE.getId().getPath()));

        // РЕГИСТРАЦИЯ МОДЕЛЕЙ ДЛЯ УНИКАЛЬНЫХ ПРЕДМЕТОВ 
        // Для предметов, зарегистрированных вручную, мы также можем генерировать модели.
        simpleItem(ModItems.BILLET_PLUTONIUM);
        simpleItem(ModItems.BALL_TNT);
        simpleItem(ModItems.DEFUSER);
        simpleItem(ModItems.SCREWDRIVER);
        simpleItem(ModItems.CROWBAR);
        simpleItem(ModItems.SCRAP);
        simpleItem(ModItems.CRT_DISPLAY);
        simpleItem(ModItems.SEQUESTRUM);
        simpleItem(ModItems.MAN_CORE);

        simpleItem(ModItems.STRAWBERRY);

        simpleItem(ModItems.LIMESTONE);
        simpleItem(ModItems.MALACHITE_CHUNK);
        simpleItem(ModItems.CANNED_ASBESTOS);
        simpleItem(ModItems.CANNED_ASS);
        simpleItem(ModItems.CANNED_BARK);
        simpleItem(ModItems.CANNED_BEEF);
        simpleItem(ModItems.CANNED_BHOLE);
        simpleItem(ModItems.CANNED_CHEESE);
        simpleItem(ModItems.CANNED_CHINESE);
        simpleItem(ModItems.CANNED_DIESEL);
        simpleItem(ModItems.CANNED_FIST);
        simpleItem(ModItems.CANNED_FRIED);
        simpleItem(ModItems.CANNED_HOTDOGS);
        simpleItem(ModItems.CANNED_JIZZ);
        simpleItem(ModItems.CANNED_KEROSENE);
        simpleItem(ModItems.CANNED_LEFTOVERS);
        simpleItem(ModItems.CANNED_MILK);
        simpleItem(ModItems.CANNED_MYSTERY);
        simpleItem(ModItems.CANNED_NAPALM);
        simpleItem(ModItems.CANNED_OIL);
        simpleItem(ModItems.CANNED_PASHTET);
        simpleItem(ModItems.CANNED_PIZZA);
        simpleItem(ModItems.CANNED_RECURSION);
        simpleItem(ModItems.CANNED_SPAM);
        simpleItem(ModItems.CANNED_STEW);
        simpleItem(ModItems.CANNED_TOMATO);
        simpleItem(ModItems.CANNED_TUNA);
        simpleItem(ModItems.CANNED_TUBE);
        simpleItem(ModItems.CANNED_YOGURT);
        simpleItem(ModItems.BOLT_STEEL);
        simpleItem(ModItems.CAN_BEPIS);
        simpleItem(ModItems.CAN_BREEN);
        simpleItem(ModItems.CAN_CREATURE);
        simpleItem(ModItems.CAN_EMPTY);
        simpleItem(ModItems.CAN_KEY);
        simpleItem(ModItems.CAN_LUNA);
        simpleItem(ModItems.CAN_MRSUGAR);
        simpleItem(ModItems.CAN_MUG);
        simpleItem(ModItems.CAN_OVERCHARGE);
        simpleItem(ModItems.CAN_REDBOMB);
        simpleItem(ModItems.CAN_SMART);

        simpleItem(ModItems.BOLT_STEEL);
        simpleItem(ModItems.COIL_MAGNETIZED_TUNGSTEN_TORUS);
        simpleItem(ModItems.COIL_MAGNETIZED_TUNGSTEN);
        simpleItem(ModItems.COIL_COPPER_TORUS);
        simpleItem(ModItems.COIL_COPPER);
        simpleItem(ModItems.COIL_GOLD_TORUS);
        simpleItem(ModItems.COIL_GOLD);
        simpleItem(ModItems.COIL_ADVANCED_ALLOY_TORUS);
        simpleItem(ModItems.COIL_ADVANCED_ALLOY);
        simpleItem(ModItems.MOTOR_BISMUTH);
        simpleItem(ModItems.MOTOR_DESH);
        simpleItem(ModItems.MOTOR);
        
        simpleItem(ModItems.ZIRCONIUM_SHARP);
        simpleItem(ModItems.BORAX);
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

        

        simpleItem(ModItems.POWDER_COAL);
        simpleItem(ModItems.POWDER_COAL_SMALL);
        simpleItem(ModItems.COIL_TUNGSTEN);
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
        simpleItem(ModItems.ALUMINUM_RAW);
        simpleItem(ModItems.BERYLLIUM_RAW);


        evenSimplerBlockItem(ModBlocks.REINFORCED_STONE_STAIRS);
        evenSimplerBlockItem(ModBlocks.REINFORCED_STONE_SLAB);

        evenSimplerBlockItem(ModBlocks.CONCRETE_HAZARD_STAIRS);
        evenSimplerBlockItem(ModBlocks.CONCRETE_HAZARD_SLAB);
        simpleBlockItem(ModBlocks.DOOR_BUNKER);
        simpleBlockItem(ModBlocks.DOOR_OFFICE);
        simpleBlockItem(ModBlocks.METAL_DOOR);
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

    private void powdersItem(RegistryObject<Item> itemObject) {
        String registrationName = itemObject.getId().getPath();
        String baseName = registrationName.replace("_powder", "");
        String textureFileName = "powder_" + baseName;
        withExistingParent(registrationName, "item/generated")
                .texture("layer0", modLoc("item/powders/" + textureFileName));
    }

    private void tinyPowderItem(RegistryObject<Item> itemObject) {
        String registrationName = itemObject.getId().getPath();
        String baseName = registrationName.replace("_powder_tiny", "");
        String textureFileName = "powder_" + baseName + "_tiny";
        withExistingParent(registrationName, "item/generated")
                .texture("layer0", modLoc("item/powders/tiny/" + textureFileName));
    }

    private void powderTexture(RegistryObject<Item> itemObject, String texturePath) {
        withExistingParent(itemObject.getId().getPath(), "item/generated")
                .texture("layer0", modLoc("item/" + texturePath));
    }

    private boolean powderTextureExists(String baseName) {
        ResourceLocation texture = modLoc("textures/item/powders/powder_" + baseName + ".png");
        return existingFileHelper.exists(texture, PackType.CLIENT_RESOURCES);
    }

    private boolean powderTinyTextureExists(String baseName) {
        ResourceLocation texture = modLoc("textures/item/powders/tiny/powder_" + baseName + "_tiny.png");
        return existingFileHelper.exists(texture, PackType.CLIENT_RESOURCES);
    }

    private void trimmedArmorItem(RegistryObject<Item> itemRegistryObject) {
        final String MOD_ID = MainRegistry.MOD_ID;

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
