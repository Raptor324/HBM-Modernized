package com.hbm_m.hazard;

// Система опасностей для предметов и блоков.
// Позволяет регистрировать опасности для Item, Block и тегов.

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.ModItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class ModHazards {

    public static final TagKey<Item> URANIUM_INGOTS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("forge", "ingots/uranium"));

    public static final TagKey<Item> ALKALI_METALS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("forge", "ingots/sodium"));

     public static void registerHazards() {
        // АВТОМАТИЧЕСКАЯ РЕГИСТРАЦИЯ ОПАСНОСТЕЙ ДЛЯ СЛИТКОВ 
        for (ModIngots ingot : ModIngots.values()) {
            // Используем switch для явного назначения опасностей
            // Это гораздо чище и гибче, чем хранить данные в enum
            switch (ingot) {

                case PU_MIX:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 6.25f)
                    ));
                    break;
                case AM_MIX:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 9f)
                    ));
                    break;
                case MUD:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 1f)
                    ));
                    break;
                case AMERICIUM_FUEL:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 4.75f)
                    ));
                    break;
                case NEPTUNIUM_FUEL:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 1.5f)
                    ));
                    break;
                case PLUTONIUM_FUEL:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 4.25f)
                    ));
                    break;
                case THORIUM_FUEL:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 1.75f)
                    ));
                    break;
                case URANIUM_FUEL:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 0.5f)
                    ));
                    break;
                case SCHRABIDIUM_FUEL:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 5.85f)
                    ));
                    break;
                case MOX_FUEL:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 2.5f)
                    ));
                    break;
                case SR90:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 15f),
                            new HazardEntry(HazardType.PYROPHORIC, 2.0f)
                    ));
                    break;
                case CO60:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 30f),
                            new HazardEntry(HazardType.PYROPHORIC, 2.0f)
                    ));
                    break;
                case THORIUM:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 0.1f)

                    ));
                    break;
                case RA226:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 7.5f)

                    ));
                    break;
                case PB209:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 10000f),
                            new HazardEntry(HazardType.PYROPHORIC, 5.0f)
                    ));
                    break;
                case AU198:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 500f),
                            new HazardEntry(HazardType.PYROPHORIC, 3.0f)
                    ));
                    break;
                case SCHRARANIUM:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 15f)
                    ));
                    break;
                case SCHRABIDATE:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 1.5f)
                    ));
                    break;
                case TECHNETIUM:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 2.75f)
                    ));
                    break;
                case POLONIUM:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 75f),
                            new HazardEntry(HazardType.PYROPHORIC, 3.0f)
                    ));
                    break;
                case NEPTUNIUM:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 2.5f)
                    ));
                    break;
                case AM242:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 9.5f)
                    ));
                    break;
                case AM241:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 8.5f)
                    ));
                    break;
                case STRONTIUM:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 15.0f)
                    ));
                    break;
                case CERIUM:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.PYROPHORIC, 3.0f)
                    ));
                    break;
                case SOLINIUM:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.RADIATION, 17.5f)
                    ));
                    break;
                case PHOSPHORUS:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                            new HazardEntry(HazardType.PYROPHORIC, 1.0f)
                    ));
                    break;
                case URANIUM:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(HazardType.RADIATION, 0.35f)
                    ));
                    break;
                case URANIUM233:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(HazardType.RADIATION, 5.0f)
                    ));
                    break;
                case URANIUM235:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(HazardType.RADIATION, 1.0f)
                    ));
                    break;
                case URANIUM238: 
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(HazardType.RADIATION, 0.25f)
                    ));
                    break;
                case THORIUM232: 
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(HazardType.RADIATION, 1.0f)
                    ));
                    break;
                case PLUTONIUM:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(HazardType.RADIATION, 7.5f)
                    ));
                    break;
                case PLUTONIUM238:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(HazardType.RADIATION, 10.0f),
                        new HazardEntry(HazardType.PYROPHORIC, 2.0f)
                    ));
                    break;
                case PLUTONIUM239:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(HazardType.RADIATION, 5.0f)
                    ));
                    break;
                case PLUTONIUM240:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(HazardType.RADIATION, 7.5f)
                    ));
                    break;
                case PLUTONIUM241:
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(HazardType.RADIATION, 25.0f)
                    ));
                    break;
                case ACTINIUM: // Ваш пример идеально ложится в эту систему
                    HazardSystem.register(ModItems.getIngot(ingot).get(), new HazardData(
                        new HazardEntry(HazardType.RADIATION, 30.0f)
                    ));
                    break;


                // Для STEEL и TECHNELLOY мы ничего не делаем, поэтому они проваливаются в default
                default:
                    // Нет опасностей, ничего не делаем
                    break;
            }
        }
        // БЛОКИ 
        HazardSystem.register(ModBlocks.URANIUM_BLOCK.get(), new HazardData(
            new HazardEntry(HazardType.RADIATION, 3.5f)
        ));
         HazardSystem.register(ModBlocks.NUCLEAR_FALLOUT.get(), new HazardData(
                 new HazardEntry(HazardType.RADIATION, 0.5f)
         ));
        HazardSystem.register(ModBlocks.POLONIUM210_BLOCK.get(), new HazardData(
        new HazardEntry(HazardType.RADIATION, 750f),
        new HazardEntry(HazardType.PYROPHORIC, 5f) // Горит 8 секунд
        ));
        HazardSystem.register(ModBlocks.PLUTONIUM_BLOCK.get(), new HazardData(
            new HazardEntry(HazardType.RADIATION, 75f)
        ));

         HazardSystem.register(ModBlocks.SELLAFIELD_SLAKED.get(), new HazardData(
                 new HazardEntry(HazardType.RADIATION, 0.1f)
         ));

         HazardSystem.register(ModBlocks.SELLAFIELD_SLAKED1.get(), new HazardData(
                 new HazardEntry(HazardType.RADIATION, 0.1f)
         ));

         HazardSystem.register(ModBlocks.SELLAFIELD_SLAKED2.get(), new HazardData(
                 new HazardEntry(HazardType.RADIATION, 0.1f)
         ));

         HazardSystem.register(ModBlocks.SELLAFIELD_SLAKED3.get(), new HazardData(
                 new HazardEntry(HazardType.RADIATION, 0.1f)
         ));

         HazardSystem.register(ModBlocks.BARREL_YELLOW.get(), new HazardData(
                 new HazardEntry(HazardType.RADIATION, 150f)
         ));

         HazardSystem.register(ModBlocks.BARREL_VITRIFIED.get(), new HazardData(
                 new HazardEntry(HazardType.RADIATION, 15f)
         ));

         HazardSystem.register(ModBlocks.BARREL_TAINT.get(), new HazardData(
                 new HazardEntry(HazardType.RADIATION, 67f)
         ));

        HazardSystem.register(ModBlocks.PLUTONIUM_FUEL_BLOCK.get(), new HazardData(
            new HazardEntry(HazardType.RADIATION, 42.5f)
        ));

        // ПРЕДМЕТЫ 
        // Порох взрывается в огне. Сила взрыва 1/4 от ТНТ (4.0f / 4 = 1.0f) за единицу.
        HazardSystem.register(Items.GUNPOWDER, new HazardData(
            new HazardEntry(HazardType.EXPLOSIVE_ON_FIRE, 1.0f)
        ));

         HazardSystem.register(ModItems.WIRE_SCHRABIDIUM.get(), new HazardData(
                 new HazardEntry(HazardType.RADIATION, 0.5f)
         ));

         HazardSystem.register(ModItems.MAN_CORE.get(), new HazardData(
                 new HazardEntry(HazardType.RADIATION, 5f)
         ));

         HazardSystem.register(ModItems.BILLET_PLUTONIUM.get(), new HazardData(
                 new HazardEntry(HazardType.RADIATION, 0.5f)
         ));


         HazardSystem.register(ModItems.PLATE_SCHRABIDIUM.get(), new HazardData(
            new HazardEntry(HazardType.RADIATION, 15.0f)
        ));

        HazardSystem.register(ModItems.getIngot(ModIngots.SCHRABIDIUM).get(), new HazardData(
            new HazardEntry(HazardType.RADIATION, 15.0f)
        ));
        
        // Блок ТНТ, когда он выброшен как предмет и горит, взрывается с ванильной силой.
        // Это НЕ влияет на уже поставленный блок ТНТ, а только на ItemEntity.
        HazardSystem.register(Blocks.TNT.asItem(), new HazardData(
            new HazardEntry(HazardType.EXPLOSIVE_ON_FIRE, 4.0f)
        ));



        HazardSystem.register(Items.TNT_MINECART, new HazardData(
            new HazardEntry(HazardType.EXPLOSIVE_ON_FIRE, 4.0f)
        ));

        // ТЕГИ 
        HazardSystem.register(URANIUM_INGOTS, new HazardData(
            new HazardEntry(HazardType.RADIATION, 0.35f)
        ));

        HazardSystem.register(ALKALI_METALS, new HazardData(
            new HazardEntry(HazardType.HYDRO_REACTIVE, 2.0f) // Взрыв силой 2.0
        ));


        // РЕГИСТРАЦИЯ ЗАЩИТЫ ДЛЯ ВАНИЛЬНОЙ БРОНИ 
        HazardSystem.registerArmorProtection(Items.IRON_HELMET, 0.004f);
        HazardSystem.registerArmorProtection(Items.IRON_CHESTPLATE, 0.009f);
        HazardSystem.registerArmorProtection(Items.IRON_LEGGINGS, 0.006f);
        HazardSystem.registerArmorProtection(Items.IRON_BOOTS, 0.002f);

        HazardSystem.registerArmorProtection(Items.GOLDEN_HELMET, 0.004f);
        HazardSystem.registerArmorProtection(Items.GOLDEN_CHESTPLATE, 0.009f);
        HazardSystem.registerArmorProtection(Items.GOLDEN_LEGGINGS, 0.006f);
        HazardSystem.registerArmorProtection(Items.GOLDEN_BOOTS, 0.002f);

        HazardSystem.registerArmorProtection(Items.DIAMOND_HELMET, 0.05f);
        HazardSystem.registerArmorProtection(Items.DIAMOND_CHESTPLATE, 0.25f);
        HazardSystem.registerArmorProtection(Items.DIAMOND_LEGGINGS, 0.1f);
        HazardSystem.registerArmorProtection(Items.DIAMOND_BOOTS, 0.025f);

        HazardSystem.registerArmorProtection(Items.NETHERITE_HELMET, 0.1f);
        HazardSystem.registerArmorProtection(Items.NETHERITE_CHESTPLATE, 0.45f);
        HazardSystem.registerArmorProtection(Items.NETHERITE_LEGGINGS, 0.2f);
        HazardSystem.registerArmorProtection(Items.NETHERITE_BOOTS, 0.05f);

        // РЕГИСТРАЦИЯ ЗАЩИТЫ ДЛЯ МОДОВОЙ БРОНИ 

        HazardSystem.registerArmorProtection(ModItems.STEEL_HELMET.get(), 0.009f);
        HazardSystem.registerArmorProtection(ModItems.STEEL_CHESTPLATE.get(), 0.018f);
        HazardSystem.registerArmorProtection(ModItems.STEEL_LEGGINGS.get(), 0.013f);
        HazardSystem.registerArmorProtection(ModItems.STEEL_BOOTS.get(), 0.004f);

        HazardSystem.registerArmorProtection(ModItems.TITANIUM_HELMET.get(), 0.009f);
        HazardSystem.registerArmorProtection(ModItems.TITANIUM_CHESTPLATE.get(), 0.018f);
        HazardSystem.registerArmorProtection(ModItems.TITANIUM_LEGGINGS.get(), 0.013f);
        HazardSystem.registerArmorProtection(ModItems.TITANIUM_BOOTS.get(), 0.004f);

        HazardSystem.registerArmorProtection(ModItems.HAZMAT_HELMET.get(), 0.225f);
        HazardSystem.registerArmorProtection(ModItems.HAZMAT_CHESTPLATE.get(), 0.4f);
        HazardSystem.registerArmorProtection(ModItems.HAZMAT_LEGGINGS.get(), 0.2f);
        HazardSystem.registerArmorProtection(ModItems.HAZMAT_BOOTS.get(), 0.075f);

        HazardSystem.registerArmorProtection(ModItems.ALLOY_HELMET.get(), 0.014f);
        HazardSystem.registerArmorProtection(ModItems.ALLOY_CHESTPLATE.get(), 0.028f);
        HazardSystem.registerArmorProtection(ModItems.ALLOY_LEGGINGS.get(), 0.021f);
        HazardSystem.registerArmorProtection(ModItems.ALLOY_BOOTS.get(), 0.007f);

        HazardSystem.registerArmorProtection(ModItems.SECURITY_HELMET.get(), 0.165f);
        HazardSystem.registerArmorProtection(ModItems.SECURITY_CHESTPLATE.get(), 0.33f);
        HazardSystem.registerArmorProtection(ModItems.SECURITY_LEGGINGS.get(), 0.247f);
        HazardSystem.registerArmorProtection(ModItems.SECURITY_BOOTS.get(), 0.082f);

        HazardSystem.registerArmorProtection(ModItems.LIQUIDATOR_HELMET.get(), 0.48f);
        HazardSystem.registerArmorProtection(ModItems.LIQUIDATOR_CHESTPLATE.get(), 0.96f);
        HazardSystem.registerArmorProtection(ModItems.LIQUIDATOR_LEGGINGS.get(), 0.72f);
        HazardSystem.registerArmorProtection(ModItems.LIQUIDATOR_BOOTS.get(), 0.24f);

        HazardSystem.registerArmorProtection(ModItems.PAA_HELMET.get(),0.34f);
        HazardSystem.registerArmorProtection(ModItems.PAA_CHESTPLATE.get(), 0.68f);
        HazardSystem.registerArmorProtection(ModItems.PAA_LEGGINGS.get(), 0.51f);
        HazardSystem.registerArmorProtection(ModItems.PAA_BOOTS.get(), 0.17f);

        HazardSystem.registerArmorProtection(ModItems.STARMETAL_HELMET.get(), 0.2f);
        HazardSystem.registerArmorProtection(ModItems.STARMETAL_CHESTPLATE.get(), 0.4f);
        HazardSystem.registerArmorProtection(ModItems.STARMETAL_LEGGINGS.get(), 0.3f);
        HazardSystem.registerArmorProtection(ModItems.STARMETAL_BOOTS.get(), 0.1f);

        HazardSystem.registerArmorProtection(ModItems.AJR_HELMET.get(), 0.26f);
        HazardSystem.registerArmorProtection(ModItems.AJR_CHESTPLATE.get(), 0.52f);
        HazardSystem.registerArmorProtection(ModItems.AJR_LEGGINGS.get(), 0.39f);
        HazardSystem.registerArmorProtection(ModItems.AJR_BOOTS.get(), 0.13f);

        HazardSystem.registerArmorProtection(ModItems.COBALT_HELMET.get(), 0.025f);
        HazardSystem.registerArmorProtection(ModItems.COBALT_CHESTPLATE.get(), 0.05f);
        HazardSystem.registerArmorProtection(ModItems.COBALT_LEGGINGS.get(), 0.037f);
        HazardSystem.registerArmorProtection(ModItems.COBALT_BOOTS.get(), 0.012f);
    }
}