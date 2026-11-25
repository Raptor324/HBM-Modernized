package com.hbm_m.test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.hbm_m.api.space.BedrockOreHooks;
import com.hbm_m.api.space.ForkRules;
import com.hbm_m.api.space.Lpw2Hooks;
import com.hbm_m.api.space.NuclearDetonation;
import com.hbm_m.api.space.SatelliteHooks;
import com.hbm_m.api.space.SpaceApi;
import com.hbm_m.api.space.SpaceEnvironment;
import com.hbm_m.api.space.SpaceResources;
import com.hbm_m.api.space.WandDHook;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineSteamCondenserBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.satellite.SpaceAssemblerRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Active Space-API integration tests simulating Space-Modernized hooks on top of HBM-Modernized.
 * Every test resets {@link SpaceApi#resetToEarth()} in {@code finally} so Earth defaults are preserved.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class SpaceIntegrationGameTest {

    private static final ResourceLocation TEST_BODY_DUNA =
            ResourceLocation.fromNamespaceAndPath("hbm_m", "test_duna");
    private static final ResourceLocation TEST_ORE_IRON =
            ResourceLocation.fromNamespaceAndPath("hbm_m", "test_duna_iron");
    private static final ResourceLocation TEST_FLUID_PRIMARY =
            ResourceLocation.fromNamespaceAndPath("hbm_m", "oil_base");
    private static final ResourceLocation TEST_FLUID_SECONDARY =
            ResourceLocation.fromNamespaceAndPath("hbm_m", "gas");

    private SpaceIntegrationGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "space_api", timeoutTicks = 100)
    public static void activeEnvironment_overridesAndVacuumCondenser(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        AtomicInteger gasEmitted = new AtomicInteger();

        SpaceEnvironment vacuumSpace = new SpaceEnvironment() {
            @Override
            public float gravity(Level l, BlockPos p) {
                return 0.0F;
            }

            @Override
            public boolean inOrbit(Level l) {
                return true;
            }

            @Override
            public double pressure(Level l, BlockPos p) {
                return 0.0D;
            }

            @Override
            public double bodyPressure(Level l) {
                return 0.0D;
            }

            @Override
            public boolean consumeOxygen(Level l, BlockPos p, int amount) {
                return false;
            }

            @Override
            public boolean drawAtmosphere(Level l, BlockPos p, int mb) {
                return false;
            }

            @Override
            public void emitGas(Level l, BlockPos p, ResourceLocation fluid, int mb) {
                gasEmitted.addAndGet(mb);
            }

            @Override
            public float ambientRadiation(Level l, BlockPos p) {
                return 2.5F;
            }

            @Override
            public boolean supportsPlantGrowth(Level l, BlockPos p) {
                return false;
            }

            @Override
            public int temperature(Level l, BlockPos p) {
                return -180;
            }

            @Override
            public float solarIntensity(Level l, BlockPos p) {
                return 1.5F;
            }

            @Override
            public ResourceLocation body(Level l) {
                return TEST_BODY_DUNA;
            }

            @Override
            public boolean overridesSky(Level l) {
                return true;
            }

            @Override
            public ResourceLocation waterTableFluid(Level l) {
                return null;
            }
        };

        try {
            SpaceApi.setEnvironment(vacuumSpace);
            check(SpaceApi.isInstalled(), "SpaceApi should report installed");
            check(SpaceApi.inOrbit(level), "inOrbit should be true");
            check(SpaceApi.bodyPressure(level) == 0.0D, "bodyPressure should be 0.0");
            check(!SpaceApi.breatheAir(level, pos, 5), "breatheAir should be false in vacuum");
            check(!SpaceApi.drawAtmosphere(level, pos, 10), "drawAtmosphere should be false in vacuum");
            check(SpaceApi.ambientRadiation(level, pos) == 2.5F, "ambientRadiation should be 2.5");
            check(SpaceApi.solarIntensity(level, pos) == 1.5F, "solarIntensity should be 1.5");
            check(SpaceApi.overridesSky(level), "overridesSky should be true");
            check(TEST_BODY_DUNA.equals(SpaceApi.body(level)), "body should match custom body");
            check(SpaceApi.waterTableFluid(level, ModFluids.WATER.getSource()) == null,
                    "waterTableFluid should be null in orbit");
            check(SpaceApi.condenserEvaporates(level, pos, false),
                    "condenserEvaporates should be true in vacuum when not vacuumOptimised");
            check(!SpaceApi.condenserEvaporates(level, pos, true),
                    "condenserEvaporates should be false when vacuumOptimised");
            check(SpaceApi.condenserNeedsAtmosphere(level, false),
                    "condenserNeedsAtmosphere should be true in vacuum");
            check(!SpaceApi.condenserNeedsAtmosphere(level, true),
                    "condenserNeedsAtmosphere should be false when vacuumOptimised");

            SpaceApi.emitGas(level, pos, SpaceEnvironment.HBM_AIR, 42);
            check(gasEmitted.get() == 42, "emitGas should forward mb to environment");

            // Place condenser in vacuum: steam evaporates instead of condensing to water
            BlockPos rel = new BlockPos(1, 1, 1);
            helper.setBlock(rel, ModBlocks.STEAM_CONDENSER.get());
            if (!(helper.getBlockEntity(rel) instanceof MachineSteamCondenserBlockEntity be)) {
                throw new GameTestAssertException("Expected MachineSteamCondenserBlockEntity");
            }
            be.getReceivingTanks()[0].fillMb(ModFluids.SPENTSTEAM.getSource(), 50);
            MachineSteamCondenserBlockEntity.tick(level, pos, level.getBlockState(pos), be);
            check(be.getAllTanks()[0].getFill() == 0, "Steam should be consumed/evaporated in vacuum");
            check(be.getAllTanks()[1].getFill() == 0, "Water should NOT condense in vacuum");
        } finally {
            SpaceApi.resetToEarth();
        }
        check(!SpaceApi.isInstalled(), "resetToEarth should restore uninstalled state");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_api", timeoutTicks = 100)
    public static void activeForkRules_overridesAppliedAndReset(GameTestHelper helper) {
        ItemStack fuelStack = new ItemStack(Items.COAL);
        ItemStack shredderStack = new ItemStack(Items.NETHER_STAR);
        ForkRules customFork = new ForkRules() {
            @Override
            public boolean active() {
                return true;
            }

            @Override
            public boolean unavoidableGravity() {
                return false;
            }

            @Override
            public int blastFurnaceFuel(ItemStack stack) {
                return stack.is(Items.COAL) ? 400 : 0;
            }

            @Override
            public com.hbm_m.inventory.material.NTMMaterial gunmetalZinc() {
                return com.hbm_m.inventory.material.Mats.MAT_IRON;
            }

            @Override
            public boolean shredderExplodes(ItemStack stack) {
                return stack.is(Items.NETHER_STAR);
            }

            @Override
            public Fluid teleporterFluid() {
                return Fluids.WATER;
            }
        };

        try {
            SpaceApi.setForkRules(customFork);
            check(SpaceApi.forkActive(), "forkActive should be true");
            check(!SpaceApi.fork().unavoidableGravity(), "unavoidableGravity should be overridden");
            check(SpaceApi.fork().blastFurnaceFuel(fuelStack) == 400, "blastFurnaceFuel should be overridden");
            check(SpaceApi.fork().gunmetalZinc() == com.hbm_m.inventory.material.Mats.MAT_IRON,
                    "gunmetalZinc should be overridden");
            check(SpaceApi.fork().shredderExplodes(shredderStack), "shredderExplodes should be overridden");
            check(SpaceApi.fork().teleporterFluid() == Fluids.WATER, "teleporterFluid should be overridden");
        } finally {
            SpaceApi.resetToEarth();
        }
        check(!SpaceApi.forkActive(), "resetToEarth should restore ForkRules.MASTER");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_api", timeoutTicks = 100)
    public static void activeAddonHooks_satelliteLpw2BedrockAndWandD(GameTestHelper helper) {
        AtomicInteger wandCalls = new AtomicInteger();
        AtomicInteger lpw2Ticks = new AtomicInteger();

        SatelliteHooks satHooks = new SatelliteHooks() {
            @Override
            public ServerLevel satelliteLevel(ServerLevel level, int x, int z) {
                return level;
            }
        };
        Lpw2Hooks lpw2Hooks = new Lpw2Hooks() {
            @Override
            public boolean active() {
                return true;
            }

            @Override
            public void serverTick(com.hbm_m.blockentity.machines.MachineLpw2BlockEntity be) {
                lpw2Ticks.incrementAndGet();
            }
        };
        BedrockOreHooks bedrockHooks = new BedrockOreHooks() {
            @Override
            public boolean setOreAmount(ServerLevel level, ItemStack stack, int x, int z, double mult) {
                com.hbm_m.platform.StackNbt.putInt(stack, TEST_ORE_IRON.toString(), (int) (100 * mult));
                return true;
            }
        };
        WandDHook wandHook = new WandDHook() {
            @Override
            public boolean active() {
                return true;
            }

            @Override
            public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, ItemStack stack) {
                wandCalls.incrementAndGet();
                return InteractionResultHolder.success(stack);
            }
        };

        try {
            SpaceApi.setSatelliteHooks(satHooks);
            SpaceApi.setLpw2Hooks(lpw2Hooks);
            SpaceApi.setBedrockOreHooks(bedrockHooks);
            SpaceApi.setWandDHook(wandHook);

            check(SpaceApi.satellites().satelliteLevel(helper.getLevel(), 0, 0) == helper.getLevel(),
                    "SatelliteHooks should return satellite level");
            check(SpaceApi.lpw2().active(), "Lpw2Hooks should be active");
            SpaceApi.lpw2().serverTick(null);
            check(lpw2Ticks.get() == 1, "Lpw2Hooks.serverTick should be invoked");

            ItemStack testStack = new ItemStack(Items.IRON_INGOT);
            check(SpaceApi.bedrockOres().setOreAmount(helper.getLevel(), testStack, 0, 0, 2.5D),
                    "BedrockOreHooks.setOreAmount should return true");
            check(com.hbm_m.platform.StackNbt.getInt(testStack, TEST_ORE_IRON.toString()) == 250,
                    "BedrockOreHooks should write ore amount");

            check(SpaceApi.wandD().active(), "WandDHook should be active");
            InteractionResultHolder<ItemStack> res =
                    SpaceApi.wandD().use(helper.getLevel(), null, InteractionHand.MAIN_HAND, testStack);
            check(res.getResult() == InteractionResult.SUCCESS, "WandDHook.use should return SUCCESS");
            check(wandCalls.get() == 1, "WandDHook.use should increment call count");
        } finally {
            SpaceApi.resetToEarth();
        }
        check(SpaceApi.satellites() == SatelliteHooks.DEFAULT, "SatelliteHooks should reset");
        check(SpaceApi.lpw2() == Lpw2Hooks.DEFAULT, "Lpw2Hooks should reset");
        check(SpaceApi.bedrockOres() == BedrockOreHooks.DEFAULT, "BedrockOreHooks should reset");
        check(SpaceApi.wandD() == WandDHook.DEFAULT, "WandDHook should reset");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_api", timeoutTicks = 100)
    public static void spaceResources_syntheticBodyRegistrations(GameTestHelper helper) {
        // Use synthetic test body & Purpur block so SpaceEnvironmentGameTest earth/stone assertions stay untouched
        SpaceResources.registerBedrockOre(TEST_BODY_DUNA, TEST_ORE_IRON, 100);
        List<SpaceResources.BedrockOre> ores = SpaceResources.bedrockOres(TEST_BODY_DUNA);
        check(!ores.isEmpty() && ores.stream().anyMatch(o -> o.oreType().equals(TEST_ORE_IRON) && o.weight() == 100),
                "SpaceResources.bedrockOres should return registered ore for synthetic body");

        SpaceResources.registerDepositFluids(
                TEST_BODY_DUNA, SpaceResources.ReserveType.OIL, TEST_FLUID_PRIMARY, TEST_FLUID_SECONDARY);
        check(TEST_FLUID_PRIMARY.equals(
                        SpaceResources.depositPrimaryFluid(TEST_BODY_DUNA, SpaceResources.ReserveType.OIL)),
                "depositPrimaryFluid should match registered primary fluid");
        check(TEST_FLUID_SECONDARY.equals(
                        SpaceResources.depositSecondaryFluid(TEST_BODY_DUNA, SpaceResources.ReserveType.OIL)),
                "depositSecondaryFluid should match registered secondary fluid");

        SpaceResources.registerPumpGround(() -> Blocks.PURPUR_BLOCK);
        check(SpaceResources.isExtraPumpGround(Blocks.PURPUR_BLOCK),
                "isExtraPumpGround should return true for registered block");

        SpaceResources.registerBrickFurnaceSpeed(() -> Items.PURPUR_BLOCK, 3);
        check(SpaceResources.brickFurnaceSpeed(Items.PURPUR_BLOCK) == 3,
                "brickFurnaceSpeed should return registered speed");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_api", timeoutTicks = 100)
    public static void spaceAssemblerRecipes_andNuclearDetonationKinds(GameTestHelper helper) {
        check(!SpaceAssemblerRecipes.all().isEmpty(), "SpaceAssemblerRecipes should not be empty");
        SpaceAssemblerRecipes.Recipe crystal = SpaceAssemblerRecipes.byName("space.crystalcircuit");
        check(crystal != null, "space.crystalcircuit recipe should exist");
        check(SpaceAssemblerRecipes.getRecipe(new ItemStack(ModItems.ORBITAL_ASSEMBLY_CRYSTAL_CIRCUIT.get())) == crystal,
                "getRecipe should match ORBITAL_ASSEMBLY_CRYSTAL_CIRCUIT");

        AtomicInteger mk5Count = new AtomicInteger();
        NuclearDetonation.Listener listener = d -> {
            if (d.kind() == NuclearDetonation.Kind.MK5 && d.blastRadius() == 120.0F) {
                mk5Count.incrementAndGet();
            }
        };
        SpaceApi.onNuclearDetonation(listener);
        try {
            BlockPos p = helper.absolutePos(new BlockPos(1, 1, 1));
            SpaceApi.fireNuclearDetonation(
                    helper.getLevel(), p.getX(), p.getY(), p.getZ(), 120.0F, NuclearDetonation.Kind.MK5);
        } finally {
            SpaceApi.removeNuclearDetonationListener(listener);
        }
        check(mk5Count.get() == 1, "MK5 detonation listener should fire once");
        helper.succeed();
    }
}
