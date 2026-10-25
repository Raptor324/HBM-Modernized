package com.hbm_m.item.industrial;

import static com.hbm_m.inventory.material.Mats.*;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial;
import com.hbm_m.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1:1 {@code ItemBedrockOreNew.BedrockOreType} samt {@code BedrockOreOutput}, {@code toFluid} und {@code extract}. Die
 * Sortensuffixe entsprechen den Port-Items {@code bedrock_ore_<grade>_<suffix>}.
 */
public enum BedrockOreType {
    //                                       primary                     sulfuric                                         solvent                                          radsolvent
    LIGHT_METAL(0xFFFFFF, 0x353535, "light", o(MAT_IRON, 9), o(MAT_COPPER, 9), o(MAT_TITANIUM, 6), o(MAT_BAUXITE, 9), o(MAT_CRYOLITE, 3), o(MAT_CHLOROCALCITE, 5), o(MAT_LITHIUM, 5), o(MAT_SODIUM, 3), o(MAT_CHLOROCALCITE, 6), o(MAT_LITHIUM, 6), o(MAT_SODIUM, 6)),
    HEAVY_METAL(0x868686, 0x000000, "heavy", o(MAT_TUNGSTEN, 9), o(MAT_LEAD, 9), o(MAT_GOLD, 2), o(MAT_GOLD, 2), o(MAT_BERYLLIUM, 3), o(MAT_TUNGSTEN, 9), o(MAT_LEAD, 9), o(MAT_GOLD, 5), o(MAT_BISMUTH, 2), o(MAT_TANTALIUM, 2), o(MAT_GOLD, 6)),
    RARE_EARTH(0xE6E6B6, 0x1C1C00, "rare", o(MAT_COBALT, 5), o(MAT_RAREEARTH, 5), o(MAT_BORON, 5), o(MAT_LANTHANIUM, 3), o(MAT_NIOBIUM, 4), o(MAT_NEODYMIUM, 3), o(MAT_STRONTIUM, 3), o(MAT_ZIRCONIUM, 3), o(MAT_NIOBIUM, 5), o(MAT_NEODYMIUM, 5), o(MAT_STRONTIUM, 3)),
    ACTINIDE(0xC1C7BD, 0x2B3227, "actinide", o(MAT_URANIUM, 4), o(MAT_THORIUM, 4), o(MAT_RADIUM, 2), o(MAT_RADIUM, 2), o(MAT_POLONIUM, 2), o(MAT_RADIUM, 2), o(MAT_RADIUM, 2), o(MAT_POLONIUM, 2), o(MAT_TECHNETIUM, 1), o(MAT_TECHNETIUM, 1), o(MAT_U238, 1)),
    NON_METAL(0xAFAFAF, 0x0F0F0F, "nonmetal", o(MAT_COAL, 9), o(MAT_SULFUR, 9), o(MAT_LIGNITE, 9), o(MAT_KNO, 6), o(MAT_FLUORITE, 6), o(MAT_PHOSPHORUS, 5), o(MAT_FLUORITE, 6), o(MAT_SULFUR, 6), o(MAT_CHLOROCALCITE, 6), o(MAT_SILICON, 2), o(MAT_SILICON, 2)),
    CRYSTALLINE(0xE2FFFA, 0x1E8A77, "crystal", o(MAT_REDSTONE, 9), o(MAT_CINNABAR, 4), o(MAT_SODALITE, 9), o(MAT_ASBESTOS, 6), o(MAT_DIAMOND, 3), o(MAT_CINNABAR, 3), o(MAT_ASBESTOS, 5), o(MAT_EMERALD, 3), o(MAT_BORAX, 3), o(MAT_MOLYSITE, 3), o(MAT_SODALITE, 9));

    public final int light;
    public final int dark;
    public final String suffix;
    public final BedrockOreOutput primary1, primary2;
    public final BedrockOreOutput byproductAcid1, byproductAcid2, byproductAcid3;
    public final BedrockOreOutput byproductSolvent1, byproductSolvent2, byproductSolvent3;
    public final BedrockOreOutput byproductRad1, byproductRad2, byproductRad3;

    BedrockOreType(int light, int dark, String suffix, BedrockOreOutput p1, BedrockOreOutput p2, BedrockOreOutput bA1, BedrockOreOutput bA2, BedrockOreOutput bA3, BedrockOreOutput bS1, BedrockOreOutput bS2, BedrockOreOutput bS3, BedrockOreOutput bR1, BedrockOreOutput bR2, BedrockOreOutput bR3) {
        this.light = light;
        this.dark = dark;
        this.suffix = suffix;
        this.primary1 = p1; this.primary2 = p2;
        this.byproductAcid1 = bA1; this.byproductAcid2 = bA2; this.byproductAcid3 = bA3;
        this.byproductSolvent1 = bS1; this.byproductSolvent2 = bS2; this.byproductSolvent3 = bS3;
        this.byproductRad1 = bR1; this.byproductRad2 = bR2; this.byproductRad3 = bR3;
    }

    /** {@code ItemBedrockOreNew.BedrockOreOutput}. */
    public record BedrockOreOutput(NTMMaterial mat, int amount) { }

    private static BedrockOreOutput o(NTMMaterial mat, int amount) {
        return new BedrockOreOutput(mat, amount);
    }

    /** {@code ItemBedrockOreNew.make(grade, type)}: das Port-Item {@code bedrock_ore_<grade>_<suffix>}. */
    @Nullable
    public Item item(String gradeKey) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "bedrock_ore_" + gradeKey + "_" + suffix));
        return item == Items.AIR ? null : item;
    }

    /** {@code ItemBedrockOreNew.toFluid}. */
    @Nullable
    public static MaterialStack toFluid(BedrockOreOutput o, double amount) {
        if (o.mat() != null && o.mat().smeltable == SmeltingBehavior.SMELTABLE) {
            return new MaterialStack(o.mat(), (int) Math.ceil(MaterialShapes.FRAGMENT.q(o.amount()) * amount));
        }
        return null;
    }

    /** {@code ItemBedrockOreNew.extract}: Grundgestein-Erzsplitter des Materials. */
    public static ItemStack extract(BedrockOreOutput o, double amount) {
        Item fragment = Mats.getItemForShape(o.mat(), MaterialShapes.FRAGMENT);
        if (fragment == null) fragment = ModItems.BEDROCK_ORE_FRAGMENT.get();
        return new ItemStack(fragment, Math.min((int) Math.ceil(o.amount() * amount), 64));
    }
}
