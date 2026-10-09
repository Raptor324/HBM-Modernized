package com.hbm_m.item.tool;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/**
 * Die {@code ToolMaterial}s des Originals ({@code MainRegistry.tMat*}, {@code EnumHelper.addToolMaterial}) mit
 * ihren exakten Werten: Abbaustufe, Haltbarkeit (0 = unzerstoerbar), Abbaugeschwindigkeit, Schadensbonus,
 * Verzauberbarkeit und Reparaturgegenstand ({@code setRepairItem}). Ein Schwert aus dem Original hat
 * {@code 4 + Schadensbonus} Angriffsschaden.
 */
public enum HbmToolMaterial implements Tier {

    WOOD(0, 59, 2.0F, 0.0F, 15, () -> Items.OAK_PLANKS),
    STONE(1, 131, 4.0F, 1.0F, 5, () -> Items.COBBLESTONE),
    EMERALD(3, 1561, 8.0F, 3.0F, 10, () -> Items.DIAMOND),
    /** tMatSchrab "SCHRABIDIUM" */
    SCHRAB(3, 10000, 50.0F, 100.0F, 200, () -> ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.INGOT)),
    /** tMatHammmer "SCHRABIDIUMHAMMER" */
    HAMMER(3, 0, 50.0F, 999999996F, 200, () -> ModBlocks.getIngotBlock(ModMaterials.SCHRABIDIUM).get()),
    /** tMatChainsaw "CHAINSAW" */
    CHAINSAW(3, 1500, 50.0F, 22.0F, 0, () -> ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)),
    /** tMatTitan "HBM_TITANIUM" */
    TITAN(3, 1000, 9.0F, 2.5F, 15, () -> ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.INGOT)),
    /** tMatSteel "HBM_STEEL" */
    STEEL(3, 750, 8.0F, 2.0F, 10, () -> ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)),
    /** tMatAlloy "HBM_ALLOY" */
    ALLOY(3, 2000, 15.0F, 5.0F, 5, null),
    /** tMatCMB "HBM_CMB" */
    CMB(3, 8500, 40.0F, 55F, 100, () -> ModMaterialItems.item(ModMaterials.COMBINE_STEEL, MaterialShape.INGOT)),
    /** tMatElec "HBM_ELEC" */
    ELEC(3, 0, 30.0F, 12.0F, 2, null),
    /** tMatDesh "HBM_DESH" */
    DESH(2, 0, 7.5F, 2.0F, 10, () -> ModMaterialItems.item(ModMaterials.DESH, MaterialShape.INGOT)),
    /** tMatCobalt "HBM_COBALT" */
    COBALT(3, 750, 9.0F, 2.5F, 60, null),
    /** matDecCobalt "HBM_COBALT2" */
    COBALT2(3, 2500, 15.0F, 2.5F, 75, () -> ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.INGOT)),
    /** matStarmetal "HBM_STARMETAL" */
    STARMETAL(3, 3000, 20.0F, 2.5F, 100, () -> ModMaterialItems.item(ModMaterials.STARMETAL, MaterialShape.INGOT)),
    /** matBismuth "HBM_BISMUTH" */
    BISMUTH(4, 0, 50F, 0.0F, 200, () -> ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.INGOT)),
    /** matVolcano "HBM_VOLCANIC" */
    VOLCANIC(4, 0, 50F, 0.0F, 200, () -> ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.INGOT)),
    /** matChlorophyte "HBM_CHLOROPHYTE" */
    CHLOROPHYTE(4, 0, 75F, 0.0F, 200, () -> ModItems.POWDER_CHLOROPHYTE.get()),
    /** matMese "HBM_MESE" */
    MESE(4, 0, 100F, 0.0F, 200, () -> ModItems.PLATE_PAA.get()),
    /** matDwarf "HBM_DWARVEN" */
    DWARVEN(2, 0, 4F, 0.0F, 10, () -> Items.COPPER_INGOT),
    /** matMeteorite "HBM_METEORITE" */
    METEORITE(4, 0, 50F, 0.0F, 200, () -> ModItems.PLATE_PAA.get()),
    /** matMeseGavel "HBM_MESEGAVEL" */
    MESEGAVEL(4, 0, 50F, 0.0F, 200, () -> ModItems.PLATE_PAA.get()),
    /** enumToolMaterialPipeLead "PIPELEAD" */
    PIPELEAD(1, 250, 1.5F, 3F, 25, null),
    /** enumToolMaterialBottleOpener "OPENER" */
    OPENER(1, 250, 1.5F, 0.5F, 200, () -> ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)),
    /** enumToolMaterialSledge "SHIMMERSLEDGE" */
    SLEDGE(1, 0, 25.0F, 26F, 200, null),
    /** enumToolMaterialMultitool "MULTITOOL" (im Original ungenutzt: multitool_* dort entfernt, nur ignoreMappings) */
    MULTITOOL(3, 5000, 25F, 5.5F, 25, null),
    /** matCrucible "CRUCIBLE" (ItemCrucible: drei Ladungen) */
    CRUCIBLE(10, 3, 50.0F, 100.0F, 0, null);

    private final int level;
    private final int uses;
    private final float speed;
    private final float damage;
    private final int enchantment;
    private final Supplier<ItemLike> repair;

    HbmToolMaterial(int level, int uses, float speed, float damage, int enchantment, Supplier<ItemLike> repair) {
        this.level = level;
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.enchantment = enchantment;
        this.repair = repair;
    }

    @Override public int getUses() { return uses; }
    @Override public float getSpeed() { return speed; }
    @Override public float getAttackDamageBonus() { return damage; }
    //? if < 1.21.1 {
    @Override public int getLevel() { return level; }
    //?} else {
    /*public int getLevel() { return level; }
    // 1.21.1: Abbaustufe als Tag der nicht abbaubaren Bloecke (Vanilla-Stufen 0 Holz .. 4 Netherit)
    @Override public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops() {
        return switch (level) {
            case 0 -> net.minecraft.tags.BlockTags.INCORRECT_FOR_WOODEN_TOOL;
            case 1 -> net.minecraft.tags.BlockTags.INCORRECT_FOR_STONE_TOOL;
            case 2 -> net.minecraft.tags.BlockTags.INCORRECT_FOR_IRON_TOOL;
            case 3 -> net.minecraft.tags.BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
            default -> net.minecraft.tags.BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        };
    }
    *///?}
    @Override public int getEnchantmentValue() { return enchantment; }

    @Override
    public @NotNull Ingredient getRepairIngredient() {
        if (repair == null) return Ingredient.EMPTY;
        ItemLike item = repair.get();
        return item == null ? Ingredient.EMPTY : Ingredient.of(item);
    }
}
