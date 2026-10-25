package com.hbm_m.datagen.recipes.custom;

import static com.hbm_m.datagen.recipes.custom.CraftingGen.m;
import static com.hbm_m.datagen.recipes.custom.CraftingGen.p;

import java.util.function.Consumer;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.PartTabMetaItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.weapon.grenade.GrenadeItems;
import com.hbm_m.item.weapon.grenade.ItemGrenadeExtra.EnumGrenadeExtra;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 1:1 {@code WeaponRecipes} Zeilen 260-284: Teile des Baukasten-Granatensystems. Die Granate selbst entsteht ueber
 * {@code GrenadeCraftingRecipe} (Spezialrezept). OreDict: STEEL.bolt() = bolt_steel, STEEL/WEAPONSTEEL.shell() =
 * shell_steel/shell_weaponsteel, WEAPONSTEEL.mechanism() = part_mechanism_weaponsteel, P_RED.dust() = fire_powder,
 * ANY_SMOKELESS = Ballistit/Cordit, ANY_HIGHEXPLOSIVE = TNT-/TATB-Kugel, circuit CHIP = microchip.
 */
public final class GrenadeRecipeGenerator {

    private GrenadeRecipeGenerator() { }

    private static Item part(String id) {
        var sup = PartTabMetaItems.get(id);
        if (sup != null) return sup.get();
        Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hbm_m", id));
        if (item == Items.AIR) throw new IllegalStateException("Teil fehlt: " + id);
        return item;
    }

