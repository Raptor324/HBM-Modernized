package com.hbm_m.datagen.recipes.custom;

import java.util.function.Consumer;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.PartTabMetaItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm_m.recipe.FluidContainerIngredient;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 1:1-Port von {@code AmmoPressRecipes.registerDefaults()} (alle 89 Rezepte, gleiche Slots, Mengen und Ausbeuten).
 * Slots 0-8 zeilenweise wie im Original. OreDict-Abbildung: ANY_PLASTIC = Polymer/Bakelit, ANY_SMOKELESS = Ballistit/
 * Cordit, ANY_HIGHEXPLOSIVE = TNT-/TATB-Kugel, P_RED.dust() = fire_powder, P_WHITE.ingot() = Phosphor-Barren,
 * PB.bolt() = bolt_lead; Fluessigkeiten ({@code Fluids.X.getDict(n)}) = {@link FluidContainerIngredient};
 * Huelsen = {@code casing_<typ>}.
 */
public class AmmoPressRecipeGenerator {

    private static Consumer<FinishedRecipe> writer;
    private static final java.util.Set<String> USED = new java.util.HashSet<>();

    /** Ein Feld: Zutat + Menge (Original {@code AStack.copy(n)}). */
    private record S(Ingredient ing, int count) {
        S copy(int n) { return new S(ing, n); }
    }

    private static S s(Ingredient i) { return new S(i, 1); }
    private static S s(net.minecraft.world.level.ItemLike i) { return new S(Ingredient.of(i), 1); }

    private static void add(EnumAmmo out, int count, S... in) {
        AmmoPressRecipeBuilder b = AmmoPressRecipeBuilder.ammoPressRecipe(new ItemStack(WeaponItems.ammo(out), count));
        for (int i = 0; i < 9; i++) {
            if (in[i] != null) b.slot(i, in[i].ing, in[i].count);
        }
        String base = "ammo_press/" + out.name().toLowerCase(java.util.Locale.US);
        String id = base;
        int n = 1;
        while (USED.contains(id)) id = base + "_" + (++n);
        USED.add(id);
        b.save(writer, id);
    }

