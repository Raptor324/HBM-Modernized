package com.hbm_m.datagen.assets;
//? if forge {
import java.util.LinkedHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.BlockAbsorber;
import com.hbm_m.client.render.missile.MissileFormFactorModels;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.CustomLoaderBuilder;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import com.hbm_m.client.render.missile.MissileItemModelDefinitions;

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
        generateMissileItemModels();

        // ЦИКЛ ДЛЯ СЛИТКОВ
        for (ModIngots ingot : ModIngots.values()) {
            RegistrySupplier<Item> ingotObject = ModItems.getIngot(ingot);
            if (ingotObject != null && ingotObject.isPresent()) {
                ingotItem(ingotObject);
            }
        }

        // Wire Dense — 11 specific materials
        for (String name : new String[]{"iron","aluminium","titanium","lead","copper","steel","gold","advanced_alloy","schrabidium","saturnite","combine_steel"}) {
            withExistingParent("wire_dense_" + name, "item/generated")
                    .texture("layer0", modLoc("item/wire_dense/wire_dense_" + name));
        }

        // ЦИКЛ ДЛЯ ModPowders
        for (ModPowders powder : ModPowders.values()) {
            RegistrySupplier<Item> powderObject = ModItems.getPowders(powder);
            if (powderObject != null && powderObject.isPresent()) {
                powdersItem(powderObject);
            }
        }

        // ЦИКЛ ДЛЯ ПОРОШКОВ ИЗ СЛИТКОВ
        for (ModIngots ingot : ModIngots.values()) {
            RegistrySupplier<Item> powder = ModItems.getPowder(ingot);
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
        generatedItemIfTextureExists(ModItems.FALLOUT, "fallout");
        powderTexture(ModItems.POWDER_DESH_MIX, "powders/powder_desh_mix");
        powderTexture(ModItems.POWDER_NITAN_MIX, "powders/powder_nitan_mix");

        // Neu portierte, freistehende Pulver (Texturen bereits im Asset-Ordner vorhanden)
        powdersItem(ModItems.POWDER_SAWDUST);
        powdersItem(ModItems.POWDER_YELLOWCAKE);
        powdersItem(ModItems.POWDER_BALEFIRE);
        powdersItem(ModItems.POWDER_PALEOGENITE);
        powdersItem(ModItems.POWDER_THERMITE);
        powdersItem(ModItems.POWDER_FERTILIZER);
        powdersItem(ModItems.POWDER_FLUX);
        powdersItem(ModItems.POWDER_MAGIC);
        powdersItem(ModItems.POWDER_ICE);
        powdersItem(ModItems.POWDER_SPARK_MIX);
        powdersItem(ModItems.POWDER_SEMTEX_MIX);
        powdersItem(ModItems.POWDER_DESH_READY);
        powdersItem(ModItems.POWDER_COLTAN);

        // Sentry-Turret Munition (MVP-Platzhalter, nutzt vorhandene Ammo-DGK-Textur)
        powderTexture(ModItems.TURRET_AMMO, "ammo_dgk");

        // Echte 9mm/.50/5.56mm-Munition fuer Sentry/Chekhov/Friendly (Platzhalter-Textur, bis eigene Assets vorhanden sind)
        powderTexture(ModItems.AMMO_9MM_SP, "ammo_dgk");
        powderTexture(ModItems.AMMO_9MM_FMJ, "ammo_dgk");
        powderTexture(ModItems.AMMO_9MM_JHP, "ammo_dgk");
        powderTexture(ModItems.AMMO_9MM_AP, "ammo_dgk");
        powderTexture(ModItems.AMMO_50_SP, "ammo_dgk");
        powderTexture(ModItems.AMMO_50_FMJ, "ammo_dgk");
        powderTexture(ModItems.AMMO_50_JHP, "ammo_dgk");
        powderTexture(ModItems.AMMO_50_AP, "ammo_dgk");
        powderTexture(ModItems.AMMO_50_DU, "ammo_dgk");
        powderTexture(ModItems.AMMO_556_SP, "ammo_dgk");
        powderTexture(ModItems.AMMO_556_FMJ, "ammo_dgk");
        powderTexture(ModItems.AMMO_556_JHP, "ammo_dgk");
        powderTexture(ModItems.AMMO_556_AP, "ammo_dgk");
        powderTexture(ModItems.ROCKET_TURRET_STANDARD, "ammo_dgk");
        powderTexture(ModItems.ROCKET_HIMARS_STANDARD, "ammo_dgk");
        powderTexture(ModItems.ROCKET_HIMARS_HE, "ammo_dgk");
        powderTexture(ModItems.ROCKET_HIMARS_LAVA, "ammo_dgk");
        powderTexture(ModItems.ROCKET_HIMARS_MINI_NUKE, "ammo_dgk");
        powderTexture(ModItems.ROCKET_HIMARS_WP, "ammo_dgk");
        powderTexture(ModItems.ROCKET_HIMARS_THERMOBARIC, "ammo_dgk");
        powderTexture(ModItems.AMMO_TAU_URANIUM, "ammo_dgk");
        powderTexture(ModItems.AMMO_FLAME_DIESEL, "ammo_dgk");

        // Missile-Assembly-Teile (Platzhalter-Texturen, bis eigene Assets vorhanden sind)
        powderTexture(ModItems.MISSILE_FUSELAGE, "pipes_steel");
        powderTexture(ModItems.MISSILE_CHIP, "silicon_circuit");

        // Fix: diese Items hatten trotz vorhandener Texturen nie ein Item-Model (FileNotFoundException
        // beim Client-Start) - dadurch unsichtbar/Missing-Model-Icon im Creative-Tab.
        simpleItem(ModItems.WARHEAD_GENERIC_SMALL);
        simpleItem(ModItems.WARHEAD_GENERIC_MEDIUM);
        simpleItem(ModItems.WARHEAD_GENERIC_LARGE);
        simpleItem(ModItems.WARHEAD_BUSTER_SMALL);
        simpleItem(ModItems.WARHEAD_BUSTER_MEDIUM);
        simpleItem(ModItems.WARHEAD_BUSTER_LARGE);
        simpleItem(ModItems.WARHEAD_CLUSTER_SMALL);
        simpleItem(ModItems.WARHEAD_CLUSTER_MEDIUM);
        simpleItem(ModItems.WARHEAD_CLUSTER_LARGE);
        simpleItem(ModItems.WARHEAD_INCENDIARY_SMALL);
        simpleItem(ModItems.WARHEAD_INCENDIARY_MEDIUM);
        simpleItem(ModItems.WARHEAD_INCENDIARY_LARGE);
        simpleItem(ModItems.WARHEAD_MIRV);
        simpleItem(ModItems.WARHEAD_VOLCANO);

        // Siren cassettes all share the single ported "cassette" texture (no per-track overlay tint,
        // simplified from the original's dye-tinted overlay layer)
        simpleItemModelByName(ModItems.CASSETTE_AMS_SIREN.getId().getPath(), "cassette");
        simpleItemModelByName(ModItems.CASSETTE_BEEP_SIREN.getId().getPath(), "cassette");
        simpleItemModelByName(ModItems.CASSETTE_CLASSIC_SIREN.getId().getPath(), "cassette");
        simpleItemModelByName(ModItems.CASSETTE_NOSTROMO_SIREN.getId().getPath(), "cassette");
        simpleItemModelByName(ModItems.CASSETTE_REGULAR_SIREN.getId().getPath(), "cassette");
        simpleItemModelByName(ModItems.CASSETTE_STRIDER_SIREN.getId().getPath(), "cassette");
        simpleItemModelByName(ModItems.CASSETTE_SWEEP_SIREN.getId().getPath(), "cassette");
        simpleItem(ModItems.WARHEAD_NUCLEAR);
        simpleItem(ModItems.THRUSTER_SMALL);
        simpleItem(ModItems.THRUSTER_MEDIUM);
        simpleItem(ModItems.THRUSTER_LARGE);
        simpleItem(ModItems.THRUSTER_NUCLEAR);
        simpleItem(ModItems.FUEL_TANK_SMALL);
        simpleItem(ModItems.FUEL_TANK_MEDIUM);
        simpleItem(ModItems.FUEL_TANK_LARGE);
        simpleItem(ModItems.MISSILE_ASSEMBLY);
        simpleItem(ModItems.MISSILE_SOYUZ_LANDER);
        simpleItem(ModItems.NEUTRON_REFLECTOR);
        simpleItem(ModItems.LOW_DENSITY_ELEMENT);
        simpleItem(ModItems.SAT_BASE);
        simpleItem(ModItems.SAT_LASER);
        simpleItem(ModItems.SAT_HEAD_LASER);
        simpleItem(ModItems.SAT_RADAR);
        simpleItem(ModItems.SAT_HEAD_RADAR);
        simpleItem(ModItems.SAT_MAPPER);
        simpleItem(ModItems.SAT_HEAD_MAPPER);
        simpleItem(ModItems.SAT_RESONATOR);
        simpleItem(ModItems.SAT_HEAD_RESONATOR);
        withExistingParent(ModItems.INGOT_TUNGSTEN_CARBIDE.getId().getPath(), "item/generated")
                .texture("layer0", modLoc("item/ingot/ingot_tungsten_carbide"));

        registerRadAbsorberItemModels();

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
            modLoc("block/doors/vault_door_skin_101"));

        withExistingParent("cargo_door",
            modLoc("block/doors/cargo_door"));

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
        simpleItem(ModItems.AIRSTRIKE_AGENT);
        simpleItem(ModItems.CROWBAR);
        simpleItem(ModItems.OIL_DETECTOR);
        simpleItem(ModItems.MULTI_DETONATOR);
        simpleItem(ModItems.AIRSTRIKE_TEST);
        simpleItem(ModItems.AIRSTRIKE_HEAVY);
        simpleItem(ModItems.DETONATOR);
        simpleItem(ModItems.FAT_MAN_EXPLOSIVE);
        simpleItem(ModItems.FAT_MAN_IGNITER);
        simpleItem(ModItems.FAT_MAN_CORE);
        simpleItem(ModItems.DESIGNATOR);
        simpleItem(ModItems.RANGEFINDER);
        simpleItem(ModItems.DESIGNATOR_RANGE);
        simpleItem(ModItems.DESIGNATOR_MANUAL);
        simpleItem(ModItems.SCRAP);
        simpleItem(ModItems.BLACK_HOLE);
        simpleItem(ModItems.PELLET_ANTIMATTER);
        simpleItem(ModItems.FLAME_PONY);
        simpleItem(ModItems.CRT_DISPLAY);
        simpleItem(ModItems.MAGNETRON);
        simpleItem(ModItems.TURBINE_TITANIUM);
        simpleItem(ModItems.SEQUESTRUM);
        simpleItem(ModItems.BLADE_STEEL);
        simpleItem(ModItems.BLADE_TITANIUM);
        simpleItem(ModItems.BLADE_ALLOY);
        simpleItem(ModItems.BLADE_TEST);
        simpleItem(ModItems.GEIGER_COUNTER);
        simpleItem(ModItems.DOSIMETER);
        simpleItem(ModItems.DIGAMMA_DIAGNOSTIC);
        
        // Music disc
        withExistingParent(ModItems.MUSIC_DISC_BUNKER.getId().getPath(), "item/generated")
                .texture("layer0", mcLoc("item/music_disc_13"));
        withExistingParent(ModItems.MUSIC_DISC_GLASS.getId().getPath(), "item/generated")
                .texture("layer0", modLoc("item/record_glass"));

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

        // Machine upgrades
        simpleItem(ModItems.UPGRADE_SPEED_1);
        simpleItem(ModItems.UPGRADE_SPEED_2);
        simpleItem(ModItems.UPGRADE_SPEED_3);
        simpleItem(ModItems.UPGRADE_STACK_1);
        simpleItem(ModItems.UPGRADE_STACK_2);
        simpleItem(ModItems.UPGRADE_STACK_3);
        simpleItem(ModItems.UPGRADE_EJECTOR_1);
        simpleItem(ModItems.UPGRADE_EJECTOR_2);
        simpleItem(ModItems.UPGRADE_EJECTOR_3);
        simpleItem(ModItems.UPGRADE_EFFECT_1);
        simpleItem(ModItems.UPGRADE_EFFECT_2);
        simpleItem(ModItems.UPGRADE_EFFECT_3);
        simpleItem(ModItems.UPGRADE_POWER_1);
        simpleItem(ModItems.UPGRADE_POWER_2);
        simpleItem(ModItems.UPGRADE_POWER_3);
        simpleItem(ModItems.UPGRADE_FORTUNE_1);
        simpleItem(ModItems.UPGRADE_FORTUNE_2);
        simpleItem(ModItems.UPGRADE_FORTUNE_3);
        simpleItem(ModItems.UPGRADE_AFTERBURN_1);
        simpleItem(ModItems.UPGRADE_AFTERBURN_2);
        simpleItem(ModItems.UPGRADE_AFTERBURN_3);
        simpleItem(ModItems.UPGRADE_OVERDRIVE_1);
        simpleItem(ModItems.UPGRADE_OVERDRIVE_2);
        simpleItem(ModItems.UPGRADE_OVERDRIVE_3);

        simpleItem(ModItems.STRAWBERRY);
        simpleItem(ModItems.INFINITE_WATER_500);
        simpleItem(ModItems.INFINITE_WATER_5000);

        simpleItem(ModItems.LIMESTONE);
        simpleItem(ModItems.SHELL_STEEL);
        simpleItem(ModItems.SHELL_COPPER);
        simpleItem(ModItems.SHELL_ALUMINUM);
        simpleItem(ModItems.SHELL_TITANIUM);
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

        simpleItem(ModItems.GAS_EMPTY);
        simpleItem(ModItems.DUCTTAPE);
        simpleItem(ModItems.HAZMAT_CLOTH);
        simpleItem(ModItems.HAZMAT_CLOTH_GREY);
        simpleItem(ModItems.HAZMAT_CLOTH_RED);
        simpleItem(ModItems.ASBESTOS_CLOTH);
        simpleItem(ModItems.GRENADE_NUC);
        simpleItem(ModItems.GRENADE_IF_HE);
        simpleItem(ModItems.GRENADE_IF_FIRE);
        simpleItem(ModItems.GRENADE_IF_SLIME);
        simpleItem(ModItems.GRENADE_IF);
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
        simpleItem(ModItems.BATTERY_SCHRABIDIUM);
        simpleItem(ModItems.BATTERY_POTATO);
        simpleItem(ModItems.BATTERY);
        simpleItem(ModItems.AIRSTRIKE_NUKE);
        simpleItem(ModItems.BATTERY_RED_CELL);
        simpleItem(ModItems.BATTERY_RED_CELL_6);
        simpleItem(ModItems.BATTERY_RED_CELL_24);
        simpleItem(ModItems.BATTERY_ADVANCED);
        simpleItem(ModItems.BATTERY_ADVANCED_CELL);
        simpleItem(ModItems.BATTERY_ADVANCED_CELL_4);
        simpleItem(ModItems.BATTERY_ADVANCED_CELL_12);
        simpleItem(ModItems.BATTERY_LITHIUM);
        simpleItem(ModItems.BATTERY_LITHIUM_CELL);
        simpleItem(ModItems.BATTERY_LITHIUM_CELL_3);
        simpleItem(ModItems.BATTERY_LITHIUM_CELL_6);
        simpleItem(ModItems.BATTERY_SCHRABIDIUM_CELL);
        simpleItem(ModItems.BATTERY_SCHRABIDIUM_CELL_2);
        simpleItem(ModItems.BATTERY_SCHRABIDIUM_CELL_4);
        simpleItem(ModItems.BATTERY_SPARK);
        simpleItem(ModItems.BATTERY_TRIXITE);
        simpleItem(ModItems.BATTERY_SPARK_CELL_6);
        simpleItem(ModItems.BATTERY_SPARK_CELL_25);
        simpleItem(ModItems.BATTERY_SPARK_CELL_100);
        simpleItem(ModItems.BATTERY_SPARK_CELL_1000);
        simpleItem(ModItems.BATTERY_SPARK_CELL_2500);
        simpleItem(ModItems.BATTERY_SPARK_CELL_10000);
        simpleItem(ModItems.BATTERY_SPARK_CELL_POWER);

        simpleItem(ModItems.DEPTH_ORES_SCANNER);
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
        simpleItem(ModItems.CENTRIFUGE_ELEMENT);
        simpleItem(ModItems.PCB);
        simpleItem(ModItems.ATOMIC_CLOCK);
        simpleItem(ModItems.MICROCHIP);

        simpleItem(ModItems.BATTLE_GEARS);
        simpleItem(ModItems.BATTLE_SENSOR);
        simpleItem(ModItems.BATTLE_CASING);
        simpleItem(ModItems.BATTLE_COUNTER);
        simpleItem(ModItems.BATTLE_MODULE);
        simpleItem(ModItems.METAL_ROD);
        simpleItem(ModItems.ROD_ZIRNOX_EMPTY);
        simpleItem(ModItems.ROD_ZIRNOX_LES_FUEL);
        simpleItem(ModItems.ROD_ZIRNOX_LES_FUEL_DEPLETED);
        simpleItem(ModItems.ROD_ZIRNOX_LITHIUM);
        simpleItem(ModItems.ROD_ZIRNOX_MOX_FUEL);
        simpleItem(ModItems.ROD_ZIRNOX_MOX_FUEL_DEPLETED);
        simpleItem(ModItems.ROD_ZIRNOX_NATURAL_URANIUM_FUEL);
        simpleItem(ModItems.ROD_ZIRNOX_PLUTONIUM_FUEL);
        simpleItem(ModItems.ROD_ZIRNOX_PLUTONIUM_FUEL_DEPLETED);
        simpleItem(ModItems.ROD_ZIRNOX_TH232);
        simpleItem(ModItems.ROD_ZIRNOX_THORIUM_FUEL);
        simpleItem(ModItems.ROD_ZIRNOX_THORIUM_FUEL_DEPLETED);
        simpleItem(ModItems.ROD_ZIRNOX_TRITIUM);
        simpleItem(ModItems.ROD_ZIRNOX_U233_FUEL);
        simpleItem(ModItems.ROD_ZIRNOX_U233_FUEL_DEPLETED);
        simpleItem(ModItems.ROD_ZIRNOX_U235_FUEL);
        simpleItem(ModItems.ROD_ZIRNOX_U235_FUEL_DEPLETED);
        simpleItem(ModItems.ROD_ZIRNOX_URANIUM_FUEL);
        simpleItem(ModItems.ROD_ZIRNOX_URANIUM_FUEL_DEPLETED);
        simpleItem(ModItems.ROD_ZIRNOX_ZFB_MOX);
        simpleItem(ModItems.ROD_ZIRNOX_ZFB_MOX_DEPLETED);

        simpleItem(ModItems.PWR_FUEL_MEU);
        simpleItem(ModItems.PWR_FUEL_MEU_HOT);
        simpleItem(ModItems.PWR_FUEL_HEU233);
        simpleItem(ModItems.PWR_FUEL_HEU233_HOT);
        simpleItem(ModItems.PWR_FUEL_HEU235);
        simpleItem(ModItems.PWR_FUEL_HEU235_HOT);
        simpleItem(ModItems.PWR_FUEL_MEN);
        simpleItem(ModItems.PWR_FUEL_MEN_HOT);
        simpleItem(ModItems.PWR_FUEL_HEN237);
        simpleItem(ModItems.PWR_FUEL_HEN237_HOT);
        simpleItem(ModItems.PWR_FUEL_MOX);
        simpleItem(ModItems.PWR_FUEL_MOX_HOT);
        simpleItem(ModItems.PWR_FUEL_MEP);
        simpleItem(ModItems.PWR_FUEL_MEP_HOT);
        simpleItem(ModItems.PWR_FUEL_HEP239);
        simpleItem(ModItems.PWR_FUEL_HEP239_HOT);
        simpleItem(ModItems.PWR_FUEL_HEP241);
        simpleItem(ModItems.PWR_FUEL_HEP241_HOT);
        simpleItem(ModItems.PWR_FUEL_MEA);
        simpleItem(ModItems.PWR_FUEL_MEA_HOT);
        simpleItem(ModItems.PWR_FUEL_HEA242);
        simpleItem(ModItems.PWR_FUEL_HEA242_HOT);
        simpleItem(ModItems.PWR_FUEL_HES326);
        simpleItem(ModItems.PWR_FUEL_HES326_HOT);
        simpleItem(ModItems.PWR_FUEL_HES327);
        simpleItem(ModItems.PWR_FUEL_HES327_HOT);
        simpleItem(ModItems.PWR_FUEL_BFB_AM_MIX);
        simpleItem(ModItems.PWR_FUEL_BFB_AM_MIX_HOT);
        simpleItem(ModItems.PWR_FUEL_BFB_PU241);
        simpleItem(ModItems.PWR_FUEL_BFB_PU241_HOT);
        simpleItem(ModItems.WATZ_PELLET_SCHRABIDIUM_OXIDE);
        simpleItem(ModItems.WATZ_PELLET_SCHRABIDIUM_OXIDE_DEPLETED);
        simpleItem(ModItems.WATZ_PELLET_LES_OXIDE);
        simpleItem(ModItems.WATZ_PELLET_LES_OXIDE_DEPLETED);
        simpleItem(ModItems.WATZ_PELLET_NATURAL_URANIUM);
        simpleItem(ModItems.WATZ_PELLET_NATURAL_URANIUM_DEPLETED);
        simpleItem(ModItems.WATZ_PELLET_BORON_CARBIDE);
        simpleItem(ModItems.WATZ_PELLET_BORON_CARBIDE_DEPLETED);
        simpleItem(ModItems.WATZ_PELLET_LEAD_SHIELD);
        simpleItem(ModItems.WATZ_PELLET_LEAD_SHIELD_DEPLETED);

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
        withExistingParent(ModItems.WASTE_PLATE_U233.getId().getPath(), "item/generated").texture("layer0", modLoc("item/waste_plate_uranium"));
        withExistingParent(ModItems.WASTE_PLATE_U235.getId().getPath(), "item/generated").texture("layer0", modLoc("item/waste_plate_uranium"));
        withExistingParent(ModItems.WASTE_PLATE_PU239.getId().getPath(), "item/generated").texture("layer0", modLoc("item/waste_plate_mox"));
        withExistingParent(ModItems.RBMK_FUEL_DRX.getId().getPath(), "item/generated")
            .texture("layer0", modLoc("block/rbmkrods/" + ModItems.RBMK_FUEL_DRX.getId().getPath()));

        // RBMK fuel rods
        withExistingParent(ModItems.RBMK_FUEL_EMPTY.getId().getPath(), "item/generated").texture("layer0", modLoc("block/rbmk/rbmk_element_inner"));
        withExistingParent(ModItems.RBMK_FUEL_LEU235.getId().getPath(), "item/generated").texture("layer0", modLoc("block/rbmk/rbmk_fuel"));
        withExistingParent(ModItems.RBMK_FUEL_HEU235.getId().getPath(), "item/generated").texture("layer0", modLoc("block/rbmk/rbmk_fuel"));
        withExistingParent(ModItems.RBMK_FUEL_LEP.getId().getPath(),    "item/generated").texture("layer0", modLoc("block/rbmk/rbmk_fuel"));
        withExistingParent(ModItems.RBMK_FUEL_HEP.getId().getPath(),    "item/generated").texture("layer0", modLoc("block/rbmk/rbmk_fuel"));
        withExistingParent(ModItems.RBMK_FUEL_MOX.getId().getPath(),    "item/generated").texture("layer0", modLoc("block/rbmk/rbmk_fuel"));
        // RBMK pellets
        withExistingParent(ModItems.RBMK_PELLET_LEU235.getId().getPath(), "item/generated").texture("layer0", modLoc("block/rbmk/rbmk_element_inner"));
        withExistingParent(ModItems.RBMK_PELLET_HEU235.getId().getPath(), "item/generated").texture("layer0", modLoc("block/rbmk/rbmk_element_inner"));
        withExistingParent(ModItems.RBMK_PELLET_LEP.getId().getPath(),    "item/generated").texture("layer0", modLoc("block/rbmk/rbmk_element_inner"));
        withExistingParent(ModItems.RBMK_PELLET_HEP.getId().getPath(),    "item/generated").texture("layer0", modLoc("block/rbmk/rbmk_element_inner"));
        withExistingParent(ModItems.RBMK_PELLET_MOX.getId().getPath(),    "item/generated").texture("layer0", modLoc("block/rbmk/rbmk_element_inner"));
        // RBMK lids
        withExistingParent(ModItems.RBMK_LID.getId().getPath(),       "item/generated").texture("layer0", modLoc("block/rbmk/rbmk_blank_cover_top"));
        withExistingParent(ModItems.RBMK_LID_GLASS.getId().getPath(), "item/generated").texture("layer0", modLoc("block/rbmk/rbmk_blank_glass_top"));

        // RBMK block items — parent points to block/rbmk/ not block/machines/
        java.util.List<dev.architectury.registry.registries.RegistrySupplier<net.minecraft.world.level.block.Block>> rbmkBlocks = java.util.List.of(
            ModBlocks.RBMK_ROD, ModBlocks.RBMK_ROD_MOD, ModBlocks.RBMK_ROD_REASIM, ModBlocks.RBMK_ROD_REASIM_MOD,
            ModBlocks.RBMK_CONTROL,
            ModBlocks.RBMK_CONTROL_BLUE, ModBlocks.RBMK_CONTROL_GREEN, ModBlocks.RBMK_CONTROL_YELLOW,
            ModBlocks.RBMK_CONTROL_PURPLE, ModBlocks.RBMK_CONTROL_MOD, ModBlocks.RBMK_CONTROL_MOD_AUTO,
            ModBlocks.RBMK_CONTROL_AUTO, ModBlocks.RBMK_CONTROL_REASIM, ModBlocks.RBMK_CONTROL_REASIM_AUTO,
            ModBlocks.RBMK_MODERATOR, ModBlocks.RBMK_ABSORBER, ModBlocks.RBMK_REFLECTOR,
            ModBlocks.RBMK_COOLER, ModBlocks.RBMK_BOILER, ModBlocks.RBMK_HEATER,
            ModBlocks.RBMK_OUTGASSER, ModBlocks.RBMK_STORAGE, ModBlocks.RBMK_BLANK,
            ModBlocks.RBMK_STEAM_INLET, ModBlocks.RBMK_STEAM_OUTLET,
            ModBlocks.RBMK_LOADER, ModBlocks.RBMK_AUTOLOADER, ModBlocks.RBMK_CRANE_CONSOLE,
            ModBlocks.RBMK_DISPLAY, ModBlocks.RBMK_GAUGE, ModBlocks.RBMK_INDICATOR,
            ModBlocks.RBMK_LEVER, ModBlocks.RBMK_NUMITRON, ModBlocks.RBMK_GRAPH,
            ModBlocks.RBMK_TERMINAL, ModBlocks.RBMK_KEYPAD, ModBlocks.RBMK_DISPLAY_BLANK,
            ModBlocks.RBMK_DEBRIS, ModBlocks.RBMK_DEBRIS_BURNING,
            ModBlocks.RBMK_DEBRIS_DIGAMMA, ModBlocks.RBMK_DEBRIS_RADIATING
        );
        for (var rb : rbmkBlocks) {
            withExistingParent(rb.getId().getPath(), modLoc("block/rbmk/" + rb.getId().getPath()));
        }

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

        simpleItem(ModItems.COIL_TUNGSTEN);
        simpleItem(ModItems.NUGGET_SILICON);
        simpleItem(ModItems.NUGGET_TANTALIUM);
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
        simpleItem(ModItems.RADIUM_RAW);
        simpleItem(ModItems.SALTPETER);
        simpleItem(ModItems.CRYOLITE);
        simpleItem(ModItems.MOLYSITE);
        simpleItem(ModItems.RAREEARTH_RAW);
        simpleItem(ModItems.POWDER_CHLOROCALCITE);
        simpleItem(ModItems.POWDER_SODIUM);

        trimmedArmorItem(ModItems.ALLOY_HELMET);
        trimmedArmorItem(ModItems.ALLOY_CHESTPLATE);
        trimmedArmorItem(ModItems.ALLOY_LEGGINGS);
        trimmedArmorItem(ModItems.ALLOY_BOOTS);
        trimmedArmorItem(ModItems.TITANIUM_HELMET);
        trimmedArmorItem(ModItems.TITANIUM_CHESTPLATE);
        trimmedArmorItem(ModItems.TITANIUM_LEGGINGS);
        trimmedArmorItem(ModItems.TITANIUM_BOOTS);
        trimmedArmorItem(ModItems.SECURITY_HELMET);
        trimmedArmorItem(ModItems.SECURITY_CHESTPLATE);
        trimmedArmorItem(ModItems.SECURITY_LEGGINGS);
        trimmedArmorItem(ModItems.SECURITY_BOOTS);
        trimmedArmorItem(ModItems.ASBESTOS_HELMET);
        trimmedArmorItem(ModItems.ASBESTOS_CHESTPLATE);
        trimmedArmorItem(ModItems.ASBESTOS_LEGGINGS);
        trimmedArmorItem(ModItems.ASBESTOS_BOOTS);
        trimmedArmorItem(ModItems.AJR_HELMET);
        trimmedArmorItem(ModItems.AJR_CHESTPLATE);
        trimmedArmorItem(ModItems.AJR_LEGGINGS);
        trimmedArmorItem(ModItems.AJR_BOOTS);
        trimmedArmorItem(ModItems.STEEL_HELMET);
        trimmedArmorItem(ModItems.STEEL_CHESTPLATE);
        trimmedArmorItem(ModItems.STEEL_LEGGINGS);
        trimmedArmorItem(ModItems.STEEL_BOOTS);
        trimmedArmorItem(ModItems.PAA_HELMET);
        trimmedArmorItem(ModItems.PAA_CHESTPLATE);
        trimmedArmorItem(ModItems.PAA_LEGGINGS);
        trimmedArmorItem(ModItems.PAA_BOOTS);
        trimmedArmorItem(ModItems.LIQUIDATOR_HELMET);
        trimmedArmorItem(ModItems.LIQUIDATOR_CHESTPLATE);
        trimmedArmorItem(ModItems.LIQUIDATOR_LEGGINGS);
        trimmedArmorItem(ModItems.LIQUIDATOR_BOOTS);
        trimmedArmorItem(ModItems.HAZMAT_HELMET);
        trimmedArmorItem(ModItems.HAZMAT_CHESTPLATE);
        trimmedArmorItem(ModItems.HAZMAT_LEGGINGS);
        trimmedArmorItem(ModItems.HAZMAT_BOOTS);
        trimmedArmorItem(ModItems.STARMETAL_HELMET);
        trimmedArmorItem(ModItems.STARMETAL_CHESTPLATE);
        trimmedArmorItem(ModItems.STARMETAL_LEGGINGS);
        trimmedArmorItem(ModItems.STARMETAL_BOOTS);
        trimmedArmorItem(ModItems.COBALT_HELMET);
        trimmedArmorItem(ModItems.COBALT_CHESTPLATE);
        trimmedArmorItem(ModItems.COBALT_LEGGINGS);
        trimmedArmorItem(ModItems.COBALT_BOOTS);

        evenSimplerBlockItem(ModBlocks.REINFORCED_STONE_STAIRS);
        evenSimplerBlockItem(ModBlocks.REINFORCED_STONE_SLAB);

        evenSimplerBlockItem(ModBlocks.CONCRETE_HAZARD_STAIRS);
        evenSimplerBlockItem(ModBlocks.CONCRETE_HAZARD_SLAB);
        simpleBlockItem(ModBlocks.DOOR_BUNKER);
        simpleBlockItem(ModBlocks.DOOR_OFFICE);
        simpleBlockItem(ModBlocks.METAL_DOOR);
        simpleItem(ModItems.GRENADEHE);
        simpleItem(ModItems.GRENADEFIRE);


        ModBlocks.getAnvilBlocks().forEach(this::blockItemFromBlockModelMachine);
        
        // Регистрация моделей предметов для машин с кастомными 3D моделями
        blockItemFromBlockModelMachine(ModBlocks.PRESS);
        blockItemFromBlockModelMachine(ModBlocks.BLAST_FURNACE);
        blockItemFromBlockModelMachine(ModBlocks.WOOD_BURNER);
        blockItemFromBlockModelMachine(ModBlocks.CHEMICAL_PLANT);
        blockItemFromBlockModelMachine(ModBlocks.CRUCIBLE);
        blockItemFromBlockModelMachine(ModBlocks.FOUNDRY_BASIN);
        blockItemFromBlockModelMachine(ModBlocks.FOUNDRY_CHANNEL, "foundry_channel_inventory");
        blockItemFromBlockModelMachine(ModBlocks.CENTRIFUGE);
        blockItemFromBlockModelMachine(ModBlocks.GAS_CENTRIFUGE);
        blockItemFromBlockModelMachine(ModBlocks.CRYSTALLIZER, "crystallizer_item");
        blockItemFromBlockModelMachine(ModBlocks.BREEDER);
        blockItemFromBlockModelMachine(ModBlocks.LARGE_PYLON);
        blockItemFromBlockModelMachine(ModBlocks.HYDRAULIC_FRACKINING_TOWER);
        blockItemFromBlockModelMachine(ModBlocks.COOLING_TOWER);
        blockItemFromBlockModelMachine(ModBlocks.TOWER_SMALL);
        blockItemFromBlockModelMachine(ModBlocks.CYCLOTRON);

        // ─── Cast / Welded Plates ─────────────────────────────────────────────
        withExistingParent(ModItems.PLATE_CAST_IRON.getId().getPath(),       "item/generated").texture("layer0", modLoc("block/plate_cast_iron"));
        withExistingParent(ModItems.PLATE_CAST_STEEL.getId().getPath(),      "item/generated").texture("layer0", modLoc("block/plate_cast_steel"));
        withExistingParent(ModItems.PLATE_CAST_COPPER.getId().getPath(),     "item/generated").texture("layer0", modLoc("block/plate_cast_copper"));
        withExistingParent(ModItems.PLATE_CAST_GOLD.getId().getPath(),       "item/generated").texture("layer0", modLoc("block/plate_cast_gold"));
        withExistingParent(ModItems.PLATE_CAST_TITANIUM.getId().getPath(),   "item/generated").texture("layer0", modLoc("block/plate_cast_titanium"));
        withExistingParent(ModItems.PLATE_CAST_ALUMINIUM.getId().getPath(),  "item/generated").texture("layer0", modLoc("block/plate_cast_aluminium"));
        withExistingParent(ModItems.PLATE_CAST_TUNGSTEN.getId().getPath(),   "item/generated").texture("layer0", modLoc("block/plate_cast_tungsten"));
        withExistingParent(ModItems.PLATE_CAST_ZIRCONIUM.getId().getPath(),  "item/generated").texture("layer0", modLoc("block/plate_cast_zirconium"));
        withExistingParent(ModItems.PLATE_CAST_OSMIRIDIUM.getId().getPath(), "item/generated").texture("layer0", modLoc("block/plate_cast_osmiridium"));
        withExistingParent(ModItems.PLATE_CAST_ALLOY.getId().getPath(),      "item/generated").texture("layer0", modLoc("block/plate_cast_alloy"));
        withExistingParent(ModItems.PLATE_CAST_DURA_STEEL.getId().getPath(), "item/generated").texture("layer0", modLoc("block/plate_cast_dura_steel"));
        withExistingParent(ModItems.PLATE_CAST_DESH.getId().getPath(),       "item/generated").texture("layer0", modLoc("block/plate_cast_desh"));
        withExistingParent(ModItems.PLATE_CAST_STAR_METAL.getId().getPath(), "item/generated").texture("layer0", modLoc("block/plate_cast_star_metal"));
        withExistingParent(ModItems.PLATE_CAST_TCALLOY.getId().getPath(),    "item/generated").texture("layer0", modLoc("block/plate_cast_tcalloy"));
        withExistingParent(ModItems.PLATE_CAST_CDALLOY.getId().getPath(),    "item/generated").texture("layer0", modLoc("block/plate_cast_cdalloy"));
        withExistingParent(ModItems.PLATE_CAST_CMB.getId().getPath(),        "item/generated").texture("layer0", modLoc("block/plate_cast_cmb"));
        withExistingParent(ModItems.PLATE_CAST_SCHRABIDIUM.getId().getPath(),"item/generated").texture("layer0", modLoc("block/plate_cast_schrabidium"));
        withExistingParent(ModItems.PLATE_CAST_BBRONZE.getId().getPath(),    "item/generated").texture("layer0", modLoc("block/plate_cast_bbronze"));
        withExistingParent(ModItems.PLATE_CAST_ABRONZE.getId().getPath(),    "item/generated").texture("layer0", modLoc("block/plate_cast_abronze"));
        withExistingParent(ModItems.PLATE_CAST_SATURNITE.getId().getPath(),  "item/generated").texture("layer0", modLoc("block/plate_cast_saturnite"));
        withExistingParent(ModItems.PLATE_WELDED_IRON.getId().getPath(),       "item/generated").texture("layer0", modLoc("block/plate_welded_iron"));
        withExistingParent(ModItems.PLATE_WELDED_STEEL.getId().getPath(),      "item/generated").texture("layer0", modLoc("block/plate_welded_steel"));
        withExistingParent(ModItems.PLATE_WELDED_COPPER.getId().getPath(),     "item/generated").texture("layer0", modLoc("block/plate_welded_copper"));
        withExistingParent(ModItems.PLATE_WELDED_TITANIUM.getId().getPath(),   "item/generated").texture("layer0", modLoc("block/plate_welded_titanium"));
        withExistingParent(ModItems.PLATE_WELDED_ALUMINIUM.getId().getPath(),  "item/generated").texture("layer0", modLoc("block/plate_welded_aluminium"));
        withExistingParent(ModItems.PLATE_WELDED_TUNGSTEN.getId().getPath(),   "item/generated").texture("layer0", modLoc("block/plate_welded_tungsten"));
        withExistingParent(ModItems.PLATE_WELDED_ZIRCONIUM.getId().getPath(),  "item/generated").texture("layer0", modLoc("block/plate_welded_zirconium"));
        withExistingParent(ModItems.PLATE_WELDED_OSMIRIDIUM.getId().getPath(), "item/generated").texture("layer0", modLoc("block/plate_welded_osmiridium"));
        withExistingParent(ModItems.PLATE_WELDED_TCALLOY.getId().getPath(),    "item/generated").texture("layer0", modLoc("block/plate_welded_tcalloy"));
        withExistingParent(ModItems.PLATE_WELDED_CDALLOY.getId().getPath(),    "item/generated").texture("layer0", modLoc("block/plate_welded_cdalloy"));
        withExistingParent(ModItems.PLATE_WELDED_CMB.getId().getPath(),        "item/generated").texture("layer0", modLoc("block/plate_welded_cmb"));
        // Cyclotron particle parts
        withExistingParent(ModItems.PART_LITHIUM.getId().getPath(),   "item/generated").texture("layer0", modLoc("item/ingot/part_lithium"));
        withExistingParent(ModItems.PART_BERYLLIUM.getId().getPath(), "item/generated").texture("layer0", modLoc("item/ingot/part_beryllium"));
        withExistingParent(ModItems.PART_CARBON.getId().getPath(),    "item/generated").texture("layer0", modLoc("item/ingot/part_carbon"));
        withExistingParent(ModItems.PART_COPPER.getId().getPath(),    "item/generated").texture("layer0", modLoc("item/ingot/part_copper"));
        withExistingParent(ModItems.PART_PLUTONIUM.getId().getPath(), "item/generated").texture("layer0", modLoc("item/ingot/part_plutonium"));
        blockItemFromBlockModelMachine(ModBlocks.ZIRNOX);
        blockItemFromBlockModelMachine(ModBlocks.ARC_WELDER);
        blockItemFromBlockModelMachine(ModBlocks.SOLDERING_STATION);
        blockItemFromBlockModelMachine(ModBlocks.MIXER);
        blockItemFromBlockModelMachine(ModBlocks.DERRICK);
        blockItemFromBlockModelMachine(ModBlocks.RBMK_CONSOLE);
        blockItemFromBlockModelMachine(ModBlocks.FLARE_STACK);
        blockItemFromBlockModelMachine(ModBlocks.PUMPJACK);
        blockItemFromBlockModelMachine(ModBlocks.RADAR, "radar_item");
        blockItemFromBlockModelMachine(ModBlocks.LARGE_RADAR, "large_radar_item");
        blockItemFromBlockModelMachine(ModBlocks.RADAR_SCREEN);
        blockItemFromBlockModelMachine(ModBlocks.CRACKING_TOWER);
        blockItemFromBlockModelMachine(ModBlocks.FRACTION_TOWER);
        blockItemFromBlockModelMachine(ModBlocks.MINING_DRILL);
        blockItemFromBlockModelMachine(ModBlocks.FEL);
        blockItemFromBlockModelMachine(ModBlocks.SILEX);
        blockItemFromBlockModelMachine(ModBlocks.MACHINE_ASSEMBLER);
        blockItemFromBlockModelMachine(ModBlocks.ADVANCED_ASSEMBLY_MACHINE);
        blockItemFromBlockModelMachine(ModBlocks.FLUID_TANK);
        blockItemFromBlockModelMachine(ModBlocks.BAT9000);
        blockItemFromBlockModelMachine(ModBlocks.LAUNCH_PAD);
        blockItemFromBlockModelMachine(ModBlocks.LAUNCH_PAD_RUSTED);
        blockItemFromBlockModelBomb(ModBlocks.NUKE_FAT_MAN);
        blockItemFromBlockModelMachine(ModBlocks.MACHINE_BATTERY_SOCKET);
        blockItemFromBlockModelMachine(ModBlocks.INDUSTRIAL_BOILER);
        blockItemFromBlockModelMachine(ModBlocks.HEATING_OVEN);
        blockItemFromBlockModelMachine(ModBlocks.SOLAR_BOILER);
        blockItemFromBlockModelMachine(ModBlocks.SOLAR_MIRRORS);
        blockItemFromBlockModelMachine(ModBlocks.WATZ_POWERPLANT);
        blockItemFromBlockModelMachine(ModBlocks.HYDROTREATER);
        blockItemFromBlockModelMachine(ModBlocks.CATALYTIC_REFORMER);
        blockItemFromBlockModelMachine(ModBlocks.DEUTERIUM_TOWER);
        blockItemFromBlockModelMachine(ModBlocks.CHEMICAL_FACTORY);
        blockItemFromBlockModelMachine(ModBlocks.STEAM_TURBINE);
        blockItemFromBlockModelMachine(ModBlocks.LIQUEFACTOR);
        blockItemFromBlockModelMachine(ModBlocks.CORE_EMITTER);
        blockItemFromBlockModelMachine(ModBlocks.CORE_INJECTOR);
        blockItemFromBlockModelMachine(ModBlocks.CORE_RECEIVER);
        blockItemFromBlockModelMachine(ModBlocks.VACUUM_DISTILL);
        blockItemFromBlockModelMachine(ModBlocks.TURBOFAN);
        blockItemFromBlockModelMachine(ModBlocks.INDUSTRIAL_TURBINE);
        blockItemFromBlockModelMachine(ModBlocks.TURBINE);
        blockItemFromBlockModelMachine(ModBlocks.MACHINE_CHUNGUS, "chungus");
        blockItemFromBlockModelMachine(ModBlocks.SUBSTATION);

        // --- WIP Machines (3D OBJ models) ---
        blockItemFromBlockModelMachine(ModBlocks.AMMO_PRESS);
        blockItemFromBlockModelMachine(ModBlocks.ANNIHILATOR);
        blockItemFromBlockModelMachine(ModBlocks.ARC_FURNACE);
        blockItemFromBlockModelMachine(ModBlocks.ASSEMBLY_FACTORY);
        blockItemFromBlockModelMachine(ModBlocks.AUTOSAW);
        blockItemFromBlockModelMachine(ModBlocks.BEAMLINE);
        blockItemFromBlockModelMachine(ModBlocks.BOILER);
        withExistingParent(ModBlocks.PUMP_STEAM.getId().getPath(),
                modLoc("block/" + ModBlocks.PUMP_STEAM.getId().getPath()));
        withExistingParent(ModBlocks.PUMP_ELECTRIC.getId().getPath(),
                modLoc("block/" + ModBlocks.PUMP_ELECTRIC.getId().getPath()));
        blockItemFromBlockModelMachine(ModBlocks.BOILER_FUSION);
        blockItemFromBlockModelMachine(ModBlocks.BREEDER_FUSION);
        blockItemFromBlockModelMachine(ModBlocks.CHIMNEY_BRICK);
        blockItemFromBlockModelMachine(ModBlocks.CHIMNEY_INDUSTRIAL);
        blockItemFromBlockModelMachine(ModBlocks.COKER);
        blockItemFromBlockModelMachine(ModBlocks.COLLECTOR);
        blockItemFromBlockModelMachine(ModBlocks.COMBINATION_OVEN);
        blockItemFromBlockModelMachine(ModBlocks.COMBUSTION_ENGINE);
        blockItemFromBlockModelMachine(ModBlocks.COMPRESSOR);
        withExistingParent(ModBlocks.MACHINE_COMPRESSOR_COMPACT.getId().getPath(),
                modLoc("block/" + ModBlocks.MACHINE_COMPRESSOR_COMPACT.getId().getPath()));
        blockItemFromBlockModelMachine(ModBlocks.CONDENSER_POWERED);
        blockItemFromBlockModelMachine(ModBlocks.CONVEYOR_PRESS);
        blockItemFromBlockModelMachine(ModBlocks.COUPLER);
        blockItemFromBlockModelMachine(ModBlocks.DETECTOR);
        blockItemFromBlockModelMachine(ModBlocks.DIESELGEN);
        blockItemFromBlockModelMachine(ModBlocks.DIPOLE);
        blockItemFromBlockModelMachine(ModBlocks.DRONE);
        blockItemFromBlockModelMachine(ModBlocks.ELECTRIC_HEATER);
        blockItemFromBlockModelMachine(ModBlocks.ELECTROLYSER);
        blockItemFromBlockModelMachine(ModBlocks.EPRESS);
        blockItemFromBlockModelMachine(ModBlocks.EXPOSURE_CHAMBER);
        blockItemFromBlockModelMachine(ModBlocks.FENSU);
        // FENSU2 (machine_battery_redd) item model is generated by orientableBlockWithItem in ModBlockStateProvider.
        blockItemFromBlockModelMachine(ModBlocks.FIREBOX);
        blockItemFromBlockModelMachine(ModBlocks.FRACTION_SPACER);
        blockItemFromBlockModelMachine(ModBlocks.FURNACE_IRON);
        blockItemFromBlockModelMachine(ModBlocks.FURNACE_STEEL);
        blockItemFromBlockModelMachine(ModBlocks.HEATEX);
        blockItemFromBlockModelMachine(ModBlocks.HEPHAESTUS);
        blockItemFromBlockModelMachine(ModBlocks.ICF);
        blockItemFromBlockModelMachine(ModBlocks.INTAKE);
        blockItemFromBlockModelMachine(ModBlocks.KLYSTRON);
        blockItemFromBlockModelMachine(ModBlocks.MHDT);
        blockItemFromBlockModelMachine(ModBlocks.MICROWAVE);
        blockItemFromBlockModelMachine(ModBlocks.MINING_LASER);
        blockItemFromBlockModelMachine(ModBlocks.OILBURNER);
        blockItemFromBlockModelMachine(ModBlocks.OILBURNER_HP);
        blockItemFromBlockModelMachine(ModBlocks.ORBUS);
        blockItemFromBlockModelMachine(ModBlocks.ORE_SLOPPER);
        blockItemFromBlockModelMachine(ModBlocks.PLASMA_FORGE);
        blockItemFromBlockModelMachine(ModBlocks.PYROOVEN);
        blockItemFromBlockModelMachine(ModBlocks.QUADRUPOLE);
        blockItemFromBlockModelMachine(ModBlocks.RADGEN);
        blockItemFromBlockModelMachine(ModBlocks.RADIOLYSIS);
        blockItemFromBlockModelMachine(ModBlocks.REACTOR_SMALL);
        blockItemFromBlockModelMachine(ModBlocks.RFC);
        blockItemFromBlockModelMachine(ModBlocks.ROTARY_FURNACE);
        blockItemFromBlockModelMachine(ModBlocks.SAWMILL);
        blockItemFromBlockModelMachine(ModBlocks.SOLIDIFIER);
        blockItemFromBlockModelMachine(ModBlocks.ASHPIT);
        blockItemFromBlockModelMachine(ModBlocks.REACTOR_RESEARCH);
        blockItemFromBlockModelMachine(ModBlocks.MACHINE_RADGEN);
        itemModelFromBlockResourcePath(ModBlocks.CRANE_INSERTER.getId().getPath(), "block/crane_inserter_north");
        itemModelFromBlockResourcePath(ModBlocks.CRANE_EXTRACTOR.getId().getPath(), "block/crane_extractor_north");
        itemModelFromBlockResourcePath(ModBlocks.CRANE_GRABBER.getId().getPath(), "block/crane_grabber_north");
        itemModelFromBlockResourcePath(ModBlocks.CRANE_BOXER.getId().getPath(), "block/crane_boxer_north");
        itemModelFromBlockResourcePath(ModBlocks.CRANE_UNBOXER.getId().getPath(), "block/crane_unboxer_north");
        blockItemFromBlockModelMachine(ModBlocks.SOURCE);
        blockItemFromBlockModelMachine(ModBlocks.MACHINE_LARGE_TURBINE);
        blockItemFromBlockModelMachine(ModBlocks.LPW2);
        blockItemFromBlockModelMachine(ModBlocks.STEAM_ENGINE);
        blockItemFromBlockModelMachine(ModBlocks.STIRLING);
        blockItemFromBlockModelMachine(ModBlocks.STIRLING_CREATIVE);
        blockItemFromBlockModelMachine(ModBlocks.STIRLING_STEEL);
        blockItemFromBlockModelMachine(ModBlocks.STRAND_CASTER);
        blockItemFromBlockModelMachine(ModBlocks.THRESHER);
        blockItemFromBlockModelMachine(ModBlocks.TORUS);
        blockItemFromBlockModelMachine(ModBlocks.TURBINEGAS);
        blockItemFromBlockModelMachine(ModBlocks.WATZ_PUMP);
        blockItemFromBlockModelMachine(ModBlocks.CHUNGUS);

        blockItemFromBlockModelBomb(ModBlocks.DUD_CONVENTIONAL);
        blockItemFromBlockModelBomb(ModBlocks.DUD_NUKE);
        blockItemFromBlockModelBomb(ModBlocks.DUD_SALTED);

        blockItemFromBlockModel(ModBlocks.FLUID_VALVE);
        blockItemFromBlockModel(ModBlocks.FLUID_PUMP);
        blockItemFromBlockModel(ModBlocks.FLUID_EXHAUST);

        // Ранее: assets/.../models/item/*.json с parent = блок или простая generated/handheld-текстура
        blockItemFromBlockModelBomb(ModBlocks.AIRBOMB);
        itemModelFromBlockResourcePath("airbomb_a", "block/bomb/airbomb");
        itemModelFromBlockResourcePath("airnukebomb_a", "block/bomb/balebomb_test");
        blockItemFromBlockModelBomb(ModBlocks.BALEBOMB_TEST);
        blockItemFromBlockModel(ModBlocks.NUCLEAR_FALLOUT);
        blockItemFromBlockModel(ModBlocks.BLOCK_FALLOUT);

        blockItemFromBlockModel(ModBlocks.ANTENNA_TOP);
        blockItemFromBlockModel(ModBlocks.ASBESTOS_ORE);
        blockItemFromBlockModel(ModBlocks.B29);
        blockItemFromBlockModel(ModBlocks.SOYUZ_LAUNCHER);
        blockItemFromBlockModel(ModBlocks.DECO_SOYUZ_ROCKET);
        blockItemFromBlockModel(ModBlocks.BARBED_WIRE);
        blockItemFromBlockModel(ModBlocks.BARBED_WIRE_FIRE);
        blockItemFromBlockModel(ModBlocks.BARBED_WIRE_POISON);
        blockItemFromBlockModel(ModBlocks.BARBED_WIRE_RAD);
        blockItemFromBlockModel(ModBlocks.BARBED_WIRE_WITHER);
        blockItemFromBlockModel(ModBlocks.BARREL_CORRODED);
        blockItemFromBlockModel(ModBlocks.BARREL_IRON);
        blockItemFromBlockModel(ModBlocks.BARREL_LOX);
        blockItemFromBlockModel(ModBlocks.BARREL_ANTIMATTER);
        blockItemFromBlockModel(ModBlocks.BARREL_PINK);
        blockItemFromBlockModel(ModBlocks.BARREL_PLASTIC);
        blockItemFromBlockModel(ModBlocks.BARREL_RED);
        blockItemFromBlockModel(ModBlocks.BARREL_STEEL);
        blockItemFromBlockModel(ModBlocks.BARREL_TAINT);
        blockItemFromBlockModel(ModBlocks.BARREL_TCALLOY);
        blockItemFromBlockModel(ModBlocks.BARREL_VITRIFIED);
        blockItemFromBlockModel(ModBlocks.BARREL_YELLOW);
        blockItemFromBlockModel(ModBlocks.C4);
        blockItemFromBlockModel(ModBlocks.CAGE_LAMP);
        blockItemFromBlockModel(ModBlocks.CINNABAR_ORE);
        blockItemFromBlockModel(ModBlocks.COBALT_ORE);
        blockItemFromBlockModel(ModBlocks.CRATE_CONSERVE);
        blockItemFromBlockModel(ModBlocks.CRT_BROKEN);
        blockItemFromBlockModel(ModBlocks.CRT_BSOD);
        blockItemFromBlockModel(ModBlocks.CRT_CLEAN);
        blockItemFromBlockModel(ModBlocks.DECO_STEEL_SCAFFOLD);
        blockItemFromBlockModel(ModBlocks.DET_MINER);
        blockItemFromBlockModel(ModBlocks.DORNIER);
        blockItemFromBlockModel(ModBlocks.EXPLOSIVE_CHARGE);
        blockItemFromBlockModel(ModBlocks.FILE_CABINET);
        blockItemFromBlockModel(ModBlocks.FLOOD_LAMP);
        itemModelFromBlockResourcePath("fluorescent_lamp", "block/fluorescent_lamp");
        blockItemFromBlockModel(ModBlocks.FLUORITE_ORE);
        blockItemFromBlockModel(ModBlocks.FREAKY_ALIEN_BLOCK);
        blockItemFromBlockModel(ModBlocks.GEIGER_COUNTER_BLOCK);
        blockItemFromBlockModel(ModBlocks.LEAD_ORE);
        blockItemFromBlockModel(ModBlocks.LEAD_ORE_DEEPSLATE);
        blockItemFromBlockModel(ModBlocks.LIGNITE_ORE);
        blockItemFromBlockModelBomb(ModBlocks.MINE_AP);
        blockItemFromBlockModelBomb(ModBlocks.MINE_FAT);
        blockItemFromBlockModelBomb(ModBlocks.NAVAL_MINE);
        blockItemFromBlockModel(ModBlocks.NUCLEAR_CHARGE);
        blockItemFromBlockModel(ModBlocks.PUTER);
        blockItemFromBlockModel(ModBlocks.RAREGROUND_ORE);
        blockItemFromBlockModel(ModBlocks.RAREGROUND_ORE_DEEPSLATE);
        blockItemFromBlockModel(ModBlocks.REBAR);
        blockItemFromBlockModel(ModBlocks.REFINERY);
        blockItemFromBlockModel(ModBlocks.SHREDDER);
        blockItemFromBlockModel(ModBlocks.SMOKE_BOMB);
        blockItemFromBlockModel(ModBlocks.STEEL_POLE);
        blockItemFromBlockModel(ModBlocks.SULFUR_ORE);
        itemModelFromBlockResourcePath("switch", "block/switch_on");
        blockItemFromBlockModel(ModBlocks.TAPE_RECORDER);
        blockItemFromBlockModel(ModBlocks.THORIUM_ORE);
        blockItemFromBlockModel(ModBlocks.THORIUM_ORE_DEEPSLATE);
        blockItemFromBlockModel(ModBlocks.TITANIUM_ORE);
        blockItemFromBlockModel(ModBlocks.TITANIUM_ORE_DEEPSLATE);
        blockItemFromBlockModel(ModBlocks.TOASTER);
        blockItemFromBlockModel(ModBlocks.TUNGSTEN_ORE);
        blockItemFromBlockModel(ModBlocks.WASTE_CHARGE);

        withExistingParent(ModItems.NOLO_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.ENTITY_MOB_TAINTED_CREEPER_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.ENTITY_MOB_VOLATILE_CREEPER_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.ENTITY_MOB_PHOSGENE_CREEPER_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.ENTITY_MOB_GOLD_CREEPER_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.ENTITY_MOB_NUCLEAR_CREEPER_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));

        simpleItem(ModItems.GRENADE);
        simpleItem(ModItems.GRENADESLIME);
        simpleItem(ModItems.GRENADESMART);
        simpleItem(ModItems.SULFUR);
        simpleItem(ModItems.COKE_PETROLEUM);
        simpleItem(ModItems.ASH_WOOD);
        simpleItem(ModItems.ASH_COAL);
        simpleItem(ModItems.ASH_MISC);
        simpleItem(ModItems.ASH_FLY);
        simpleItem(ModItems.ASH_SOOT);
        simpleItem(ModItems.FLUORITE);
        simpleItem(ModItems.LIGNITE);
        simpleItem(ModItems.CINNABAR);
        simpleItem(ModItems.FIREBRICK);
        simpleItem(ModItems.FIRECLAY_BALL);
        simpleItem(ModItems.ARMOR_BATTERY);
        simpleItem(ModItems.ARMOR_BATTERY_MK2);
        simpleItem(ModItems.ARMOR_BATTERY_MK3);
        simpleItem(ModItems.RAREGROUND_ORE_CHUNK);
        simpleItem(ModItems.WOOD_ASH_POWDER);
        simpleItem(ModItems.URANIUM_RAW);
        simpleItem(ModItems.LEAD_RAW);
        simpleItem(ModItems.THORIUM_RAW);
        simpleItem(ModItems.TITANIUM_RAW);
        simpleItem(ModItems.TUNGSTEN_RAW);
        simpleItemModelByName("bucket_crude_oil", "bucket_crude_oil");
        simpleItemModelByName("iron_plate", "iron_plate");
        simpleItemModelByName("titanium_stamp_plate", "titanium_stamp_plate");
        withExistingParent(ModItems.BLUEPRINT_FOLDER.getId().getPath(), "item/generated")
                .texture("layer0", modLoc("item/template_folder"));
        blockItemFromBlockModel(ModBlocks.STRAWBERRY_BUSH);

        java.util.List.of(
                ModItems.CRYSTAL_ALUMINIUM,
                ModItems.CRYSTAL_BERYLLIUM,
                ModItems.CRYSTAL_CHARRED,
                ModItems.CRYSTAL_CINNEBAR,
                ModItems.CRYSTAL_COAL,
                ModItems.CRYSTAL_COBALT,
                ModItems.CRYSTAL_COPPER,
                ModItems.CRYSTAL_DIAMOND,
                ModItems.CRYSTAL_FLUORITE,
                ModItems.CRYSTAL_GOLD,
                ModItems.CRYSTAL_HARDENED,
                ModItems.CRYSTAL_HORN,
                ModItems.CRYSTAL_IRON,
                ModItems.CRYSTAL_LAPIS,
                ModItems.CRYSTAL_LEAD,
                ModItems.CRYSTAL_LITHIUM,
                ModItems.CRYSTAL_NITER,
                ModItems.CRYSTAL_OSMIRIDIUM,
                ModItems.CRYSTAL_PHOSPHORUS,
                ModItems.CRYSTAL_PLUTONIUM,
                ModItems.CRYSTAL_PULSAR,
                ModItems.CRYSTAL_RARE,
                ModItems.CRYSTAL_REDSTONE,
                ModItems.CRYSTAL_SCHRABIDIUM,
                ModItems.CRYSTAL_SCHRARANIUM,
                ModItems.CRYSTAL_STARMETAL,
                ModItems.CRYSTAL_SULFUR,
                ModItems.CRYSTAL_THORIUM,
                ModItems.CRYSTAL_TITANIUM,
                ModItems.CRYSTAL_TRIXITE,
                ModItems.CRYSTAL_TUNGSTEN,
                ModItems.CRYSTAL_URANIUM,
                ModItems.CRYSTAL_VIRUS,
                ModItems.CRYSTAL_XEN
        ).forEach(this::crystalItem);

        java.util.List.of(
                ModItems.ALLOY_SWORD,
                ModItems.ALLOY_AXE,
                ModItems.ALLOY_PICKAXE,
                ModItems.ALLOY_SHOVEL,
                ModItems.ALLOY_HOE,
                ModItems.STEEL_SWORD,
                ModItems.STEEL_AXE,
                ModItems.STEEL_PICKAXE,
                ModItems.STEEL_SHOVEL,
                ModItems.STEEL_HOE,
                ModItems.TITANIUM_SWORD,
                ModItems.TITANIUM_AXE,
                ModItems.TITANIUM_PICKAXE,
                ModItems.DRILL_TITANIUM,
                ModItems.TITANIUM_SHOVEL,
                ModItems.TITANIUM_HOE,
                ModItems.STARMETAL_SWORD,
                ModItems.STARMETAL_AXE,
                ModItems.STARMETAL_PICKAXE,
                ModItems.STARMETAL_SHOVEL,
                ModItems.STARMETAL_HOE
        ).forEach(this::handheldItem);

        // DEV: importierte fehlende Items aus dem Original-HBM (zur Sichtung)
        java.util.List.of(
                ModItems.ACETYLENE_TORCH,
                ModItems.AJR_LEGS,
                ModItems.AJR_PLATE,
                ModItems.AJRO_LEGS,
                ModItems.AJRO_PLATE,
                ModItems.ALLOY_LEGS,
                ModItems.ALLOY_PLATE,
                ModItems.AMMO_ARTY,
                ModItems.AMMO_ARTY_CARGO,
                ModItems.AMMO_ARTY_CHLORINE,
                ModItems.AMMO_ARTY_CLASSIC,
                ModItems.AMMO_ARTY_HE,
                ModItems.AMMO_ARTY_MINI_NUKE,
                ModItems.AMMO_ARTY_MINI_NUKE_MULTI,
                ModItems.AMMO_ARTY_MUSTARD_GAS,
                ModItems.AMMO_ARTY_NUKE,
                ModItems.AMMO_ARTY_PHOSGENE,
                ModItems.AMMO_ARTY_PHOSPHORUS,
                ModItems.AMMO_ARTY_PHOSPHORUS_MULTI,
                ModItems.AMMO_BAG,
                ModItems.AMMO_BAG_INFINITE,
                ModItems.AMMO_CONTAINER,
                ModItems.AMMO_DGK,
                ModItems.AMMO_FIREEXT,
                ModItems.AMMO_FIREEXT_FOAM,
                ModItems.AMMO_FIREEXT_SAND,
                ModItems.AMMO_SHELL,
                ModItems.AMMO_SHELL_APFSDS_DU,
                ModItems.AMMO_SHELL_APFSDS_T,
                ModItems.AMMO_SHELL_EXPLOSIVE,
                ModItems.AMMO_SHELL_W9,
                ModItems.AMS_CATALYST_ALUMINIUM,
                ModItems.AMS_CATALYST_BERYLLIUM,
                ModItems.AMS_CATALYST_BLANK,
                ModItems.AMS_CATALYST_CAESIUM,
                ModItems.AMS_CATALYST_CERIUM,
                ModItems.AMS_CATALYST_COBALT,
                ModItems.AMS_CATALYST_COPPER,
                ModItems.AMS_CATALYST_DINEUTRONIUM,
                ModItems.AMS_CATALYST_EUPHEMIUM,
                ModItems.AMS_CATALYST_IRON,
                ModItems.AMS_CATALYST_LITHIUM,
                ModItems.AMS_CATALYST_NIOBIUM,
                ModItems.AMS_CATALYST_SCHRABIDIUM,
                ModItems.AMS_CATALYST_STRONTIUM,
                ModItems.AMS_CATALYST_THORIUM,
                ModItems.AMS_CATALYST_TUNGSTEN,
                ModItems.AMS_CORE_EYEOFHARMONY,
                ModItems.AMS_CORE_SING,
                ModItems.AMS_CORE_THINGY,
                ModItems.AMS_CORE_WORMHOLE,
                ModItems.AMS_LENS,
                ModItems.ANALYSIS_TOOL,
                ModItems.ANALYZER,
                ModItems.ANCHOR_REMOTE,
                ModItems.APPLE_EUPHEMIUM,
                ModItems.APPLE_LEAD,
                ModItems.APPLE_SCHRABIDIUM,
                ModItems.ARC_ELECTRODE,
                ModItems.ARMOR_POLISH,
                ModItems.ASBESTOS_LEGS,
                ModItems.ASBESTOS_PLATE,
                ModItems.ASHGLASSES,
                ModItems.ASSEMBLY_NUKE,
                ModItems.ATTACHMENT_MASK,
                ModItems.ATTACHMENT_MASK_MONO,
                ModItems.AUSTRALIUM_III,
                ModItems.BACK_TESLA,
                ModItems.BALEFIRE_AND_HAM,
                ModItems.BALEFIRE_AND_STEEL,
                ModItems.BALEFIRE_SCRAMBLED,
                ModItems.BALL_DYNAMITE,
                ModItems.BALL_FIRECLAY,
                ModItems.BALL_RESIN,
                ModItems.BALL_TATB,
                ModItems.BALLISTIC_GAUNTLET,
                ModItems.BALLISTITE,
                ModItems.BANDAID,
                ModItems.BATHWATER,
                ModItems.BATHWATER_MK2,
                ModItems.BDCL,
                ModItems.BEDROCK_ORE_FRAGMENT,
                ModItems.BETA,
                ModItems.BIG_SWORD,
                ModItems.BILLET_ACTINIUM,
                ModItems.BILLET_AM241,
                ModItems.BILLET_AM242,
                ModItems.BILLET_AM_MIX,
                ModItems.BILLET_AMERICIUM_FUEL,
                ModItems.BILLET_AU198,
                ModItems.BILLET_AUSTRALIUM,
                ModItems.BILLET_AUSTRALIUM_GREATER,
                ModItems.BILLET_AUSTRALIUM_LESSER,
                ModItems.BILLET_BALEFIRE_GOLD,
                ModItems.BILLET_BERYLLIUM,
                ModItems.BILLET_BISMUTH,
                ModItems.BILLET_CO60,
                ModItems.BILLET_COBALT,
                ModItems.BILLET_FLASHLEAD,
                ModItems.BILLET_GH336,
                ModItems.BILLET_HES,
                ModItems.BILLET_LES,
                ModItems.BILLET_MOX_FUEL,
                ModItems.BILLET_NEPTUNIUM,
                ModItems.BILLET_NEPTUNIUM_FUEL,
                ModItems.BILLET_NUCLEAR_WASTE,
                ModItems.BILLET_PB209,
                ModItems.BILLET_PLUTONIUM_FUEL,
                ModItems.BILLET_PO210BE,
                ModItems.BILLET_POLONIUM,
                ModItems.BILLET_PU238,
                ModItems.BILLET_PU238BE,
                ModItems.BILLET_PU239,
                ModItems.BILLET_PU240,
                ModItems.BILLET_PU241,
                ModItems.BILLET_PU_MIX,
                ModItems.BILLET_RA226,
                ModItems.BILLET_RA226BE,
                ModItems.BILLET_SCHRABIDIUM,
                ModItems.BILLET_SCHRABIDIUM_FUEL,
                ModItems.BILLET_SOLINIUM,
                ModItems.BILLET_SR90,
                ModItems.BILLET_TECHNETIUM,
                ModItems.BILLET_TH232,
                ModItems.BILLET_THORIUM_FUEL,
                ModItems.BILLET_U233,
                ModItems.BILLET_U235,
                ModItems.BILLET_U238,
                ModItems.BILLET_URANIUM,
                ModItems.BILLET_URANIUM_FUEL,
                ModItems.BILLET_UZH,
                ModItems.BILLET_YHARONITE,
                ModItems.BILLET_ZFB_AM_MIX,
                ModItems.BILLET_ZFB_BISMUTH,
                ModItems.BILLET_ZFB_PU241,
                ModItems.BILLET_ZIRCONIUM,
                ModItems.BIO_WAFER,
                ModItems.BIOMASS,
                ModItems.BIOMASS_COMPRESSED,
                ModItems.BISMUTH_AXE,
                ModItems.BISMUTH_LEGS,
                ModItems.BISMUTH_PICKAXE,
                ModItems.BISMUTH_PLATE,
                ModItems.BISMUTH_TOOL,
                ModItems.BJ_BOOTS,
                ModItems.BJ_HELMET,
                ModItems.BJ_LEGS,
                ModItems.BJ_PLATE,
                ModItems.BJ_PLATE_JETPACK,
                ModItems.BLADE_METEORITE,
                ModItems.BLADE_TUNGSTEN,
                ModItems.BLADES_ADVANCED_ALLOY,
                ModItems.BLADES_DESH,
                ModItems.BLADES_STEEL,
                ModItems.BLADES_TITANIUM,
                ModItems.BLOWTORCH,
                ModItems.BLUEPRINTS,
                ModItems.BOARD_COPPER,
                ModItems.BOAT_RUBBER,
                ModItems.BOBMAZON,
                ModItems.BOLT_SPIKE,
                ModItems.BOLTGUN,
                ModItems.BOMB_CALLER,
                ModItems.BOMB_WAFFLE,
                ModItems.BOOK_GUIDE,
                ModItems.BOOK_LEMEGETON,
                ModItems.BOOK_OF_,
                ModItems.BOOK_SECRET,
                ModItems.BOTTLE2_EMPTY,
                ModItems.BOTTLE2_FRITZ,
                ModItems.BOTTLE2_KORL,
                ModItems.BOTTLE2_SUNSET,
                ModItems.BOTTLE_CHERRY,
                ModItems.BOTTLE_EMPTY,
                ModItems.BOTTLE_MERCURY,
                ModItems.BOTTLE_NUKA,
                ModItems.BOTTLE_OPENER,
                ModItems.BOTTLE_QUANTUM,
                ModItems.BOTTLE_RAD,
                ModItems.BOTTLE_SPARKLE,
                ModItems.BOTTLED_CLOUD,
                ModItems.BOY_BULLET,
                ModItems.BOY_IGNITER,
                ModItems.BOY_KIT,
                ModItems.BOY_PROPELLANT,
                ModItems.BOY_SHIELDING,
                ModItems.BOY_TARGET,
                ModItems.BROKEN_ITEM,
                ModItems.BUCKET_ACID,
                ModItems.BUCKET_MUD,
                ModItems.BUCKET_SCHRABIDIC_ACID,
                ModItems.BUCKET_SULFURIC_ACID,
                ModItems.BUCKET_TOXIC,
                ModItems.BURNT_BARK,
                ModItems.CANISTER_EMPTY,
                ModItems.CANISTER_NAPALM,
                ModItems.CANNED_SLIME,
                ModItems.CANTEEN_VODKA,
                ModItems.CAP_FRITZ,
                ModItems.CAP_KORL,
                ModItems.CAP_NUKA,
                ModItems.CAP_QUANTUM,
                ModItems.CAP_RAD,
                ModItems.CAP_SPARKLE,
                ModItems.CAP_STAR,
                ModItems.CAP_SUNSET,
                ModItems.CAPE_GASMASK,
                ModItems.CAPE_RADIATION,
                ModItems.CAPE_SCHRABIDIUM,
                ModItems.CARD_AOS,
                ModItems.CARD_QOS,
                ModItems.CASING_BAG,
                ModItems.CATALYST_CLAY,
                ModItems.CATALYTIC_CONVERTER,
                ModItems.CBT_DEVICE,
                ModItems.CELL_ANTI_SCHRABIDIUM,
                ModItems.CELL_ANTIMATTER,
                ModItems.CELL_BALEFIRE,
                ModItems.CELL_DEUTERIUM,
                ModItems.CELL_EMPTY,
                ModItems.CELL_PUF6,
                ModItems.CELL_TRITIUM,
                ModItems.CELL_UF6,
                ModItems.CENTRI_STICK,
                ModItems.CHAINSAW,
                ModItems.CHEESE,
                ModItems.CHEMISTRY_SET,
                ModItems.CHEMISTRY_SET_BORON,
                ModItems.CHERNOBYLSIGN,
                ModItems.CHLORINE_PINWHEEL,
                ModItems.CHLOROPHYTE_AXE,
                ModItems.CHLOROPHYTE_PICKAXE,
                ModItems.CHOCOLATE,
                ModItems.CHOCOLATE_MILK,
                ModItems.CHOPPER,
                ModItems.CHOPPER_BLADES,
                ModItems.CHOPPER_GUN,
                ModItems.CHOPPER_HEAD,
                ModItems.CHOPPER_TAIL,
                ModItems.CHOPPER_TORSO,
                ModItems.CHOPPER_WING,
                ModItems.CIGARETTE,
                ModItems.CINNEBAR,
                ModItems.CIRCUIT_STAR,
                ModItems.CLAY_TABLET,
                ModItems.CMB_AXE,
                ModItems.CMB_BOOTS,
                ModItems.CMB_HELMET,
                ModItems.CMB_HOE,
                ModItems.CMB_LEGS,
                ModItems.CMB_PLATE,
                ModItems.CMB_SHOVEL,
                ModItems.CMB_SWORD,
                ModItems.COAL_INFERNAL,
                ModItems.COBALT_AXE,
                ModItems.COBALT_DECORATED_AXE,
                ModItems.COBALT_DECORATED_HOE,
                ModItems.COBALT_DECORATED_PICKAXE,
                ModItems.COBALT_DECORATED_SHOVEL,
                ModItems.COBALT_DECORATED_SWORD,
                ModItems.COBALT_HOE,
                ModItems.COBALT_LEGS,
                ModItems.COBALT_PICKAXE,
                ModItems.COBALT_PLATE,
                ModItems.COBALT_SHOVEL,
                ModItems.COBALT_SWORD,
                ModItems.COFFEE,
                ModItems.COFFEE_RADIUM,
                ModItems.COIN_CREEPER,
                ModItems.COIN_MASKMAN,
                ModItems.COIN_RADIATION,
                ModItems.COIN_TOKEN,
                ModItems.COIN_UFO,
                ModItems.COIN_WORM,
                ModItems.COMBINE_SCRAP,
                ModItems.COMPONENT_EMITTER,
                ModItems.COMPONENT_LIMITER,
                ModItems.CONTAINMENT_BOX,
                ModItems.CORDITE,
                ModItems.COTTON_CANDY,
                ModItems.CRACKPIPE,
                ModItems.CRATE_CALLER,
                ModItems.CRUCIBLE_TEMPLATE,
                ModItems.CUBE_POWER,
                ModItems.CUSTOM_AMAT,
                ModItems.CUSTOM_DIRTY,
                ModItems.CUSTOM_FALL,
                ModItems.CUSTOM_HYDRO,
                ModItems.CUSTOM_KIT,
                ModItems.CUSTOM_NUKE,
                ModItems.CUSTOM_SCHRAB,
                ModItems.CUSTOM_TNT,
                ModItems.DEBRIS_CONCRETE,
                ModItems.DEBRIS_ELEMENT,
                ModItems.DEBRIS_EXCHANGER,
                ModItems.DEBRIS_FUEL,
                ModItems.DEBRIS_GRAPHITE,
                ModItems.DEBRIS_METAL,
                ModItems.DEBRIS_SHRAPNEL,
                ModItems.DEFINITELYFOOD,
                ModItems.DEFUSER_GOLD,
                ModItems.DEMON_CORE_CLOSED,
                ModItems.DEMON_CORE_OPEN,
                ModItems.DESH_AXE,
                ModItems.DESH_HOE,
                ModItems.DESH_PICKAXE,
                ModItems.DESH_SHOVEL,
                ModItems.DESH_SWORD,
                ModItems.DESIGNATOR_ARTY_RANGE,
                ModItems.DETONATOR_DE,
                ModItems.DETONATOR_DEADMAN,
                ModItems.DETONATOR_LASER,
                ModItems.DETONATOR_MULTI,
                ModItems.DEUTERIUM_FILTER,
                ModItems.DIAMOND_GAVEL,
                ModItems.DIESELSUIT_BOOTS,
                ModItems.DIESELSUIT_HELMET,
                ModItems.DIESELSUIT_LEGS,
                ModItems.DIESELSUIT_PLATE,
                ModItems.DISPERSER_CANISTER,
                ModItems.DNS_BOOTS,
                ModItems.DNS_HELMET,
                ModItems.DNS_LEGS,
                ModItems.DNS_PLATE,
                ModItems.DNT_LEGS,
                ModItems.DNT_PLATE,
                ModItems.DNT_SWORD,
                ModItems.DOOR_METAL,
                ModItems.DOOR_RED,
                ModItems.DRAX,
                ModItems.DRAX_MK2,
                ModItems.DRAX_MK3,
                ModItems.DRILLBIT_DESH,
                ModItems.DRILLBIT_DESH_DIAMOND,
                ModItems.DRILLBIT_FERRO,
                ModItems.DRILLBIT_FERRO_DIAMOND,
                ModItems.DRILLBIT_HSS,
                ModItems.DRILLBIT_HSS_DIAMOND,
                ModItems.DRILLBIT_STEEL,
                ModItems.DRILLBIT_STEEL_DIAMOND,
                ModItems.DRILLBIT_TCALLOY,
                ModItems.DRILLBIT_TCALLOY_DIAMOND,
                ModItems.DRONE_LINKER,
                ModItems.DRONE_PATROL,
                ModItems.DRONE_PATROL_CHUNKLOADING,
                ModItems.DRONE_PATROL_EXPRESS,
                ModItems.DRONE_PATROL_EXPRESS_CHUNKLOADING,
                ModItems.DRONE_REQUEST,
                ModItems.DWARVEN_PICKAXE,
                ModItems.DYSFUNCTIONAL_REACTOR,
                ModItems.EGG_BALEFIRE,
                ModItems.EGG_BALEFIRE_SHARD,
                ModItems.EGG_GLYPHID,
                ModItems.ELEC_SHOVEL,
                ModItems.ELEC_SWORD,
                ModItems.ENERGY_CORE,
                ModItems.ENTANGLEMENT_KIT,
                ModItems.ENVSUIT_BOOTS,
                ModItems.ENVSUIT_LEGS,
                ModItems.ENVSUIT_PLATE,
                ModItems.EUPHEMIUM_BOOTS,
                ModItems.EUPHEMIUM_HELMET,
                ModItems.EUPHEMIUM_LEGS,
                ModItems.EUPHEMIUM_PLATE,
                ModItems.FALLOUT,
                ModItems.FAU_BOOTS,
                ModItems.FAU_HELMET,
                ModItems.FAU_LEGS,
                ModItems.FAU_PLATE,
                ModItems.FILTER_COAL,
                ModItems.FINS_BIG_STEEL,
                ModItems.FINS_FLAT,
                ModItems.FINS_QUAD_TITANIUM,
                ModItems.FINS_SMALL_STEEL,
                ModItems.FINS_TRI_STEEL,
                ModItems.FLAME_CONSPIRACY,
                ModItems.FLAME_OPINION,
                ModItems.FLAME_POLITICS,
                ModItems.FLAME_PONY,
                ModItems.FLEIJA_CORE,
                ModItems.FLEIJA_IGNITER,
                ModItems.FLEIJA_KIT,
                ModItems.FLEIJA_PROPELLANT,
                ModItems.FLUID_IDENTIFIER_MULTI,
                ModItems.FLYWHEEL_BERYLLIUM,
                ModItems.FOODITEM,
                ModItems.FRAGMENT_ACTINIUM,
                ModItems.FRAGMENT_BORON,
                ModItems.FRAGMENT_CERIUM,
                ModItems.FRAGMENT_COBALT,
                ModItems.FRAGMENT_COLTAN,
                ModItems.FRAGMENT_LANTHANIUM,
                ModItems.FRAGMENT_METEORITE,
                ModItems.FRAGMENT_NEODYMIUM,
                ModItems.FRAGMENT_NIOBIUM,
                ModItems.FUSE,
                ModItems.FUSION_CORE,
                ModItems.FUSION_CORE_INFINITE,
                ModItems.FUSION_SHIELD_CHLOROPHYTE,
                ModItems.FUSION_SHIELD_DESH,
                ModItems.FUSION_SHIELD_TUNGSTEN,
                ModItems.FUSION_SHIELD_VAPORWAVE,
                ModItems.GADGET_CORE,
                ModItems.GADGET_EXPLOSIVE,
                ModItems.GADGET_KIT,
                ModItems.GADGET_WIREING,
                ModItems.GAS_MASK,
                ModItems.GAS_MASK_FILTER,
                ModItems.GAS_MASK_FILTER_COMBO,
                ModItems.GAS_MASK_FILTER_MONO,
                ModItems.GAS_MASK_FILTER_PISS,
                ModItems.GAS_MASK_FILTER_RAG,
                ModItems.GAS_MASK_M65,
                ModItems.GAS_MASK_MONO,
                ModItems.GAS_MASK_OLDE,
                ModItems.GAS_TESTER,
                ModItems.GEAR_LARGE,
                ModItems.GEM_ALEXANDRITE,
                ModItems.GEM_RAD,
                ModItems.GEM_SODALITE,
                ModItems.GEM_TANTALIUM,
                ModItems.GEM_VOLCANIC,
                ModItems.GENERATOR_FRONT,
                ModItems.GENERATOR_STEEL,
                ModItems.GLITCH,
                ModItems.GLOWING_STEW,
                ModItems.GLYPHID_GLAND,
                ModItems.GLYPHID_MEAT,
                ModItems.GLYPHID_MEAT_GRILLED,
                ModItems.GOGGLES,
                ModItems.GRENADE_UNIVERSAL,
                ModItems.GUN_B92,
                ModItems.GUN_FIREEXT,
                ModItems.GUN_KIT_1,
                ModItems.GUN_KIT_2,
                ModItems.GUN_PA_RANGED,
                ModItems.HAND_DRILL,
                ModItems.HAND_DRILL_DESH,
                ModItems.HAZMAT_BOOTS_GREY,
                ModItems.HAZMAT_BOOTS_RED,
                ModItems.HAZMAT_GREY_KIT,
                ModItems.HAZMAT_HELMET_GREY,
                ModItems.HAZMAT_HELMET_RED,
                ModItems.HAZMAT_KIT,
                ModItems.HAZMAT_LEGS,
                ModItems.HAZMAT_LEGS_GREY,
                ModItems.HAZMAT_LEGS_RED,
                ModItems.HAZMAT_PAA_BOOTS,
                ModItems.HAZMAT_PAA_HELMET,
                ModItems.HAZMAT_PAA_LEGS,
                ModItems.HAZMAT_PAA_PLATE,
                ModItems.HAZMAT_PLATE,
                ModItems.HAZMAT_PLATE_GREY,
                ModItems.HAZMAT_PLATE_RED,
                ModItems.HAZMAT_RED_KIT,
                ModItems.HEAVY_COMPONENT,
                ModItems.HEV_BOOTS,
                ModItems.HEV_HELMET,
                ModItems.HEV_LEGS,
                ModItems.HEV_PLATE,
                ModItems.HOLOTAPE_DAMAGED,
                ModItems.HORSESHOE_MAGNET,
                ModItems.HULL_BIG_ALUMINIUM,
                ModItems.HULL_BIG_STEEL,
                ModItems.HULL_BIG_TITANIUM,
                ModItems.HULL_SMALL_ALUMINIUM,
                ModItems.HULL_SMALL_STEEL,
                ModItems.ICF_PELLET,
                ModItems.ICF_PELLET_DEPLETED,
                ModItems.ICF_PELLET_EMPTY,
                ModItems.INDUSTRIAL_MAGNET,
                ModItems.INGOT_ALUMINIUM,
                ModItems.INJECTOR_5HTP,
                ModItems.INJECTOR_KNIFE,
                ModItems.INK,
                ModItems.INSERT_DOXIUM,
                ModItems.INSERT_DU,
                ModItems.INSERT_ERA,
                ModItems.INSERT_ESAPI,
                ModItems.INSERT_GHIORSIUM,
                ModItems.INSERT_KEVLAR,
                ModItems.INSERT_POLONIUM,
                ModItems.INSERT_SAPI,
                ModItems.INSERT_STEEL,
                ModItems.INSERT_XSAPI,
                ModItems.INSERT_YHARONITE,
                ModItems.IV_BLOOD,
                ModItems.IV_EMPTY,
                ModItems.IV_XP,
                ModItems.IV_XP_EMPTY,
                ModItems.JACKT,
                ModItems.JACKT2,
                ModItems.JETPACK_BOOST,
                ModItems.JETPACK_BREAK,
                ModItems.JETPACK_FLY,
                ModItems.JETPACK_TANK,
                ModItems.JETPACK_VECTOR,
                ModItems.JOURNAL_BJ,
                ModItems.JOURNAL_PIP,
                ModItems.JOURNAL_SILVER,
                ModItems.KEY,
                ModItems.KEY_RED,
                ModItems.KEY_RED_CRACKED,
                ModItems.LASER_CRYSTAL_BISMUTH,
                ModItems.LASER_CRYSTAL_CMB,
                ModItems.LASER_CRYSTAL_CO2,
                ModItems.LASER_CRYSTAL_DIGAMMA,
                ModItems.LASER_CRYSTAL_DNT,
                ModItems.LAUNCH_CODE,
                ModItems.LAUNCH_CODE_PIECE,
                ModItems.LAUNCH_KEY,
                ModItems.LEAD_GAVEL,
                ModItems.LEMON,
                ModItems.LINKER,
                ModItems.LIQUIDATOR_LEGS,
                ModItems.LIQUIDATOR_PLATE,
                ModItems.LITHIUM,
                ModItems.LODESTONE,
                ModItems.LOOP_STEW,
                ModItems.LOOPS,
                ModItems.LOOT_10,
                ModItems.LOOT_15,
                ModItems.LOOT_MISC,
                ModItems.MAN_CORE,
                ModItems.MAN_IGNITER,
                ModItems.MAN_KIT,
                ModItems.MARSHMALLOW,
                ModItems.MASK_OF_INFAMY,
                ModItems.MASK_PISS,
                ModItems.MASK_RAG,
                ModItems.MATCHSTICK,
                ModItems.MECH_KEY,
                ModItems.MED_BAG,
                ModItems.MED_IPECAC,
                ModItems.MED_PTSD,
                ModItems.MEDAL_LIQUIDATOR,
                ModItems.MELTDOWN_TOOL,
                ModItems.MEMESPOON,
                ModItems.MESE_AXE,
                ModItems.MESE_GAVEL,
                ModItems.MESE_PICKAXE,
                ModItems.METEOR_CHARM,
                ModItems.METEOR_REMOTE,
                ModItems.MIKE_COOLING_UNIT,
                ModItems.MIKE_CORE,
                ModItems.MIKE_DEUT,
                ModItems.MIKE_KIT,
                ModItems.MIRROR_TOOL,
                ModItems.MISSILE_ANTI_BALLISTIC,
                ModItems.MISSILE_CARRIER,
                ModItems.MISSILE_CUSTOM,
                ModItems.MISSILE_ENDO,
                ModItems.MISSILE_EXO,
                ModItems.MISSILE_KIT,
                ModItems.MORNING_GLORY,
                ModItems.MP_C_1,
                ModItems.MP_C_2,
                ModItems.MP_C_3,
                ModItems.MP_C_4,
                ModItems.MP_C_5,
                ModItems.MUCHO_MANGO,
                ModItems.MULTI_KIT,
                ModItems.N2_CHARGE,
                ModItems.NEUTRINO_LENS,
                ModItems.NIGHT_VISION,
                ModItems.NITRA,
                ModItems.NITRA_SMALL,
                ModItems.NO9,
                ModItems.NOTHING,
                ModItems.NUCLEAR_WASTE,
                ModItems.NUCLEAR_WASTE_LONG,
                ModItems.NUCLEAR_WASTE_LONG_DEPLETED,
                ModItems.NUCLEAR_WASTE_PEARL,
                ModItems.NUCLEAR_WASTE_SHORT,
                ModItems.NUCLEAR_WASTE_SHORT_DEPLETED,
                ModItems.NUCLEAR_WASTE_VITRIFIED,
                ModItems.NUGGET,
                ModItems.NUGGET_ACTINIUM,
                ModItems.NUGGET_AM241,
                ModItems.NUGGET_AM242,
                ModItems.NUGGET_AM_MIX,
                ModItems.NUGGET_AMERICIUM_FUEL,
                ModItems.NUGGET_ARSENIC,
                ModItems.NUGGET_AU198,
                ModItems.NUGGET_AUSTRALIUM,
                ModItems.NUGGET_AUSTRALIUM_GREATER,
                ModItems.NUGGET_AUSTRALIUM_LESSER,
                ModItems.NUGGET_BERYLLIUM,
                ModItems.NUGGET_BISMUTH,
                ModItems.NUGGET_CO60,
                ModItems.NUGGET_COBALT,
                ModItems.NUGGET_DESH,
                ModItems.NUGGET_DINEUTRONIUM,
                ModItems.NUGGET_EUPHEMIUM,
                ModItems.NUGGET_GH336,
                ModItems.NUGGET_HES,
                ModItems.NUGGET_LEAD,
                ModItems.NUGGET_LES,
                ModItems.NUGGET_MERCURY,
                ModItems.NUGGET_MOX_FUEL,
                ModItems.NUGGET_NEPTUNIUM,
                ModItems.NUGGET_NEPTUNIUM_FUEL,
                ModItems.NUGGET_NIOBIUM,
                ModItems.NUGGET_OSMIRIDIUM,
                ModItems.NUGGET_PB209,
                ModItems.NUGGET_PLUTONIUM,
                ModItems.NUGGET_PLUTONIUM_FUEL,
                ModItems.NUGGET_POLONIUM,
                ModItems.NUGGET_PU238,
                ModItems.NUGGET_PU239,
                ModItems.NUGGET_PU240,
                ModItems.NUGGET_PU241,
                ModItems.NUGGET_PU_MIX,
                ModItems.NUGGET_RA226,
                ModItems.NUGGET_SCHRABIDIUM,
                ModItems.NUGGET_SCHRABIDIUM_FUEL,
                ModItems.NUGGET_SOLINIUM,
                ModItems.NUGGET_SR90,
                ModItems.NUGGET_TECHNETIUM,
                ModItems.NUGGET_TH232,
                ModItems.NUGGET_THORIUM_FUEL,
                ModItems.NUGGET_U233,
                ModItems.NUGGET_U235,
                ModItems.NUGGET_U238,
                ModItems.NUGGET_URANIUM,
                ModItems.NUGGET_URANIUM_FUEL,
                ModItems.NUGGET_ZIRCONIUM,
                ModItems.NUKE_ADVANCED_KIT,
                ModItems.NUKE_COMMERCIALLY_KIT,
                ModItems.NUKE_ELECTRIC_KIT,
                ModItems.NUKE_STARTER_KIT,
                ModItems.ORE_BEDROCK,
                ModItems.ORE_CENTRIFUGED,
                ModItems.ORE_CLEANED,
                ModItems.ORE_DEEPCLEANED,
                ModItems.ORE_DENSITY_SCANNER,
                ModItems.ORE_ENRICHED,
                ModItems.ORE_NITRATED,
                ModItems.ORE_NITROCRYSTALLINE,
                ModItems.ORE_PURIFIED,
                ModItems.ORE_RADCLEANED,
                ModItems.ORE_SEARED,
                ModItems.ORE_SEPARATED,
                ModItems.OVERFUSE,
                ModItems.PAA_LEGS,
                ModItems.PAA_PLATE,
                ModItems.PADLOCK,
                ModItems.PADLOCK_REINFORCED,
                ModItems.PADLOCK_RUSTY,
                ModItems.PADLOCK_UNBREAKABLE,
                ModItems.PADS_RUBBER,
                ModItems.PADS_SLIME,
                ModItems.PADS_STATIC,
                ModItems.PANCAKE,
                ModItems.PART_BARREL_HEAVY,
                ModItems.PART_BARREL_LIGHT,
                ModItems.PART_GRIP,
                ModItems.PART_MECHANISM,
                ModItems.PART_RECEIVER_HEAVY,
                ModItems.PART_RECEIVER_LIGHT,
                ModItems.PART_STOCK,
                ModItems.KEY_PIN,
                ModItems.PARTICLE_AMAT,
                ModItems.PARTICLE_ASCHRAB,
                ModItems.PARTICLE_COPPER,
                ModItems.PARTICLE_DARK,
                ModItems.PARTICLE_DIGAMMA,
                ModItems.PARTICLE_EMPTY,
                ModItems.PARTICLE_HIGGS,
                ModItems.PARTICLE_HYDROGEN,
                ModItems.PARTICLE_LEAD,
                ModItems.PARTICLE_LUTECE,
                ModItems.PARTICLE_MUON,
                ModItems.PARTICLE_SPARKTICLE,
                ModItems.PARTICLE_STRANGE,
                ModItems.PARTICLE_TACHYON,
                ModItems.PARTS_LEGENDARY,
                ModItems.PEAS,
                ModItems.PEDESTAL_STEEL,
                ModItems.PELLET_ANTIMATTER,
                ModItems.PELLET_CLUSTER,
                ModItems.PELLET_GAS,
                ModItems.PELLET_RTG,
                ModItems.PELLET_RTG_ACTINIUM,
                ModItems.PELLET_RTG_AMERICIUM,
                ModItems.PELLET_RTG_BERKELIUM,
                ModItems.PELLET_RTG_COBALT,
                ModItems.PELLET_RTG_GOLD,
                ModItems.PELLET_RTG_LEAD,
                ModItems.PELLET_RTG_POLONIUM,
                ModItems.PELLET_RTG_RADIUM,
                ModItems.PELLET_RTG_STRONTIUM,
                ModItems.PELLET_RTG_WEAK,
                ModItems.PHOTO_PANEL,
                ModItems.PILE_ROD_BORON,
                ModItems.PILE_ROD_DETECTOR,
                ModItems.PILE_ROD_LITHIUM,
                ModItems.PILE_ROD_PLUTONIUM,
                ModItems.PILE_ROD_PU239,
                ModItems.PILE_ROD_SOURCE,
                ModItems.PILE_ROD_URANIUM,
                ModItems.PILL_HERBAL,
                ModItems.PILL_IODINE,
                ModItems.PILL_RED,
                ModItems.PIN,
                ModItems.PIPES_STEEL,
                ModItems.PIPETTE,
                ModItems.PIPETTE_BORON,
                ModItems.PIPETTE_LABORATORY,
                ModItems.PISTON_SELENIUM,
                ModItems.PISTON_SET_DESH,
                ModItems.PISTON_SET_DURA,
                ModItems.PISTON_SET_STARMETAL,
                ModItems.PISTON_SET_STEEL,
                ModItems.PLAN_C,
                ModItems.PLASTIC_BAG,
                ModItems.PLATE_ALUMINIUM,
                ModItems.PLATE_POLYMER,
                ModItems.POLAROID,
                ModItems.POLLUTION_DETECTOR,
                ModItems.POWER_NET_TOOL,
                ModItems.PROTECTION_CHARM,
                ModItems.PROTOTYPE_KIT,
                ModItems.PUDDING,
                ModItems.PWR_PRINTER,
                ModItems.QUARTZ_PLUTONIUM,
                ModItems.RADAR_LINKER,
                ModItems.RAG,
                ModItems.RAG_DAMP,
                ModItems.RAG_PISS,
                ModItems.RBMK_FUEL_BALEFIRE,
                ModItems.RBMK_FUEL_BALEFIRE_GOLD,
                ModItems.RBMK_FUEL_FLASHLEAD,
                ModItems.RBMK_FUEL_HEA241,
                ModItems.RBMK_FUEL_HEA242,
                ModItems.RBMK_FUEL_HEAUS,
                ModItems.RBMK_FUEL_HEN,
                ModItems.RBMK_FUEL_HEP_ALT,
                ModItems.RBMK_FUEL_HEP241,
                ModItems.RBMK_FUEL_HES,
                ModItems.RBMK_FUEL_HEU233,
                ModItems.RBMK_FUEL_LEA,
                ModItems.RBMK_FUEL_LEAUS,
                ModItems.RBMK_FUEL_LES,
                ModItems.RBMK_FUEL_MEA,
                ModItems.RBMK_FUEL_MEN,
                ModItems.RBMK_FUEL_MEP,
                ModItems.RBMK_FUEL_MES,
                ModItems.RBMK_FUEL_MEU,
                ModItems.RBMK_FUEL_PO210BE,
                ModItems.RBMK_FUEL_PU238BE,
                ModItems.RBMK_FUEL_RA226BE,
                ModItems.RBMK_FUEL_THMEU,
                ModItems.RBMK_FUEL_UEU,
                ModItems.RBMK_FUEL_UZH,
                ModItems.RBMK_FUEL_ZFB_AM_MIX,
                ModItems.RBMK_FUEL_ZFB_BISMUTH,
                ModItems.RBMK_FUEL_ZFB_PU241,
                ModItems.RBMK_PELLET_BALEFIRE,
                ModItems.RBMK_PELLET_BALEFIRE_GOLD,
                ModItems.RBMK_PELLET_DRX,
                ModItems.RBMK_PELLET_FLASHLEAD,
                ModItems.RBMK_PELLET_HEA241,
                ModItems.RBMK_PELLET_HEA242,
                ModItems.RBMK_PELLET_HEAUS,
                ModItems.RBMK_PELLET_HEN,
                ModItems.RBMK_PELLET_HEP241,
                ModItems.RBMK_PELLET_HES,
                ModItems.RBMK_PELLET_HEU233,
                ModItems.RBMK_PELLET_LEA,
                ModItems.RBMK_PELLET_LEAUS,
                ModItems.RBMK_PELLET_LES,
                ModItems.RBMK_PELLET_MEA,
                ModItems.RBMK_PELLET_MEN,
                ModItems.RBMK_PELLET_MEP,
                ModItems.RBMK_PELLET_MES,
                ModItems.RBMK_PELLET_MEU,
                ModItems.RBMK_PELLET_PO210BE,
                ModItems.RBMK_PELLET_PU238BE,
                ModItems.RBMK_PELLET_RA226BE,
                ModItems.RBMK_PELLET_THMEU,
                ModItems.RBMK_PELLET_UEU,
                ModItems.RBMK_PELLET_UZH,
                ModItems.RBMK_PELLET_ZFB_AM_MIX,
                ModItems.RBMK_PELLET_ZFB_BISMUTH,
                ModItems.RBMK_PELLET_ZFB_PU241,
                ModItems.RBMK_TOOL,
                ModItems.REACHER,
                ModItems.REACTOR_CORE,
                ModItems.REACTOR_SENSOR,
                ModItems.REBAR_PLACER,
                ModItems.REDSTONE_SWORD,
                ModItems.RING_PULL,
                ModItems.RING_STARMETAL,
                ModItems.ROBES_BOOTS,
                ModItems.ROBES_HELMET,
                ModItems.ROBES_LEGS,
                ModItems.ROBES_PLATE,
                ModItems.ROCKET_FUEL,
                ModItems.ROD_DUAL_EMPTY,
                ModItems.ROD_EMPTY,
                ModItems.ROD_OF_DISCORD,
                ModItems.ROD_QUAD_EMPTY,
                ModItems.RPA_BOOTS,
                ModItems.RPA_HELMET,
                ModItems.RPA_LEGS,
                ModItems.RPA_PLATE,
                ModItems.RTG_UNIT,
                ModItems.RTTY_PAGER,
                ModItems.RUNE_BLANK,
                ModItems.RUNE_DAGAZ,
                ModItems.RUNE_HAGALAZ,
                ModItems.RUNE_ISA,
                ModItems.RUNE_JERA,
                ModItems.RUNE_THURISAZ,
                ModItems.SAFETY_FUSE,
                ModItems.SAT_CHIP,
                ModItems.SAT_COORD,
                ModItems.SAT_DESIGNATOR,
                ModItems.SAT_GERALD,
                ModItems.SAT_HEAD_SCANNER,
                ModItems.SAT_INTERFACE,
                ModItems.SAT_LUNAR_MINER,
                ModItems.SAT_MINER,
                ModItems.SAT_RELAY,
                ModItems.SAWBLADE,
                ModItems.SCHNITZEL_VEGAN,
                ModItems.SCHRABIDIUM_AXE,
                ModItems.SCHRABIDIUM_BOOTS,
                ModItems.SCHRABIDIUM_HAMMER,
                ModItems.SCHRABIDIUM_HELMET,
                ModItems.SCHRABIDIUM_HOE,
                ModItems.SCHRABIDIUM_LEGS,
                ModItems.SCHRABIDIUM_PICKAXE,
                ModItems.SCHRABIDIUM_PLATE,
                ModItems.SCHRABIDIUM_SHOVEL,
                ModItems.SCHRABIDIUM_SWORD,
                ModItems.SCRAP_NUCLEAR,
                ModItems.SCRAP_OIL,
                ModItems.SCRAP_PLASTIC,
                ModItems.SCRAPS,
                ModItems.SCREWDRIVER_DESH,
                ModItems.SCRUMPY,
                ModItems.SECURITY_LEGS,
                ModItems.SECURITY_PLATE,
                ModItems.SEG_10,
                ModItems.SEG_15,
                ModItems.SEG_20,
                ModItems.SERUM,
                ModItems.SERVO_SET,
                ModItems.SERVO_SET_DESH,
                ModItems.SETTINGS_TOOL,
                ModItems.SHACKLES,
                ModItems.SHIMMER_AXE,
                ModItems.SHIMMER_AXE_HEAD,
                ModItems.SHIMMER_HANDLE,
                ModItems.SHIMMER_HEAD,
                ModItems.SHIMMER_SLEDGE,
                ModItems.SINGULARITY,
                ModItems.SIOX,
                ModItems.SIPHON,
                ModItems.SMASHING_HAMMER,
                ModItems.SOLID_FUEL,
                ModItems.SOLID_FUEL_BF,
                ModItems.SOLID_FUEL_PRESTO,
                ModItems.SOLID_FUEL_PRESTO_BF,
                ModItems.SOLID_FUEL_PRESTO_TRIPLET,
                ModItems.SOLID_FUEL_PRESTO_TRIPLET_BF,
                ModItems.SOLINIUM_CORE,
                ModItems.SOLINIUM_IGNITER,
                ModItems.SOLINIUM_KIT,
                ModItems.SOLINIUM_PROPELLANT,
                ModItems.SOPSIGN,
                ModItems.SPAWN_DUCK,
                ModItems.SPAWN_UFO,
                ModItems.SPAWN_WORM,
                ModItems.SPHERE_STEEL,
                ModItems.SPIDER_MILK,
                ModItems.SPONGEBOB_MACARONI,
                ModItems.STAMP_357,
                ModItems.STAMP_44,
                ModItems.STAMP_50,
                ModItems.STAMP_9,
                ModItems.STARMETAL_LEGS,
                ModItems.STARMETAL_PLATE,
                ModItems.STATIC_SANDWICH,
                ModItems.STEALTH_BOY,
                ModItems.STEAMSUIT_BOOTS,
                ModItems.STEAMSUIT_HELMET,
                ModItems.STEAMSUIT_LEGS,
                ModItems.STEAMSUIT_PLATE,
                ModItems.STEEL_LEGS,
                ModItems.STEEL_PLATE,
                ModItems.STICK_C4,
                ModItems.STICK_DYNAMITE,
                ModItems.STICK_DYNAMITE_FISHING,
                ModItems.STICK_SEMTEX,
                ModItems.STICK_TNT,
                ModItems.STOPSIGN,
                ModItems.STRUCTURE_CUSTOMMACHINE,
                ModItems.SURVEY_SCANNER,
                ModItems.SYRINGE_ANTIDOTE,
                ModItems.SYRINGE_AWESOME,
                ModItems.SYRINGE_EMPTY,
                ModItems.SYRINGE_METAL_EMPTY,
                ModItems.SYRINGE_METAL_MEDX,
                ModItems.SYRINGE_METAL_PSYCHO,
                ModItems.SYRINGE_METAL_STIMPAK,
                ModItems.SYRINGE_METAL_SUPER,
                ModItems.SYRINGE_MKUNICORN,
                ModItems.SYRINGE_POISON,
                ModItems.SYRINGE_TAINT,
                ModItems.TANK_STEEL,
                ModItems.TAURUN_BOOTS,
                ModItems.TAURUN_HELMET,
                ModItems.TAURUN_LEGS,
                ModItems.TAURUN_PLATE,
                ModItems.TEM_FLAKES,
                ModItems.THERMO_ELEMENT,
                ModItems.THRUSTER_NUCLEAR,
                ModItems.TITANIUM_FILTER,
                ModItems.TITANIUM_LEGS,
                ModItems.TITANIUM_PLATE,
                ModItems.TRENCHMASTER_BOOTS,
                ModItems.TRENCHMASTER_HELMET,
                ModItems.TRENCHMASTER_LEGS,
                ModItems.TRENCHMASTER_PLATE,
                ModItems.TRINITITE,
                ModItems.TSAR_CORE,
                ModItems.TSAR_KIT,
                ModItems.TURBINE_TUNGSTEN,
                ModItems.TURRET_CHIP,
                ModItems.TWINKIE,
                ModItems.ULLAPOOL_CABER,
                ModItems.UNDEFINED,
                ModItems.VOLCANIC_AXE,
                ModItems.VOLCANIC_PICKAXE,
                ModItems.WAND_D,
                ModItems.WAND_S,
                ModItems.WARHEAD_INCENDIARY_LARGE,
                ModItems.WASTE_MOX,
                ModItems.WASTE_PLATE_MOX,
                ModItems.WASTE_PLATE_PU238BE,
                ModItems.WASTE_PLATE_RA226BE,
                ModItems.WASTE_PLATE_SA326,
                ModItems.WASTE_PLUTONIUM,
                ModItems.WASTE_SCHRABIDIUM,
                ModItems.WASTE_THORIUM,
                ModItems.WASTE_URANIUM,
                ModItems.WASTE_ZFB_MOX,
                ModItems.WATCH,
                ModItems.WD40,
                ModItems.WILD_P,
                ModItems.WINGS_LIMP,
                ModItems.WINGS_MURK,
                ModItems.WIRING_RED_COPPER,
                ModItems.WOOD_GAVEL,
                ModItems.WRENCH,
                ModItems.WRENCH_ARCHINEER,
                ModItems.WRENCH_FLIPPED,
                ModItems.XANAX,
                ModItems.ZIRCONIUM_LEGS
        ).forEach(this::simpleItem);

        // Bedrock Ore Progression: Rohprodukt + alle 156 Veredelungsstufen (Grade x Type)
        simpleItem(ModItems.BEDROCK_ORE_BASE);
        ModItems.BEDROCK_ORE_ALL_VARIANTS.forEach(this::simpleItem);
    };

    /**
     * Вспомогательный метод для генерации простой модели предмета.
     * Он предполагает, что модель имеет родителя "item/generated" и одну текстуру "layer0".
     * Это стандарт для большинства 2D предметов в Minecraft.
     * @param itemObject RegistrySupplier предмета, для которого генерируется модель.
     */

    private void simpleItem(RegistrySupplier<Item> itemObject) {
        // Получаем имя предмета из его ID (например, "uranium_ingot")
        String name = itemObject.getId().getPath();
        
        // Генерируем .json файл модели.
        // Он будет искать текстуру по пути 'assets/hbm_m/textures/item/ИМЯ_ПРЕДМЕТА.png'
        withExistingParent(name, "item/generated")
                .texture("layer0", modLoc("item/" + name));

    }
    private ItemModelBuilder simpleBlockItem(RegistrySupplier<Block> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.tryParse("item/generated")).texture("layer0",
                //? if fabric && < 1.21.1 {
                /*new ResourceLocation(MainRegistry.MOD_ID,"item/" + item.getId().getPath()));
                *///?} else {
                                ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID,"item/" + item.getId().getPath()));
                //?}

    }

    private ItemModelBuilder blockItemFromBlockModelMachine(RegistrySupplier<Block> block) {
        return blockItemFromBlockModelMachine(block, block.getId().getPath());
    }

    private ItemModelBuilder blockItemFromBlockModelMachine(RegistrySupplier<Block> block, String machineModelFileName) {
        return withExistingParent(block.getId().getPath(), modLoc("block/machines/" + machineModelFileName));
    }

    /** Модель предмета с parent = hbm_m:&lt;путь&gt; (без отдельного ModBlocks, если id не совпадает с блоком). */
    private void itemModelFromBlockResourcePath(String itemModelName, String pathUnderModWithoutNamespace) {
        withExistingParent(itemModelName, modLoc(pathUnderModWithoutNamespace));
    }

    private void handheldItem(RegistrySupplier<Item> itemObject) {
        String name = itemObject.getId().getPath();
        withExistingParent(name, "item/handheld").texture("layer0", modLoc("item/" + name));
    }

    private void crystalItem(RegistrySupplier<Item> itemObject) {
        String name = itemObject.getId().getPath();
        withExistingParent(name, "item/generated").texture("layer0", modLoc("item/crystall/" + name));
    }

    /** item/generated, текстура hbm_m:item/&lt;texturePathUnderItem&gt; */
    private void simpleItemModelByName(String modelName, String texturePathUnderItem) {
        withExistingParent(modelName, "item/generated").texture("layer0", modLoc("item/" + texturePathUnderItem));
    }

    private ItemModelBuilder blockItemFromBlockModelBomb(RegistrySupplier<Block> block) {
        return withExistingParent(block.getId().getPath(), modLoc("block/bomb/" + block.getId().getPath()));
    }

    private ItemModelBuilder blockItemFromBlockModel(RegistrySupplier<Block> block) {
        return withExistingParent(block.getId().getPath(), modLoc("block/" + block.getId().getPath()));
    }
    
    private void ingotItem(RegistrySupplier<Item> itemObject) {
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

    private void powdersItem(RegistrySupplier<Item> itemObject) {
        String registrationName = itemObject.getId().getPath();
        String baseName = registrationName.replace("_powder", "");
        String textureFileName = "powder_" + baseName;
        withExistingParent(registrationName, "item/generated")
                .texture("layer0", modLoc("item/powders/" + textureFileName));
    }

    private void tinyPowderItem(RegistrySupplier<Item> itemObject) {
        String registrationName = itemObject.getId().getPath();
        String baseName = registrationName.replace("_powder_tiny", "");
        String textureFileName = "powder_" + baseName + "_tiny";
        withExistingParent(registrationName, "item/generated")
                .texture("layer0", modLoc("item/powders/tiny/" + textureFileName));
    }

    private void powderTexture(RegistrySupplier<Item> itemObject, String texturePath) {
        withExistingParent(itemObject.getId().getPath(), "item/generated")
                .texture("layer0", modLoc("item/" + texturePath));
    }

    private void generatedItemIfTextureExists(RegistrySupplier<Item> item, String texturePath) {
        ResourceLocation texture = modLoc("textures/item/" + texturePath + ".png");
        if (!existingFileHelper.exists(texture, PackType.CLIENT_RESOURCES)) {
            return;
        }
        withExistingParent(item.getId().getPath(), "item/generated")
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

    private void trimmedArmorItem(RegistrySupplier<Item> itemRegistrySupplier) {
        final String MOD_ID = MainRegistry.MOD_ID;

        if(itemRegistrySupplier.get() instanceof ArmorItem armorItem) {
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
                //? if fabric && < 1.21.1 {
                /*ResourceLocation armorItemResLoc = new ResourceLocation(MOD_ID, armorItemPath);
                *///?} else {
                                ResourceLocation armorItemResLoc = ResourceLocation.fromNamespaceAndPath(MOD_ID, armorItemPath);
                //?}

                ResourceLocation trimResLoc = ResourceLocation.tryParse(trimPath); // minecraft namespace
                //? if fabric && < 1.21.1 {
                /*ResourceLocation trimNameResLoc = new ResourceLocation(MOD_ID, currentTrimName);
                *///?} else {
                                ResourceLocation trimNameResLoc = ResourceLocation.fromNamespaceAndPath(MOD_ID, currentTrimName);
                //?}


                existingFileHelper.trackGenerated(trimResLoc, PackType.CLIENT_RESOURCES, ".png", "textures");

                getBuilder(currentTrimName)
                        .parent(new ModelFile.UncheckedModelFile("item/generated"))
                        .texture("layer0", armorItemResLoc)
                        .texture("layer1", trimResLoc);

                this.withExistingParent(itemRegistrySupplier.getId().getPath(),
                                mcLoc("item/generated"))
                        .override()
                        .model(new ModelFile.UncheckedModelFile(trimNameResLoc))
                        .predicate(mcLoc("trim_type"), trimValue).end()
                        .texture("layer0",
                                //? if fabric && < 1.21.1 {
                                /*new ResourceLocation(MOD_ID,
                                        "item/" + itemRegistrySupplier.getId().getPath()));
                                *///?} else {
                                                                ResourceLocation.fromNamespaceAndPath(MOD_ID,
                                        "item/" + itemRegistrySupplier.getId().getPath()));
                                //?}

            });
        }
    }

    public void evenSimplerBlockItem(RegistrySupplier<Block> block) {
        this.withExistingParent(MainRegistry.MOD_ID + ":" + BuiltInRegistries.BLOCK.getKey(block.get()).getPath(),
                modLoc("block/" + BuiltInRegistries.BLOCK.getKey(block.get()).getPath()));
    }

    /**
     * {@code hbm_m:missile_loader} item model — OBJ under {@code models/missiles/}, texture under {@code models/missile/}.
     * No {@code display} block: transforms come from {@link com.hbm_m.client.render.item.ItemRenderMissileGeneric}.
     */
    private void missileItemFromObjModel(String itemPath, MissileFormFactorModels hull, ResourceLocation texture) {
        objPartItemModel(itemPath, hull.getObjModel(), texture, hull.getPartNames().toArray(String[]::new));
    }

    /**
     * OBJ item model via {@code hbm_m:missile_loader}; texture under {@code textures/models/missile/} (block atlas).
     * BEWLR applies transforms — no {@code display} block (see missiles / range detonator).
     */
    private void objPartItemModel(String itemPath, ResourceLocation objModel, ResourceLocation texture, String... parts) {
        getBuilder(itemPath).customLoader((parent, helper) ->
                new CustomLoaderBuilder<ItemModelBuilder>(
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "missile_loader"), parent, helper) {
                    @Override
                    public JsonObject toJson(JsonObject json) {
                        super.toJson(json);
                        json.addProperty("model", objModel.toString());
                        json.addProperty("flip_v", true);
                        JsonArray partsArray = new JsonArray();
                        for (String part : parts) {
                            partsArray.add(part);
                        }
                        json.add("parts", partsArray);
                        JsonObject textures = new JsonObject();
                        String tex = texture.toString();
                        textures.addProperty("default", tex);
                        textures.addProperty("particle", tex);
                        json.add("textures", textures);
                        return json;
                    }
                });
    }

    private void generateMissileItemModels() {
        for (MissileItemModelDefinitions.Definition definition : MissileItemModelDefinitions.all()) {
            missileItemFromObjModel(definition.itemPath(), definition.hull(), definition.texture());
        }
    }

    /** Item model overrides by {@code hbm_m:tier} (see {@link com.hbm_m.client.ClientSetup}). */
    private void registerRadAbsorberItemModels() {
        ItemModelBuilder builder = withExistingParent("rad_absorber", modLoc("block/rad_absorber_base"));
        for (BlockAbsorber.EnumAbsorberTier tier : BlockAbsorber.EnumAbsorberTier.values()) {
            if (tier == BlockAbsorber.EnumAbsorberTier.BASE) {
                continue;
            }
            builder = builder.override()
                    .model(new ModelFile.UncheckedModelFile(modLoc("block/rad_absorber_" + tier.getSerializedName())))
                    .predicate(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "tier"), tier.ordinal())
                    .end();
        }
    }

}
//?}