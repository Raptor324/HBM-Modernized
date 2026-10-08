package com.hbm_m.datagen.recipes.custom;

import static com.hbm_m.datagen.recipes.custom.CraftingGen.m;
import static com.hbm_m.datagen.recipes.custom.CraftingGen.p;

import java.util.function.Consumer;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.custom.CMBlocks.CMCircuit;
import com.hbm_m.block.machines.custom.CMBlocks.CMEngine;
import com.hbm_m.block.machines.custom.CMBlocks.CMMaterial;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 1:1 {@code CraftingManager} "custom machine" (Z. 1046-1066): Gehaeuse je Material, daraus Bleche, Tanks und Ports,
 * Motor- und Schaltkreisbloecke, Fluss- und Waermeempfaenger. ANY_RESISTANTALLOY = TC-/CD-Legierung,
 * ANY_BISMOIDBRONZE = Bismut-/Arsenbronze, KEY_ANYGLASS = Glas-Tag.
 */
public final class CustomMachineRecipeGenerator {

    private CustomMachineRecipeGenerator() { }

    public static void generate(Consumer<FinishedRecipe> w) {
        CraftingGen g = new CraftingGen(w, "custom_machine");

        Ingredient resistantIngot = Ingredient.of(m(ModMaterials.TCALLOY, MaterialShape.INGOT), m(ModMaterials.CDALLOY, MaterialShape.INGOT));
        Ingredient resistantCast = Ingredient.of(m(ModMaterials.TCALLOY, MaterialShape.PLATE_CAST), m(ModMaterials.CDALLOY, MaterialShape.PLATE_CAST));
        Ingredient bronzeIngot = Ingredient.of(m(ModMaterials.BISMUTH_BRONZE, MaterialShape.INGOT), m(ModMaterials.ARSENIC_BRONZE, MaterialShape.INGOT));
        Ingredient bronzeCast = Ingredient.of(m(ModMaterials.BBRONZE, MaterialShape.PLATE_CAST), m(ModMaterials.ABRONZE, MaterialShape.PLATE_CAST));
        var steelIngot = m(ModMaterials.STEEL, MaterialShape.INGOT);

        g.shaped(ModBlocks.CM_BLOCK.get(CMMaterial.STEEL).get(), 4, p(" I ", "IPI", " I "), 'I', steelIngot, 'P', m(ModMaterials.STEEL, MaterialShape.PLATE_CAST));
        g.shaped(ModBlocks.CM_BLOCK.get(CMMaterial.DESH).get(), 4, p(" I ", "IPI", " I "), 'I', m(ModMaterials.DESH, MaterialShape.INGOT), 'P', m(ModMaterials.DESH, MaterialShape.PLATE_CAST));
        g.shaped(ModBlocks.CM_BLOCK.get(CMMaterial.TCALLOY).get(), 4, p(" I ", "IPI", " I "), 'I', resistantIngot, 'P', resistantCast);
        g.shaped(ModBlocks.CM_BLOCK.get(CMMaterial.ALLOY).get(), 4, p(" I ", "IPI", " I "), 'I', bronzeIngot, 'P', bronzeCast);

        for (CMMaterial mat : CMMaterial.values()) {
            var block = ModBlocks.CM_BLOCK.get(mat).get();
            g.shaped(ModBlocks.CM_SHEET.get(mat).get(), 16, p("BB", "BB"), 'B', block);
            g.shaped(ModBlocks.CM_TANK.get(mat).get(), 4, p(" B ", "BGB", " B "), 'B', block, 'G', Ingredient.of(net.minecraftforge.common.Tags.Items.GLASS));
            g.shaped(ModBlocks.CM_PORT.get(mat).get(), 1, p("P", "B", "P"), 'B', block, 'P', m(ModMaterials.IRON, MaterialShape.PLATE));
        }

        g.shaped(ModBlocks.CM_ENGINE.get(CMEngine.STANDARD).get(), 1, p(" I ", "IMI", " I "), 'I', steelIngot, 'M', ModItems.MOTOR);
        g.shaped(ModBlocks.CM_ENGINE.get(CMEngine.DESH).get(), 1, p(" I ", "IMI", " I "), 'I', steelIngot, 'M', ModItems.MOTOR_DESH);
        g.shaped(ModBlocks.CM_ENGINE.get(CMEngine.BISMUTH).get(), 1, p(" I ", "IMI", " I "), 'I', steelIngot, 'M', ModItems.MOTOR_BISMUTH);

        Object[] circuits = { ModItems.VACUUM_TUBE, ModItems.ANALOG_CIRCUIT, ModItems.INTEGRATED_CIRCUIT, ModItems.ADVANCED_CIRCUIT, ModItems.BISMOID_CIRCUIT };
        CMCircuit[] tiers = CMCircuit.values();
        for (int i = 0; i < tiers.length; i++) {
            g.shaped(ModBlocks.CM_CIRCUIT.get(tiers[i]).get(), 1, p(" I ", "IMI", " I "), 'I', steelIngot, 'M', circuits[i]);
        }

        g.shaped(ModBlocks.CM_FLUX.get(), 1, p("NNN", "ZCZ", "NNN"), 'Z', m(ModMaterials.ZIRCONIUM, MaterialShape.PLATE_CAST), 'N', ModItems.NEUTRON_REFLECTOR, 'C', ModItems.REACTOR_CORE);
        g.shaped(ModBlocks.CM_HEAT.get(), 1, p("PCP", "PCP", "PCP"), 'P', m(ModMaterials.POLYMER, MaterialShape.PLATE), 'C', net.minecraft.world.item.Items.COPPER_INGOT);
    }
}
