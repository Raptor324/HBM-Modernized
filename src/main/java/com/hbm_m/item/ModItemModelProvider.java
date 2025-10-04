package com.hbm_m.item;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;

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
        super(output, MainRegistry.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {

        trimmedArmorItem(com.hbm_m.item.ModItems.ALLOY_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.ALLOY_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.ALLOY_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.ALLOY_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.TITANIUM_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.TITANIUM_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.TITANIUM_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.TITANIUM_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.SECURITY_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.SECURITY_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.SECURITY_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.SECURITY_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.ASBESTOS_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.ASBESTOS_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.ASBESTOS_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.ASBESTOS_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.AJR_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.AJR_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.AJR_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.AJR_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.STEEL_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.STEEL_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.STEEL_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.STEEL_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.PAA_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.PAA_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.PAA_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.PAA_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.LIQUIDATOR_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.LIQUIDATOR_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.LIQUIDATOR_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.LIQUIDATOR_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.HAZMAT_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.HAZMAT_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.HAZMAT_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.HAZMAT_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.STARMETAL_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.STARMETAL_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.STARMETAL_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.STARMETAL_BOOTS);
        trimmedArmorItem(com.hbm_m.item.ModItems.COBALT_HELMET);
        trimmedArmorItem(com.hbm_m.item.ModItems.COBALT_CHESTPLATE);
        trimmedArmorItem(com.hbm_m.item.ModItems.COBALT_LEGGINGS);
        trimmedArmorItem(com.hbm_m.item.ModItems.COBALT_BOOTS);

        evenSimplerBlockItem(ModBlocks.REINFORCED_STONE_STAIRS);
        evenSimplerBlockItem(ModBlocks.REINFORCED_STONE_SLAB);

    }

    private void trimmedArmorItem(RegistryObject<Item> itemRegistryObject) {
        final String MOD_ID = MainRegistry.MOD_ID; // Change this to your mod id

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
