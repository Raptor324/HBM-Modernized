package com.hbm_m.test;

import com.hbm_m.api.space.NuclearDetonation;
import com.hbm_m.api.space.SpaceApi;
import com.hbm_m.api.space.SpaceEnvironment;
import com.hbm_m.api.space.SpaceResources;
import com.hbm_m.blockentity.machines.PumpGroundCheck;
import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
 *///?}

/**
 * Space-API "Standardumgebung": ohne Space-Addon liefert {@link SpaceApi} ueberall die Erde, und die HBM-Aufrufstellen
 * verhalten sich wie vor der API (Sauerstoff immer da, Normalschwerkraft, kein Orbit, Wassertabelle = HBM-Wasser).
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class SpaceEnvironmentGameTest {

    private SpaceEnvironmentGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "space_api", timeoutTicks = 100)
    public static void standardEnvironmentIsEarth(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));

        check(!SpaceApi.isInstalled(), "kein Space-Addon in HBM-GameTests");
        check(SpaceApi.env() == SpaceApi.EARTH, "Standardumgebung = Erde");
        SpaceEnvironment env = SpaceApi.env();

        check(env.gravity(level, pos) == SpaceEnvironment.STANDARD_GRAVITY, "Schwerkraft = 1.6 (NTM-Einheit)");
        check(SpaceEnvironment.STANDARD_GRAVITY == 1.6F, "STANDARD_GRAVITY 1:1 AstronomyUtil");
        check(!env.inOrbit(level) && !SpaceApi.inOrbit(level), "kein Orbit");
        check(env.pressure(level, pos) == 1.0D, "Druck 1 atm");
        check(env.gasPressure(level, pos, SpaceEnvironment.HBM_AIR) == 1.0D, "Luft 1 atm");
        check(env.gasPressure(level, pos, SpaceEnvironment.HBM_WATER) == 0.0D, "kein anderes Gas");
        check(env.consumeOxygen(level, pos, 1000) && SpaceApi.breatheAir(level, pos, 5), "Sauerstoff immer vorhanden");
        check(env.temperature(level, pos) == 20, "Raumtemperatur 20 C");
        check(env.solarIntensity(level, pos) == 1.0F && SpaceApi.solarIntensity(level, pos) == 1.0F, "Sonnenleistung 1.0");
        check(SpaceEnvironment.EARTH.equals(env.body(level)) && SpaceEnvironment.EARTH.equals(env.targetBody(level, pos)), "Koerper = hbm_m:earth");
        check(!env.overridesSky(level) && !SpaceApi.overridesSky(level), "HBM-Himmel bleibt aktiv");
        check(SpaceEnvironment.HBM_WATER.equals(env.waterTableFluid(level)), "Wassertabelle = hbm_m:water");
        check(SpaceApi.waterTableFluid(level, ModFluids.WATER.getSource()) == ModFluids.WATER.getSource(), "Pumpentabelle = HBM-Wasser");
        check(SpaceApi.pressure(null, pos) == 1.0D && SpaceApi.breatheAir(null, pos, 1), "null-Level = Erde");
        // R2: Atmosphaere
        check(env.bodyPressure(level) == 1.0D && SpaceApi.bodyPressure(level) == 1.0D, "Koerperdruck 1 atm");
        check(env.drawAtmosphere(level, pos, 10) && SpaceApi.drawAtmosphere(level, pos, 0), "Ansaugstutzen bekommt immer Luft");
        check(env.ambientRadiation(level, pos) == 0.0F && SpaceApi.ambientRadiation(level, pos) == 0.0F, "keine Sonnenstrahlung");
        check(env.supportsPlantGrowth(level, pos), "Gras waechst");
        check(!SpaceApi.condenserEvaporates(level, pos, false) && !SpaceApi.condenserEvaporates(level, pos, true), "Kondensator verdampft nie");
        check(!SpaceApi.condenserNeedsAtmosphere(level, false), "keine Vakuum-Warnung");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_api", timeoutTicks = 100)
    public static void callSitesAreNeutral(GameTestHelper helper) {
        // Pumpen: ohne Addon keine Drosselung, keine Zusatzbloecke
        check(PumpGroundCheck.pumpSpeed(ModFluids.WATER.getSource(), 10_000) == 10_000, "Pumpe Wasser volle Rate");
        check(PumpGroundCheck.pumpSpeed(ModFluids.OIL_BASE.getSource(), 10_000) == 10_000, "ohne Addon keine nonWaterDebuff-Drosselung");
        check(!SpaceResources.isExtraPumpGround(Blocks.STONE), "keine zusaetzlichen Pumpenboeden");
        check(SpaceResources.bedrockOres(SpaceEnvironment.EARTH).isEmpty(), "keine Bedrock-Erze registriert");
        check(SpaceResources.depositPrimaryFluid(SpaceEnvironment.EARTH, SpaceResources.ReserveType.OIL) == null, "keine Lagerstaetten-Fluide");
        check(SpaceResources.brickFurnaceSpeed(net.minecraft.world.item.Items.COBBLESTONE) == 0, "keine Zusatz-Ziegelofenwerte");

        // Detonations-Listener: Server-Aufruf erreicht Listener, danach wieder abmelden
        final int[] calls = {0};
        NuclearDetonation.Listener l = d -> { if (d.kind() == NuclearDetonation.Kind.MINI_NUKE && d.blastRadius() == 15F) calls[0]++; };
        SpaceApi.onNuclearDetonation(l);
        try {
            BlockPos p = helper.absolutePos(new BlockPos(1, 1, 1));
            SpaceApi.fireNuclearDetonation(helper.getLevel(), p.getX(), p.getY(), p.getZ(), 15F, NuclearDetonation.Kind.MINI_NUKE);
        } finally {
            SpaceApi.removeNuclearDetonationListener(l);
        }
        check(calls[0] == 1, "Listener genau einmal aufgerufen");
        helper.succeed();
    }

    /** R2: Kondensator kondensiert auf der Erde weiter (kein Verdampfen ohne Addon). */
    @GameTest(template = "empty3x3x3", batch = "space_api", timeoutTicks = 100)
    public static void condenserStillCondenses(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, com.hbm_m.block.ModBlocks.STEAM_CONDENSER.get());
        if (!(helper.getBlockEntity(rel) instanceof com.hbm_m.blockentity.machines.MachineSteamCondenserBlockEntity be)) {
            throw new GameTestAssertException("kein Kondensator-BE");
        }
        be.getReceivingTanks()[0].fillMb(ModFluids.SPENTSTEAM.getSource(), 50);
        helper.succeedWhen(() -> {
            check(be.getAllTanks()[0].getFill() == 0, "Dampf verbraucht");
            check(be.getAllTanks()[1].getFill() == 50, "50 mB Wasser kondensiert");
        });
    }

    /** Wunschrunde W-R4A/W-R4b/W-GR: neue Erweiterungspunkte aendern ohne Addon nichts an HBM-Werten. */
    @GameTest(template = "empty3x3x3", batch = "space_api", timeoutTicks = 100)
    public static void extensionPointsAreNeutral(GameTestHelper helper) {
        // W-R4A-1/2: Behaelter-Tabelle, Fluid-Vorbelegung gelaufen
        check(com.hbm_m.inventory.fluid.FluidContainerDefs.getCanister(ModFluids.OIL_BASE.getSource()) != null
                || com.hbm_m.inventory.fluid.FluidContainerDefs.getCanister(ModFluids.KEROSENE.getSource()) != null, "HBM-Kanister vorhanden");
        check(com.hbm_m.api.space.HbmFluidApi.fluidDefaultsApplied(), "HBM-Fluid-Vorbelegung gelaufen");
        final int[] ran = {0};
        com.hbm_m.api.space.HbmFluidApi.afterHbmFluidDefaults(() -> ran[0]++);
        check(ran[0] == 1, "spaete Anmeldung laeuft sofort");
        // W-R4b-1: Schmelzwert eines Vanilla-Barrens unveraendert 72
        var mats = com.hbm_m.inventory.material.Mats.getMaterialsFromItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT));
        check(mats.size() == 1 && mats.get(0).material == com.hbm_m.inventory.material.Mats.MAT_IRON && mats.get(0).amount == 72, "Eisenbarren = 72 Quanten");
        // W-GR-1: kein Zusatzpegel ohne Anmeldung
        check(com.hbm_m.hazard.type.HazardTypeRadiation.extraLevel(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE)) == 0F, "kein Zusatz-Strahlungspegel");
        // W-R4b-4: Wellenlaengen-Stufen wie bisher (Ordinalzahl vor XRAY)
        for (var w : com.hbm_m.item.machine.ItemFELCrystal.EnumWavelengths.values()) {
            int expected = switch (w) { case NULL -> 0; case IR -> 1; case VISIBLE -> 2; case UV -> 3; case XRAY -> 4; case GAMMA -> 4; case DRX -> 5; };
            check(w.tier == expected, "FEL-Stufe " + w);
        }
        check(com.hbm_m.item.machine.ItemFELCrystal.EnumWavelengths.XRAY.maxRecipeTier == 3, "XRAY nur bis UV-Rezepte");
        // W-R4b-5: nur die 12 HBM-Watz-Typen
        check(com.hbm_m.item.nuclear.WatzPelletType.values().length == 12 && com.hbm_m.item.nuclear.WatzPelletType.all().size() == 12, "12 Watz-Typen");
        check(com.hbm_m.item.nuclear.WatzPelletType.NQR.depletedItem() != null, "verbrauchtes Pellet ueber Typ");
        // W-R6C: Satelliten-Haken = HBM-Standard
        check(SpaceApi.satellites() == com.hbm_m.api.space.SatelliteHooks.DEFAULT, "Standard-Satelliten-Haken");
        net.minecraft.server.level.ServerLevel sl = helper.getLevel();
        check(SpaceApi.satellites().satelliteLevel(sl, 0, 0) == sl, "Satelliten der eigenen Welt");
        check(com.hbm_m.satellite.SatelliteManager.get(sl, 0, 0) == com.hbm_m.satellite.SatelliteManager.get(sl), "get(level,x,z) = get(level)");
        // W-R6B-1: LPW-2 bleibt Deko (keine Tanks, Weltzeit-Animation)
        check(!SpaceApi.lpw2().active(), "LPW-2-Haken inaktiv");
        var lpw = new com.hbm_m.blockentity.machines.MachineLpw2BlockEntity(BlockPos.ZERO, com.hbm_m.block.ModBlocks.LPW2.get().defaultBlockState());
        check(lpw.tanks.length == 0 && lpw.animationTime(100L, 0.5F) == 100.5D, "LPW-2 ohne Tanks, Weltzeit");
        // W-R7A-1: Phantomteile sind ohne Freigabe des Kerns keine Waermequelle
        var part = new com.hbm_m.blockentity.machines.UniversalMachinePartBlockEntity(BlockPos.ZERO, com.hbm_m.block.ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        check(part.getHeatStored() == 0 && part.getMaxHeatStored() == 0, "Phantomteil ohne Kern: 0 TU");
        // W-R8B-1/2: Bedrock-Erz-Haken = HBM-Standard, HBM-Oel ist keine Addon-Lagerstaette
        check(SpaceApi.bedrockOres() == com.hbm_m.api.space.BedrockOreHooks.DEFAULT, "Standard-Bedrock-Erz-Haken");
        net.minecraft.world.item.ItemStack ore = new net.minecraft.world.item.ItemStack(com.hbm_m.item.ModItems.BEDROCK_ORE_BASE.get());
        check(!SpaceApi.bedrockOres().setOreAmount(sl, ore, 0, 0, 1D) && SpaceApi.bedrockOres().oreAmounts(ore) == null
                && SpaceApi.bedrockOres().scan(sl, 0, 0) == null && SpaceApi.bedrockOres().bedrockAcid(sl) == null, "Bedrock-Erz: HBM zustaendig");
        check(com.hbm_m.api.space.SpaceResources.fluidDepositBlock(com.hbm_m.block.ModBlocks.ORE_OIL.get()) == null
                && !com.hbm_m.api.space.SpaceResources.isEmptyFluidDeposit(com.hbm_m.block.ModBlocks.ORE_OIL_EMPTY.get()), "ore_oil bleibt HBM-Lagerstaette");
        // W-R9A-5/6, W-R9B2-1: Angel-Haken leer, Stein-Tag ohne Vanilla-Bloecke, Zaun wie Vanilla
        check(SpaceApi.env().fishingOverride(sl, sl.random) == null, "kein Koerper-Fang");
        check(!net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState().is(com.hbm_m.handler.ability.IToolAreaAbility.AREA_ABILITY_STONE), "Bruchstein nicht im Aderabbau-Stein-Tag");
        check(com.hbm_m.block.ModBlocks.FENCE_METAL.get() instanceof net.minecraft.world.level.block.FenceBlock fence
                && !fence.connectsTo(net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), false, net.minecraft.core.Direction.NORTH), "Maschendraht verbindet nicht mit Erde");
        // E1-E17: Fork-Regeln = Master
        check(SpaceApi.fork() == com.hbm_m.api.space.ForkRules.MASTER && !SpaceApi.forkActive(), "Fork-Regeln = Master");
        check(SpaceApi.fork().unavoidableGravity(), "E1 Big-Ass-Tank kippt immer");
        check(SpaceApi.fork().blastFurnaceFuel(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL)) == 0, "E4 kein Zusatzbrennstoff");
        check(SpaceApi.fork().gunmetalZinc() == null && SpaceApi.fork().teleporterFluid() == null, "E8/E16 kein Zink/keine N-Masse");
        check(!SpaceApi.fork().shredderExplodes(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE)), "E11 Schredder sprengt nicht");
        check(com.hbm_m.entity.mob.siege.SiegeTier.DNT.drops() == com.hbm_m.entity.mob.siege.SiegeTier.DNT.dropItem, "E5 Master-Beute");
        check(com.hbm_m.item.nuclear.WatzPelletType.NQR.yield == 500_000_000D, "E6 Watz-Ertrag 500 Mio");
        check(com.hbm_m.blockentity.machines.DeuteriumExtractorBlockEntity.powerDivisor() == 20, "E8 Deuterium /20");
        var part2 = new com.hbm_m.blockentity.network.CranePartitionerBlockEntity(BlockPos.ZERO, com.hbm_m.block.ModBlocks.CRANE_PARTITIONER.get().defaultBlockState());
        check(part2.inputCount == 45 && part2.outputCount == 45 && part2.inventory.getSlots() == 90, "E8 Sortierer 45+45");
        var tele = new com.hbm_m.blockentity.machines.MachineTeleporterBlockEntity(BlockPos.ZERO, com.hbm_m.block.ModBlocks.MACHINE_TELEPORTER.get().defaultBlockState());
        check(tele.tank == null && tele.getAllTanks().length == 0, "E16 Teleporter ohne Tank");
        var barrel = new com.hbm_m.blockentity.machines.BarrelIronBlockEntity(BlockPos.ZERO, com.hbm_m.block.ModBlocks.BARREL_IRON.get().defaultBlockState());
        check(barrel.isBarrel() && !barrel.handlesOverpressure(), "E3 Fass nimmt nicht am Ueberdruck teil");
        var ocelot = net.minecraft.world.entity.EntityType.OCELOT.create(sl);
        check(ocelot != null && com.hbm_m.util.ContaminationUtil.isRadImmune(ocelot), "E15 Ozelot immun");
        check(com.hbm_m.handler.rbmk.RBMKOutgasserRecipes.getRecipe(com.hbm_m.item.material.ModMaterialItems.stack(com.hbm_m.item.material.ModMaterials.COBALT, com.hbm_m.item.material.MaterialShape.INGOT, 1)) == null, "E8 kein Kobalt im Ausgaser");
        check(com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.RAZORTRAIN.ordinal() == 20, "E11 Sirenen-Ordnungszahlen unveraendert");
        // wand_d: Space-Haken inaktiv, HBM-Tooltip unveraendert
        check(SpaceApi.wandD() == com.hbm_m.api.space.WandDHook.DEFAULT && !SpaceApi.wandD().active(), "wand_d-Haken = HBM");
        var wandStack = new net.minecraft.world.item.ItemStack(com.hbm_m.item.ModItems.WAND_D.get());
        var wandTip = new java.util.ArrayList<net.minecraft.network.chat.Component>();
        ((com.hbm_m.item.tool.ItemWandD) wandStack.getItem()).appendHbmTooltip(wandStack, sl, wandTip, net.minecraft.world.item.TooltipFlag.Default.NORMAL);
        check(wandTip.size() == 1 && "Used for debugging purposes.".equals(wandTip.get(0).getString()), "wand_d-Tooltip = HBM");
        // W-R9B5-1/2: Datapack-Mischer/-Elektrolyse ohne Addon-JSONs = Code-Tabellen (HBM liefert keine solchen Rezepte)
        var rm = sl.getRecipeManager();
        check(com.hbm_m.platform.recipe.RecipeHooks.getAllRecipes(rm, com.hbm_m.recipe.MixerDataRecipe.Type.INSTANCE).isEmpty(), "keine hbm_m:mixer-JSONs");
        check(com.hbm_m.platform.recipe.RecipeHooks.getAllRecipes(rm, com.hbm_m.recipe.ElectrolysisFluidDataRecipe.Type.INSTANCE).isEmpty(), "keine hbm_m:electrolysis_fluid-JSONs");
        check(com.hbm_m.recipe.MixerRecipes.getRecipes(rm) == com.hbm_m.recipe.MixerRecipes.getRecipes(), "Mischer = Code-Tabelle");
        check(com.hbm_m.inventory.recipes.ElectrolyserFluidRecipes.all(rm) == com.hbm_m.inventory.recipes.ElectrolyserFluidRecipes.all(), "Elektrolyse = Code-Tabelle");
        var oxy = com.hbm_m.recipe.MixerRecipes.getOutput(sl, ModFluids.OXYHYDROGEN.getSource());
        check(oxy != null && oxy.length == 2, "Knallgas: Master 2 Rezepte (Luft/Sauerstoff)");
        helper.succeed();
    }
}
