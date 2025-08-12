package com.hbm_m.hazard;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModIngots;
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
        HazardSystem.register(ModBlocks.POLONIUM210_BLOCK.get(), new HazardData(
        new HazardEntry(HazardType.RADIATION, 750f),
        new HazardEntry(HazardType.PYROPHORIC, 5f) // Горит 8 секунд
        ));
        HazardSystem.register(ModBlocks.PLUTONIUM_BLOCK.get(), new HazardData(
            new HazardEntry(HazardType.RADIATION, 75f)
        ));
        HazardSystem.register(ModBlocks.PLUTONIUM_FUEL_BLOCK.get(), new HazardData(
            new HazardEntry(HazardType.RADIATION, 42.5f)
        ));

        // ПРЕДМЕТЫ 
        // Порох взрывается в огне. Сила взрыва 1/4 от ТНТ (4.0f / 4 = 1.0f) за единицу.
        HazardSystem.register(Items.GUNPOWDER, new HazardData(
            new HazardEntry(HazardType.EXPLOSIVE_ON_FIRE, 1.0f)
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
    }
}