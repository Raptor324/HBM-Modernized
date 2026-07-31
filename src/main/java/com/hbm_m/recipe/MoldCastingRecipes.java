package com.hbm_m.recipe;

import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;
import com.hbm_m.item.material.ItemCastMold;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Casting outputs for the foundry basin's molds.
 * Follows the original 1.7.10 {@code Mold.getOutput(material)} idea: each mold
 * shape dictates the amount of molten material needed (= basin capacity) and
 * the resulting item for a given material.
 *
 * <p><b>PLATE vs PLATE_CAST:</b> in the original these are two distinct molds —
 * {@code PLATE} (shape {@code MaterialShapes.PLATE}) produces the plain,
 * pressed-look plate item ({@code plate_<mat>}), while {@code PLATE_CAST}
 * (shape {@code MaterialShapes.CASTPLATE}) produces the rougher cast-look
 * plate ({@code plate_cast_<mat>}, i.e. {@link MaterialType#getCastPlate}).
 *
 * <p>Outputs are resolved data-driven via forge item tags
 * (forge:ingots/&lt;mat&gt;, forge:nuggets/&lt;mat&gt;, forge:storage_blocks/&lt;mat&gt;)
 * or by item id (hbm_m:plate_&lt;mat&gt;, hbm_m:wire_&lt;mat&gt;, ...), so all
 * materials work without hardcoding every mold × material combination.
 */
public class MoldCastingRecipes {

    /** Alternate spellings/names used across this mod's item ids for the same material. */
    private static List<String> nameCandidates(MaterialType mat) {
        List<String> out = new ArrayList<>(3);
        out.add(mat.name);
        if (mat.name.equals("aluminium")) out.add("aluminum");
        if (mat.name.equals("cmb"))       out.add("combine_steel");
        if (mat.name.equals("alloy"))     out.add("advanced_alloy");
        return out;
    }

    /** Amount of molten material (mb) a mold requires. 0 = mold not castable (yet). */
    public static int getCost(ItemCastMold.MoldType mold) {
        return switch (mold) {
            case NUGGET                        -> MaterialStack.MB_PER_NUGGET;
            case PLATE, INGOT, WIRE, WIRE_DENSE,
                 SHELL, PIPE, BILLET            -> MaterialStack.MB_PER_INGOT;
            case PLATE_CAST                    -> MaterialStack.MB_PER_PLATE;
            case PLATES_CAST                   -> MaterialStack.MB_PER_PLATE * 3;
            case INGOTS, PLATES, WIRES_DENSE,
                 BLOCK                          -> MaterialStack.MB_PER_INGOT * 9;
            default                            -> 0;
        };
    }

    /** The item cast by the given mold from the given material, or EMPTY if that combo has no output. */
    public static ItemStack getOutput(ItemCastMold.MoldType mold, MaterialType mat) {
        return switch (mold) {
            case PLATE       -> byId("plate_", mat, 1);
            case PLATES      -> byId("plate_", mat, 9);
            case PLATE_CAST  -> castPlate(mat, 1);
            case PLATES_CAST -> castPlate(mat, 3);
            case INGOT       -> fromTag("ingots", mat, 1);
            case INGOTS      -> fromTag("ingots", mat, 9);
            case NUGGET      -> fromTag("nuggets", mat, 1);
            case BLOCK       -> fromTag("storage_blocks", mat, 1);
            case WIRE        -> byId("wire_", mat, 8);
            case WIRE_DENSE  -> byId("wire_dense_", mat, 1);
            case WIRES_DENSE -> byId("wire_dense_", mat, 9);
            case SHELL       -> byId("shell_", mat, 1);
            case PIPE        -> byId("pipe_", mat, 1);
            case BILLET      -> byId("billet_", mat, 1);
            default          -> ItemStack.EMPTY;
        };
    }

    /** A representative item for the given material, for JEI display purposes only. */
    public static ItemStack getMaterialIcon(MaterialType mat) {
        ItemStack ingot = fromTag("ingots", mat, 1);
        if (!ingot.isEmpty()) return ingot;
        return castPlate(mat, 1);
    }

    private static ItemStack castPlate(MaterialType mat, int count) {
        ItemStack plate = mat.getCastPlate(count);
        return plate != null ? plate : ItemStack.EMPTY;
    }

    private static ItemStack fromTag(String prefix, MaterialType mat, int count) {
        for (String candidate : nameCandidates(mat)) {
            ItemStack out = firstTagItem(prefix + "/" + candidate, count);
            if (!out.isEmpty()) return out;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack firstTagItem(String path, int count) {
        TagKey<Item> key = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("forge", path));
        return BuiltInRegistries.ITEM.getTag(key)
                .map(tag -> tag.size() > 0 ? new ItemStack(tag.get(0).value(), count) : ItemStack.EMPTY)
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack byId(String prefix, MaterialType mat, int count) {
        for (String candidate : nameCandidates(mat)) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm_m", prefix + candidate);
            if (BuiltInRegistries.ITEM.containsKey(id)) {
                return new ItemStack(BuiltInRegistries.ITEM.get(id), count);
            }
        }
        return ItemStack.EMPTY;
    }
}
