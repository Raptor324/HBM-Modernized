package com.hbm_m.item;

import static com.hbm_m.lib.RefStrings.MODID;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.item.ItemModBattery;
import com.hbm_m.armormod.item.ItemModHealth;
// import com.hbm_m.armormod.item.ItemModCladding;
// import com.hbm_m.armormod.item.ItemModExtra;
// import com.hbm_m.armormod.item.ItemModHealth;
// import com.hbm_m.armormod.item.ItemModKevlar;
// import com.hbm_m.armormod.item.ItemModRadProtection;
// import com.hbm_m.armormod.item.ItemModServos;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.crates.CrateType;
import com.hbm_m.blockentity.machines.rbmk.IRBMKFluxReceiver.NType;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.grenades.GrenadeIfType;
import com.hbm_m.entity.grenades.GrenadeType;
import com.hbm_m.item.crates.CrateItem;
import com.hbm_m.item.designator.ItemDesignator;
import com.hbm_m.item.designator.ItemDesignatorManual;
import com.hbm_m.item.designator.ItemDesignatorRange;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.fekal_electric.ModBatteryItem;
import com.hbm_m.item.nuclear.WatzPelletItem;
import com.hbm_m.item.nuclear.WatzPelletType;
import com.hbm_m.item.food.ItemConserve;
import com.hbm_m.item.food.ModFoods;
import com.hbm_m.item.gasmask.ArmorGasMaskItem;
import com.hbm_m.item.gasmask.ArmorGasMaskItem.Variant;
import com.hbm_m.item.gasmask.ItemGasMaskFilter;
import com.hbm_m.item.tools_and_armor.RagMaskItem;
import com.hbm_m.armormod.item.ItemModGasmask;
import com.hbm_m.item.special.ItemCigarette;
import com.hbm_m.item.special.ModConsumables;
import com.hbm_m.item.grenades_and_activators.AirBombItem;
import com.hbm_m.item.grenades_and_activators.AirNukeBombItem;
import com.hbm_m.item.grenades_and_activators.AirstrikeItem;
import com.hbm_m.item.grenades_and_activators.AirstrikeItem.AirstrikeType;
import com.hbm_m.item.grenades_and_activators.DetonatorItem;
import com.hbm_m.item.grenades_and_activators.GrenadeIfItem;
import com.hbm_m.item.grenades_and_activators.GrenadeItem;
import com.hbm_m.item.grenades_and_activators.GrenadeNucItem;
import com.hbm_m.item.grenades_and_activators.MultiDetonatorItem;
import com.hbm_m.item.grenades_and_activators.RangeDetonatorItem;
import com.hbm_m.handler.ability.IToolAreaAbility;
import com.hbm_m.handler.ability.IToolHarvestAbility;
import com.hbm_m.handler.ability.IWeaponAbility;
import com.hbm_m.item.tool.BigSword;
import com.hbm_m.item.tool.HbmToolMaterial;
import com.hbm_m.item.tool.HoeSchrabidium;
import com.hbm_m.item.tool.ItemChainsaw;
import com.hbm_m.item.tool.ItemSwordAbility;
import com.hbm_m.item.tool.ItemSwordAbilityPower;
import com.hbm_m.item.tool.ItemSwordMeteorite;
import com.hbm_m.item.tool.ItemToolAbility;
import com.hbm_m.item.tool.ItemToolAbilityPower;
import com.hbm_m.item.tool.ModHoe;
import com.hbm_m.item.tool.ModSword;
import com.hbm_m.item.tool.RedstoneSword;
import com.hbm_m.item.tool.ConfettiTesterItem;
import com.hbm_m.item.tool.RangefinderItem;
import com.hbm_m.item.industrial.FuelItem;
import com.hbm_m.item.industrial.ItemAssemblyTemplate;
import com.hbm_m.item.industrial.ItemBlades;
import com.hbm_m.item.industrial.ItemBlueprintFolder;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemStamp;
import com.hbm_m.item.industrial.ItemTemplateFolder;
import com.hbm_m.item.industrial.ZirnoxRodItem;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.liquids.FluidDuctItem;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.item.liquids.InfiniteFluidItem;
import com.hbm_m.item.missile.MissileItem;
import com.hbm_m.item.radiation_meter.ItemDigammaDiagnostic;
import com.hbm_m.item.radiation_meter.ItemDosimeter;
import com.hbm_m.item.radiation_meter.ItemGeigerCounter;
import com.hbm_m.item.rbmk.RBMKLidItem;
import com.hbm_m.item.rbmk.RBMKPelletItem;
import com.hbm_m.item.rbmk.RBMKRodItem;
import com.hbm_m.item.scanners.DepthOresScannerItem;
import com.hbm_m.item.scanners.OilDetectorItem;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.item.tools_and_armor.ModArmorMaterialsAccess;
import com.hbm_m.item.tools_and_armor.ScrewdriverItem;
import com.hbm_m.multiblock.DoorBlockItem;
import com.hbm_m.item.machine.ItemRTGPellet;
import com.hbm_m.multiblock.MultiblockBlockItem;
import com.hbm_m.powerarmor.AJRArmor;
import com.hbm_m.powerarmor.AJROArmor;
import com.hbm_m.powerarmor.BismuthArmor;
import com.hbm_m.powerarmor.DNTArmor;
import com.hbm_m.powerarmor.T51Armor;
import com.hbm_m.sound.ModSounds;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import com.hbm_m.platform.PlatformHooks;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import dev.architectury.core.item.ArchitecturySpawnEggItem;


public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(MODID, Registries.ITEM);

    // --- Standalone Pulver ohne Ingot-Gegenstueck (aus Original-Rezepten portiert, DEV-Tab bis einsortiert) ---
    // ===== Ruestungen 1:1 aus ModItemsArmor / ModItems (1.7.10) =====
    public static final RegistrySupplier<Item> GOGGLES = ITEMS.register("goggles", () -> new com.hbm_m.powerarmor.ArmorModel(ModArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> ASHGLASSES = ITEMS.register("ashglasses", () -> new com.hbm_m.powerarmor.ArmorAshGlasses(ModArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> HAT = ITEMS.register("nossy_hat", () -> new com.hbm_m.powerarmor.ArmorHat(ModArmorMaterials.ALLOY, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> NO9 = ITEMS.register("no9", () -> new com.hbm_m.powerarmor.ArmorNo9(ModArmorMaterials.STEEL, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> EUPHEMIUM_HELMET = ITEMS.register("euphemium_helmet", () -> new com.hbm_m.powerarmor.ArmorEuphemium(ModArmorMaterials.EUPHEMIUM, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> EUPHEMIUM_PLATE = ITEMS.register("euphemium_plate", () -> new com.hbm_m.powerarmor.ArmorEuphemium(ModArmorMaterials.EUPHEMIUM, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> EUPHEMIUM_LEGS = ITEMS.register("euphemium_legs", () -> new com.hbm_m.powerarmor.ArmorEuphemium(ModArmorMaterials.EUPHEMIUM, ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> EUPHEMIUM_BOOTS = ITEMS.register("euphemium_boots", () -> new com.hbm_m.powerarmor.ArmorEuphemium(ModArmorMaterials.EUPHEMIUM, ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> SCHRABIDIUM_HELMET = ITEMS.register("schrabidium_helmet", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.SCHRABIDIUM, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/schrabidium_1.png").addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SPEED, 20, 2)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 20, 2)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP, 20, 1)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 20, 2)));
    public static final RegistrySupplier<Item> SCHRABIDIUM_PLATE = ITEMS.register("schrabidium_plate", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.SCHRABIDIUM, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/schrabidium_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) SCHRABIDIUM_HELMET.get()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_LEGS = ITEMS.register("schrabidium_legs", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.SCHRABIDIUM, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/schrabidium_2.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) SCHRABIDIUM_HELMET.get()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_BOOTS = ITEMS.register("schrabidium_boots", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.SCHRABIDIUM, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/schrabidium_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) SCHRABIDIUM_HELMET.get()));
    public static final RegistrySupplier<Item> BISMUTH_HELMET = ITEMS.register("bismuth_helmet", () -> new com.hbm_m.powerarmor.BismuthArmor(ModArmorMaterials.BISMUTH, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png").addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP, 20, 6)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 20, 6)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 20, 1)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 300, 0)).setDashCount(3));
    public static final RegistrySupplier<Item> BISMUTH_PLATE = ITEMS.register("bismuth_plate", () -> new com.hbm_m.powerarmor.BismuthArmor(ModArmorMaterials.BISMUTH, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) BISMUTH_HELMET.get()));
    public static final RegistrySupplier<Item> BISMUTH_LEGS = ITEMS.register("bismuth_legs", () -> new com.hbm_m.powerarmor.BismuthArmor(ModArmorMaterials.BISMUTH, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/starmetal_2.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) BISMUTH_HELMET.get()));
    public static final RegistrySupplier<Item> BISMUTH_BOOTS = ITEMS.register("bismuth_boots", () -> new com.hbm_m.powerarmor.BismuthArmor(ModArmorMaterials.BISMUTH, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) BISMUTH_HELMET.get()));
    public static final RegistrySupplier<Item> TITANIUM_HELMET = ITEMS.register("titanium_helmet", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.TITANIUM, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/titanium_1.png"));
    public static final RegistrySupplier<Item> TITANIUM_PLATE = ITEMS.register("titanium_plate", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.TITANIUM, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/titanium_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) TITANIUM_HELMET.get()));
    public static final RegistrySupplier<Item> TITANIUM_LEGS = ITEMS.register("titanium_legs", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.TITANIUM, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/titanium_2.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) TITANIUM_HELMET.get()));
    public static final RegistrySupplier<Item> TITANIUM_BOOTS = ITEMS.register("titanium_boots", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.TITANIUM, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/titanium_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) TITANIUM_HELMET.get()));
    public static final RegistrySupplier<Item> STEEL_HELMET = ITEMS.register("steel_helmet", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.STEEL, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/steel_1.png"));
    public static final RegistrySupplier<Item> STEEL_PLATE = ITEMS.register("steel_plate", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.STEEL, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/steel_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) STEEL_HELMET.get()));
    public static final RegistrySupplier<Item> STEEL_LEGS = ITEMS.register("steel_legs", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.STEEL, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/steel_2.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) STEEL_HELMET.get()));
    public static final RegistrySupplier<Item> STEEL_BOOTS = ITEMS.register("steel_boots", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.STEEL, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/steel_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) STEEL_HELMET.get()));
    public static final RegistrySupplier<Item> ALLOY_HELMET = ITEMS.register("alloy_helmet", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.ALLOY, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/alloy_1.png"));
    public static final RegistrySupplier<Item> ALLOY_PLATE = ITEMS.register("alloy_plate", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.ALLOY, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/alloy_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) ALLOY_HELMET.get()));
    public static final RegistrySupplier<Item> ALLOY_LEGS = ITEMS.register("alloy_legs", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.ALLOY, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/alloy_2.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) ALLOY_HELMET.get()));
    public static final RegistrySupplier<Item> ALLOY_BOOTS = ITEMS.register("alloy_boots", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.ALLOY, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/alloy_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) ALLOY_HELMET.get()));
    public static final RegistrySupplier<Item> CMB_HELMET = ITEMS.register("cmb_helmet", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.CMB, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/cmb_1.png").addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 20, 2)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SPEED, 20, 2)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 20, 4)));
    public static final RegistrySupplier<Item> CMB_PLATE = ITEMS.register("cmb_plate", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.CMB, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/cmb_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) CMB_HELMET.get()));
    public static final RegistrySupplier<Item> CMB_LEGS = ITEMS.register("cmb_legs", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.CMB, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/cmb_2.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) CMB_HELMET.get()));
    public static final RegistrySupplier<Item> CMB_BOOTS = ITEMS.register("cmb_boots", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.CMB, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/cmb_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) CMB_HELMET.get()));
    public static final RegistrySupplier<Item> PAA_PLATE = ITEMS.register("paa_plate", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.PAA, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/paa_1.png").setNoHelmet(true).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SPEED, 20, 0)));
    public static final RegistrySupplier<Item> PAA_LEGS = ITEMS.register("paa_legs", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.PAA, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/paa_2.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) PAA_PLATE.get()));
    public static final RegistrySupplier<Item> PAA_BOOTS = ITEMS.register("paa_boots", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.PAA, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/paa_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) PAA_PLATE.get()));
    public static final RegistrySupplier<Item> ASBESTOS_HELMET = ITEMS.register("asbestos_helmet", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.ASBESTOS, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/asbestos_1.png").setOverlay("hbm_m:textures/misc/overlay_asbestos.png"));
    public static final RegistrySupplier<Item> ASBESTOS_PLATE = ITEMS.register("asbestos_plate", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.ASBESTOS, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/asbestos_1.png"));
    public static final RegistrySupplier<Item> ASBESTOS_LEGS = ITEMS.register("asbestos_legs", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.ASBESTOS, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/asbestos_2.png"));
    public static final RegistrySupplier<Item> ASBESTOS_BOOTS = ITEMS.register("asbestos_boots", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.ASBESTOS, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/asbestos_1.png"));
    public static final RegistrySupplier<Item> SECURITY_HELMET = ITEMS.register("security_helmet", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.SECURITY, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/security_1.png"));
    public static final RegistrySupplier<Item> SECURITY_PLATE = ITEMS.register("security_plate", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.SECURITY, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/security_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) SECURITY_HELMET.get()));
    public static final RegistrySupplier<Item> SECURITY_LEGS = ITEMS.register("security_legs", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.SECURITY, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/security_2.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) SECURITY_HELMET.get()));
    public static final RegistrySupplier<Item> SECURITY_BOOTS = ITEMS.register("security_boots", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.SECURITY, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/security_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) SECURITY_HELMET.get()));
    public static final RegistrySupplier<Item> COBALT_HELMET = ITEMS.register("cobalt_helmet", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.COBALT, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/cobalt_1.png"));
    public static final RegistrySupplier<Item> COBALT_PLATE = ITEMS.register("cobalt_plate", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.COBALT, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/cobalt_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) COBALT_HELMET.get()));
    public static final RegistrySupplier<Item> COBALT_LEGS = ITEMS.register("cobalt_legs", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.COBALT, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/cobalt_2.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) COBALT_HELMET.get()));
    public static final RegistrySupplier<Item> COBALT_BOOTS = ITEMS.register("cobalt_boots", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.COBALT, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/cobalt_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) COBALT_HELMET.get()));
    public static final RegistrySupplier<Item> STARMETAL_HELMET = ITEMS.register("starmetal_helmet", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.STARMETAL, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png"));
    public static final RegistrySupplier<Item> STARMETAL_PLATE = ITEMS.register("starmetal_plate", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.STARMETAL, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) STARMETAL_HELMET.get()));
    public static final RegistrySupplier<Item> STARMETAL_LEGS = ITEMS.register("starmetal_legs", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.STARMETAL, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/starmetal_2.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) STARMETAL_HELMET.get()));
    public static final RegistrySupplier<Item> STARMETAL_BOOTS = ITEMS.register("starmetal_boots", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.STARMETAL, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) STARMETAL_HELMET.get()));
    public static final RegistrySupplier<Item> ROBES_HELMET = ITEMS.register("robes_helmet", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.CHAIN, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/robes_1.png"));
    public static final RegistrySupplier<Item> ROBES_PLATE = ITEMS.register("robes_plate", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.CHAIN, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/robes_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) ROBES_HELMET.get()));
    public static final RegistrySupplier<Item> ROBES_LEGS = ITEMS.register("robes_legs", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.CHAIN, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/robes_2.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) ROBES_HELMET.get()));
    public static final RegistrySupplier<Item> ROBES_BOOTS = ITEMS.register("robes_boots", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.CHAIN, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/robes_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) ROBES_HELMET.get()));
    public static final RegistrySupplier<Item> ZIRCONIUM_LEGS = ITEMS.register("zirconium_legs", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.ZIRCONIUM, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/zirconium_2.png"));
    public static final RegistrySupplier<Item> DNT_HELMET = ITEMS.register("dnt_helmet", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.DNT, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/dnt_1.png"));
    public static final RegistrySupplier<Item> DNT_PLATE = ITEMS.register("dnt_plate", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.DNT, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/dnt_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) DNT_HELMET.get()));
    public static final RegistrySupplier<Item> DNT_LEGS = ITEMS.register("dnt_legs", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.DNT, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/dnt_2.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) DNT_HELMET.get()));
    public static final RegistrySupplier<Item> DNT_BOOTS = ITEMS.register("dnt_boots", () -> new com.hbm_m.powerarmor.ModArmorFSB(ModArmorMaterials.DNT, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/dnt_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) DNT_HELMET.get()));
    public static final RegistrySupplier<Item> T51_HELMET = ITEMS.register("t51_helmet", () -> new com.hbm_m.powerarmor.T51Armor(ModArmorMaterials.T51, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 1000000L, 10000L, 1000L, 5L).enableVATS(true).setHasGeigerSound(true).setHasHardLanding(true).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 20, 0)).setStep("hbm:step.metal").setJump("hbm:step.iron_jump").setFall("hbm:step.iron_land").hides("hat").setHazardClass(com.hbm_m.util.ArmorUtil.FULL_NO_LIGHT).setRadResist(1D /*90%*/));
    public static final RegistrySupplier<Item> T51_PLATE = ITEMS.register("t51_plate", () -> new com.hbm_m.powerarmor.T51Armor(ModArmorMaterials.T51, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 1000000L, 10000L, 1000L, 5L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) T51_HELMET.get()));
    public static final RegistrySupplier<Item> T51_LEGS = ITEMS.register("t51_legs", () -> new com.hbm_m.powerarmor.T51Armor(ModArmorMaterials.T51, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/starmetal_2.png", 1000000L, 10000L, 1000L, 5L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) T51_HELMET.get()));
    public static final RegistrySupplier<Item> T51_BOOTS = ITEMS.register("t51_boots", () -> new com.hbm_m.powerarmor.T51Armor(ModArmorMaterials.T51, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 1000000L, 10000L, 1000L, 5L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) T51_HELMET.get()));
    public static final RegistrySupplier<Item> STEAMSUIT_HELMET = ITEMS.register("steamsuit_helmet", () -> new com.hbm_m.powerarmor.ArmorDesh(ModArmorMaterials.DESH, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", () -> com.hbm_m.inventory.fluid.ModFluids.STEAM.getSource(), 64_000, 500, 50, 1).setHasHardLanding(true).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SPEED, 20, 4)).hides("hat").setHazardClass(com.hbm_m.util.ArmorUtil.FULL_PACKAGE).setRadResist(1.3D /*95%*/));
    public static final RegistrySupplier<Item> STEAMSUIT_PLATE = ITEMS.register("steamsuit_plate", () -> new com.hbm_m.powerarmor.ArmorDesh(ModArmorMaterials.DESH, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", () -> com.hbm_m.inventory.fluid.ModFluids.STEAM.getSource(), 64_000, 500, 50, 1).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) STEAMSUIT_HELMET.get()));
    public static final RegistrySupplier<Item> STEAMSUIT_LEGS = ITEMS.register("steamsuit_legs", () -> new com.hbm_m.powerarmor.ArmorDesh(ModArmorMaterials.DESH, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/starmetal_2.png", () -> com.hbm_m.inventory.fluid.ModFluids.STEAM.getSource(), 64_000, 500, 50, 1).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) STEAMSUIT_HELMET.get()));
    public static final RegistrySupplier<Item> STEAMSUIT_BOOTS = ITEMS.register("steamsuit_boots", () -> new com.hbm_m.powerarmor.ArmorDesh(ModArmorMaterials.DESH, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", () -> com.hbm_m.inventory.fluid.ModFluids.STEAM.getSource(), 64_000, 500, 50, 1).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) STEAMSUIT_HELMET.get()));
    public static final RegistrySupplier<Item> DIESELSUIT_HELMET = ITEMS.register("dieselsuit_helmet", () -> new com.hbm_m.powerarmor.ArmorDiesel(ModArmorMaterials.DIESEL, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", () -> com.hbm_m.inventory.fluid.ModFluids.DIESEL.getSource(), 64_000, 500, 50, 1).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 20, 2)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP, 20, 2)).enableThermalSight(true).enableVATS(true));
    public static final RegistrySupplier<Item> DIESELSUIT_PLATE = ITEMS.register("dieselsuit_plate", () -> new com.hbm_m.powerarmor.ArmorDiesel(ModArmorMaterials.DIESEL, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", () -> com.hbm_m.inventory.fluid.ModFluids.DIESEL.getSource(), 64_000, 500, 50, 1).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) DIESELSUIT_HELMET.get()));
    public static final RegistrySupplier<Item> DIESELSUIT_LEGS = ITEMS.register("dieselsuit_legs", () -> new com.hbm_m.powerarmor.ArmorDiesel(ModArmorMaterials.DIESEL, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/starmetal_2.png", () -> com.hbm_m.inventory.fluid.ModFluids.DIESEL.getSource(), 64_000, 500, 50, 1).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) DIESELSUIT_HELMET.get()));
    public static final RegistrySupplier<Item> DIESELSUIT_BOOTS = ITEMS.register("dieselsuit_boots", () -> new com.hbm_m.powerarmor.ArmorDiesel(ModArmorMaterials.DIESEL, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", () -> com.hbm_m.inventory.fluid.ModFluids.DIESEL.getSource(), 64_000, 500, 50, 1).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) DIESELSUIT_HELMET.get()));
    public static final RegistrySupplier<Item> AJR_HELMET = ITEMS.register("ajr_helmet", () -> new com.hbm_m.powerarmor.AJRArmor(ModArmorMaterials.AJR, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 2500000L, 10000L, 2000L, 25L).enableVATS(true).setHasGeigerSound(true).setHasHardLanding(true).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP, 20, 0)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 20, 0)).setStep("hbm:step.metal").setJump("hbm:step.iron_jump").setFall("hbm:step.iron_land").hides("hat").setHazardClass(com.hbm_m.util.ArmorUtil.FULL_PACKAGE).setRadResist(1.3D /*95%*/));
    public static final RegistrySupplier<Item> AJR_PLATE = ITEMS.register("ajr_plate", () -> new com.hbm_m.powerarmor.AJRArmor(ModArmorMaterials.AJR, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 2500000L, 10000L, 2000L, 25L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) AJR_HELMET.get()));
    public static final RegistrySupplier<Item> AJR_LEGS = ITEMS.register("ajr_legs", () -> new com.hbm_m.powerarmor.AJRArmor(ModArmorMaterials.AJR, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/starmetal_2.png", 2500000L, 10000L, 2000L, 25L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) AJR_HELMET.get()));
    public static final RegistrySupplier<Item> AJR_BOOTS = ITEMS.register("ajr_boots", () -> new com.hbm_m.powerarmor.AJRArmor(ModArmorMaterials.AJR, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 2500000L, 10000L, 2000L, 25L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) AJR_HELMET.get()));
    public static final RegistrySupplier<Item> AJRO_HELMET = ITEMS.register("ajro_helmet", () -> new com.hbm_m.powerarmor.AJROArmor(ModArmorMaterials.AJR, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 2500000L, 10000L, 2000L, 25L).enableVATS(true).setHasGeigerSound(true).setHasHardLanding(true).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP, 20, 0)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 20, 0)).setStep("hbm:step.metal").setJump("hbm:step.iron_jump").setFall("hbm:step.iron_land").hides("hat").setHazardClass(com.hbm_m.util.ArmorUtil.FULL_PACKAGE).setRadResist(1.3D /*95%*/));
    public static final RegistrySupplier<Item> AJRO_PLATE = ITEMS.register("ajro_plate", () -> new com.hbm_m.powerarmor.AJROArmor(ModArmorMaterials.AJR, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 2500000L, 10000L, 2000L, 25L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) AJRO_HELMET.get()));
    public static final RegistrySupplier<Item> AJRO_LEGS = ITEMS.register("ajro_legs", () -> new com.hbm_m.powerarmor.AJROArmor(ModArmorMaterials.AJR, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/starmetal_2.png", 2500000L, 10000L, 2000L, 25L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) AJRO_HELMET.get()));
    public static final RegistrySupplier<Item> AJRO_BOOTS = ITEMS.register("ajro_boots", () -> new com.hbm_m.powerarmor.AJROArmor(ModArmorMaterials.AJR, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 2500000L, 10000L, 2000L, 25L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) AJRO_HELMET.get()));
    public static final RegistrySupplier<Item> RPA_HELMET = ITEMS.register("rpa_helmet", () -> new com.hbm_m.powerarmor.ArmorRPA(ModArmorMaterials.AJR, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 2500000L, 10000L, 2000L, 25L).enableVATS(true).setHasGeigerSound(true).setHasHardLanding(true).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 20, 3)).setStep("hbm:step.powered").setJump("hbm:step.powered").setFall("hbm:step.powered").hides("hat").setHazardClass(com.hbm_m.util.ArmorUtil.FULL_PACKAGE).setRadResist(2D /*99%*/));
    public static final RegistrySupplier<Item> RPA_PLATE = ITEMS.register("rpa_plate", () -> new com.hbm_m.powerarmor.ArmorRPA(ModArmorMaterials.AJR, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 2500000L, 10000L, 2000L, 25L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) RPA_HELMET.get()));
    public static final RegistrySupplier<Item> RPA_LEGS = ITEMS.register("rpa_legs", () -> new com.hbm_m.powerarmor.ArmorRPA(ModArmorMaterials.AJR, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/starmetal_2.png", 2500000L, 10000L, 2000L, 25L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) RPA_HELMET.get()));
    public static final RegistrySupplier<Item> RPA_BOOTS = ITEMS.register("rpa_boots", () -> new com.hbm_m.powerarmor.ArmorRPA(ModArmorMaterials.AJR, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 2500000L, 10000L, 2000L, 25L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) RPA_HELMET.get()));
    public static final RegistrySupplier<Item> NCRPA_HELMET = ITEMS.register("ncrpa_helmet", () -> new com.hbm_m.powerarmor.ArmorNCRPA(ModArmorMaterials.AJR, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 2500000L, 10000L, 2000L, 25L).enableVATS(true).setHasGeigerSound(true).setHasHardLanding(true).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 20, 3)).setStep("hbm:step.powered").setJump("hbm:step.powered").setFall("hbm:step.powered").hides("hat").setHazardClass(com.hbm_m.util.ArmorUtil.FULL_PACKAGE).setRadResist(1.7D /*97%*/));
    public static final RegistrySupplier<Item> NCRPA_PLATE = ITEMS.register("ncrpa_plate", () -> new com.hbm_m.powerarmor.ArmorNCRPA(ModArmorMaterials.AJR, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 2500000L, 10000L, 2000L, 25L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) NCRPA_HELMET.get()));
    public static final RegistrySupplier<Item> NCRPA_LEGS = ITEMS.register("ncrpa_legs", () -> new com.hbm_m.powerarmor.ArmorNCRPA(ModArmorMaterials.AJR, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/starmetal_2.png", 2500000L, 10000L, 2000L, 25L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) NCRPA_HELMET.get()));
    public static final RegistrySupplier<Item> NCRPA_BOOTS = ITEMS.register("ncrpa_boots", () -> new com.hbm_m.powerarmor.ArmorNCRPA(ModArmorMaterials.AJR, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 2500000L, 10000L, 2000L, 25L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) NCRPA_HELMET.get()));
    public static final RegistrySupplier<Item> BJ_HELMET = ITEMS.register("bj_helmet", () -> new com.hbm_m.powerarmor.ArmorBJ(ModArmorMaterials.BJ, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 10000000L, 10000L, 1000L, 100L).enableVATS(true).enableThermalSight(true).setHasGeigerSound(true).setHasHardLanding(true).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 20, 1)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP, 20, 0)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SATURATION, 20, 0)).addEffect(new net.minecraft.world.effect.MobEffectInstance(com.hbm_m.effect.ModEffects.RADX.get(), 20, 0)).setStep("hbm:step.metal").setJump("hbm:step.iron_jump").setFall("hbm:step.iron_land").setRadResist(1D /*90%%*/));
    public static final RegistrySupplier<Item> BJ_PLATE = ITEMS.register("bj_plate", () -> new com.hbm_m.powerarmor.ArmorBJ(ModArmorMaterials.BJ, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 10000000L, 10000L, 1000L, 100L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) BJ_HELMET.get()));
    public static final RegistrySupplier<Item> BJ_PLATE_JETPACK = ITEMS.register("bj_plate_jetpack", () -> new com.hbm_m.powerarmor.ArmorBJJetpack(ModArmorMaterials.BJ, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 10000000L, 10000L, 1000L, 100L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) BJ_HELMET.get()));
    public static final RegistrySupplier<Item> BJ_LEGS = ITEMS.register("bj_legs", () -> new com.hbm_m.powerarmor.ArmorBJ(ModArmorMaterials.BJ, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/starmetal_2.png", 10000000L, 10000L, 1000L, 100L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) BJ_HELMET.get()));
    public static final RegistrySupplier<Item> BJ_BOOTS = ITEMS.register("bj_boots", () -> new com.hbm_m.powerarmor.ArmorBJ(ModArmorMaterials.BJ, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 10000000L, 10000L, 1000L, 100L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) BJ_HELMET.get()));
    public static final RegistrySupplier<Item> ENVSUIT_HELMET = ITEMS.register("envsuit_helmet", () -> new com.hbm_m.powerarmor.ArmorEnvsuit(ModArmorMaterials.ENV, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 100_000L, 1_000L, 250L, 0L).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 20, 1)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP, 20, 0)).hides("hat").setHazardClass(com.hbm_m.util.ArmorUtil.FULL_PACKAGE).setRadResist(1.0D /*90%*/));
    public static final RegistrySupplier<Item> ENVSUIT_PLATE = ITEMS.register("envsuit_plate", () -> new com.hbm_m.powerarmor.ArmorEnvsuit(ModArmorMaterials.ENV, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 100_000L, 1_000L, 250L, 0L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) ENVSUIT_HELMET.get()));
    public static final RegistrySupplier<Item> ENVSUIT_LEGS = ITEMS.register("envsuit_legs", () -> new com.hbm_m.powerarmor.ArmorEnvsuit(ModArmorMaterials.ENV, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/starmetal_2.png", 100_000L, 1_000L, 250L, 0L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) ENVSUIT_HELMET.get()));
    public static final RegistrySupplier<Item> ENVSUIT_BOOTS = ITEMS.register("envsuit_boots", () -> new com.hbm_m.powerarmor.ArmorEnvsuit(ModArmorMaterials.ENV, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 100_000L, 1_000L, 250L, 0L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) ENVSUIT_HELMET.get()));
    public static final RegistrySupplier<Item> HEV_HELMET = ITEMS.register("hev_helmet", () -> new com.hbm_m.powerarmor.ArmorHEV(ModArmorMaterials.HEV, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 1000000L, 10000L, 2500L, 0L).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 20, 1)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP, 20, 0)).setHasGeigerSound(true).setHasCustomGeiger(true).hides("hat").setHazardClass(com.hbm_m.util.ArmorUtil.FULL_PACKAGE).setRadResist(2.3D /*99.5%*/));
    public static final RegistrySupplier<Item> HEV_PLATE = ITEMS.register("hev_plate", () -> new com.hbm_m.powerarmor.ArmorHEV(ModArmorMaterials.HEV, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 1000000L, 10000L, 2500L, 0L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) HEV_HELMET.get()));
    public static final RegistrySupplier<Item> HEV_LEGS = ITEMS.register("hev_legs", () -> new com.hbm_m.powerarmor.ArmorHEV(ModArmorMaterials.HEV, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/starmetal_2.png", 1000000L, 10000L, 2500L, 0L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) HEV_HELMET.get()));
    public static final RegistrySupplier<Item> HEV_BOOTS = ITEMS.register("hev_boots", () -> new com.hbm_m.powerarmor.ArmorHEV(ModArmorMaterials.HEV, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 1000000L, 10000L, 2500L, 0L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) HEV_HELMET.get()));
    public static final RegistrySupplier<Item> JACKT = ITEMS.register("jackt", () -> new com.hbm_m.powerarmor.ModArmor(ModArmorMaterials.STEEL, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> JACKT2 = ITEMS.register("jackt2", () -> new com.hbm_m.powerarmor.ModArmor(ModArmorMaterials.STEEL, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> FAU_HELMET = ITEMS.register("fau_helmet", () -> new com.hbm_m.powerarmor.ArmorDigamma(ModArmorMaterials.FAU, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 10000000L, 10000L, 2500L, 0L).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP, 20, 1)).setHasGeigerSound(true).enableThermalSight(true).setHasHardLanding(true).setStep("hbm:step.metal").setJump("hbm:step.iron_jump").setFall("hbm:step.iron_land").hides("hat").setHazardClass(com.hbm_m.util.ArmorUtil.FULL_PACKAGE).setRadResist(4D /*99.99%*/));
    public static final RegistrySupplier<Item> FAU_PLATE = ITEMS.register("fau_plate", () -> new com.hbm_m.powerarmor.ArmorDigamma(ModArmorMaterials.FAU, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 10000000L, 10000L, 2500L, 0L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) FAU_HELMET.get()).setFullSetForHide());
    public static final RegistrySupplier<Item> FAU_LEGS = ITEMS.register("fau_legs", () -> new com.hbm_m.powerarmor.ArmorDigamma(ModArmorMaterials.FAU, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/starmetal_2.png", 10000000L, 10000L, 2500L, 0L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) FAU_HELMET.get()).hides("left_leg", "right_leg").setFullSetForHide());
    public static final RegistrySupplier<Item> FAU_BOOTS = ITEMS.register("fau_boots", () -> new com.hbm_m.powerarmor.ArmorDigamma(ModArmorMaterials.FAU, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 10000000L, 10000L, 2500L, 0L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) FAU_HELMET.get()));
    public static final RegistrySupplier<Item> DNS_HELMET = ITEMS.register("dns_helmet", () -> new com.hbm_m.powerarmor.DNTArmor(ModArmorMaterials.DNS, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 1000000000L, 1000000L, 100000L, 115L).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 20, 9)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SPEED, 20, 7)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP, 20, 2)).setHasGeigerSound(true).enableVATS(true).enableThermalSight(true).setHasHardLanding(true).setStep("hbm:step.metal").setJump("hbm:step.iron_jump").setFall("hbm:step.iron_land").hides("hat").setHazardClass(com.hbm_m.util.ArmorUtil.FULL_PACKAGE).setRadResist(5D /*99.999%*/));
    public static final RegistrySupplier<Item> DNS_PLATE = ITEMS.register("dns_plate", () -> new com.hbm_m.powerarmor.DNTArmor(ModArmorMaterials.DNS, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 1000000000L, 1000000L, 100000L, 115L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) DNS_HELMET.get()));
    public static final RegistrySupplier<Item> DNS_LEGS = ITEMS.register("dns_legs", () -> new com.hbm_m.powerarmor.DNTArmor(ModArmorMaterials.DNS, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/starmetal_2.png", 1000000000L, 1000000L, 100000L, 115L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) DNS_HELMET.get()));
    public static final RegistrySupplier<Item> DNS_BOOTS = ITEMS.register("dns_boots", () -> new com.hbm_m.powerarmor.DNTArmor(ModArmorMaterials.DNS, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png", 1000000000L, 1000000L, 100000L, 115L).cloneStats((com.hbm_m.powerarmor.ModArmorFSB) DNS_HELMET.get()));
    public static final RegistrySupplier<Item> TAURUN_HELMET = ITEMS.register("taurun_helmet", () -> new com.hbm_m.powerarmor.ArmorTaurun(ModArmorMaterials.TAURUN, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png").addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 20, 0)).setStepSize(1).hides("hat").setHazardClass(com.hbm_m.util.ArmorUtil.FULL_PACKAGE).setRadResist(0.125D /*25%*/));
    public static final RegistrySupplier<Item> TAURUN_PLATE = ITEMS.register("taurun_plate", () -> new com.hbm_m.powerarmor.ArmorTaurun(ModArmorMaterials.TAURUN, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) TAURUN_HELMET.get()));
    public static final RegistrySupplier<Item> TAURUN_LEGS = ITEMS.register("taurun_legs", () -> new com.hbm_m.powerarmor.ArmorTaurun(ModArmorMaterials.TAURUN, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/starmetal_2.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) TAURUN_HELMET.get()));
    public static final RegistrySupplier<Item> TAURUN_BOOTS = ITEMS.register("taurun_boots", () -> new com.hbm_m.powerarmor.ArmorTaurun(ModArmorMaterials.TAURUN, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) TAURUN_HELMET.get()));
    public static final RegistrySupplier<Item> TRENCHMASTER_HELMET = ITEMS.register("trenchmaster_helmet", () -> new com.hbm_m.powerarmor.ArmorTrenchmaster(ModArmorMaterials.TRENCH, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png").addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 20, 2)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SPEED, 20, 1)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP, 20, 1)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 20, 0)).enableVATS(true).setStepSize(1).hides("hat").setRadResist(1D /*90%*/).setHazardClass(com.hbm_m.util.ArmorUtil.FULL_PACKAGE));
    public static final RegistrySupplier<Item> TRENCHMASTER_PLATE = ITEMS.register("trenchmaster_plate", () -> new com.hbm_m.powerarmor.ArmorTrenchmaster(ModArmorMaterials.TRENCH, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) TRENCHMASTER_HELMET.get()));
    public static final RegistrySupplier<Item> TRENCHMASTER_LEGS = ITEMS.register("trenchmaster_legs", () -> new com.hbm_m.powerarmor.ArmorTrenchmaster(ModArmorMaterials.TRENCH, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/starmetal_2.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) TRENCHMASTER_HELMET.get()));
    public static final RegistrySupplier<Item> TRENCHMASTER_BOOTS = ITEMS.register("trenchmaster_boots", () -> new com.hbm_m.powerarmor.ArmorTrenchmaster(ModArmorMaterials.TRENCH, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/starmetal_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) TRENCHMASTER_HELMET.get()));
    public static final RegistrySupplier<Item> MASK_OF_INFAMY = ITEMS.register("mask_of_infamy", () -> new com.hbm_m.powerarmor.MaskOfInfamy(ModArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> HAZMAT_HELMET = ITEMS.register("hazmat_helmet", () -> new com.hbm_m.powerarmor.ArmorHazmatMask(ModArmorMaterials.HAZMAT, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/hazmat_1.png"));
    public static final RegistrySupplier<Item> HAZMAT_PLATE = ITEMS.register("hazmat_plate", () -> new com.hbm_m.powerarmor.ArmorHazmat(ModArmorMaterials.HAZMAT, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/hazmat_1.png"));
    public static final RegistrySupplier<Item> HAZMAT_LEGS = ITEMS.register("hazmat_legs", () -> new com.hbm_m.powerarmor.ArmorHazmat(ModArmorMaterials.HAZMAT, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/hazmat_2.png"));
    public static final RegistrySupplier<Item> HAZMAT_BOOTS = ITEMS.register("hazmat_boots", () -> new com.hbm_m.powerarmor.ArmorHazmat(ModArmorMaterials.HAZMAT, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/hazmat_1.png"));
    public static final RegistrySupplier<Item> HAZMAT_HELMET_RED = ITEMS.register("hazmat_helmet_red", () -> new com.hbm_m.powerarmor.ArmorHazmatMask(ModArmorMaterials.HAZMAT_RED, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/models/modelhazred.png"));
    public static final RegistrySupplier<Item> HAZMAT_PLATE_RED = ITEMS.register("hazmat_plate_red", () -> new com.hbm_m.powerarmor.ArmorHazmat(ModArmorMaterials.HAZMAT_RED, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/hazmat_1_red.png"));
    public static final RegistrySupplier<Item> HAZMAT_LEGS_RED = ITEMS.register("hazmat_legs_red", () -> new com.hbm_m.powerarmor.ArmorHazmat(ModArmorMaterials.HAZMAT_RED, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/hazmat_2_red.png"));
    public static final RegistrySupplier<Item> HAZMAT_BOOTS_RED = ITEMS.register("hazmat_boots_red", () -> new com.hbm_m.powerarmor.ArmorHazmat(ModArmorMaterials.HAZMAT_RED, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/hazmat_1_red.png"));
    public static final RegistrySupplier<Item> HAZMAT_HELMET_GREY = ITEMS.register("hazmat_helmet_grey", () -> new com.hbm_m.powerarmor.ArmorHazmatMask(ModArmorMaterials.HAZMAT_GREY, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/models/modelhazgrey.png"));
    public static final RegistrySupplier<Item> HAZMAT_PLATE_GREY = ITEMS.register("hazmat_plate_grey", () -> new com.hbm_m.powerarmor.ArmorHazmat(ModArmorMaterials.HAZMAT_GREY, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/hazmat_1_grey.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) HAZMAT_HELMET_GREY.get()));
    public static final RegistrySupplier<Item> HAZMAT_LEGS_GREY = ITEMS.register("hazmat_legs_grey", () -> new com.hbm_m.powerarmor.ArmorHazmat(ModArmorMaterials.HAZMAT_GREY, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/hazmat_2_grey.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) HAZMAT_HELMET_GREY.get()));
    public static final RegistrySupplier<Item> HAZMAT_BOOTS_GREY = ITEMS.register("hazmat_boots_grey", () -> new com.hbm_m.powerarmor.ArmorHazmat(ModArmorMaterials.HAZMAT_GREY, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/hazmat_1_grey.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) HAZMAT_HELMET_GREY.get()));
    public static final RegistrySupplier<Item> HAZMAT_PAA_HELMET = ITEMS.register("hazmat_paa_helmet", () -> new com.hbm_m.powerarmor.ArmorHazmatMask(ModArmorMaterials.PAA, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/hazmat_paa_1.png"));
    public static final RegistrySupplier<Item> HAZMAT_PAA_PLATE = ITEMS.register("hazmat_paa_plate", () -> new com.hbm_m.powerarmor.ArmorHazmat(ModArmorMaterials.PAA, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/hazmat_paa_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) HAZMAT_PAA_HELMET.get()));
    public static final RegistrySupplier<Item> HAZMAT_PAA_LEGS = ITEMS.register("hazmat_paa_legs", () -> new com.hbm_m.powerarmor.ArmorHazmat(ModArmorMaterials.PAA, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/hazmat_paa_2.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) HAZMAT_PAA_HELMET.get()));
    public static final RegistrySupplier<Item> HAZMAT_PAA_BOOTS = ITEMS.register("hazmat_paa_boots", () -> new com.hbm_m.powerarmor.ArmorHazmat(ModArmorMaterials.PAA, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/hazmat_paa_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) HAZMAT_PAA_HELMET.get()));
    public static final RegistrySupplier<Item> LIQUIDATOR_HELMET = ITEMS.register("liquidator_helmet", () -> new com.hbm_m.powerarmor.ArmorLiquidatorMask(ModArmorMaterials.LIQUIDATOR, ArmorItem.Type.HELMET, new Item.Properties(), "hbm_m:textures/armor/liquidator_helmet.png").setStep("hbm:step.metal").setJump("hbm:step.iron_jump").setFall("hbm:step.iron_land"));
    public static final RegistrySupplier<Item> LIQUIDATOR_PLATE = ITEMS.register("liquidator_plate", () -> new com.hbm_m.powerarmor.ArmorLiquidator(ModArmorMaterials.LIQUIDATOR, ArmorItem.Type.CHESTPLATE, new Item.Properties(), "hbm_m:textures/armor/liquidator_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) LIQUIDATOR_HELMET.get()));
    public static final RegistrySupplier<Item> LIQUIDATOR_LEGS = ITEMS.register("liquidator_legs", () -> new com.hbm_m.powerarmor.ArmorLiquidator(ModArmorMaterials.LIQUIDATOR, ArmorItem.Type.LEGGINGS, new Item.Properties(), "hbm_m:textures/armor/liquidator_2.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) LIQUIDATOR_HELMET.get()));
    public static final RegistrySupplier<Item> LIQUIDATOR_BOOTS = ITEMS.register("liquidator_boots", () -> new com.hbm_m.powerarmor.ArmorLiquidator(ModArmorMaterials.LIQUIDATOR, ArmorItem.Type.BOOTS, new Item.Properties(), "hbm_m:textures/armor/liquidator_1.png").cloneStats((com.hbm_m.powerarmor.ModArmorFSB) LIQUIDATOR_HELMET.get()));
    public static final RegistrySupplier<Item> CAPE_RADIATION = ITEMS.register("cape_radiation", () -> new com.hbm_m.powerarmor.ArmorModel(ModArmorMaterials.CHAIN, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> CAPE_GASMASK = ITEMS.register("cape_gasmask", () -> new com.hbm_m.powerarmor.ArmorModel(ModArmorMaterials.CHAIN, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> CAPE_SCHRABIDIUM = ITEMS.register("cape_schrabidium", () -> new com.hbm_m.powerarmor.ArmorModel(ModArmorMaterials.SCHRABIDIUM, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> CAPE_HIDDEN = ITEMS.register("cape_hidden", () -> new com.hbm_m.powerarmor.ArmorModel(ModArmorMaterials.CHAIN, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> POWDER_SAWDUST      = ITEMS.register("sawdust_powder",      () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_YELLOWCAKE   = ITEMS.register("yellowcake_powder",   () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_BALEFIRE     = ITEMS.register("balefire_powder",     () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_THERMITE     = ITEMS.register("thermite_powder",     () -> new Item(new Item.Properties()));
    // Энерго-порошок (ориг. 4383 powder_power, ItemCustomLore uncommon, текстура powder_energy_alt).
    public static final RegistrySupplier<Item> POWDER_POWER        = ITEMS.register("powder_power",        () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistrySupplier<Item> POWDER_FERTILIZER   = ITEMS.register("fertilizer_powder",   () -> new com.hbm_m.item.tool.ItemFertilizer(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_FLUX         = ITEMS.register("flux_powder",         () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_MAGIC        = ITEMS.register("magic_powder",        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_ICE          = ITEMS.register("ice_powder",          () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_SPARK_MIX    = ITEMS.register("spark_mix_powder",    () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_SEMTEX_MIX   = ITEMS.register("semtex_mix_powder",   () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_DESH_READY   = ITEMS.register("desh_ready_powder",   () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_COLTAN       = ITEMS.register("coltan_powder",       () -> new Item(new Item.Properties()));

    static {
        // Единый реестр материалов: слитки, порошки, tiny-порошки, наггетсы, биллеты,
        // кристаллы, пластины, провода, лом — по (материал, форма) из ModMaterials.
        // Блоки хранения регистрирует ModBlocks (форма BLOCK).
        ModMaterialItems.registerAll();
        // Развёртка мета-предметов оригинала (dye/crayon/casing/part_*/waste/nuclear_waste_*):
        // по одному предмету на мету — для 1:1 слотов вкладки Parts.
        PartTabMetaItems.registerAll();
        com.hbm_m.item.missile.MissilePartItems.registerAll();
        com.hbm_m.item.weapon.sedna.WeaponItems.registerAll();
        com.hbm_m.item.weapon.grenade.GrenadeItems.registerAll();
    }

    // РЕЕСТР МАТЕРИАЛОВ: доступ через ModMaterialItems.get(ModMaterials.X, MaterialShape.Y)
    
    public static final int SLOT_HELMET = 0;
    public static final int SLOT_CHEST = 1;
    public static final int SLOT_LEGS = 2;
    public static final int SLOT_BOOTS = 3;
    public static final int SLOT_BATTERY = 8;  //  ArmorModificationHelper.battery
    public static final int SLOT_SPECIAL = 7;  //  ArmorModificationHelper.extra
    public static final int SLOT_INSERT = 6;
    public static final int SLOT_CLADDING = 5; //  ArmorModificationHelper.cladding
    public static final int SLOT_SERVOS = 4;   //  ArmorModificationHelper.servos

    public static final int SLOT_HELMET_ONLY = 0;
    public static final int SLOT_PLATE_ONLY = 1;
    public static final int SLOT_LEGS_ONLY = 2;
    public static final int SLOT_BOOTS_ONLY = 3;
    public static final int SLOT_KEVLAR = 6;

    public static final int BATTERY_CAPACITY = 1_000_000;

    public static final RegistrySupplier<Item> STRAWBERRY = ITEMS.register("strawberry",
            () -> new Item(new Item.Properties().food(ModFoods.STRAWBERRY)));
    public static final RegistrySupplier<Item> CANNED_ASBESTOS = ITEMS.register("canned_asbestos",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.ASBESTOS));
    public static final RegistrySupplier<Item> CANNED_ASS = ITEMS.register("canned_ass",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.ASS));
    public static final RegistrySupplier<Item> CANNED_BARK = ITEMS.register("canned_bark",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.BARK));
    public static final RegistrySupplier<Item> CANNED_BEEF = ITEMS.register("canned_beef",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.BEEF));
    public static final RegistrySupplier<Item> CANNED_BHOLE = ITEMS.register("canned_bhole",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.BHOLE));
    public static final RegistrySupplier<Item> CANNED_CHEESE = ITEMS.register("canned_cheese",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.CHEESE));
    public static final RegistrySupplier<Item> CANNED_CHINESE = ITEMS.register("canned_chinese",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.CHINESE));
    public static final RegistrySupplier<Item> CANNED_DIESEL = ITEMS.register("canned_diesel",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.DIESEL));
    public static final RegistrySupplier<Item> CANNED_FIST = ITEMS.register("canned_fist",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.FIST));
    public static final RegistrySupplier<Item> CANNED_FRIED = ITEMS.register("canned_fried",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.FRIED));
    public static final RegistrySupplier<Item> CANNED_HOTDOGS = ITEMS.register("canned_hotdogs",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.HOTDOGS));
    public static final RegistrySupplier<Item> CANNED_KEROSENE = ITEMS.register("canned_kerosene",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.KEROSENE));
    public static final RegistrySupplier<Item> CANNED_LEFTOVERS = ITEMS.register("canned_leftovers",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.LEFTOVERS));
    public static final RegistrySupplier<Item> CANNED_MILK = ITEMS.register("canned_milk",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.MILK));
    public static final RegistrySupplier<Item> CANNED_MYSTERY = ITEMS.register("canned_mystery",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.MYSTERY));
    public static final RegistrySupplier<Item> CANNED_NAPALM = ITEMS.register("canned_napalm",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.NAPALM));
    public static final RegistrySupplier<Item> CANNED_OIL = ITEMS.register("canned_oil",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.OIL));
    public static final RegistrySupplier<Item> CANNED_PASHTET = ITEMS.register("canned_pashtet",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.PASHTET));
    public static final RegistrySupplier<Item> CANNED_PIZZA = ITEMS.register("canned_pizza",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.PIZZA));
    public static final RegistrySupplier<Item> CANNED_RECURSION = ITEMS.register("canned_recursion",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.RECURSION));
    public static final RegistrySupplier<Item> CANNED_SPAM = ITEMS.register("canned_spam",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.SPAM));
    public static final RegistrySupplier<Item> CANNED_STEW = ITEMS.register("canned_stew",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.STEW));
    public static final RegistrySupplier<Item> CANNED_TOMATO = ITEMS.register("canned_tomato",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.TOMATO));
    public static final RegistrySupplier<Item> CANNED_TUNA = ITEMS.register("canned_tuna",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.TUNA));
    public static final RegistrySupplier<Item> CANNED_TUBE = ITEMS.register("canned_tube",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.TUBE));
    public static final RegistrySupplier<Item> CANNED_YOGURT = ITEMS.register("canned_yogurt",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.YOGURT));


    public static final RegistrySupplier<Item> CAN_BEPIS = ITEMS.register("can_bepis",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()).makeCan());
    public static final RegistrySupplier<Item> CAN_BREEN = ITEMS.register("can_breen",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()).makeCan());
    public static final RegistrySupplier<Item> CAN_CREATURE = ITEMS.register("can_creature",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()).makeCan());
    public static final RegistrySupplier<Item> CAN_EMPTY = ITEMS.register("can_empty",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAN_LUNA = ITEMS.register("can_luna",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()).makeCan());
    public static final RegistrySupplier<Item> CAN_MRSUGAR = ITEMS.register("can_mrsugar",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()).makeCan());
    public static final RegistrySupplier<Item> CAN_MUG = ITEMS.register("can_mug",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()).makeCan());
    public static final RegistrySupplier<Item> CAN_OVERCHARGE = ITEMS.register("can_overcharge",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()).makeCan());
    public static final RegistrySupplier<Item> CAN_REDBOMB = ITEMS.register("can_redbomb",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()).makeCan());
    public static final RegistrySupplier<Item> CAN_SMART = ITEMS.register("can_smart",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()).makeCan());

    public static final RegistrySupplier<Item> STARMETAL_SWORD = ITEMS.register("starmetal_sword",
            () -> new ItemSwordAbility(25F, 0, HbmToolMaterial.STARMETAL)
                    .addAbility(IWeaponAbility.BEHEADER, 0)
                    .addAbility(IWeaponAbility.STUN, 1)
                    .addAbility(IWeaponAbility.BOBBLE, 0));
    public static final RegistrySupplier<Item> STARMETAL_AXE = ITEMS.register("starmetal_axe",
            () -> new ItemToolAbility(12F, 0, HbmToolMaterial.STARMETAL, ItemToolAbility.EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.RECURSION, 3)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 4)
                    .addAbility(IWeaponAbility.BEHEADER, 0)
                    .addAbility(IWeaponAbility.STUN, 1));
    public static final RegistrySupplier<Item> STARMETAL_PICKAXE = ITEMS.register("starmetal_pickaxe",
            () -> new ItemToolAbility(8F, 0, HbmToolMaterial.STARMETAL, ItemToolAbility.EnumToolType.PICKAXE)
                    .addAbility(IToolAreaAbility.RECURSION, 3)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 4)
                    .addAbility(IWeaponAbility.STUN, 1));
    public static final RegistrySupplier<Item> STARMETAL_SHOVEL = ITEMS.register("starmetal_shovel",
            () -> new ItemToolAbility(7F, 0, HbmToolMaterial.STARMETAL, ItemToolAbility.EnumToolType.SHOVEL)
                    .addAbility(IToolAreaAbility.RECURSION, 3)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 4)
                    .addAbility(IWeaponAbility.STUN, 1));
    public static final RegistrySupplier<Item> STARMETAL_HOE = ITEMS.register("starmetal_hoe",
            () -> new ModHoe(HbmToolMaterial.STARMETAL));

    public static final RegistrySupplier<Item> ALLOY_SWORD = ITEMS.register("alloy_sword",
            () -> new ItemSwordAbility(8F, 0, HbmToolMaterial.ALLOY)
                    .addAbility(IWeaponAbility.STUN, 0));

    public static final RegistrySupplier<Item> ALLOY_AXE = ITEMS.register("alloy_axe",
            () -> new ItemToolAbility(7F, 0, HbmToolMaterial.ALLOY, ItemToolAbility.EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.RECURSION, 0)
                    .addAbility(IWeaponAbility.BEHEADER, 0));

    public static final RegistrySupplier<Item> ALLOY_PICKAXE = ITEMS.register("alloy_pickaxe",
            () -> new ItemToolAbility(5F, 0, HbmToolMaterial.ALLOY, ItemToolAbility.EnumToolType.PICKAXE)
                    .addAbility(IToolAreaAbility.RECURSION, 0));

    public static final RegistrySupplier<Item> ALLOY_SHOVEL = ITEMS.register("alloy_shovel",
            () -> new ItemToolAbility(4F, 0, HbmToolMaterial.ALLOY, ItemToolAbility.EnumToolType.SHOVEL)
                    .addAbility(IToolAreaAbility.RECURSION, 0));

    public static final RegistrySupplier<Item> ALLOY_HOE = ITEMS.register("alloy_hoe",
            () -> new ModHoe(HbmToolMaterial.ALLOY));

    public static final RegistrySupplier<Item> STEEL_SWORD = ITEMS.register("steel_sword",
            () -> new ItemSwordAbility(6F, 0, HbmToolMaterial.STEEL)
                    .addAbility(IWeaponAbility.STUN, 0));
    public static final RegistrySupplier<Item> STEEL_AXE = ITEMS.register("steel_axe",
            () -> new ItemToolAbility(5F, 0, HbmToolMaterial.STEEL, ItemToolAbility.EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.RECURSION, 0)
                    .addAbility(IWeaponAbility.BEHEADER, 0));
    public static final RegistrySupplier<Item> STEEL_PICKAXE = ITEMS.register("steel_pickaxe",
            () -> new ItemToolAbility(4F, 0, HbmToolMaterial.STEEL, ItemToolAbility.EnumToolType.PICKAXE)
                    .addAbility(IToolAreaAbility.RECURSION, 0));
    public static final RegistrySupplier<Item> STEEL_SHOVEL = ITEMS.register("steel_shovel",
            () -> new ItemToolAbility(3F, 0, HbmToolMaterial.STEEL, ItemToolAbility.EnumToolType.SHOVEL)
                    .addAbility(IToolAreaAbility.RECURSION, 0));
    public static final RegistrySupplier<Item> STEEL_HOE = ITEMS.register("steel_hoe",
            () -> new ModHoe(HbmToolMaterial.STEEL));

    public static final RegistrySupplier<Item> TITANIUM_SWORD = ITEMS.register("titanium_sword",
            () -> new ItemSwordAbility(6.5F, 0, HbmToolMaterial.TITAN));
    public static final RegistrySupplier<Item> TITANIUM_AXE = ITEMS.register("titanium_axe",
            () -> new ItemToolAbility(5.5F, 0, HbmToolMaterial.TITAN, ItemToolAbility.EnumToolType.AXE)
                    .addAbility(IWeaponAbility.BEHEADER, 0));

    // Meteorite swords (registered so recipes can produce them)
    public static final RegistrySupplier<Item> METEORITE_SWORD = ITEMS.register("meteorite_sword",
            () -> new ItemSwordMeteorite(9F, 0, HbmToolMaterial.METEORITE));
    public static final RegistrySupplier<Item> METEORITE_SWORD_SEARED = ITEMS.register("meteorite_sword_seared",
            () -> new ItemSwordMeteorite(10F, 0, HbmToolMaterial.METEORITE));
    // Original chain continues: seared -> reforged -> hardened -> alloyed -> machined -> treated -> etched -> bred -> ...
    // Only hardened/alloyed are added here (needed by the Blast Furnace recipe below); the rest of the chain
    // (Press/Crystallizer/Breeder steps) is tracked separately and not yet wired up.
    public static final RegistrySupplier<Item> METEORITE_SWORD_HARDENED = ITEMS.register("meteorite_sword_hardened",
            () -> new ItemSwordMeteorite(15F, 0, HbmToolMaterial.METEORITE));
    public static final RegistrySupplier<Item> METEORITE_SWORD_ALLOYED = ITEMS.register("meteorite_sword_alloyed",
            () -> new ItemSwordMeteorite(17.5F, 0, HbmToolMaterial.METEORITE));
    // Fusions-Brueter-Schritt der Schwertkette (Original: meteorite_sword_irradiated -> _fused,
    // siehe TileEntityFusionBreeder.processSolid).
    public static final RegistrySupplier<Item> METEORITE_SWORD_IRRADIATED = ITEMS.register("meteorite_sword_irradiated",
            () -> new ItemSwordMeteorite(35F, 0, HbmToolMaterial.METEORITE));
    public static final RegistrySupplier<Item> METEORITE_SWORD_FUSED = ITEMS.register("meteorite_sword_fused",
            () -> new ItemSwordMeteorite(50F, 0, HbmToolMaterial.METEORITE));
    public static final RegistrySupplier<Item> TITANIUM_PICKAXE = ITEMS.register("titanium_pickaxe",
            () -> new ItemToolAbility(4.5F, 0, HbmToolMaterial.TITAN, ItemToolAbility.EnumToolType.PICKAXE));
    public static final RegistrySupplier<Item> DRILL_TITANIUM = ITEMS.register("drill_titanium",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TITANIUM_SHOVEL = ITEMS.register("titanium_shovel",
            () -> new ItemToolAbility(3.5F, 0, HbmToolMaterial.TITAN, ItemToolAbility.EnumToolType.SHOVEL));
    public static final RegistrySupplier<Item> TITANIUM_HOE = ITEMS.register("titanium_hoe",
            () -> new ModHoe(HbmToolMaterial.TITAN));


    public static final RegistrySupplier<Item> GRENADE = ITEMS.register("grenade",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.STANDARD, ModEntities.GRENADE_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADEHE = ITEMS.register("grenadehe",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.HE, ModEntities.GRENADEHE_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADEFIRE = ITEMS.register("grenadefire",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.FIRE, ModEntities.GRENADEFIRE_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADESLIME = ITEMS.register("grenadeslime",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.SLIME, ModEntities.GRENADESLIME_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADESMART = ITEMS.register("grenadesmart",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.SMART, ModEntities.GRENADESMART_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADE_IF = ITEMS.register("grenade_if",
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF, ModEntities.GRENADE_IF_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADE_IF_HE = ITEMS.register("grenade_if_he",
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF_HE, ModEntities.GRENADE_IF_HE_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADE_IF_SLIME = ITEMS.register("grenade_if_slime",
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF_SLIME, ModEntities.GRENADE_IF_SLIME_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADE_IF_FIRE = ITEMS.register("grenade_if_fire",
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF_FIRE, ModEntities.GRENADE_IF_FIRE_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADE_NUC = ITEMS.register("grenade_nuc",
            () -> new GrenadeNucItem(new Item.Properties(), ModEntities.GRENADE_NUC_PROJECTILE));

    public static final RegistrySupplier<Item> AIRBOMB_A = ITEMS.register("airbomb_a",
            () -> new AirBombItem(new Item.Properties(), ModEntities.AIRBOMB_PROJECTILE));
    public static final RegistrySupplier<Item> AIRNUKEBOMB_A = ITEMS.register("airnukebomb_a",
            () -> new AirNukeBombItem(new Item.Properties(), ModEntities.AIRNUKEBOMB_PROJECTILE));
    public static final RegistrySupplier<Item> BOT_PRIME_SPAWN_EGG = ITEMS.register("bot_prime_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.BOT_PRIME_HEAD, 0x3a3f45, 0xc03020, new Item.Properties()));
    public static final RegistrySupplier<Item> UFO_SPAWN_EGG = ITEMS.register("ufo_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.UFO, 0x505a64, 0x30ff90, new Item.Properties()));
    public static final RegistrySupplier<Item> RAD_BEAST_SPAWN_EGG = ITEMS.register("rad_beast_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.RAD_BEAST, 0x303030, 0x008000, new Item.Properties()));
    public static final RegistrySupplier<Item> HUNTER_CHOPPER_SPAWN_EGG = ITEMS.register("hunter_chopper_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.HUNTER_CHOPPER, 0x000020, 0x2D2D72, new Item.Properties()));
    public static final RegistrySupplier<Item> CYBER_CRAB_SPAWN_EGG = ITEMS.register("cyber_crab_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.CYBER_CRAB, 0xAAAAAA, 0x444444, new Item.Properties()));
    public static final RegistrySupplier<Item> TESLA_CRAB_SPAWN_EGG = ITEMS.register("tesla_crab_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.TESLA_CRAB, 0xAAAAAA, 0x440000, new Item.Properties()));
    public static final RegistrySupplier<Item> TAINT_CRAB_SPAWN_EGG = ITEMS.register("taint_crab_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.TAINT_CRAB, 0xAAAAAA, 0xFF00FF, new Item.Properties()));
    public static final RegistrySupplier<Item> QUACKOS_SPAWN_EGG = ITEMS.register("quackos_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.QUACKOS, 0xd0d0d0, 0xFFBF00, new Item.Properties()));
    public static final RegistrySupplier<Item> PIGEON_SPAWN_EGG = ITEMS.register("pigeon_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.PIGEON, 0xC8C9CD, 0x858894, new Item.Properties()));
    /** Original addMob(EntityUndeadSoldier, 0x749F30, 0x6C5B44). */
    public static final RegistrySupplier<Item> UNDEAD_SOLDIER_SPAWN_EGG = ITEMS.register("undead_soldier_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.UNDEAD_SOLDIER, 0x749F30, 0x6C5B44, new Item.Properties()));
    public static final RegistrySupplier<Item> PLASTIC_BAG_SPAWN_EGG = ITEMS.register("plastic_bag_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.PLASTIC_BAG, 0xd0d0d0, 0x808080, new Item.Properties()));
    public static final RegistrySupplier<Item> TEST_DUMMY_SPAWN_EGG = ITEMS.register("test_dummy_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.TEST_DUMMY, 0xffffff, 0x000000, new Item.Properties()));
    public static final RegistrySupplier<Item> FBI_SPAWN_EGG = ITEMS.register("fbi_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.FBI, 0x008000, 0x404040, new Item.Properties()));
    public static final RegistrySupplier<Item> FBI_DRONE_SPAWN_EGG = ITEMS.register("fbi_drone_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.FBI_DRONE, 0x008000, 0x404040, new Item.Properties()));
    public static final RegistrySupplier<Item> DUCK_SPAWN_EGG = ITEMS.register("duck_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.DUCK, 0xd0d0d0, 0xFFBF00, new Item.Properties()));
    public static final RegistrySupplier<Item> GLYPHID_SPAWN_EGG = ITEMS.register("glyphid_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.GLYPHID, 0x724A21, 0xD2BB72, new Item.Properties()));
    public static final RegistrySupplier<Item> GLYPHID_BRAWLER_SPAWN_EGG = ITEMS.register("glyphid_brawler_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.GLYPHID_BRAWLER, 0x273038, 0xD2BB72, new Item.Properties()));
    public static final RegistrySupplier<Item> GLYPHID_BEHEMOTH_SPAWN_EGG = ITEMS.register("glyphid_behemoth_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.GLYPHID_BEHEMOTH, 0x267F00, 0xD2BB72, new Item.Properties()));
    public static final RegistrySupplier<Item> GLYPHID_BRENDA_SPAWN_EGG = ITEMS.register("glyphid_brenda_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.GLYPHID_BRENDA, 0x4FC0C0, 0xA0A0A0, new Item.Properties()));
    public static final RegistrySupplier<Item> GLYPHID_BOMBARDIER_SPAWN_EGG = ITEMS.register("glyphid_bombardier_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.GLYPHID_BOMBARDIER, 0xDDD919, 0xDBB79D, new Item.Properties()));
    public static final RegistrySupplier<Item> GLYPHID_BLASTER_SPAWN_EGG = ITEMS.register("glyphid_blaster_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.GLYPHID_BLASTER, 0xD83737, 0xDBB79D, new Item.Properties()));
    public static final RegistrySupplier<Item> GLYPHID_SCOUT_SPAWN_EGG = ITEMS.register("glyphid_scout_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.GLYPHID_SCOUT, 0x273038, 0xB9E36B, new Item.Properties()));
    public static final RegistrySupplier<Item> GLYPHID_NUCLEAR_SPAWN_EGG = ITEMS.register("glyphid_nuclear_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.GLYPHID_NUCLEAR, 0x267F00, 0xA0A0A0, new Item.Properties()));
    public static final RegistrySupplier<Item> GLYPHID_DIGGER_SPAWN_EGG = ITEMS.register("glyphid_digger_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.GLYPHID_DIGGER, 0x273038, 0x724A21, new Item.Properties()));
    public static final RegistrySupplier<Item> PARASITE_MAGGOT_SPAWN_EGG = ITEMS.register("parasite_maggot_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.PARASITE_MAGGOT, 0xD0D0D0, 0x808080, new Item.Properties()));
    public static final RegistrySupplier<Item> CRYSTAL_HORN = ITEMS.register("crystal_horn", () -> new com.hbm_m.item.ItemCustomLore(new Item.Properties()));
    public static final RegistrySupplier<Item> CRYSTAL_CHARRED = ITEMS.register("crystal_charred", () -> new com.hbm_m.item.ItemCustomLore(new Item.Properties()));
    public static final RegistrySupplier<Item> MASKMAN_SPAWN_EGG = ITEMS.register("maskman_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.MASKMAN, 0x818572, 0xC7C1B7, new Item.Properties()));

    public static final RegistrySupplier<Item> NOLO_SPAWN_EGG = ITEMS.register("nolo_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.NOLO, 0x8b5e3c, 0xf0d8b0, new Item.Properties()));

    public static final RegistrySupplier<Item> ENTITY_MOB_TAINTED_CREEPER_SPAWN_EGG = ITEMS.register(
            "entity_mob_tainted_creeper_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.ENTITY_MOB_TAINTED_CREEPER, 0x813b9b, 0xd71fdd, new Item.Properties()));

    public static final RegistrySupplier<Item> ENTITY_MOB_VOLATILE_CREEPER_SPAWN_EGG = ITEMS.register(
            "entity_mob_volatile_creeper_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.ENTITY_MOB_VOLATILE_CREEPER, 0xC28153, 0x4D382C, new Item.Properties()));

    public static final RegistrySupplier<Item> ENTITY_MOB_PHOSGENE_CREEPER_SPAWN_EGG = ITEMS.register(
            "entity_mob_phosgene_creeper_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.ENTITY_MOB_PHOSGENE_CREEPER, 0xE3D398, 0xB8A06B, new Item.Properties()));

    public static final RegistrySupplier<Item> ENTITY_MOB_GOLD_CREEPER_SPAWN_EGG = ITEMS.register(
            "entity_mob_gold_creeper_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.ENTITY_MOB_GOLD_CREEPER, 0xECC136, 0x9E8B3E, new Item.Properties()));

    public static final RegistrySupplier<Item> ENTITY_MOB_NUCLEAR_CREEPER_SPAWN_EGG = ITEMS.register(
            "entity_mob_nuclear_creeper_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.ENTITY_MOB_NUCLEAR_CREEPER, 0x204131, 0x75CE00, new Item.Properties()));

    // Р‘Р РћРќРЇ Р“РћР РќРЇРљРђ:











    //-----------------------POWER ARMOR ----------------------------------











			









    public static final RegistrySupplier<Item> GEIGER_COUNTER = ITEMS.register("geiger_counter",
            () -> new ItemGeigerCounter(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> DOSIMETER = ITEMS.register("dosimeter",
            () -> new ItemDosimeter(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> DIGAMMA_DIAGNOSTIC = ITEMS.register("digamma_diagnostic",
            () -> new ItemDigammaDiagnostic(new Item.Properties().stacksTo(1)));

     public static final RegistrySupplier<Item> MUSIC_DISC_BUNKER = ITEMS.register("music_disc_bunker",
            () -> PlatformHooks.createRecordItem(
                    1,
                    ModSounds.MUSIC_DISC_BUNKER.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                    20 * 120
            ));

    public static final RegistrySupplier<Item> MUSIC_DISC_GLASS = ITEMS.register("music_disc_glass",
            () -> com.hbm_m.platform.PlatformHooks.createRecordItem(
                    2,
                    ModSounds.MUSIC_DISC_GLASS.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                    1245
            ));

    /**
     * Schweizerpsalm - the Swiss national anthem. Length is the source file's 1:24 rounded up; the
     * comparator value continues the mod's own sequence (bunker 1, glass 2).
     */
    public static final RegistrySupplier<Item> MUSIC_DISC_CH = ITEMS.register("music_disc_ch",
            () -> new FlavouredRecordItem(
                    3,
                    ModSounds.MUSIC_DISC_CH.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                    20 * 85,
                    "item.hbm_m.music_disc_ch.flavour"
            ));




    public static final RegistrySupplier<Item> CRATE_IRON = ITEMS.register("crate_iron",
            () -> new CrateItem(ModBlocks.CRATE_IRON.get(), new Item.Properties(), CrateType.IRON.getSlotCount()));
    public static final RegistrySupplier<Item> CRATE_STEEL = ITEMS.register("crate_steel",
            () -> new CrateItem(ModBlocks.CRATE_STEEL.get(), new Item.Properties(), CrateType.STEEL.getSlotCount()));
    public static final RegistrySupplier<Item> CRATE_DESH = ITEMS.register("crate_desh",
            () -> new CrateItem(ModBlocks.CRATE_DESH.get(), new Item.Properties(), CrateType.DESH.getSlotCount()));
    public static final RegistrySupplier<Item> SAFE = ITEMS.register("safe",
            () -> new CrateItem(ModBlocks.SAFE.get(), new Item.Properties(), CrateType.SAFE.getSlotCount()));
    public static final RegistrySupplier<Item> CRATE_TUNGSTEN = ITEMS.register("crate_tungsten",
            () -> new CrateItem(ModBlocks.CRATE_TUNGSTEN.get(), new Item.Properties(), CrateType.TUNGSTEN.getSlotCount()));
    public static final RegistrySupplier<Item> CRATE_TEMPLATE = ITEMS.register("crate_template",
            () -> new CrateItem(ModBlocks.CRATE_TEMPLATE.get(), new Item.Properties(), CrateType.TEMPLATE.getSlotCount()));

    public static final RegistrySupplier<Item> HEART_PIECE = ITEMS.register("heart_piece", () -> new com.hbm_m.armormod.item.ItemModHealth(5F));
    public static final RegistrySupplier<Item> HEART_CONTAINER = ITEMS.register("heart_container", () -> new com.hbm_m.armormod.item.ItemModHealth(20F));
    public static final RegistrySupplier<Item> HEART_BOOSTER = ITEMS.register("heart_booster", () -> new com.hbm_m.armormod.item.ItemModHealth(40F));
    public static final RegistrySupplier<Item> HEART_FAB = ITEMS.register("heart_fab", () -> new com.hbm_m.armormod.item.ItemModHealth(60F));
    public static final RegistrySupplier<Item> BLACK_DIAMOND = ITEMS.register("black_diamond", () -> new com.hbm_m.armormod.item.ItemModHealth(40F));

    public static final RegistrySupplier<Item> GHIORSIUM_CLADDING = ITEMS.register("cladding_ghiorsium", () -> new com.hbm_m.armormod.item.ItemModCladding(0.5));
    public static final RegistrySupplier<Item> DESH_CLADDING = ITEMS.register("cladding_desh", () -> new com.hbm_m.armormod.item.ItemModCladding(0.2));
    public static final RegistrySupplier<Item> LEAD_CLADDING = ITEMS.register("cladding_lead", () -> new com.hbm_m.armormod.item.ItemModCladding(0.1));
    public static final RegistrySupplier<Item> RUBBER_CLADDING = ITEMS.register("cladding_rubber", () -> new com.hbm_m.armormod.item.ItemModCladding(0.005));
    public static final RegistrySupplier<Item> CLADDING_IRON = ITEMS.register("cladding_iron", () -> new com.hbm_m.armormod.item.ItemModIron());
    public static final RegistrySupplier<Item> CLADDING_OBSIDIAN = ITEMS.register("cladding_obsidian", () -> new com.hbm_m.armormod.item.ItemModObsidian());
    public static final RegistrySupplier<Item> PAINT_CLADDING = ITEMS.register("cladding_paint", () -> new com.hbm_m.armormod.item.ItemModCladding(0.025));

    // РќРѕРІС‹Рµ РјРѕРґРёС„РёРєР°С†РёРё Р±СЂРѕРЅРё
//     public static final RegistrySupplier<Item> ARMOR_MOD_SERVOS = ITEMS.register("armor_mod_servos",
//             () -> new ItemModServos(new Item.Properties())
//     );

//     public static final RegistrySupplier<Item> ARMOR_MOD_CLADDING = ITEMS.register("armor_mod_cladding",
//             () -> new ItemModCladding(new Item.Properties())
//     );

//     public static final RegistrySupplier<Item> ARMOR_MOD_KEVLAR = ITEMS.register("armor_mod_kevlar",
//             () -> new ItemModKevlar(new Item.Properties())
//     );

//     public static final RegistrySupplier<Item> ARMOR_MOD_EXTRA = ITEMS.register("armor_mod_extra",
//             () -> new ItemModExtra(new Item.Properties())
//     );

    // РњРѕРґРёС„РёРєР°С‚РѕСЂС‹ Р±Р°С‚Р°СЂРµРё (СѓРІРµР»РёС‡РёРІР°СЋС‚ Р·Р°СЂСЏРґ Р±СЂРѕРЅРё)
    public static final RegistrySupplier<Item> ARMOR_BATTERY = ITEMS.register("armor_battery", () -> new com.hbm_m.armormod.item.ItemModBattery(1.25D));

    public static final RegistrySupplier<Item> ARMOR_BATTERY_MK2 = ITEMS.register("armor_battery_mk2", () -> new com.hbm_m.armormod.item.ItemModBattery(1.5D));

    public static final RegistrySupplier<Item> ARMOR_BATTERY_MK3 = ITEMS.register("armor_battery_mk3", () -> new com.hbm_m.armormod.item.ItemModBattery(2D));
    public static final RegistrySupplier<Item> CREATIVE_BATTERY = ITEMS.register("battery_creative",
            () -> new ItemCreativeBattery(
                    new Item.Properties()
            )
    );
    public static final RegistrySupplier<Item> ASSEMBLY_TEMPLATE = ITEMS.register("assembly_template",
            () -> new ItemAssemblyTemplate(
                    new Item.Properties().stacksTo(1)
            )
    );
    public static final RegistrySupplier<Item> TEMPLATE_FOLDER = ITEMS.register("template_folder",
            () -> new ItemTemplateFolder(
                    new Item.Properties().stacksTo(1)
            )
    );
    // Original ItemBlueprintFolder Meta 0/1/2: Zufallsspender fuer Blaupausen der Gruppen alt./discover./secret.
    public static final RegistrySupplier<Item> BLUEPRINT_FOLDER = ITEMS.register("blueprint_folder",
        () -> new ItemBlueprintFolder(new Item.Properties(), com.hbm_m.item.industrial.BlueprintPools.POOL_PREFIX_ALT));
    public static final RegistrySupplier<Item> BLUEPRINT_FOLDER_DISCOVER = ITEMS.register("blueprint_folder_discover",
        () -> new ItemBlueprintFolder(new Item.Properties(), com.hbm_m.item.industrial.BlueprintPools.POOL_PREFIX_DISCOVER));
    public static final RegistrySupplier<Item> BLUEPRINT_FOLDER_SECRET = ITEMS.register("blueprint_folder_secret",
        () -> new ItemBlueprintFolder(new Item.Properties(), com.hbm_m.item.industrial.BlueprintPools.POOL_PREFIX_SECRET));

    // в•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђ MACHINE UPGRADES в•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђ

    public static final RegistrySupplier<Item> UPGRADE_SPEED_1 = ITEMS.register("upgrade_speed_1",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.SPEED, 1));
    public static final RegistrySupplier<Item> UPGRADE_SPEED_2 = ITEMS.register("upgrade_speed_2",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.SPEED, 2));
    public static final RegistrySupplier<Item> UPGRADE_SPEED_3 = ITEMS.register("upgrade_speed_3",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.SPEED, 3));
    public static final RegistrySupplier<Item> UPGRADE_STACK_1 = ITEMS.register("upgrade_stack_1",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.STACK, 1));
    public static final RegistrySupplier<Item> UPGRADE_STACK_2 = ITEMS.register("upgrade_stack_2",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.STACK, 2));
    public static final RegistrySupplier<Item> UPGRADE_STACK_3 = ITEMS.register("upgrade_stack_3",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.STACK, 3));
    public static final RegistrySupplier<Item> UPGRADE_EJECTOR_1 = ITEMS.register("upgrade_ejector_1",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.EJECTOR, 1));
    public static final RegistrySupplier<Item> UPGRADE_EJECTOR_2 = ITEMS.register("upgrade_ejector_2",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.EJECTOR, 2));
    public static final RegistrySupplier<Item> UPGRADE_EJECTOR_3 = ITEMS.register("upgrade_ejector_3",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.EJECTOR, 3));

    public static final RegistrySupplier<Item> UPGRADE_EFFECT_1 = ITEMS.register("upgrade_effect_1",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.EFFECT, 1));
    public static final RegistrySupplier<Item> UPGRADE_EFFECT_2 = ITEMS.register("upgrade_effect_2",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.EFFECT, 2));
    public static final RegistrySupplier<Item> UPGRADE_EFFECT_3 = ITEMS.register("upgrade_effect_3",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.EFFECT, 3));

    public static final RegistrySupplier<Item> UPGRADE_POWER_1 = ITEMS.register("upgrade_power_1",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.POWER, 1));
    public static final RegistrySupplier<Item> UPGRADE_POWER_2 = ITEMS.register("upgrade_power_2",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.POWER, 2));
    public static final RegistrySupplier<Item> UPGRADE_POWER_3 = ITEMS.register("upgrade_power_3",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.POWER, 3));

    public static final RegistrySupplier<Item> UPGRADE_FORTUNE_1 = ITEMS.register("upgrade_fortune_1",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.FORTUNE, 1));
    public static final RegistrySupplier<Item> UPGRADE_FORTUNE_2 = ITEMS.register("upgrade_fortune_2",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.FORTUNE, 2));
    public static final RegistrySupplier<Item> UPGRADE_FORTUNE_3 = ITEMS.register("upgrade_fortune_3",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.FORTUNE, 3));

    public static final RegistrySupplier<Item> UPGRADE_AFTERBURN_1 = ITEMS.register("upgrade_afterburn_1",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.AFTERBURN, 1));
    public static final RegistrySupplier<Item> UPGRADE_AFTERBURN_2 = ITEMS.register("upgrade_afterburn_2",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.AFTERBURN, 2));
    public static final RegistrySupplier<Item> UPGRADE_AFTERBURN_3 = ITEMS.register("upgrade_afterburn_3",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.AFTERBURN, 3));

    public static final RegistrySupplier<Item> UPGRADE_OVERDRIVE_1 = ITEMS.register("upgrade_overdrive_1",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.OVERDRIVE, 1));
    public static final RegistrySupplier<Item> UPGRADE_OVERDRIVE_2 = ITEMS.register("upgrade_overdrive_2",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.OVERDRIVE, 2));
    public static final RegistrySupplier<Item> UPGRADE_OVERDRIVE_3 = ITEMS.register("upgrade_overdrive_3",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.OVERDRIVE, 3));

    // Maxwell accepts the full upgrade range in the original (TileEntityTurretMaxwell's
    // getAmmoTypesForDisplay lists seventeen); these last two were never registered here, so the
    // turret was missing two of its ammo types outright. Textures were already in the repo.
    // Kraftfeldaufwertungen. Original: eigene Eintraege mit Stapelgroesse 16, keine Stufen -
    // die Wirkung haengt allein daran, wie viele im Slot liegen.
    public static final RegistrySupplier<Item> UPGRADE_RADIUS = ITEMS.register("upgrade_radius",
            () -> new ItemMachineUpgrade(new Item.Properties(),
                    ItemMachineUpgrade.UpgradeType.RADIUS, 1, 16));
    public static final RegistrySupplier<Item> UPGRADE_HEALTH = ITEMS.register("upgrade_health",
            () -> new ItemMachineUpgrade(new Item.Properties(),
                    ItemMachineUpgrade.UpgradeType.HEALTH, 1, 16));

    public static final RegistrySupplier<Item> UPGRADE_5G = ITEMS.register("upgrade_5g",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.SPECIAL));
    // Original: Bergbaulaser- und Gaszentrifugen-Upgrades (new ItemMachineUpgrade() = SPECIAL)
    public static final RegistrySupplier<Item> UPGRADE_SMELTER = ITEMS.register("upgrade_smelter",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.SPECIAL));
    public static final RegistrySupplier<Item> UPGRADE_SHREDDER = ITEMS.register("upgrade_shredder",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.SPECIAL));
    public static final RegistrySupplier<Item> UPGRADE_CENTRIFUGE = ITEMS.register("upgrade_centrifuge",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.SPECIAL));
    public static final RegistrySupplier<Item> UPGRADE_CRYSTALLIZER = ITEMS.register("upgrade_crystallizer",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.SPECIAL));
    public static final RegistrySupplier<Item> UPGRADE_NULLIFIER = ITEMS.register("upgrade_nullifier",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.SPECIAL));
    public static final RegistrySupplier<Item> UPGRADE_GC_SPEED = ITEMS.register("upgrade_gc_speed",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.SPECIAL));
    public static final RegistrySupplier<Item> UPGRADE_SCREM = ITEMS.register("upgrade_screm",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.SPECIAL));

    // ═══════════════════ END MACHINE UPGRADES ═══════════════════
    // в•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђ END MACHINE UPGRADES в•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђ

    public static final RegistrySupplier<Item> RADAWAY = ITEMS.register("radaway",
            () -> com.hbm_m.item.special.ItemSimpleConsumable.radaway(140));
    public static final RegistrySupplier<Item> RADAWAY_STRONG = ITEMS.register("radaway_strong",
            () -> com.hbm_m.item.special.ItemSimpleConsumable.radaway(350));
    public static final RegistrySupplier<Item> RADAWAY_FLUSH = ITEMS.register("radaway_flush",
            () -> com.hbm_m.item.special.ItemSimpleConsumable.radaway(500));
    public static final RegistrySupplier<Item> OIL_DETECTOR = ITEMS.register("oil_detector",
            () -> new OilDetectorItem(new Item.Properties()));

    public static final RegistrySupplier<Item> DEPTH_ORES_SCANNER = ITEMS.register("depth_ores_scanner",
            () -> new DepthOresScannerItem(new Item.Properties()));

    public static final RegistrySupplier<Item> RANGEFINDER = ITEMS.register("rangefinder",
            () -> new RangefinderItem(new Item.Properties()));

    public static final RegistrySupplier<Item> CONFETTI_TESTER = ITEMS.register("confetti_tester",
            ConfettiTesterItem::new);

    public static final RegistrySupplier<Item> RANGE_DETONATOR = ITEMS.register("range_detonator",
            () -> new RangeDetonatorItem(new Item.Properties()));

    public static final RegistrySupplier<Item> MULTI_DETONATOR = ITEMS.register("multi_detonator",
            () -> new MultiDetonatorItem(new Item.Properties()));

    public static final RegistrySupplier<Item> DETONATOR = ITEMS.register("detonator",
            () -> new DetonatorItem(new Item.Properties()));

    public static final RegistrySupplier<Item> BALL_TNT = ITEMS.register("ball_tnt",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FAT_MAN_EXPLOSIVE = ITEMS.register("fat_man_explosive",
            () -> new LoreTooltipItem(List.of(
                    Component.translatable("tooltip.hbm_m.fat_man_explosive.desc1").withStyle(ChatFormatting.GRAY),
                    Component.translatable("tooltip.hbm_m.fat_man_explosive.desc2").withStyle(ChatFormatting.GRAY)),
                    new Item.Properties()));
    public static final RegistrySupplier<Item> FAT_MAN_IGNITER = ITEMS.register("fat_man_igniter",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> IGNITER = ITEMS.register("igniter",
            () -> new ItemCustomLore(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> FAT_MAN_CORE = ITEMS.register("fat_man_core",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    public static final RegistrySupplier<Item> CELL_SAS3 = ITEMS.register("cell_sas3",
            () -> new com.hbm_m.item.ContainerItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), () -> ModItems.CELL_EMPTY.get()));
    public static final RegistrySupplier<Item> ROD_QUAD_LEAD = ITEMS.register("rod_quad_lead",
            () -> new com.hbm_m.item.nuclear.ItemBreedingRod(new Item.Properties(), com.hbm_m.item.nuclear.BreedingRodType.LEAD, () -> ModItems.ROD_QUAD_EMPTY.get()));
    public static final RegistrySupplier<Item> ROD_QUAD_NP237 = ITEMS.register("rod_quad_np237",
            () -> new com.hbm_m.item.nuclear.ItemBreedingRod(new Item.Properties(), com.hbm_m.item.nuclear.BreedingRodType.NP237, () -> ModItems.ROD_QUAD_EMPTY.get()));
    public static final RegistrySupplier<Item> ROD_QUAD_URANIUM = ITEMS.register("rod_quad_uranium",
            () -> new com.hbm_m.item.nuclear.ItemBreedingRod(new Item.Properties(), com.hbm_m.item.nuclear.BreedingRodType.URANIUM, () -> ModItems.ROD_QUAD_EMPTY.get()));

    public static final RegistrySupplier<Item> CROWBAR = ITEMS.register("crowbar",
            () -> new ModSword(HbmToolMaterial.STEEL));


    public static final RegistrySupplier<Item> MALACHITE_CHUNK = ITEMS.register("malachite_chunk",
            () -> new Item(new Item.Properties()));
    /** Original chunk_ore ILMENITE: aus Titanerz (Zentrifuge), wird zu Titantetrachlorid verarbeitet. */
    public static final RegistrySupplier<Item> ILMENITE_CHUNK = ITEMS.register("ilmenite_chunk",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LIMESTONE = ITEMS.register("limestone",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHELL_STEEL = ITEMS.register("shell_steel",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHELL_COPPER = ITEMS.register("shell_copper",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHELL_ALUMINUM = ITEMS.register("shell_aluminum",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHELL_TITANIUM = ITEMS.register("shell_titanium",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAN_KEY = ITEMS.register("can_key",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEFUSER = ITEMS.register("defuser",
            () -> new com.hbm_m.item.tool.ItemDefuser(100, new Item.Properties()));

    public static final RegistrySupplier<Item> GAS_EMPTY = ITEMS.register("gas_empty",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DUCTTAPE = ITEMS.register("ducttape",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_CLOTH = ITEMS.register("hazmat_cloth",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_CLOTH_GREY = ITEMS.register("hazmat_cloth_grey",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_CLOTH_RED = ITEMS.register("hazmat_cloth_red",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASBESTOS_CLOTH = ITEMS.register("asbestos_cloth",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLT_STEEL = ITEMS.register("bolt_steel",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLT_LEAD = ITEMS.register("bolt_lead",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLT_TUNGSTEN = ITEMS.register("bolt_tungsten",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLT_HIGHSPEED_STEEL = ITEMS.register("bolt_highspeed_steel",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ZIRCONIUM_SHARP = ITEMS.register("zirconium_sharp",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_TUNGSTEN = ITEMS.register("coil_tungsten",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_GOLD_TORUS = ITEMS.register("coil_gold_torus",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_GOLD = ITEMS.register("coil_gold",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_MAGNETIZED_TUNGSTEN_TORUS = ITEMS.register("coil_magnetized_tungsten_torus",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_MAGNETIZED_TUNGSTEN = ITEMS.register("coil_magnetized_tungsten",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_COPPER_TORUS = ITEMS.register("coil_copper_torus",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_COPPER = ITEMS.register("coil_copper",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_ADVANCED_ALLOY_TORUS = ITEMS.register("coil_advanced_alloy_torus",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_ADVANCED_ALLOY = ITEMS.register("coil_advanced_alloy",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MOTOR_DESH = ITEMS.register("motor_desh",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MOTOR_BISMUTH = ITEMS.register("motor_bismuth",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MOTOR = ITEMS.register("motor",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BORAX = ITEMS.register("borax",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DUST = ITEMS.register("dust",
            () -> new LoreTooltipItem(List.of(
                    Component.translatable("tooltip.hbm_m.dust.desc1").withStyle(ChatFormatting.GRAY)),
                    new Item.Properties()));
    public static final RegistrySupplier<Item> DUST_TINY = ITEMS.register("dust_tiny",
            () -> new Item(new Item.Properties()));
    /** 1.7.10 ModItems.fallout вЂ” РєСѓС‡РєР° РѕСЃР°РґРєРѕРІ. */
    public static final RegistrySupplier<Item> FALLOUT = ITEMS.register("fallout",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> NUCLEAR_WASTE_TINY = ITEMS.register("nuclear_waste_tiny", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));

    public static final RegistrySupplier<Item> NUCLEAR_WASTE_LONG_TINY = ITEMS.register("nuclear_waste_long_tiny", () -> new com.hbm_m.item.special.ItemNuclearWaste(java.util.List.of(net.minecraft.network.chat.Component.literal("Uranium-235").withStyle(net.minecraft.ChatFormatting.ITALIC)), new Item.Properties()));

    public static final RegistrySupplier<Item> NUCLEAR_WASTE_LONG_DEPLETED_TINY = ITEMS.register("nuclear_waste_long_depleted_tiny", () -> new com.hbm_m.item.special.ItemNuclearWaste(java.util.List.of(net.minecraft.network.chat.Component.literal("Uranium-235").withStyle(net.minecraft.ChatFormatting.ITALIC)), new Item.Properties()));

    public static final RegistrySupplier<Item> NUCLEAR_WASTE_SHORT_TINY = ITEMS.register("nuclear_waste_short_tiny", () -> new com.hbm_m.item.LoreTooltipItem(java.util.List.of(net.minecraft.network.chat.Component.literal("Uranium-235").withStyle(net.minecraft.ChatFormatting.ITALIC)), new Item.Properties()));

    public static final RegistrySupplier<Item> NUCLEAR_WASTE_SHORT_DEPLETED_TINY = ITEMS.register("nuclear_waste_short_depleted_tiny", () -> new com.hbm_m.item.LoreTooltipItem(java.util.List.of(net.minecraft.network.chat.Component.literal("Uranium-235").withStyle(net.minecraft.ChatFormatting.ITALIC)), new Item.Properties()));

    public static final RegistrySupplier<Item> NUCLEAR_WASTE_VITRIFIED_TINY = ITEMS.register("nuclear_waste_vitrified_tiny", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));

    public static final RegistrySupplier<Item> NUGGET_MERCURY_TINY = ITEMS.register("nugget_mercury_tiny",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> SILICON_CIRCUIT = ITEMS.register("silicon_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BISMOID_CIRCUIT = ITEMS.register("bismoid_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> QUANTUM_CHIP = ITEMS.register("quantum_chip",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CAPACITOR_BOARD = ITEMS.register("capacitor_board",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CAPACITOR_TANTALUM = ITEMS.register("capacitor_tantalum",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BISMOID_CHIP = ITEMS.register("bismoid_chip",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CONTROLLER_CHASSIS = ITEMS.register("controller_chassis",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CONTROLLER = ITEMS.register("controller",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CONTROLLER_ADVANCED = ITEMS.register("controller_advanced",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> QUANTUM_COMPUTER = ITEMS.register("quantum_computer",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> QUANTUM_CIRCUIT = ITEMS.register("quantum_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ANALOG_CIRCUIT = ITEMS.register("analog_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> INTEGRATED_CIRCUIT = ITEMS.register("integrated_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ADVANCED_CIRCUIT = ITEMS.register("advanced_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> VACUUM_TUBE = ITEMS.register("vacuum_tube",
            () -> new Item(new Item.Properties()));

    // EnumCircuitType.NUMITRON from the original's ItemCircuit - needed by the rbmk_numitron recipe.
    public static final RegistrySupplier<Item> CIRCUIT_NUMITRON = ITEMS.register("circuit_numitron",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CAPACITOR = ITEMS.register("capacitor",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CENTRIFUGE_ELEMENT = ITEMS.register("centrifuge_element",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> MICROCHIP = ITEMS.register("microchip",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ATOMIC_CLOCK = ITEMS.register("atomic_clock",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PCB = ITEMS.register("pcb",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> METAL_ROD = ITEMS.register("metal_rod",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BATTLE_MODULE = ITEMS.register("battle_module",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BATTLE_GEARS = ITEMS.register("battle_gears",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BATTLE_CASING = ITEMS.register("battle_casing",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BATTLE_SENSOR = ITEMS.register("battle_sensor",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BATTLE_COUNTER = ITEMS.register("battle_counter",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRT_DISPLAY = ITEMS.register("crt_display",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> MAGNETRON = ITEMS.register("magnetron",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> TURBINE_TITANIUM = ITEMS.register("turbine_titanium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_KEVLAR = ITEMS.register("plate_kevlar",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_MIXED = ITEMS.register("plate_mixed",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_PAA = ITEMS.register("plate_paa",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    /** Original ModItems.plate_polymer ("Insulator") = Materialplatte POLYMER/PLATE (ID plate_polymer).
     *  Die fruehere Doppel-ID "insulator" wird per ForgeMainEvents.LEGACY_IDS umgeleitet. */
    public static final java.util.function.Supplier<Item> INSULATOR =
            () -> com.hbm_m.item.material.ModMaterialItems.item(com.hbm_m.item.material.ModMaterials.POLYMER, com.hbm_m.item.material.MaterialShape.PLATE);

    public static final RegistrySupplier<Item> PLATE_ARMOR_AJR = ITEMS.register("plate_armor_ajr",
        () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_DNT = ITEMS.register("plate_armor_dnt",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_DNT_RUSTED = ITEMS.register("plate_armor_dnt_rusted",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_FAU = ITEMS.register("plate_armor_fau",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_HEV = ITEMS.register("plate_armor_hev",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_LUNAR = ITEMS.register("plate_armor_lunar",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_TITANIUM = ITEMS.register("plate_armor_titanium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_CAST = ITEMS.register("plate_cast",
        () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_CAST_ALT = ITEMS.register("plate_cast_alt",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_CAST_DARK = ITEMS.register("plate_cast_dark",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_DALEKANIUM = ITEMS.register("plate_dalekanium",
        () -> new Item(new Item.Properties()));

    // Research-Reactor-Brennstoffplatten - Funktion/Reaktivitaet/Lebensdauer 1:1 aus dem Original
    // (ModItems.java, registerDefaults) uebernommen.
    public static final RegistrySupplier<Item> PLATE_FUEL_MOX = ITEMS.register("plate_fuel_mox",
        () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                2_400_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.LOGARITHM, 50));

    public static final RegistrySupplier<Item> PLATE_FUEL_PU238BE = ITEMS.register("plate_fuel_pu238be",
            () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                    1_000_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.PASSIVE, 50));

    public static final RegistrySupplier<Item> PLATE_FUEL_PU239 = ITEMS.register("plate_fuel_pu239",
            () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                    2_000_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.NEGATIVE_QUADRATIC, 50));

    public static final RegistrySupplier<Item> PLATE_FUEL_RA226BE = ITEMS.register("plate_fuel_ra226be",
            () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                    1_300_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.PASSIVE, 30));

    public static final RegistrySupplier<Item> PLATE_FUEL_SA326 = ITEMS.register("plate_fuel_sa326",
            () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                    2_000_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.LINEAR, 80));

    public static final RegistrySupplier<Item> PLATE_FUEL_U233 = ITEMS.register("plate_fuel_u233",
            () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                    2_200_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.SQUARE_ROOT, 50));

    public static final RegistrySupplier<Item> PLATE_FUEL_U235 = ITEMS.register("plate_fuel_u235",
            () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                    2_200_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.SQUARE_ROOT, 40));

    // 1:1 with the original's ItemRBMKRod stat block for rbmk_fuel_drx (items/ModItems.java:3307-3313):
    // yield 10,000,000 / reactivity 1000 / selfRate 10 / QUADRATIC burn / heat 0.1 / melting point
    // 100,000. Was previously registered as a plain flavor-text Item, meaning it could never
    // actually be loaded as reactor fuel (RBMKRodBlock#use gates on `instanceof RBMKRodItem`).
    public static final RegistrySupplier<Item> RBMK_FUEL_DRX = ITEMS.register("rbmk_fuel_drx",
            () -> new RbmkFuelDrxItem(new Item.Properties())
                    .setYield(100_000_000).setStats(1000, 10).setFunction(RBMKRodItem.EnumBurnFunc.QUADRATIC)
                    .setHeat(0.1).setMeltingPoint(100_000).setTint(0xD77276).setPellet(() -> ModItems.RBMK_PELLET_DRX.get()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_EMPTY = ITEMS.register("rod_zirnox_empty",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_LES_FUEL = ITEMS.register("rod_zirnox_les_fuel",
            () -> new ZirnoxRodItem(new Item.Properties(), 150_000, 150, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_LES_FUEL_DEPLETED = ITEMS.register("rod_zirnox_les_fuel_depleted", () -> new Item(new Item.Properties().craftRemainder(ROD_ZIRNOX_EMPTY.get())));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_LITHIUM = ITEMS.register("rod_zirnox_lithium",
            () -> new ZirnoxRodItem(new Item.Properties(), 20_000, 0, true));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_MOX_FUEL = ITEMS.register("rod_zirnox_mox_fuel",
            () -> new ZirnoxRodItem(new Item.Properties(), 165_000, 75, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_MOX_FUEL_DEPLETED = ITEMS.register("rod_zirnox_mox_fuel_depleted", () -> new Item(new Item.Properties().craftRemainder(ROD_ZIRNOX_EMPTY.get())));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_NATURAL_URANIUM_FUEL = ITEMS.register("rod_zirnox_natural_uranium_fuel",
            () -> new ZirnoxRodItem(new Item.Properties(), 250_000, 30, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_PLUTONIUM_FUEL = ITEMS.register("rod_zirnox_plutonium_fuel",
            () -> new ZirnoxRodItem(new Item.Properties(), 175_000, 65, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_PLUTONIUM_FUEL_DEPLETED = ITEMS.register("rod_zirnox_plutonium_fuel_depleted", () -> new Item(new Item.Properties().craftRemainder(ROD_ZIRNOX_EMPTY.get())));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_TH232 = ITEMS.register("rod_zirnox_th232",
            () -> new ZirnoxRodItem(new Item.Properties(), 20_000, 0, true));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_THORIUM_FUEL = ITEMS.register("rod_zirnox_thorium_fuel",
            () -> new ZirnoxRodItem(new Item.Properties(), 200_000, 40, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_THORIUM_FUEL_DEPLETED = ITEMS.register("rod_zirnox_thorium_fuel_depleted", () -> new Item(new Item.Properties().craftRemainder(ROD_ZIRNOX_EMPTY.get())));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_TRITIUM = ITEMS.register("rod_zirnox_tritium",
            () -> new com.hbm_m.item.ContainerItem(new Item.Properties().stacksTo(1), () -> ModItems.ROD_ZIRNOX_EMPTY.get()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_U233_FUEL = ITEMS.register("rod_zirnox_u233_fuel",
            () -> new ZirnoxRodItem(new Item.Properties(), 150_000, 100, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_U233_FUEL_DEPLETED = ITEMS.register("rod_zirnox_u233_fuel_depleted", () -> new Item(new Item.Properties().craftRemainder(ROD_ZIRNOX_EMPTY.get())));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_U235_FUEL = ITEMS.register("rod_zirnox_u235_fuel",
            () -> new ZirnoxRodItem(new Item.Properties(), 165_000, 85, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_U235_FUEL_DEPLETED = ITEMS.register("rod_zirnox_u235_fuel_depleted", () -> new Item(new Item.Properties().craftRemainder(ROD_ZIRNOX_EMPTY.get())));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_URANIUM_FUEL = ITEMS.register("rod_zirnox_uranium_fuel",
            () -> new ZirnoxRodItem(new Item.Properties(), 200_000, 50, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_NATURAL_URANIUM_FUEL_DEPLETED = ITEMS.register("rod_zirnox_natural_uranium_fuel_depleted",
            () -> new Item(new Item.Properties().craftRemainder(ROD_ZIRNOX_EMPTY.get())));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_URANIUM_FUEL_DEPLETED = ITEMS.register("rod_zirnox_uranium_fuel_depleted", () -> new Item(new Item.Properties().craftRemainder(ROD_ZIRNOX_EMPTY.get())));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_ZFB_MOX = ITEMS.register("rod_zirnox_zfb_mox",
            () -> new ZirnoxRodItem(new Item.Properties(), 50_000, 35, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_ZFB_MOX_DEPLETED = ITEMS.register("rod_zirnox_zfb_mox_depleted", () -> new Item(new Item.Properties().craftRemainder(ROD_ZIRNOX_EMPTY.get())));

    // RAW METALS

    public static final RegistrySupplier<Item> URANIUM_RAW = ITEMS.register("uranium_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> LEAD_RAW = ITEMS.register("lead_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BERYLLIUM_RAW = ITEMS.register("beryllium_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ALUMINUM_RAW = ITEMS.register("aluminum_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> TITANIUM_RAW = ITEMS.register("titanium_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> THORIUM_RAW = ITEMS.register("thorium_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> COBALT_RAW = ITEMS.register("cobalt_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> TUNGSTEN_RAW = ITEMS.register("tungsten_raw",
            () -> new Item(new Item.Properties()));

    // Bedrock-Ore-Veredelung: fehlende Rohmaterialien aus der Original-Rezeptkette
    // (ItemBedrockOreNew.BedrockOreType, siehe CentrifugeRecipes/CrystallizerRecipes des Originals).
    public static final RegistrySupplier<Item> RADIUM_RAW = ITEMS.register("radium_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> SALTPETER = ITEMS.register("saltpeter",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYOLITE = ITEMS.register("cryolite",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> MOLYSITE = ITEMS.register("molysite",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> RAREEARTH_RAW = ITEMS.register("rareearth_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> POWDER_CHLOROCALCITE = ITEMS.register("powder_chlorocalcite",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> POWDER_SODIUM = ITEMS.register("powder_sodium",
            () -> new Item(new Item.Properties()));



    // РњР°С‚РµСЂРёР°Р»С‹
    public static final RegistrySupplier<Item> SULFUR = ITEMS.register("sulfur",
            () -> new Item(new Item.Properties()));

    // Kokerer-Ausgabe - im Original ein Metadata-Subtyp von ItemEnumMulti(EnumCokeType) mit
    // COAL/LIGNITE/PETROLEUM; hier nur die fuer den Coker benoetigte PETROLEUM-Variante als
    // eigenstaendiges Item (COAL/LIGNITE gehoeren zu anderen, noch nicht portierten Maschinen).
    public static final RegistrySupplier<Item> COKE_PETROLEUM = ITEMS.register("coke_petroleum",
            () -> new Item(new Item.Properties()));

    // Ashpit-Ausgabe - im Original ein Metadata-Subtyp von ItemEnumMulti(EnumAshType) mit
    // WOOD/COAL/MISC/FLY/SOOT (+FULLERENE, hier nicht benoetigt); hier als 5 eigenstaendige Items.
    public static final RegistrySupplier<Item> ASH_WOOD = ITEMS.register("ash_wood", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASH_COAL = ITEMS.register("ash_coal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASH_MISC = ITEMS.register("ash_misc", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASH_FLY  = ITEMS.register("ash_fly",  () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASH_SOOT = ITEMS.register("ash_soot", () -> new Item(new Item.Properties()));

    // Teer - im Original ein ItemEnumMulti(EnumTarType) mit sechs Metadata-Subtypen
    // (CRUDE/CRACK/COAL/WOOD/WAX/PARAFFIN); hier als sechs eigenstaendige Items, analog zur
    // Asche oben. Der RBMK-Outgasser erzeugt COAL-Teer aus Kohle und verarbeitet COAL/WAX weiter;
    // die uebrigen Sorten gehoeren zu Raffinerie/Kristallisator und sind hier nur registriert,
    // damit die Familie vollstaendig ist und jene Rezepte spaeter darauf zeigen koennen.
    public static final RegistrySupplier<Item> OIL_TAR_CRUDE = ITEMS.register("oil_tar_crude", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> OIL_TAR_CRACK = ITEMS.register("oil_tar_crack", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> OIL_TAR_COAL = ITEMS.register("oil_tar_coal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> OIL_TAR_WOOD = ITEMS.register("oil_tar_wood", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> OIL_TAR_WAX = ITEMS.register("oil_tar_wax", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> OIL_TAR_PARAFFIN = ITEMS.register("oil_tar_paraffin", () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> SEQUESTRUM = ITEMS.register("sequestrum",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> FLUORITE = ITEMS.register("fluorite",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> RAREGROUND_ORE_CHUNK = ITEMS.register("rareground_ore_chunk",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> FIRECLAY_BALL = ITEMS.register("fireclay_ball",
            () -> new Item(new Item.Properties()));

    // wood_ash_powder: doppelte Port-ID von powder_ash@WOOD (= ash_wood), Umleitung in ForgeMainEvents.LEGACY_IDS

    /** РџРѕСЂС‚ {@code powder_desh_mix}. */
    public static final RegistrySupplier<Item> POWDER_DESH_MIX = ITEMS.register("powder_desh_mix",
            () -> new Item(new Item.Properties()));

    /** РџРѕСЂС‚ {@code powder_nitan_mix}. */
    public static final RegistrySupplier<Item> POWDER_NITAN_MIX = ITEMS.register("powder_nitan_mix",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> LIGNITE_POWDER = ITEMS.register("lignite_powder",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> FIRE_POWDER = ITEMS.register("fire_powder",
            () -> new LoreTooltipItem(List.of(
                    Component.translatable("tooltip.hbm_m.fire_powder.desc1").withStyle(ChatFormatting.GRAY),
                    Component.translatable("tooltip.hbm_m.fire_powder.desc2").withStyle(ChatFormatting.GRAY)),
                    new Item.Properties()));

    public static final RegistrySupplier<Item> FIREBRICK = ITEMS.register("firebrick",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> LIGNITE = ITEMS.register("lignite",
            () -> new FuelItem(new Item.Properties(), 1000));




    public static final RegistrySupplier<Item> MACHINE_ASSEMBLER = ITEMS.register("machine_assembler",
        () -> new MultiblockBlockItem(ModBlocks.MACHINE_ASSEMBLER.get(), new Item.Properties()));
            
    public static final RegistrySupplier<Item> ADVANCED_ASSEMBLY_MACHINE = ITEMS.register("advanced_assembly_machine",
        () -> new MultiblockBlockItem(ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> HYDRAULIC_FRACKINING_TOWER = ITEMS.register("hydraulic_frackining_tower",
        () -> new MultiblockBlockItem(ModBlocks.HYDRAULIC_FRACKINING_TOWER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> COOLING_TOWER = ITEMS.register("cooling_tower",
        () -> new MultiblockBlockItem(ModBlocks.COOLING_TOWER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> TOWER_SMALL = ITEMS.register("tower_small",
        () -> new MultiblockBlockItem(ModBlocks.TOWER_SMALL.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> CYCLOTRON = ITEMS.register("cyclotron",
        () -> new MultiblockBlockItem(ModBlocks.CYCLOTRON.get(), new Item.Properties()));

    // ===== Fusionsreaktor: Blockitems der Multiblock-Maschinen =====
    public static final RegistrySupplier<Item> FUSION_TORUS_ITEM = ITEMS.register("torus",
            () -> new MultiblockBlockItem(ModBlocks.TORUS.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_KLYSTRON_ITEM = ITEMS.register("klystron",
            () -> new MultiblockBlockItem(ModBlocks.KLYSTRON.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_KLYSTRON_CREATIVE_ITEM = ITEMS.register("klystron_creative",
            () -> new MultiblockBlockItem(ModBlocks.KLYSTRON_CREATIVE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_BREEDER_ITEM = ITEMS.register("breeder_fusion",
            () -> new MultiblockBlockItem(ModBlocks.BREEDER_FUSION.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_COLLECTOR_ITEM = ITEMS.register("collector",
            () -> new MultiblockBlockItem(ModBlocks.COLLECTOR.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_COUPLER_ITEM = ITEMS.register("coupler",
            () -> new MultiblockBlockItem(ModBlocks.COUPLER.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_BOILER_ITEM = ITEMS.register("boiler_fusion",
            () -> new MultiblockBlockItem(ModBlocks.BOILER_FUSION.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_MHDT_ITEM = ITEMS.register("mhdt",
            () -> new MultiblockBlockItem(ModBlocks.MHDT.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_PLASMA_FORGE_ITEM = ITEMS.register("plasma_forge",
            () -> new MultiblockBlockItem(ModBlocks.PLASMA_FORGE.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> PART_LITHIUM    = ITEMS.register("part_lithium",    () -> new Item(new Item.Properties()));
    /** Beryllium particle вЂ” medium-energy cyclotron projectile. */
    public static final RegistrySupplier<Item> PART_BERYLLIUM  = ITEMS.register("part_beryllium",  () -> new Item(new Item.Properties()));
    /** Carbon (coal-derived) particle вЂ” low-energy cyclotron projectile. */
    public static final RegistrySupplier<Item> PART_CARBON     = ITEMS.register("part_carbon",     () -> new Item(new Item.Properties()));
    /** Copper ion вЂ” medium-energy cyclotron projectile. */
    public static final RegistrySupplier<Item> PART_COPPER     = ITEMS.register("part_copper",     () -> new Item(new Item.Properties()));
    /** Plutonium nucleus вЂ” high-energy cyclotron projectile, produces australium. */
    public static final RegistrySupplier<Item> PART_PLUTONIUM  = ITEMS.register("part_plutonium",  () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> MOLD_BARREL_HEAVY = ITEMS.register("mold_barrel_heavy",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BARREL_HEAVY, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_BARREL_LIGHT = ITEMS.register("mold_barrel_light",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BARREL_LIGHT, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_BASE = ITEMS.register("mold_base",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BASE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_BILLET = ITEMS.register("mold_billet",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BILLET, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_BLADE = ITEMS.register("mold_blade",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BLADE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_BLADES = ITEMS.register("mold_blades",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BLADES, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_BLOCK = ITEMS.register("mold_block",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BLOCK, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_C357 = ITEMS.register("mold_c357",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.C357, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_CBUCKSHOT = ITEMS.register("mold_cbuckshot",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.CBUCKSHOT, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_GEM = ITEMS.register("mold_gem",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.GEM, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_GRIP = ITEMS.register("mold_grip",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.GRIP, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_HULL_BIG = ITEMS.register("mold_hull_big",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.HULL_BIG, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_HULL_SMALL = ITEMS.register("mold_hull_small",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.HULL_SMALL, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_INGOT = ITEMS.register("mold_ingot",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.INGOT, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_INGOTS = ITEMS.register("mold_ingots",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.INGOTS, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_MECHANISM = ITEMS.register("mold_mechanism",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.MECHANISM, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_MOGUS = ITEMS.register("mold_mogus",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.MOGUS, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_NUGGET = ITEMS.register("mold_nugget",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.NUGGET, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_PIPE = ITEMS.register("mold_pipe",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.PIPE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_PIPES = ITEMS.register("mold_pipes",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.PIPES, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_PLATE = ITEMS.register("mold_plate",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.PLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_PLATE_CAST = ITEMS.register("mold_plate_cast",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.PLATE_CAST, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_PLATES = ITEMS.register("mold_plates",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.PLATES, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_PLATES_CAST = ITEMS.register("mold_plates_cast",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.PLATES_CAST, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_RECEIVER_HEAVY = ITEMS.register("mold_receiver_heavy",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.RECEIVER_HEAVY, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_RECEIVER_LIGHT = ITEMS.register("mold_receiver_light",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.RECEIVER_LIGHT, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_SHELL = ITEMS.register("mold_shell",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.SHELL, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_C9 = ITEMS.register("mold_c9",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.C9, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_C50 = ITEMS.register("mold_c50",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.C50, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_STAMP = ITEMS.register("mold_stamp",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.STAMP, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_STEEL_BASE = ITEMS.register("mold_steel_base",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.STEEL_BASE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_STOCK = ITEMS.register("mold_stock",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.STOCK, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_WIRE = ITEMS.register("mold_wire",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.WIRE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_WIRE_DENSE = ITEMS.register("mold_wire_dense",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.WIRE_DENSE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_WIRES_DENSE = ITEMS.register("mold_wires_dense",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.WIRES_DENSE, new Item.Properties()));

    public static final RegistrySupplier<Item> ZIRNOX = ITEMS.register("zirnox",
        () -> new MultiblockBlockItem(ModBlocks.ZIRNOX.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> ARC_WELDER = ITEMS.register("arc_welder",
        () -> new MultiblockBlockItem(ModBlocks.ARC_WELDER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> SOLDERING_STATION = ITEMS.register("soldering_station",
        () -> new MultiblockBlockItem(ModBlocks.SOLDERING_STATION.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> MIXER = ITEMS.register("mixer",
        () -> new MultiblockBlockItem(ModBlocks.MIXER.get(), new Item.Properties()));

	/** Original machine_battery_redd (FEnSU, BlockDummyable). */
	public static final RegistrySupplier<Item> MACHINE_BATTERY_REDD = ITEMS.register("machine_battery_redd",
        () -> new MultiblockBlockItem(ModBlocks.FENSU2.get(), new Item.Properties()));
	/** Original soyuz_launcher (Startrampe, Kern 4 Bloecke ueber dem Setzpunkt). */
	public static final RegistrySupplier<Item> SOYUZ_LAUNCHER = ITEMS.register("soyuz_launcher",
        () -> new MultiblockBlockItem(ModBlocks.SOYUZ_LAUNCHER.get(), new Item.Properties()));
	public static final RegistrySupplier<Item> DERRICK = ITEMS.register("derrick",
        () -> new MultiblockBlockItem(ModBlocks.DERRICK.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> ASHPIT = ITEMS.register("ashpit",
        () -> new MultiblockBlockItem(ModBlocks.ASHPIT.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> REACTOR_RESEARCH = ITEMS.register("reactor_research",
        () -> new MultiblockBlockItem(ModBlocks.REACTOR_RESEARCH.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> RBMK_CONSOLE = ITEMS.register("rbmk_console",
        () -> new MultiblockBlockItem(ModBlocks.RBMK_CONSOLE.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> FLARE_STACK = ITEMS.register("flare_stack",
        () -> new MultiblockBlockItem(ModBlocks.FLARE_STACK.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> PUMPJACK = ITEMS.register("pumpjack",
        () -> new MultiblockBlockItem(ModBlocks.PUMPJACK.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> RADAR = ITEMS.register("radar",
        () -> new MultiblockBlockItem(ModBlocks.RADAR.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> LARGE_RADAR = ITEMS.register("large_radar",
	    () -> new MultiblockBlockItem(ModBlocks.LARGE_RADAR.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> RADAR_SCREEN = ITEMS.register("radar_screen",
	    () -> new MultiblockBlockItem(ModBlocks.RADAR_SCREEN.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> CRACKING_TOWER = ITEMS.register("cracking_tower",
        () -> new MultiblockBlockItem(ModBlocks.CRACKING_TOWER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> FRACTION_SPACER = ITEMS.register("fraction_spacer",
        () -> new MultiblockBlockItem(ModBlocks.FRACTION_SPACER.get(), new Item.Properties()));
	public static final RegistrySupplier<Item> FRACTION_TOWER = ITEMS.register("fraction_tower",
        () -> new MultiblockBlockItem(ModBlocks.FRACTION_TOWER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> MINING_DRILL = ITEMS.register("mining_drill",
        () -> new MultiblockBlockItem(ModBlocks.MINING_DRILL.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> FEL = ITEMS.register("fel",
        () -> new MultiblockBlockItem(ModBlocks.FEL.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> SILEX = ITEMS.register("silex",
        () -> new MultiblockBlockItem(ModBlocks.SILEX.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> CHEMICAL_PLANT = ITEMS.register("chemical_plant",
        () -> new MultiblockBlockItem(ModBlocks.CHEMICAL_PLANT.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> GAS_CENTRIFUGE = ITEMS.register("gas_centrifuge",
        () -> new MultiblockBlockItem(ModBlocks.GAS_CENTRIFUGE.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> CRYSTALLIZER = ITEMS.register("crystallizer",
        () -> new MultiblockBlockItem(ModBlocks.CRYSTALLIZER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> BREEDER = ITEMS.register("breeder",
        () -> new MultiblockBlockItem(ModBlocks.BREEDER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> LARGE_PYLON = ITEMS.register("large_pylon",
        () -> new MultiblockBlockItem(ModBlocks.LARGE_PYLON.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> CENTRIFUGE = ITEMS.register("centrifuge",
        () -> new MultiblockBlockItem(ModBlocks.CENTRIFUGE.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> FLUID_TANK = ITEMS.register("fluid_tank",
        () -> new MultiblockBlockItem(ModBlocks.FLUID_TANK.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> BAT9000 = ITEMS.register("bat9000",
        () -> new MultiblockBlockItem(ModBlocks.BAT9000.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> MACHINE_BATTERY_SOCKET = ITEMS.register("machine_battery_socket",
        () -> new MultiblockBlockItem(ModBlocks.MACHINE_BATTERY_SOCKET.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> PRESS = ITEMS.register("press",
        () -> new MultiblockBlockItem(ModBlocks.PRESS.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> WOOD_BURNER = ITEMS.register("wood_burner",
        () -> new MultiblockBlockItem(ModBlocks.WOOD_BURNER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> INDUSTRIAL_BOILER = ITEMS.register("industrial_boiler",
        () -> new MultiblockBlockItem(ModBlocks.INDUSTRIAL_BOILER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SOLAR_BOILER = ITEMS.register("solar_boiler",
        () -> new MultiblockBlockItem(ModBlocks.SOLAR_BOILER.get(), new Item.Properties()));


    public static final RegistrySupplier<Item> WATZ_PUMP = ITEMS.register("watz_pump",
        () -> new MultiblockBlockItem(ModBlocks.WATZ_PUMP.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> WATZ_POWERPLANT = ITEMS.register("watz_powerplant",
        () -> new MultiblockBlockItem(ModBlocks.WATZ_POWERPLANT.get(), new Item.Properties()));

    /** 1:1 {@code watz_pellet} / {@code watz_pellet_depleted}: je Metadatenstufe ({@link WatzPelletType}) ein Item. */
    public static final java.util.Map<WatzPelletType, RegistrySupplier<Item>> WATZ_PELLET = registerWatzPellets(false);
    public static final java.util.Map<WatzPelletType, RegistrySupplier<Item>> WATZ_PELLET_DEPLETED = registerWatzPellets(true);

    private static java.util.Map<WatzPelletType, RegistrySupplier<Item>> registerWatzPellets(boolean depleted) {
        java.util.Map<WatzPelletType, RegistrySupplier<Item>> map = new java.util.EnumMap<>(WatzPelletType.class);
        for (WatzPelletType t : WatzPelletType.values()) {
            map.put(t, ITEMS.register((depleted ? "watz_pellet_depleted_" : "watz_pellet_") + t.id(),
                    () -> new WatzPelletItem(new Item.Properties(), t, depleted)));
        }
        return map;
    }

    public static final RegistrySupplier<Item> HYDROTREATER = ITEMS.register("hydrotreater",
        () -> new MultiblockBlockItem(ModBlocks.HYDROTREATER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> CATALYTIC_REFORMER = ITEMS.register("catalytic_reformer",
        () -> new MultiblockBlockItem(ModBlocks.CATALYTIC_REFORMER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> DEUTERIUM_TOWER = ITEMS.register("deuterium_tower",
        () -> new MultiblockBlockItem(ModBlocks.DEUTERIUM_TOWER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> CHEMICAL_FACTORY = ITEMS.register("chemical_factory",
        () -> new MultiblockBlockItem(ModBlocks.CHEMICAL_FACTORY.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> ASSEMBLY_FACTORY = ITEMS.register("assembly_factory",
        () -> new MultiblockBlockItem(ModBlocks.ASSEMBLY_FACTORY.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> INTAKE = ITEMS.register("intake",
        () -> new MultiblockBlockItem(ModBlocks.INTAKE.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> STEAM_TURBINE = ITEMS.register("steam_turbine",
        () -> new MultiblockBlockItem(ModBlocks.STEAM_TURBINE.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> LIQUEFACTOR = ITEMS.register("liquefactor",
        () -> new MultiblockBlockItem(ModBlocks.LIQUEFACTOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> CORE_EMITTER = ITEMS.register("core_emitter",
        () -> new net.minecraft.world.item.BlockItem(ModBlocks.CORE_EMITTER.get(), new Item.Properties())); // Einzelblock, 6 Richtungen

    public static final RegistrySupplier<Item> CORE_INJECTOR = ITEMS.register("core_injector",
        () -> new net.minecraft.world.item.BlockItem(ModBlocks.CORE_INJECTOR.get(), new Item.Properties())); // Einzelblock, 6 Richtungen

    public static final RegistrySupplier<Item> CORE_RECEIVER = ITEMS.register("core_receiver",
        () -> new net.minecraft.world.item.BlockItem(ModBlocks.CORE_RECEIVER.get(), new Item.Properties())); // Einzelblock, 6 Richtungen

    public static final RegistrySupplier<Item> VACUUM_DISTILL = ITEMS.register("vacuum_distill",
        () -> new MultiblockBlockItem(ModBlocks.VACUUM_DISTILL.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> TURBOFAN = ITEMS.register("turbofan",
        () -> new MultiblockBlockItem(ModBlocks.TURBOFAN.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> INDUSTRIAL_TURBINE = ITEMS.register("industrial_turbine",
        () -> new MultiblockBlockItem(ModBlocks.INDUSTRIAL_TURBINE.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> MACHINE_CHUNGUS = ITEMS.register("machine_chungus",
        () -> new MultiblockBlockItem(ModBlocks.MACHINE_CHUNGUS.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SUBSTATION = ITEMS.register("substation",
        () -> new MultiblockBlockItem(ModBlocks.SUBSTATION.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> REFINERY = ITEMS.register("refinery",
        () -> new MultiblockBlockItem(ModBlocks.REFINERY.get(), new Item.Properties()));
	public static final RegistrySupplier<Item> LAUNCH_PAD = ITEMS.register("launch_pad",
        () -> new MultiblockBlockItem(ModBlocks.LAUNCH_PAD.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> LAUNCH_PAD_RUSTED = ITEMS.register("launch_pad_rusted",
        () -> new MultiblockBlockItem(ModBlocks.LAUNCH_PAD_RUSTED.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> MOBILE_LAUNCH_PAD = ITEMS.register("mobile_launch_pad",
        () -> new MultiblockBlockItem(ModBlocks.MOBILE_LAUNCH_PAD.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> TOPOL_LAUNCH_PAD = ITEMS.register("topol_launch_pad",
        () -> new MultiblockBlockItem(ModBlocks.TOPOL_LAUNCH_PAD.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> NUKE_FAT_MAN = ITEMS.register("nuke_fat_man",
        () -> new MultiblockBlockItem(ModBlocks.NUKE_FAT_MAN.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> NUKE_PROTOTYPE = ITEMS.register("nuke_prototype",
        () -> new net.minecraft.world.item.BlockItem(com.hbm_m.block.ModBlocks.NUKE_PROTOTYPE.get(), new Item.Properties()));

    // РџР РћРўРћРўРРџ Р РђРљР•РўР« (TIER 0, MICRO)
    public static final RegistrySupplier<Item> MISSILE_TEST = ITEMS.register("missile_test",
        () -> new MissileItem(MissileItem.MissileFormFactor.MICRO, MissileItem.MissileTier.TIER0,
                MissileItem.MissileFuel.SOLID));

    /** Original-ID missile_anti_ballistic (frueher Port-ID missile_abm, Umleitung in LEGACY_IDS). */
    public static final RegistrySupplier<Item> MISSILE_ABM = ITEMS.register("missile_anti_ballistic",
                () -> new MissileItem(MissileItem.MissileFormFactor.ABM, MissileItem.MissileTier.TIER1,
                                MissileItem.MissileFuel.SOLID));

  /** РЎРёРЅРіСѓР»СЏСЂРЅРѕСЃС‚Рё / РѕРїР°СЃРЅС‹Рµ РґСЂРѕРїС‹ (1.7.10 {@code ModItems.black_hole}, {@code pellet_antimatter}, {@code flame_pony}). */
    public static final RegistrySupplier<Item> BLACK_HOLE = ITEMS.register("black_hole", () -> new com.hbm_m.item.special.ItemDrop(new Item.Properties().stacksTo(1), () -> ModItems.NUCLEAR_WASTE.get()));
    public static final RegistrySupplier<Item> PELLET_ANTIMATTER = ITEMS.register("pellet_antimatter", () -> new com.hbm_m.item.special.ItemDrop(new Item.Properties(), () -> ModItems.CELL_EMPTY.get()));
    public static final RegistrySupplier<Item> FLAME_PONY = ITEMS.register("flame_pony",
            () -> new ItemCustomLore(new Item.Properties()));

    // Tier 0
    public static final RegistrySupplier<Item> MISSILE_MICRO = ITEMS.register("missile_micro",
            () -> new MissileItem(MissileItem.MissileFormFactor.MICRO, MissileItem.MissileTier.TIER0));
    public static final RegistrySupplier<Item> MISSILE_SCHRABIDIUM = ITEMS.register("missile_schrabidium",
            () -> new MissileItem(MissileItem.MissileFormFactor.MICRO, MissileItem.MissileTier.TIER0));
    public static final RegistrySupplier<Item> MISSILE_BHOLE = ITEMS.register("missile_bhole",
            () -> new MissileItem(MissileItem.MissileFormFactor.MICRO, MissileItem.MissileTier.TIER0));
    public static final RegistrySupplier<Item> MISSILE_TAINT = ITEMS.register("missile_taint",
            () -> new MissileItem(MissileItem.MissileFormFactor.MICRO, MissileItem.MissileTier.TIER0));
    public static final RegistrySupplier<Item> MISSILE_EMP = ITEMS.register("missile_emp",
            () -> new MissileItem(MissileItem.MissileFormFactor.MICRO, MissileItem.MissileTier.TIER0));

    // Tier 1
    public static final RegistrySupplier<Item> MISSILE_GENERIC = ITEMS.register("missile_generic",
            () -> new MissileItem(MissileItem.MissileFormFactor.V2, MissileItem.MissileTier.TIER1));
    public static final RegistrySupplier<Item> MISSILE_INCENDIARY = ITEMS.register("missile_incendiary",
            () -> new MissileItem(MissileItem.MissileFormFactor.V2, MissileItem.MissileTier.TIER1));
    public static final RegistrySupplier<Item> MISSILE_CLUSTER = ITEMS.register("missile_cluster",
            () -> new MissileItem(MissileItem.MissileFormFactor.V2, MissileItem.MissileTier.TIER1));
    public static final RegistrySupplier<Item> MISSILE_BUSTER = ITEMS.register("missile_buster",
            () -> new MissileItem(MissileItem.MissileFormFactor.V2, MissileItem.MissileTier.TIER1));
    public static final RegistrySupplier<Item> MISSILE_DECOY = ITEMS.register("missile_decoy",
            () -> new MissileItem(MissileItem.MissileFormFactor.V2, MissileItem.MissileTier.TIER1));

    public static final RegistrySupplier<Item> MISSILE_STEALTH = ITEMS.register("missile_stealth",
            () -> new MissileItem(MissileItem.MissileFormFactor.STEALTH, MissileItem.MissileTier.TIER1));

    // Tier 2
    public static final RegistrySupplier<Item> MISSILE_STRONG = ITEMS.register("missile_strong",
            () -> new MissileItem(MissileItem.MissileFormFactor.STRONG, MissileItem.MissileTier.TIER2));
    public static final RegistrySupplier<Item> MISSILE_INCENDIARY_STRONG = ITEMS.register("missile_incendiary_strong",
            () -> new MissileItem(MissileItem.MissileFormFactor.STRONG, MissileItem.MissileTier.TIER2));
    public static final RegistrySupplier<Item> MISSILE_CLUSTER_STRONG = ITEMS.register("missile_cluster_strong",
            () -> new MissileItem(MissileItem.MissileFormFactor.STRONG, MissileItem.MissileTier.TIER2));
    public static final RegistrySupplier<Item> MISSILE_BUSTER_STRONG = ITEMS.register("missile_buster_strong",
            () -> new MissileItem(MissileItem.MissileFormFactor.STRONG, MissileItem.MissileTier.TIER2));
    public static final RegistrySupplier<Item> MISSILE_EMP_STRONG = ITEMS.register("missile_emp_strong",
            () -> new MissileItem(MissileItem.MissileFormFactor.STRONG, MissileItem.MissileTier.TIER2));

    // Tier 3
    public static final RegistrySupplier<Item> MISSILE_BURST = ITEMS.register("missile_burst",
            () -> new MissileItem(MissileItem.MissileFormFactor.HUGE, MissileItem.MissileTier.TIER3));
    public static final RegistrySupplier<Item> MISSILE_INFERNO = ITEMS.register("missile_inferno",
            () -> new MissileItem(MissileItem.MissileFormFactor.HUGE, MissileItem.MissileTier.TIER3));
    public static final RegistrySupplier<Item> MISSILE_RAIN = ITEMS.register("missile_rain",
            () -> new MissileItem(MissileItem.MissileFormFactor.HUGE, MissileItem.MissileTier.TIER3));
    public static final RegistrySupplier<Item> MISSILE_DRILL = ITEMS.register("missile_drill",
            () -> new MissileItem(MissileItem.MissileFormFactor.HUGE, MissileItem.MissileTier.TIER3));

    // Iskander-M 9M723. Rumpf und Treibstoff folgen dem Tier-3-Schema des Spiels; die reale
    // Rakete ist feststoffgetrieben, das waere hier aber eine Tier-3-Rakete ohne Betankung.
    public static final RegistrySupplier<Item> MISSILE_9M723 = ITEMS.register("missile_9m723",
            () -> new MissileItem(MissileItem.MissileFormFactor.ISKANDER, MissileItem.MissileTier.TIER3));
    public static final RegistrySupplier<Item> MISSILE_9M723_BUSTER = ITEMS.register("missile_9m723_buster",
            () -> new MissileItem(MissileItem.MissileFormFactor.ISKANDER, MissileItem.MissileTier.TIER3));

    /** RT-2PM2 Topol-M: eigener Rumpf, 22,5 Bloecke hoch. */
    public static final RegistrySupplier<Item> MISSILE_TOPOL = ITEMS.register("missile_topol",
            () -> new MissileItem(MissileItem.MissileFormFactor.TOPOL, MissileItem.MissileTier.TIER4));
    public static final RegistrySupplier<Item> MISSILE_SHUTTLE = ITEMS.register("missile_shuttle",
            () -> new MissileItem(MissileItem.MissileFormFactor.OTHER, MissileItem.MissileTier.TIER3,
                    MissileItem.MissileFuel.KEROSENE_PEROXIDE));

    // Soyuz Launcher lander module (the rocket itself reuses ModBlocks.DECO_SOYUZ_ROCKET's item - see SoyuzLauncherBlockEntity.rocketItem())
    /** Original missile_soyuz (ItemSoyuz) Meta 0/1/2 = Skins Original, Luna Space Center, Post War. */
    /** Original missile_custom: Baukasten-Rakete aus der Raketenmontage (kein Kreativtab). */
    /** 1:1 ItemCustomMachine: Steuerung, Typ als machineType im NBT. */
    public static final RegistrySupplier<Item> CUSTOM_MACHINE = ITEMS.register("custom_machine",
            () -> new com.hbm_m.block.machines.custom.ItemCustomMachine(com.hbm_m.block.ModBlocks.CUSTOM_MACHINE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_CUSTOM = ITEMS.register("missile_custom",
            () -> new com.hbm_m.item.missile.ItemCustomMissile(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_SOYUZ = ITEMS.register("missile_soyuz",
            () -> new com.hbm_m.item.special.ItemSoyuz(0, new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_SOYUZ_LUNA = ITEMS.register("missile_soyuz_luna",
            () -> new com.hbm_m.item.special.ItemSoyuz(1, new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_SOYUZ_POSTWAR = ITEMS.register("missile_soyuz_postwar",
            () -> new com.hbm_m.item.special.ItemSoyuz(2, new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_SOYUZ_LANDER = ITEMS.register("missile_soyuz_lander",
            () -> new com.hbm_m.item.ItemCustomLore(new Item.Properties().stacksTo(1)));
    /** 1:1 {@code missile_lambda}: Lambda-Rakete fuer die Lambda-Startrampe. */
    public static final RegistrySupplier<Item> MISSILE_LAMBDA = ITEMS.register("missile_lambda",
            () -> new Item(new Item.Properties().stacksTo(1)));


    // Tier 4
    public static final RegistrySupplier<Item> MISSILE_NUCLEAR = ITEMS.register("missile_nuclear",
            () -> new MissileItem(MissileItem.MissileFormFactor.ATLAS, MissileItem.MissileTier.TIER4));
    public static final RegistrySupplier<Item> MISSILE_NUCLEAR_CLUSTER = ITEMS.register("missile_nuclear_cluster",
            () -> new MissileItem(MissileItem.MissileFormFactor.ATLAS, MissileItem.MissileTier.TIER4));
    public static final RegistrySupplier<Item> MISSILE_VOLCANO = ITEMS.register("missile_volcano",
            () -> new MissileItem(MissileItem.MissileFormFactor.ATLAS, MissileItem.MissileTier.TIER4));
    public static final RegistrySupplier<Item> MISSILE_DOOMSDAY = ITEMS.register("missile_doomsday",
            () -> new MissileItem(MissileItem.MissileFormFactor.ATLAS, MissileItem.MissileTier.TIER4));
    public static final RegistrySupplier<Item> MISSILE_DOOMSDAY_RUSTED = ITEMS.register("missile_doomsday_rusted",
            () -> new MissileItem(MissileItem.MissileFormFactor.ATLAS, MissileItem.MissileTier.TIER4).notLaunchable());

    public static final RegistrySupplier<Item> DESIGNATOR = ITEMS.register("designator",
        () -> new ItemDesignator(new Item.Properties()));
    public static final RegistrySupplier<Item> DESIGNATOR_RANGE = ITEMS.register("designator_range",
        () -> new ItemDesignatorRange(new Item.Properties()));
    public static final RegistrySupplier<Item> DESIGNATOR_MANUAL = ITEMS.register("designator_manual",
        () -> new ItemDesignatorManual(new Item.Properties()));

	// MULTIBLOCK DOORS

    public static final RegistrySupplier<Item> LARGE_VEHICLE_DOOR = ITEMS.register("large_vehicle_door",
        () -> new DoorBlockItem(ModBlocks.LARGE_VEHICLE_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> ROUND_AIRLOCK_DOOR = ITEMS.register("round_airlock_door",
        () -> new DoorBlockItem(ModBlocks.ROUND_AIRLOCK_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> TRANSITION_SEAL = ITEMS.register("transition_seal",
        () -> new MultiblockBlockItem(ModBlocks.TRANSITION_SEAL.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SILO_HATCH = ITEMS.register("silo_hatch",
        () -> new DoorBlockItem(ModBlocks.SILO_HATCH.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SILO_HATCH_LARGE = ITEMS.register("silo_hatch_large",
        () -> new DoorBlockItem(ModBlocks.SILO_HATCH_LARGE.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> QE_CONTAINMENT = ITEMS.register("qe_containment_door",
        () -> new DoorBlockItem(ModBlocks.QE_CONTAINMENT.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> WATER_DOOR = ITEMS.register("water_door",
        () -> new DoorBlockItem(ModBlocks.WATER_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> FIRE_DOOR = ITEMS.register("fire_door",
        () -> new DoorBlockItem(ModBlocks.FIRE_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SLIDE_DOOR = ITEMS.register("sliding_blast_door",
        () -> new DoorBlockItem(ModBlocks.SLIDE_DOOR.get(), new Item.Properties()));
        
    public static final RegistrySupplier<Item> SLIDING_SEAL_DOOR = ITEMS.register("sliding_seal_door",
        () -> new DoorBlockItem(ModBlocks.SLIDING_SEAL_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SECURE_ACCESS_DOOR = ITEMS.register("secure_access_door",
        () -> new DoorBlockItem(ModBlocks.SECURE_ACCESS_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> QE_SLIDING = ITEMS.register("qe_sliding_door",
        () -> new DoorBlockItem(ModBlocks.QE_SLIDING.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> VAULT_DOOR = ITEMS.register("vault_door",
        () -> new DoorBlockItem(ModBlocks.VAULT_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> CARGO_DOOR = ITEMS.register("cargo_door",
        () -> new DoorBlockItem(ModBlocks.CARGO_DOOR.get(), new Item.Properties()));



    public static final RegistrySupplier<Item> STAMP_STONE_FLAT = ITEMS.register("stamp_stone_flat",
            () -> new ItemStamp(new Item.Properties(), 32));
    public static final RegistrySupplier<Item> STAMP_STONE_PLATE = ITEMS.register("stamp_stone_plate",
            () -> new ItemStamp(new Item.Properties(), 32));
    public static final RegistrySupplier<Item> STAMP_STONE_WIRE = ITEMS.register("stamp_stone_wire",
            () -> new ItemStamp(new Item.Properties(), 32));
    public static final RegistrySupplier<Item> STAMP_STONE_CIRCUIT = ITEMS.register("stamp_stone_circuit",
            () -> new ItemStamp(new Item.Properties(), 32));


    public static final RegistrySupplier<Item> BLADE_TEST = ITEMS.register("blade_test",
            () -> new ItemBlades(new Item.Properties()));

    public static final RegistrySupplier<Item> BLADE_STEEL = ITEMS.register("blade_steel",
            () -> new ItemBlades(new Item.Properties(), 200));

    public static final RegistrySupplier<Item> BLADE_TITANIUM = ITEMS.register("blade_titanium",
            () -> new ItemBlades(new Item.Properties(), 350));

    public static final RegistrySupplier<Item> BLADE_ALLOY = ITEMS.register("blade_alloy",
            () -> new ItemBlades(new Item.Properties(), 700));

    // Р–РµР»РµР·РЅС‹Рµ С€С‚Р°РјРїС‹ (48 РёСЃРїРѕР»СЊР·РѕРІР°РЅРёР№)
    public static final RegistrySupplier<Item> STAMP_IRON_FLAT = ITEMS.register("stamp_iron_flat",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_PLATE = ITEMS.register("stamp_iron_plate",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_WIRE = ITEMS.register("stamp_iron_wire",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_CIRCUIT = ITEMS.register("stamp_iron_circuit",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_9 = ITEMS.register("stamp_iron_9",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_44 = ITEMS.register("stamp_iron_44",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_50 = ITEMS.register("stamp_iron_50",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_357 = ITEMS.register("stamp_iron_357",
            () -> new ItemStamp(new Item.Properties(), 48));

    // РЎС‚Р°Р»СЊРЅС‹Рµ С€С‚Р°РјРїС‹ (64 РёСЃРїРѕР»СЊР·РѕРІР°РЅРёСЏ)
    public static final RegistrySupplier<Item> STAMP_STEEL_FLAT = ITEMS.register("stamp_steel_flat",
            () -> new ItemStamp(new Item.Properties(), 64));
    public static final RegistrySupplier<Item> STAMP_STEEL_PLATE = ITEMS.register("stamp_steel_plate",
            () -> new ItemStamp(new Item.Properties(), 64));
    public static final RegistrySupplier<Item> STAMP_STEEL_WIRE = ITEMS.register("stamp_steel_wire",
            () -> new ItemStamp(new Item.Properties(), 64));
    public static final RegistrySupplier<Item> STAMP_STEEL_CIRCUIT = ITEMS.register("stamp_steel_circuit",
            () -> new ItemStamp(new Item.Properties(), 64));

    // РўРёС‚Р°РЅРѕРІС‹Рµ С€С‚Р°РјРїС‹ (80 РёСЃРїРѕР»СЊР·РѕРІР°РЅРёР№)
    public static final RegistrySupplier<Item> STAMP_TITANIUM_FLAT = ITEMS.register("stamp_titanium_flat",
            () -> new ItemStamp(new Item.Properties(), 80));
    public static final RegistrySupplier<Item> STAMP_TITANIUM_PLATE = ITEMS.register("stamp_titanium_plate",
            () -> new ItemStamp(new Item.Properties(), 80));
    public static final RegistrySupplier<Item> STAMP_TITANIUM_WIRE = ITEMS.register("stamp_titanium_wire",
            () -> new ItemStamp(new Item.Properties(), 80));
    public static final RegistrySupplier<Item> STAMP_TITANIUM_CIRCUIT = ITEMS.register("stamp_titanium_circuit",
            () -> new ItemStamp(new Item.Properties(), 80));

    // РћР±СЃРёРґРёР°РЅРѕРІС‹Рµ С€С‚Р°РјРїС‹ (96 РёСЃРїРѕР»СЊР·РѕРІР°РЅРёР№)
    public static final RegistrySupplier<Item> STAMP_OBSIDIAN_FLAT = ITEMS.register("stamp_obsidian_flat",
            () -> new ItemStamp(new Item.Properties(), 96));
    public static final RegistrySupplier<Item> STAMP_OBSIDIAN_PLATE = ITEMS.register("stamp_obsidian_plate",
            () -> new ItemStamp(new Item.Properties(), 96));
    public static final RegistrySupplier<Item> STAMP_OBSIDIAN_WIRE = ITEMS.register("stamp_obsidian_wire",
            () -> new ItemStamp(new Item.Properties(), 96));
    public static final RegistrySupplier<Item> STAMP_OBSIDIAN_CIRCUIT = ITEMS.register("stamp_obsidian_circuit",
            () -> new ItemStamp(new Item.Properties(), 96));

    // Desh С€С‚Р°РјРїС‹ (Р±РµСЃРєРѕРЅРµС‡РЅР°СЏ РїСЂРѕС‡РЅРѕСЃС‚СЊ)
    public static final RegistrySupplier<Item> STAMP_DESH_FLAT = ITEMS.register("stamp_desh_flat",
            () -> new ItemStamp(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> STAMP_DESH_PLATE = ITEMS.register("stamp_desh_plate",
            () -> new ItemStamp(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> STAMP_DESH_WIRE = ITEMS.register("stamp_desh_wire",
            () -> new ItemStamp(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> STAMP_DESH_CIRCUIT = ITEMS.register("stamp_desh_circuit",
            () -> new ItemStamp(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> STAMP_DESH_9 = ITEMS.register("stamp_desh_9",
            () -> new ItemStamp(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> STAMP_DESH_44 = ITEMS.register("stamp_desh_44",
            () -> new ItemStamp(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> STAMP_DESH_50 = ITEMS.register("stamp_desh_50",
            () -> new ItemStamp(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> STAMP_DESH_357 = ITEMS.register("stamp_desh_357",
            () -> new ItemStamp(new Item.Properties().stacksTo(1)));


    //Р±Р°С‚Р°СЂРµР№РєРё

    public static final RegistrySupplier<Item> BATTERY_SCHRABIDIUM = ITEMS.register("battery_schrabidium",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    1000000,
                    5000,
                    5000
            ));

    // ========== РљРђР РўРћР¤Р•Р›Р¬РќРђРЇ Р Р‘РђР—РћР’Р«Р• ==========
    public static final RegistrySupplier<Item> BATTERY_POTATO = ITEMS.register("battery_potato",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    1_000,
                    100,
                    100
            ));

    public static final RegistrySupplier<Item> BATTERY = ITEMS.register("battery",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    5000,
                    100,
                    100
            ));

    // ========== РљР РђРЎРќР«Р• Р‘РђРўРђР Р•Р™РљР (RED CELL) ==========
    public static final RegistrySupplier<Item> BATTERY_RED_CELL = ITEMS.register("battery_red_cell",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    15000,
                    100,
                    100
            ));

    public static final RegistrySupplier<Item> BATTERY_RED_CELL_6 = ITEMS.register("battery_red_cell_6",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    90000,
                    100,
                    100
            ));

    public static final RegistrySupplier<Item> BATTERY_RED_CELL_24 = ITEMS.register("battery_red_cell_24",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    240000,
                    100,
                    100
            ));

    public static final RegistrySupplier<Item> BATTERY_ADVANCED = ITEMS.register("battery_advanced",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    20000,
                    500,
                    500
            ));

    public static final RegistrySupplier<Item> BATTERY_ADVANCED_CELL = ITEMS.register("battery_advanced_cell",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    60000,
                    500,
                    500
            ));

    public static final RegistrySupplier<Item> BATTERY_ADVANCED_CELL_4 = ITEMS.register("battery_advanced_cell_4",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    240000,
                    500,
                    500
            ));

    public static final RegistrySupplier<Item> BATTERY_ADVANCED_CELL_12 = ITEMS.register("battery_advanced_cell_12",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    720000,
                    500,
                    500
            ));

    // ========== Р›РРўРР•Р’Р«Р• Р‘РђРўРђР Р•Р™РљР (LITHIUM) ==========
    /** Original battery_pack (ItemBatteryPack, Meta = EnumBatteryPack) als Einzelgegenstaende. */
    public static final RegistrySupplier<Item> BATTERY_PACK_BATTERY_REDSTONE = ITEMS.register("battery_pack_battery_redstone", () -> new com.hbm_m.item.fekal_electric.ItemBatteryPack(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatteryPack.EnumBatteryPack.BATTERY_REDSTONE));
    public static final RegistrySupplier<Item> BATTERY_PACK_BATTERY_LEAD = ITEMS.register("battery_pack_battery_lead", () -> new com.hbm_m.item.fekal_electric.ItemBatteryPack(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatteryPack.EnumBatteryPack.BATTERY_LEAD));
    public static final RegistrySupplier<Item> BATTERY_PACK_BATTERY_LITHIUM = ITEMS.register("battery_pack_battery_lithium", () -> new com.hbm_m.item.fekal_electric.ItemBatteryPack(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatteryPack.EnumBatteryPack.BATTERY_LITHIUM));
    public static final RegistrySupplier<Item> BATTERY_PACK_BATTERY_SODIUM = ITEMS.register("battery_pack_battery_sodium", () -> new com.hbm_m.item.fekal_electric.ItemBatteryPack(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatteryPack.EnumBatteryPack.BATTERY_SODIUM));
    public static final RegistrySupplier<Item> BATTERY_PACK_BATTERY_SCHRABIDIUM = ITEMS.register("battery_pack_battery_schrabidium", () -> new com.hbm_m.item.fekal_electric.ItemBatteryPack(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatteryPack.EnumBatteryPack.BATTERY_SCHRABIDIUM));
    public static final RegistrySupplier<Item> BATTERY_PACK_BATTERY_QUANTUM = ITEMS.register("battery_pack_battery_quantum", () -> new com.hbm_m.item.fekal_electric.ItemBatteryPack(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatteryPack.EnumBatteryPack.BATTERY_QUANTUM));
    public static final RegistrySupplier<Item> BATTERY_PACK_CAPACITOR_COPPER = ITEMS.register("battery_pack_capacitor_copper", () -> new com.hbm_m.item.fekal_electric.ItemBatteryPack(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatteryPack.EnumBatteryPack.CAPACITOR_COPPER));
    public static final RegistrySupplier<Item> BATTERY_PACK_CAPACITOR_GOLD = ITEMS.register("battery_pack_capacitor_gold", () -> new com.hbm_m.item.fekal_electric.ItemBatteryPack(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatteryPack.EnumBatteryPack.CAPACITOR_GOLD));
    public static final RegistrySupplier<Item> BATTERY_PACK_CAPACITOR_NIOBIUM = ITEMS.register("battery_pack_capacitor_niobium", () -> new com.hbm_m.item.fekal_electric.ItemBatteryPack(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatteryPack.EnumBatteryPack.CAPACITOR_NIOBIUM));
    public static final RegistrySupplier<Item> BATTERY_PACK_CAPACITOR_TANTALUM = ITEMS.register("battery_pack_capacitor_tantalum", () -> new com.hbm_m.item.fekal_electric.ItemBatteryPack(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatteryPack.EnumBatteryPack.CAPACITOR_TANTALUM));
    public static final RegistrySupplier<Item> BATTERY_PACK_CAPACITOR_BISMUTH = ITEMS.register("battery_pack_capacitor_bismuth", () -> new com.hbm_m.item.fekal_electric.ItemBatteryPack(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatteryPack.EnumBatteryPack.CAPACITOR_BISMUTH));
    public static final RegistrySupplier<Item> BATTERY_PACK_CAPACITOR_SPARK = ITEMS.register("battery_pack_capacitor_spark", () -> new com.hbm_m.item.fekal_electric.ItemBatteryPack(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatteryPack.EnumBatteryPack.CAPACITOR_SPARK));
    public static final RegistrySupplier<Item> BATTERY_LITHIUM = ITEMS.register("battery_lithium",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    250000,
                    1000,
                    1000
            ));

    public static final RegistrySupplier<Item> BATTERY_LITHIUM_CELL = ITEMS.register("battery_lithium_cell",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    750000,
                    1000,
                    1000
            ));

    public static final RegistrySupplier<Item> BATTERY_LITHIUM_CELL_3 = ITEMS.register("battery_lithium_cell_3",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    2250000,
                    1000,
                    1000
            ));

    public static final RegistrySupplier<Item> BATTERY_LITHIUM_CELL_6 = ITEMS.register("battery_lithium_cell_6",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    4500000,
                    1000,
                    1000
            ));

// ========== РЁР РђР‘РР”РР•Р’Р«Р• Р‘РђРўРђР Р•Р™РљР (SCHRABIDIUM) - СѓР¶Рµ РµСЃС‚СЊ ==========

    public static final RegistrySupplier<Item> BATTERY_SCHRABIDIUM_CELL = ITEMS.register("battery_schrabidium_cell",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    3000000,
                    5000,
                    5000
            ));

    public static final RegistrySupplier<Item> BATTERY_SCHRABIDIUM_CELL_2 = ITEMS.register("battery_schrabidium_cell_2",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    6000000,
                    5000,
                    5000
            ));

    public static final RegistrySupplier<Item> BATTERY_SCHRABIDIUM_CELL_4 = ITEMS.register("battery_schrabidium_cell_4",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    12000000,
                    5000,
                    5000
            ));

    // ========== РРЎРљР РћР’Р«Р• Р‘РђРўРђР Р•Р™РљР (SPARK) - Р­РљРЎРўР Р•РњРђР›Р¬РќР«Р• ==========
    public static final RegistrySupplier<Item> BATTERY_SPARK = ITEMS.register("battery_spark",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    100000000,
                    2000000,
                    2000000
            ));

    public static final RegistrySupplier<Item> BATTERY_TRIXITE = ITEMS.register("battery_trixite",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    5000000,
                    40000,
                    200000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_6 = ITEMS.register("battery_spark_cell_6",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    600_000_000L,
                    2000000,
                    2000000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_25 = ITEMS.register("battery_spark_cell_25",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    2_500_000_000L,
                    2000000,
                    2000000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_100 = ITEMS.register("battery_spark_cell_100",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    10_000_000_000L,
                    20000000,
                    2000000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_1000 = ITEMS.register("battery_spark_cell_1000",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    100_000_000_000L,
                    20000000,
                    20000000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_2500 = ITEMS.register("battery_spark_cell_2500",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    250_000_000_000L,
                    20000000,
                    20000000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_10000 = ITEMS.register("battery_spark_cell_10000",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    1_000_000_000_000L,
                    200000000,
                    200000000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_POWER = ITEMS.register("battery_spark_cell_power",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    100_000_000_000_000L,
                    200000000,
                    200000000
            ));


    public static final RegistrySupplier<Item> AIRSTRIKE_TEST = ITEMS.register("airstrike_test",
            () -> new AirstrikeItem(new Item.Properties(), AirstrikeType.NORMAL));
    public static final RegistrySupplier<Item> AIRSTRIKE_AGENT= ITEMS.register("airstrike_agent",
            () -> new AirstrikeItem(new Item.Properties(), AirstrikeType.AGENT));
    public static final RegistrySupplier<Item> AIRSTRIKE_HEAVY = ITEMS.register("airstrike_heavy",
            () -> new AirstrikeItem(new Item.Properties(), AirstrikeType.HEAVY));
    public static final RegistrySupplier<Item> AIRSTRIKE_NUKE = ITEMS.register("airstrike_nuke",
            () -> new AirstrikeItem(new Item.Properties(), AirstrikeType.NUKE));
    public static final RegistrySupplier<Item> WIRE_FINE = ITEMS.register("wire_fine",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> SAT_HEAD_LASER = ITEMS.register("sat_head_laser",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_BASE = ITEMS.register("sat_base",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_LASER = ITEMS.register("sat_laser",
            () -> new com.hbm_m.item.satellite.ItemSatChip(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_HEAD_RADAR = ITEMS.register("sat_head_radar",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_RADAR = ITEMS.register("sat_radar",
            () -> new com.hbm_m.item.satellite.ItemSatChip(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_HEAD_MAPPER = ITEMS.register("sat_head_mapper",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_MAPPER = ITEMS.register("sat_mapper",
            () -> new com.hbm_m.item.satellite.ItemSatChip(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_HEAD_RESONATOR = ITEMS.register("sat_head_resonator",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_RESONATOR = ITEMS.register("sat_resonator",
            () -> new com.hbm_m.item.satellite.ItemSatChip(new Item.Properties()));

    public static final RegistrySupplier<Item> THRUSTER_LARGE         = ITEMS.register("thruster_large",         () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUEL_TANK_LARGE        = ITEMS.register("fuel_tank_large",        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_NUCLEAR        = ITEMS.register("warhead_nuclear",        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_ASSEMBLY           = ITEMS.register("missile_assembly",           () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> INGOT_TUNGSTEN_CARBIDE     = ITEMS.register("ingot_tungsten_carbide",     () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_HIGHSPEED_STEEL      = ITEMS.register("ingot_highspeed_steel",      () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NEUTRON_REFLECTOR          = ITEMS.register("neutron_reflector",          () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_GENERIC_SMALL      = ITEMS.register("warhead_generic_small",      () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_CLUSTER_LARGE      = ITEMS.register("warhead_cluster_large",      () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_INCENDIARY_MEDIUM  = ITEMS.register("warhead_incendiary_medium",  () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_BUSTER_SMALL       = ITEMS.register("warhead_buster_small",       () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> THRUSTER_SMALL          = ITEMS.register("thruster_small",          () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUEL_TANK_SMALL         = ITEMS.register("fuel_tank_small",         () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_CLUSTER_SMALL      = ITEMS.register("warhead_cluster_small",      () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_INCENDIARY_SMALL   = ITEMS.register("warhead_incendiary_small",   () -> new Item(new Item.Properties()));
    // low_density_element: doppelte Port-ID von part_generic@LDE (= part_generic_lde), Umleitung in LEGACY_IDS
    public static final RegistrySupplier<Item> THRUSTER_MEDIUM         = ITEMS.register("thruster_medium",         () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUEL_TANK_MEDIUM        = ITEMS.register("fuel_tank_medium",        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_GENERIC_MEDIUM  = ITEMS.register("warhead_generic_medium",  () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_GENERIC_LARGE   = ITEMS.register("warhead_generic_large",   () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_BUSTER_LARGE    = ITEMS.register("warhead_buster_large",    () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_MIRV            = ITEMS.register("warhead_mirv",            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_VOLCANO         = ITEMS.register("warhead_volcano",         () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_BUSTER_MEDIUM   = ITEMS.register("warhead_buster_medium",   () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_CLUSTER_MEDIUM  = ITEMS.register("warhead_cluster_medium",  () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> SCREWDRIVER = ITEMS.register("screwdriver",
            () -> new ScrewdriverItem(100, new Item.Properties()));

	// РњРµРґР»РµРЅРЅС‹Р№ РёСЃС‚РѕС‡РЅРёРє (500 mB/t)
	public static final RegistrySupplier<Item> INFINITE_WATER_500 = ITEMS.register("inf_water",
					() -> new InfiniteFluidItem(new Item.Properties().stacksTo(1), net.minecraft.world.level.material.Fluids.WATER, 500));

	// Р‘С‹СЃС‚СЂС‹Р№ РёСЃС‚РѕС‡РЅРёРє (5000 mB/t)
	public static final RegistrySupplier<Item> INFINITE_WATER_5000 = ITEMS.register("inf_water_mk2",
            () -> new InfiniteFluidItem(new Item.Properties().stacksTo(1), net.minecraft.world.level.material.Fluids.WATER, 5000));

    // ---- R5: Fluessigkeitsbehaelter 1:1 (FluidContainerRegistry); die Fluessigkeit steht im NBT "type" ----
    public static final RegistrySupplier<Item> FLUID_TANK_EMPTY = ITEMS.register("fluid_tank_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLUID_TANK_FULL = ITEMS.register("fluid_tank_full", () -> new com.hbm_m.item.liquids.ItemFluidTank(new Item.Properties(), () -> ModItems.FLUID_TANK_EMPTY.get()));
    public static final RegistrySupplier<Item> FLUID_TANK_LEAD_EMPTY = ITEMS.register("fluid_tank_lead_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLUID_TANK_LEAD_FULL = ITEMS.register("fluid_tank_lead_full", () -> new com.hbm_m.item.liquids.ItemFluidTank(new Item.Properties(), () -> ModItems.FLUID_TANK_LEAD_EMPTY.get()));
    public static final RegistrySupplier<Item> FLUID_BARREL_EMPTY = ITEMS.register("fluid_barrel_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLUID_BARREL_FULL = ITEMS.register("fluid_barrel_full", () -> new com.hbm_m.item.liquids.ItemFluidTank(new Item.Properties(), () -> ModItems.FLUID_BARREL_EMPTY.get()));
    public static final RegistrySupplier<Item> FLUID_PACK_EMPTY = ITEMS.register("fluid_pack_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLUID_PACK_FULL = ITEMS.register("fluid_pack_full", () -> new com.hbm_m.item.liquids.ItemFluidTank(new Item.Properties(), () -> ModItems.FLUID_PACK_EMPTY.get()));
    public static final RegistrySupplier<Item> CANISTER_FULL = ITEMS.register("canister_full", () -> new com.hbm_m.item.liquids.ItemCanister(new Item.Properties(), () -> ModItems.CANISTER_EMPTY.get()));
    public static final RegistrySupplier<Item> GAS_FULL = ITEMS.register("gas_full", () -> new com.hbm_m.item.liquids.ItemGasTank(new Item.Properties(), () -> ModItems.GAS_EMPTY.get()));
    public static final RegistrySupplier<Item> DISPERSER_CANISTER_EMPTY = ITEMS.register("disperser_canister_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GLYPHID_GLAND_EMPTY = ITEMS.register("glyphid_gland_empty", () -> new Item(new Item.Properties()));

    // Universal infinite fluid source (any fluid type, 1B mB/t - like 1.7.10 fluid_barrel_infinite)
    public static final RegistrySupplier<Item> FLUID_BARREL_INFINITE = ITEMS.register("fluid_barrel_infinite",
            () -> new InfiniteFluidItem(new Item.Properties().stacksTo(1), 1_000_000_000));


    // Universal fluid identifier - two fluid slots, Shift+RMB opens selection GUI
    public static final RegistrySupplier<Item> FLUID_IDENTIFIER = ITEMS.register("fluid_identifier_multi",
            () -> new FluidIdentifierItem(new Item.Properties().stacksTo(1)));

    // Mineral Pipes - individual pipe items per mineral, all using pipe.png with color tinting
    public static final RegistrySupplier<Item> PIPE_IRON = ITEMS.register("pipe_iron",
            () -> new MineralPipeItem(new Item.Properties(), 0xD8D8D8));
    public static final RegistrySupplier<Item> PIPE_COPPER = ITEMS.register("pipe_copper",
            () -> new MineralPipeItem(new Item.Properties(), 0xE77C56));
    public static final RegistrySupplier<Item> PIPE_GOLD = ITEMS.register("pipe_gold",
            () -> new MineralPipeItem(new Item.Properties(), 0xFCEE4B));
    public static final RegistrySupplier<Item> PIPE_LEAD = ITEMS.register("pipe_lead",
            () -> new MineralPipeItem(new Item.Properties(), 0x414166));
    public static final RegistrySupplier<Item> PIPE_STEEL = ITEMS.register("pipe_steel",
            () -> new MineralPipeItem(new Item.Properties(), 0x767676));
    public static final RegistrySupplier<Item> PIPE_TUNGSTEN = ITEMS.register("pipe_tungsten",
            () -> new MineralPipeItem(new Item.Properties(), 0x3D3D3D));
    public static final RegistrySupplier<Item> PIPE_TITANIUM = ITEMS.register("pipe_titanium",
            () -> new MineralPipeItem(new Item.Properties(), 0x8DC5E2));
    public static final RegistrySupplier<Item> PIPE_ALUMINUM = ITEMS.register("pipe_aluminum",
            () -> new MineralPipeItem(new Item.Properties(), 0xC5C5DE));
    public static final RegistrySupplier<Item> PIPE_DURA_STEEL = ITEMS.register("pipe_dura_steel",
            () -> new MineralPipeItem(new Item.Properties(), 0x82A59C));

    // Fluid Duct - pipe per fluid type, overlay tinted with fluid color (like fluid barrel)
    public static final RegistrySupplier<Item> FLUID_DUCT = ITEMS.register("fluid_duct",
            () -> new FluidDuctItem(new Item.Properties(), ModBlocks.FLUID_DUCT,
                    "item.hbm_m.fluid_duct", "item.hbm_m.fluid_duct.empty"));
    public static final RegistrySupplier<Item> FLUID_DUCT_COLORED = ITEMS.register("fluid_duct_colored",
            () -> new FluidDuctItem(new Item.Properties(), ModBlocks.FLUID_DUCT_COLORED,
                    "item.hbm_m.fluid_duct_colored", "item.hbm_m.fluid_duct_colored.empty"));
    public static final RegistrySupplier<Item> FLUID_DUCT_SILVER = ITEMS.register("fluid_duct_silver",
            () -> new FluidDuctItem(new Item.Properties(), ModBlocks.FLUID_DUCT_SILVER,
                    "item.hbm_m.fluid_duct_silver", "item.hbm_m.fluid_duct_silver.empty"));

    public static final RegistrySupplier<Item> FLUID_VALVE = ITEMS.register("fluid_valve",
            () -> new BlockItem(ModBlocks.FLUID_VALVE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FLUID_PUMP = ITEMS.register("fluid_pump",
            () -> new BlockItem(ModBlocks.FLUID_PUMP.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FLUID_EXHAUST = ITEMS.register("fluid_exhaust",
            () -> new BlockItem(ModBlocks.FLUID_EXHAUST.get(), new Item.Properties()));

    //=============================== Р’РЃР”Р Рђ Р”Р›РЇ Р–РР”РљРћРЎРўР•Р™ ===============================//

//    public static final RegistrySupplier<Item> CRUDE_OIL_BUCKET = ITEMS.register("bucket_crude_oil",
//            () -> new BucketItem(
//                    () -> ModFluids.CRUDE_OIL.source.get(),
//                    new Item.Properties()
//                            .craftRemainder(Items.BUCKET)
//                            .stacksTo(1)));


    // в”Ђв”Ђв”Ђ RBMK Items в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    public static final RegistrySupplier<Item> RBMK_LID = ITEMS.register("rbmk_lid",
            () -> new RBMKLidItem(1, new Item.Properties()));

    public static final RegistrySupplier<Item> RBMK_LID_GLASS = ITEMS.register("rbmk_lid_glass",
            () -> new RBMKLidItem(2, new Item.Properties()));

    // Pellets - stats mirror the matching rod's, 1:1 with the original's ItemRBMKRod definitions
    // in ModItems.java (see RBMKRodItem's class doc / this port's convention of duplicating stats
    // onto the pellet rather than deriving the rod from it).
    public static final RegistrySupplier<Item> RBMK_PELLET_HEU235 = ITEMS.register("rbmk_pellet_heu235",
            () -> new RBMKPelletItem(new Item.Properties())
                    .setFullName("High-Enriched Uranium-235").setYield(100_000_000).setReactivity(50)
                    .setMeltingPoint(2865).setTint(0x868D82));

    public static final RegistrySupplier<Item> RBMK_PELLET_LEP = ITEMS.register("rbmk_pellet_lep",
            () -> new RBMKPelletItem(new Item.Properties())
                    .setFullName("Low-Enriched Plutonium").setYield(100_000_000).setReactivity(35)
                    .setHeat(0.75).setMeltingPoint(2744).setTint(0x656E6B));

    public static final RegistrySupplier<Item> RBMK_PELLET_HEP = ITEMS.register("rbmk_pellet_hep239",
            () -> new RBMKPelletItem(new Item.Properties())
                    .setFullName("High-Enriched Plutonium-239").setYield(100_000_000).setReactivity(30)
                    .setHeat(1.25).setMeltingPoint(2744).setTint(0x656E6B));

    public static final RegistrySupplier<Item> RBMK_PELLET_MOX = ITEMS.register("rbmk_pellet_mox",
            () -> new RBMKPelletItem(new Item.Properties())
                    .setFullName("Mixed MEU & LEP Oxide").setYield(100_000_000).setReactivity(40)
                    .setMeltingPoint(2815).setTint(0x868D82));

    // Fuel Rods (assembled from pellets) - 1:1 port of the original's ItemRBMKRod stat blocks.
    public static final RegistrySupplier<Item> RBMK_FUEL_HEU235 = ITEMS.register("rbmk_fuel_heu235",
            () -> new RBMKRodItem("High-Enriched Uranium-235 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(50).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setMeltingPoint(2865).setTint(0x868D82).setPellet(() -> ModItems.RBMK_PELLET_HEU235.get()));

    public static final RegistrySupplier<Item> RBMK_FUEL_LEP = ITEMS.register("rbmk_fuel_lep",
            () -> new RBMKRodItem("Low-Enriched Plutonium Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(35).setFunction(RBMKRodItem.EnumBurnFunc.LOG_TEN)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.RAISING_SLOPE)
                    .setHeat(0.75).setMeltingPoint(2744).setTint(0x656E6B).setPellet(() -> ModItems.RBMK_PELLET_LEP.get()));

    public static final RegistrySupplier<Item> RBMK_FUEL_HEP = ITEMS.register("rbmk_fuel_hep",
            () -> new RBMKRodItem("High-Enriched Plutonium-239 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(30).setFunction(RBMKRodItem.EnumBurnFunc.LINEAR)
                    .setHeat(1.25).setMeltingPoint(2744).setTint(0x656E6B).setPellet(() -> ModItems.RBMK_PELLET_HEP.get()));

    public static final RegistrySupplier<Item> RBMK_FUEL_MOX = ITEMS.register("rbmk_fuel_mox",
            () -> new RBMKRodItem("Mixed Oxide Fuel Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(40).setFunction(RBMKRodItem.EnumBurnFunc.LOG_TEN)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.RAISING_SLOPE)
                    .setMeltingPoint(2815).setTint(0x868D82).setPellet(() -> ModItems.RBMK_PELLET_MOX.get()));

    public static final RegistrySupplier<Item> RBMK_FUEL_EMPTY = ITEMS.register("rbmk_fuel_empty",
            () -> new Item(new Item.Properties()));

    // в•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђ
    // DEV: importierte fehlende Items aus dem Original-HBM (zur Sichtung)
    // в•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђ
    public static final RegistrySupplier<Item> ACETYLENE_TORCH = ITEMS.register("acetylene_torch", () -> new com.hbm_m.item.tool.ItemBlowtorch(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY = ITEMS.register("ammo_arty", () -> new com.hbm_m.item.weapon.ItemAmmoArty(0, new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_CARGO = ITEMS.register("ammo_arty_cargo", () -> new com.hbm_m.item.weapon.ItemAmmoArty(8, new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_CHLORINE = ITEMS.register("ammo_arty_chlorine", () -> new com.hbm_m.item.weapon.ItemAmmoArty(9, new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_CLASSIC = ITEMS.register("ammo_arty_classic", () -> new com.hbm_m.item.weapon.ItemAmmoArty(1, new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_HE = ITEMS.register("ammo_arty_he", () -> new com.hbm_m.item.weapon.ItemAmmoArty(2, new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_MINI_NUKE = ITEMS.register("ammo_arty_mini_nuke", () -> new com.hbm_m.item.weapon.ItemAmmoArty(3, new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_MINI_NUKE_MULTI = ITEMS.register("ammo_arty_mini_nuke_multi", () -> new com.hbm_m.item.weapon.ItemAmmoArty(6, new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_MUSTARD_GAS = ITEMS.register("ammo_arty_mustard_gas", () -> new com.hbm_m.item.weapon.ItemAmmoArty(11, new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_NUKE = ITEMS.register("ammo_arty_nuke", () -> new com.hbm_m.item.weapon.ItemAmmoArty(4, new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_PHOSGENE = ITEMS.register("ammo_arty_phosgene", () -> new com.hbm_m.item.weapon.ItemAmmoArty(10, new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_PHOSPHORUS = ITEMS.register("ammo_arty_phosphorus", () -> new com.hbm_m.item.weapon.ItemAmmoArty(5, new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_PHOSPHORUS_MULTI = ITEMS.register("ammo_arty_phosphorus_multi", () -> new com.hbm_m.item.weapon.ItemAmmoArty(7, new Item.Properties()));
    // SEDNA: Original ItemAmmoBag (8 Plaetze, GUIAmmoBag)
    public static final RegistrySupplier<Item> AMMO_BAG = ITEMS.register("ammo_bag", () -> new com.hbm_m.item.tool.ItemAmmoBag(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_BAG_INFINITE = ITEMS.register("ammo_bag_infinite", () -> new com.hbm_m.item.tool.ItemAmmoBag(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_CONTAINER = ITEMS.register("ammo_container", () -> new com.hbm_m.item.tool.ItemAmmoContainer(false, new Item.Properties()));
    /** {@code ItemAmmoContainer} Meta 1 (Behelfskiste, Textur ammo_container_alt). */
    public static final RegistrySupplier<Item> AMMO_CONTAINER_1 = ITEMS.register("ammo_container_1", () -> new com.hbm_m.item.tool.ItemAmmoContainer(true, new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_DGK = ITEMS.register("ammo_dgk", () -> new Item(new Item.Properties().stacksTo(4)));
    public static final RegistrySupplier<Item> AMMO_FIREEXT = ITEMS.register("ammo_fireext", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_FIREEXT_FOAM = ITEMS.register("ammo_fireext_foam", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_FIREEXT_SAND = ITEMS.register("ammo_fireext_sand", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_SHELL = ITEMS.register("ammo_shell", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_SHELL_APFSDS_DU = ITEMS.register("ammo_shell_apfsds_du", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_SHELL_APFSDS_T = ITEMS.register("ammo_shell_apfsds_t", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_SHELL_EXPLOSIVE = ITEMS.register("ammo_shell_explosive", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_SHELL_W9 = ITEMS.register("ammo_shell_w9", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_ALUMINIUM = ITEMS.register("ams_catalyst_aluminium",
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0xCCCCCC, 1_000_000L, 1.15F, 0.85F, 1.15F));
    public static final RegistrySupplier<Item> AMS_CATALYST_BERYLLIUM = ITEMS.register("ams_catalyst_beryllium",
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0x97978B, 0L, 1.25F, 0.95F, 1.05F));
    public static final RegistrySupplier<Item> AMS_CATALYST_BLANK = ITEMS.register("ams_catalyst_blank", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> AMS_CATALYST_CAESIUM = ITEMS.register("ams_catalyst_caesium",
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0x6400FF, 2_500_000L, 1.00F, 0.85F, 1.15F));
    public static final RegistrySupplier<Item> AMS_CATALYST_CERIUM = ITEMS.register("ams_catalyst_cerium",
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0x1D3FFF, 1_000_000L, 1.15F, 1.15F, 0.85F));
    public static final RegistrySupplier<Item> AMS_CATALYST_COBALT = ITEMS.register("ams_catalyst_cobalt",
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0x789BBE, 0L, 1.25F, 1.05F, 0.95F));
    public static final RegistrySupplier<Item> AMS_CATALYST_COPPER = ITEMS.register("ams_catalyst_copper",
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0xAADE29, 0L, 1.25F, 1.00F, 1.00F));
    public static final RegistrySupplier<Item> AMS_CATALYST_DINEUTRONIUM = ITEMS.register("ams_catalyst_dineutronium",
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0x334077, 2_500_000L, 1.00F, 1.15F, 0.85F));
    public static final RegistrySupplier<Item> AMS_CATALYST_EUPHEMIUM = ITEMS.register("ams_catalyst_euphemium",
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0xFF9CD2, 2_500_000L, 1.00F, 1.00F, 1.00F));
    public static final RegistrySupplier<Item> AMS_CATALYST_IRON = ITEMS.register("ams_catalyst_iron",
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0xFF7E22, 1_000_000L, 1.15F, 0.95F, 1.05F));
    public static final RegistrySupplier<Item> AMS_CATALYST_LITHIUM = ITEMS.register("ams_catalyst_lithium",
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0xFF2727, 0L, 1.25F, 0.85F, 1.15F));
    public static final RegistrySupplier<Item> AMS_CATALYST_NIOBIUM = ITEMS.register("ams_catalyst_niobium",
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0x3BF1B6, 1_000_000L, 1.15F, 1.05F, 0.95F));
    public static final RegistrySupplier<Item> AMS_CATALYST_SCHRABIDIUM = ITEMS.register("ams_catalyst_schrabidium",
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0x32FFFF, 2_500_000L, 1.00F, 1.05F, 0.95F));
    public static final RegistrySupplier<Item> AMS_CATALYST_STRONTIUM = ITEMS.register("ams_catalyst_strontium",
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0xDD0D35, 1_000_000L, 1.15F, 1.00F, 1.00F));
    public static final RegistrySupplier<Item> AMS_CATALYST_THORIUM = ITEMS.register("ams_catalyst_thorium",
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0x653B22, 2_500_000L, 1.00F, 0.95F, 1.05F));
    public static final RegistrySupplier<Item> AMS_CATALYST_TUNGSTEN = ITEMS.register("ams_catalyst_tungsten",
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0xF5FF48, 0L, 1.25F, 1.15F, 0.85F));
    public static final RegistrySupplier<Item> AMS_CORE_EYEOFHARMONY = ITEMS.register("ams_core_eyeofharmony", () -> new com.hbm_m.item.special.ItemAMSCore(2500000000L, 300, 10, new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CORE_SING = ITEMS.register("ams_core_sing", () -> new com.hbm_m.item.special.ItemAMSCore(1000000000L, 200, 10, new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CORE_THINGY = ITEMS.register("ams_core_thingy", () -> new com.hbm_m.item.special.ItemAMSCore(5000000000L, 250, 5, new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CORE_WORMHOLE = ITEMS.register("ams_core_wormhole", () -> new com.hbm_m.item.special.ItemAMSCore(1500000000L, 200, 15, new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_LENS = ITEMS.register("ams_lens",
            () -> new com.hbm_m.item.machine.ItemAMSLens(new Item.Properties(),
                    com.hbm_m.item.machine.ItemAMSLens.DEFAULT_MAX_DAMAGE));
    public static final RegistrySupplier<Item> ANALYSIS_TOOL = ITEMS.register("analysis_tool", () -> new com.hbm_m.item.tool.ItemAnalysisTool(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> ANALYZER = ITEMS.register("analyzer", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ANCHOR_REMOTE = ITEMS.register("anchor_remote", () -> new com.hbm_m.item.tool.ItemAnchorRemote(new Item.Properties()));
    public static final RegistrySupplier<Item> APPLE_EUPHEMIUM = ITEMS.register("apple_euphemium",
            () -> com.hbm_m.item.food.HbmFoodItem.of(20, 100, false).alwaysEdible().noDesc().stacksTo(1).foil().rarity(net.minecraft.world.item.Rarity.EPIC).onEaten(com.hbm_m.item.food.FoodBehaviors::appleEuphemium).build());
    public static final RegistrySupplier<Item> APPLE_LEAD = ITEMS.register("apple_lead",
            () -> com.hbm_m.item.food.HbmFoodItem.of(5, 0, false).alwaysEdible().noDesc().rarity(net.minecraft.world.item.Rarity.UNCOMMON).onEaten((st, w, pl) -> com.hbm_m.item.food.FoodBehaviors.appleLead(0, st, w, pl)).build());
    public static final RegistrySupplier<Item> APPLE_SCHRABIDIUM = ITEMS.register("apple_schrabidium",
            () -> com.hbm_m.item.food.HbmFoodItem.of(20, 100, false).alwaysEdible().noDesc().rarity(net.minecraft.world.item.Rarity.UNCOMMON).onEaten((st, w, pl) -> com.hbm_m.item.food.FoodBehaviors.appleSchrabidium(0, st, w, pl)).build());
    public static final RegistrySupplier<Item> ARC_ELECTRODE = ITEMS.register("arc_electrode", () -> new com.hbm_m.item.industrial.ItemArcElectrode(new Item.Properties(), com.hbm_m.item.industrial.ItemArcElectrode.EnumElectrodeType.GRAPHITE));
    public static final RegistrySupplier<Item> ARC_ELECTRODE_LANTHANIUM = ITEMS.register("arc_electrode_lanthanium", () -> new com.hbm_m.item.industrial.ItemArcElectrode(new Item.Properties(), com.hbm_m.item.industrial.ItemArcElectrode.EnumElectrodeType.LANTHANIUM));
    public static final RegistrySupplier<Item> ARC_ELECTRODE_DESH = ITEMS.register("arc_electrode_desh", () -> new com.hbm_m.item.industrial.ItemArcElectrode(new Item.Properties(), com.hbm_m.item.industrial.ItemArcElectrode.EnumElectrodeType.DESH));
    public static final RegistrySupplier<Item> ARC_ELECTRODE_SATURNITE = ITEMS.register("arc_electrode_saturnite", () -> new com.hbm_m.item.industrial.ItemArcElectrode(new Item.Properties(), com.hbm_m.item.industrial.ItemArcElectrode.EnumElectrodeType.SATURNITE));
    public static final RegistrySupplier<Item> ARC_ELECTRODE_BURNT_GRAPHITE = ITEMS.register("arc_electrode_burnt_graphite", () -> new com.hbm_m.item.industrial.ItemArcElectrodeBurnt(new Item.Properties(), com.hbm_m.item.industrial.ItemArcElectrode.EnumElectrodeType.GRAPHITE));
    public static final RegistrySupplier<Item> ARC_ELECTRODE_BURNT_LANTHANIUM = ITEMS.register("arc_electrode_burnt_lanthanium", () -> new com.hbm_m.item.industrial.ItemArcElectrodeBurnt(new Item.Properties(), com.hbm_m.item.industrial.ItemArcElectrode.EnumElectrodeType.LANTHANIUM));
    public static final RegistrySupplier<Item> ARC_ELECTRODE_BURNT_DESH = ITEMS.register("arc_electrode_burnt_desh", () -> new com.hbm_m.item.industrial.ItemArcElectrodeBurnt(new Item.Properties(), com.hbm_m.item.industrial.ItemArcElectrode.EnumElectrodeType.DESH));
    public static final RegistrySupplier<Item> ARC_ELECTRODE_BURNT_SATURNITE = ITEMS.register("arc_electrode_burnt_saturnite", () -> new com.hbm_m.item.industrial.ItemArcElectrodeBurnt(new Item.Properties(), com.hbm_m.item.industrial.ItemArcElectrode.EnumElectrodeType.SATURNITE));
    public static final RegistrySupplier<Item> ARMOR_POLISH = ITEMS.register("armor_polish", () -> new com.hbm_m.armormod.item.ItemModPolish());
    public static final RegistrySupplier<Item> ASSEMBLY_NUKE = ITEMS.register("assembly_nuke", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ATTACHMENT_MASK = ITEMS.register("attachment_mask", () -> new ItemModGasmask(new Item.Properties(), false));
    public static final RegistrySupplier<Item> ATTACHMENT_MASK_MONO = ITEMS.register("attachment_mask_mono", () -> new ItemModGasmask(new Item.Properties(), true));
    public static final RegistrySupplier<Item> AUSTRALIUM_III = ITEMS.register("australium_iii", () -> new com.hbm_m.armormod.item.ItemModShield(25F));
    public static final RegistrySupplier<Item> BACK_TESLA = ITEMS.register("back_tesla", () -> new com.hbm_m.armormod.item.ItemModTesla());
    public static final RegistrySupplier<Item> BALEFIRE_AND_HAM = ITEMS.register("balefire_and_ham",
            () -> com.hbm_m.item.food.HbmFoodItem.of(6, 0.6F, false).stacksTo(1).noDesc().container(() -> net.minecraft.world.item.Items.BOWL).build());
    public static final RegistrySupplier<Item> BALEFIRE_AND_STEEL = ITEMS.register("balefire_and_steel", () -> new com.hbm_m.item.tool.ItemBalefireMatch(new Item.Properties()));
    public static final RegistrySupplier<Item> BALEFIRE_SCRAMBLED = ITEMS.register("balefire_scrambled",
            () -> com.hbm_m.item.food.HbmFoodItem.of(6, 0.6F, false).stacksTo(1).noDesc().container(() -> net.minecraft.world.item.Items.BOWL).build());
    public static final RegistrySupplier<Item> BALL_DYNAMITE = ITEMS.register("ball_dynamite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALL_FIRECLAY = ITEMS.register("ball_fireclay", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALL_RESIN = ITEMS.register("ball_resin", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALL_TATB = ITEMS.register("ball_tatb", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALLISTIC_GAUNTLET = ITEMS.register("ballistic_gauntlet", () -> new com.hbm_m.armormod.item.ItemModTwoKick());
    public static final RegistrySupplier<Item> BALLISTITE = ITEMS.register("ballistite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BANDAID = ITEMS.register("bandaid", () -> new com.hbm_m.armormod.item.ItemModBandaid());
    public static final RegistrySupplier<Item> BATHWATER = ITEMS.register("bathwater", () -> new com.hbm_m.armormod.item.ItemModBathwater());
    public static final RegistrySupplier<Item> BATHWATER_MK2 = ITEMS.register("bathwater_mk2", () -> new com.hbm_m.armormod.item.ItemModBathwater());
    public static final RegistrySupplier<Item> BDCL = ITEMS.register("bdcl",
            () -> new com.hbm_m.item.food.SpecialFoodItems.BDCLItem());
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT = ITEMS.register("bedrock_ore_fragment", () -> new Item(new Item.Properties()));
    // в•ђв•ђв•ђ Bedrock Ore Progression: Rohprodukt + alle Veredelungsstufen (Grade x Type) в•ђв•ђв•ђ
    // Grade-Namen/Traits 1:1 aus ItemBedrockOreNew.BedrockOreGrade (Original-Repo) uebernommen.
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE = ITEMS.register("bedrock_ore_base", () -> new com.hbm_m.item.industrial.ItemBedrockOreBase(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_LIGHT = ITEMS.register("bedrock_ore_base_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_HEAVY = ITEMS.register("bedrock_ore_base_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_RARE = ITEMS.register("bedrock_ore_base_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ACTINIDE = ITEMS.register("bedrock_ore_base_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_NONMETAL = ITEMS.register("bedrock_ore_base_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_CRYSTAL = ITEMS.register("bedrock_ore_base_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ROASTED_LIGHT = ITEMS.register("bedrock_ore_base_roasted_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ROASTED_HEAVY = ITEMS.register("bedrock_ore_base_roasted_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ROASTED_RARE = ITEMS.register("bedrock_ore_base_roasted_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ROASTED_ACTINIDE = ITEMS.register("bedrock_ore_base_roasted_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ROASTED_NONMETAL = ITEMS.register("bedrock_ore_base_roasted_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ROASTED_CRYSTAL = ITEMS.register("bedrock_ore_base_roasted_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_WASHED_LIGHT = ITEMS.register("bedrock_ore_base_washed_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_WASHED_HEAVY = ITEMS.register("bedrock_ore_base_washed_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_WASHED_RARE = ITEMS.register("bedrock_ore_base_washed_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_WASHED_ACTINIDE = ITEMS.register("bedrock_ore_base_washed_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_WASHED_NONMETAL = ITEMS.register("bedrock_ore_base_washed_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_WASHED_CRYSTAL = ITEMS.register("bedrock_ore_base_washed_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_LIGHT = ITEMS.register("bedrock_ore_primary_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_HEAVY = ITEMS.register("bedrock_ore_primary_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RARE = ITEMS.register("bedrock_ore_primary_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ACTINIDE = ITEMS.register("bedrock_ore_primary_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NONMETAL = ITEMS.register("bedrock_ore_primary_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_CRYSTAL = ITEMS.register("bedrock_ore_primary_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ROASTED_LIGHT = ITEMS.register("bedrock_ore_primary_roasted_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ROASTED_HEAVY = ITEMS.register("bedrock_ore_primary_roasted_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ROASTED_RARE = ITEMS.register("bedrock_ore_primary_roasted_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ROASTED_ACTINIDE = ITEMS.register("bedrock_ore_primary_roasted_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ROASTED_NONMETAL = ITEMS.register("bedrock_ore_primary_roasted_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ROASTED_CRYSTAL = ITEMS.register("bedrock_ore_primary_roasted_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SULFURIC_LIGHT = ITEMS.register("bedrock_ore_primary_sulfuric_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SULFURIC_HEAVY = ITEMS.register("bedrock_ore_primary_sulfuric_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SULFURIC_RARE = ITEMS.register("bedrock_ore_primary_sulfuric_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SULFURIC_ACTINIDE = ITEMS.register("bedrock_ore_primary_sulfuric_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SULFURIC_NONMETAL = ITEMS.register("bedrock_ore_primary_sulfuric_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SULFURIC_CRYSTAL = ITEMS.register("bedrock_ore_primary_sulfuric_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSULFURIC_LIGHT = ITEMS.register("bedrock_ore_primary_nosulfuric_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSULFURIC_HEAVY = ITEMS.register("bedrock_ore_primary_nosulfuric_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSULFURIC_RARE = ITEMS.register("bedrock_ore_primary_nosulfuric_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSULFURIC_ACTINIDE = ITEMS.register("bedrock_ore_primary_nosulfuric_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSULFURIC_NONMETAL = ITEMS.register("bedrock_ore_primary_nosulfuric_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSULFURIC_CRYSTAL = ITEMS.register("bedrock_ore_primary_nosulfuric_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SOLVENT_LIGHT = ITEMS.register("bedrock_ore_primary_solvent_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SOLVENT_HEAVY = ITEMS.register("bedrock_ore_primary_solvent_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SOLVENT_RARE = ITEMS.register("bedrock_ore_primary_solvent_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SOLVENT_ACTINIDE = ITEMS.register("bedrock_ore_primary_solvent_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SOLVENT_NONMETAL = ITEMS.register("bedrock_ore_primary_solvent_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SOLVENT_CRYSTAL = ITEMS.register("bedrock_ore_primary_solvent_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSOLVENT_LIGHT = ITEMS.register("bedrock_ore_primary_nosolvent_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSOLVENT_HEAVY = ITEMS.register("bedrock_ore_primary_nosolvent_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSOLVENT_RARE = ITEMS.register("bedrock_ore_primary_nosolvent_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSOLVENT_ACTINIDE = ITEMS.register("bedrock_ore_primary_nosolvent_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSOLVENT_NONMETAL = ITEMS.register("bedrock_ore_primary_nosolvent_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSOLVENT_CRYSTAL = ITEMS.register("bedrock_ore_primary_nosolvent_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RAD_LIGHT = ITEMS.register("bedrock_ore_primary_rad_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_RAD, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RAD_HEAVY = ITEMS.register("bedrock_ore_primary_rad_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_RAD, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RAD_RARE = ITEMS.register("bedrock_ore_primary_rad_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_RAD, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RAD_ACTINIDE = ITEMS.register("bedrock_ore_primary_rad_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_RAD, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RAD_NONMETAL = ITEMS.register("bedrock_ore_primary_rad_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_RAD, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RAD_CRYSTAL = ITEMS.register("bedrock_ore_primary_rad_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_RAD, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NORAD_LIGHT = ITEMS.register("bedrock_ore_primary_norad_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NORAD, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NORAD_HEAVY = ITEMS.register("bedrock_ore_primary_norad_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NORAD, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NORAD_RARE = ITEMS.register("bedrock_ore_primary_norad_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NORAD, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NORAD_ACTINIDE = ITEMS.register("bedrock_ore_primary_norad_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NORAD, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NORAD_NONMETAL = ITEMS.register("bedrock_ore_primary_norad_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NORAD, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NORAD_CRYSTAL = ITEMS.register("bedrock_ore_primary_norad_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NORAD, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_FIRST_LIGHT = ITEMS.register("bedrock_ore_primary_first_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_FIRST, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_FIRST_HEAVY = ITEMS.register("bedrock_ore_primary_first_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_FIRST, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_FIRST_RARE = ITEMS.register("bedrock_ore_primary_first_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_FIRST, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_FIRST_ACTINIDE = ITEMS.register("bedrock_ore_primary_first_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_FIRST, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_FIRST_NONMETAL = ITEMS.register("bedrock_ore_primary_first_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_FIRST, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_FIRST_CRYSTAL = ITEMS.register("bedrock_ore_primary_first_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_FIRST, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SECOND_LIGHT = ITEMS.register("bedrock_ore_primary_second_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SECOND, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SECOND_HEAVY = ITEMS.register("bedrock_ore_primary_second_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SECOND, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SECOND_RARE = ITEMS.register("bedrock_ore_primary_second_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SECOND, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SECOND_ACTINIDE = ITEMS.register("bedrock_ore_primary_second_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SECOND, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SECOND_NONMETAL = ITEMS.register("bedrock_ore_primary_second_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SECOND, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SECOND_CRYSTAL = ITEMS.register("bedrock_ore_primary_second_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SECOND, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_CRUMBS_LIGHT = ITEMS.register("bedrock_ore_crumbs_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.CRUMBS, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_CRUMBS_HEAVY = ITEMS.register("bedrock_ore_crumbs_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.CRUMBS, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_CRUMBS_RARE = ITEMS.register("bedrock_ore_crumbs_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.CRUMBS, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_CRUMBS_ACTINIDE = ITEMS.register("bedrock_ore_crumbs_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.CRUMBS, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_CRUMBS_NONMETAL = ITEMS.register("bedrock_ore_crumbs_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.CRUMBS, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_CRUMBS_CRYSTAL = ITEMS.register("bedrock_ore_crumbs_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.CRUMBS, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_BYPRODUCT_LIGHT = ITEMS.register("bedrock_ore_sulfuric_byproduct_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_BYPRODUCT_HEAVY = ITEMS.register("bedrock_ore_sulfuric_byproduct_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_BYPRODUCT_RARE = ITEMS.register("bedrock_ore_sulfuric_byproduct_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_BYPRODUCT_ACTINIDE = ITEMS.register("bedrock_ore_sulfuric_byproduct_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_BYPRODUCT_NONMETAL = ITEMS.register("bedrock_ore_sulfuric_byproduct_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_BYPRODUCT_CRYSTAL = ITEMS.register("bedrock_ore_sulfuric_byproduct_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ROASTED_LIGHT = ITEMS.register("bedrock_ore_sulfuric_roasted_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ROASTED_HEAVY = ITEMS.register("bedrock_ore_sulfuric_roasted_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ROASTED_RARE = ITEMS.register("bedrock_ore_sulfuric_roasted_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ROASTED_ACTINIDE = ITEMS.register("bedrock_ore_sulfuric_roasted_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ROASTED_NONMETAL = ITEMS.register("bedrock_ore_sulfuric_roasted_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ROASTED_CRYSTAL = ITEMS.register("bedrock_ore_sulfuric_roasted_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ARC_LIGHT = ITEMS.register("bedrock_ore_sulfuric_arc_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ARC_HEAVY = ITEMS.register("bedrock_ore_sulfuric_arc_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ARC_RARE = ITEMS.register("bedrock_ore_sulfuric_arc_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ARC_ACTINIDE = ITEMS.register("bedrock_ore_sulfuric_arc_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ARC_NONMETAL = ITEMS.register("bedrock_ore_sulfuric_arc_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ARC_CRYSTAL = ITEMS.register("bedrock_ore_sulfuric_arc_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_WASHED_LIGHT = ITEMS.register("bedrock_ore_sulfuric_washed_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_WASHED_HEAVY = ITEMS.register("bedrock_ore_sulfuric_washed_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_WASHED_RARE = ITEMS.register("bedrock_ore_sulfuric_washed_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_WASHED_ACTINIDE = ITEMS.register("bedrock_ore_sulfuric_washed_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_WASHED_NONMETAL = ITEMS.register("bedrock_ore_sulfuric_washed_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_WASHED_CRYSTAL = ITEMS.register("bedrock_ore_sulfuric_washed_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_BYPRODUCT_LIGHT = ITEMS.register("bedrock_ore_solvent_byproduct_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_BYPRODUCT_HEAVY = ITEMS.register("bedrock_ore_solvent_byproduct_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_BYPRODUCT_RARE = ITEMS.register("bedrock_ore_solvent_byproduct_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_BYPRODUCT_ACTINIDE = ITEMS.register("bedrock_ore_solvent_byproduct_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_BYPRODUCT_NONMETAL = ITEMS.register("bedrock_ore_solvent_byproduct_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_BYPRODUCT_CRYSTAL = ITEMS.register("bedrock_ore_solvent_byproduct_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ROASTED_LIGHT = ITEMS.register("bedrock_ore_solvent_roasted_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ROASTED_HEAVY = ITEMS.register("bedrock_ore_solvent_roasted_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ROASTED_RARE = ITEMS.register("bedrock_ore_solvent_roasted_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ROASTED_ACTINIDE = ITEMS.register("bedrock_ore_solvent_roasted_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ROASTED_NONMETAL = ITEMS.register("bedrock_ore_solvent_roasted_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ROASTED_CRYSTAL = ITEMS.register("bedrock_ore_solvent_roasted_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ARC_LIGHT = ITEMS.register("bedrock_ore_solvent_arc_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ARC_HEAVY = ITEMS.register("bedrock_ore_solvent_arc_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ARC_RARE = ITEMS.register("bedrock_ore_solvent_arc_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ARC_ACTINIDE = ITEMS.register("bedrock_ore_solvent_arc_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ARC_NONMETAL = ITEMS.register("bedrock_ore_solvent_arc_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ARC_CRYSTAL = ITEMS.register("bedrock_ore_solvent_arc_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_WASHED_LIGHT = ITEMS.register("bedrock_ore_solvent_washed_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_WASHED_HEAVY = ITEMS.register("bedrock_ore_solvent_washed_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_WASHED_RARE = ITEMS.register("bedrock_ore_solvent_washed_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_WASHED_ACTINIDE = ITEMS.register("bedrock_ore_solvent_washed_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_WASHED_NONMETAL = ITEMS.register("bedrock_ore_solvent_washed_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_WASHED_CRYSTAL = ITEMS.register("bedrock_ore_solvent_washed_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_BYPRODUCT_LIGHT = ITEMS.register("bedrock_ore_rad_byproduct_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_BYPRODUCT_HEAVY = ITEMS.register("bedrock_ore_rad_byproduct_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_BYPRODUCT_RARE = ITEMS.register("bedrock_ore_rad_byproduct_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_BYPRODUCT_ACTINIDE = ITEMS.register("bedrock_ore_rad_byproduct_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_BYPRODUCT_NONMETAL = ITEMS.register("bedrock_ore_rad_byproduct_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_BYPRODUCT_CRYSTAL = ITEMS.register("bedrock_ore_rad_byproduct_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ROASTED_LIGHT = ITEMS.register("bedrock_ore_rad_roasted_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ROASTED_HEAVY = ITEMS.register("bedrock_ore_rad_roasted_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ROASTED_RARE = ITEMS.register("bedrock_ore_rad_roasted_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ROASTED_ACTINIDE = ITEMS.register("bedrock_ore_rad_roasted_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ROASTED_NONMETAL = ITEMS.register("bedrock_ore_rad_roasted_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ROASTED_CRYSTAL = ITEMS.register("bedrock_ore_rad_roasted_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ARC_LIGHT = ITEMS.register("bedrock_ore_rad_arc_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ARC_HEAVY = ITEMS.register("bedrock_ore_rad_arc_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ARC_RARE = ITEMS.register("bedrock_ore_rad_arc_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ARC_ACTINIDE = ITEMS.register("bedrock_ore_rad_arc_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ARC_NONMETAL = ITEMS.register("bedrock_ore_rad_arc_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ARC_CRYSTAL = ITEMS.register("bedrock_ore_rad_arc_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_WASHED_LIGHT = ITEMS.register("bedrock_ore_rad_washed_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_WASHED_HEAVY = ITEMS.register("bedrock_ore_rad_washed_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_WASHED_RARE = ITEMS.register("bedrock_ore_rad_washed_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_WASHED_ACTINIDE = ITEMS.register("bedrock_ore_rad_washed_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_WASHED_NONMETAL = ITEMS.register("bedrock_ore_rad_washed_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_WASHED_CRYSTAL = ITEMS.register("bedrock_ore_rad_washed_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));

    /** Alle 156 Bedrock-Ore-Veredelungsstufen (26 Grades x 6 Types), fuer Datagen/Verarbeitungslogik. */
    public static final java.util.List<RegistrySupplier<Item>> BEDROCK_ORE_ALL_VARIANTS = java.util.List.of(
        BEDROCK_ORE_BASE_LIGHT, BEDROCK_ORE_BASE_HEAVY, BEDROCK_ORE_BASE_RARE, BEDROCK_ORE_BASE_ACTINIDE,
        BEDROCK_ORE_BASE_NONMETAL, BEDROCK_ORE_BASE_CRYSTAL, BEDROCK_ORE_BASE_ROASTED_LIGHT, BEDROCK_ORE_BASE_ROASTED_HEAVY,
        BEDROCK_ORE_BASE_ROASTED_RARE, BEDROCK_ORE_BASE_ROASTED_ACTINIDE, BEDROCK_ORE_BASE_ROASTED_NONMETAL, BEDROCK_ORE_BASE_ROASTED_CRYSTAL,
        BEDROCK_ORE_BASE_WASHED_LIGHT, BEDROCK_ORE_BASE_WASHED_HEAVY, BEDROCK_ORE_BASE_WASHED_RARE, BEDROCK_ORE_BASE_WASHED_ACTINIDE,
        BEDROCK_ORE_BASE_WASHED_NONMETAL, BEDROCK_ORE_BASE_WASHED_CRYSTAL, BEDROCK_ORE_PRIMARY_LIGHT, BEDROCK_ORE_PRIMARY_HEAVY,
        BEDROCK_ORE_PRIMARY_RARE, BEDROCK_ORE_PRIMARY_ACTINIDE, BEDROCK_ORE_PRIMARY_NONMETAL, BEDROCK_ORE_PRIMARY_CRYSTAL,
        BEDROCK_ORE_PRIMARY_ROASTED_LIGHT, BEDROCK_ORE_PRIMARY_ROASTED_HEAVY, BEDROCK_ORE_PRIMARY_ROASTED_RARE, BEDROCK_ORE_PRIMARY_ROASTED_ACTINIDE,
        BEDROCK_ORE_PRIMARY_ROASTED_NONMETAL, BEDROCK_ORE_PRIMARY_ROASTED_CRYSTAL, BEDROCK_ORE_PRIMARY_SULFURIC_LIGHT, BEDROCK_ORE_PRIMARY_SULFURIC_HEAVY,
        BEDROCK_ORE_PRIMARY_SULFURIC_RARE, BEDROCK_ORE_PRIMARY_SULFURIC_ACTINIDE, BEDROCK_ORE_PRIMARY_SULFURIC_NONMETAL, BEDROCK_ORE_PRIMARY_SULFURIC_CRYSTAL,
        BEDROCK_ORE_PRIMARY_NOSULFURIC_LIGHT, BEDROCK_ORE_PRIMARY_NOSULFURIC_HEAVY, BEDROCK_ORE_PRIMARY_NOSULFURIC_RARE, BEDROCK_ORE_PRIMARY_NOSULFURIC_ACTINIDE,
        BEDROCK_ORE_PRIMARY_NOSULFURIC_NONMETAL, BEDROCK_ORE_PRIMARY_NOSULFURIC_CRYSTAL, BEDROCK_ORE_PRIMARY_SOLVENT_LIGHT, BEDROCK_ORE_PRIMARY_SOLVENT_HEAVY,
        BEDROCK_ORE_PRIMARY_SOLVENT_RARE, BEDROCK_ORE_PRIMARY_SOLVENT_ACTINIDE, BEDROCK_ORE_PRIMARY_SOLVENT_NONMETAL, BEDROCK_ORE_PRIMARY_SOLVENT_CRYSTAL,
        BEDROCK_ORE_PRIMARY_NOSOLVENT_LIGHT, BEDROCK_ORE_PRIMARY_NOSOLVENT_HEAVY, BEDROCK_ORE_PRIMARY_NOSOLVENT_RARE, BEDROCK_ORE_PRIMARY_NOSOLVENT_ACTINIDE,
        BEDROCK_ORE_PRIMARY_NOSOLVENT_NONMETAL, BEDROCK_ORE_PRIMARY_NOSOLVENT_CRYSTAL, BEDROCK_ORE_PRIMARY_RAD_LIGHT, BEDROCK_ORE_PRIMARY_RAD_HEAVY,
        BEDROCK_ORE_PRIMARY_RAD_RARE, BEDROCK_ORE_PRIMARY_RAD_ACTINIDE, BEDROCK_ORE_PRIMARY_RAD_NONMETAL, BEDROCK_ORE_PRIMARY_RAD_CRYSTAL,
        BEDROCK_ORE_PRIMARY_NORAD_LIGHT, BEDROCK_ORE_PRIMARY_NORAD_HEAVY, BEDROCK_ORE_PRIMARY_NORAD_RARE, BEDROCK_ORE_PRIMARY_NORAD_ACTINIDE,
        BEDROCK_ORE_PRIMARY_NORAD_NONMETAL, BEDROCK_ORE_PRIMARY_NORAD_CRYSTAL, BEDROCK_ORE_PRIMARY_FIRST_LIGHT, BEDROCK_ORE_PRIMARY_FIRST_HEAVY,
        BEDROCK_ORE_PRIMARY_FIRST_RARE, BEDROCK_ORE_PRIMARY_FIRST_ACTINIDE, BEDROCK_ORE_PRIMARY_FIRST_NONMETAL, BEDROCK_ORE_PRIMARY_FIRST_CRYSTAL,
        BEDROCK_ORE_PRIMARY_SECOND_LIGHT, BEDROCK_ORE_PRIMARY_SECOND_HEAVY, BEDROCK_ORE_PRIMARY_SECOND_RARE, BEDROCK_ORE_PRIMARY_SECOND_ACTINIDE,
        BEDROCK_ORE_PRIMARY_SECOND_NONMETAL, BEDROCK_ORE_PRIMARY_SECOND_CRYSTAL, BEDROCK_ORE_CRUMBS_LIGHT, BEDROCK_ORE_CRUMBS_HEAVY,
        BEDROCK_ORE_CRUMBS_RARE, BEDROCK_ORE_CRUMBS_ACTINIDE, BEDROCK_ORE_CRUMBS_NONMETAL, BEDROCK_ORE_CRUMBS_CRYSTAL,
        BEDROCK_ORE_SULFURIC_BYPRODUCT_LIGHT, BEDROCK_ORE_SULFURIC_BYPRODUCT_HEAVY, BEDROCK_ORE_SULFURIC_BYPRODUCT_RARE, BEDROCK_ORE_SULFURIC_BYPRODUCT_ACTINIDE,
        BEDROCK_ORE_SULFURIC_BYPRODUCT_NONMETAL, BEDROCK_ORE_SULFURIC_BYPRODUCT_CRYSTAL, BEDROCK_ORE_SULFURIC_ROASTED_LIGHT, BEDROCK_ORE_SULFURIC_ROASTED_HEAVY,
        BEDROCK_ORE_SULFURIC_ROASTED_RARE, BEDROCK_ORE_SULFURIC_ROASTED_ACTINIDE, BEDROCK_ORE_SULFURIC_ROASTED_NONMETAL, BEDROCK_ORE_SULFURIC_ROASTED_CRYSTAL,
        BEDROCK_ORE_SULFURIC_ARC_LIGHT, BEDROCK_ORE_SULFURIC_ARC_HEAVY, BEDROCK_ORE_SULFURIC_ARC_RARE, BEDROCK_ORE_SULFURIC_ARC_ACTINIDE,
        BEDROCK_ORE_SULFURIC_ARC_NONMETAL, BEDROCK_ORE_SULFURIC_ARC_CRYSTAL, BEDROCK_ORE_SULFURIC_WASHED_LIGHT, BEDROCK_ORE_SULFURIC_WASHED_HEAVY,
        BEDROCK_ORE_SULFURIC_WASHED_RARE, BEDROCK_ORE_SULFURIC_WASHED_ACTINIDE, BEDROCK_ORE_SULFURIC_WASHED_NONMETAL, BEDROCK_ORE_SULFURIC_WASHED_CRYSTAL,
        BEDROCK_ORE_SOLVENT_BYPRODUCT_LIGHT, BEDROCK_ORE_SOLVENT_BYPRODUCT_HEAVY, BEDROCK_ORE_SOLVENT_BYPRODUCT_RARE, BEDROCK_ORE_SOLVENT_BYPRODUCT_ACTINIDE,
        BEDROCK_ORE_SOLVENT_BYPRODUCT_NONMETAL, BEDROCK_ORE_SOLVENT_BYPRODUCT_CRYSTAL, BEDROCK_ORE_SOLVENT_ROASTED_LIGHT, BEDROCK_ORE_SOLVENT_ROASTED_HEAVY,
        BEDROCK_ORE_SOLVENT_ROASTED_RARE, BEDROCK_ORE_SOLVENT_ROASTED_ACTINIDE, BEDROCK_ORE_SOLVENT_ROASTED_NONMETAL, BEDROCK_ORE_SOLVENT_ROASTED_CRYSTAL,
        BEDROCK_ORE_SOLVENT_ARC_LIGHT, BEDROCK_ORE_SOLVENT_ARC_HEAVY, BEDROCK_ORE_SOLVENT_ARC_RARE, BEDROCK_ORE_SOLVENT_ARC_ACTINIDE,
        BEDROCK_ORE_SOLVENT_ARC_NONMETAL, BEDROCK_ORE_SOLVENT_ARC_CRYSTAL, BEDROCK_ORE_SOLVENT_WASHED_LIGHT, BEDROCK_ORE_SOLVENT_WASHED_HEAVY,
        BEDROCK_ORE_SOLVENT_WASHED_RARE, BEDROCK_ORE_SOLVENT_WASHED_ACTINIDE, BEDROCK_ORE_SOLVENT_WASHED_NONMETAL, BEDROCK_ORE_SOLVENT_WASHED_CRYSTAL,
        BEDROCK_ORE_RAD_BYPRODUCT_LIGHT, BEDROCK_ORE_RAD_BYPRODUCT_HEAVY, BEDROCK_ORE_RAD_BYPRODUCT_RARE, BEDROCK_ORE_RAD_BYPRODUCT_ACTINIDE,
        BEDROCK_ORE_RAD_BYPRODUCT_NONMETAL, BEDROCK_ORE_RAD_BYPRODUCT_CRYSTAL, BEDROCK_ORE_RAD_ROASTED_LIGHT, BEDROCK_ORE_RAD_ROASTED_HEAVY,
        BEDROCK_ORE_RAD_ROASTED_RARE, BEDROCK_ORE_RAD_ROASTED_ACTINIDE, BEDROCK_ORE_RAD_ROASTED_NONMETAL, BEDROCK_ORE_RAD_ROASTED_CRYSTAL,
        BEDROCK_ORE_RAD_ARC_LIGHT, BEDROCK_ORE_RAD_ARC_HEAVY, BEDROCK_ORE_RAD_ARC_RARE, BEDROCK_ORE_RAD_ARC_ACTINIDE,
        BEDROCK_ORE_RAD_ARC_NONMETAL, BEDROCK_ORE_RAD_ARC_CRYSTAL, BEDROCK_ORE_RAD_WASHED_LIGHT, BEDROCK_ORE_RAD_WASHED_HEAVY,
        BEDROCK_ORE_RAD_WASHED_RARE, BEDROCK_ORE_RAD_WASHED_ACTINIDE, BEDROCK_ORE_RAD_WASHED_NONMETAL, BEDROCK_ORE_RAD_WASHED_CRYSTAL
    );

    public static final RegistrySupplier<Item> BETA = ITEMS.register("beta", () -> new com.hbm_m.item.special.ItemDrop(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> BIG_SWORD = ITEMS.register("big_sword",
            () -> new BigSword(HbmToolMaterial.EMERALD));
    public static final RegistrySupplier<Item> BIO_WAFER = ITEMS.register("bio_wafer",
            () -> com.hbm_m.item.food.HbmFoodItem.of(4, 2F, false).build());
    public static final RegistrySupplier<Item> BIOMASS = ITEMS.register("biomass", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BIOMASS_COMPRESSED = ITEMS.register("biomass_compressed", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BISMUTH_AXE = ITEMS.register("bismuth_axe",
            () -> new ItemToolAbility(25F, 0, HbmToolMaterial.BISMUTH, ItemToolAbility.EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.SHREDDER, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IWeaponAbility.STUN, 3)
                    .addAbility(IWeaponAbility.VAMPIRE, 1)
                    .addAbility(IWeaponAbility.BEHEADER, 0));
    public static final RegistrySupplier<Item> BISMUTH_PICKAXE = ITEMS.register("bismuth_pickaxe",
            () -> new ItemToolAbility(15F, 0, HbmToolMaterial.BISMUTH, ItemToolAbility.EnumToolType.MINER)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.SHREDDER, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IWeaponAbility.STUN, 2)
                    .addAbility(IWeaponAbility.VAMPIRE, 0)
                    .addAbility(IWeaponAbility.BEHEADER, 0).setDepthRockBreaker());
    public static final RegistrySupplier<Item> BISMUTH_TOOL = ITEMS.register("bismuth_tool", () -> new Item(new Item.Properties().stacksTo(1))); // ItemAmatExtractor: leere Klasse
    public static final RegistrySupplier<Item> BLADE_METEORITE = ITEMS.register("blade_meteorite",
            () -> new com.hbm_m.item.special.ItemHot(200, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/item/blade_meteorite_hot.png"), new Item.Properties()));
    /** ingot_steel_dusted Meta 1-9 (Anzahl Faltungen; Meta 0 = Materialgegenstand steel_dusted_ingot). */
    public static final java.util.List<RegistrySupplier<Item>> STEEL_DUSTED_INGOTS = new java.util.ArrayList<>();
    static {
        for (int i = 1; i <= 9; i++) {
            final int forged = i;
            STEEL_DUSTED_INGOTS.add(ITEMS.register("steel_dusted_ingot_" + i,
                    () -> new com.hbm_m.item.special.ItemHotDusted(200, forged,
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/item/ingot/ingot_steel_dusted_hot.png"), new Item.Properties())));
        }
    }
    public static final RegistrySupplier<Item> BLADE_TUNGSTEN = ITEMS.register("blade_tungsten", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLADES_ADVANCED_ALLOY = ITEMS.register("blades_advanced_alloy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLADES_DESH = ITEMS.register("blades_desh", () -> new com.hbm_m.item.industrial.ItemBlades(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> BLADES_STEEL = ITEMS.register("blades_steel", () -> new com.hbm_m.item.industrial.ItemBlades(new Item.Properties().stacksTo(1), 400));
    public static final RegistrySupplier<Item> BLADES_TITANIUM = ITEMS.register("blades_titanium", () -> new com.hbm_m.item.industrial.ItemBlades(new Item.Properties().stacksTo(1), 500));
    public static final RegistrySupplier<Item> BLOWTORCH = ITEMS.register("blowtorch", () -> new com.hbm_m.item.tool.ItemBlowtorch(new Item.Properties()));
    public static final RegistrySupplier<Item> BLUEPRINTS = ITEMS.register("blueprints", () -> new com.hbm_m.item.industrial.ItemBlueprints(new Item.Properties()));
    public static final RegistrySupplier<Item> BOARD_COPPER = ITEMS.register("board_copper", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOAT_RUBBER = ITEMS.register("boat_rubber", () -> new com.hbm_m.item.tool.ItemBoatRubber(new Item.Properties()));
    // ─── Original ItemModMinecart (cart): je Art/Basis ein Gegenstand ───
    public static final RegistrySupplier<Item> CART_EMPTY_WOOD = ITEMS.register("cart_empty_wood",
            () -> new com.hbm_m.item.tool.ItemModMinecart(new Item.Properties(), com.hbm_m.item.tool.ItemModMinecart.EnumMinecart.EMPTY, com.hbm_m.item.tool.ItemModMinecart.EnumCartBase.WOOD));
    public static final RegistrySupplier<Item> CART_EMPTY_STEEL = ITEMS.register("cart_empty_steel",
            () -> new com.hbm_m.item.tool.ItemModMinecart(new Item.Properties(), com.hbm_m.item.tool.ItemModMinecart.EnumMinecart.EMPTY, com.hbm_m.item.tool.ItemModMinecart.EnumCartBase.STEEL));
    public static final RegistrySupplier<Item> CART_EMPTY_PAINTED = ITEMS.register("cart_empty_painted",
            () -> new com.hbm_m.item.tool.ItemModMinecart(new Item.Properties(), com.hbm_m.item.tool.ItemModMinecart.EnumMinecart.EMPTY, com.hbm_m.item.tool.ItemModMinecart.EnumCartBase.PAINTED));
    public static final RegistrySupplier<Item> CART_CRATE = ITEMS.register("cart_crate",
            () -> new com.hbm_m.item.tool.ItemModMinecart(new Item.Properties(), com.hbm_m.item.tool.ItemModMinecart.EnumMinecart.CRATE, com.hbm_m.item.tool.ItemModMinecart.EnumCartBase.VANILLA));
    public static final RegistrySupplier<Item> CART_DESTROYER_STEEL = ITEMS.register("cart_destroyer_steel",
            () -> new com.hbm_m.item.tool.ItemModMinecart(new Item.Properties(), com.hbm_m.item.tool.ItemModMinecart.EnumMinecart.DESTROYER, com.hbm_m.item.tool.ItemModMinecart.EnumCartBase.STEEL));
    public static final RegistrySupplier<Item> CART_DESTROYER_PAINTED = ITEMS.register("cart_destroyer_painted",
            () -> new com.hbm_m.item.tool.ItemModMinecart(new Item.Properties(), com.hbm_m.item.tool.ItemModMinecart.EnumMinecart.DESTROYER, com.hbm_m.item.tool.ItemModMinecart.EnumCartBase.PAINTED));
    public static final RegistrySupplier<Item> CART_POWDER_WOOD = ITEMS.register("cart_powder_wood",
            () -> new com.hbm_m.item.tool.ItemModMinecart(new Item.Properties(), com.hbm_m.item.tool.ItemModMinecart.EnumMinecart.POWDER, com.hbm_m.item.tool.ItemModMinecart.EnumCartBase.WOOD));
    public static final RegistrySupplier<Item> CART_POWDER_STEEL = ITEMS.register("cart_powder_steel",
            () -> new com.hbm_m.item.tool.ItemModMinecart(new Item.Properties(), com.hbm_m.item.tool.ItemModMinecart.EnumMinecart.POWDER, com.hbm_m.item.tool.ItemModMinecart.EnumCartBase.STEEL));
    public static final RegistrySupplier<Item> CART_POWDER_PAINTED = ITEMS.register("cart_powder_painted",
            () -> new com.hbm_m.item.tool.ItemModMinecart(new Item.Properties(), com.hbm_m.item.tool.ItemModMinecart.EnumMinecart.POWDER, com.hbm_m.item.tool.ItemModMinecart.EnumCartBase.PAINTED));
    public static final RegistrySupplier<Item> CART_SEMTEX_WOOD = ITEMS.register("cart_semtex_wood",
            () -> new com.hbm_m.item.tool.ItemModMinecart(new Item.Properties(), com.hbm_m.item.tool.ItemModMinecart.EnumMinecart.SEMTEX, com.hbm_m.item.tool.ItemModMinecart.EnumCartBase.WOOD));
    public static final RegistrySupplier<Item> CART_SEMTEX_STEEL = ITEMS.register("cart_semtex_steel",
            () -> new com.hbm_m.item.tool.ItemModMinecart(new Item.Properties(), com.hbm_m.item.tool.ItemModMinecart.EnumMinecart.SEMTEX, com.hbm_m.item.tool.ItemModMinecart.EnumCartBase.STEEL));
    public static final RegistrySupplier<Item> CART_SEMTEX_PAINTED = ITEMS.register("cart_semtex_painted",
            () -> new com.hbm_m.item.tool.ItemModMinecart(new Item.Properties(), com.hbm_m.item.tool.ItemModMinecart.EnumMinecart.SEMTEX, com.hbm_m.item.tool.ItemModMinecart.EnumCartBase.PAINTED));
    // ─── Zugsystem: Original ItemTrain (train, Meta je Typ) und ItemCouplingTool (coupling_tool) ───
    public static final RegistrySupplier<Item> TRAIN_CARGO_TRAM = ITEMS.register("train_cargo_tram",
            () -> new com.hbm_m.item.special.ItemTrain(new Item.Properties(), com.hbm_m.item.special.ItemTrain.EnumTrainType.CARGO_TRAM));
    public static final RegistrySupplier<Item> TRAIN_CARGO_TRAM_TRAILER = ITEMS.register("train_cargo_tram_trailer",
            () -> new com.hbm_m.item.special.ItemTrain(new Item.Properties(), com.hbm_m.item.special.ItemTrain.EnumTrainType.CARGO_TRAM_TRAILER));
    public static final RegistrySupplier<Item> COUPLING_TOOL = ITEMS.register("coupling_tool",
            () -> new Item(new Item.Properties().stacksTo(1)));

    /** Original {@code ItemModMinecart.createCartItem}: Art + Basis -> Port-Gegenstand. */
    public static Item cart(com.hbm_m.item.tool.ItemModMinecart.EnumMinecart type, com.hbm_m.item.tool.ItemModMinecart.EnumCartBase base) {
        return switch (type.name() + "_" + base.name()) {
            case "EMPTY_WOOD" -> CART_EMPTY_WOOD.get();
            case "EMPTY_STEEL" -> CART_EMPTY_STEEL.get();
            case "EMPTY_PAINTED" -> CART_EMPTY_PAINTED.get();
            case "CRATE_VANILLA" -> CART_CRATE.get();
            case "DESTROYER_STEEL" -> CART_DESTROYER_STEEL.get();
            case "DESTROYER_PAINTED" -> CART_DESTROYER_PAINTED.get();
            case "POWDER_WOOD" -> CART_POWDER_WOOD.get();
            case "POWDER_STEEL" -> CART_POWDER_STEEL.get();
            case "POWDER_PAINTED" -> CART_POWDER_PAINTED.get();
            case "SEMTEX_WOOD" -> CART_SEMTEX_WOOD.get();
            case "SEMTEX_STEEL" -> CART_SEMTEX_STEEL.get();
            case "SEMTEX_PAINTED" -> CART_SEMTEX_PAINTED.get();
            default -> net.minecraft.world.item.Items.MINECART;
        };
    }
    public static final RegistrySupplier<Item> BOBMAZON = ITEMS.register("bobmazon", () -> new com.hbm_m.item.tool.ItemCatalog(new Item.Properties()));
    /** Original bobmazon_hidden: versteckter Katalog (kein Kreativ-Reiter, Schild-Hash in HbmForgeEvents). */
    public static final RegistrySupplier<Item> BOBMAZON_HIDDEN = ITEMS.register("bobmazon_hidden", () -> new com.hbm_m.item.tool.ItemCatalog(new Item.Properties()));
    /** Original kit_custom (ItemKitCustom): Bobmazon-Pakete und Laternen-Vorraete, kein Kreativ-Reiter. */
    public static final RegistrySupplier<Item> KIT_CUSTOM = ITEMS.register("kit_custom", () -> new com.hbm_m.item.special.ItemKitCustom(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLT_SPIKE = ITEMS.register("bolt_spike", () -> new ItemCustomLore(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLTGUN = ITEMS.register("boltgun",
            () -> new com.hbm_m.item.tool.ItemBoltgun(new Item.Properties()));




    public static final RegistrySupplier<Item> BOMB_CALLER = ITEMS.register("bomb_caller",
            () -> new com.hbm_m.item.tool.ItemBombCaller(com.hbm_m.item.tool.ItemBombCaller.Strike.CARPET, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> BOMB_CALLER_NAPALM = ITEMS.register("bomb_caller_napalm",
            () -> new com.hbm_m.item.tool.ItemBombCaller(com.hbm_m.item.tool.ItemBombCaller.Strike.NAPALM, new Item.Properties()));
    public static final RegistrySupplier<Item> BOMB_CALLER_CHLORINE = ITEMS.register("bomb_caller_chlorine",
            () -> new com.hbm_m.item.tool.ItemBombCaller(com.hbm_m.item.tool.ItemBombCaller.Strike.CHLORINE, new Item.Properties()));
    public static final RegistrySupplier<Item> BOMB_CALLER_ATOMIC = ITEMS.register("bomb_caller_atomic",
            () -> new com.hbm_m.item.tool.ItemBombCaller(com.hbm_m.item.tool.ItemBombCaller.Strike.ATOMIC, new Item.Properties()));
    /** Original bomb_caller Meta 3 (Agent Orange), 5 (Stinger), 6 (Boxcar), 7 (Giftwolke). */
    public static final RegistrySupplier<Item> BOMB_CALLER_ORANGE = ITEMS.register("bomb_caller_orange",
            () -> new com.hbm_m.item.tool.ItemBombCaller(com.hbm_m.item.tool.ItemBombCaller.Strike.ORANGE, new Item.Properties()));
    public static final RegistrySupplier<Item> BOMB_CALLER_STINGER = ITEMS.register("bomb_caller_stinger",
            () -> new com.hbm_m.item.tool.ItemBombCaller(com.hbm_m.item.tool.ItemBombCaller.Strike.STINGER, new Item.Properties()));
    public static final RegistrySupplier<Item> BOMB_CALLER_BOXCAR = ITEMS.register("bomb_caller_boxcar",
            () -> new com.hbm_m.item.tool.ItemBombCaller(com.hbm_m.item.tool.ItemBombCaller.Strike.BOXCAR, new Item.Properties()));
    public static final RegistrySupplier<Item> BOMB_CALLER_PC = ITEMS.register("bomb_caller_pc",
            () -> new com.hbm_m.item.tool.ItemBombCaller(com.hbm_m.item.tool.ItemBombCaller.Strike.PC, new Item.Properties()));
    public static final RegistrySupplier<Item> BOMB_WAFFLE = ITEMS.register("bomb_waffle",
            () -> com.hbm_m.item.food.HbmFoodItem.of(20, 0.6F, false).noDesc().onEaten(com.hbm_m.item.food.FoodBehaviors::waffle).build());
    public static final RegistrySupplier<Item> BOOK_GUIDE = ITEMS.register("book_guide", () -> new com.hbm_m.item.tool.ItemGuideBook(new Item.Properties()));
    public static final RegistrySupplier<Item> BOOK_LEMEGETON = ITEMS.register("book_lemegeton", () -> new com.hbm_m.item.tool.ItemBookLemegeton(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> BOOK_OF_ = ITEMS.register("book_of_", () -> new com.hbm_m.item.special.ItemBook(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> BOOK_SECRET = ITEMS.register("book_secret", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE2_EMPTY = ITEMS.register("bottle2_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE2_FRITZ = ITEMS.register("bottle2_fritz",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()).makeBottle(() -> ModItems.BOTTLE2_EMPTY.get(), () -> ModItems.CAP_FRITZ.get()).withCraftingContainer());
    public static final RegistrySupplier<Item> BOTTLE2_KORL = ITEMS.register("bottle2_korl",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()).makeBottle(() -> ModItems.BOTTLE2_EMPTY.get(), () -> ModItems.CAP_KORL.get()).withCraftingContainer());
    public static final RegistrySupplier<Item> BOTTLE2_SUNSET = ITEMS.register("bottle2_sunset", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE_CHERRY = ITEMS.register("bottle_cherry",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()).makeBottle(() -> ModItems.BOTTLE_EMPTY.get(), () -> ModItems.CAP_NUKA.get()).withCraftingContainer());
    public static final RegistrySupplier<Item> BOTTLE_EMPTY = ITEMS.register("bottle_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE_MERCURY = ITEMS.register("bottle_mercury", () -> new com.hbm_m.item.ContainerItem(new Item.Properties(), () -> net.minecraft.world.item.Items.GLASS_BOTTLE));
    public static final RegistrySupplier<Item> BOTTLE_NUKA = ITEMS.register("bottle_nuka",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()).makeBottle(() -> ModItems.BOTTLE_EMPTY.get(), () -> ModItems.CAP_NUKA.get()));
    public static final RegistrySupplier<Item> BOTTLE_OPENER = ITEMS.register("bottle_opener", () -> new com.hbm_m.item.tool.WeaponSpecial(com.hbm_m.item.tool.HbmToolMaterial.OPENER, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> BOTTLE_QUANTUM = ITEMS.register("bottle_quantum",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()).makeBottle(() -> ModItems.BOTTLE_EMPTY.get(), () -> ModItems.CAP_QUANTUM.get()).withCraftingContainer());
    public static final RegistrySupplier<Item> BOTTLE_RAD = ITEMS.register("bottle_rad",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()).makeBottle(() -> ModItems.BOTTLE_EMPTY.get(), () -> ModItems.CAP_RAD.get()).withCraftingContainer());
    public static final RegistrySupplier<Item> BOTTLE_SPARKLE = ITEMS.register("bottle_sparkle",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()).makeBottle(() -> ModItems.BOTTLE_EMPTY.get(), () -> ModItems.CAP_SPARKLE.get()).withCraftingContainer());
    public static final RegistrySupplier<Item> BOTTLED_CLOUD = ITEMS.register("bottled_cloud", () -> new com.hbm_m.armormod.item.ItemModCloud());
    public static final RegistrySupplier<Item> BOY_BULLET = ITEMS.register("boy_bullet", () -> new Item(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistrySupplier<Item> BOY_IGNITER = ITEMS.register("boy_igniter", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> BOY_KIT = ITEMS.register("boy_kit", () -> new com.hbm_m.item.special.ItemStarterKit("boy_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> BOY_PROPELLANT = ITEMS.register("boy_propellant", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> BOY_SHIELDING = ITEMS.register("boy_shielding", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> BOY_TARGET = ITEMS.register("boy_target", () -> new Item(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistrySupplier<Item> BROKEN_ITEM = ITEMS.register("broken_item", () -> new BrokenItem(new Item.Properties()));
    public static final RegistrySupplier<Item> BUCKET_ACID = ITEMS.register("bucket_acid", () -> new dev.architectury.core.item.ArchitecturyBucketItem(() -> com.hbm_m.inventory.fluid.WorldFluids.ACID.getSource(), new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));
    public static final RegistrySupplier<Item> BUCKET_MUD = ITEMS.register("bucket_mud", () -> new dev.architectury.core.item.ArchitecturyBucketItem(() -> com.hbm_m.inventory.fluid.WorldFluids.MUD.getSource(), new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));
    public static final RegistrySupplier<Item> BUCKET_SCHRABIDIC_ACID = ITEMS.register("bucket_schrabidic_acid", () -> new dev.architectury.core.item.ArchitecturyBucketItem(() -> com.hbm_m.inventory.fluid.WorldFluids.SCHRABIDIC.getSource(), new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));
    public static final RegistrySupplier<Item> BUCKET_SULFURIC_ACID = ITEMS.register("bucket_sulfuric_acid", () -> new dev.architectury.core.item.ArchitecturyBucketItem(() -> com.hbm_m.inventory.fluid.WorldFluids.SULFURIC_ACID.getSource(), new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));
    public static final RegistrySupplier<Item> BUCKET_TOXIC = ITEMS.register("bucket_toxic", () -> new dev.architectury.core.item.ArchitecturyBucketItem(() -> com.hbm_m.inventory.fluid.WorldFluids.TOXIC.getSource(), new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));
    public static final RegistrySupplier<Item> BURNT_BARK = ITEMS.register("burnt_bark", () -> new ItemCustomLore(new Item.Properties()));
    public static final RegistrySupplier<Item> CANISTER_EMPTY = ITEMS.register("canister_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CANISTER_NAPALM = ITEMS.register("canister_napalm", () -> new com.hbm_m.item.ContainerItem(new Item.Properties(), () -> ModItems.CANISTER_EMPTY.get()));
    public static final RegistrySupplier<Item> CANNED_SLIME = ITEMS.register("canned_slime",
            () -> new ItemConserve(new Item.Properties(), ItemConserve.EnumFoodType.SLIME));
    public static final RegistrySupplier<Item> CANTEEN_VODKA = ITEMS.register("canteen_vodka",
            () -> new com.hbm_m.item.food.SpecialFoodItems.CanteenItem(3 * 60));
    public static final RegistrySupplier<Item> CAP_FRITZ = ITEMS.register("cap_fritz", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_KORL = ITEMS.register("cap_korl", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_NUKA = ITEMS.register("cap_nuka", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_QUANTUM = ITEMS.register("cap_quantum", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_RAD = ITEMS.register("cap_rad", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_SPARKLE = ITEMS.register("cap_sparkle", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_STAR = ITEMS.register("cap_star", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_SUNSET = ITEMS.register("cap_sunset", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CARD_AOS = ITEMS.register("card_aos", () -> new com.hbm_m.armormod.item.ItemModCard());
    public static final RegistrySupplier<Item> CARD_QOS = ITEMS.register("card_qos", () -> new com.hbm_m.armormod.item.ItemModCard());
    public static final RegistrySupplier<Item> CASING_BAG = ITEMS.register("casing_bag", () -> new com.hbm_m.item.tool.ItemHeldInventory(com.hbm_m.inventory.menu.HeldItemMenu.Layout.CASING_BAG, new Item.Properties()));
    public static final RegistrySupplier<Item> CATALYST_CLAY = ITEMS.register("catalyst_clay", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CATALYTIC_CONVERTER = ITEMS.register("catalytic_converter", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> CBT_DEVICE = ITEMS.register("cbt_device", () -> new com.hbm_m.item.special.ItemSyringe(com.hbm_m.item.special.ItemSyringe.Type.CBT_DEVICE, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> CELL_ANTI_SCHRABIDIUM = ITEMS.register("cell_anti_schrabidium", () -> new com.hbm_m.item.special.ItemDrop(new Item.Properties(), () -> ModItems.CELL_EMPTY.get()));
    public static final RegistrySupplier<Item> CELL_ANTIMATTER = ITEMS.register("cell_antimatter", () -> new com.hbm_m.item.special.ItemDrop(new Item.Properties(), () -> ModItems.CELL_EMPTY.get()));
    public static final RegistrySupplier<Item> CELL_BALEFIRE = ITEMS.register("cell_balefire", () -> new com.hbm_m.item.ContainerItem(new Item.Properties(), () -> ModItems.CELL_EMPTY.get()));
    public static final RegistrySupplier<Item> CELL_DEUTERIUM = ITEMS.register("cell_deuterium", () -> new com.hbm_m.item.ContainerItem(new Item.Properties(), () -> ModItems.CELL_EMPTY.get()));
    public static final RegistrySupplier<Item> CELL_EMPTY = ITEMS.register("cell_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CELL_PUF6 = ITEMS.register("cell_puf6", () -> new com.hbm_m.item.ContainerItem(new Item.Properties(), () -> ModItems.CELL_EMPTY.get()));
    public static final RegistrySupplier<Item> CELL_TRITIUM = ITEMS.register("cell_tritium", () -> new com.hbm_m.item.ContainerItem(new Item.Properties(), () -> ModItems.CELL_EMPTY.get()));
    public static final RegistrySupplier<Item> CELL_UF6 = ITEMS.register("cell_uf6", () -> new com.hbm_m.item.ContainerItem(new Item.Properties(), () -> ModItems.CELL_EMPTY.get()));
    public static final RegistrySupplier<Item> CENTRI_STICK = ITEMS.register("centri_stick",
            () -> new ItemToolAbility(3F, 0, HbmToolMaterial.ELEC, ItemToolAbility.EnumToolType.MINER, new Item.Properties().durability(50), true)
                    .addAbility(IToolHarvestAbility.CENTRIFUGE, 0));
    /** Original {@code crucible = new ItemCrucible(5000, 1F, matCrucible)}; Port-ID crucible_sword (crucible = Giesstiegel). */
    public static final RegistrySupplier<Item> CRUCIBLE_SWORD = ITEMS.register("crucible_sword",
            () -> new com.hbm_m.item.weapon.ItemCrucible(5000, 1F, com.hbm_m.item.tool.HbmToolMaterial.CRUCIBLE));
    public static final RegistrySupplier<Item> CHAINSAW = ITEMS.register("chainsaw",
            () -> new ItemChainsaw(25F, -0.05, HbmToolMaterial.CHAINSAW, ItemToolAbility.EnumToolType.AXE, 5000, 1, 250, () -> com.hbm_m.inventory.fluid.ModFluids.DIESEL.getSource(), () -> com.hbm_m.inventory.fluid.ModFluids.DIESEL_CRACK.getSource(), () -> com.hbm_m.inventory.fluid.ModFluids.KEROSENE.getSource(), () -> com.hbm_m.inventory.fluid.ModFluids.BIOFUEL.getSource(), () -> com.hbm_m.inventory.fluid.ModFluids.GASOLINE.getSource(), () -> com.hbm_m.inventory.fluid.ModFluids.GASOLINE_LEADED.getSource(), () -> com.hbm_m.inventory.fluid.ModFluids.PETROIL.getSource(), () -> com.hbm_m.inventory.fluid.ModFluids.PETROIL_LEADED.getSource(), () -> com.hbm_m.inventory.fluid.ModFluids.COALGAS.getSource(), () -> com.hbm_m.inventory.fluid.ModFluids.COALGAS_LEADED.getSource())
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IWeaponAbility.CHAINSAW, 1)
                    .addAbility(IWeaponAbility.BEHEADER, 0).setShears());
    public static final RegistrySupplier<Item> CHEESE = ITEMS.register("cheese",
            () -> com.hbm_m.item.food.HbmFoodItem.of(5, 0.75F, false).build());
    public static final RegistrySupplier<Item> CHEMISTRY_SET = ITEMS.register("chemistry_set", () -> new com.hbm_m.item.tool.ItemCraftingDegradation(100, new Item.Properties()));
    public static final RegistrySupplier<Item> CHEMISTRY_SET_BORON = ITEMS.register("chemistry_set_boron", () -> new com.hbm_m.item.tool.ItemCraftingDegradation(0, new Item.Properties()));
    public static final RegistrySupplier<Item> CHERNOBYLSIGN = ITEMS.register("chernobylsign", () -> new com.hbm_m.item.tool.WeaponSpecial(com.hbm_m.item.tool.HbmToolMaterial.ALLOY, new Item.Properties()));
    public static final RegistrySupplier<Item> CHLORINE_PINWHEEL = ITEMS.register("chlorine_pinwheel",
            () -> new com.hbm_m.item.liquids.InfiniteFluidItem(new Item.Properties(), com.hbm_m.inventory.fluid.ModFluids.CHLORINE.getSource(), 1).chance(2));
    public static final RegistrySupplier<Item> CHLOROPHYTE_AXE = ITEMS.register("chlorophyte_axe",
            () -> new ItemToolAbility(50F, 0, HbmToolMaterial.CHLOROPHYTE, ItemToolAbility.EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.LUCK, 3)
                    .addAbility(IWeaponAbility.STUN, 4)
                    .addAbility(IWeaponAbility.VAMPIRE, 3)
                    .addAbility(IWeaponAbility.BEHEADER, 0));
    public static final RegistrySupplier<Item> CHLOROPHYTE_PICKAXE = ITEMS.register("chlorophyte_pickaxe",
            () -> new ItemToolAbility(20F, 0, HbmToolMaterial.CHLOROPHYTE, ItemToolAbility.EnumToolType.MINER)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.LUCK, 3)
                    .addAbility(IToolHarvestAbility.CENTRIFUGE, 0)
                    .addAbility(IToolHarvestAbility.MERCURY, 0)
                    .addAbility(IWeaponAbility.STUN, 3)
                    .addAbility(IWeaponAbility.VAMPIRE, 2)
                    .addAbility(IWeaponAbility.BEHEADER, 0).setDepthRockBreaker());
    public static final RegistrySupplier<Item> CHOCOLATE = ITEMS.register("chocolate",
            () -> new com.hbm_m.item.food.SpecialFoodItems.PillItem());
    public static final RegistrySupplier<Item> CHOCOLATE_MILK = ITEMS.register("chocolate_milk",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER = ITEMS.register("chopper", () -> new com.hbm_m.item.special.ItemChopper(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> CHOPPER_BLADES = ITEMS.register("chopper_blades", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER_GUN = ITEMS.register("chopper_gun", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER_HEAD = ITEMS.register("chopper_head", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER_TAIL = ITEMS.register("chopper_tail", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER_TORSO = ITEMS.register("chopper_torso", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER_WING = ITEMS.register("chopper_wing", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIGARETTE = ITEMS.register("cigarette",
            () -> new com.hbm_m.item.special.ItemCigarette(false, new Item.Properties().stacksTo(16)));
    public static final RegistrySupplier<Item> CINNEBAR = ITEMS.register("cinnebar", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR = ITEMS.register("circuit_star", () -> new ItemCustomLore(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistrySupplier<Item> CLAY_TABLET = ITEMS.register("clay_tablet", () -> new com.hbm_m.item.special.ItemClayTablet(0, new Item.Properties()));
    /** {@code ItemClayTablet} Meta 1 (zweiter Sockel-Rezeptsatz, Belohnung aus LogicBlockActions/DungeonSpawner). */
    public static final RegistrySupplier<Item> CLAY_TABLET_1 = ITEMS.register("clay_tablet_1", () -> new com.hbm_m.item.special.ItemClayTablet(1, new Item.Properties()));
    public static final RegistrySupplier<Item> CMB_AXE = ITEMS.register("cmb_axe",
            () -> new ItemToolAbility(30F, 0, HbmToolMaterial.CMB, ItemToolAbility.EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 2)
                    .addAbility(IWeaponAbility.BEHEADER, 0));
    public static final RegistrySupplier<Item> CMB_HOE = ITEMS.register("cmb_hoe",
            () -> new ModHoe(HbmToolMaterial.CMB));
    public static final RegistrySupplier<Item> CMB_SHOVEL = ITEMS.register("cmb_shovel",
            () -> new ItemToolAbility(8F, 0, HbmToolMaterial.CMB, ItemToolAbility.EnumToolType.SHOVEL)
                    .addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 2));
    public static final RegistrySupplier<Item> CMB_SWORD = ITEMS.register("cmb_sword",
            () -> new ItemSwordAbility(35F, 0, HbmToolMaterial.CMB)
                    .addAbility(IWeaponAbility.STUN, 0)
                    .addAbility(IWeaponAbility.VAMPIRE, 0));
    public static final RegistrySupplier<Item> COAL_INFERNAL = ITEMS.register("coal_infernal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_AXE = ITEMS.register("cobalt_axe",
            () -> new ItemToolAbility(6F, 0, HbmToolMaterial.COBALT, ItemToolAbility.EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 0)
                    .addAbility(IWeaponAbility.BEHEADER, 0));
    public static final RegistrySupplier<Item> COBALT_DECORATED_AXE = ITEMS.register("cobalt_decorated_axe",
            () -> new ItemToolAbility(8F, 0, HbmToolMaterial.COBALT2, ItemToolAbility.EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 2)
                    .addAbility(IWeaponAbility.BEHEADER, 0));
    public static final RegistrySupplier<Item> COBALT_DECORATED_HOE = ITEMS.register("cobalt_decorated_hoe",
            () -> new ModHoe(HbmToolMaterial.COBALT2));
    public static final RegistrySupplier<Item> COBALT_DECORATED_PICKAXE = ITEMS.register("cobalt_decorated_pickaxe",
            () -> new ItemToolAbility(6F, 0, HbmToolMaterial.COBALT2, ItemToolAbility.EnumToolType.PICKAXE)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 2));
    public static final RegistrySupplier<Item> COBALT_DECORATED_SHOVEL = ITEMS.register("cobalt_decorated_shovel",
            () -> new ItemToolAbility(5F, 0, HbmToolMaterial.COBALT2, ItemToolAbility.EnumToolType.SHOVEL)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 2));
    public static final RegistrySupplier<Item> COBALT_DECORATED_SWORD = ITEMS.register("cobalt_decorated_sword",
            () -> new ItemSwordAbility(15F, 0, HbmToolMaterial.COBALT2)
                    .addAbility(IWeaponAbility.BOBBLE, 0));
    public static final RegistrySupplier<Item> COBALT_HOE = ITEMS.register("cobalt_hoe",
            () -> new ModHoe(HbmToolMaterial.COBALT));
    public static final RegistrySupplier<Item> COBALT_PICKAXE = ITEMS.register("cobalt_pickaxe",
            () -> new ItemToolAbility(4F, 0, HbmToolMaterial.COBALT, ItemToolAbility.EnumToolType.PICKAXE)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 0));
    public static final RegistrySupplier<Item> COBALT_SHOVEL = ITEMS.register("cobalt_shovel",
            () -> new ItemToolAbility(3.5F, 0, HbmToolMaterial.COBALT, ItemToolAbility.EnumToolType.SHOVEL)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 0));
    public static final RegistrySupplier<Item> COBALT_SWORD = ITEMS.register("cobalt_sword",
            () -> new ItemSwordAbility(12F, 0, HbmToolMaterial.COBALT));
    public static final RegistrySupplier<Item> COFFEE = ITEMS.register("coffee",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()));
    public static final RegistrySupplier<Item> COFFEE_RADIUM = ITEMS.register("coffee_radium",
            () -> new com.hbm_m.item.food.ItemEnergy(new Item.Properties()));
    public static final RegistrySupplier<Item> COIN_CREEPER = ITEMS.register("coin_creeper", () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistrySupplier<Item> COIN_MASKMAN = ITEMS.register("coin_maskman", () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistrySupplier<Item> COIN_RADIATION = ITEMS.register("coin_radiation", () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistrySupplier<Item> COIN_TOKEN = ITEMS.register("coin_token", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIN_UFO = ITEMS.register("coin_ufo", () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistrySupplier<Item> COIN_WORM = ITEMS.register("coin_worm", () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistrySupplier<Item> COMPONENT_EMITTER = ITEMS.register("component_emitter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COMPONENT_LIMITER = ITEMS.register("component_limiter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CONTAINMENT_BOX = ITEMS.register("containment_box", () -> new com.hbm_m.item.tool.ItemHeldInventory(com.hbm_m.inventory.menu.HeldItemMenu.Layout.LEAD_BOX, new Item.Properties()));
    public static final RegistrySupplier<Item> CORDITE = ITEMS.register("cordite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COTTON_CANDY = ITEMS.register("cotton_candy",
            () -> com.hbm_m.item.food.HbmFoodItem.of(5, 0.6F, false).alwaysEdible().noDesc().onEaten(com.hbm_m.item.food.FoodBehaviors::cottonCandy).build());
    public static final RegistrySupplier<Item> CRACKPIPE = ITEMS.register("crackpipe",
            () -> new com.hbm_m.item.special.ItemCigarette(true, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> CRATE_CALLER = ITEMS.register("crate_caller", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CRUCIBLE_TEMPLATE = ITEMS.register("crucible_template", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUBE_POWER = ITEMS.register("cube_power", () -> new com.hbm_m.item.fekal_electric.ModBatteryItem(new Item.Properties(), 1000000000000000000L, 1000000000000000L, 1000000000000000L));
    public static final RegistrySupplier<Item> CUSTOM_AMAT = ITEMS.register("custom_amat", () -> new com.hbm_m.item.ItemCustomLore(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> CUSTOM_DIRTY = ITEMS.register("custom_dirty", () -> new com.hbm_m.item.ItemCustomLore(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> CUSTOM_FALL = ITEMS.register("custom_fall", () -> new com.hbm_m.item.ItemCustomLore(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> CUSTOM_HYDRO = ITEMS.register("custom_hydro", () -> new com.hbm_m.item.ItemCustomLore(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> CUSTOM_KIT = ITEMS.register("custom_kit", () -> new com.hbm_m.item.special.ItemStarterKit("custom_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> CUSTOM_NUKE = ITEMS.register("custom_nuke", () -> new com.hbm_m.item.ItemCustomLore(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> CUSTOM_SCHRAB = ITEMS.register("custom_schrab", () -> new com.hbm_m.item.ItemCustomLore(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> CUSTOM_TNT = ITEMS.register("custom_tnt", () -> new com.hbm_m.item.ItemCustomLore(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> DEBRIS_CONCRETE = ITEMS.register("debris_concrete", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_ELEMENT = ITEMS.register("debris_element", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_EXCHANGER = ITEMS.register("debris_exchanger", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_FUEL = ITEMS.register("debris_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_GRAPHITE = ITEMS.register("debris_graphite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_METAL = ITEMS.register("debris_metal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_SHRAPNEL = ITEMS.register("debris_shrapnel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEFINITELYFOOD = ITEMS.register("definitelyfood",
            () -> com.hbm_m.item.food.HbmFoodItem.of(3, 0.5F, false).build());
    public static final RegistrySupplier<Item> DEFUSER_GOLD = ITEMS.register("defuser_gold", () -> new com.hbm_m.armormod.item.ItemModDefuser());
    public static final RegistrySupplier<Item> DEMON_CORE_CLOSED = ITEMS.register("demon_core_closed", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEMON_CORE_OPEN = ITEMS.register("demon_core_open", () -> new com.hbm_m.item.special.ItemDemonCore(new Item.Properties()));
    public static final RegistrySupplier<Item> DESH_AXE = ITEMS.register("desh_axe",
            () -> new ItemToolAbility(7.5F, -0.05, HbmToolMaterial.DESH, ItemToolAbility.EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolAreaAbility.RECURSION, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1)
                    .addAbility(IWeaponAbility.BEHEADER, 0));
    public static final RegistrySupplier<Item> DESH_HOE = ITEMS.register("desh_hoe",
            () -> new ModHoe(HbmToolMaterial.DESH));
    public static final RegistrySupplier<Item> DESH_PICKAXE = ITEMS.register("desh_pickaxe",
            () -> new ItemToolAbility(5F, -0.05, HbmToolMaterial.DESH, ItemToolAbility.EnumToolType.PICKAXE)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolAreaAbility.RECURSION, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1));
    public static final RegistrySupplier<Item> DESH_SHOVEL = ITEMS.register("desh_shovel",
            () -> new ItemToolAbility(4F, -0.05, HbmToolMaterial.DESH, ItemToolAbility.EnumToolType.SHOVEL)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolAreaAbility.RECURSION, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1));
    public static final RegistrySupplier<Item> DESH_SWORD = ITEMS.register("desh_sword",
            () -> new ItemSwordAbility(12.5F, 0, HbmToolMaterial.DESH)
                    .addAbility(IWeaponAbility.STUN, 0));
    public static final RegistrySupplier<Item> DESIGNATOR_ARTY_RANGE = ITEMS.register("designator_arty_range", () -> new com.hbm_m.item.tool.ItemDesignatorArtyRange(new Item.Properties()));
    public static final RegistrySupplier<Item> DETONATOR_DE = ITEMS.register("detonator_de", () -> new com.hbm_m.item.special.ItemDrop(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> DETONATOR_DEADMAN = ITEMS.register("detonator_deadman", () -> new com.hbm_m.item.special.ItemDrop(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> DEUTERIUM_FILTER = ITEMS.register("deuterium_filter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DIAMOND_GAVEL = ITEMS.register("diamond_gavel", () -> new com.hbm_m.item.tool.WeaponSpecial(com.hbm_m.item.tool.HbmToolMaterial.EMERALD, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> DISPERSER_CANISTER = ITEMS.register("disperser_canister", () -> new com.hbm_m.item.liquids.ItemDisperser(new Item.Properties(), () -> ModItems.DISPERSER_CANISTER_EMPTY.get()));
    public static final RegistrySupplier<Item> DNT_SWORD = ITEMS.register("dnt_sword",
            () -> new ItemSwordAbility(12F, 0, HbmToolMaterial.MESE));
    /** Порт {@code door_red} (1.7.10) — BlockItem двери красной комнаты. */
    public static final RegistrySupplier<Item> DOOR_RED = ITEMS.register("door_red",
            () -> new com.hbm_m.item.tool.ItemModDoor(ModBlocks.DOOR_RED_BLOCK.get(), new Item.Properties()));

    // ================== Секреты красной комнаты (порт item_secret, 1.7.10) ==================
    public static final RegistrySupplier<Item> ITEM_SECRET_CANISTER = ITEMS.register("item_secret_canister", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ITEM_SECRET_CONTROLLER = ITEMS.register("item_secret_controller", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ITEM_SECRET_SELENIUM_STEEL = ITEMS.register("item_secret_selenium_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ITEM_SECRET_ABERRATOR = ITEMS.register("item_secret_aberrator", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ITEM_SECRET_FOLLY = ITEMS.register("item_secret_folly", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRAX = ITEMS.register("drax", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRAX_MK2 = ITEMS.register("drax_mk2", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRAX_MK3 = ITEMS.register("drax_mk3", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_DESH = ITEMS.register("drillbit_desh", () -> new com.hbm_m.item.industrial.ItemDrillbit(new Item.Properties(), com.hbm_m.item.industrial.ItemDrillbit.EnumDrillType.DESH));
    public static final RegistrySupplier<Item> DRILLBIT_DESH_DIAMOND = ITEMS.register("drillbit_desh_diamond", () -> new com.hbm_m.item.industrial.ItemDrillbit(new Item.Properties(), com.hbm_m.item.industrial.ItemDrillbit.EnumDrillType.DESH_DIAMOND));
    public static final RegistrySupplier<Item> DRILLBIT_FERRO = ITEMS.register("drillbit_ferro", () -> new com.hbm_m.item.industrial.ItemDrillbit(new Item.Properties(), com.hbm_m.item.industrial.ItemDrillbit.EnumDrillType.FERRO));
    public static final RegistrySupplier<Item> DRILLBIT_FERRO_DIAMOND = ITEMS.register("drillbit_ferro_diamond", () -> new com.hbm_m.item.industrial.ItemDrillbit(new Item.Properties(), com.hbm_m.item.industrial.ItemDrillbit.EnumDrillType.FERRO_DIAMOND));
    public static final RegistrySupplier<Item> DRILLBIT_HSS = ITEMS.register("drillbit_hss", () -> new com.hbm_m.item.industrial.ItemDrillbit(new Item.Properties(), com.hbm_m.item.industrial.ItemDrillbit.EnumDrillType.HSS));
    public static final RegistrySupplier<Item> DRILLBIT_HSS_DIAMOND = ITEMS.register("drillbit_hss_diamond", () -> new com.hbm_m.item.industrial.ItemDrillbit(new Item.Properties(), com.hbm_m.item.industrial.ItemDrillbit.EnumDrillType.HSS_DIAMOND));
    public static final RegistrySupplier<Item> DRILLBIT_STEEL = ITEMS.register("drillbit_steel", () -> new com.hbm_m.item.industrial.ItemDrillbit(new Item.Properties(), com.hbm_m.item.industrial.ItemDrillbit.EnumDrillType.STEEL));
    public static final RegistrySupplier<Item> DRILLBIT_STEEL_DIAMOND = ITEMS.register("drillbit_steel_diamond", () -> new com.hbm_m.item.industrial.ItemDrillbit(new Item.Properties(), com.hbm_m.item.industrial.ItemDrillbit.EnumDrillType.STEEL_DIAMOND));
    public static final RegistrySupplier<Item> DRILLBIT_TCALLOY = ITEMS.register("drillbit_tcalloy", () -> new com.hbm_m.item.industrial.ItemDrillbit(new Item.Properties(), com.hbm_m.item.industrial.ItemDrillbit.EnumDrillType.TCALLOY));
    public static final RegistrySupplier<Item> DRILLBIT_TCALLOY_DIAMOND = ITEMS.register("drillbit_tcalloy_diamond", () -> new com.hbm_m.item.industrial.ItemDrillbit(new Item.Properties(), com.hbm_m.item.industrial.ItemDrillbit.EnumDrillType.TCALLOY_DIAMOND));
    public static final RegistrySupplier<Item> DRONE_LINKER = ITEMS.register("drone_linker",
            () -> new com.hbm_m.item.tools_and_armor.ItemDroneLinker(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> DRONE_PATROL = ITEMS.register("drone_patrol",
            () -> new com.hbm_m.item.tools_and_armor.ItemDrone(new Item.Properties(), false, false));
    public static final RegistrySupplier<Item> DRONE_PATROL_CHUNKLOADING = ITEMS.register("drone_patrol_chunkloading",
            () -> new com.hbm_m.item.tools_and_armor.ItemDrone(new Item.Properties(), false, true));
    public static final RegistrySupplier<Item> DRONE_PATROL_EXPRESS = ITEMS.register("drone_patrol_express",
            () -> new com.hbm_m.item.tools_and_armor.ItemDrone(new Item.Properties(), true, false));
    public static final RegistrySupplier<Item> DRONE_PATROL_EXPRESS_CHUNKLOADING = ITEMS.register("drone_patrol_express_chunkloading",
            () -> new com.hbm_m.item.tools_and_armor.ItemDrone(new Item.Properties(), true, true));
    public static final RegistrySupplier<Item> DRONE_REQUEST = ITEMS.register("drone_request",
            () -> new com.hbm_m.item.tools_and_armor.ItemDrone(new Item.Properties(), false, false, true));
    public static final RegistrySupplier<Item> DWARVEN_PICKAXE = ITEMS.register("dwarven_pickaxe",
            () -> new ItemToolAbility(5F, -0.1, HbmToolMaterial.DWARVEN, ItemToolAbility.EnumToolType.MINER, new Item.Properties().durability(250), true) // 1:1 setMaxDamage(250)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0));
    public static final RegistrySupplier<Item> DYSFUNCTIONAL_REACTOR = ITEMS.register("dysfunctional_reactor", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> EGG_BALEFIRE = ITEMS.register("egg_balefire", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> EXPLOSIVE_LENSES = ITEMS.register("explosive_lenses", () -> new ItemCustomLore(new Item.Properties()));
    public static final RegistrySupplier<Item> EGG_BALEFIRE_SHARD = ITEMS.register("egg_balefire_shard", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistrySupplier<Item> EGG_GLYPHID = ITEMS.register("egg_glyphid", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ELEC_SHOVEL = ITEMS.register("elec_shovel",
            () -> new ItemToolAbilityPower(5F, 0, HbmToolMaterial.ELEC, ItemToolAbility.EnumToolType.SHOVEL, 500000, 1000, 100)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1));
    public static final RegistrySupplier<Item> ELEC_SWORD = ITEMS.register("elec_sword",
            () -> new ItemSwordAbilityPower(12.5F, 0, HbmToolMaterial.ELEC, 500000, 1000, 100)
                    .addAbility(IWeaponAbility.STUN, 2));
    // Faehigkeitswerkzeuge des Originals, die im Port noch fehlten
    public static final RegistrySupplier<Item> CMB_PICKAXE = ITEMS.register("cmb_pickaxe",
            () -> new ItemToolAbility(10F, 0, HbmToolMaterial.CMB, ItemToolAbility.EnumToolType.PICKAXE)
                    .addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 2));
    public static final RegistrySupplier<Item> ELEC_PICKAXE = ITEMS.register("elec_pickaxe",
            () -> new ItemToolAbilityPower(6F, 0, HbmToolMaterial.ELEC, ItemToolAbility.EnumToolType.PICKAXE, 500000, 1000, 100)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1));
    public static final RegistrySupplier<Item> ELEC_AXE = ITEMS.register("elec_axe",
            () -> new ItemToolAbilityPower(10F, 0, HbmToolMaterial.ELEC, ItemToolAbility.EnumToolType.AXE, 500000, 1000, 100)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1)
                    .addAbility(IWeaponAbility.CHAINSAW, 0)
                    .addAbility(IWeaponAbility.BEHEADER, 0).setShears());
    public static final RegistrySupplier<Item> METEORITE_SWORD_REFORGED = ITEMS.register("meteorite_sword_reforged",
            () -> new ItemSwordMeteorite(12.5F, 0, HbmToolMaterial.METEORITE));
    public static final RegistrySupplier<Item> METEORITE_SWORD_MACHINED = ITEMS.register("meteorite_sword_machined",
            () -> new ItemSwordMeteorite(20F, 0, HbmToolMaterial.METEORITE));
    public static final RegistrySupplier<Item> METEORITE_SWORD_TREATED = ITEMS.register("meteorite_sword_treated",
            () -> new ItemSwordMeteorite(22.5F, 0, HbmToolMaterial.METEORITE));
    public static final RegistrySupplier<Item> METEORITE_SWORD_ETCHED = ITEMS.register("meteorite_sword_etched",
            () -> new ItemSwordMeteorite(25F, 0, HbmToolMaterial.METEORITE));
    public static final RegistrySupplier<Item> METEORITE_SWORD_BRED = ITEMS.register("meteorite_sword_bred",
            () -> new ItemSwordMeteorite(30F, 0, HbmToolMaterial.METEORITE));
    public static final RegistrySupplier<Item> METEORITE_SWORD_BALEFUL = ITEMS.register("meteorite_sword_baleful",
            () -> new ItemSwordMeteorite(75F, 0, HbmToolMaterial.METEORITE));
    public static final RegistrySupplier<Item> WEAPON_PIPE_LEAD = ITEMS.register("weapon_pipe_lead",
            () -> new ModSword(HbmToolMaterial.PIPELEAD));
    public static final RegistrySupplier<Item> REER_GRAAR = ITEMS.register("reer_graar",
            () -> new ModSword(HbmToolMaterial.TITAN));
    public static final RegistrySupplier<Item> ENERGY_CORE = ITEMS.register("energy_core", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ENTANGLEMENT_KIT = ITEMS.register("entanglement_kit", () -> new ItemCustomLore(new Item.Properties()));
    public static final RegistrySupplier<Item> FILTER_COAL = ITEMS.register("filter_coal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FINS_BIG_STEEL = ITEMS.register("fins_big_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FINS_FLAT = ITEMS.register("fins_flat", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FINS_QUAD_TITANIUM = ITEMS.register("fins_quad_titanium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FINS_SMALL_STEEL = ITEMS.register("fins_small_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FINS_TRI_STEEL = ITEMS.register("fins_tri_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLAME_CONSPIRACY = ITEMS.register("flame_conspiracy", () -> new ItemCustomLore(new Item.Properties()));
    public static final RegistrySupplier<Item> FLAME_OPINION = ITEMS.register("flame_opinion", () -> new ItemCustomLore(new Item.Properties()));
    public static final RegistrySupplier<Item> FLAME_POLITICS = ITEMS.register("flame_politics", () -> new ItemCustomLore(new Item.Properties()));
    public static final RegistrySupplier<Item> FLEIJA_CORE = ITEMS.register("fleija_core", () -> new com.hbm_m.item.bomb.ItemBombPart(new Item.Properties().stacksTo(1), () -> com.hbm_m.block.ModBlocks.NUKE_FLEIJA.get()));
    public static final RegistrySupplier<Item> FLEIJA_IGNITER = ITEMS.register("fleija_igniter", () -> new com.hbm_m.item.bomb.ItemBombPart(new Item.Properties().stacksTo(1), () -> com.hbm_m.block.ModBlocks.NUKE_FLEIJA.get()));
    public static final RegistrySupplier<Item> FLEIJA_KIT = ITEMS.register("fleija_kit", () -> new com.hbm_m.item.special.ItemStarterKit("fleija_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> FLEIJA_PROPELLANT = ITEMS.register("fleija_propellant", () -> new com.hbm_m.item.bomb.ItemBombPart(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE), () -> com.hbm_m.block.ModBlocks.NUKE_FLEIJA.get()));
    /** Original-ID des Fluessigkeitsidentifikators (der Port nannte ihn fluid_identifier). */
    public static final RegistrySupplier<Item> FLUID_IDENTIFIER_MULTI = FLUID_IDENTIFIER;
    public static final RegistrySupplier<Item> FLYWHEEL_BERYLLIUM = ITEMS.register("flywheel_beryllium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FOODITEM = ITEMS.register("fooditem",
            () -> com.hbm_m.item.food.HbmFoodItem.of(2, 5F, false).build());
    public static final RegistrySupplier<Item> FRAGMENT_ACTINIUM = ITEMS.register("fragment_actinium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_BORON = ITEMS.register("fragment_boron", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_CERIUM = ITEMS.register("fragment_cerium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_COBALT = ITEMS.register("fragment_cobalt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_COLTAN = ITEMS.register("fragment_coltan", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_LANTHANIUM = ITEMS.register("fragment_lanthanium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_METEORITE = ITEMS.register("fragment_meteorite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_NEODYMIUM = ITEMS.register("fragment_neodymium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_NIOBIUM = ITEMS.register("fragment_niobium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUSE = ITEMS.register("fuse", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_CORE = ITEMS.register("fusion_core", () -> new com.hbm_m.item.tool.ItemFusionCore(2500000, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> FUSION_CORE_INFINITE = ITEMS.register("fusion_core_infinite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_SHIELD_CHLOROPHYTE = ITEMS.register("fusion_shield_chlorophyte", () -> new com.hbm_m.item.special.ItemFusionShield(60L * 60 * 60 * 15, 9000, new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_SHIELD_DESH = ITEMS.register("fusion_shield_desh", () -> new com.hbm_m.item.special.ItemFusionShield(60L * 60 * 60 * 10, 4500, new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_SHIELD_TUNGSTEN = ITEMS.register("fusion_shield_tungsten", () -> new com.hbm_m.item.special.ItemFusionShield(60L * 60 * 60 * 5, 3500, new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_SHIELD_VAPORWAVE = ITEMS.register("fusion_shield_vaporwave", () -> new com.hbm_m.item.special.ItemFusionShield(60L * 60 * 60 * 10, 1916169, new Item.Properties()));
    public static final RegistrySupplier<Item> GADGET_CORE = ITEMS.register("gadget_core", () -> new Item(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistrySupplier<Item> GADGET_EXPLOSIVE = ITEMS.register("gadget_explosive", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GADGET_KIT = ITEMS.register("gadget_kit", () -> new com.hbm_m.item.special.ItemStarterKit("gadget_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> GADGET_WIREING = ITEMS.register("gadget_wireing", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> GAS_MASK = ITEMS.register("gas_mask", () -> new ArmorGasMaskItem(Variant.GAS_MASK, new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_FILTER = ITEMS.register("gas_mask_filter", () -> new ItemGasMaskFilter(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_FILTER_COMBO = ITEMS.register("gas_mask_filter_combo", () -> new ItemGasMaskFilter(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_FILTER_MONO = ITEMS.register("gas_mask_filter_mono", () -> new ItemGasMaskFilter(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_FILTER_PISS = ITEMS.register("gas_mask_filter_piss", () -> new ItemGasMaskFilter(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_FILTER_RAG = ITEMS.register("gas_mask_filter_rag", () -> new ItemGasMaskFilter(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_M65 = ITEMS.register("gas_mask_m65", () -> new ArmorGasMaskItem(Variant.M65, new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_MONO = ITEMS.register("gas_mask_mono", () -> new ArmorGasMaskItem(Variant.MONO, new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_OLDE = ITEMS.register("gas_mask_olde", () -> new ArmorGasMaskItem(Variant.OLDE, new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_TESTER = ITEMS.register("gas_tester", () -> new com.hbm_m.armormod.item.ItemModSensor());
    public static final RegistrySupplier<Item> GEAR_LARGE = ITEMS.register("gear_large", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GEM_ALEXANDRITE = ITEMS.register("gem_alexandrite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GEM_RAD = ITEMS.register("gem_rad", () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON))); // gem_rad: uncommon в оригинале (setRarity)
    public static final RegistrySupplier<Item> GEM_SODALITE = ITEMS.register("gem_sodalite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GEM_TANTALIUM = ITEMS.register("gem_tantalium",
            () -> new LoreTooltipItem(List.of(
                    Component.translatable("tooltip.hbm_m.tantalum_polycrystal.desc1").withStyle(ChatFormatting.GRAY)),
                    new Item.Properties()));
    // Ориг.: ItemCustomLore().setRarity(EnumRarity.uncommon) — в 1.7.10 uncommon = ЖЁЛТЫЙ
    // (vanilla Rarity.UNCOMMON современного MC тоже жёлтый).
    public static final RegistrySupplier<Item> GEM_VOLCANIC = ITEMS.register("gem_volcanic",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistrySupplier<Item> GENERATOR_FRONT = ITEMS.register("generator_front", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GENERATOR_STEEL = ITEMS.register("generator_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GLITCH = ITEMS.register("glitch", () -> new com.hbm_m.item.special.ItemGlitch(new Item.Properties()));
    public static final RegistrySupplier<Item> GLOWING_STEW = ITEMS.register("glowing_stew",
            () -> com.hbm_m.item.food.HbmFoodItem.of(6, 0.6F, false).stacksTo(1).noDesc().container(() -> net.minecraft.world.item.Items.BOWL).build());
    public static final RegistrySupplier<Item> GLYPHID_GLAND = ITEMS.register("glyphid_gland", () -> new com.hbm_m.item.liquids.ItemDisperser(new Item.Properties(), () -> ModItems.GLYPHID_GLAND_EMPTY.get()));
    public static final RegistrySupplier<Item> GLYPHID_MEAT = ITEMS.register("glyphid_meat",
            () -> com.hbm_m.item.food.HbmFoodItem.of(3, 0.5F, true).build());
    public static final RegistrySupplier<Item> GLYPHID_MEAT_GRILLED = ITEMS.register("glyphid_meat_grilled",
            () -> com.hbm_m.item.food.HbmFoodItem.of(8, 0.75F, true).potion(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 180, 1, 1F).build());
    /** Restport: 1:1 GunB92 / GunB92Cell / WeaponizedCell (Altwaffe "Star Blaster", Strahl EntityB92Beam). */
    public static final RegistrySupplier<Item> GUN_B92 = ITEMS.register("gun_b92", () -> new com.hbm_m.item.weapon.GunB92Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GUN_B92_AMMO = ITEMS.register("gun_b92_ammo", () -> new com.hbm_m.item.weapon.GunB92CellItem(new Item.Properties()));
    public static final RegistrySupplier<Item> WEAPONIZED_STARBLASTER_CELL = ITEMS.register("weaponized_starblaster_cell", () -> new com.hbm_m.item.weapon.WeaponizedCellItem(new Item.Properties()));
    /** Restport: achievement_icon (ItemEnumMulti EnumAchievementType) - je Meta ein Icon-Gegenstand ohne Creative-Tab. */
    public static final RegistrySupplier<Item> ACHIEVEMENT_ICON_GOFISH = ITEMS.register("achievement_icon_gofish", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ACHIEVEMENT_ICON_ACID = ITEMS.register("achievement_icon_acid", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ACHIEVEMENT_ICON_BALLS = ITEMS.register("achievement_icon_balls", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ACHIEVEMENT_ICON_DIGAMMASEE = ITEMS.register("achievement_icon_digammasee", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ACHIEVEMENT_ICON_DIGAMMAFEEL = ITEMS.register("achievement_icon_digammafeel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ACHIEVEMENT_ICON_DIGAMMAKNOW = ITEMS.register("achievement_icon_digammaknow", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ACHIEVEMENT_ICON_DIGAMMAKAUAIMOHO = ITEMS.register("achievement_icon_digammakauaimoho", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ACHIEVEMENT_ICON_DIGAMMAUPONTOP = ITEMS.register("achievement_icon_digammaupontop", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ACHIEVEMENT_ICON_DIGAMMAFOROURRIGHT = ITEMS.register("achievement_icon_digammaforourright", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ACHIEVEMENT_ICON_QUESTIONMARK = ITEMS.register("achievement_icon_questionmark", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GUN_KIT_1 = ITEMS.register("gun_kit_1", () -> new com.hbm_m.item.tool.ItemRepairKit(10, new Item.Properties()));
    public static final RegistrySupplier<Item> GUN_KIT_2 = ITEMS.register("gun_kit_2", () -> new com.hbm_m.item.tool.ItemRepairKit(100, new Item.Properties()));
    public static final RegistrySupplier<Item> HAND_DRILL = ITEMS.register("hand_drill", () -> new com.hbm_m.item.tool.ItemTooling(com.hbm_m.api.block.IToolable.ToolType.HAND_DRILL, 100, new Item.Properties()));
    public static final RegistrySupplier<Item> HAND_DRILL_DESH = ITEMS.register("hand_drill_desh", () -> new com.hbm_m.item.tool.ItemTooling(com.hbm_m.api.block.IToolable.ToolType.HAND_DRILL, 0, new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_GREY_KIT = ITEMS.register("hazmat_grey_kit", () -> new com.hbm_m.item.special.ItemStarterKit("hazmat_grey_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> HAZMAT_KIT = ITEMS.register("hazmat_kit", () -> new com.hbm_m.item.special.ItemStarterKit("hazmat_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> HAZMAT_RED_KIT = ITEMS.register("hazmat_red_kit", () -> new com.hbm_m.item.special.ItemStarterKit("hazmat_red_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> HEAVY_COMPONENT = ITEMS.register("heavy_component", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HOLOTAPE_DAMAGED = ITEMS.register("holotape_damaged", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HORSESHOE_MAGNET = ITEMS.register("horseshoe_magnet", () -> new com.hbm_m.armormod.item.ItemModLodestone(8));
    public static final RegistrySupplier<Item> HULL_BIG_ALUMINIUM = ITEMS.register("hull_big_aluminium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HULL_BIG_STEEL = ITEMS.register("hull_big_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HULL_BIG_TITANIUM = ITEMS.register("hull_big_titanium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HULL_SMALL_ALUMINIUM = ITEMS.register("hull_small_aluminium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HULL_SMALL_STEEL = ITEMS.register("hull_small_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ICF_PELLET = ITEMS.register("icf_pellet",
            () -> new com.hbm_m.item.machine.ItemICFPellet(new Item.Properties()));
    public static final RegistrySupplier<Item> ICF_PELLET_DEPLETED = ITEMS.register("icf_pellet_depleted", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> ICF_PELLET_EMPTY = ITEMS.register("icf_pellet_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INDUSTRIAL_MAGNET = ITEMS.register("industrial_magnet", () -> new com.hbm_m.armormod.item.ItemModLodestone(12));
    /** Original ingot_aluminium = Material-Barren ALUMINUM (Registry-ID ingot_aluminium, siehe MaterialShape.itemId). */
    public static final RegistrySupplier<Item> INGOT_ALUMINIUM = com.hbm_m.item.material.ModMaterialItems.get(
            com.hbm_m.item.material.ModMaterials.ALUMINUM, com.hbm_m.item.material.MaterialShape.INGOT);
    public static final RegistrySupplier<Item> INJECTOR_5HTP = ITEMS.register("injector_5htp", () -> new com.hbm_m.armormod.item.ItemModAuto());
    public static final RegistrySupplier<Item> INJECTOR_KNIFE = ITEMS.register("injector_knife",
            () -> new com.hbm_m.armormod.item.ItemModKnife(new Item.Properties()));
    public static final RegistrySupplier<Item> INK = ITEMS.register("ink", () -> new com.hbm_m.armormod.item.ItemModInk());
    public static final RegistrySupplier<Item> INSERT_DOXIUM = ITEMS.register("insert_doxium", () -> new com.hbm_m.armormod.item.ItemModInsert(9999, 5.0F, 1F, 1F, 1F));
    public static final RegistrySupplier<Item> INSERT_DU = ITEMS.register("insert_du", () -> new com.hbm_m.armormod.item.ItemModInsert(1500, 0.9F, 0.85F, 0.5F, 0.9F));
    public static final RegistrySupplier<Item> INSERT_ERA = ITEMS.register("insert_era", () -> new com.hbm_m.armormod.item.ItemModInsert(25, 0.5F, 1F, 0.25F, 1F));
    public static final RegistrySupplier<Item> INSERT_ESAPI = ITEMS.register("insert_esapi", () -> new com.hbm_m.armormod.item.ItemModInsert(2000, 0.95F, 0.8F, 1F, 1F));
    public static final RegistrySupplier<Item> INSERT_GHIORSIUM = ITEMS.register("insert_ghiorsium", () -> new com.hbm_m.armormod.item.ItemModInsert(2000, 0.8F, 0.75F, 0.35F, 0.9F));
    public static final RegistrySupplier<Item> INSERT_KEVLAR = ITEMS.register("insert_kevlar", () -> new com.hbm_m.armormod.item.ItemModInsert(1500, 1F, 0.9F, 1F, 1F));
    public static final RegistrySupplier<Item> INSERT_POLONIUM = ITEMS.register("insert_polonium", () -> new com.hbm_m.armormod.item.ItemModInsert(500, 0.9F, 1F, 0.95F, 0.9F));
    public static final RegistrySupplier<Item> INSERT_SAPI = ITEMS.register("insert_sapi", () -> new com.hbm_m.armormod.item.ItemModInsert(1750, 1F, 0.85F, 1F, 1F));
    public static final RegistrySupplier<Item> INSERT_STEEL = ITEMS.register("insert_steel", () -> new com.hbm_m.armormod.item.ItemModInsert(1000, 1F, 0.95F, 0.75F, 0.95F));
    public static final RegistrySupplier<Item> INSERT_XSAPI = ITEMS.register("insert_xsapi", () -> new com.hbm_m.armormod.item.ItemModInsert(2500, 0.9F, 0.75F, 1F, 1F));
    public static final RegistrySupplier<Item> INSERT_YHARONITE = ITEMS.register("insert_yharonite", () -> new com.hbm_m.armormod.item.ItemModInsert(9999, 0.01F, 1F, 1F, 1F));
    public static final RegistrySupplier<Item> IV_BLOOD = ITEMS.register("iv_blood", () -> com.hbm_m.item.special.ItemSimpleConsumable.ivBlood());
    public static final RegistrySupplier<Item> IV_EMPTY = ITEMS.register("iv_empty", () -> com.hbm_m.item.special.ItemSimpleConsumable.ivEmpty());
    public static final RegistrySupplier<Item> IV_XP = ITEMS.register("iv_xp", () -> com.hbm_m.item.special.ItemSimpleConsumable.ivXp());
    public static final RegistrySupplier<Item> IV_XP_EMPTY = ITEMS.register("iv_xp_empty", () -> com.hbm_m.item.special.ItemSimpleConsumable.ivXpEmpty());
    public static final RegistrySupplier<Item> JETPACK_BOOST = ITEMS.register("jetpack_boost", () -> new com.hbm_m.armormod.item.JetpackBooster(new Item.Properties(), () -> com.hbm_m.inventory.fluid.ModFluids.BALEFIRE.getSource(), 32000));
    public static final RegistrySupplier<Item> JETPACK_BREAK = ITEMS.register("jetpack_break", () -> new com.hbm_m.armormod.item.JetpackBreak(new Item.Properties(), () -> com.hbm_m.inventory.fluid.ModFluids.KEROSENE.getSource(), 12000));
    public static final RegistrySupplier<Item> JETPACK_FLY = ITEMS.register("jetpack_fly", () -> new com.hbm_m.armormod.item.JetpackRegular(new Item.Properties(), () -> com.hbm_m.inventory.fluid.ModFluids.KEROSENE.getSource(), 12000));
    public static final RegistrySupplier<Item> JETPACK_TANK = ITEMS.register("jetpack_tank", () -> new com.hbm_m.item.special.ItemSyringe(com.hbm_m.item.special.ItemSyringe.Type.JETPACK_TANK, new Item.Properties().stacksTo(16)));
    public static final RegistrySupplier<Item> JETPACK_VECTOR = ITEMS.register("jetpack_vector", () -> new com.hbm_m.armormod.item.JetpackVectorized(new Item.Properties(), () -> com.hbm_m.inventory.fluid.ModFluids.KEROSENE.getSource(), 16000));
    public static final RegistrySupplier<Item> JOURNAL_BJ = ITEMS.register("journal_bj", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JOURNAL_PIP = ITEMS.register("journal_pip", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JOURNAL_SILVER = ITEMS.register("journal_silver", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> KEY = ITEMS.register("key", () -> new com.hbm_m.item.tool.ItemKey(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> KEY_RED = ITEMS.register("key_red", () -> new ItemCustomLore(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> KEY_RED_CRACKED = ITEMS.register("key_red_cracked", () -> new ItemCustomLore(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> LASER_CRYSTAL_BISMUTH = ITEMS.register("laser_crystal_bismuth", () -> new com.hbm_m.item.machine.ItemFELCrystal(com.hbm_m.item.machine.ItemFELCrystal.EnumWavelengths.VISIBLE, new Item.Properties()));
    public static final RegistrySupplier<Item> LASER_CRYSTAL_CMB = ITEMS.register("laser_crystal_cmb", () -> new com.hbm_m.item.machine.ItemFELCrystal(com.hbm_m.item.machine.ItemFELCrystal.EnumWavelengths.UV, new Item.Properties()));
    public static final RegistrySupplier<Item> LASER_CRYSTAL_CO2 = ITEMS.register("laser_crystal_co2", () -> new com.hbm_m.item.machine.ItemFELCrystal(com.hbm_m.item.machine.ItemFELCrystal.EnumWavelengths.IR, new Item.Properties()));
    public static final RegistrySupplier<Item> LASER_CRYSTAL_DIGAMMA = ITEMS.register("laser_crystal_digamma", () -> new com.hbm_m.item.machine.ItemFELCrystal(com.hbm_m.item.machine.ItemFELCrystal.EnumWavelengths.DRX, new Item.Properties()));
    public static final RegistrySupplier<Item> LASER_CRYSTAL_DNT = ITEMS.register("laser_crystal_dnt", () -> new com.hbm_m.item.machine.ItemFELCrystal(com.hbm_m.item.machine.ItemFELCrystal.EnumWavelengths.GAMMA, new Item.Properties()));
    public static final RegistrySupplier<Item> LAUNCH_CODE = ITEMS.register("launch_code", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> LAUNCH_CODE_PIECE = ITEMS.register("launch_code_piece", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> LAUNCH_KEY = ITEMS.register("launch_key", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> LEAD_GAVEL = ITEMS.register("lead_gavel", () -> new com.hbm_m.item.tool.WeaponSpecial(com.hbm_m.item.tool.HbmToolMaterial.STEEL, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> LEMON = ITEMS.register("lemon",
            () -> com.hbm_m.item.food.HbmFoodItem.of(3, 0.5F, false).build());
    public static final RegistrySupplier<Item> LINKER = ITEMS.register("linker", () -> new com.hbm_m.item.ItemTeleLink(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> LITHIUM = ITEMS.register("lithium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LODESTONE = ITEMS.register("lodestone", () -> new com.hbm_m.armormod.item.ItemModLodestone(5));
    public static final RegistrySupplier<Item> LOOP_STEW = ITEMS.register("loop_stew",
            () -> com.hbm_m.item.food.HbmFoodItem.of(10, 0.5F, false).stacksTo(1).container(() -> net.minecraft.world.item.Items.BOWL).onEaten(com.hbm_m.item.food.FoodBehaviors::loopStew).build());
    public static final RegistrySupplier<Item> LOOPS = ITEMS.register("loops",
            () -> com.hbm_m.item.food.HbmFoodItem.of(4, 0.25F, false).build());
    public static final RegistrySupplier<Item> LOOT_10 = ITEMS.register("loot_10", () -> new com.hbm_m.item.missile.ItemLootCrate(new Item.Properties(), 10));
    public static final RegistrySupplier<Item> LOOT_15 = ITEMS.register("loot_15", () -> new com.hbm_m.item.missile.ItemLootCrate(new Item.Properties(), 15));
    public static final RegistrySupplier<Item> LOOT_MISC = ITEMS.register("loot_misc", () -> new com.hbm_m.item.missile.ItemLootCrate(new Item.Properties(), 0));
    public static final RegistrySupplier<Item> MAN_KIT = ITEMS.register("man_kit", () -> new com.hbm_m.item.special.ItemStarterKit("man_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> MARSHMALLOW = ITEMS.register("marshmallow",
            () -> new Item(new Item.Properties().stacksTo(1)));
    // Тряпичные маски: обычная броня без фильтра (порт ModArmor/aMatRags), см. RagMaskItem.
    public static final RegistrySupplier<Item> MASK_PISS = ITEMS.register("mask_piss", () -> new RagMaskItem(true, new Item.Properties()));
    public static final RegistrySupplier<Item> MASK_RAG = ITEMS.register("mask_rag", () -> new RagMaskItem(false, new Item.Properties()));
    public static final RegistrySupplier<Item> MATCHSTICK = ITEMS.register("matchstick", () -> new com.hbm_m.item.tool.ItemMatch(new Item.Properties()));
    public static final RegistrySupplier<Item> MECH_KEY = ITEMS.register("mech_key", () -> new ItemCustomLore(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> MED_BAG = ITEMS.register("med_bag", () -> new com.hbm_m.item.special.ItemSyringe(com.hbm_m.item.special.ItemSyringe.Type.MED_BAG, new Item.Properties()));
    public static final RegistrySupplier<Item> MED_IPECAC = ITEMS.register("med_ipecac",
            () -> com.hbm_m.item.food.HbmFoodItem.of(0, 0, false).alwaysEdible().onEaten(com.hbm_m.item.food.FoodBehaviors::ipecac).build());
    public static final RegistrySupplier<Item> MED_PTSD = ITEMS.register("med_ptsd",
            () -> com.hbm_m.item.food.HbmFoodItem.of(0, 0, false).alwaysEdible().onEaten(com.hbm_m.item.food.FoodBehaviors::ipecac).build());
    public static final RegistrySupplier<Item> MEDAL_LIQUIDATOR = ITEMS.register("medal_liquidator", () -> new com.hbm_m.armormod.item.ItemModMedal());
    public static final RegistrySupplier<Item> MELTDOWN_TOOL = ITEMS.register("meltdown_tool", () -> new com.hbm_m.item.tool.ItemDyatlov(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> MEMESPOON = ITEMS.register("memespoon", () -> new com.hbm_m.item.tool.WeaponSpecial(com.hbm_m.item.tool.HbmToolMaterial.STEEL, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> MESE_AXE = ITEMS.register("mese_axe",
            () -> new ItemToolAbility(75F, 0, HbmToolMaterial.MESE, ItemToolAbility.EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.HAMMER, 2)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 2)
                    .addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 5)
                    .addAbility(IToolAreaAbility.EXPLOSION, 3)
                    .addAbility(IWeaponAbility.STUN, 4)
                    .addAbility(IWeaponAbility.PHOSPHORUS, 1)
                    .addAbility(IWeaponAbility.BEHEADER, 0));
    public static final RegistrySupplier<Item> MESE_GAVEL = ITEMS.register("mese_gavel",
            () -> new ItemSwordAbility(250F, 1.5, HbmToolMaterial.MESEGAVEL, new Item.Properties().stacksTo(1))
                    .addAbility(IWeaponAbility.PHOSPHORUS, 0)
                    .addAbility(IWeaponAbility.RADIATION, 2)
                    .addAbility(IWeaponAbility.STUN, 3)
                    .addAbility(IWeaponAbility.VAMPIRE, 4)
                    .addAbility(IWeaponAbility.BEHEADER, 0));
    public static final RegistrySupplier<Item> MESE_PICKAXE = ITEMS.register("mese_pickaxe",
            () -> new ItemToolAbility(35F, 0, HbmToolMaterial.MESE, ItemToolAbility.EnumToolType.MINER)
                    .addAbility(IToolAreaAbility.HAMMER, 2)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 2)
                    .addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.CRYSTALLIZER, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 5)
                    .addAbility(IToolAreaAbility.EXPLOSION, 3)
                    .addAbility(IWeaponAbility.STUN, 3)
                    .addAbility(IWeaponAbility.PHOSPHORUS, 0)
                    .addAbility(IWeaponAbility.BEHEADER, 0).setDepthRockBreaker());
    public static final RegistrySupplier<Item> METEOR_CHARM = ITEMS.register("meteor_charm", () -> new com.hbm_m.armormod.item.ItemModCharm());
    public static final RegistrySupplier<Item> METEOR_REMOTE = ITEMS.register("meteor_remote", () -> new com.hbm_m.item.tool.ItemMeteorRemote(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> MIKE_COOLING_UNIT = ITEMS.register("mike_cooling_unit", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> MIKE_CORE = ITEMS.register("mike_core", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> MIKE_DEUT = ITEMS.register("mike_deut", () -> new com.hbm_m.item.ContainerItem(new Item.Properties().stacksTo(1), () -> ModItems.TANK_STEEL.get()));
    public static final RegistrySupplier<Item> MIKE_KIT = ITEMS.register("mike_kit", () -> new com.hbm_m.item.special.ItemStarterKit("mike_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> MIRROR_TOOL = ITEMS.register("mirror_tool", () -> new com.hbm_m.item.tool.ItemMirrorTool(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_CARRIER = ITEMS.register("missile_carrier", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_ENDO = ITEMS.register("missile_endo", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_EXO = ITEMS.register("missile_exo", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_KIT = ITEMS.register("missile_kit", () -> new com.hbm_m.item.special.ItemStarterKit("missile_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> MORNING_GLORY = ITEMS.register("morning_glory", () -> new com.hbm_m.armormod.item.ItemModMorningGlory());
    public static final RegistrySupplier<Item> MUCHO_MANGO = ITEMS.register("mucho_mango",
            () -> new com.hbm_m.item.food.SpecialFoodItems.MuchoMangoItem());
    public static final RegistrySupplier<Item> MULTI_KIT = ITEMS.register("multi_kit", () -> new com.hbm_m.item.special.ItemStarterKit("multi_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> N2_CHARGE = ITEMS.register("n2_charge", () -> new com.hbm_m.item.bomb.ItemBombPart(new Item.Properties().stacksTo(1), () -> com.hbm_m.block.ModBlocks.NUKE_N2.get()));
    public static final RegistrySupplier<Item> NEUTRINO_LENS = ITEMS.register("neutrino_lens", () -> new com.hbm_m.armormod.item.ItemModLens(new Item.Properties()));
    public static final RegistrySupplier<Item> NIGHT_VISION = ITEMS.register("night_vision", () -> new com.hbm_m.armormod.item.ItemModNightVision());
    public static final RegistrySupplier<Item> NITRA = ITEMS.register("nitra", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NITRA_SMALL = ITEMS.register("nitra_small", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NOTHING = ITEMS.register("nothing", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE = ITEMS.register("nuclear_waste", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE_LONG = ITEMS.register("nuclear_waste_long", () -> new com.hbm_m.item.special.ItemNuclearWaste(java.util.List.of(net.minecraft.network.chat.Component.literal("Uranium-235").withStyle(net.minecraft.ChatFormatting.ITALIC)), new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE_LONG_DEPLETED = ITEMS.register("nuclear_waste_long_depleted", () -> new com.hbm_m.item.special.ItemNuclearWaste(java.util.List.of(net.minecraft.network.chat.Component.literal("Uranium-235").withStyle(net.minecraft.ChatFormatting.ITALIC)), new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE_PEARL = ITEMS.register("nuclear_waste_pearl", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE_SHORT = ITEMS.register("nuclear_waste_short", () -> new com.hbm_m.item.LoreTooltipItem(java.util.List.of(net.minecraft.network.chat.Component.literal("Uranium-235").withStyle(net.minecraft.ChatFormatting.ITALIC)), new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE_SHORT_DEPLETED = ITEMS.register("nuclear_waste_short_depleted", () -> new com.hbm_m.item.LoreTooltipItem(java.util.List.of(net.minecraft.network.chat.Component.literal("Uranium-235").withStyle(net.minecraft.ChatFormatting.ITALIC)), new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE_VITRIFIED = ITEMS.register("nuclear_waste_vitrified", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET = ITEMS.register("nugget",
            () -> com.hbm_m.item.food.HbmFoodItem.of(200, 1F, false).build());
    public static final RegistrySupplier<Item> NUGGET_MERCURY = ITEMS.register("nugget_mercury", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUKE_ADVANCED_KIT = ITEMS.register("nuke_advanced_kit", () -> new com.hbm_m.item.special.ItemStarterKit("nuke_advanced_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> NUKE_COMMERCIALLY_KIT = ITEMS.register("nuke_commercially_kit", () -> new com.hbm_m.item.special.ItemStarterKit("nuke_commercially_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> NUKE_ELECTRIC_KIT = ITEMS.register("nuke_electric_kit", () -> new com.hbm_m.item.special.ItemStarterKit("nuke_electric_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> NUKE_STARTER_KIT = ITEMS.register("nuke_starter_kit", () -> new com.hbm_m.item.special.ItemStarterKit("nuke_starter_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> ORE_BEDROCK = ITEMS.register("ore_bedrock", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_CENTRIFUGED = ITEMS.register("ore_centrifuged", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_CLEANED = ITEMS.register("ore_cleaned", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_DEEPCLEANED = ITEMS.register("ore_deepcleaned", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_DENSITY_SCANNER = ITEMS.register("ore_density_scanner", () -> new com.hbm_m.item.tool.ItemOreDensityScanner(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> ORE_ENRICHED = ITEMS.register("ore_enriched", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_NITRATED = ITEMS.register("ore_nitrated", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_NITROCRYSTALLINE = ITEMS.register("ore_nitrocrystalline", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_PURIFIED = ITEMS.register("ore_purified", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_RADCLEANED = ITEMS.register("ore_radcleaned", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_SEARED = ITEMS.register("ore_seared", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_SEPARATED = ITEMS.register("ore_separated", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> OVERFUSE = ITEMS.register("overfuse", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PADLOCK = ITEMS.register("padlock", () -> new com.hbm_m.item.tool.ItemLock(new Item.Properties().stacksTo(1), 0.1));
    public static final RegistrySupplier<Item> PADLOCK_REINFORCED = ITEMS.register("padlock_reinforced", () -> new com.hbm_m.item.tool.ItemLock(new Item.Properties().stacksTo(1), 0.02));
    public static final RegistrySupplier<Item> PADLOCK_RUSTY = ITEMS.register("padlock_rusty", () -> new com.hbm_m.item.tool.ItemLock(new Item.Properties().stacksTo(1), 1));
    public static final RegistrySupplier<Item> PADLOCK_UNBREAKABLE = ITEMS.register("padlock_unbreakable", () -> new com.hbm_m.item.tool.ItemLock(new Item.Properties().stacksTo(1), 0));
    public static final RegistrySupplier<Item> PADS_RUBBER = ITEMS.register("pads_rubber", () -> new com.hbm_m.armormod.item.ItemModPads(0.5F));
    public static final RegistrySupplier<Item> PADS_SLIME = ITEMS.register("pads_slime", () -> new com.hbm_m.armormod.item.ItemModPads(0.25F));
    public static final RegistrySupplier<Item> PADS_STATIC = ITEMS.register("pads_static", () -> new com.hbm_m.armormod.item.ItemModPads(0.75F));
    public static final RegistrySupplier<Item> PANCAKE = ITEMS.register("pancake",
            () -> new com.hbm_m.item.food.SpecialFoodItems.PancakeItem());
    public static final RegistrySupplier<Item> PART_BARREL_HEAVY = ITEMS.register("part_barrel_heavy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_BARREL_LIGHT = ITEMS.register("part_barrel_light", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_GRIP = ITEMS.register("part_grip", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_MECHANISM = ITEMS.register("part_mechanism", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_RECEIVER_HEAVY = ITEMS.register("part_receiver_heavy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_RECEIVER_LIGHT = ITEMS.register("part_receiver_light", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_STOCK = ITEMS.register("part_stock", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_AMAT = ITEMS.register("particle_amat", () -> new com.hbm_m.item.special.ItemParticleCapsule(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_ASCHRAB = ITEMS.register("particle_aschrab", () -> new com.hbm_m.item.special.ItemParticleCapsule(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_COPPER = ITEMS.register("particle_copper", () -> new com.hbm_m.item.special.ItemParticleCapsule(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_DARK = ITEMS.register("particle_dark", () -> new com.hbm_m.item.special.ItemParticleCapsule(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_DIGAMMA = ITEMS.register("particle_digamma", () -> new com.hbm_m.item.special.ItemDigamma(60, new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_EMPTY = ITEMS.register("particle_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_HIGGS = ITEMS.register("particle_higgs", () -> new com.hbm_m.item.special.ItemParticleCapsule(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_HYDROGEN = ITEMS.register("particle_hydrogen", () -> new com.hbm_m.item.special.ItemParticleCapsule(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_LEAD = ITEMS.register("particle_lead", () -> new com.hbm_m.item.special.ItemParticleCapsule(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_LUTECE = ITEMS.register("particle_lutece", () -> new com.hbm_m.item.special.ItemParticleCapsule(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_MUON = ITEMS.register("particle_muon", () -> new com.hbm_m.item.special.ItemParticleCapsule(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_SPARKTICLE = ITEMS.register("particle_sparkticle", () -> new com.hbm_m.item.special.ItemParticleCapsule(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_STRANGE = ITEMS.register("particle_strange", () -> new com.hbm_m.item.special.ItemParticleCapsule(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_TACHYON = ITEMS.register("particle_tachyon", () -> new com.hbm_m.item.special.ItemParticleCapsule(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTS_LEGENDARY = ITEMS.register("parts_legendary", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PEAS = ITEMS.register("peas",
            () -> new com.hbm_m.item.food.SpecialFoodItems.PeasItem());
    public static final RegistrySupplier<Item> PEDESTAL_STEEL = ITEMS.register("pedestal_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_CLUSTER = ITEMS.register("pellet_cluster", () -> new ItemCustomLore(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_GAS = ITEMS.register("pellet_gas", () -> new ItemCustomLore(new Item.Properties()));
    // ── RTG-Pellets ──
    // 1:1 aus {@code ModItems} (1.7.10): Heizleistung, Halbwertszeit und Zerfallsprodukt je Pellet.
    // Die Lebensdauer ist ueberall die Halbwertszeit mal anderthalb, so wie dort.

    /** Die ausgebrannten Pellets - im Original sechs Metadaten eines Gegenstands. */
    public static final RegistrySupplier<Item> PELLET_RTG_DEPLETED_BISMUTH = ITEMS.register("pellet_rtg_depleted_bismuth",
            () -> new com.hbm_m.item.machine.ItemRTGPelletDepleted(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_DEPLETED_MERCURY = ITEMS.register("pellet_rtg_depleted_mercury",
            () -> new com.hbm_m.item.machine.ItemRTGPelletDepleted(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_DEPLETED_NEPTUNIUM = ITEMS.register("pellet_rtg_depleted_neptunium",
            () -> new com.hbm_m.item.machine.ItemRTGPelletDepleted(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_DEPLETED_LEAD = ITEMS.register("pellet_rtg_depleted_lead",
            () -> new com.hbm_m.item.machine.ItemRTGPelletDepleted(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_DEPLETED_ZIRCONIUM = ITEMS.register("pellet_rtg_depleted_zirconium",
            () -> new com.hbm_m.item.machine.ItemRTGPelletDepleted(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_DEPLETED_NICKEL = ITEMS.register("pellet_rtg_depleted_nickel",
            () -> new com.hbm_m.item.machine.ItemRTGPelletDepleted(new Item.Properties()));

    public static final RegistrySupplier<Item> PELLET_RTG = ITEMS.register("pellet_rtg",
            () -> new ItemRTGPellet(10, ItemRTGPellet.lifespan15(87.7F, ItemRTGPellet.HalfLifeType.MEDIUM),
                    () -> PELLET_RTG_DEPLETED_LEAD.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_ACTINIUM = ITEMS.register("pellet_rtg_actinium",
            () -> new ItemRTGPellet(20, ItemRTGPellet.lifespan15(21.8F, ItemRTGPellet.HalfLifeType.MEDIUM),
                    () -> PELLET_RTG_DEPLETED_LEAD.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_AMERICIUM = ITEMS.register("pellet_rtg_americium",
            () -> new ItemRTGPellet(20, ItemRTGPellet.lifespan15(4.7F, ItemRTGPellet.HalfLifeType.LONG),
                    () -> PELLET_RTG_DEPLETED_NEPTUNIUM.get(), new Item.Properties()));
    /** Im Original nicht vorhanden - dieser Port hat die Textur, aber keine Werte dafuer. */
    public static final RegistrySupplier<Item> PELLET_RTG_BERKELIUM = ITEMS.register("pellet_rtg_berkelium",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_COBALT = ITEMS.register("pellet_rtg_cobalt",
            () -> new ItemRTGPellet(15, ItemRTGPellet.lifespan15(5.3F, ItemRTGPellet.HalfLifeType.MEDIUM),
                    () -> PELLET_RTG_DEPLETED_NICKEL.get(), new Item.Properties()));
    /** Original: {@code rtgDecay() ? 200 : 100}. */
    public static final RegistrySupplier<Item> PELLET_RTG_GOLD = ITEMS.register("pellet_rtg_gold",
            () -> new ItemRTGPellet(com.hbm_m.config.VersatileConfig.rtgDecay() ? 200 : 100, ItemRTGPellet.lifespan15(2.7F, ItemRTGPellet.HalfLifeType.SHORT),
                    () -> PELLET_RTG_DEPLETED_MERCURY.get(), new Item.Properties()));
    /** Original: {@code rtgDecay() ? 600 : 200}. */
    public static final RegistrySupplier<Item> PELLET_RTG_LEAD = ITEMS.register("pellet_rtg_lead",
            () -> new ItemRTGPellet(com.hbm_m.config.VersatileConfig.rtgDecay() ? 600 : 200, ItemRTGPellet.lifespan15(0.3F, ItemRTGPellet.HalfLifeType.SHORT),
                    () -> PELLET_RTG_DEPLETED_BISMUTH.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_POLONIUM = ITEMS.register("pellet_rtg_polonium",
            () -> new ItemRTGPellet(50, ItemRTGPellet.lifespan15(138.0F, ItemRTGPellet.HalfLifeType.SHORT),
                    () -> PELLET_RTG_DEPLETED_LEAD.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_RADIUM = ITEMS.register("pellet_rtg_radium",
            () -> new ItemRTGPellet(3, ItemRTGPellet.lifespan15(16.0F, ItemRTGPellet.HalfLifeType.LONG),
                    () -> PELLET_RTG_DEPLETED_LEAD.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_STRONTIUM = ITEMS.register("pellet_rtg_strontium",
            () -> new ItemRTGPellet(15, ItemRTGPellet.lifespan15(29.0F, ItemRTGPellet.HalfLifeType.MEDIUM),
                    () -> PELLET_RTG_DEPLETED_ZIRCONIUM.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_WEAK = ITEMS.register("pellet_rtg_weak",
            () -> new ItemRTGPellet(5, ItemRTGPellet.lifespan15(1.0F, ItemRTGPellet.HalfLifeType.LONG),
                    () -> PELLET_RTG_DEPLETED_LEAD.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> PHOTO_PANEL = ITEMS.register("photo_panel", () -> new Item(new Item.Properties()));
    // Brennstaebe des Uranmeilers (MK2). Sie stehen neben den alten Staeben oben, weil das
    // Original beide Familien fuehrt - die alten gehoeren zum abgekuendigten Graphitmeiler.
    public static final RegistrySupplier<Item> PILE_ROD_RA226BE = ITEMS.register("pile_rod_mk2_ra226be",
            () -> new com.hbm_m.item.machine.ItemPileRodMK2(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPileRodMK2.EnumPileRod.RA226BE));
    public static final RegistrySupplier<Item> PILE_ROD_PO210BE = ITEMS.register("pile_rod_mk2_po210be",
            () -> new com.hbm_m.item.machine.ItemPileRodMK2(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPileRodMK2.EnumPileRod.PO210BE));
    public static final RegistrySupplier<Item> PILE_ROD_ZR = ITEMS.register("pile_rod_mk2_zr",
            () -> new com.hbm_m.item.machine.ItemPileRodMK2(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPileRodMK2.EnumPileRod.ZR));
    public static final RegistrySupplier<Item> PILE_ROD_NU = ITEMS.register("pile_rod_mk2_nu",
            () -> new com.hbm_m.item.machine.ItemPileRodMK2(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPileRodMK2.EnumPileRod.NU));
    public static final RegistrySupplier<Item> PILE_ROD_MK2_PU239 = ITEMS.register("pile_rod_mk2_pu239",
            () -> new com.hbm_m.item.machine.ItemPileRodMK2(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPileRodMK2.EnumPileRod.PU239));
    public static final RegistrySupplier<Item> PILE_ROD_RGP = ITEMS.register("pile_rod_mk2_rgp",
            () -> new com.hbm_m.item.machine.ItemPileRodMK2(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPileRodMK2.EnumPileRod.RGP));
    public static final RegistrySupplier<Item> PILE_ROD_WASTE = ITEMS.register("pile_rod_mk2_waste",
            () -> new com.hbm_m.item.machine.ItemPileRodMK2(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPileRodMK2.EnumPileRod.WASTE));
    /** Original: Thorium-Brutstab (wird zu Thorium-Brennstoff) und Thorium-Brennstoffstab. */
    public static final RegistrySupplier<Item> PILE_ROD_THORIUM = ITEMS.register("pile_rod_mk2_thorium",
            () -> new com.hbm_m.item.machine.ItemPileRodMK2(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPileRodMK2.EnumPileRod.THORIUM));
    public static final RegistrySupplier<Item> PILE_ROD_THORIUM_FUEL = ITEMS.register("pile_rod_mk2_thorium_fuel",
            () -> new com.hbm_m.item.machine.ItemPileRodMK2(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPileRodMK2.EnumPileRod.THORIUM_FUEL));

    public static final RegistrySupplier<Item> PILE_ROD_BORON = ITEMS.register("pile_rod_boron", () -> new com.hbm_m.item.machine.ItemPileRod(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_DETECTOR = ITEMS.register("pile_rod_detector", () -> new com.hbm_m.item.machine.ItemPileRod(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_LITHIUM = ITEMS.register("pile_rod_lithium", () -> new com.hbm_m.item.machine.ItemPileRod(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_PLUTONIUM = ITEMS.register("pile_rod_plutonium", () -> new com.hbm_m.item.machine.ItemPileRod(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_PU239 = ITEMS.register("pile_rod_pu239", () -> new com.hbm_m.item.machine.ItemPileRod(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_SOURCE = ITEMS.register("pile_rod_source", () -> new com.hbm_m.item.machine.ItemPileRod(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_URANIUM = ITEMS.register("pile_rod_uranium", () -> new com.hbm_m.item.machine.ItemPileRod(new Item.Properties()));
    public static final RegistrySupplier<Item> PILL_HERBAL = ITEMS.register("pill_herbal",
            () -> new com.hbm_m.item.food.SpecialFoodItems.PillItem());
    public static final RegistrySupplier<Item> PILL_IODINE = ITEMS.register("pill_iodine",
            () -> new com.hbm_m.item.food.SpecialFoodItems.PillItem());
    public static final RegistrySupplier<Item> PILL_RED = ITEMS.register("pill_red",
            () -> new com.hbm_m.item.food.SpecialFoodItems.PillItem());
    /** Original: {@code ModItems.radx} - die Textur lag im Port schon vor, das Item fehlte. */
    public static final RegistrySupplier<Item> RADX = ITEMS.register("radx",
            () -> new com.hbm_m.item.food.SpecialFoodItems.PillItem());
    public static final RegistrySupplier<Item> PIN = ITEMS.register("pin", () -> new ItemCustomLore(new Item.Properties().stacksTo(8)));
    public static final RegistrySupplier<Item> PIPES_STEEL = ITEMS.register("pipes_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PIPETTE = ITEMS.register("pipette", () -> new com.hbm_m.item.tool.ItemPipette(new Item.Properties()));
    public static final RegistrySupplier<Item> PIPETTE_BORON = ITEMS.register("pipette_boron", () -> new com.hbm_m.item.tool.ItemPipette(new Item.Properties()));
    public static final RegistrySupplier<Item> PIPETTE_LABORATORY = ITEMS.register("pipette_laboratory", () -> new com.hbm_m.item.tool.ItemPipette(new Item.Properties()));
    public static final RegistrySupplier<Item> PISTON_SELENIUM = ITEMS.register("piston_selenium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PISTON_SET_DESH = ITEMS.register("piston_set_desh", () -> new com.hbm_m.item.machine.ItemPistons(2, new Item.Properties()));
    public static final RegistrySupplier<Item> PISTON_SET_DURA = ITEMS.register("piston_set_dura", () -> new com.hbm_m.item.machine.ItemPistons(1, new Item.Properties()));
    public static final RegistrySupplier<Item> PISTON_SET_STARMETAL = ITEMS.register("piston_set_starmetal", () -> new com.hbm_m.item.machine.ItemPistons(3, new Item.Properties()));
    public static final RegistrySupplier<Item> PISTON_SET_STEEL = ITEMS.register("piston_set_steel", () -> new com.hbm_m.item.machine.ItemPistons(0, new Item.Properties()));
    public static final RegistrySupplier<Item> PLAN_C = ITEMS.register("plan_c",
            () -> new com.hbm_m.item.food.SpecialFoodItems.PillItem());
    public static final RegistrySupplier<Item> PLASTIC_BAG = ITEMS.register("plastic_bag", () -> new com.hbm_m.item.tool.ItemHeldInventory(com.hbm_m.inventory.menu.HeldItemMenu.Layout.PLASTIC_BAG, new Item.Properties()));
    /** Original plate_aluminium = Material-Platte ALUMINUM (Registry-ID plate_aluminium). */
    public static final RegistrySupplier<Item> PLATE_ALUMINIUM = com.hbm_m.item.material.ModMaterialItems.get(
            com.hbm_m.item.material.ModMaterials.ALUMINUM, com.hbm_m.item.material.MaterialShape.PLATE);
    public static final RegistrySupplier<Item> POLAROID = ITEMS.register("polaroid", () -> new com.hbm_m.item.special.ItemPolaroid(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> POLLUTION_DETECTOR = ITEMS.register("pollution_detector", () -> new com.hbm_m.item.tool.ItemPollutionDetector(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> POWER_NET_TOOL = ITEMS.register("power_net_tool", () -> new com.hbm_m.item.tool.ItemPowerNetTool(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> PROTECTION_CHARM = ITEMS.register("protection_charm", () -> new com.hbm_m.armormod.item.ItemModCharm());
    public static final RegistrySupplier<Item> PROTOTYPE_KIT = ITEMS.register("prototype_kit", () -> new com.hbm_m.item.special.ItemStarterKit("prototype_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> PUDDING = ITEMS.register("pudding",
            () -> com.hbm_m.item.food.HbmFoodItem.of(6, 1F, false).build());
    public static final RegistrySupplier<Item> PWR_PRINTER = ITEMS.register("pwr_printer",
        () -> new com.hbm_m.item.nuclear.PWRFuelPrinterItem(new Item.Properties()));

    // PWR reactor fuel - see com.hbm_m.item.nuclear.PWRFuelType for the mechanics.
    /** Original {@code pwr_fuel_depleted} (ItemEnumMulti, eine Textur fuer alle Typen). */
    public static final java.util.Map<com.hbm_m.item.nuclear.PWRFuelType, RegistrySupplier<Item>> PWR_FUEL_DEPLETED = new java.util.EnumMap<>(com.hbm_m.item.nuclear.PWRFuelType.class);
    static {
        for (com.hbm_m.item.nuclear.PWRFuelType t : com.hbm_m.item.nuclear.PWRFuelType.values()) {
            PWR_FUEL_DEPLETED.put(t, ITEMS.register("pwr_fuel_" + t.name().toLowerCase(java.util.Locale.ROOT) + "_depleted", () -> new Item(new Item.Properties())));
        }
    }
    public static final RegistrySupplier<Item> PWR_FUEL_MEU = ITEMS.register("pwr_fuel_meu",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.MEU));
    public static final RegistrySupplier<Item> PWR_FUEL_MEU_HOT = ITEMS.register("pwr_fuel_meu_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_HEU233 = ITEMS.register("pwr_fuel_heu233",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.HEU233));
    public static final RegistrySupplier<Item> PWR_FUEL_HEU233_HOT = ITEMS.register("pwr_fuel_heu233_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_HEU235 = ITEMS.register("pwr_fuel_heu235",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.HEU235));
    public static final RegistrySupplier<Item> PWR_FUEL_HEU235_HOT = ITEMS.register("pwr_fuel_heu235_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_MEN = ITEMS.register("pwr_fuel_men",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.MEN));
    public static final RegistrySupplier<Item> PWR_FUEL_MEN_HOT = ITEMS.register("pwr_fuel_men_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_HEN237 = ITEMS.register("pwr_fuel_hen237",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.HEN237));
    public static final RegistrySupplier<Item> PWR_FUEL_HEN237_HOT = ITEMS.register("pwr_fuel_hen237_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_MOX = ITEMS.register("pwr_fuel_mox",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.MOX));
    public static final RegistrySupplier<Item> PWR_FUEL_MOX_HOT = ITEMS.register("pwr_fuel_mox_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_MEP = ITEMS.register("pwr_fuel_mep",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.MEP));
    public static final RegistrySupplier<Item> PWR_FUEL_MEP_HOT = ITEMS.register("pwr_fuel_mep_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_HEP239 = ITEMS.register("pwr_fuel_hep239",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.HEP239));
    public static final RegistrySupplier<Item> PWR_FUEL_HEP239_HOT = ITEMS.register("pwr_fuel_hep239_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_HEP241 = ITEMS.register("pwr_fuel_hep241",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.HEP241));
    public static final RegistrySupplier<Item> PWR_FUEL_HEP241_HOT = ITEMS.register("pwr_fuel_hep241_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_MEA = ITEMS.register("pwr_fuel_mea",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.MEA));
    public static final RegistrySupplier<Item> PWR_FUEL_MEA_HOT = ITEMS.register("pwr_fuel_mea_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_HEA242 = ITEMS.register("pwr_fuel_hea242",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.HEA242));
    public static final RegistrySupplier<Item> PWR_FUEL_HEA242_HOT = ITEMS.register("pwr_fuel_hea242_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_HES326 = ITEMS.register("pwr_fuel_hes326",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.HES326));
    public static final RegistrySupplier<Item> PWR_FUEL_HES326_HOT = ITEMS.register("pwr_fuel_hes326_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_HES327 = ITEMS.register("pwr_fuel_hes327",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.HES327));
    public static final RegistrySupplier<Item> PWR_FUEL_HES327_HOT = ITEMS.register("pwr_fuel_hes327_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_BFB_AM_MIX = ITEMS.register("pwr_fuel_bfb_am_mix",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.BFB_AM_MIX));
    public static final RegistrySupplier<Item> PWR_FUEL_BFB_AM_MIX_HOT = ITEMS.register("pwr_fuel_bfb_am_mix_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_BFB_PU241 = ITEMS.register("pwr_fuel_bfb_pu241",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.BFB_PU241));
    public static final RegistrySupplier<Item> PWR_FUEL_BFB_PU241_HOT = ITEMS.register("pwr_fuel_bfb_pu241_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> QUARTZ_PLUTONIUM = ITEMS.register("quartz_plutonium", () -> new com.hbm_m.armormod.item.ItemModQuartz());
    public static final RegistrySupplier<Item> RADAR_LINKER = ITEMS.register("radar_linker", () -> new com.hbm_m.item.tool.ItemRadarLinker(new Item.Properties()));
    // Тряпка (ориг. ItemRag, 4541): тултип 1:1 из item.rag.desc оригинала (2 строки, без стиля).
    public static final RegistrySupplier<Item> RAG = ITEMS.register("rag", () -> new com.hbm_m.item.special.ItemRag(List.of(
            Component.translatable("tooltip.hbm_m.rag.desc1"),
            Component.translatable("tooltip.hbm_m.rag.desc2")),
            new Item.Properties()));
    public static final RegistrySupplier<Item> RAG_DAMP = ITEMS.register("rag_damp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RAG_PISS = ITEMS.register("rag_piss", () -> new Item(new Item.Properties()));
    // Remaining RBMK fuel rods/pellets - 1:1 port of the original's ItemRBMKRod stat blocks
    // (com.hbm.items.ModItems, RBMKRodItem class doc explains the burn-curve math).
    public static final RegistrySupplier<Item> RBMK_FUEL_BALEFIRE = ITEMS.register("rbmk_fuel_balefire",
            () -> new RBMKRodItem("Draconic Flames", new Item.Properties())
                    .setYield(100_000_000).setStats(100, 35).setFunction(RBMKRodItem.EnumBurnFunc.LINEAR)
                    .setXenon(0.0, 50).setHeat(3.0).setMeltingPoint(3652).setTint(0xB2FF1B).setPellet(() -> ModItems.RBMK_PELLET_BALEFIRE.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_BALEFIRE_GOLD = ITEMS.register("rbmk_fuel_balefire_gold",
            () -> new RBMKRodItem("Antihydrogen in a Magnetized Gold-198 Lattice", new Item.Properties())
                    .setYield(100_000_000).setStats(50, 10).setFunction(RBMKRodItem.EnumBurnFunc.ARCH)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.LINEAR).setXenon(0.0, 50)
                    .setMeltingPoint(2000).setTint(0xDC9613).setPellet(() -> ModItems.RBMK_PELLET_BALEFIRE_GOLD.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_FLASHLEAD = ITEMS.register("rbmk_fuel_flashlead",
            () -> new RBMKRodItem("Antihydrogen confined by a Magnetized Gold-198 and Lead-209 Lattice", new Item.Properties())
                    .setYield(250_000_000).setStats(40, 50).setFunction(RBMKRodItem.EnumBurnFunc.ARCH)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.LINEAR).setXenon(0.0, 50)
                    .setMeltingPoint(2050).setTint(0x7B7B87).setPellet(() -> ModItems.RBMK_PELLET_FLASHLEAD.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEA241 = ITEMS.register("rbmk_fuel_hea241",
            () -> new RBMKRodItem("Highly Enriched Americium-241 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(65, 15).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setHeat(1.85).setMeltingPoint(2386).setNeutronTypes(NType.FAST, NType.FAST).setTint(0xA88A8F).setPellet(() -> ModItems.RBMK_PELLET_HEA241.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEA242 = ITEMS.register("rbmk_fuel_hea242",
            () -> new RBMKRodItem("Highly Enriched Americium-242 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(45).setFunction(RBMKRodItem.EnumBurnFunc.LINEAR)
                    .setHeat(2.0).setMeltingPoint(3386).setTint(0xA88A8F).setPellet(() -> ModItems.RBMK_PELLET_HEA242.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEAUS = ITEMS.register("rbmk_fuel_heaus",
            () -> new RBMKRodItem("Highly Enriched Australium (Ayerite) Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(35).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setXenon(0.05, 50).setHeat(2.0).setMeltingPoint(5211).setTint(0xFFEE00).setPellet(() -> ModItems.RBMK_PELLET_HEAUS.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEN = ITEMS.register("rbmk_fuel_hen",
            () -> new RBMKRodItem("Highly Enriched Neptunium-237 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(40).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setMeltingPoint(2800).setNeutronTypes(NType.FAST, NType.FAST).setTint(0x757E73).setPellet(() -> ModItems.RBMK_PELLET_HEN.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEP241 = ITEMS.register("rbmk_fuel_hep241",
            () -> new RBMKRodItem("High-Enriched Plutonium-241 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(40).setFunction(RBMKRodItem.EnumBurnFunc.LINEAR)
                    .setHeat(1.75).setMeltingPoint(2744).setTint(0x656E6B).setPellet(() -> ModItems.RBMK_PELLET_HEP241.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HES = ITEMS.register("rbmk_fuel_hes",
            () -> new RBMKRodItem("Highly Enriched Schrabidium-326 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(90).setFunction(RBMKRodItem.EnumBurnFunc.LINEAR)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.LINEAR)
                    .setHeat(1.75).setMeltingPoint(3000).setTint(0x2D9A94).setPellet(() -> ModItems.RBMK_PELLET_HES.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEU233 = ITEMS.register("rbmk_fuel_heu233",
            () -> new RBMKRodItem("Highly Enriched Uranium-233 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(27.5).setFunction(RBMKRodItem.EnumBurnFunc.LINEAR)
                    .setHeat(1.25).setMeltingPoint(2865).setTint(0x868D82).setPellet(() -> ModItems.RBMK_PELLET_HEU233.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_LEA = ITEMS.register("rbmk_fuel_lea",
            () -> new RBMKRodItem("Low Enriched Americium-242 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(60, 10).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.RAISING_SLOPE)
                    .setHeat(1.5).setMeltingPoint(2386).setTint(0xA88A8F).setPellet(() -> ModItems.RBMK_PELLET_LEA.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_LEAUS = ITEMS.register("rbmk_fuel_leaus",
            () -> new RBMKRodItem("Low Enriched Australium (Tasmanite) Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(30).setFunction(RBMKRodItem.EnumBurnFunc.SIGMOID)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.LINEAR).setXenon(0.05, 50)
                    .setHeat(1.5).setMeltingPoint(7029).setTint(0xFFEE00).setPellet(() -> ModItems.RBMK_PELLET_LEAUS.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_LES = ITEMS.register("rbmk_fuel_les",
            () -> new RBMKRodItem("Low Enriched Schrabidium-326 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(50).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setHeat(1.25).setMeltingPoint(2500).setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0x2D9A94).setPellet(() -> ModItems.RBMK_PELLET_LES.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_MEA = ITEMS.register("rbmk_fuel_mea",
            () -> new RBMKRodItem("Medium Enriched Americium-242 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(35, 20).setFunction(RBMKRodItem.EnumBurnFunc.ARCH)
                    .setHeat(1.75).setMeltingPoint(2386).setTint(0xA88A8F).setPellet(() -> ModItems.RBMK_PELLET_MEA.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_MEN = ITEMS.register("rbmk_fuel_men",
            () -> new RBMKRodItem("Medium Enriched Neptunium-237 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(30).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.RAISING_SLOPE)
                    .setHeat(0.75).setMeltingPoint(2800).setNeutronTypes(NType.ANY, NType.FAST).setTint(0x757E73).setPellet(() -> ModItems.RBMK_PELLET_MEN.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_MEP = ITEMS.register("rbmk_fuel_mep",
            () -> new RBMKRodItem("Medium Enriched Plutonium-239 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(35).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setMeltingPoint(2744).setTint(0x656E6B).setPellet(() -> ModItems.RBMK_PELLET_MEP.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_MES = ITEMS.register("rbmk_fuel_mes",
            () -> new RBMKRodItem("Medium Enriched Schrabidium-326 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(75).setFunction(RBMKRodItem.EnumBurnFunc.ARCH)
                    .setHeat(1.5).setMeltingPoint(2750).setTint(0x2D9A94).setPellet(() -> ModItems.RBMK_PELLET_MES.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_MEU = ITEMS.register("rbmk_fuel_meu",
            () -> new RBMKRodItem("Medium Enriched Uranium-235 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(20).setFunction(RBMKRodItem.EnumBurnFunc.LOG_TEN)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.RAISING_SLOPE)
                    .setHeat(0.65).setMeltingPoint(2865).setTint(0x868D82).setPellet(() -> ModItems.RBMK_PELLET_MEU.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_PO210BE = ITEMS.register("rbmk_fuel_po210be",
            () -> new RBMKRodItem("Polonium-210 & Beryllium Neutron Source", new Item.Properties())
                    .setYield(25_000_000).setStats(0, 50).setFunction(RBMKRodItem.EnumBurnFunc.PASSIVE)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.LINEAR).setXenon(0.0, 50)
                    .setHeat(0.1).setDiffusion(0.05).setMeltingPoint(1287)
                    .setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0x563A26).setPellet(() -> ModItems.RBMK_PELLET_PO210BE.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_PU238BE = ITEMS.register("rbmk_fuel_pu238be",
            () -> new RBMKRodItem("Plutonium-238 & Beryllium Neutron Source", new Item.Properties())
                    .setYield(50_000_000).setStats(40, 40).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setHeat(0.1).setDiffusion(0.05).setMeltingPoint(1287)
                    .setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0x656E6B).setPellet(() -> ModItems.RBMK_PELLET_PU238BE.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_RA226BE = ITEMS.register("rbmk_fuel_ra226be",
            () -> new RBMKRodItem("Radium-226 & Beryllium Neutron Source", new Item.Properties())
                    .setYield(100_000_000).setStats(0, 20).setFunction(RBMKRodItem.EnumBurnFunc.PASSIVE)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.LINEAR).setXenon(0.0, 50)
                    .setHeat(0.035).setDiffusion(0.5).setMeltingPoint(700)
                    .setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0xB3B6AD).setPellet(() -> ModItems.RBMK_PELLET_RA226BE.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_THMEU = ITEMS.register("rbmk_fuel_thmeu",
            () -> new RBMKRodItem("Thorium with MEU Driver Fuel Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(20).setFunction(RBMKRodItem.EnumBurnFunc.PLATEU)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.BOOSTED_SLOPE)
                    .setHeat(0.65).setMeltingPoint(3350).setTint(0x665448).setPellet(() -> ModItems.RBMK_PELLET_THMEU.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_UEU = ITEMS.register("rbmk_fuel_ueu",
            () -> new RBMKRodItem("Unenriched Uranium Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(15).setFunction(RBMKRodItem.EnumBurnFunc.LOG_TEN)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.RAISING_SLOPE)
                    .setHeat(0.65).setMeltingPoint(2865).setTint(0x868D82).setPellet(() -> ModItems.RBMK_PELLET_UEU.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_UZH = ITEMS.register("rbmk_fuel_uzh",
            () -> new RBMKRodItem("Uranium Zirconium Hydride Rod", new Item.Properties())
                    .setYield(50_000_000).setStats(30).setFunction(RBMKRodItem.EnumBurnFunc.LOG_TEN)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.GENTLE_SLOPE)
                    .setHeat(0.75).setHeatCoeff(1000, 500).setDiffusion(0.1)
                    .setMeltingPoint(1845).setTint(0x7077AF).setPellet(() -> ModItems.RBMK_PELLET_UZH.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_ZFB_AM_MIX = ITEMS.register("rbmk_fuel_zfb_am_mix",
            () -> new RBMKRodItem("Zirconium Fast Breeder - HEP-241#MEA Rod", new Item.Properties())
                    .setYield(50_000_000).setStats(20).setFunction(RBMKRodItem.EnumBurnFunc.LINEAR)
                    .setHeat(1.75).setMeltingPoint(2744).setTint(0xAAA36A).setPellet(() -> ModItems.RBMK_PELLET_ZFB_AM_MIX.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_ZFB_BISMUTH = ITEMS.register("rbmk_fuel_zfb_bismuth",
            () -> new RBMKRodItem("Zirconium Fast Breeder - LEU/HEP-241#Bi Rod", new Item.Properties())
                    .setYield(50_000_000).setStats(20).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setHeat(1.75).setMeltingPoint(2744).setTint(0xAAA36A).setPellet(() -> ModItems.RBMK_PELLET_ZFB_BISMUTH.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_ZFB_PU241 = ITEMS.register("rbmk_fuel_zfb_pu241",
            () -> new RBMKRodItem("Zirconium Fast Breeder - HEU-235/HEP-240#Pu-241 Rod", new Item.Properties())
                    .setYield(50_000_000).setStats(20).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setMeltingPoint(2865).setTint(0xAAA36A).setPellet(() -> ModItems.RBMK_PELLET_ZFB_PU241.get()));

    // Original ModItems.java:3314 - the debug rod, no pellet (cannot be disassembled).
    public static final RegistrySupplier<Item> RBMK_FUEL_TEST = ITEMS.register("rbmk_fuel_test",
            () -> new RBMKRodItem("THE VOICES", new Item.Properties())
                    .setYield(1_000_000).setStats(100).setFunction(RBMKRodItem.EnumBurnFunc.EXPERIMENTAL)
                    .setHeat(1.0).setMeltingPoint(100_000));

    public static final RegistrySupplier<Item> RBMK_PELLET_BALEFIRE = ITEMS.register("rbmk_pellet_balefire",
            () -> new RBMKPelletItem(new Item.Properties()).disableXenon().setFullName("Draconic Flames")
                    .setYield(100_000_000).setReactivity(100).setHeat(3.0).setMeltingPoint(3652).setTint(0xB2FF1B));
    public static final RegistrySupplier<Item> RBMK_PELLET_BALEFIRE_GOLD = ITEMS.register("rbmk_pellet_balefire_gold",
            () -> new RBMKPelletItem(new Item.Properties()).disableXenon().setFullName("Antihydrogen in a Magnetized Gold-198 Lattice")
                    .setYield(100_000_000).setReactivity(50).setMeltingPoint(2000).setTint(0xDC9613));
    public static final RegistrySupplier<Item> RBMK_PELLET_DRX = ITEMS.register("rbmk_pellet_drx",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("can't you hear, can't you hear the thunder?")
                    .setYield(10_000_000).setReactivity(1000).setHeat(0.1).setMeltingPoint(100_000).setTint(0xD77276));
    public static final RegistrySupplier<Item> RBMK_PELLET_FLASHLEAD = ITEMS.register("rbmk_pellet_flashlead",
            () -> new RBMKPelletItem(new Item.Properties()).disableXenon().setFullName("Antihydrogen confined by a Magnetized Gold-198 and Lead-209 Lattice")
                    .setYield(250_000_000).setReactivity(40).setMeltingPoint(2050).setTint(0x7B7B87));
    public static final RegistrySupplier<Item> RBMK_PELLET_HEA241 = ITEMS.register("rbmk_pellet_hea241",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Highly Enriched Americium-241")
                    .setYield(100_000_000).setReactivity(65).setHeat(1.85).setMeltingPoint(2386)
                    .setNeutronTypes(NType.FAST, NType.FAST).setTint(0xA88A8F));
    public static final RegistrySupplier<Item> RBMK_PELLET_HEA242 = ITEMS.register("rbmk_pellet_hea242",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Highly Enriched Americium-242")
                    .setYield(100_000_000).setReactivity(45).setHeat(2.0).setMeltingPoint(2386).setTint(0xA88A8F));
    public static final RegistrySupplier<Item> RBMK_PELLET_HEAUS = ITEMS.register("rbmk_pellet_heaus",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Highly Enriched Australium (Ayerite)")
                    .setYield(100_000_000).setReactivity(35).setXenon(0.05, 50).setHeat(1.5).setMeltingPoint(5211).setTint(0xFFEE00));
    public static final RegistrySupplier<Item> RBMK_PELLET_HEN = ITEMS.register("rbmk_pellet_hen",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Highly Enriched Neptunium-237")
                    .setYield(100_000_000).setReactivity(40).setMeltingPoint(2800)
                    .setNeutronTypes(NType.FAST, NType.FAST).setTint(0x757E73));
    public static final RegistrySupplier<Item> RBMK_PELLET_HEP241 = ITEMS.register("rbmk_pellet_hep241",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Highly Enriched Plutonium-241")
                    .setYield(100_000_000).setReactivity(40).setHeat(1.75).setMeltingPoint(2744).setTint(0x656E6B));
    public static final RegistrySupplier<Item> RBMK_PELLET_HES = ITEMS.register("rbmk_pellet_hes",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Highly Enriched Schrabidium-326")
                    .setYield(100_000_000).setReactivity(90).setHeat(1.75).setMeltingPoint(3000).setTint(0x2D9A94));
    public static final RegistrySupplier<Item> RBMK_PELLET_HEU233 = ITEMS.register("rbmk_pellet_heu233",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Highly Enriched Uranium-233")
                    .setYield(100_000_000).setReactivity(27.5).setHeat(1.25).setMeltingPoint(2865).setTint(0x868D82));
    public static final RegistrySupplier<Item> RBMK_PELLET_LEA = ITEMS.register("rbmk_pellet_lea",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Low Enriched Americium-242")
                    .setYield(100_000_000).setReactivity(60).setHeat(1.5).setMeltingPoint(2386).setTint(0xA88A8F));
    public static final RegistrySupplier<Item> RBMK_PELLET_LEAUS = ITEMS.register("rbmk_pellet_leaus",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Low Enriched Australium (Tasmanite)")
                    .setYield(100_000_000).setReactivity(30).setXenon(0.05, 50).setHeat(1.5).setMeltingPoint(7029).setTint(0xFFEE00));
    public static final RegistrySupplier<Item> RBMK_PELLET_LES = ITEMS.register("rbmk_pellet_les",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Low Enriched Schrabidium-326")
                    .setYield(100_000_000).setReactivity(50).setHeat(1.25).setMeltingPoint(2500)
                    .setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0x2D9A94));
    public static final RegistrySupplier<Item> RBMK_PELLET_MEA = ITEMS.register("rbmk_pellet_mea",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Medium Enriched Americium-242")
                    .setYield(100_000_000).setReactivity(35).setHeat(1.75).setMeltingPoint(2386).setTint(0xA88A8F));
    public static final RegistrySupplier<Item> RBMK_PELLET_MEN = ITEMS.register("rbmk_pellet_men",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Medium Enriched Neptunium-237")
                    .setYield(100_000_000).setReactivity(30).setHeat(0.75).setMeltingPoint(2800)
                    .setNeutronTypes(NType.ANY, NType.FAST).setTint(0x757E73));
    public static final RegistrySupplier<Item> RBMK_PELLET_MEP = ITEMS.register("rbmk_pellet_mep",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Medium Enriched Plutonium-239")
                    .setYield(100_000_000).setReactivity(35).setMeltingPoint(2744).setTint(0x656E6B));
    public static final RegistrySupplier<Item> RBMK_PELLET_MES = ITEMS.register("rbmk_pellet_mes",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Medium Enriched Schrabidium-326")
                    .setYield(100_000_000).setReactivity(75).setHeat(1.5).setMeltingPoint(2750).setTint(0x2D9A94));
    public static final RegistrySupplier<Item> RBMK_PELLET_MEU = ITEMS.register("rbmk_pellet_meu",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Medium Enriched Uranium-235")
                    .setYield(100_000_000).setReactivity(20).setHeat(0.65).setMeltingPoint(2865).setTint(0x868D82));
    public static final RegistrySupplier<Item> RBMK_PELLET_PO210BE = ITEMS.register("rbmk_pellet_po210be",
            () -> new RBMKPelletItem(new Item.Properties()).disableXenon().setFullName("Polonium-210 & Beryllium Neutron Source")
                    .setYield(25_000_000).setReactivity(0).setXenon(0.0, 50).setHeat(0.1).setDiffusion(0.05)
                    .setMeltingPoint(1287).setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0x563A26));
    public static final RegistrySupplier<Item> RBMK_PELLET_PU238BE = ITEMS.register("rbmk_pellet_pu238be",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Plutonium-238 & Beryllium Neutron Source")
                    .setYield(50_000_000).setReactivity(40).setHeat(0.1).setDiffusion(0.05)
                    .setMeltingPoint(1287).setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0x656E6B));
    public static final RegistrySupplier<Item> RBMK_PELLET_RA226BE = ITEMS.register("rbmk_pellet_ra226be",
            () -> new RBMKPelletItem(new Item.Properties()).disableXenon().setFullName("Radium-226 & Beryllium Neutron Source")
                    .setYield(100_000_000).setReactivity(0).setXenon(0.0, 50).setHeat(0.035).setDiffusion(0.5)
                    .setMeltingPoint(700).setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0xB3B6AD));
    public static final RegistrySupplier<Item> RBMK_PELLET_THMEU = ITEMS.register("rbmk_pellet_thmeu",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Thorium with MEU Driver Fuel")
                    .setYield(100_000_000).setReactivity(20).setHeat(0.65).setMeltingPoint(3350).setTint(0x665448));
    public static final RegistrySupplier<Item> RBMK_PELLET_UEU = ITEMS.register("rbmk_pellet_ueu",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Unenriched Uranium")
                    .setYield(100_000_000).setReactivity(15).setHeat(0.65).setMeltingPoint(2865).setTint(0x868D82));
    public static final RegistrySupplier<Item> RBMK_PELLET_UZH = ITEMS.register("rbmk_pellet_uzh",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Uranium Zirconium Hydride")
                    .setYield(50_000_000).setReactivity(30).setHeat(0.75).setDiffusion(0.1).setMeltingPoint(1845).setTint(0x7077AF));
    public static final RegistrySupplier<Item> RBMK_PELLET_ZFB_AM_MIX = ITEMS.register("rbmk_pellet_zfb_am_mix",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Zirconium Fast Breeder - HEP-241#MEA")
                    .setYield(50_000_000).setReactivity(20).setHeat(1.75).setMeltingPoint(2744).setTint(0xAAA36A));
    public static final RegistrySupplier<Item> RBMK_PELLET_ZFB_BISMUTH = ITEMS.register("rbmk_pellet_zfb_bismuth",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Zirconium Fast Breeder - LEU/HEP-241#Bi")
                    .setYield(50_000_000).setReactivity(20).setHeat(1.75).setMeltingPoint(2744).setTint(0xAAA36A));
    public static final RegistrySupplier<Item> RBMK_PELLET_ZFB_PU241 = ITEMS.register("rbmk_pellet_zfb_pu241",
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Zirconium Fast Breeder - HEU-235/HEP-240#Pu-241")
                    .setYield(50_000_000).setReactivity(20).setMeltingPoint(2865).setTint(0xAAA36A));
    public static final RegistrySupplier<Item> RBMK_TOOL = ITEMS.register("rbmk_tool",
            () -> new com.hbm_m.item.rbmk.RBMKToolItem(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> REACHER = ITEMS.register("reacher", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> REACTOR_CORE = ITEMS.register("reactor_core", () -> new Item(new Item.Properties()));
    // ── Spulen des Teilchenbeschleunigers (Original: ItemPACoil mit vier Metadaten) ──
    public static final RegistrySupplier<Item> PA_COIL_GOLD = ITEMS.register("pa_coil_gold",
            () -> new com.hbm_m.item.machine.ItemPACoil(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPACoil.CoilType.GOLD));
    public static final RegistrySupplier<Item> PA_COIL_NIOBIUM = ITEMS.register("pa_coil_niobium",
            () -> new com.hbm_m.item.machine.ItemPACoil(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPACoil.CoilType.NIOBIUM));
    public static final RegistrySupplier<Item> PA_COIL_BSCCO = ITEMS.register("pa_coil_bscco",
            () -> new com.hbm_m.item.machine.ItemPACoil(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPACoil.CoilType.BSCCO));
    public static final RegistrySupplier<Item> PA_COIL_CHLOROPHYTE = ITEMS.register("pa_coil_chlorophyte",
            () -> new com.hbm_m.item.machine.ItemPACoil(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPACoil.CoilType.CHLOROPHYTE));

    /** Original: {@code ItemReactorSensor} - bindet per Rechtsklick einen Forschungsreaktor. */
    public static final RegistrySupplier<Item> REACTOR_SENSOR = ITEMS.register("reactor_sensor",
            () -> new com.hbm_m.item.machine.ItemReactorSensor(new Item.Properties()));
    public static final RegistrySupplier<Item> REBAR_PLACER = ITEMS.register("rebar_placer", () -> new com.hbm_m.item.tool.ItemRebarPlacer(new Item.Properties()));
    public static final RegistrySupplier<Item> REDSTONE_SWORD = ITEMS.register("redstone_sword",
            () -> new RedstoneSword(HbmToolMaterial.STONE));
    public static final RegistrySupplier<Item> RING_PULL = ITEMS.register("ring_pull", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RING_STARMETAL = ITEMS.register("ring_starmetal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_FUEL = ITEMS.register("rocket_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROD_DUAL_EMPTY = ITEMS.register("rod_dual_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROD_EMPTY = ITEMS.register("rod_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROD_OF_DISCORD = ITEMS.register("rod_of_discord", () -> new com.hbm_m.item.tool.ItemDiscord(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> ROD_QUAD_EMPTY = ITEMS.register("rod_quad_empty", () -> new Item(new Item.Properties()));
    /** Original rod / rod_dual / rod_quad (ItemBreedingRod, je BreedingRodType ein Item; rod_quad_lead/np237/uranium siehe oben). */
    public static final java.util.Map<com.hbm_m.item.nuclear.BreedingRodType, RegistrySupplier<Item>> ROD = registerBreedingRods("rod_", 1);
    public static final java.util.Map<com.hbm_m.item.nuclear.BreedingRodType, RegistrySupplier<Item>> ROD_DUAL = registerBreedingRods("rod_dual_", 2);
    public static final java.util.Map<com.hbm_m.item.nuclear.BreedingRodType, RegistrySupplier<Item>> ROD_QUAD = registerBreedingRods("rod_quad_", 4);

    private static java.util.Map<com.hbm_m.item.nuclear.BreedingRodType, RegistrySupplier<Item>> registerBreedingRods(String prefix, int size) {
        java.util.Map<com.hbm_m.item.nuclear.BreedingRodType, RegistrySupplier<Item>> map = new java.util.EnumMap<>(com.hbm_m.item.nuclear.BreedingRodType.class);
        java.util.function.Supplier<Item> empty = size == 1 ? () -> ROD_EMPTY.get() : size == 2 ? () -> ROD_DUAL_EMPTY.get() : () -> ROD_QUAD_EMPTY.get();
        for (com.hbm_m.item.nuclear.BreedingRodType t : com.hbm_m.item.nuclear.BreedingRodType.values()) {
            if (size == 4 && t == com.hbm_m.item.nuclear.BreedingRodType.LEAD) { map.put(t, ROD_QUAD_LEAD); continue; }
            if (size == 4 && t == com.hbm_m.item.nuclear.BreedingRodType.NP237) { map.put(t, ROD_QUAD_NP237); continue; }
            if (size == 4 && t == com.hbm_m.item.nuclear.BreedingRodType.URANIUM) { map.put(t, ROD_QUAD_URANIUM); continue; }
            map.put(t, ITEMS.register(prefix + t.id(), () -> new com.hbm_m.item.nuclear.ItemBreedingRod(new Item.Properties(), t, empty)));
        }
        return map;
    }
    public static final RegistrySupplier<Item> RTG_UNIT = ITEMS.register("rtg_unit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RTTY_PAGER = ITEMS.register("rtty_pager", () -> new com.hbm_m.item.tool.ItemRTTYPager(new Item.Properties().stacksTo(1)));
    // Runes (Catalyst Matrix): ориг. ItemCustomLore().setEffect().setMaxStackSize(1) —
    // зачарованный блеск (isFoil) и стак в 1 предмет.
    public static final RegistrySupplier<Item> RUNE_BLANK = ITEMS.register("rune_blank", () -> new FoilItem(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> RUNE_DAGAZ = ITEMS.register("rune_dagaz", () -> new FoilItem(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> RUNE_HAGALAZ = ITEMS.register("rune_hagalaz", () -> new FoilItem(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> RUNE_ISA = ITEMS.register("rune_isa", () -> new FoilItem(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> RUNE_JERA = ITEMS.register("rune_jera", () -> new FoilItem(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> RUNE_THURISAZ = ITEMS.register("rune_thurisaz", () -> new FoilItem(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> SAFETY_FUSE = ITEMS.register("safety_fuse", () -> new Item(new Item.Properties()));
    /** Original {@code satellite} (ItemSatellite, Meta = EnumSatType) als Einzelgegenstaende. */
    public static final RegistrySupplier<Item> SATELLITE_SPY = ITEMS.register("satellite_spy", () -> new com.hbm_m.item.satellite.ItemSatellite(new Item.Properties()));
    public static final RegistrySupplier<Item> SATELLITE_SCANNER = ITEMS.register("satellite_scanner", () -> new com.hbm_m.item.satellite.ItemSatellite(new Item.Properties()));
    public static final RegistrySupplier<Item> SATELLITE_RADAR = ITEMS.register("satellite_radar", () -> new com.hbm_m.item.satellite.ItemSatellite(new Item.Properties()));
    public static final RegistrySupplier<Item> SATELLITE_MINER_ASTRO = ITEMS.register("satellite_miner_astro", () -> new com.hbm_m.item.satellite.ItemSatellite(new Item.Properties()));
    public static final RegistrySupplier<Item> SATELLITE_MINER_LUNAR = ITEMS.register("satellite_miner_lunar", () -> new com.hbm_m.item.satellite.ItemSatellite(new Item.Properties()));
    public static final RegistrySupplier<Item> SATELLITE_PRECISION_LASER = ITEMS.register("satellite_precision_laser", () -> new com.hbm_m.item.satellite.ItemSatellite(new Item.Properties()));
    public static final RegistrySupplier<Item> SATELLITE_DEATH_RAY = ITEMS.register("satellite_death_ray", () -> new com.hbm_m.item.satellite.ItemSatellite(new Item.Properties()));
    public static final RegistrySupplier<Item> SATELLITE_XENIUM_RESONATOR = ITEMS.register("satellite_xenium_resonator", () -> new com.hbm_m.item.satellite.ItemSatellite(new Item.Properties()));
    public static final RegistrySupplier<Item> SATELLITE_RELAY = ITEMS.register("satellite_relay", () -> new com.hbm_m.item.satellite.ItemSatellite(new Item.Properties()));
    public static final RegistrySupplier<Item> SATELLITE_DETECTOR = ITEMS.register("satellite_detector", () -> new com.hbm_m.item.satellite.ItemSatellite(new Item.Properties()));
    public static final RegistrySupplier<Item> SATELLITE_RAY_SCAN = ITEMS.register("satellite_ray_scan", () -> new com.hbm_m.item.satellite.ItemSatellite(new Item.Properties()));
    /** Original {@code satellite} Meta SCIENCE / SCIENCE_ASSEMBLER / SCIENCE_SENSOR: Weltraumlabor und seine Erweiterungen. */
    public static final RegistrySupplier<Item> SATELLITE_SCIENCE = ITEMS.register("satellite_science", () -> new com.hbm_m.item.satellite.ItemSatellite(new Item.Properties()));
    public static final RegistrySupplier<Item> SATELLITE_SCIENCE_ASSEMBLER = ITEMS.register("satellite_science_assembler", () -> new com.hbm_m.item.satellite.ItemSatellite(new Item.Properties()));
    public static final RegistrySupplier<Item> SATELLITE_SCIENCE_SENSOR = ITEMS.register("satellite_science_sensor", () -> new com.hbm_m.item.satellite.ItemSatellite(new Item.Properties()));
    /** Original {@code orbital_assembly} Meta CRYSTAL_CIRCUIT: wird per Lambda-Rakete zur 0G-Fabrik geschickt. */
    public static final RegistrySupplier<Item> ORBITAL_ASSEMBLY_CRYSTAL_CIRCUIT = ITEMS.register("orbital_assembly_crystal_circuit", () -> new com.hbm_m.item.satellite.ItemSatellite(new Item.Properties()));
    /** Original {@code circuit} Meta CRYSTAL. */
    public static final RegistrySupplier<Item> CIRCUIT_CRYSTAL = ITEMS.register("circuit_crystal", () -> new Item(new Item.Properties()));
    // ---- R4: fehlende einfache Items (1:1 aus ModItems 1.7.10) ----
    public static final RegistrySupplier<Item> BOOK_LORE = ITEMS.register("book_lore", () -> new com.hbm_m.item.special.ItemBookLore(new Item.Properties()));
    public static final RegistrySupplier<Item> HOLOTAPE_IMAGE_DIGAMMA = ITEMS.register("holotape_image_digamma", () -> new com.hbm_m.item.special.ItemHolotapeImage(new Item.Properties(), com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage.HOLO_DIGAMMA));
    public static final RegistrySupplier<Item> HOLOTAPE_IMAGE_RESTORED = ITEMS.register("holotape_image_restored", () -> new com.hbm_m.item.special.ItemHolotapeImage(new Item.Properties(), com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage.HOLO_RESTORED));
    public static final RegistrySupplier<Item> HOLOTAPE_IMAGE_FE_HALL = ITEMS.register("holotape_image_fe_hall", () -> new com.hbm_m.item.special.ItemHolotapeImage(new Item.Properties(), com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage.HOLO_FE_HALL));
    public static final RegistrySupplier<Item> HOLOTAPE_IMAGE_FE_CORRIDOR = ITEMS.register("holotape_image_fe_corridor", () -> new com.hbm_m.item.special.ItemHolotapeImage(new Item.Properties(), com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage.HOLO_FE_CORRIDOR));
    public static final RegistrySupplier<Item> HOLOTAPE_IMAGE_FE_SERVER = ITEMS.register("holotape_image_fe_server", () -> new com.hbm_m.item.special.ItemHolotapeImage(new Item.Properties(), com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage.HOLO_FE_SERVER));
    public static final RegistrySupplier<Item> HOLOTAPE_IMAGE_FEH_DOME = ITEMS.register("holotape_image_feh_dome", () -> new com.hbm_m.item.special.ItemHolotapeImage(new Item.Properties(), com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage.HOLO_FEH_DOME));
    public static final RegistrySupplier<Item> HOLOTAPE_IMAGE_FEH_BOAT = ITEMS.register("holotape_image_feh_boat", () -> new com.hbm_m.item.special.ItemHolotapeImage(new Item.Properties(), com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage.HOLO_FEH_BOAT));
    public static final RegistrySupplier<Item> HOLOTAPE_IMAGE_FEH_LSC = ITEMS.register("holotape_image_feh_lsc", () -> new com.hbm_m.item.special.ItemHolotapeImage(new Item.Properties(), com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage.HOLO_FEH_LSC));
    public static final RegistrySupplier<Item> HOLOTAPE_IMAGE_F3_RC = ITEMS.register("holotape_image_f3_rc", () -> new com.hbm_m.item.special.ItemHolotapeImage(new Item.Properties(), com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage.HOLO_F3_RC));
    public static final RegistrySupplier<Item> HOLOTAPE_IMAGE_F3_IV = ITEMS.register("holotape_image_f3_iv", () -> new com.hbm_m.item.special.ItemHolotapeImage(new Item.Properties(), com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage.HOLO_F3_IV));
    public static final RegistrySupplier<Item> HOLOTAPE_IMAGE_F3_WM = ITEMS.register("holotape_image_f3_wm", () -> new com.hbm_m.item.special.ItemHolotapeImage(new Item.Properties(), com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage.HOLO_F3_WM));
    public static final RegistrySupplier<Item> HOLOTAPE_IMAGE_NV_CRATER = ITEMS.register("holotape_image_nv_crater", () -> new com.hbm_m.item.special.ItemHolotapeImage(new Item.Properties(), com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage.HOLO_NV_CRATER));
    public static final RegistrySupplier<Item> HOLOTAPE_IMAGE_NV_DIVIDE = ITEMS.register("holotape_image_nv_divide", () -> new com.hbm_m.item.special.ItemHolotapeImage(new Item.Properties(), com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage.HOLO_NV_DIVIDE));
    public static final RegistrySupplier<Item> HOLOTAPE_IMAGE_NV_BM = ITEMS.register("holotape_image_nv_bm", () -> new com.hbm_m.item.special.ItemHolotapeImage(new Item.Properties(), com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage.HOLO_NV_BM));
    public static final RegistrySupplier<Item> HOLOTAPE_IMAGE_O_1 = ITEMS.register("holotape_image_o_1", () -> new com.hbm_m.item.special.ItemHolotapeImage(new Item.Properties(), com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage.HOLO_O_1));
    public static final RegistrySupplier<Item> HOLOTAPE_IMAGE_O_2 = ITEMS.register("holotape_image_o_2", () -> new com.hbm_m.item.special.ItemHolotapeImage(new Item.Properties(), com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage.HOLO_O_2));
    public static final RegistrySupplier<Item> HOLOTAPE_IMAGE_O_3 = ITEMS.register("holotape_image_o_3", () -> new com.hbm_m.item.special.ItemHolotapeImage(new Item.Properties(), com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage.HOLO_O_3));
    public static final RegistrySupplier<Item> HOLOTAPE_IMAGE_CHALLENGE = ITEMS.register("holotape_image_challenge", () -> new com.hbm_m.item.special.ItemHolotapeImage(new Item.Properties(), com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage.HOLO_CHALLENGE));
    public static final RegistrySupplier<Item> COLTAN_TOOL = ITEMS.register("coltan_tool", () -> new com.hbm_m.item.tool.ItemColtanCompass(new Item.Properties()));
    public static final RegistrySupplier<Item> TOOLBOX = ITEMS.register("toolbox", () -> new com.hbm_m.item.tool.ItemToolBox(new Item.Properties()));
    public static final RegistrySupplier<Item> CONVEYOR_WAND_REGULAR = ITEMS.register("conveyor_wand_regular", () -> new com.hbm_m.item.tool.ItemConveyorWand(new Item.Properties(), com.hbm_m.item.tool.ItemConveyorWand.ConveyorType.REGULAR));
    public static final RegistrySupplier<Item> CONVEYOR_WAND_EXPRESS = ITEMS.register("conveyor_wand_express", () -> new com.hbm_m.item.tool.ItemConveyorWand(new Item.Properties(), com.hbm_m.item.tool.ItemConveyorWand.ConveyorType.EXPRESS));
    public static final RegistrySupplier<Item> CONVEYOR_WAND_DOUBLE = ITEMS.register("conveyor_wand_double", () -> new com.hbm_m.item.tool.ItemConveyorWand(new Item.Properties(), com.hbm_m.item.tool.ItemConveyorWand.ConveyorType.DOUBLE));
    public static final RegistrySupplier<Item> CONVEYOR_WAND_TRIPLE = ITEMS.register("conveyor_wand_triple", () -> new com.hbm_m.item.tool.ItemConveyorWand(new Item.Properties(), com.hbm_m.item.tool.ItemConveyorWand.ConveyorType.TRIPLE));
    public static final RegistrySupplier<Item> COAL_ETERNAL = ITEMS.register("coal_eternal", () -> new com.hbm_m.item.industrial.FuelItem(new Item.Properties().stacksTo(1), 3200));
    public static final RegistrySupplier<Item> FUEL_ADDITIVE_ANTIKNOCK = ITEMS.register("fuel_additive_antiknock", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUEL_ADDITIVE_DEICER = ITEMS.register("fuel_additive_deicer", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PAGE_OF_PAGE1 = ITEMS.register("page_of_page1", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> PAGE_OF_PAGE2 = ITEMS.register("page_of_page2", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> PAGE_OF_PAGE3 = ITEMS.register("page_of_page3", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> PAGE_OF_PAGE4 = ITEMS.register("page_of_page4", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> PAGE_OF_PAGE5 = ITEMS.register("page_of_page5", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> PAGE_OF_PAGE6 = ITEMS.register("page_of_page6", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> PAGE_OF_PAGE7 = ITEMS.register("page_of_page7", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> PAGE_OF_PAGE8 = ITEMS.register("page_of_page8", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> INGOT_METAL_SCRAP = ITEMS.register("ingot_metal_scrap", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_METAL_INGOT = ITEMS.register("ingot_metal_ingot", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_METAL_COUNTER = ITEMS.register("ingot_metal_counter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_METAL_KEY = ITEMS.register("ingot_metal_key", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_METAL_BEACON = ITEMS.register("ingot_metal_beacon", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_METAL_CASING = ITEMS.register("ingot_metal_casing", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_METAL_CLOCKWORK = ITEMS.register("ingot_metal_clockwork", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_METAL_BAR = ITEMS.register("ingot_metal_bar", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_METAL_DETECTOR = ITEMS.register("ingot_metal_detector", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_BOOK_PRINTING1 = ITEMS.register("stamp_book_printing1", () -> new com.hbm_m.item.industrial.ItemStamp(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> STAMP_BOOK_PRINTING2 = ITEMS.register("stamp_book_printing2", () -> new com.hbm_m.item.industrial.ItemStamp(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> STAMP_BOOK_PRINTING3 = ITEMS.register("stamp_book_printing3", () -> new com.hbm_m.item.industrial.ItemStamp(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> STAMP_BOOK_PRINTING4 = ITEMS.register("stamp_book_printing4", () -> new com.hbm_m.item.industrial.ItemStamp(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> STAMP_BOOK_PRINTING5 = ITEMS.register("stamp_book_printing5", () -> new com.hbm_m.item.industrial.ItemStamp(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> STAMP_BOOK_PRINTING6 = ITEMS.register("stamp_book_printing6", () -> new com.hbm_m.item.industrial.ItemStamp(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> STAMP_BOOK_PRINTING7 = ITEMS.register("stamp_book_printing7", () -> new com.hbm_m.item.industrial.ItemStamp(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> STAMP_BOOK_PRINTING8 = ITEMS.register("stamp_book_printing8", () -> new com.hbm_m.item.industrial.ItemStamp(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_BOARD_BLANK = ITEMS.register("circuit_star_piece_board_blank", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_BOARD_TRANSISTOR = ITEMS.register("circuit_star_piece_board_transistor", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_BOARD_CONVERTER = ITEMS.register("circuit_star_piece_board_converter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_BRIDGE_NORTH = ITEMS.register("circuit_star_piece_bridge_north", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_BRIDGE_SOUTH = ITEMS.register("circuit_star_piece_bridge_south", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_BRIDGE_IO = ITEMS.register("circuit_star_piece_bridge_io", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_BRIDGE_BUS = ITEMS.register("circuit_star_piece_bridge_bus", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_BRIDGE_CHIPSET = ITEMS.register("circuit_star_piece_bridge_chipset", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_BRIDGE_CMOS = ITEMS.register("circuit_star_piece_bridge_cmos", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_BRIDGE_BIOS = ITEMS.register("circuit_star_piece_bridge_bios", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_CPU_REGISTER = ITEMS.register("circuit_star_piece_cpu_register", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_CPU_CLOCK = ITEMS.register("circuit_star_piece_cpu_clock", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_CPU_LOGIC = ITEMS.register("circuit_star_piece_cpu_logic", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_CPU_CACHE = ITEMS.register("circuit_star_piece_cpu_cache", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_CPU_EXT = ITEMS.register("circuit_star_piece_cpu_ext", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_CPU_SOCKET = ITEMS.register("circuit_star_piece_cpu_socket", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_MEM_SOCKET = ITEMS.register("circuit_star_piece_mem_socket", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_MEM_16K_A = ITEMS.register("circuit_star_piece_mem_16k_a", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_MEM_16K_B = ITEMS.register("circuit_star_piece_mem_16k_b", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_MEM_16K_C = ITEMS.register("circuit_star_piece_mem_16k_c", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_MEM_16K_D = ITEMS.register("circuit_star_piece_mem_16k_d", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_CARD_BOARD = ITEMS.register("circuit_star_piece_card_board", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_PIECE_CARD_PROCESSOR = ITEMS.register("circuit_star_piece_card_processor", () -> new Item(new Item.Properties()));
    /** Original ItemPlasticScrap.ScrapType (Reihenfolge = Meta), gleiche Reihenfolge wie circuit_star_piece_*. */
    public static final java.util.List<String> SCRAP_PLASTIC_TYPES = java.util.List.of(
            "board_blank", "board_transistor", "board_converter",
            "bridge_north", "bridge_south", "bridge_io", "bridge_bus", "bridge_chipset", "bridge_cmos", "bridge_bios",
            "cpu_register", "cpu_clock", "cpu_logic", "cpu_cache", "cpu_ext", "cpu_socket",
            "mem_socket", "mem_16k_a", "mem_16k_b", "mem_16k_c", "mem_16k_d",
            "card_board", "card_processor");
    /** scrap_plastic Meta 1-22 als scrap_plastic_<typ> (Meta 0 BOARD_BLANK = scrap_plastic aus ModMaterials.SCRAP_PLASTIC). */
    public static final java.util.Map<String, RegistrySupplier<Item>> SCRAP_PLASTIC_VARIANTS = registerScrapPlastic();

    private static java.util.Map<String, RegistrySupplier<Item>> registerScrapPlastic() {
        java.util.Map<String, RegistrySupplier<Item>> map = new java.util.LinkedHashMap<>();
        for (String type : SCRAP_PLASTIC_TYPES.subList(1, SCRAP_PLASTIC_TYPES.size())) {
            map.put(type, ITEMS.register("scrap_plastic_" + type, () -> new Item(new Item.Properties())));
        }
        return map;
    }

    /** Plastikschrott einer Original-Meta (ScrapType-Name klein). */
    public static Item scrapPlastic(String type) {
        if (SCRAP_PLASTIC_TYPES.get(0).equals(type)) {
            return com.hbm_m.item.material.ModMaterialItems.item(com.hbm_m.item.material.ModMaterials.SCRAP_PLASTIC,
                    com.hbm_m.item.material.MaterialShape.SCRAP);
        }
        return SCRAP_PLASTIC_VARIANTS.get(type).get();
    }

    /** Original ingot_u238m2 Meta 1-3 (ItemUnstable: ELEMENTS, ARSENIC, VAULT - Bruchstuecke ohne Zerfall). */
    public static final RegistrySupplier<Item> INGOT_U238M2_1 = ITEMS.register("ingot_u238m2_1", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_U238M2_2 = ITEMS.register("ingot_u238m2_2", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_U238M2_3 = ITEMS.register("ingot_u238m2_3", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_COMPONENT_CHIPSET = ITEMS.register("circuit_star_component_chipset", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_COMPONENT_CPU = ITEMS.register("circuit_star_component_cpu", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_COMPONENT_RAM = ITEMS.register("circuit_star_component_ram", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR_COMPONENT_CARD = ITEMS.register("circuit_star_component_card", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BATTERY_SC_EMPTY = ITEMS.register("battery_sc_empty", () -> new com.hbm_m.item.fekal_electric.ItemBatterySC(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatterySC.EnumBatterySC.EMPTY));
    public static final RegistrySupplier<Item> BATTERY_SC_WASTE = ITEMS.register("battery_sc_waste", () -> new com.hbm_m.item.fekal_electric.ItemBatterySC(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatterySC.EnumBatterySC.WASTE));
    public static final RegistrySupplier<Item> BATTERY_SC_RA226 = ITEMS.register("battery_sc_ra226", () -> new com.hbm_m.item.fekal_electric.ItemBatterySC(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatterySC.EnumBatterySC.RA226));
    public static final RegistrySupplier<Item> BATTERY_SC_TC99 = ITEMS.register("battery_sc_tc99", () -> new com.hbm_m.item.fekal_electric.ItemBatterySC(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatterySC.EnumBatterySC.TC99));
    public static final RegistrySupplier<Item> BATTERY_SC_CO60 = ITEMS.register("battery_sc_co60", () -> new com.hbm_m.item.fekal_electric.ItemBatterySC(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatterySC.EnumBatterySC.CO60));
    public static final RegistrySupplier<Item> BATTERY_SC_PU238 = ITEMS.register("battery_sc_pu238", () -> new com.hbm_m.item.fekal_electric.ItemBatterySC(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatterySC.EnumBatterySC.PU238));
    public static final RegistrySupplier<Item> BATTERY_SC_PO210 = ITEMS.register("battery_sc_po210", () -> new com.hbm_m.item.fekal_electric.ItemBatterySC(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatterySC.EnumBatterySC.PO210));
    public static final RegistrySupplier<Item> BATTERY_SC_AU198 = ITEMS.register("battery_sc_au198", () -> new com.hbm_m.item.fekal_electric.ItemBatterySC(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatterySC.EnumBatterySC.AU198));
    public static final RegistrySupplier<Item> BATTERY_SC_PB209 = ITEMS.register("battery_sc_pb209", () -> new com.hbm_m.item.fekal_electric.ItemBatterySC(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatterySC.EnumBatterySC.PB209));
    public static final RegistrySupplier<Item> BATTERY_SC_AM241 = ITEMS.register("battery_sc_am241", () -> new com.hbm_m.item.fekal_electric.ItemBatterySC(new Item.Properties(), com.hbm_m.item.fekal_electric.ItemBatterySC.EnumBatterySC.AM241));
    public static final RegistrySupplier<Item> BATTERY_POTATOS = ITEMS.register("battery_potatos", () -> new com.hbm_m.item.fekal_electric.ItemPotatos(new Item.Properties(), 500000, 0, 100));
    public static final RegistrySupplier<Item> MEMORY = ITEMS.register("memory", () -> new com.hbm_m.item.fekal_electric.ModBatteryItem(new Item.Properties(), Long.MAX_VALUE / 100L, 100000000000000L, 100000000000000L));
    public static final RegistrySupplier<Item> MYSTERYSHOVEL = ITEMS.register("mysteryshovel", () -> new com.hbm_m.item.tool.ItemMS(new Item.Properties()));
    public static final RegistrySupplier<Item> KEY_KIT = ITEMS.register("key_kit", () -> new com.hbm_m.item.tool.ItemCounterfeitKeys(new Item.Properties()));
    public static final RegistrySupplier<Item> KEY_FAKE = ITEMS.register("key_fake", () -> new com.hbm_m.item.tool.ItemKey(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> RECORD_LC = ITEMS.register("record_lc", () -> new com.hbm_m.item.special.ItemModRecord(1, () -> com.hbm_m.sound.HbmSoundsNT.get("hbm:music.recordlambdacore"), new Item.Properties(), 2091));
    public static final RegistrySupplier<Item> RECORD_SS = ITEMS.register("record_ss", () -> new com.hbm_m.item.special.ItemModRecord(1, () -> com.hbm_m.sound.HbmSoundsNT.get("hbm:music.recordsectorsweep"), new Item.Properties(), 3331));
    /** Original record_glass ("glass", music.transmission): nur in den Strandkapseln, kein Creative-Tab. */
    public static final RegistrySupplier<Item> RECORD_GLASS = ITEMS.register("record_glass", () -> new com.hbm_m.item.special.ItemModRecord(1, () -> com.hbm_m.sound.HbmSoundsNT.get("hbm:music.transmission"), new Item.Properties(), 1244));
    public static final RegistrySupplier<Item> RECORD_VC = ITEMS.register("record_vc", () -> new com.hbm_m.item.special.ItemModRecord(1, () -> com.hbm_m.sound.HbmSoundsNT.get("hbm:music.recordvortalcombat"), new Item.Properties(), 3895));
    public static final RegistrySupplier<Item> WAND_K = ITEMS.register("wand_k", () -> new com.hbm_m.item.tool.ItemWand(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_CHIP = ITEMS.register("sat_chip", () -> new com.hbm_m.item.satellite.ItemSatChip(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_COORD = ITEMS.register("sat_coord", () -> new com.hbm_m.item.satellite.ItemSatInterface(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_DESIGNATOR = ITEMS.register("sat_designator",
            () -> new com.hbm_m.item.designator.ItemSatDesignator(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_GERALD = ITEMS.register("sat_gerald",
            () -> new com.hbm_m.item.satellite.ItemSatChip(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_FOEQ = ITEMS.register("sat_foeq", () -> new com.hbm_m.item.satellite.ItemSatChip(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_SCANNER = ITEMS.register("sat_scanner", () -> new com.hbm_m.item.satellite.ItemSatChip(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_HEAD_SCANNER = ITEMS.register("sat_head_scanner", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_INTERFACE = ITEMS.register("sat_interface", () -> new com.hbm_m.item.satellite.ItemSatInterface(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_LUNAR_MINER = ITEMS.register("sat_lunar_miner", () -> new com.hbm_m.item.satellite.ItemSatChip(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_MINER = ITEMS.register("sat_miner", () -> new com.hbm_m.item.satellite.ItemSatChip(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_RELAY = ITEMS.register("sat_relay", () -> new com.hbm_m.item.satellite.ItemSatChip(new Item.Properties()));
    public static final RegistrySupplier<Item> SAWBLADE = ITEMS.register("sawblade", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHNITZEL_VEGAN = ITEMS.register("schnitzel_vegan",
            () -> com.hbm_m.item.food.HbmFoodItem.of(0, 0.6F, true).noDesc().onEaten(com.hbm_m.item.food.FoodBehaviors::schnitzelVegan).build());
    public static final RegistrySupplier<Item> SCHRABIDIUM_AXE = ITEMS.register("schrabidium_axe",
            () -> new ItemToolAbility(25F, 0, HbmToolMaterial.SCHRAB, ItemToolAbility.EnumToolType.AXE)
                    .addAbility(IWeaponAbility.RADIATION, 0)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolAreaAbility.RECURSION, 6)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 4)
                    .addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.SHREDDER, 0)
                    .addAbility(IWeaponAbility.BEHEADER, 0).setRarity(Rarity.RARE));
    public static final RegistrySupplier<Item> PCH = ITEMS.register("pch", () -> new com.hbm_m.item.tool.WeaponSpecial(com.hbm_m.item.tool.HbmToolMaterial.HAMMER, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> SCHRABIDIUM_HAMMER = ITEMS.register("schrabidium_hammer", () -> new com.hbm_m.item.tool.WeaponSpecial(com.hbm_m.item.tool.HbmToolMaterial.HAMMER, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> SCHRABIDIUM_HOE = ITEMS.register("schrabidium_hoe",
            () -> new HoeSchrabidium(HbmToolMaterial.SCHRAB));
    public static final RegistrySupplier<Item> SCHRABIDIUM_PICKAXE = ITEMS.register("schrabidium_pickaxe",
            () -> new ItemToolAbility(20F, 0, HbmToolMaterial.SCHRAB, ItemToolAbility.EnumToolType.PICKAXE)
                    .addAbility(IWeaponAbility.RADIATION, 0)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolAreaAbility.RECURSION, 6)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 4)
                    .addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.SHREDDER, 0).setRarity(Rarity.RARE));
    public static final RegistrySupplier<Item> SCHRABIDIUM_SHOVEL = ITEMS.register("schrabidium_shovel",
            () -> new ItemToolAbility(15F, 0, HbmToolMaterial.SCHRAB, ItemToolAbility.EnumToolType.SHOVEL)
                    .addAbility(IWeaponAbility.RADIATION, 0)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolAreaAbility.RECURSION, 6)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 4)
                    .addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.SHREDDER, 0).setRarity(Rarity.RARE));
    public static final RegistrySupplier<Item> SCHRABIDIUM_SWORD = ITEMS.register("schrabidium_sword",
            () -> new ItemSwordAbility(75F, 0, HbmToolMaterial.SCHRAB)
                    .addAbility(IWeaponAbility.RADIATION, 1)
                    .addAbility(IWeaponAbility.VAMPIRE, 0).setRarity(Rarity.RARE));
    public static final RegistrySupplier<Item> SCRAPS = ITEMS.register("scraps", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCREWDRIVER_DESH = ITEMS.register("screwdriver_desh", () -> new ScrewdriverItem(0, new Item.Properties()));
    public static final RegistrySupplier<Item> SCRUMPY = ITEMS.register("scrumpy", () -> new com.hbm_m.armormod.item.ItemModRevive(1));
    public static final RegistrySupplier<Item> SEG_10 = ITEMS.register("seg_10", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SEG_15 = ITEMS.register("seg_15", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SEG_20 = ITEMS.register("seg_20", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SERUM = ITEMS.register("serum", () -> new com.hbm_m.armormod.item.ItemModSerum());
    public static final RegistrySupplier<Item> SERVO_SET = ITEMS.register("servo_set", () -> new com.hbm_m.armormod.item.ItemModServos());
    public static final RegistrySupplier<Item> SERVO_SET_DESH = ITEMS.register("servo_set_desh", () -> new com.hbm_m.armormod.item.ItemModServos());
    public static final RegistrySupplier<Item> SETTINGS_TOOL = ITEMS.register("settings_tool", () -> new com.hbm_m.item.tool.ItemSettingsTool(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> SHACKLES = ITEMS.register("shackles", () -> new com.hbm_m.armormod.item.ItemModShackles());
    public static final RegistrySupplier<Item> SHIMMER_AXE = ITEMS.register("shimmer_axe", () -> new com.hbm_m.item.tool.WeaponSpecial(com.hbm_m.item.tool.HbmToolMaterial.SLEDGE, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> SHIMMER_AXE_HEAD = ITEMS.register("shimmer_axe_head", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHIMMER_HANDLE = ITEMS.register("shimmer_handle", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHIMMER_HEAD = ITEMS.register("shimmer_head", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHIMMER_SLEDGE = ITEMS.register("shimmer_sledge", () -> new com.hbm_m.item.tool.WeaponSpecial(com.hbm_m.item.tool.HbmToolMaterial.SLEDGE, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> SINGULARITY_COUNTER_RESONANT = ITEMS.register("singularity_counter_resonant", () -> new com.hbm_m.item.special.ItemDrop(new Item.Properties().stacksTo(1), () -> ModItems.NUCLEAR_WASTE.get()));
    public static final RegistrySupplier<Item> SINGULARITY_SUPER_HEATED = ITEMS.register("singularity_super_heated", () -> new com.hbm_m.item.special.ItemDrop(new Item.Properties().stacksTo(1), () -> ModItems.NUCLEAR_WASTE.get()));
    public static final RegistrySupplier<Item> SINGULARITY_SPARK = ITEMS.register("singularity_spark", () -> new com.hbm_m.item.special.ItemDrop(new Item.Properties().stacksTo(1), () -> ModItems.NUCLEAR_WASTE.get()));
    public static final RegistrySupplier<Item> SINGULARITY = ITEMS.register("singularity", () -> new com.hbm_m.item.special.ItemDrop(new Item.Properties().stacksTo(1), () -> ModItems.NUCLEAR_WASTE.get()));
    public static final RegistrySupplier<Item> SIOX = ITEMS.register("siox",
            () -> new com.hbm_m.item.food.SpecialFoodItems.PillItem());
    public static final RegistrySupplier<Item> SIPHON = ITEMS.register("siphon", () -> new com.hbm_m.item.machine.ItemFluidSiphon(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> SMASHING_HAMMER = ITEMS.register("smashing_hammer",
            () -> new ItemToolAbility(12F, -0.1, HbmToolMaterial.STEEL, ItemToolAbility.EnumToolType.MINER, new Item.Properties().durability(2500), true)
                    .addAbility(IToolHarvestAbility.SHREDDER, 0));
    public static final RegistrySupplier<Item> SOLID_FUEL = ITEMS.register("solid_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLID_FUEL_BF = ITEMS.register("solid_fuel_bf", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLID_FUEL_PRESTO = ITEMS.register("solid_fuel_presto", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLID_FUEL_PRESTO_BF = ITEMS.register("solid_fuel_presto_bf", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLID_FUEL_PRESTO_TRIPLET = ITEMS.register("solid_fuel_presto_triplet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLID_FUEL_PRESTO_TRIPLET_BF = ITEMS.register("solid_fuel_presto_triplet_bf", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLINIUM_CORE = ITEMS.register("solinium_core", () -> new com.hbm_m.item.bomb.ItemBombPart(new Item.Properties().stacksTo(1), () -> com.hbm_m.block.ModBlocks.NUKE_SOLINIUM.get()));
    public static final RegistrySupplier<Item> SOLINIUM_IGNITER = ITEMS.register("solinium_igniter", () -> new com.hbm_m.item.bomb.ItemBombPart(new Item.Properties().stacksTo(1), () -> com.hbm_m.block.ModBlocks.NUKE_SOLINIUM.get()));
    public static final RegistrySupplier<Item> SOLINIUM_KIT = ITEMS.register("solinium_kit", () -> new com.hbm_m.item.special.ItemStarterKit("solinium_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> SOLINIUM_PROPELLANT = ITEMS.register("solinium_propellant", () -> new com.hbm_m.item.bomb.ItemBombPart(new Item.Properties().stacksTo(1), () -> com.hbm_m.block.ModBlocks.NUKE_SOLINIUM.get()));
    public static final RegistrySupplier<Item> SOPSIGN = ITEMS.register("sopsign", () -> new com.hbm_m.item.tool.WeaponSpecial(com.hbm_m.item.tool.HbmToolMaterial.ALLOY, new Item.Properties()));
    public static final RegistrySupplier<Item> SPAWN_DUCK = ITEMS.register("spawn_duck", () -> new com.hbm_m.item.special.ItemChopper(new Item.Properties().stacksTo(16)));
    public static final RegistrySupplier<Item> SPAWN_UFO = ITEMS.register("spawn_ufo", () -> new com.hbm_m.item.special.ItemChopper(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> SPAWN_WORM = ITEMS.register("spawn_worm", () -> new com.hbm_m.item.special.ItemChopper(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> SPHERE_STEEL = ITEMS.register("sphere_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SPIDER_MILK = ITEMS.register("spider_milk", () -> new com.hbm_m.armormod.item.ItemModMilk());
    public static final RegistrySupplier<Item> SPONGEBOB_MACARONI = ITEMS.register("spongebob_macaroni",
            () -> com.hbm_m.item.food.HbmFoodItem.of(5, 1F, false).build());
    public static final RegistrySupplier<Item> STAMP_357 = ITEMS.register("stamp_357", () -> new com.hbm_m.item.industrial.ItemStamp(new Item.Properties().stacksTo(1), 1000));
    public static final RegistrySupplier<Item> STAMP_44 = ITEMS.register("stamp_44", () -> new com.hbm_m.item.industrial.ItemStamp(new Item.Properties().stacksTo(1), 1000));
    public static final RegistrySupplier<Item> STAMP_50 = ITEMS.register("stamp_50", () -> new com.hbm_m.item.industrial.ItemStamp(new Item.Properties().stacksTo(1), 1000));
    public static final RegistrySupplier<Item> STAMP_9 = ITEMS.register("stamp_9", () -> new com.hbm_m.item.industrial.ItemStamp(new Item.Properties().stacksTo(1), 1000));
    public static final RegistrySupplier<Item> STATIC_SANDWICH = ITEMS.register("static_sandwich",
            () -> com.hbm_m.item.food.HbmFoodItem.of(6, 1F, false).build());
    public static final RegistrySupplier<Item> EUPHEMIUM_KIT = ITEMS.register("euphemium_kit", () -> new com.hbm_m.item.special.ItemStarterKit("euphemium_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> STEALTH_BOY = ITEMS.register("stealth_boy", () -> new com.hbm_m.item.special.ItemStarterKit("stealth_boy", new Item.Properties()));
    public static final RegistrySupplier<Item> STICK_C4 = ITEMS.register("stick_c4", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STICK_DYNAMITE = ITEMS.register("stick_dynamite", () -> new com.hbm_m.item.weapon.ItemGrenadeDynamite(3, new Item.Properties()));
    public static final RegistrySupplier<Item> STICK_DYNAMITE_FISHING = ITEMS.register("stick_dynamite_fishing", () -> new com.hbm_m.item.weapon.ItemGrenadeFishing(3, new Item.Properties()));
    public static final RegistrySupplier<Item> STICK_SEMTEX = ITEMS.register("stick_semtex", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STICK_TNT = ITEMS.register("stick_tnt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STOPSIGN = ITEMS.register("stopsign", () -> new com.hbm_m.item.tool.WeaponSpecial(com.hbm_m.item.tool.HbmToolMaterial.ALLOY, new Item.Properties()));
    public static final RegistrySupplier<Item> STRUCTURE_CUSTOMMACHINE = ITEMS.register("structure_custommachine", () -> new com.hbm_m.item.tool.ItemCMStructure(new Item.Properties()));
    public static final RegistrySupplier<Item> SURVEY_SCANNER = ITEMS.register("survey_scanner", () -> new com.hbm_m.item.tool.ItemSurveyScanner(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> SYRINGE_ANTIDOTE = ITEMS.register("syringe_antidote", () -> com.hbm_m.item.special.ItemSimpleConsumable.syringeAntidote());
    public static final RegistrySupplier<Item> SYRINGE_AWESOME = ITEMS.register("syringe_awesome", () -> com.hbm_m.item.special.ItemSimpleConsumable.syringeAwesome());
    public static final RegistrySupplier<Item> SYRINGE_EMPTY = ITEMS.register("syringe_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_METAL_EMPTY = ITEMS.register("syringe_metal_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_METAL_MEDX = ITEMS.register("syringe_metal_medx", () -> new com.hbm_m.item.special.ItemSyringe(com.hbm_m.item.special.ItemSyringe.Type.MEDX, new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_METAL_PSYCHO = ITEMS.register("syringe_metal_psycho", () -> new com.hbm_m.item.special.ItemSyringe(com.hbm_m.item.special.ItemSyringe.Type.PSYCHO, new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_METAL_STIMPAK = ITEMS.register("syringe_metal_stimpak", () -> new com.hbm_m.item.special.ItemSyringe(com.hbm_m.item.special.ItemSyringe.Type.STIMPAK, new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_METAL_SUPER = ITEMS.register("syringe_metal_super", () -> new com.hbm_m.item.special.ItemSyringe(com.hbm_m.item.special.ItemSyringe.Type.SUPER, new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_MKUNICORN = ITEMS.register("syringe_mkunicorn", () -> new com.hbm_m.item.special.ItemSyringe(com.hbm_m.item.special.ItemSyringe.Type.MKUNICORN, new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_POISON = ITEMS.register("syringe_poison", () -> com.hbm_m.item.special.ItemSimpleConsumable.syringePoison());
    public static final RegistrySupplier<Item> SYRINGE_TAINT = ITEMS.register("syringe_taint", () -> new com.hbm_m.item.special.ItemSyringe(com.hbm_m.item.special.ItemSyringe.Type.TAINT, new Item.Properties()));
    public static final RegistrySupplier<Item> TANK_STEEL = ITEMS.register("tank_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TEM_FLAKES = ITEMS.register("tem_flakes",
            () -> com.hbm_m.item.food.HbmFoodItem.of(0, 0, false).alwaysEdible().onEaten(com.hbm_m.item.food.FoodBehaviors::temFlakes).build());
    public static final RegistrySupplier<Item> THERMO_ELEMENT = ITEMS.register("thermo_element", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> THRUSTER_NUCLEAR = ITEMS.register("thruster_nuclear", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TITANIUM_FILTER = ITEMS.register("titanium_filter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TRINITITE = ITEMS.register("trinitite", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    public static final RegistrySupplier<Item> TSAR_CORE = ITEMS.register("tsar_core", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> TSAR_KIT = ITEMS.register("tsar_kit", () -> new com.hbm_m.item.special.ItemStarterKit("tsar_kit", new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> TURBINE_TUNGSTEN = ITEMS.register("turbine_tungsten", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TURRET_CHIP = ITEMS.register("turret_chip", () -> new com.hbm_m.item.machine.ItemTurretBiometry(new Item.Properties().stacksTo(1))); // ItemTurretChip extends ItemTurretBiometry
    /** Gelenkte Raketenvarianten fuer den Himars-Turret (Original: {@code ItemAmmoHIMARS}). */
    public static final RegistrySupplier<Item> ROCKET_HIMARS_STANDARD = ITEMS.register("rocket_himars_standard", () -> new com.hbm_m.item.weapon.ItemAmmoHIMARS(com.hbm_m.item.weapon.ItemAmmoHIMARS.SMALL, new Item.Properties()));
    // The original's ItemAmmoHIMARS ships eight variants; the port was missing the two
    // large-calibre ones (LARGE / LARGE_TB, "single" and "single_tb").
    public static final RegistrySupplier<Item> ROCKET_HIMARS_SINGLE = ITEMS.register("rocket_himars_single",
            () -> new com.hbm_m.item.weapon.ItemAmmoHIMARS(com.hbm_m.item.weapon.ItemAmmoHIMARS.LARGE, new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_HIMARS_SINGLE_TB = ITEMS.register("rocket_himars_single_tb",
            () -> new com.hbm_m.item.weapon.ItemAmmoHIMARS(com.hbm_m.item.weapon.ItemAmmoHIMARS.LARGE_TB, new Item.Properties()));

    public static final RegistrySupplier<Item> ROCKET_HIMARS_HE = ITEMS.register("rocket_himars_he", () -> new com.hbm_m.item.weapon.ItemAmmoHIMARS(com.hbm_m.item.weapon.ItemAmmoHIMARS.SMALL_HE, new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_HIMARS_LAVA = ITEMS.register("rocket_himars_lava", () -> new com.hbm_m.item.weapon.ItemAmmoHIMARS(com.hbm_m.item.weapon.ItemAmmoHIMARS.SMALL_LAVA, new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_HIMARS_MINI_NUKE = ITEMS.register("rocket_himars_mini_nuke", () -> new com.hbm_m.item.weapon.ItemAmmoHIMARS(com.hbm_m.item.weapon.ItemAmmoHIMARS.SMALL_MINI_NUKE, new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_HIMARS_WP = ITEMS.register("rocket_himars_wp", () -> new com.hbm_m.item.weapon.ItemAmmoHIMARS(com.hbm_m.item.weapon.ItemAmmoHIMARS.SMALL_WP, new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_HIMARS_THERMOBARIC = ITEMS.register("rocket_himars_thermobaric", () -> new com.hbm_m.item.weapon.ItemAmmoHIMARS(com.hbm_m.item.weapon.ItemAmmoHIMARS.SMALL_TB, new Item.Properties()));
    /** Fehlende Missile-Assembly-Teile (Original: {@code ItemCustomMissilePart} mit Typ FUSELAGE/CHIP). */
    public static final RegistrySupplier<Item> MISSILE_FUSELAGE = ITEMS.register("missile_fuselage", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_CHIP = ITEMS.register("missile_chip", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TWINKIE = ITEMS.register("twinkie",
            () -> com.hbm_m.item.food.HbmFoodItem.of(3, 0.25F, false).build());
    public static final RegistrySupplier<Item> ULLAPOOL_CABER = ITEMS.register("ullapool_caber", () -> new com.hbm_m.item.tool.WeaponSpecial(com.hbm_m.item.tool.HbmToolMaterial.STEEL, new Item.Properties()));
    public static final RegistrySupplier<Item> UNDEFINED = ITEMS.register("undefined", () -> new com.hbm_m.item.UndefinedItem(new Item.Properties()));
    public static final RegistrySupplier<Item> VOLCANIC_AXE = ITEMS.register("volcanic_axe",
            () -> new ItemToolAbility(25F, 0, HbmToolMaterial.VOLCANIC, ItemToolAbility.EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 2)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IWeaponAbility.FIRE, 1)
                    .addAbility(IWeaponAbility.VAMPIRE, 1)
                    .addAbility(IWeaponAbility.BEHEADER, 0));
    public static final RegistrySupplier<Item> VOLCANIC_PICKAXE = ITEMS.register("volcanic_pickaxe",
            () -> new ItemToolAbility(15F, 0, HbmToolMaterial.VOLCANIC, ItemToolAbility.EnumToolType.MINER)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 2)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IWeaponAbility.FIRE, 0)
                    .addAbility(IWeaponAbility.VAMPIRE, 0)
                    .addAbility(IWeaponAbility.BEHEADER, 0).setDepthRockBreaker());
    public static final RegistrySupplier<Item> WAND_D = ITEMS.register("wand_d", () -> new com.hbm_m.item.tool.ItemWandD(new Item.Properties()));
    /** 1:1 ItemWandS (Strukturstab). */
    public static final RegistrySupplier<Item> WAND_S = ITEMS.register("wand_s", () -> new com.hbm_m.item.tool.ItemWandS(new Item.Properties()));
    /** 1:1 ItemStructureSingle/Solid/Pattern/Randomized/Randomly (Strukturwerkzeuge, kein Kreativtab). */
    public static final RegistrySupplier<Item> STRUCTURE_SINGLE = ITEMS.register("structure_single", () -> new com.hbm_m.item.tool.ItemStructureSingle(new Item.Properties()));
    public static final RegistrySupplier<Item> STRUCTURE_SOLID = ITEMS.register("structure_solid", () -> new com.hbm_m.item.tool.ItemStructureSolid(new Item.Properties()));
    public static final RegistrySupplier<Item> STRUCTURE_PATTERN = ITEMS.register("structure_pattern", () -> new com.hbm_m.item.tool.ItemStructurePattern(new Item.Properties()));
    public static final RegistrySupplier<Item> STRUCTURE_RANDOMIZED = ITEMS.register("structure_randomized", () -> new com.hbm_m.item.tool.ItemStructureRandomized(new Item.Properties()));
    public static final RegistrySupplier<Item> STRUCTURE_RANDOMLY = ITEMS.register("structure_randomly", () -> new com.hbm_m.item.tool.ItemStructureRandomly(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_INCENDIARY_LARGE = ITEMS.register("warhead_incendiary_large", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_MOX = ITEMS.register("waste_mox", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_MOX = ITEMS.register("waste_plate_mox", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_PU238BE = ITEMS.register("waste_plate_pu238be", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_RA226BE = ITEMS.register("waste_plate_ra226be", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_SA326 = ITEMS.register("waste_plate_sa326", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_U233 = ITEMS.register("waste_plate_u233", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_U235 = ITEMS.register("waste_plate_u235", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_PU239 = ITEMS.register("waste_plate_pu239", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLUTONIUM = ITEMS.register("waste_plutonium", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_SCHRABIDIUM = ITEMS.register("waste_schrabidium", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_THORIUM = ITEMS.register("waste_thorium", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_URANIUM = ITEMS.register("waste_uranium", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_ZFB_MOX = ITEMS.register("waste_zfb_mox", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    public static final RegistrySupplier<Item> WATCH = ITEMS.register("watch", () -> new ItemCustomLore(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    /** 1:1 {@code siren_track} (ItemCassette): je TrackType ein Gegenstand, Original-Reihenfolge (getSubItems). */
    public static final RegistrySupplier<Item> CASSETTE_HATCH = ITEMS.register("cassette_hatch", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.HATCH, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_AUTOPILOT = ITEMS.register("cassette_autopilot", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.ATUOPILOT, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_AMS_SIREN = ITEMS.register("cassette_ams_siren", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.AMS_SIREN, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_BLAST_DOOR = ITEMS.register("cassette_blast_door", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.BLAST_DOOR, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_APC_LOOP = ITEMS.register("cassette_apc_loop", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.APC_LOOP, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_KLAXON = ITEMS.register("cassette_klaxon", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.KLAXON, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_KLAXON_A = ITEMS.register("cassette_klaxon_a", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.KLAXON_A, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_KLAXON_B = ITEMS.register("cassette_klaxon_b", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.KLAXON_B, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_REGULAR_SIREN = ITEMS.register("cassette_regular_siren", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.SIREN, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_CLASSIC_SIREN = ITEMS.register("cassette_classic_siren", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.CLASSIC, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_BANK_ALARM = ITEMS.register("cassette_bank_alarm", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.BANK_ALARM, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_BEEP_SIREN = ITEMS.register("cassette_beep_siren", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.BEEP_SIREN, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_CONTAINER_ALARM = ITEMS.register("cassette_container_alarm", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.CONTAINER_ALARM, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_SWEEP_SIREN = ITEMS.register("cassette_sweep_siren", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.SWEEP_SIREN, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_STRIDER_SIREN = ITEMS.register("cassette_strider_siren", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.STRIDER_SIREN, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_AIR_RAID = ITEMS.register("cassette_air_raid", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.AIR_RAID, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_NOSTROMO_SIREN = ITEMS.register("cassette_nostromo_siren", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.NOSTROMO_SIREN, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_EAS_ALARM = ITEMS.register("cassette_eas_alarm", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.EAS_ALARM, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_APC_PASS = ITEMS.register("cassette_apc_pass", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.APC_PASS, new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_RAZORTRAIN = ITEMS.register("cassette_razortrain", () -> new com.hbm_m.item.machine.ItemCassette(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.RAZORTRAIN, new Item.Properties()));
    /** Alle Kassetten in TrackType-Reihenfolge (Creative-Tab, Item-Modelle, Farben). */
    public static final java.util.List<RegistrySupplier<Item>> CASSETTES = java.util.List.of(CASSETTE_HATCH, CASSETTE_AUTOPILOT, CASSETTE_AMS_SIREN, CASSETTE_BLAST_DOOR, CASSETTE_APC_LOOP, CASSETTE_KLAXON, CASSETTE_KLAXON_A, CASSETTE_KLAXON_B, CASSETTE_REGULAR_SIREN, CASSETTE_CLASSIC_SIREN, CASSETTE_BANK_ALARM, CASSETTE_BEEP_SIREN, CASSETTE_CONTAINER_ALARM, CASSETTE_SWEEP_SIREN, CASSETTE_STRIDER_SIREN, CASSETTE_AIR_RAID, CASSETTE_NOSTROMO_SIREN, CASSETTE_EAS_ALARM, CASSETTE_APC_PASS, CASSETTE_RAZORTRAIN);
    public static final RegistrySupplier<Item> WD40 = ITEMS.register("wd40", () -> new com.hbm_m.armormod.item.ItemModWD40());
    public static final RegistrySupplier<Item> WILD_P = ITEMS.register("wild_p", () -> new com.hbm_m.armormod.item.ItemModRevive(3));
    public static final RegistrySupplier<Item> WINGS_LIMP = ITEMS.register("wings_limp", () -> new com.hbm_m.armormod.item.WingsMurk(new Item.Properties()));
    public static final RegistrySupplier<Item> WINGS_MURK = ITEMS.register("wings_murk", () -> new com.hbm_m.armormod.item.WingsMurk(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRING_RED_COPPER = ITEMS.register("wiring_red_copper", () -> new com.hbm_m.item.tool.ItemWiring(new Item.Properties()));
    public static final RegistrySupplier<Item> WOOD_GAVEL = ITEMS.register("wood_gavel", () -> new com.hbm_m.item.tool.WeaponSpecial(com.hbm_m.item.tool.HbmToolMaterial.WOOD, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> WRENCH = ITEMS.register("wrench", () -> new com.hbm_m.item.tool.ItemWrench(com.hbm_m.item.tool.HbmToolMaterial.STEEL, new Item.Properties()));
    public static final RegistrySupplier<Item> WRENCH_ARCHINEER = ITEMS.register("wrench_archineer", () -> new com.hbm_m.item.tool.ItemToolingWeapon(com.hbm_m.api.block.IToolable.ToolType.WRENCH, 1000, 12F, new Item.Properties()));
    public static final RegistrySupplier<Item> WRENCH_FLIPPED = ITEMS.register("wrench_flipped", () -> new com.hbm_m.item.tool.WeaponSpecial(com.hbm_m.item.tool.HbmToolMaterial.ELEC, new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> XANAX = ITEMS.register("xanax",
            () -> new com.hbm_m.item.food.SpecialFoodItems.PillItem());

    // ==================== Заглушки диапазона вкладки Parts (id 4098–4500, раздел C отчёта) ====================
    // Простые Item-заглушки предметов, отсутствовавших в порте; registry-имена соответствуют оригиналу 1.7.10.
    // billet_les/nugget_les генерируются материалом LES_FUEL (id "les"); слиток — ручной предмет
    // INGOT_LES (id "ingot_les", как в оригинале).
    public static final RegistrySupplier<Item> INGOT_HES = ITEMS.register("ingot_hes", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_LES = ITEMS.register("ingot_les", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_REDSTONE = ITEMS.register("ingot_redstone", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_BORAX = ITEMS.register("ingot_borax", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_SODIUM = ITEMS.register("ingot_sodium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_SLAG = ITEMS.register("ingot_slag", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COAL_COKE = ITEMS.register("coal_coke", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LIGNITE_COKE = ITEMS.register("lignite_coke", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COAL_BRIQUETTE = ITEMS.register("coal_briquette", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LIGNITE_BRIQUETTE = ITEMS.register("lignite_briquette", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAWDUST_BRIQUETTE = ITEMS.register("sawdust_briquette", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NITER = ITEMS.register("niter", () -> new Item(new Item.Properties()));
    // "coltan_powder" занят ModItems.POWDER_COLTAN (Crushed Coltan); очищенный колтан = "powder_coltan" (как в оригинале).
    public static final RegistrySupplier<Item> POWDER_COLTAN_PURE = ITEMS.register("powder_coltan", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_TEKTITE = ITEMS.register("powder_tektite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_IMPURE_OSMIRIDIUM = ITEMS.register("powder_impure_osmiridium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_CHLOROPHYTE = ITEMS.register("powder_chlorophyte", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_TCALLOY = ITEMS.register("powder_tcalloy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_POISON = ITEMS.register("powder_poison",
            () -> new LoreTooltipItem(List.of(
                    Component.translatable("tooltip.hbm_m.poison_powder.desc1").withStyle(ChatFormatting.GRAY),
                    Component.translatable("tooltip.hbm_m.poison_powder.desc2").withStyle(ChatFormatting.GRAY)),
                    new Item.Properties()));
    public static final RegistrySupplier<Item> MOONSTONE = ITEMS.register("moonstone", () -> new Item(new Item.Properties()));
    // Кусок криолита (ориг. 4447/2, chunk_ore.cryolite) — отдельный предмет по образцу malachite/moonstone.
    public static final RegistrySupplier<Item> CRYOLITE_CHUNK = ITEMS.register("cryolite_chunk", () -> new Item(new Item.Properties()));
    // Фуллерен (ориг. 4376/5, powder_ash.fullerene) — в порте ash — отдельные предметы.
    public static final RegistrySupplier<Item> FULLERENE = ITEMS.register("fullerene", () -> new Item(new Item.Properties()));
    // Фрагменты бедрок-руды (в оригинале — мета-предмет на 31 материал; в оригинале одна базовая текстура).
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_COAL = ITEMS.register("bedrock_ore_fragment_coal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_LIGNITE = ITEMS.register("bedrock_ore_fragment_lignite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_IRON = ITEMS.register("bedrock_ore_fragment_iron", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_GOLD = ITEMS.register("bedrock_ore_fragment_gold", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_REDSTONE = ITEMS.register("bedrock_ore_fragment_redstone", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_BAUXITE = ITEMS.register("bedrock_ore_fragment_bauxite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_CRYOLITE = ITEMS.register("bedrock_ore_fragment_cryolite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_URANIUM = ITEMS.register("bedrock_ore_fragment_uranium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_U238 = ITEMS.register("bedrock_ore_fragment_u238", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_PO210 = ITEMS.register("bedrock_ore_fragment_po210", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_TC99 = ITEMS.register("bedrock_ore_fragment_tc99", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_TITANIUM = ITEMS.register("bedrock_ore_fragment_titanium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_TUNGSTEN = ITEMS.register("bedrock_ore_fragment_tungsten", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_ALUMINIUM = ITEMS.register("bedrock_ore_fragment_aluminium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_BISMUTH = ITEMS.register("bedrock_ore_fragment_bismuth", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_NEODYMIUM = ITEMS.register("bedrock_ore_fragment_neodymium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_NIOBIUM = ITEMS.register("bedrock_ore_fragment_niobium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_BERYLLIUM = ITEMS.register("bedrock_ore_fragment_beryllium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_COBALT = ITEMS.register("bedrock_ore_fragment_cobalt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_BORON = ITEMS.register("bedrock_ore_fragment_boron", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_BORAX = ITEMS.register("bedrock_ore_fragment_borax", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_ZIRCONIUM = ITEMS.register("bedrock_ore_fragment_zirconium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_SODIUM = ITEMS.register("bedrock_ore_fragment_sodium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_STRONTIUM = ITEMS.register("bedrock_ore_fragment_strontium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_LITHIUM = ITEMS.register("bedrock_ore_fragment_lithium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_SULFUR = ITEMS.register("bedrock_ore_fragment_sulfur", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_FLUORITE = ITEMS.register("bedrock_ore_fragment_fluorite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_CHLOROCALCITE = ITEMS.register("bedrock_ore_fragment_chlorocalcite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_CINNABAR = ITEMS.register("bedrock_ore_fragment_cinnabar", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_SILICON = ITEMS.register("bedrock_ore_fragment_silicon", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_RARE_EARTH = ITEMS.register("bedrock_ore_fragment_rare_earth", () -> new Item(new Item.Properties()));
    // Недостающие 13 фрагментов бедрок-руды (ориг. 4404, ItemAutogen FRAGMENT); текстура —
    // серый шаблон bedrock_ore_fragment.png, тинт solidColorLight материала в ClientSetup.
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_DIAMOND = ITEMS.register("bedrock_ore_fragment_diamond", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_THORIUM = ITEMS.register("bedrock_ore_fragment_thorium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_RA226 = ITEMS.register("bedrock_ore_fragment_ra226", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_COPPER = ITEMS.register("bedrock_ore_fragment_copper", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_LEAD = ITEMS.register("bedrock_ore_fragment_lead", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_TANTALIUM = ITEMS.register("bedrock_ore_fragment_tantalium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_EMERALD = ITEMS.register("bedrock_ore_fragment_emerald", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_LANTHANIUM = ITEMS.register("bedrock_ore_fragment_lanthanium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_SODALITE = ITEMS.register("bedrock_ore_fragment_sodalite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_KNO = ITEMS.register("bedrock_ore_fragment_kno", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_PHOSPHORUS = ITEMS.register("bedrock_ore_fragment_phosphorus", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_MOLYSITE = ITEMS.register("bedrock_ore_fragment_molysite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_ASBESTOS = ITEMS.register("bedrock_ore_fragment_asbestos", () -> new Item(new Item.Properties()));
    // ==================== Заглушки хвоста вкладки Parts (id 4502+, оригинальный порядок вызовов registerItem) ====================
    public static final RegistrySupplier<Item> CHEMICAL_DYE = ITEMS.register("chemical_dye", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CRAYON = ITEMS.register("crayon", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_GENERIC = ITEMS.register("part_generic", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ITEM_EXPENSIVE = ITEMS.register("item_expensive", () -> new LoreTooltipItem(List.of(
            Component.translatable("tooltip.hbm_m.item_expensive.desc").withStyle(ChatFormatting.RED)),
            new Item.Properties()));
    public static final RegistrySupplier<Item> PLANT_ITEM = ITEMS.register("plant_item", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CASING = ITEMS.register("casing", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_BUCKSHOT = ITEMS.register("pellet_buckshot", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_CHARGED = ITEMS.register("pellet_charged", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> UPGRADE_MUFFLER = ITEMS.register("upgrade_muffler", () -> new com.hbm_m.item.machine.ItemMuffler(new Item.Properties()));
    public static final RegistrySupplier<Item> UPGRADE_TEMPLATE = ITEMS.register("upgrade_template", () -> new Item(new Item.Properties()));
    // Обеднённое топливо: в оригинале ItemDepletedFuel; у u233/u235/natural отдельной текстуры нет — используется waste_uranium (как в оригинале).
    public static final RegistrySupplier<Item> WASTE_NATURAL_URANIUM = ITEMS.register("waste_natural_uranium", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_U233 = ITEMS.register("waste_u233", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_U235 = ITEMS.register("waste_u235", () -> new com.hbm_m.item.special.ItemNuclearWaste(new Item.Properties()));
    // Остатки хвоста Parts, не имевшие заглушек: shell/pipe/bolt (в оригинале ItemAutogen),
    // plate_welded/wire_dense (автоген-формы), circuit (в оригинале ItemCircuit с вариантами).
    public static final RegistrySupplier<Item> SHELL = ITEMS.register("shell", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PIPE = ITEMS.register("pipe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLT = ITEMS.register("bolt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_WELDED = ITEMS.register("plate_welded", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_DENSE = ITEMS.register("wire_dense", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT = ITEMS.register("circuit", () -> new Item(new Item.Properties()));


    // ==================== Restport R2: fehlende Nahrung/Verbrauchsgueter ====================
    public static final RegistrySupplier<Item> MED_SCHIZOPHRENIA = ITEMS.register("med_schizophrenia",
            () -> com.hbm_m.item.food.HbmFoodItem.of(0, 0, false).build());
    /** Original {@code quesadilla} mit der ID {@code cheese_quesadilla}. */
    public static final RegistrySupplier<Item> CHEESE_QUESADILLA = ITEMS.register("cheese_quesadilla",
            () -> com.hbm_m.item.food.HbmFoodItem.of(8, 1F, false).build());
    public static final RegistrySupplier<Item> FMN = ITEMS.register("fmn",
            () -> new com.hbm_m.item.food.SpecialFoodItems.PillItem());
    public static final RegistrySupplier<Item> FIVE_HTP = ITEMS.register("five_htp",
            () -> new com.hbm_m.item.food.SpecialFoodItems.PillItem());
    public static final RegistrySupplier<Item> FLASK_INFUSION = ITEMS.register("flask_infusion",
            () -> new com.hbm_m.item.food.SpecialFoodItems.FlaskItem());
    /** Metadaten 1 und 2 des Originals ({@code ItemAppleSchrabidium}): eigene IDs im Port. */
    public static final RegistrySupplier<Item> APPLE_SCHRABIDIUM_1 = ITEMS.register("apple_schrabidium_1",
            () -> com.hbm_m.item.food.HbmFoodItem.of(20, 100, false).alwaysEdible().noDesc().rarity(net.minecraft.world.item.Rarity.RARE)
                    .onEaten((st, w, pl) -> com.hbm_m.item.food.FoodBehaviors.appleSchrabidium(1, st, w, pl)).build());
    public static final RegistrySupplier<Item> APPLE_SCHRABIDIUM_2 = ITEMS.register("apple_schrabidium_2",
            () -> com.hbm_m.item.food.HbmFoodItem.of(20, 100, false).alwaysEdible().noDesc().foil().rarity(net.minecraft.world.item.Rarity.EPIC)
                    .onEaten((st, w, pl) -> com.hbm_m.item.food.FoodBehaviors.appleSchrabidium(2, st, w, pl)).build());
    public static final RegistrySupplier<Item> APPLE_LEAD_1 = ITEMS.register("apple_lead_1",
            () -> com.hbm_m.item.food.HbmFoodItem.of(5, 0, false).alwaysEdible().noDesc().rarity(net.minecraft.world.item.Rarity.RARE)
                    .onEaten((st, w, pl) -> com.hbm_m.item.food.FoodBehaviors.appleLead(1, st, w, pl)).build());
    public static final RegistrySupplier<Item> APPLE_LEAD_2 = ITEMS.register("apple_lead_2",
            () -> com.hbm_m.item.food.HbmFoodItem.of(5, 0, false).alwaysEdible().noDesc().foil().rarity(net.minecraft.world.item.Rarity.EPIC)
                    .onEaten((st, w, pl) -> com.hbm_m.item.food.FoodBehaviors.appleLead(2, st, w, pl)).build());
    /** {@code ItemTemFlakes} Metadaten 1 und 2 (gleiche Textur, eigener Tooltip). */
    public static final RegistrySupplier<Item> TEM_FLAKES_1 = ITEMS.register("tem_flakes_1",
            () -> com.hbm_m.item.food.HbmFoodItem.of(0, 0, false).alwaysEdible().onEaten(com.hbm_m.item.food.FoodBehaviors::temFlakes).build());
    public static final RegistrySupplier<Item> TEM_FLAKES_2 = ITEMS.register("tem_flakes_2",
            () -> com.hbm_m.item.food.HbmFoodItem.of(0, 0, false).alwaysEdible().onEaten(com.hbm_m.item.food.FoodBehaviors::temFlakes).build());
    /** {@code ItemMarshmallow} Metadatum 1: geroestet (Zutat fuer den S'more). */
    public static final RegistrySupplier<Item> MARSHMALLOW_ROASTED = ITEMS.register("marshmallow_roasted",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static void init() {
        ITEMS.register();
    }
}
