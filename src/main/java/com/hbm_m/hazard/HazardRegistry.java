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
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

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
        for (ModMaterials ingot : ModMaterials.values()) {
            switch (ingot) {
                case PU_MIX -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 6.25f)));
                case AM_MIX -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 9f)));
                case MUD -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 1f)));
                case AMERICIUM_FUEL -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 4.75f)));
                case NEPTUNIUM_FUEL -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 1.5f)));
                case PLUTONIUM_FUEL -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 4.25f)));
                case THORIUM_FUEL -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 1.75f)));
                case URANIUM_FUEL -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 0.5f)));
                case SCHRABIDIUM -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 15.0f),
                        new HazardEntry(BLINDING, BLINDING_SCHRAB)));
                case SCHRABIDIUM_FUEL -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 5.85f),
                        new HazardEntry(BLINDING, 5.0f)));
                case MOX_FUEL -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 2.5f)));
                case SR90 -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 15f),
                        new HazardEntry(HOT, 1.0f),
                        new HazardEntry(HYDROACTIVE, 1.0f)));
                case CO60 -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 30f),
                        new HazardEntry(HOT, 1.0f)));
                case THORIUM -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 0.1f)));
                case RA226 -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 7.5f)));
                case PB209 -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 10000f),
                        new HazardEntry(BLINDING, 50.0f),
                        new HazardEntry(HOT, 7.0f)));
                case AU198 -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 500f),
                        new HazardEntry(HOT, 5.0f)));
                case SCHRARANIUM -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        // GIT OreDictManager SRN: .rad(HazardRegistry.sr) — sr = sa326 * 0.1F = 1.5F
                        new HazardEntry(RADIATION, 1.5f),
                        new HazardEntry(BLINDING, BLINDING_SCHRAB)));
                case SCHRABIDATE -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 1.5f),
                        new HazardEntry(BLINDING, BLINDING_SCHRAB)));
                case TECHNETIUM -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 2.75f)));
                case POLONIUM -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 75f),
                        new HazardEntry(HOT, 3.0f)));
                case NEPTUNIUM -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 2.5f)));
                case AM242 -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 9.5f)));
                case AM241 -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 8.5f)));
                case STRONTIUM -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        // GIT OreDictManager SR: .hot(1F).hydro(1F) — без радиации
                        new HazardEntry(HOT, 1.0f),
                        new HazardEntry(HYDROACTIVE, 1.0f)));
                case CERIUM -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(HOT, 3.0f)));
                case SOLINIUM -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 17.5f),
                        new HazardEntry(BLINDING, BLINDING_SCHRAB)));
                case PHOSPHORUS -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(HOT, 1.0f)));
                case DIGAMMA -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(DIGAMMA, 1.0f)));
                case URANIUM233 -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 5.0f)));
                case URANIUM235 -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 1.0f)));
                case URANIUM238 -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 0.25f)));
                case THORIUM232 -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 1.0f)));
                case PLUTONIUM -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 7.5f)));
                case PLUTONIUM238 -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 10.0f),
                        new HazardEntry(HOT, 3.0f)));
                case PLUTONIUM239 -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 5.0f)));
                case PLUTONIUM240 -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 7.5f)));
                case PLUTONIUM241 -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
                        new HazardEntry(RADIATION, 25.0f)));
                case ACTINIUM -> HazardSystem.register(ModMaterialItems.item(ingot, MaterialShape.INGOT), new HazardData(
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

        HazardSystem.register(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.WIRE), new HazardData(
                new HazardEntry(RADIATION, 0.5f)));
        HazardSystem.register(ModItems.FAT_MAN_CORE.get(), new HazardData(
                new HazardEntry(RADIATION, 5f)));
        HazardSystem.register(ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.BILLET), new HazardData(
                new HazardEntry(RADIATION, 3.75f)));

        // scrap_nuclear (GIT HazardRegistry: RADIATION 1F)
        HazardSystem.register(ModMaterialItems.item(ModMaterials.SCRAP_NUCLEAR, MaterialShape.SCRAP), new HazardData(
                new HazardEntry(RADIATION, 1f)));

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

        // ── Развёрнутые мета-предметы отходов (GIT HazardRegistry 1:1) ─────────────────
        // Полноразмерные nuclear_waste (wst = 15F, ingot = 1F, nugget = 0.1F):
        HazardSystem.register(ModItems.NUCLEAR_WASTE.get(), new HazardData(
                new HazardEntry(RADIATION, 15f * 1f)));
        HazardSystem.register(ModItems.NUCLEAR_WASTE_VITRIFIED.get(), new HazardData(
                new HazardEntry(RADIATION, 7.5f * 1f)));
        HazardSystem.register(ModItems.NUCLEAR_WASTE_LONG.get(), new HazardData(
                new HazardEntry(RADIATION, 5f)));
        HazardSystem.register(ModItems.NUCLEAR_WASTE_SHORT.get(), new HazardData(
                new HazardEntry(RADIATION, 30f),
                new HazardEntry(HOT, 5f)));
        HazardSystem.register(ModItems.NUCLEAR_WASTE_LONG_DEPLETED.get(), new HazardData(
                new HazardEntry(RADIATION, 0.5f)));
        HazardSystem.register(ModItems.NUCLEAR_WASTE_SHORT_DEPLETED.get(), new HazardData(
                new HazardEntry(RADIATION, 3f)));

        // nuclear_waste_long/short per-isotope (GIT: хазард общий на предмет, не на мету):
        // long 5F; long_tiny 0.5F; short 30F+HOT5; short_tiny 3F+HOT5;
        // long_depleted 0.5F; long_depleted_tiny 0.05F; short_depleted 3F; short_depleted_tiny 0.3F.
        registerWasteGroup("nw_long", 5f, false);
        registerWasteGroup("nw_long_tiny", 0.5f, false);
        registerWasteGroup("nw_short", 30f, true);
        registerWasteGroup("nw_short_tiny", 3f, true);
        registerWasteGroup("nw_long_dep", 0.5f, false);
        registerWasteGroup("nw_long_dep_tiny", 0.05f, false);
        registerWasteGroup("nw_short_dep", 3f, false);
        registerWasteGroup("nw_short_dep_tiny", 0.3f, false);

        // waste_* (ItemDepletedFuel, GIT registerOtherWaste): base = wst * billet * mult;
        // мета0 (свежее, уже существующий предмет) = base * 0.075;
        // мета1 (охлаждающееся, PartTabMetaItems) = base + HOT 5.
        registerOtherWastePair(ModItems.WASTE_NATURAL_URANIUM.get(), "waste_natural_uranium_cooling", 11.5f);
        registerOtherWastePair(ModItems.WASTE_URANIUM.get(), "waste_uranium_cooling", 10f);
        registerOtherWastePair(ModItems.WASTE_THORIUM.get(), "waste_thorium_cooling", 7.5f);
        registerOtherWastePair(ModItems.WASTE_MOX.get(), "waste_mox_cooling", 10f);
        registerOtherWastePair(ModItems.WASTE_PLUTONIUM.get(), "waste_plutonium_cooling", 12.5f);
        registerOtherWastePair(ModItems.WASTE_U233.get(), "waste_u233_cooling", 10f);
        registerOtherWastePair(ModItems.WASTE_U235.get(), "waste_u235_cooling", 11f);
        registerOtherWastePair(ModItems.WASTE_SCHRABIDIUM.get(), "waste_schrabidium_cooling", 15f);
        registerOtherWastePair(ModItems.WASTE_ZFB_MOX.get(), "waste_zfb_mox_cooling", 5f);

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
        HazardSystem.register(ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.CRYSTAL), new HazardData(
                new HazardEntry(RADIATION, 3.5f)));
        HazardSystem.register(ModMaterialItems.item(ModMaterials.THORIUM, MaterialShape.CRYSTAL), new HazardData(
                new HazardEntry(RADIATION, 1f)));
        HazardSystem.register(ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.CRYSTAL), new HazardData(
                new HazardEntry(RADIATION, 75f)));
        HazardSystem.register(ModMaterialItems.item(ModMaterials.SCHRARANIUM, MaterialShape.CRYSTAL), new HazardData(
                new HazardEntry(RADIATION, 15f),
                new HazardEntry(BLINDING, BLINDING_SCHRAB)));
        HazardSystem.register(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.CRYSTAL), new HazardData(
                new HazardEntry(RADIATION, 150f),
                new HazardEntry(BLINDING, BLINDING_SCHRAB)));
        HazardSystem.register(ModMaterialItems.item(ModMaterials.TRIXITE, MaterialShape.CRYSTAL), new HazardData(
                new HazardEntry(RADIATION, 250f)));
        HazardSystem.register(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.PLATE), new HazardData(
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
        HazardSystem.register(ModMaterialItems.item(ModMaterials.COAL, MaterialShape.POWDER), new HazardData(
                new HazardEntry(COAL, 3.0f)));
        HazardSystem.register(ModMaterialItems.item(ModMaterials.COAL, MaterialShape.POWDER_TINY), new HazardData(
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

        // ── Yellowcake (GIT: powder_yellowcake RADIATION = yc * powder = 0.35 * 3.0) ──
        HazardSystem.register(ModItems.POWDER_YELLOWCAKE.get(), new HazardData(
                new HazardEntry(RADIATION, 1.05f)));
        // Примечание: powder_strontium в оригинальном HazardRegistry хазардов НЕ имеет
        // (радиация только у pellet_rtg_strontium = SR90) — ничего не добавляем.

        registerFrameHazards();
        registerOtherHazards();
    }

    /**
     * Пара waste_* из GIT {@code registerOtherWaste}: свежее = base * 0.075,
     * охлаждающееся (meta 1, предмет из PartTabMetaItems) = base + HOT 5,
     * где base = wst(15) * billet(0.5) * mult.
     */
    private static void registerOtherWastePair(Item fresh, String coolingId, float mult) {
        float base = 15f * 0.5f * mult;
        if (fresh != null) {
            HazardSystem.register(fresh, new HazardData(new HazardEntry(RADIATION, base * 0.075f)));
        }
        Item cooling = com.hbm_m.item.PartTabMetaItems.itemOrNull(coolingId);
        if (cooling != null) {
            HazardSystem.register(cooling, new HazardData(
                    new HazardEntry(RADIATION, base),
                    new HazardEntry(HOT, 5f)));
        }
    }

    /**
     * Хазард для всех предметов группы nuclear_waste_long/short (развёрнутые меты
     * ItemWasteLong/ItemWasteShort): в оригинале хазард общий на предмет, не на мету.
     */
    private static void registerWasteGroup(String group, float radiation, boolean hot) {
        for (Item item : com.hbm_m.item.PartTabMetaItems.group(group)) {
            HazardData data = hot
                    ? new HazardData(new HazardEntry(RADIATION, radiation), new HazardEntry(HOT, 5f))
                    : new HazardData(new HazardEntry(RADIATION, radiation));
            HazardSystem.register(item, data);
        }
    }

    /** Множители форм — 1:1 с GIT {@code HazardRegistry} (nugget/ingot/billet/powder/powder_tiny). */
    private static float shapeMult(MaterialShape shape) {
        return switch (shape) {
            case NUGGET -> 0.1f;
            case BILLET -> 0.5f;
            case POWDER -> 3.0f;
            case POWDER_TINY -> 0.3f;
            default -> 1.0f;
        };
    }

    /**
     * Регистрация хазардов для форм NUGGET/BILLET/POWDER/POWDER_TINY по образцу GIT
     * {@link com.hbm.inventory.OreDictManager} DictFrame: слитковые значения масштабируются
     * множителем формы (включая HOT/BLINDING/HYDROACTIVE, как в оригинале).
     * Форма INGOT здесь не регистрируется — слитки покрыты {@link #registerItems()}.
     */
    private static void registerFrameHazards() {
        frame(ModMaterials.URANIUM,         0.35f, 0, 0, 0);
        frame(ModMaterials.URANIUM233,      5.0f, 0, 0, 0);
        frame(ModMaterials.URANIUM235,      1.0f, 0, 0, 0);
        frame(ModMaterials.URANIUM238,      0.25f, 0, 0, 0);
        frame(ModMaterials.THORIUM232,      0.1f, 0, 0, 0);
        frame(ModMaterials.THORIUM,         0.1f, 0, 0, 0);   // powder_thorium (GIT TH232.dust)
        frame(ModMaterials.PLUTONIUM,       7.5f, 0, 0, 0);
        frame(ModMaterials.PLUTONIUM238,   10.0f, 3, 0, 0);
        frame(ModMaterials.PLUTONIUM239,    5.0f, 0, 0, 0);
        frame(ModMaterials.PLUTONIUM240,    7.5f, 0, 0, 0);
        frame(ModMaterials.PLUTONIUM241,   25.0f, 0, 0, 0);
        frame(ModMaterials.PU_MIX,          6.25f, 0, 0, 0);
        frame(ModMaterials.AM241,           8.5f, 0, 0, 0);
        frame(ModMaterials.AM242,           9.5f, 0, 0, 0);
        frame(ModMaterials.AM_MIX,          9.0f, 0, 0, 0);
        frame(ModMaterials.NEPTUNIUM,       2.5f, 0, 0, 0);
        frame(ModMaterials.POLONIUM,       75.0f, 3, 0, 0);
        frame(ModMaterials.TECHNETIUM,      2.75f, 0, 0, 0);
        frame(ModMaterials.RA226,           7.5f, 0, 0, 0);
        frame(ModMaterials.ACTINIUM,       30.0f, 0, 0, 0);
        frame(ModMaterials.CO60,           30.0f, 1, 0, 0);
        frame(ModMaterials.SR90,           15.0f, 1, 0, 1);
        frame(ModMaterials.AU198,         500.0f, 5, 0, 0);
        frame(ModMaterials.PB209,       10000.0f, 7, 50, 0);
        frame(ModMaterials.SCHRABIDIUM,    15.0f, 0, 50, 0);
        frame(ModMaterials.SCHRABIDATE,     1.5f, 0, 50, 0);
        frame(ModMaterials.SCHRARANIUM,     1.5f, 0, 50, 0);
        frame(ModMaterials.SOLINIUM,       17.5f, 0, 50, 0);
        frame(ModMaterials.GH336,           5.0f, 0, 0, 0);
        frame(ModMaterials.URANIUM_FUEL,    0.5f, 0, 0, 0);
        frame(ModMaterials.THORIUM_FUEL,    1.75f, 0, 0, 0);
        frame(ModMaterials.PLUTONIUM_FUEL,  4.25f, 0, 0, 0);
        frame(ModMaterials.NEPTUNIUM_FUEL,  1.5f, 0, 0, 0);
        frame(ModMaterials.MOX_FUEL,        2.5f, 0, 0, 0);
        frame(ModMaterials.AMERICIUM_FUEL,  4.75f, 0, 0, 0);
        frame(ModMaterials.SCHRABIDIUM_FUEL, 5.85f, 0, 5, 0);
        frame(ModMaterials.HES,             5.85f, 0, 0, 0);
        frame(ModMaterials.I131,          150.0f, 1, 0, 0);
        frame(ModMaterials.XE135,        1250.0f, 10, 0, 0);
        frame(ModMaterials.CS137,          20.0f, 3, 0, 3);
        // Ориг. AT209 (OreDictManager: .rad(HazardRegistry.at209=7500).hot(20F).dust(powder_at209)).
        // Обычный астатин (powder_astatine, 4339) в оригинале опасностей не имеет.
        frame(ModMaterials.AT209,        7500.0f, 20, 0, 0);
        frame(ModMaterials.LITHIUM,         0, 0, 0, 1);          // GIT LI: .hydro(1F)
    }

    /** Регистрирует (материал, форма) для всех форм, кроме INGOT/CRYSTAL/BLOCK. */
    private static void frame(ModMaterials mat, float rad, float hot, float blinding, float hydro) {
        for (MaterialShape shape : MaterialShape.values()) {
            if (shape == MaterialShape.INGOT || shape == MaterialShape.CRYSTAL || shape == MaterialShape.BLOCK) continue;
            if (!ModMaterialItems.has(mat, shape)) continue;

            float mult = shapeMult(shape);
            List<HazardEntry> entries = new ArrayList<>();
            if (rad > 0) entries.add(new HazardEntry(RADIATION, rad * mult));
            if (hot > 0) entries.add(new HazardEntry(HOT, hot * mult));
            if (blinding > 0) entries.add(new HazardEntry(BLINDING, blinding * mult));
            if (hydro > 0) entries.add(new HazardEntry(HYDROACTIVE, hydro * mult));
            if (!entries.isEmpty()) {
                HazardSystem.register(ModMaterialItems.item(mat, shape), new HazardData(entries.toArray(new HazardEntry[0])));
            }
        }
    }

    /** Прочие предметы диапазона вкладки Parts (GIT HazardRegistry.registerItems / OreDictManager). */
    private static void registerOtherHazards() {
        // Броше-заготовки, существующие только в форме billet (базовые значения — billet-уровень).
        hazard(ModMaterialItems.item(ModMaterials.PO210BE, MaterialShape.BILLET), new HazardEntry(RADIATION, 112.5f));
        hazard(ModMaterialItems.item(ModMaterials.RA226BE, MaterialShape.BILLET), new HazardEntry(RADIATION, 11.25f));
        hazard(ModMaterialItems.item(ModMaterials.PU238BE, MaterialShape.BILLET), new HazardEntry(RADIATION, 15.0f));
        hazard(ModMaterialItems.item(ModMaterials.FLASHLEAD, MaterialShape.BILLET), new HazardEntry(RADIATION, 6250f),
                new HazardEntry(HOT, 3.5f)); // pb209*1.25 (rad), hot 7*0.5
        hazard(ModMaterialItems.item(ModMaterials.BALEFIRE_GOLD, MaterialShape.BILLET), new HazardEntry(RADIATION, 250f));
        hazard(ModMaterialItems.item(ModMaterials.NUCLEAR_WASTE, MaterialShape.BILLET), new HazardEntry(RADIATION, 7.5f));
        // GIT HazardRegistry: billet_uzh = RADIATION, uzh(0.125) * billet(0.5)
        hazard(ModMaterialItems.item(ModMaterials.UZH, MaterialShape.BILLET), new HazardEntry(RADIATION, 0.0625f));

        // Заглушки HES/LES топлива (в порте пока отдельные предметы).
        hazard(ModItems.INGOT_HES.get(), new HazardEntry(RADIATION, 5.85f));
        hazard(ModItems.INGOT_LES.get(), new HazardEntry(RADIATION, 5.85f));
        hazard(ModMaterialItems.item(ModMaterials.LES_FUEL, MaterialShape.BILLET), new HazardEntry(RADIATION, 2.925f));
        hazard(ModMaterialItems.item(ModMaterials.LES_FUEL, MaterialShape.NUGGET), new HazardEntry(RADIATION, 0.585f));

        // Твёрдое топливо и продукты бале-файра.
        hazard(ModItems.SOLID_FUEL_BF.get(), new HazardEntry(RADIATION, 1000f));
        hazard(ModItems.SOLID_FUEL_PRESTO_BF.get(), new HazardEntry(RADIATION, 2000f));
        hazard(ModItems.SOLID_FUEL_PRESTO_TRIPLET_BF.get(), new HazardEntry(RADIATION, 6000f));
        hazard(ModItems.POWDER_BALEFIRE.get(), new HazardEntry(RADIATION, 500f)); // thermonuclear ashes
        hazard(ModItems.GEM_RAD.get(), new HazardEntry(RADIATION, 25f));

        // Щелочные металлы и литий-куб.
        hazard(ModItems.LITHIUM.get(), new HazardEntry(HYDROACTIVE, 1.0f));
        hazard(ModItems.INGOT_SODIUM.get(), new HazardEntry(HYDROACTIVE, 1.0f));
        hazard(ModItems.POWDER_SODIUM.get(), new HazardEntry(HYDROACTIVE, 1.0f));
        hazard(ModMaterialItems.item(ModMaterials.CAESIUM, MaterialShape.POWDER), new HazardEntry(HOT, 1.0f),
                new HazardEntry(HYDROACTIVE, 1.0f));

        // Асбест (лист = ingot-форма, порошок).
        hazard(ModMaterialItems.item(ModMaterials.ASBESTOS, MaterialShape.INGOT), new HazardEntry(ASBESTOS, 1.0f));
        hazard(ModMaterialItems.item(ModMaterials.ASBESTOS, MaterialShape.POWDER), new HazardEntry(ASBESTOS, 3.0f));

        // Кристаллы фосфора (hot 1 * crystal-множитель 10).
        hazard(ModMaterialItems.item(ModMaterials.PHOSPHORUS, MaterialShape.CRYSTAL), new HazardEntry(HOT, 10.0f));

        // Метательные ВВ (GIT: cordite 2F, ballistite 1F, ball_dynamite 2F).
        hazard(ModItems.CORDITE.get(), new HazardEntry(EXPLOSIVE, 2.0f));
        hazard(ModItems.BALLISTITE.get(), new HazardEntry(EXPLOSIVE, 1.0f));
        hazard(ModItems.BALL_DYNAMITE.get(), new HazardEntry(EXPLOSIVE, 2.0f));
    }

    private static void hazard(Item item, HazardEntry... entries) {
        if (item != null) {
            HazardSystem.register(item, new HazardData(entries));
        }
    }

    /** GIT {@link com.hbm.hazard.HazardRegistry#block} (=10) × базовая rad слитка ({@link com.hbm.inventory.OreDictManager} DictFrame). */
    private static void registerIngotStorageBlocks() {
        final float block = 10.0f;

        for (ModMaterials ingot : ModMaterials.values()) {
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
