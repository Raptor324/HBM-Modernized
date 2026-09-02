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
import com.hbm_m.hazard.modifier.HazardModifierRBMKHot;
import com.hbm_m.hazard.modifier.HazardModifierRBMKRadiation;
import com.hbm_m.item.tags_and_tiers.ModIngots;

import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.List;
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

    /** GIT {@code HazardRegistry.xe135} — Xenon-135 radiation contribution used by {@link com.hbm_m.hazard.modifier.HazardModifierRBMKRadiation}. */
    public static final float xe135 = 1250.0F;

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
                new HazardEntry(RADIATION, 60f)));
        HazardSystem.register(ModBlocks.BLOCK_FALLOUT.get(), new HazardData(
                new HazardEntry(RADIATION, 10.5f)));
        HazardSystem.register(ModItems.FALLOUT.get(), new HazardData(
                new HazardEntry(RADIATION, 30f)));
        HazardSystem.register(ModBlocks.POLONIUM210_BLOCK.get(), new HazardData(
                new HazardEntry(RADIATION, 750f),
                new HazardEntry(HOT, 3f)));

        HazardSystem.register(ModBlocks.ORE_SELLAFIELD_RADGEM.get(), new HazardData(
                new HazardEntry(RADIATION, 25f)));
        HazardSystem.register(ModBlocks.WASTE_TRINITITE.get(), new HazardData(
                new HazardEntry(RADIATION, 0.5f)));
        HazardSystem.register(ModBlocks.WASTE_TRINITITE_RED.get(), new HazardData(
                new HazardEntry(RADIATION, 0.5f)));
        HazardSystem.register(ModBlocks.WASTE_MYCELIUM.get(), new HazardData(
                new HazardEntry(RADIATION, 0.25f)));

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

        registerRbmkHazards();

        HazardSystem.register(URANIUM_INGOTS, new HazardData(
                new HazardEntry(RADIATION, 0.35f)));
        HazardSystem.register(ALKALI_METALS, new HazardData(
                new HazardEntry(HYDROACTIVE, 2.0f)));

        // ── Угольная пыль (GIT: dust → COAL at powder=3.0F, tiny=0.3F) ──────────
        HazardSystem.register(ModItems.getPowders(com.hbm_m.item.tags_and_tiers.ModPowders.COAL).get(), new HazardData(
                new HazardEntry(COAL, 3.0f)));
        HazardSystem.register(ModItems.COAL_POWDER_TINY.get(), new HazardData(
                new HazardEntry(COAL, 0.3f)));
        HazardSystem.register(ModItems.LIGNITE_POWDER.get(), new HazardData(
                new HazardEntry(COAL, 3.0f)));

        // ── Асбест (GIT: brick_asbestos 1F, tile_lab_broken 1F, powder_coltan_ore 3F) ──
        HazardSystem.register(ModBlocks.BRICK_ASBESTOS.get(), new HazardData(
                new HazardEntry(ASBESTOS, 1.0f)));
        HazardSystem.register(ModBlocks.TILE_LAB_BROKEN.get(), new HazardData(
                new HazardEntry(ASBESTOS, 1.0f)));
        HazardSystem.register(ModItems.POWDER_COLTAN.get(), new HazardData(
                new HazardEntry(ASBESTOS, 3.0f)));
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
    /**
     * RBMK rod and pellet hazards, 1:1 with the original's
     * {@code com.hbm.hazard.HazardRegistry:377-442} (registerRBMKRod / registerRBMKPellet).
     * Every rod additionally carries the HOT entry; the radiation entry always gets the
     * depletion-scaling modifier, whose second argument is the fully-depleted target value.
     */
    private static void registerRbmkHazards() {
        // HazardRegistry constants (:74-149)
        final float nugget = 0.1f, billet = 0.5f, rod = 0.5f, rod_rbmk = rod * 8;
        final float wst = 15.0f, bf = 300_000.0f;
        final float au198 = 500.0f, pb209 = 10_000.0f, po210 = 75.0f, ra226 = 7.5f;
        final float thf = 1.75f, u = 0.35f, u233 = 5.0f, u235 = 1.0f, uf = 0.5f, uzh = 0.125f;
        final float np237 = 2.5f, npf = 1.5f, purg = 6.25f, pu238 = 10.0f, pu239 = 5.0f;
        final float pu241 = 25.0f, puf = 4.25f, am241 = 8.5f, am242 = 9.5f, amrg = 9.0f, amf = 4.75f;
        final float mox = 2.5f, saf = 5.85f;
        final float radsource_mult = 3.0f;
        final float pobe = po210 * radsource_mult, rabe = ra226 * radsource_mult, pube = pu238 * radsource_mult;

        // :377-407 - rods
        rbmkRodHazard(ModItems.RBMK_FUEL_UEU,     u     * rod_rbmk, wst * rod_rbmk * 20f);
        rbmkRodHazard(ModItems.RBMK_FUEL_MEU,     uf    * rod_rbmk, wst * rod_rbmk * 21.5f);
        rbmkRodHazard(ModItems.RBMK_FUEL_HEU233,  u233  * rod_rbmk, wst * rod_rbmk * 31f);
        rbmkRodHazard(ModItems.RBMK_FUEL_HEU235,  u235  * rod_rbmk, wst * rod_rbmk * 30f);
        rbmkRodHazard(ModItems.RBMK_FUEL_UZH,     uzh   * rod_rbmk, wst * rod_rbmk * 20f);
        rbmkRodHazard(ModItems.RBMK_FUEL_THMEU,   thf   * rod_rbmk, wst * rod_rbmk * 17.5f);
        rbmkRodHazard(ModItems.RBMK_FUEL_LEP,     puf   * rod_rbmk, wst * rod_rbmk * 25f);
        rbmkRodHazard(ModItems.RBMK_FUEL_MEP,     purg  * rod_rbmk, wst * rod_rbmk * 30f);
        rbmkRodHazard(ModItems.RBMK_FUEL_HEP,     pu239 * rod_rbmk, wst * rod_rbmk * 32.5f);
        rbmkRodHazard(ModItems.RBMK_FUEL_HEP241,  pu241 * rod_rbmk, wst * rod_rbmk * 35f);
        rbmkRodHazard(ModItems.RBMK_FUEL_LEA,     amf   * rod_rbmk, wst * rod_rbmk * 26f);
        rbmkRodHazard(ModItems.RBMK_FUEL_MEA,     amrg  * rod_rbmk, wst * rod_rbmk * 30.5f);
        rbmkRodHazard(ModItems.RBMK_FUEL_HEA241,  am241 * rod_rbmk, wst * rod_rbmk * 33.5f);
        rbmkRodHazard(ModItems.RBMK_FUEL_HEA242,  am242 * rod_rbmk, wst * rod_rbmk * 34f);
        rbmkRodHazard(ModItems.RBMK_FUEL_MEN,     npf   * rod_rbmk, wst * rod_rbmk * 22.5f);
        rbmkRodHazard(ModItems.RBMK_FUEL_HEN,     np237 * rod_rbmk, wst * rod_rbmk * 30f);
        rbmkRodHazard(ModItems.RBMK_FUEL_MOX,     mox   * rod_rbmk, wst * rod_rbmk * 25.5f);
        rbmkRodHazard(ModItems.RBMK_FUEL_LES,     saf   * rod_rbmk, wst * rod_rbmk * 24.5f);
        rbmkRodHazard(ModItems.RBMK_FUEL_MES,     saf   * rod_rbmk, wst * rod_rbmk * 30f);
        rbmkRodHazard(ModItems.RBMK_FUEL_HES,     saf   * rod_rbmk, wst * rod_rbmk * 50f);
        rbmkRodHazard(ModItems.RBMK_FUEL_LEAUS,   0f,               wst * rod_rbmk * 37.5f);
        rbmkRodHazard(ModItems.RBMK_FUEL_HEAUS,   0f,               wst * rod_rbmk * 32.5f);
        rbmkRodHazard(ModItems.RBMK_FUEL_PO210BE, pobe  * rod_rbmk, pobe * rod_rbmk * 0.1f, true, 0f, 0f);
        rbmkRodHazard(ModItems.RBMK_FUEL_RA226BE, rabe  * rod_rbmk, rabe * rod_rbmk * 0.4f, true, 0f, 0f);
        rbmkRodHazard(ModItems.RBMK_FUEL_PU238BE, pube  * rod_rbmk, wst * rod_rbmk * 2.5f);
        rbmkRodHazard(ModItems.RBMK_FUEL_BALEFIRE_GOLD, au198 * rod_rbmk, bf * rod_rbmk * 0.5f, true, 0f, 0f);
        rbmkRodHazard(ModItems.RBMK_FUEL_FLASHLEAD, pb209 * 1.25f * rod_rbmk, pb209 * nugget * 0.05f * rod_rbmk, true, 0f, 0f);
        rbmkRodHazard(ModItems.RBMK_FUEL_BALEFIRE, bf * rod_rbmk, bf * rod_rbmk * 100f, true, 0f, 0f);
        rbmkRodHazard(ModItems.RBMK_FUEL_ZFB_BISMUTH, pu241 * rod_rbmk * 0.1f, wst * rod_rbmk * 5f);
        rbmkRodHazard(ModItems.RBMK_FUEL_ZFB_PU241,   pu239 * rod_rbmk * 0.1f, wst * rod_rbmk * 7.5f);
        rbmkRodHazard(ModItems.RBMK_FUEL_ZFB_AM_MIX,  pu241 * rod_rbmk * 0.1f, wst * rod_rbmk * 10f);
        // :408 - registerRBMK(..., hot, linear, blinding=0, digamma=1/3)
        rbmkRodHazard(ModItems.RBMK_FUEL_DRX, bf * rod_rbmk, bf * rod_rbmk * 100f, true, 0f, 1f / 3f);

        // :411-442 - pellets (no HOT entry)
        rbmkPelletHazard(ModItems.RBMK_PELLET_UEU,     u     * billet, wst * billet * 20f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_MEU,     uf    * billet, wst * billet * 21.5f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_HEU233,  u233  * billet, wst * billet * 31f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_HEU235,  u235  * billet, wst * billet * 30f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_UZH,     uzh   * billet, wst * billet * 20f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_THMEU,   thf   * billet, wst * billet * 17.5f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_LEP,     puf   * billet, wst * billet * 25f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_MEP,     purg  * billet, wst * billet * 30f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_HEP,     pu239 * billet, wst * billet * 32.5f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_HEP241,  pu241 * billet, wst * billet * 35f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_LEA,     amf   * billet, wst * billet * 26f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_MEA,     amrg  * billet, wst * billet * 30.5f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_HEA241,  am241 * billet, wst * billet * 33.5f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_HEA242,  am242 * billet, wst * billet * 34f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_MEN,     npf   * billet, wst * billet * 22.5f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_HEN,     np237 * billet, wst * billet * 30f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_MOX,     mox   * billet, wst * billet * 25.5f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_LES,     saf   * billet, wst * billet * 24.5f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_MES,     saf   * billet, wst * billet * 30f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_HES,     saf   * billet, wst * billet * 50f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_LEAUS,   0f,             wst * billet * 37.5f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_HEAUS,   0f,             wst * billet * 32.5f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_PO210BE, pobe  * billet, pobe * billet * 0.1f, true, 0f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_RA226BE, rabe  * billet, rabe * billet * 0.4f, true, 0f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_PU238BE, pube  * billet, wst * 1.5f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_BALEFIRE_GOLD, au198 * billet, bf * billet * 0.5f, true, 0f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_FLASHLEAD, pb209 * 1.25f * billet, pb209 * nugget * 0.05f, true, 0f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_BALEFIRE,  bf * billet, bf * billet * 100f, true, 0f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_ZFB_BISMUTH, pu241 * billet * 0.1f, wst * billet * 5f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_ZFB_PU241,   pu239 * billet * 0.1f, wst * billet * 7.5f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_ZFB_AM_MIX,  pu241 * billet * 0.1f, wst * billet * 10f);
        rbmkPelletHazard(ModItems.RBMK_PELLET_DRX, bf * billet, bf * billet * 100f, true, 1f / 24f);
    }

    /** HazardRegistry.registerRBMKRod(rod, base, dep) - non-linear depletion scaling. */
    private static void rbmkRodHazard(RegistrySupplier<Item> rod, float base, float dep) {
        rbmkRodHazard(rod, base, dep, false, 0f, 0f);
    }

    /** HazardRegistry.registerRBMK(rod, base, dep, hot=true, linear, blinding, digamma). */
    private static void rbmkRodHazard(RegistrySupplier<Item> rod, float base, float dep,
            boolean linear, float blinding, float digamma) {
        List<HazardEntry> entries = new ArrayList<>();
        entries.add(new HazardEntry(RADIATION, base).addMod(new HazardModifierRBMKRadiation(dep, linear)));
        entries.add(new HazardEntry(HOT, 0).addMod(new HazardModifierRBMKHot()));
        if (blinding > 0) entries.add(new HazardEntry(BLINDING, blinding));
        if (digamma > 0) entries.add(new HazardEntry(DIGAMMA, digamma));
        HazardSystem.register(rod.get(), new HazardData(entries.toArray(new HazardEntry[0])));
    }

    /** HazardRegistry.registerRBMKPellet(pellet, base, dep) - no HOT entry. */
    private static void rbmkPelletHazard(RegistrySupplier<Item> pellet, float base, float dep) {
        rbmkPelletHazard(pellet, base, dep, false, 0f);
    }

    private static void rbmkPelletHazard(RegistrySupplier<Item> pellet, float base, float dep,
            boolean linear, float digamma) {
        List<HazardEntry> entries = new ArrayList<>();
        entries.add(new HazardEntry(RADIATION, base).addMod(new HazardModifierRBMKRadiation(dep, linear)));
        if (digamma > 0) entries.add(new HazardEntry(DIGAMMA, digamma));
        HazardSystem.register(pellet.get(), new HazardData(entries.toArray(new HazardEntry[0])));
    }

}
