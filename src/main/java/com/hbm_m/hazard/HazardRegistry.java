package com.hbm_m.hazard;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.hazard.type.HazardTypeAsbestos;
import com.hbm_m.hazard.type.HazardTypeBase;
import com.hbm_m.hazard.type.HazardTypeBlinding;
import com.hbm_m.hazard.type.HazardTypeCoal;
import com.hbm_m.hazard.type.HazardTypeDigamma;
import com.hbm_m.hazard.type.HazardTypeExplosive;
import com.hbm_m.hazard.type.HazardTypeHot;
import com.hbm_m.hazard.type.HazardTypeHydroactive;
import com.hbm_m.hazard.type.HazardTypeRadiation;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Регистрация опасностей предметов и блоков. Порт {@link com.hbm.hazard.HazardRegistry} (1.7.10).
 */
public class HazardRegistry {

    /** GIT {@link com.hbm.inventory.OreDictManager} blinding(50F) для шрабидиевых материалов. */
    private static final float BLINDING_SCHRAB = 50.0F;

    public static final HazardTypeBase RADIATION = new HazardTypeRadiation();
    public static final HazardTypeBase HOT = new HazardTypeHot();
    public static final HazardTypeBase DIGAMMA = new HazardTypeDigamma();
    public static final HazardTypeBase BLINDING = new HazardTypeBlinding();
    public static final HazardTypeBase ASBESTOS = new HazardTypeAsbestos();
    public static final HazardTypeBase COAL = new HazardTypeCoal();
    public static final HazardTypeBase HYDROACTIVE = new HazardTypeHydroactive();
    public static final HazardTypeBase EXPLOSIVE = new HazardTypeExplosive();