    public static void generate(Consumer<FinishedRecipe> w) {
        writer = w;
        USED.clear();

        S lead = s(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.INGOT));
        S nugget = s(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.NUGGET));
        S flechette = s(ModItems.BOLT_LEAD.get());
        S steel = s(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT));
        S wSteel = s(ModMaterialItems.item(ModMaterials.WEAPONSTEEL, MaterialShape.INGOT));
        S copper = s(Items.COPPER_INGOT);
        S plastic = s(Ingredient.of(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.INGOT), ModMaterialItems.item(ModMaterials.BAKELITE, MaterialShape.INGOT)));
        S uranium = s(ModMaterialItems.item(ModMaterials.URANIUM238, MaterialShape.INGOT));
        S ferro = s(ModMaterialItems.item(ModMaterials.FERROURANIUM, MaterialShape.INGOT));
        S nb = s(ModMaterialItems.item(ModMaterials.NIOBIUM, MaterialShape.INGOT));
        S smokeless = s(Ingredient.of(ModItems.BALLISTITE.get(), ModItems.CORDITE.get()));
        S he = s(Ingredient.of(ModItems.BALL_TNT.get(), ModItems.BALL_TATB.get()));
        S wp = s(ModMaterialItems.item(ModMaterials.PHOSPHORUS, MaterialShape.INGOT));
        S rp = s(ModItems.FIRE_POWDER.get());
        S pipe = s(ModItems.PIPE_STEEL.get());
        S smokeful = s(Items.GUNPOWDER);
        S rocket = s(ModItems.ROCKET_FUEL.get());
        S cSmall = s(PartTabMetaItems.get("casing_small").get());
        S cBig = s(PartTabMetaItems.get("casing_large").get());
        S sSmall = s(PartTabMetaItems.get("casing_small_steel").get());
        S sBig = s(PartTabMetaItems.get("casing_large_steel").get());
        S bpShell = s(PartTabMetaItems.get("casing_shotshell").get());
        S pShell = s(PartTabMetaItems.get("casing_buckshot").get());
        S sShell = s(PartTabMetaItems.get("casing_buckshot_advanced").get());

        add(EnumAmmo.M357_BP, 16,
                null,	lead.copy(2),	null,
                null,	smokeful,		null,
                null,	cSmall,			null);
        add(EnumAmmo.M357_SP, 8,
                null,	lead,		null,
                null,	smokeless,	null,
                null,	cSmall,		null);
        add(EnumAmmo.M357_FMJ, 8,
                null,	steel,		null,
                null,	smokeless,	null,
                null,	cSmall,		null);
        add(EnumAmmo.M357_JHP, 8,
                plastic,	copper,		null,
                null,		smokeless,	null,
                null,		cSmall,		null);
        add(EnumAmmo.M357_AP, 8,
                null,	wSteel,				null,
                null,	smokeless.copy(2),	null,
                null,	sSmall,				null);
        add(EnumAmmo.M357_EXPRESS, 8,
                null,	steel,				null,
                null,	smokeless.copy(3),	null,
                null,	cSmall,				null);

        add(EnumAmmo.M44_BP, 12,
                null,	lead.copy(2),		null,
                null,	smokeful,	null,
                null,	cSmall,		null);
        add(EnumAmmo.M44_SP, 6,
                null,	lead,		null,
                null,	smokeless,	null,
                null,	cSmall,		null);
        add(EnumAmmo.M44_FMJ, 6,
                null,	steel,		null,
                null,	smokeless,	null,
                null,	cSmall,		null);
        add(EnumAmmo.M44_JHP, 6,
                plastic,	copper,		null,
                null,		smokeless,	null,
                null,		cSmall,		null);
        add(EnumAmmo.M44_AP, 6,
                null,	wSteel,				null,
                null,	smokeless.copy(2),	null,
                null,	sSmall,				null);
        add(EnumAmmo.M44_EXPRESS, 6,
                null,	steel,				null,
                null,	smokeless.copy(3),	null,
                null,	cSmall,				null);

        add(EnumAmmo.P22_SP, 24,
                null,	lead,		null,
                null,	smokeless,	null,
                null,	cSmall,		null);
        add(EnumAmmo.P22_FMJ, 24,
                null,	steel,		null,
                null,	smokeless,	null,
                null,	cSmall,		null);
        add(EnumAmmo.P22_JHP, 24,
                plastic,	copper,		null,
                null,		smokeless,	null,
                null,		cSmall,		null);
        add(EnumAmmo.P22_AP, 24,
                null,	wSteel,				null,
                null,	smokeless.copy(2),	null,
                null,	sSmall,				null);

        add(EnumAmmo.P9_SP, 12,
                null,	lead,		null,
                null,	smokeless,	null,
                null,	cSmall,		null);
        add(EnumAmmo.P9_FMJ, 12,
                null,	steel,		null,
                null,	smokeless,	null,
                null,	cSmall,		null);
        add(EnumAmmo.P9_JHP, 12,
                plastic,	copper,		null,
                null,		smokeless,	null,
                null,		cSmall,		null);
        add(EnumAmmo.P9_AP, 12,
                null,	wSteel,				null,
                null,	smokeless.copy(2),	null,
                null,	sSmall,				null);

        add(EnumAmmo.P45_SP, 8,
                null,	lead,		null,
                null,	smokeless,	null,
                null,	cSmall,		null);
        add(EnumAmmo.P45_FMJ, 8,
                null,	steel,		null,
                null,	smokeless,	null,
                null,	cSmall,		null);
        add(EnumAmmo.P45_JHP, 8,
                plastic,	copper,		null,
                null,		smokeless,	null,
                null,		cSmall,		null);
        add(EnumAmmo.P45_AP, 8,
                null,	wSteel,				null,
                null,	smokeless.copy(2),	null,
                null,	sSmall,				null);
        add(EnumAmmo.P45_DU, 8,
                null,	uranium,			null,
                null,	smokeless.copy(2),	null,
                null,	sSmall,				null);

        add(EnumAmmo.R556_SP, 16,
                null,	lead.copy(2),		null,
                null,	smokeless.copy(2),	null,
                null,	cSmall.copy(2),		null);
        add(EnumAmmo.R556_FMJ, 16,
                null,	steel.copy(2),		null,
                null,	smokeless.copy(2),	null,
                null,	cSmall.copy(2),		null);
        add(EnumAmmo.R556_JHP, 16,
                plastic,	copper.copy(2),		null,
                null,		smokeless.copy(2),	null,
                null,		cSmall.copy(2),		null);
        add(EnumAmmo.R556_AP, 16,
                null,	wSteel.copy(2),		null,
                null,	smokeless.copy(4),	null,
                null,	sSmall.copy(2),		null);

        add(EnumAmmo.R762_SP, 12,
                null,	lead.copy(2),		null,
                null,	smokeless.copy(2),	null,
                null,	cSmall.copy(2),		null);
        add(EnumAmmo.R762_FMJ, 12,
                null,	steel.copy(2),		null,
                null,	smokeless.copy(2),	null,
                null,	cSmall.copy(2),		null);
        add(EnumAmmo.R762_JHP, 12,
                plastic,	copper.copy(2),		null,
                null,		smokeless.copy(2),	null,
                null,		cSmall.copy(2),		null);
        add(EnumAmmo.R762_AP, 12,
                null,	wSteel.copy(2),		null,
                null,	smokeless.copy(4),	null,
                null,	sSmall.copy(2),		null);
        add(EnumAmmo.R762_DU, 12,
                null,	uranium.copy(2),	null,
                null,	smokeless.copy(4),	null,
                null,	sSmall.copy(2),		null);
        add(EnumAmmo.R762_HE, 12,
                he,		ferro,				null,
                null,	smokeless.copy(4),	null,
                null,	sSmall.copy(2),		null);

        add(EnumAmmo.BMG50_SP, 12,
                null,	lead.copy(2),		null,
                null,	smokeless.copy(3),	null,
                null,	cBig,				null);
        add(EnumAmmo.BMG50_FMJ, 12,
                null,	steel.copy(2),		null,
                null,	smokeless.copy(3),	null,
                null,	cBig,				null);
        add(EnumAmmo.BMG50_JHP, 12,
                plastic,	copper.copy(2),		null,
                null,		smokeless.copy(3),	null,
                null,		cBig,				null);
        add(EnumAmmo.BMG50_AP, 12,
                null,	wSteel.copy(2),		null,
                null,	smokeless.copy(6),	null,
                null,	sBig,				null);
        add(EnumAmmo.BMG50_DU, 12,
                null,	uranium.copy(2),	null,
                null,	smokeless.copy(6),	null,
                null,	sBig,				null);
        add(EnumAmmo.BMG50_HE, 12,
                he,		ferro,				null,
                null,	smokeless.copy(6),	null,
                null,	sBig,				null);

        add(EnumAmmo.G12_BP, 6,
                null,	nugget.copy(6), null,
                null,	smokeful,		null,
                null,	bpShell,		null);
        add(EnumAmmo.G12_BP_MAGNUM, 6,
                null,	nugget.copy(8), null,
                null,	smokeful,		null,
                null,	bpShell,		null);
        add(EnumAmmo.G12_BP_SLUG, 6,
                null,	lead, 		null,
                null,	smokeful,	null,
                null,	bpShell,	null);

        add(EnumAmmo.G12, 6,
                null,	nugget.copy(6),	null,
                null,	smokeless,		null,
                null,	pShell,			null);
        add(EnumAmmo.G12_SLUG, 6,
                null,	lead, 		null,
                null,	smokeless,	null,
                null,	pShell,		null);
        add(EnumAmmo.G12_FLECHETTE, 6,
                null,	flechette.copy(12),	null,
                null,	smokeless,			null,
                null,	pShell,				null);
        add(EnumAmmo.G12_MAGNUM, 6,
                null,	nugget.copy(8),		null,
                null,	smokeless,			null,
                null,	sShell,				null);
        add(EnumAmmo.G12_EXPLOSIVE, 6,
                null,	he,			null,
                null,	smokeless,	null,
                null,	sShell,		null);
        add(EnumAmmo.G12_PHOSPHORUS, 6,
                null,	wp,			null,
                null,	smokeless,	null,
                null,	sShell,		null);

        add(EnumAmmo.G10, 4,
                null,	nugget.copy(8),		null,
                null,	smokeless.copy(2),	null,
                null,	sShell,				null);
        add(EnumAmmo.G10_SHRAPNEL, 4,
                plastic,	nugget.copy(8),		null,
                null,		smokeless.copy(2),	null,
                null,		sShell,				null);
        add(EnumAmmo.G10_DU, 4,
                null,	uranium,			null,
                null,	smokeless.copy(2),	null,
                null,	sShell,				null);
        add(EnumAmmo.G10_SLUG, 4,
                null,	lead,				null,
                null,	smokeless.copy(2),	null,
                null,	sShell,				null);
        add(EnumAmmo.G10_EXPLOSIVE, 4,
                he,		ferro,				null,
                null,	smokeless.copy(2),	null,
                null,	sShell,				null);

        add(EnumAmmo.G26_FLARE, 4,
                null,	rp,			null,
                null,	smokeless,	null,
                null,	cBig,		null);

        S dyn = s(ModItems.BALL_DYNAMITE.get());
        S coplate = s(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE));
        S diesel = s(FluidContainerIngredient.of(ModFluids.DIESEL.getSource(), 1_000));
        add(EnumAmmo.G40_HE, 4,
                null,	dyn,		null,
                null,	smokeless,	null,
                null,	cBig,		null);
        add(EnumAmmo.G40_HEAT, 4,
                coplate,	he,			null,
                null,		smokeless,	null,
                null,		cBig,		null);
        add(EnumAmmo.G40_DEMO, 4,
                null,	he.copy(2),	null,
                null,	smokeless,	null,
                null,	cBig,		null);
        add(EnumAmmo.G40_INC, 4,
                diesel,	dyn,	null,
                null,	smokeless,	null,
                null,	cBig,		null);
        add(EnumAmmo.G40_PHOSPHORUS, 4,
                wp,		he,			null,
                null,	smokeless,	null,
                null,	cBig,		null);

        add(EnumAmmo.ROCKET_HE, 2,
                null,	dyn,				null,
                null,	cBig,				null,
                null,	smokeless.copy(3),	null);
        add(EnumAmmo.ROCKET_HE, 2,
                null,	dyn,	null,
                null,	cBig,	null,
                null,	rocket,	null);
        add(EnumAmmo.ROCKET_HEAT, 2,
                coplate,	he,					null,
                null,		cBig,				null,
                null,		smokeless.copy(3),	null);
        add(EnumAmmo.ROCKET_HEAT, 2,
                coplate,	he,		null,
                null,		cBig,	null,
                null,		rocket,	null);
        add(EnumAmmo.ROCKET_DEMO, 2,
                null,	he.copy(2),			null,
                null,	cBig,				null,
                null,	smokeless.copy(3),	null);
        add(EnumAmmo.ROCKET_DEMO, 2,
                null,	he.copy(2),	null,
                null,	cBig,		null,
                null,	rocket,		null);
        add(EnumAmmo.ROCKET_INC, 2,
                diesel,	dyn,				null,
                null,	cBig,				null,
                null,	smokeless.copy(3),	null);
        add(EnumAmmo.ROCKET_INC, 2,
                diesel,	dyn,	null,
                null,	cBig,	null,
                null,	rocket,	null);
        add(EnumAmmo.ROCKET_PHOSPHORUS, 2,
                wp,		he,					null,
                null,	cBig,				null,
                null,	smokeless.copy(3),	null);
        add(EnumAmmo.ROCKET_PHOSPHORUS, 2,
                wp,		he,		null,
                null,	cBig,	null,
                null,	rocket,	null);

        S sPlate = s(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE));
        S napalm = s(ModItems.CANISTER_NAPALM.get());
        S gas = s(FluidContainerIngredient.of(ModFluids.GAS.getSource(), 1000));
        S bf = s(FluidContainerIngredient.of(ModFluids.BALEFIRE.getSource(), 1000));
        add(EnumAmmo.FLAME_DIESEL, 1,
                null,	sPlate,	null,
                null,	diesel,	null,
                null,	sPlate,	null);
        add(EnumAmmo.FLAME_NAPALM, 1,
                null,	sPlate,	null,
                null,	napalm,	null,
                null,	sPlate,	null);
        add(EnumAmmo.FLAME_GAS, 1,
                null,	sPlate,	null,
                null,	gas,	null,
                null,	sPlate,	null);
        add(EnumAmmo.FLAME_BALEFIRE, 1,
                null,	sPlate,	null,
                null,	bf,		null,
                null,	sPlate,	null);

        S silicon = s(ModMaterialItems.item(ModMaterials.SILICON, MaterialShape.BILLET));
        add(EnumAmmo.CAPACITOR, 4,
                null,	plastic,			null,
                null,	silicon.copy(4),	null,
                null,	plastic,			null);
        add(EnumAmmo.CAPACITOR_OVERCHARGE, 4,
                null,	plastic,			null,
                null,	silicon.copy(6),	null,
                null,	plastic,			null);
        add(EnumAmmo.CAPACITOR_IR, 4,
                null,	plastic,	null,
                null,	nb,			null,
                null,	plastic,	null);

        S lPlate = s(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.PLATE));
        add(EnumAmmo.TAU_URANIUM, 16,
                null,	lPlate,		null,
                null,	uranium,	null,
                null,	lPlate	,	null);

        S tungsten = s(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.INGOT));
        add(EnumAmmo.COIL_TUNGSTEN, 4,
                null,	null,		null,
                null,	tungsten,	null,
                null,	null	,	null);
        add(EnumAmmo.COIL_FERROURANIUM, 4,
                null,	null,		null,
                null,	ferro,		null,
                null,	null	,	null);

        S shell = s(ModItems.ASSEMBLY_NUKE.get());
        S tatb = s(ModItems.BALL_TATB.get());
        S plutonium = s(ModMaterialItems.item(ModMaterials.PLUTONIUM239, MaterialShape.NUGGET));
        add(EnumAmmo.NUKE_STANDARD, 1,
                null,	plutonium,	null,
                null,	shell,		null,
                null,	null	,	null);
        add(EnumAmmo.NUKE_DEMO, 1,
                null,	plutonium.copy(2),	null,
                null,	shell,				null,
                null,	null	,			null);
        add(EnumAmmo.NUKE_HIGH, 1,
                null,	plutonium.copy(4),	null,
                null,	shell,				null,
                null,	null	,			null);
        add(EnumAmmo.NUKE_TOTS, 1,
                null,	plutonium.copy(2),	null,
                null,	tatb.copy(2),		null,
                null,	sPlate.copy(4)	,	null);
        add(EnumAmmo.NUKE_HIVE, 1,
                null,	he.copy(8),			null,
                null,	sBig.copy(2),		null,
                null,	sPlate.copy(4),		null);
        add(EnumAmmo.NUKE_BALEFIRE, 1,
                null,	s(ModItems.EGG_BALEFIRE_SHARD.get()),	null,
                null,	shell,									null,
                null,	null	,								null);

        add(EnumAmmo.CT_HOOK, 16,
                null,	steel,		null,
                null,	pipe,		null,
                null,	smokeless,	null);
        add(EnumAmmo.CT_MORTAR, 4,
                null,	he.copy(4),	null,
                null,	pipe,		null,
                null,	smokeless,	null);
    }
}
