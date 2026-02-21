package com.hbm_m.datagen.assets;

import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModPowders;
import com.hbm_m.lib.RefStrings;
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
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {

    // Теги, определяющие, в какой ФИЗИЧЕСКИЙ СЛОТ можно положить предмет
    public static final TagKey<Item> UPGRADE_MODULES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "upgrade_modules"));

    public static final TagKey<Item> SLOT_HELMET_MODS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "mods/slot_helmet"));
    public static final TagKey<Item> SLOT_CHESTPLATE_MODS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "mods/slot_chestplate"));
    public static final TagKey<Item> SLOT_LEGGINGS_MODS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "mods/slot_leggings"));
    public static final TagKey<Item> SLOT_BOOTS_MODS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "mods/slot_boots"));
    public static final TagKey<Item> SLOT_SERVOS_MODS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "mods/slot_servos"));
    public static final TagKey<Item> SLOT_CLADDING_MODS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "mods/slot_cladding"));
    public static final TagKey<Item> SLOT_SPECIAL_MODS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "mods/slot_special"));
    public static final TagKey<Item> SLOT_BATTERY_MODS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "mods/slot_battery"));
    public static final TagKey<Item> SLOT_INSERT_MODS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "mods/slot_insert"));

    // Теги, определяющие, с каким ТИПОМ БРОНИ совместим предмет
    public static final TagKey<Item> REQUIRES_HELMET = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "mods/requires_helmet"));
    public static final TagKey<Item> REQUIRES_CHESTPLATE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "mods/requires_chestplate"));
    public static final TagKey<Item> REQUIRES_LEGGINGS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "mods/requires_leggings"));
    public static final TagKey<Item> REQUIRES_BOOTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "mods/requires_boots"));

    public static final TagKey<Item> BLADES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "blades"));

    // Теги для штампов пресса
    public static final TagKey<Item> STAMPS_PLATE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "stamps/plate"));
    public static final TagKey<Item> STAMPS_WIRE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "stamps/wire"));
    public static final TagKey<Item> STAMPS_CIRCUIT = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "stamps/circuit"));

    public ModItemTagProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTagsProvider, ExistingFileHelper existingFileHelper) {
        super(packOutput, lookupProvider, blockTagsProvider, RefStrings.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(@Nonnull HolderLookup.Provider provider) {

        //  АВТОМАТИЧЕСКАЯ ГЕНЕРАЦИЯ ТЕГОВ ДЛЯ СЛИТКОВ
        TagsProvider.TagAppender<Item> ingotsTagBuilder = this.tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "ingots")));

        for (ModIngots ingot : ModIngots.values()) {
            RegistryObject<Item> ingotObject = ModItems.getIngot(ingot);
            //  ПРОВЕРКА НА NULL И НА РЕГИСТРАЦИЮ
            if (ingotObject != null && ingotObject.isPresent()) {
                String ingotName = ingot.getName();
                this.tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "ingots/" + ingotName)))
                        .add(ingotObject.get());
                ingotsTagBuilder.add(ingotObject.getKey());
            }
        }

        //  АВТОМАТИЧЕСКАЯ ГЕНЕРАЦИЯ ТЕГОВ ДЛЯ ПОРОШКОВ
        TagsProvider.TagAppender<Item> powdersTagBuilder = this.tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "powders")));

        for (ModPowders powder : ModPowders.values()) {
            RegistryObject<Item> powderObject = ModItems.getPowders(powder);
            //  ПОЛНАЯ ПРОВЕРКА - ИСПРАВЛЕНА ОСНОВНАЯ ОШИБКА!
            if (powderObject != null && powderObject.isPresent()) {
                String powderName = powder.getName();
                this.tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "powders/" + powderName)))
                        .add(powderObject.get());
                powdersTagBuilder.add(powderObject.getKey());
            }
        }

        //  ПОРОШКИ ИЗ СЛИТКОВ
        for (ModIngots ingot : ModIngots.values()) {
            RegistryObject<Item> powderObject = ModItems.getPowder(ingot);
            if (powderObject != null && powderObject.isPresent()) {  //  ДОБАВЛЕНА ПРОВЕРКА isPresent()
                this.tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "powders/" + ingot.getName())))
                        .add(powderObject.get());
                powdersTagBuilder.add(powderObject.getKey());
            }

            //  МАЛЕНЬКИЕ ПОРОШКИ С ПРОВЕРКОЙ
            ModItems.getTinyPowder(ingot).ifPresent(tiny -> {
                if (tiny != null && tiny.isPresent()) {  //  ДОПОЛНИТЕЛЬНАЯ ПРОВЕРКА
                    this.tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "powders/" + ingot.getName() + "/tiny")))
                            .add(tiny.get());
                }
            });
        }

        //  БАЗОВЫЕ ПОРОШКИ (всегда существуют)
        powdersTagBuilder.add(ModItems.DUST.getKey());
        powdersTagBuilder.add(ModItems.DUST_TINY.getKey());


        // АВТОМАТИЧЕСКОЕ КОПИРОВАНИЕ ТЕГОВ ИЗ БЛОКОВ
        this.copy(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/uranium")),
                ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/uranium")));
        this.copy(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/plutonium")),
                ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/plutonium")));
        this.copy(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "ores/uranium")),
                ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "ores/uranium")));
    }
}