    public static final TagKey<Item> URANIUM_INGOTS = TagKey.create(Registries.ITEM,
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation("forge", "ingots/uranium"));
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath("forge", "ingots/uranium"));
            //?}

    public static final TagKey<Item> ALKALI_METALS = TagKey.create(Registries.ITEM,
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation("forge", "ingots/sodium"));
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath("forge", "ingots/sodium"));
            //?}

    public static void registerItems() {
        for (ModIngots ingot : ModIngots.values()) {
            switch (ingot) {
                case PU_MIX -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 6.25f)));
                case AM_MIX -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 9f)));
                case MUD -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 1f)));
                case AMERICIUM_FUEL -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 4.75f)));
                case NEPTUNIUM_FUEL -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 1.5f)));
                case PLUTONIUM_FUEL -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 4.25f)));
                case THORIUM_FUEL -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 1.75f)));
                case URANIUM_FUEL -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 0.5f)));
                case SCHRABIDIUM -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 15.0f),
                        new HazardEntry(BLINDING, BLINDING_SCHRAB)));
                case SCHRABIDIUM_FUEL -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 5.85f),
                        new HazardEntry(BLINDING, 5.0f)));
                case MOX_FUEL -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 2.5f)));
                case SR90 -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 15f),
                        new HazardEntry(HOT, 2.0f)));
                case CO60 -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 30f),
                        new HazardEntry(HOT, 2.0f)));
                case THORIUM -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 0.1f)));
                case RA226 -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 7.5f)));
                case PB209 -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 10000f),
                        new HazardEntry(HOT, 5.0f)));
                case AU198 -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 500f),
                        new HazardEntry(HOT, 3.0f)));
                case SCHRARANIUM -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 15f),
                        new HazardEntry(BLINDING, BLINDING_SCHRAB)));
                case SCHRABIDATE -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 1.5f),
                        new HazardEntry(BLINDING, BLINDING_SCHRAB)));
                case TECHNETIUM -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 2.75f)));
                case POLONIUM -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 75f),
                        new HazardEntry(HOT, 3.0f)));
                case NEPTUNIUM -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 2.5f)));
                case AM242 -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 9.5f)));
                case AM241 -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 8.5f)));
                case STRONTIUM -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 15.0f)));
                case CERIUM -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(HOT, 3.0f)));
                case SOLINIUM -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 17.5f),
                        new HazardEntry(BLINDING, BLINDING_SCHRAB)));
                case PHOSPHORUS -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(HOT, 1.0f)));
                case DIGAMMA -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(DIGAMMA, 1.0f)));
                case URANIUM233 -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 5.0f)));
                case URANIUM235 -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 1.0f)));
                case URANIUM238 -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 0.25f)));
                case THORIUM232 -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 1.0f)));
                case PLUTONIUM -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 7.5f)));
                case PLUTONIUM238 -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 10.0f),
                        new HazardEntry(HOT, 2.0f)));
                case PLUTONIUM239 -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 5.0f)));
                case PLUTONIUM240 -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 7.5f)));
                case PLUTONIUM241 -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 25.0f)));
                case ACTINIUM -> HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 30.0f)));
                default -> {
                }
            }
        }

        registerIngotStorageBlocks();

        HazardSystem.register(ModBlocks.NUCLEAR_FALLOUT.get(), new HazardData(
                new HazardEntry(RADIATION, 0.5f)));
        HazardSystem.register(ModBlocks.POLONIUM210_BLOCK.get(), new HazardData(
                new HazardEntry(RADIATION, 750f),
                new HazardEntry(HOT, 3f)));

        HazardSystem.register(ModBlocks.SELLAFIELD_SLAKED.get(), new HazardData(
                new HazardEntry(RADIATION, 0.1f)));
        HazardSystem.register(ModBlocks.SELLAFIELD_SLAKED1.get(), new HazardData(
                new HazardEntry(RADIATION, 0.1f)));
        HazardSystem.register(ModBlocks.SELLAFIELD_SLAKED2.get(), new HazardData(
                new HazardEntry(RADIATION, 0.1f)));
        HazardSystem.register(ModBlocks.SELLAFIELD_SLAKED3.get(), new HazardData(
                new HazardEntry(RADIATION, 0.1f)));

        HazardSystem.register(ModBlocks.BARREL_YELLOW.get(), new HazardData(
                new HazardEntry(RADIATION, 150f)));
        HazardSystem.register(ModBlocks.BARREL_VITRIFIED.get(), new HazardData(
                new HazardEntry(RADIATION, 15f)));
        HazardSystem.register(ModBlocks.BARREL_TAINT.get(), new HazardData(
                new HazardEntry(RADIATION, 67f)));

        HazardSystem.register(ModBlocks.URANIUM_ORE.get(), new HazardData(
                new HazardEntry(RADIATION, 0.35f)));
        HazardSystem.register(ModBlocks.URANIUM_ORE_DEEPSLATE.get(), new HazardData(
                new HazardEntry(RADIATION, 0.35f)));
        HazardSystem.register(ModBlocks.THORIUM_ORE.get(), new HazardData(
                new HazardEntry(RADIATION, 0.1f)));

        HazardSystem.register(ModBlocks.SCHRABIDIUM_ORE.get(), new HazardData(
                new HazardEntry(RADIATION, 0.5f)));
        HazardSystem.register(ModBlocks.SCHRABIDIUM_ORE_NETHER.get(), new HazardData(
                new HazardEntry(RADIATION, 0.5f)));
        HazardSystem.register(ModBlocks.SCHRABIDIUM_ORE_GNEISS.get(), new HazardData(
                new HazardEntry(RADIATION, 0.5f)));
        HazardSystem.register(ModBlocks.BLOCK_SCHRABIDIUM_CLUSTER.get(), new HazardData(
                new HazardEntry(RADIATION, 15.0f * 10.0f),
                new HazardEntry(BLINDING, BLINDING_SCHRAB)));

        HazardSystem.register(Items.GUNPOWDER, new HazardData(
                new HazardEntry(EXPLOSIVE, 1.0f)));

        HazardSystem.register(ModItems.WIRE_SCHRABIDIUM.get(), new HazardData(
                new HazardEntry(RADIATION, 0.5f)));
        HazardSystem.register(ModItems.FAT_MAN_CORE.get(), new HazardData(
                new HazardEntry(RADIATION, 5f)));
        HazardSystem.register(ModItems.BILLET_PLUTONIUM.get(), new HazardData(
                new HazardEntry(RADIATION, 3.75f)));

        // nuclear waste (GIT HazardRegistry; full-size items not ported yet)
        HazardSystem.register(ModItems.NUCLEAR_WASTE_LONG_TINY.get(), new HazardData(
                new HazardEntry(RADIATION, 0.5f)));
        HazardSystem.register(ModItems.NUCLEAR_WASTE_SHORT_TINY.get(), new HazardData(
                new HazardEntry(RADIATION, 3f),
                new HazardEntry(HOT, 5f)));
        HazardSystem.register(ModItems.NUCLEAR_WASTE_LONG_DEPLETED_TINY.get(), new HazardData(
                new HazardEntry(RADIATION, 0.05f)));
        HazardSystem.register(ModItems.NUCLEAR_WASTE_SHORT_DEPLETED_TINY.get(), new HazardData(
                new HazardEntry(RADIATION, 0.3f)));
        HazardSystem.register(ModItems.NUCLEAR_WASTE_VITRIFIED_TINY.get(), new HazardData(
                new HazardEntry(RADIATION, 0.75f)));
        HazardSystem.register(ModItems.NUCLEAR_WASTE_TINY.get(), new HazardData(
                new HazardEntry(RADIATION, 1.5f)));

        // plate fuel — base radiation only (GIT registerOtherFuel, no FuelRadiation modifier yet)
        HazardSystem.register(ModItems.PLATE_FUEL_U233.get(), new HazardData(
                new HazardEntry(RADIATION, 5f)));
        HazardSystem.register(ModItems.PLATE_FUEL_U235.get(), new HazardData(
                new HazardEntry(RADIATION, 1f)));
        HazardSystem.register(ModItems.PLATE_FUEL_MOX.get(), new HazardData(
                new HazardEntry(RADIATION, 2.5f)));
        HazardSystem.register(ModItems.PLATE_FUEL_PU239.get(), new HazardData(
                new HazardEntry(RADIATION, 5f)));
        HazardSystem.register(ModItems.PLATE_FUEL_SA326.get(), new HazardData(
                new HazardEntry(RADIATION, 15f)));
        HazardSystem.register(ModItems.PLATE_FUEL_RA226BE.get(), new HazardData(
                new HazardEntry(RADIATION, 11.25f)));
        HazardSystem.register(ModItems.PLATE_FUEL_PU238BE.get(), new HazardData(
                new HazardEntry(RADIATION, 15f)));

        // crystals (GIT HazardRegistry)
        HazardSystem.register(ModItems.CRYSTAL_URANIUM.get(), new HazardData(
                new HazardEntry(RADIATION, 3.5f)));
        HazardSystem.register(ModItems.CRYSTAL_THORIUM.get(), new HazardData(
                new HazardEntry(RADIATION, 1f)));
        HazardSystem.register(ModItems.CRYSTAL_PLUTONIUM.get(), new HazardData(
                new HazardEntry(RADIATION, 75f)));
        HazardSystem.register(ModItems.CRYSTAL_SCHRARANIUM.get(), new HazardData(
                new HazardEntry(RADIATION, 15f),
                new HazardEntry(BLINDING, BLINDING_SCHRAB)));
        HazardSystem.register(ModItems.CRYSTAL_SCHRABIDIUM.get(), new HazardData(
                new HazardEntry(RADIATION, 150f),
                new HazardEntry(BLINDING, BLINDING_SCHRAB)));
        HazardSystem.register(ModItems.CRYSTAL_TRIXITE.get(), new HazardData(
                new HazardEntry(RADIATION, 250f)));
        HazardSystem.register(ModItems.PLATE_SCHRABIDIUM.get(), new HazardData(
                new HazardEntry(RADIATION, 15.0f),
                new HazardEntry(BLINDING, BLINDING_SCHRAB)));

        HazardSystem.register(Blocks.TNT.asItem(), new HazardData(
                new HazardEntry(EXPLOSIVE, 4.0f)));
        HazardSystem.register(Items.TNT_MINECART, new HazardData(
                new HazardEntry(EXPLOSIVE, 4.0f)));

        HazardSystem.register(ModItems.RBMK_FUEL_DRX.get(), new HazardData(
                new HazardEntry(RADIATION, 1200000f)));

        HazardSystem.register(URANIUM_INGOTS, new HazardData(
                new HazardEntry(RADIATION, 0.35f)));
        HazardSystem.register(ALKALI_METALS, new HazardData(
                new HazardEntry(HYDROACTIVE, 2.0f)));
    }

    /** GIT {@link com.hbm.hazard.HazardRegistry#block} (=10) × базовая rad слитка ({@link com.hbm.inventory.OreDictManager} DictFrame). */
    private static void registerIngotStorageBlocks() {
        final float block = 10.0f;

        for (ModIngots ingot : ModIngots.values()) {
            if (!ModBlocks.hasIngotBlock(ingot)) {
                continue;
            }

            switch (ingot) {
                case URANIUM -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 0.35f * block)));
                case URANIUM233 -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 5.0f * block)));
                case URANIUM235 -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 1.0f * block)));
                case URANIUM238 -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 0.25f * block)));
                case THORIUM -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 0.1f * block)));
                case PLUTONIUM -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 7.5f * block)));
                case PLUTONIUM238 -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 10.0f * block),
                        new HazardEntry(HOT, 3.0f)));
                case PLUTONIUM239 -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 5.0f * block)));
                case PLUTONIUM240 -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 7.5f * block)));
                case PLUTONIUM241 -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 25.0f * block)));
                case NEPTUNIUM -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 2.5f * block)));
                case RA226 -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 7.5f * block)));
                case ACTINIUM -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 30.0f * block)));
                case MOX_FUEL -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 2.5f * block)));
                case URANIUM_FUEL -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 0.5f * block)));
                case THORIUM_FUEL -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 1.75f * block)));
                case PLUTONIUM_FUEL -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 4.25f * block)));
                case SCHRABIDIUM -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 15.0f * block),
                        new HazardEntry(BLINDING, BLINDING_SCHRAB)));
                case SCHRARANIUM -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 1.5f * block),
                        new HazardEntry(BLINDING, BLINDING_SCHRAB)));
                case SCHRABIDATE -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 1.5f * block),
                        new HazardEntry(BLINDING, BLINDING_SCHRAB)));
                case SOLINIUM -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 17.5f * block),
                        new HazardEntry(BLINDING, BLINDING_SCHRAB)));
                case SCHRABIDIUM_FUEL -> HazardSystem.register(ModBlocks.getIngotBlock(ingot).get(), new HazardData(
                        new HazardEntry(RADIATION, 5.85f * block),
                        new HazardEntry(BLINDING, 5.0f * block)));
                default -> {
                }
            }
        }
    }
}
