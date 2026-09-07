package com.hbm_m.datagen.assets;
//? if forge {
import java.util.concurrent.CompletableFuture;

import com.hbm_m.item.tags_and_tiers.ModTags;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.lib.RefStrings;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;

import static com.hbm_m.item.tags_and_tiers.ModTags.Items.*;

public class ModItemTagProvider extends ItemTagsProvider {

    public ModItemTagProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTagsProvider, ExistingFileHelper existingFileHelper) {
        super(packOutput, lookupProvider, blockTagsProvider, RefStrings.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider) {

        //  АВТОМАТИЧЕСКАЯ ГЕНЕРАЦИЯ ТЕГОВ ДЛЯ СЛИТКОВ
        //? if fabric && < 1.21.1 {
        /*TagsProvider.TagAppender<Item> ingotsTagBuilder = this.tag(ItemTags.create(new ResourceLocation("forge", "ingots")));
        *///?} else {
                TagsProvider.TagAppender<Item> ingotsTagBuilder = this.tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "ingots")));
        //?}


        for (ModMaterials mat : ModMaterials.values()) {
            if (!mat.has(MaterialShape.INGOT)) continue;
            RegistrySupplier<Item> ingotObject = ModMaterialItems.get(mat, MaterialShape.INGOT);
            //  ПРОВЕРКА НА NULL И НА РЕГИСТРАЦИЮ
            if (ingotObject != null && ingotObject.isPresent()) {
                String ingotName = mat.getId();
                //? if fabric && < 1.21.1 {
                /*this.tag(ItemTags.create(new ResourceLocation("forge", "ingots/" + ingotName)))
                        .add(ingotObject.get());
                *///?} else {
                                this.tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "ingots/" + ingotName)))
                        .add(ingotObject.get());
                //?}

                ingotsTagBuilder.add(ingotObject.getKey());
            }
        }

        //  АВТОМАТИЧЕСКАЯ ГЕНЕРАЦИЯ ТЕГОВ ДЛЯ ПОРОШКОВ
        //? if fabric && < 1.21.1 {
        /*TagsProvider.TagAppender<Item> powdersTagBuilder = this.tag(ItemTags.create(new ResourceLocation("forge", "powders")));
        *///?} else {
                TagsProvider.TagAppender<Item> powdersTagBuilder = this.tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "powders")));
        //?}


        //  ПОРОШКИ (базовые и из слитков — единый реестр материалов)
        for (ModMaterials mat : ModMaterials.values()) {
            if (mat.has(MaterialShape.POWDER)) {
                RegistrySupplier<Item> powderObject = ModMaterialItems.get(mat, MaterialShape.POWDER);
                //  ПОЛНАЯ ПРОВЕРКА - ИСПРАВЛЕНА ОСНОВНАЯ ОШИБКА!
                if (powderObject != null && powderObject.isPresent()) {
                    String powderName = mat.getId();
                    //? if fabric && < 1.21.1 {
                    /*this.tag(ItemTags.create(new ResourceLocation("forge", "powders/" + powderName)))
                            .add(powderObject.get());
                    *///?} else {
                                    this.tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "powders/" + powderName)))
                            .add(powderObject.get());
                    //?}

                    powdersTagBuilder.add(powderObject.getKey());
                }
            }

            //  МАЛЕНЬКИЕ ПОРОШКИ С ПРОВЕРКОЙ
            if (mat.has(MaterialShape.POWDER_TINY)) {
                RegistrySupplier<Item> tinyObject = ModMaterialItems.get(mat, MaterialShape.POWDER_TINY);
                if (tinyObject != null && tinyObject.isPresent()) {
                    //? if fabric && < 1.21.1 {
                    /*this.tag(ItemTags.create(new ResourceLocation("forge", "powders/" + mat.getId() + "/tiny")))
                            .add(tinyObject.get());
                    *///?} else {
                                        this.tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "powders/" + mat.getId() + "/tiny")))
                            .add(tinyObject.get());
                    //?}
                }
            }
        }

        //  БАЗОВЫЕ ПОРОШКИ (всегда существуют)
        powdersTagBuilder.add(ModItems.DUST.getKey());
        powdersTagBuilder.add(ModItems.DUST_TINY.getKey());


        // АВТОМАТИЧЕСКОЕ КОПИРОВАНИЕ ТЕГОВ ИЗ БЛОКОВ
        //? if fabric && < 1.21.1 {
        /*this.copy(BlockTags.create(new ResourceLocation("forge", "storage_blocks/uranium")),
                ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/uranium")));
        *///?} else {
                this.copy(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/uranium")),
                ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/uranium")));
        //?}

        //? if fabric && < 1.21.1 {
        /*this.copy(BlockTags.create(new ResourceLocation("forge", "storage_blocks/plutonium")),
                ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/plutonium")));
        *///?} else {
                this.copy(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/plutonium")),
                ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/plutonium")));
        //?}

        //? if fabric && < 1.21.1 {
        /*this.copy(BlockTags.create(new ResourceLocation("forge", "ores/uranium")),
                ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "ores/uranium")));
        *///?} else {
                this.copy(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "ores/uranium")),
                ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "ores/uranium")));
        //?}



        this.tag(ModTags.Items.SLABS_HARD)
                .add(Items.STONE_SLAB)
                .add(Items.COBBLESTONE_SLAB)
                .add(Items.SMOOTH_STONE_SLAB)
                .add(Items.STONE_BRICK_SLAB)
                .add(Items.MOSSY_STONE_BRICK_SLAB)
                .add(Items.GRANITE_SLAB)
                .add(Items.POLISHED_GRANITE_SLAB)
                .add(Items.DIORITE_SLAB)
                .add(Items.POLISHED_DIORITE_SLAB)
                .add(Items.ANDESITE_SLAB)
                .add(Items.POLISHED_ANDESITE_SLAB)
                .add(Items.COBBLED_DEEPSLATE_SLAB)
                .add(Items.POLISHED_DEEPSLATE_SLAB)
                .add(Items.DEEPSLATE_BRICK_SLAB)
                .add(Items.DEEPSLATE_TILE_SLAB)
                .add(Items.BRICK_SLAB)
                .add(Items.MUD_BRICK_SLAB)
                .add(Items.SANDSTONE_SLAB)
                .add(Items.SMOOTH_SANDSTONE_SLAB)
                .add(Items.CUT_STANDSTONE_SLAB)
                .add(Items.RED_SANDSTONE_SLAB)
                .add(Items.SMOOTH_RED_SANDSTONE_SLAB)
                .add(Items.CUT_RED_SANDSTONE_SLAB)
                .add(Items.PRISMARINE_BRICK_SLAB)
                .add(Items.PRISMARINE_SLAB)
                .add(Items.DARK_PRISMARINE_SLAB)
                .add(Items.NETHER_BRICK_SLAB)
                .add(Items.RED_NETHER_BRICK_SLAB)
                .add(Items.BLACKSTONE_SLAB)
                .add(Items.POLISHED_BLACKSTONE_SLAB)
                .add(Items.POLISHED_BLACKSTONE_BRICK_SLAB)
                .add(Items.END_STONE_BRICK_SLAB)
                .add(Items.PURPUR_SLAB)
                .add(Items.QUARTZ_SLAB)
                .add(Items.SMOOTH_QUARTZ_SLAB)
                .add(Items.CUT_COPPER_SLAB)
                .add(Items.EXPOSED_CUT_COPPER_SLAB)
                .add(Items.WEATHERED_CUT_COPPER_SLAB)
                .add(Items.OXIDIZED_CUT_COPPER_SLAB)
                .add(Items.MOSSY_COBBLESTONE_SLAB);

        this.tag(ModTags.Items.BLADES)
                .add(ModItems.BLADE_STEEL.get())
                .add(ModItems.BLADE_TITANIUM.get())
                .add(ModItems.BLADE_ALLOY.get())
                .add(ModItems.BLADE_TEST.get());
        // ТЕГИ ДЛЯ ШТАМПОВ ПРЕССА

        // Alle Flach-Stempel (StampType.FLAT im Original)
        this.tag(ModTags.Items.STAMPS_FLAT)
                .add(ModItems.STAMP_STONE_FLAT.get())
                .add(ModItems.STAMP_IRON_FLAT.get())
                .add(ModItems.STAMP_STEEL_FLAT.get())
                .add(ModItems.STAMP_TITANIUM_FLAT.get())
                .add(ModItems.STAMP_OBSIDIAN_FLAT.get())
                .add(ModItems.STAMP_DESH_FLAT.get());

        // Все штампы пластин
        this.tag(ModTags.Items.STAMPS_PLATE)
                .add(ModItems.STAMP_STONE_PLATE.get())
                .add(ModItems.STAMP_IRON_PLATE.get())
                .add(ModItems.STAMP_STEEL_PLATE.get())
                .add(ModItems.STAMP_TITANIUM_PLATE.get())
                .add(ModItems.STAMP_OBSIDIAN_PLATE.get())
                .add(ModItems.STAMP_DESH_PLATE.get());

        // Rubber Bars
        this.tag(ModTags.Items.RUBBER_BAR)
                .add(ModMaterialItems.item(ModMaterials.BIORUBBER, MaterialShape.INGOT))
                .add(ModMaterialItems.item(ModMaterials.RUBBER, MaterialShape.INGOT));

        this.tag(ZIRNOX_RODS)
                .add(ModItems.ROD_ZIRNOX_LES_FUEL.get())
                .add(ModItems.ROD_ZIRNOX_LITHIUM.get())
                .add(ModItems.ROD_ZIRNOX_MOX_FUEL.get())
                .add(ModItems.ROD_ZIRNOX_NATURAL_URANIUM_FUEL.get())
                .add(ModItems.ROD_ZIRNOX_PLUTONIUM_FUEL.get())
                .add(ModItems.ROD_ZIRNOX_TH232.get())
                .add(ModItems.ROD_ZIRNOX_THORIUM_FUEL.get())
                .add(ModItems.ROD_ZIRNOX_U233_FUEL.get())
                .add(ModItems.ROD_ZIRNOX_U235_FUEL.get())
                .add(ModItems.ROD_ZIRNOX_URANIUM_FUEL.get())
                .add(ModItems.ROD_ZIRNOX_ZFB_MOX.get())
                ;

        // Все штампы проводов
        this.tag(ModTags.Items.STAMPS_WIRE)
                .add(ModItems.STAMP_STONE_WIRE.get())
                .add(ModItems.STAMP_IRON_WIRE.get())
                .add(ModItems.STAMP_STEEL_WIRE.get())
                .add(ModItems.STAMP_TITANIUM_WIRE.get())
                .add(ModItems.STAMP_OBSIDIAN_WIRE.get())
                .add(ModItems.STAMP_DESH_WIRE.get());

        // Все штампы микросхем
        this.tag(ModTags.Items.STAMPS_CIRCUIT)
                .add(ModItems.STAMP_STONE_CIRCUIT.get())
                .add(ModItems.STAMP_IRON_CIRCUIT.get())
                .add(ModItems.STAMP_STEEL_CIRCUIT.get())
                .add(ModItems.STAMP_TITANIUM_CIRCUIT.get())
                .add(ModItems.STAMP_OBSIDIAN_CIRCUIT.get())
                .add(ModItems.STAMP_DESH_CIRCUIT.get());

        this.tag(ModTags.Items.REQUIRES_HELMET)
                .add(ModItems.GHIORSIUM_CLADDING.get())
                .add(ModItems.DESH_CLADDING.get())
                .add(ModItems.LEAD_CLADDING.get())
                .add(ModItems.RUBBER_CLADDING.get())
                .add(ModItems.PAINT_CLADDING.get())
                // Новые модификаторы брони
                // .add(ModItems.ARMOR_MOD_SERVOS.get())
                // .add(ModItems.ARMOR_MOD_CLADDING.get())
                // .add(ModItems.ARMOR_MOD_KEVLAR.get())
                // .add(ModItems.ARMOR_MOD_EXTRA.get())
                .add(ModItems.ARMOR_BATTERY.get())
                .add(ModItems.ARMOR_BATTERY_MK2.get())
                .add(ModItems.ARMOR_BATTERY_MK3.get());

        this.tag(ModTags.Items.REQUIRES_CHESTPLATE)
                .add(ModItems.HEART_PIECE.get())
                .add(ModItems.HEART_CONTAINER.get())
                .add(ModItems.HEART_BOOSTER.get())
                .add(ModItems.HEART_FAB.get())
                .add(ModItems.BLACK_DIAMOND.get())
                .add(ModItems.GHIORSIUM_CLADDING.get())
                .add(ModItems.DESH_CLADDING.get())
                .add(ModItems.LEAD_CLADDING.get())
                .add(ModItems.RUBBER_CLADDING.get())
                .add(ModItems.PAINT_CLADDING.get())
                // Новые модификаторы брони
                // .add(ModItems.ARMOR_MOD_SERVOS.get())
                // .add(ModItems.ARMOR_MOD_CLADDING.get())
                // .add(ModItems.ARMOR_MOD_KEVLAR.get())
                // .add(ModItems.ARMOR_MOD_EXTRA.get())
                .add(ModItems.ARMOR_BATTERY.get())
                .add(ModItems.ARMOR_BATTERY_MK2.get())
                .add(ModItems.ARMOR_BATTERY_MK3.get());

        this.tag(ModTags.Items.REQUIRES_LEGGINGS)
                .add(ModItems.GHIORSIUM_CLADDING.get())
                .add(ModItems.DESH_CLADDING.get())
                .add(ModItems.LEAD_CLADDING.get())
                .add(ModItems.RUBBER_CLADDING.get())
                .add(ModItems.PAINT_CLADDING.get())
                // Новые модификаторы брони
                // .add(ModItems.ARMOR_MOD_SERVOS.get())
                // .add(ModItems.ARMOR_MOD_CLADDING.get())
                // .add(ModItems.ARMOR_MOD_KEVLAR.get())
                // .add(ModItems.ARMOR_MOD_EXTRA.get())
                .add(ModItems.ARMOR_BATTERY.get())
                .add(ModItems.ARMOR_BATTERY_MK2.get())
                .add(ModItems.ARMOR_BATTERY_MK3.get());

        this.tag(ModTags.Items.REQUIRES_BOOTS)
                .add(ModItems.GHIORSIUM_CLADDING.get())
                .add(ModItems.DESH_CLADDING.get())
                .add(ModItems.LEAD_CLADDING.get())
                .add(ModItems.RUBBER_CLADDING.get())
                .add(ModItems.PAINT_CLADDING.get())
                // Новые модификаторы брони
                // .add(ModItems.ARMOR_MOD_SERVOS.get())
                // .add(ModItems.ARMOR_MOD_CLADDING.get())
                // .add(ModItems.ARMOR_MOD_KEVLAR.get())
                // .add(ModItems.ARMOR_MOD_EXTRA.get())
                .add(ModItems.ARMOR_BATTERY.get())
                .add(ModItems.ARMOR_BATTERY_MK2.get())
                .add(ModItems.ARMOR_BATTERY_MK3.get());

        // ТЕГИ ДЛЯ МОДИФИКАТОРОВ БРОНИ
        this.tag(SLOT_SPECIAL_MODS)
                .add(ModItems.HEART_PIECE.get())
                .add(ModItems.HEART_CONTAINER.get())
                .add(ModItems.HEART_BOOSTER.get())
                .add(ModItems.HEART_FAB.get())
                .add(ModItems.BLACK_DIAMOND.get());

        this.tag(ModTags.Items.SLOT_CLADDING_MODS)
                .add(ModItems.GHIORSIUM_CLADDING.get())
                .add(ModItems.DESH_CLADDING.get())
                .add(ModItems.LEAD_CLADDING.get())
                .add(ModItems.RUBBER_CLADDING.get())
                .add(ModItems.PAINT_CLADDING.get());


        this.tag(ModTags.Items.SLOT_HELMET_MODS);
        this.tag(ModTags.Items.SLOT_CHESTPLATE_MODS);
        this.tag(ModTags.Items.SLOT_LEGGINGS_MODS);
        this.tag(ModTags.Items.SLOT_BOOTS_MODS);
        this.tag(ModTags.Items.SLOT_SERVOS_MODS);
        
        this.tag(SLOT_BATTERY_MODS)
                .add(ModItems.ARMOR_BATTERY.get())
                .add(ModItems.ARMOR_BATTERY_MK2.get())
                .add(ModItems.ARMOR_BATTERY_MK3.get());
                
        this.tag(SLOT_INSERT_MODS);

        this.tag(ModTags.Items.UPGRADE_MODULES)
                .addTag(SLOT_HELMET_MODS)
                .addTag(SLOT_CHESTPLATE_MODS)
                .addTag(SLOT_LEGGINGS_MODS)
                .addTag(SLOT_BOOTS_MODS)
                .addTag(SLOT_SERVOS_MODS)
                .addTag(SLOT_CLADDING_MODS)
                .addTag(SLOT_SPECIAL_MODS)
                .addTag(SLOT_BATTERY_MODS)
                .addTag(SLOT_INSERT_MODS);

        this.tag(ItemTags.TRIMMABLE_ARMOR)
        .add(ModItems.ALLOY_HELMET.get(),
                ModItems.ALLOY_CHESTPLATE.get(),
                ModItems.ALLOY_LEGGINGS.get(),
                ModItems.TITANIUM_HELMET.get(),
                ModItems.TITANIUM_CHESTPLATE.get(),
                ModItems.TITANIUM_LEGGINGS.get(),
                ModItems.TITANIUM_BOOTS.get(),
                ModItems.STEEL_HELMET.get(),
                ModItems.STEEL_CHESTPLATE.get(),
                ModItems.STEEL_LEGGINGS.get(),
                ModItems.STEEL_BOOTS.get(),
                ModItems.HAZMAT_HELMET.get(),
                ModItems.HAZMAT_CHESTPLATE.get(),
                ModItems.HAZMAT_LEGGINGS.get(),
                ModItems.HAZMAT_BOOTS.get(),
                ModItems.SECURITY_HELMET.get(),
                ModItems.SECURITY_CHESTPLATE.get(),
                ModItems.SECURITY_LEGGINGS.get(),
                ModItems.SECURITY_BOOTS.get(),
                ModItems.AJR_HELMET.get(),
                ModItems.AJR_CHESTPLATE.get(),
                ModItems.AJR_LEGGINGS.get(),
                ModItems.AJR_BOOTS.get(),
                ModItems.STARMETAL_HELMET.get(),
                ModItems.STARMETAL_CHESTPLATE.get(),
                ModItems.STARMETAL_LEGGINGS.get(),
                ModItems.STARMETAL_BOOTS.get(),
                ModItems.ASBESTOS_HELMET.get(),
                ModItems.ASBESTOS_CHESTPLATE.get(),
                ModItems.ASBESTOS_LEGGINGS.get(),
                ModItems.ASBESTOS_BOOTS.get(),
                ModItems.COBALT_HELMET.get(),
                ModItems.COBALT_CHESTPLATE.get(),
                ModItems.COBALT_LEGGINGS.get(),
                ModItems.COBALT_BOOTS.get(),
                ModItems.ALLOY_BOOTS.get());

        // Vanilla jukebox accepts only items in this tag (1.20.1). JukeboxBlockEntity.setItem
        // skips storing, setting HAS_RECORD and startPlaying() entirely when the stack is not
        // tagged - while RecordItem.useOn shrinks the held stack regardless. An untagged disc is
        // therefore silently destroyed on insertion, so every disc MUST be listed here.
        this.tag(ItemTags.MUSIC_DISCS)
                .add(ModItems.MUSIC_DISC_BUNKER.get())
                .add(ModItems.MUSIC_DISC_GLASS.get())
                .add(ModItems.MUSIC_DISC_CH.get());

        // Forge dye tags used by assembler recipes (JEI + crafting)
        this.tag(Tags.Items.DYES_GREEN).add(Items.GREEN_DYE);
        this.tag(Tags.Items.DYES_RED).add(Items.RED_DYE);
        this.tag(Tags.Items.DYES_BLACK).add(Items.BLACK_DYE);
        this.tag(Tags.Items.DYES_WHITE).add(Items.WHITE_DYE);
        this.tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "wires_fine/lead")))
                .add(ModMaterialItems.item(ModMaterials.CARBON, MaterialShape.WIRE));

        // Химические красители и мелки: ванильные теги minecraft:<color>_dyes, чтобы их
        // принимали ванильные/модовые рецепты красителей (16 цветов EnumChemDye оригинала).
        String[][] dyeColors = {
                {"black", "black"}, {"red", "red"}, {"green", "green"}, {"brown", "brown"},
                {"blue", "blue"}, {"purple", "purple"}, {"cyan", "cyan"}, {"silver", "light_gray"},
                {"gray", "gray"}, {"pink", "pink"}, {"lime", "lime"}, {"yellow", "yellow"},
                {"lightblue", "light_blue"}, {"magenta", "magenta"}, {"orange", "orange"}, {"white", "white"},
        };
        for (String[] dc : dyeColors) {
            Item chemical = com.hbm_m.item.PartTabMetaItems.itemOrNull("chemical_dye_" + dc[0]);
            Item crayonItem = com.hbm_m.item.PartTabMetaItems.itemOrNull("crayon_" + dc[0]);
            var builder = this.tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("minecraft", dc[1] + "_dyes")));
            if (chemical != null) builder.add(chemical);
            if (crayonItem != null) builder.add(crayonItem);
        }
    }
}
//?}