    public static void generate(Consumer<FinishedRecipe> w) {
        CraftingGen g = new CraftingGen(w, "grenades");

        Item steelBolt = ModItems.BOLT_STEEL.get();
        Item steelShell = part("shell_steel");
        Item wsShell = part("shell_weaponsteel");
        Item wsMech = part("part_mechanism_weaponsteel");
        Item alPlate = m(ModMaterials.ALUMINUM, MaterialShape.PLATE);
        Item wsPlate = m(ModMaterials.WEAPONSTEEL, MaterialShape.PLATE);
        Item polymerPlate = m(ModMaterials.POLYMER, MaterialShape.PLATE);
        Item wp = m(ModMaterials.PHOSPHORUS, MaterialShape.INGOT);
        Item pu239 = m(ModMaterials.PLUTONIUM239, MaterialShape.NUGGET);
        Item cmbCast = m(ModMaterials.CMB, MaterialShape.PLATE_CAST);
        Item crystalRedstone = m(ModMaterials.REDSTONE, MaterialShape.CRYSTAL);
        Ingredient anySmokeless = Ingredient.of(ModItems.BALLISTITE.get(), ModItems.CORDITE.get());
        Ingredient anyHighExplosive = Ingredient.of(ModItems.BALL_TNT.get(), ModItems.BALL_TATB.get());
        Ingredient keyPlanks = Ingredient.of(ItemTags.PLANKS);

        g.shaped(GrenadeItems.shell(EnumGrenadeShell.FRAG), 4, p("B", "P", "S"), 'B', steelBolt, 'P', alPlate, 'S', steelShell);
        g.shaped(GrenadeItems.shell(EnumGrenadeShell.STICK), 4, p("S", "B", "W"), 'B', steelBolt, 'S', steelShell, 'W', keyPlanks);
        g.shaped(GrenadeItems.shell(EnumGrenadeShell.TECH), 4, p("C", "M", "S"), 'C', ModItems.INTEGRATED_CIRCUIT, 'M', wsMech, 'S', wsShell);
        g.shaped(GrenadeItems.shell(EnumGrenadeShell.NUKE), 2, p(" S ", "CMC", " S "), 'C', ModItems.ADVANCED_CIRCUIT, 'M', wsMech, 'S', wsShell);
        g.shaped(GrenadeItems.fuze(EnumGrenadeFuze.S3), 4, p("S", "F"), 'S', steelBolt, 'F', ModItems.SAFETY_FUSE);
        g.shaped(GrenadeItems.fuze(EnumGrenadeFuze.S7), 4, p("S", "F", "F"), 'S', steelBolt, 'F', ModItems.SAFETY_FUSE);
        g.shaped(GrenadeItems.fuze(EnumGrenadeFuze.S15), 4, p(" S ", " F ", "FFF"), 'S', steelBolt, 'F', ModItems.SAFETY_FUSE);
        g.shaped(GrenadeItems.fuze(EnumGrenadeFuze.IMPACT), 4, p("C", "S", "F"), 'C', anySmokeless, 'S', steelBolt, 'F', ModItems.SAFETY_FUSE);
        g.shaped(GrenadeItems.fuze(EnumGrenadeFuze.AIRBURST), 4, p("C", "S", "F"), 'C', ModItems.VACUUM_TUBE, 'S', steelBolt, 'F', ModItems.SAFETY_FUSE);
        g.shaped(GrenadeItems.filling(EnumGrenadeFilling.POWDER), 4, p("F", "I", "F"), 'F', Items.GUNPOWDER, 'I', polymerPlate);
        g.shaped(GrenadeItems.filling(EnumGrenadeFilling.HE), 4, p("F", "I", "F"), 'F', ModItems.BALL_DYNAMITE, 'I', polymerPlate);
        g.shaped(GrenadeItems.filling(EnumGrenadeFilling.DEMO), 4, p("F", "I", "F"), 'F', anyHighExplosive, 'I', polymerPlate);
        g.shaped(GrenadeItems.filling(EnumGrenadeFilling.INC), 4, p("F", "I", "F"), 'F', ModItems.FIRE_POWDER, 'I', polymerPlate);
        g.shaped(GrenadeItems.filling(EnumGrenadeFilling.WP), 4, p("F", "I", "F"), 'F', wp, 'I', polymerPlate);
        g.shaped(GrenadeItems.filling(EnumGrenadeFilling.CLUSTER), 4, p("F", "I", "F"), 'F', ModItems.PELLET_CLUSTER, 'I', polymerPlate);
        g.shaped(GrenadeItems.filling(EnumGrenadeFilling.CLUSTER_HEAVY), 1, p("F", "I", "F"), 'F', ModItems.PELLET_CLUSTER, 'I', wsPlate);
        g.shaped(GrenadeItems.filling(EnumGrenadeFilling.EMP), 4, p(" C ", "KWK"), 'C', ModItems.INTEGRATED_CIRCUIT, 'K', ModItems.COIL_GOLD, 'W', wsPlate);
        g.shaped(GrenadeItems.filling(EnumGrenadeFilling.PLASMA), 4, p(" C ", "KWK"), 'C', ModItems.CAPACITOR_BOARD, 'K', ModItems.CELL_TRITIUM, 'W', wsPlate);
        g.shaped(GrenadeItems.filling(EnumGrenadeFilling.LASER), 4, p(" C ", "KWK"), 'C', ModItems.ATOMIC_CLOCK, 'K', crystalRedstone, 'W', wsPlate);
        g.shaped(GrenadeItems.filling(EnumGrenadeFilling.NUCLEAR), 1, p(" T ", "CPC", " T "), 'T', ModItems.BALL_TATB, 'C', wsPlate, 'P', pu239);
        g.shaped(GrenadeItems.filling(EnumGrenadeFilling.NUCLEAR_DEMO), 1, p("TPT", "CPC", "TPT"), 'T', ModItems.BALL_TATB, 'C', ModItems.NEUTRON_REFLECTOR, 'P', pu239);
        g.shaped(GrenadeItems.filling(EnumGrenadeFilling.SCHRAB), 1, p("BCB", "TST", "BCB"), 'B', cmbCast, 'C', ModItems.CONTROLLER, 'T', ModItems.BALL_TATB, 'S', ModItems.CELL_SAS3);
        g.shaped(GrenadeItems.extra(EnumGrenadeExtra.GLUE), 1, p(" P ", "PSP", " P "), 'P', Items.PAPER, 'S', Items.SLIME_BALL);
        g.shaped(GrenadeItems.extra(EnumGrenadeExtra.PROXY_FUZE), 1, p("C", "F"), 'C', ModItems.MICROCHIP, 'F', ModItems.SAFETY_FUSE);
        g.shaped(GrenadeItems.extra(EnumGrenadeExtra.FRAG_SLEEVE), 1, p("BBB", " T ", "BBB"), 'B', steelBolt, 'T', ModItems.DUCTTAPE);

        // ConsumableRecipes: bomb_caller Brandbombe (Meta 1) braucht grenade_filling INC
        // -> siehe ConsumableRecipeGenerator (bomb_caller-Varianten), falls dort vorhanden.
    }
}
