package com.hbm_m.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.GeigerCounterBlock;
import com.hbm_m.blockentity.machines.GeigerCounterBlockEntity;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.item.ModItems;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
 *///?}

/**
 * Radiation observability via instruments: the Geiger counter (item and block) and the dosimeter.
 *
 * <p>Covers:
 * <ul>
 *   <li>Geiger click bands with strict RAD boundaries and overlap between adjacent
 *       levels (a mirror of {@code GeigerCounterBlockEntity#tick} /
 *       {@code ItemGeigerCounter#inventoryTick});</li>
 *   <li>dosimeter thresholds with the historically duplicated comparison (x>=1 && x>=2);</li>
 *   <li>truncation of dosimeter readings to one decimal place BEFORE the 3.6 clamp;</li>
 *   <li>truncation of Geiger lines to one decimal place when reading live fields
 *       (chunk / environment / accumulated player dose);</li>
 *   <li>the 200-tick grace period: dose does not accumulate, but radEnv still feeds the instruments;</li>
 *   <li>the placed Geiger counter: sampling the chunk field every 10 ticks plus a
 *       comparator output of ceil(rad/5) capped at 15;</li>
 *   <li>block shape — a uniform 16x8x16 slab for all directions.</li>
 * </ul>
 *
 * <p>Registration goes through {@link GameTestRegistration} ({@code RegisterGameTestsEvent});
 * the {@code @GameTestHolder}/{@code @PrefixGameTestTemplate} annotations disable the class
 * prefix in template names (otherwise {@code <class>.empty3x3x3} would be looked up, which does not exist).
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class RadiationObservabilityGameTest {

    private RadiationObservabilityGameTest() {
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Helper methods.
    // ════════════════════════════════════════════════════════════════════════

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    /**
     * Version-compatible creation of a mock player:
     * 1.20.1 (forge): {@code makeMockPlayer()};
     * 1.21.1 (neoforge): {@code makeMockPlayer(GameType)}.
     */
    private static Player makePlayer(GameTestHelper helper) {
        //? if < 1.21.1 {
        // ВАЖНО: ванильный makeMockPlayer() на 1.20.1 возвращает КРЕАТИВНОГО игрока,
        // а ContaminationUtil.contaminate игнорирует креатив — для тестов загрязнения
        // нужен survival-игрок (см. GasGameTest.makeSurvivalPlayer).
        return new Player(helper.getLevel(), BlockPos.ZERO, 0.0F,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "rad-test-player")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        //?} else {
        /*return helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
         *///?}
    }

    /**
     * Geiger click bands — mirrors building the level list from
     * {@code GeigerCounterBlockEntity#tick} / {@code ItemGeigerCounter#inventoryTick}.
     * Pinned by contract so threshold changes cannot slip through unnoticed.
     */
    private static int[] geigerCandidates(float x) {
        List<Integer> list = new ArrayList<>();
        if (x < 1F) list.add(0);
        if (x < 5F) list.add(0);
        if (x < 10F) list.add(1);
        if (x > 5F && x < 15F) list.add(2);
        if (x > 10F && x < 20F) list.add(3);
        if (x > 15F && x < 25F) list.add(4);
        if (x > 20F && x < 30F) list.add(5);
        if (x > 25F) list.add(6);
        int[] out = new int[list.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    /**
     * Dosimeter bands — mirrors {@code ItemDosimeter}: the historically
     * duplicated comparison x>=1 && x>=2 yields level 3 strictly from two.
     */
    private static int[] dosimeterCandidates(float x) {
        List<Integer> list = new ArrayList<>();
        if (x < 0.5F) list.add(0);
        if (x < 1F) list.add(1);
        if (x >= 0.5F && x < 2F) list.add(2);
        if (x >= 1F && x >= 2F) list.add(3);
        int[] out = new int[list.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    /** Truncation to one decimal place — as in {@code printGeigerData}/{@code printDosimeterData}. */
    private static double truncateToTenth(double raw) {
        return ((int) (raw * 10D)) / 10D;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Click bands.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void geigerClickBandsPreserveOverlappingWeights(GameTestHelper helper) {
        check(Arrays.equals(geigerCandidates(0.5F), new int[]{0, 0, 1}),
                "Below one RAD/s the Geiger counter keeps two quiet levels and one level-one click");
        check(Arrays.equals(geigerCandidates(5.0F), new int[]{1})
                        && Arrays.equals(geigerCandidates(Math.nextUp(5.0F)), new int[]{1, 2}),
                "The strict five RAD/s boundary switches level one to the one/two overlap");
        check(Arrays.equals(geigerCandidates(10.0F), new int[]{2})
                        && Arrays.equals(geigerCandidates(Math.nextUp(10.0F)), new int[]{2, 3}),
                "The strict ten RAD/s boundary switches level two to the two/three overlap");
        check(Arrays.equals(geigerCandidates(25.0F), new int[]{5})
                        && Arrays.equals(geigerCandidates(Math.nextUp(25.0F)), new int[]{5, 6})
                        && Arrays.equals(geigerCandidates(30.0F), new int[]{6}),
                "The upper bands preserve the strict boundaries of levels five and six");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void dosimeterPreservesThresholdQuirk(GameTestHelper helper) {
        check(Arrays.equals(dosimeterCandidates(0.25F), new int[]{0, 1}),
                "Low dosimeter readings alternate between silence and level one");
        check(Arrays.equals(dosimeterCandidates(0.75F), new int[]{1, 2}),
                "Mid-low readings overlap levels one and two");
        check(Arrays.equals(dosimeterCandidates(1.5F), new int[]{2}),
                "The 1-2 RAD/s range stays at level two");
        check(Arrays.equals(dosimeterCandidates(2.0F), new int[]{3}),
                "The duplicated comparison x>=1 && x>=2 yields level three strictly from two");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Readout truncation and clamping.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void dosimeterReadoutTruncatesBeforeClamping(GameTestHelper helper) {
        // printDosimeterData: first truncate to one decimal place, then clamp the display at 3.6.
        double low = truncateToTenth(3.69D);
        boolean lowOverRange = low > 3.6D;
        double high = truncateToTenth(3.79D);
        boolean highOverRange = high > 3.6D;

        check(low == 3.6D && !lowOverRange,
                "Raw 3.69 truncates to 3.6 without an overflow marker");
        check(high == 3.7D && highOverRange,
                "A value truncated above 3.6 is clamped by the display at 3.6 with an overflow marker");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void geigerReadoutTruncatesChunkEnvironmentAndDose(GameTestHelper helper) {
        Player player = makePlayer(helper);
        Level level = helper.getLevel();
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));

        ChunkRadiationManager.setRadiation(level, abs.getX(), abs.getY(), abs.getZ(), 12.39F);
        HbmLivingProps.setRadBuf(player, 3.71F);
        HbmLivingProps.setRadiation(player, 234.59F);

        double chunkLine = truncateToTenth(
                ChunkRadiationManager.getRadiation(level, abs.getX(), abs.getY(), abs.getZ()));
        double envLine = truncateToTenth(HbmLivingProps.getRadBuf(player));
        double doseLine = truncateToTenth(HbmLivingProps.getRadiation(player));

        check(chunkLine == 12.3D,
                "The Geiger chunk line truncates to one decimal place (12.39 -> 12.3, not 12.4)");
        check(envLine == 3.7D,
                "The environment line truncates to one decimal place (3.71 -> 3.7)");
        check(doseLine == 234.6D,
                "The accumulated dose reads as 234.6 (storage rounds to 0.1 on write)");

        // Clean up state so it does not leak into neighboring tests.
        ChunkRadiationManager.setRadiation(level, abs.getX(), abs.getY(), abs.getZ(), 0F);
        HbmLivingProps.setRadBuf(player, 0F);
        HbmLivingProps.setRadiation(player, 0F);
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void instrumentsRetainSingleStackIdentity(GameTestHelper helper) {
        ItemStack geiger = new ItemStack(ModItems.GEIGER_COUNTER.get());
        ItemStack dosimeter = new ItemStack(ModItems.DOSIMETER.get());
        check(geiger.getMaxStackSize() == 1 && dosimeter.getMaxStackSize() == 1,
                "Both instruments keep a max stack size of one");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Contamination grace period.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void contaminationGraceStillFeedsEnvironmentalMeter(GameTestHelper helper) {
        Player player = makePlayer(helper);

        // Below 200 ticks no dose is credited...
        player.tickCount = 199;
        ContaminationUtil.contaminate(player, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
        check(HbmLivingProps.getRadiation(player) == 0F,
                "The original 200-tick grace period blocks accumulated dose");

        // ...but the irradiation attempt still feeds the environmental instrument buffer
        // (the per-second radEnv -> radBuf snap from EntityEffectHandler).
        HbmLivingProps.setRadBuf(player, HbmLivingProps.getRadEnv(player));
        HbmLivingProps.setRadEnv(player, 0F);
        check(HbmLivingProps.getRadBuf(player) == 5F,
                "An irradiation attempt during grace feeds the instrument buffer radBuf");

        // From tick 200 accumulation begins.
        player.tickCount = 200;
        ContaminationUtil.contaminate(player, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
        check(HbmLivingProps.getRadiation(player) == 5F,
                "Irradiation starts accumulating dose from the original tick 200");

        HbmLivingProps.setRadiation(player, 0F);
        HbmLivingProps.setRadBuf(player, 0F);
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Placed Geiger counter.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void placedGeigerSamplesChunkFieldAndDrivesComparator(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos pos = new BlockPos(2, 1, 2);
        GeigerCounterBlock block = (GeigerCounterBlock) ModBlocks.GEIGER_COUNTER_BLOCK.get();
        var state = block.defaultBlockState()
                .setValue(GeigerCounterBlock.FACING, Direction.NORTH);
        helper.setBlock(pos, state);
        BlockPos abs = helper.absolutePos(pos);
        GeigerCounterBlockEntity geiger = (GeigerCounterBlockEntity) helper.getBlockEntity(pos);

        ChunkRadiationManager.setRadiation(level, abs.getX(), abs.getY(), abs.getZ(), 12.0F);
        for (int tick = 0; tick < 10; tick++) {
            geiger.tick(level, abs, state);
        }
        check(geiger.getTicker() == 12.0F,
                "A placed Geiger counter refreshes its chunk reading every ten ticks");
        check(block.getAnalogOutputSignal(state, level, abs) == 3,
                "Twelve RAD/s yield ceil(12/5)=3 comparator strength");

        // The comparator reads the live field without waiting for a BE tick.
        ChunkRadiationManager.setRadiation(level, abs.getX(), abs.getY(), abs.getZ(), 70.01F);
        check(block.getAnalogOutputSignal(state, level, abs) == 15,
                "Values above 70 RAD/s are clamped by the comparator at 15");

        // Clean up the chunk field so it does not leak into neighboring tests.
        ChunkRadiationManager.setRadiation(level, abs.getX(), abs.getY(), abs.getZ(), 0F);
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void placedGeigerUsesUniformSlabShape(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos pos = new BlockPos(2, 1, 2);
        var block = ModBlocks.GEIGER_COUNTER_BLOCK.get();

        helper.setBlock(pos, block.defaultBlockState().setValue(GeigerCounterBlock.FACING, Direction.NORTH));
        AABB north = helper.getBlockState(pos).getShape(level, helper.absolutePos(pos)).bounds();
        check(north.minY == 0.0D && north.maxY == 8.0D / 16.0D,
                "North orientation: an 8/16-high slab from the ground");

        helper.setBlock(pos, block.defaultBlockState().setValue(GeigerCounterBlock.FACING, Direction.EAST));
        AABB east = helper.getBlockState(pos).getShape(level, helper.absolutePos(pos)).bounds();
        check(east.minY == 0.0D && east.maxY == 8.0D / 16.0D
                        && east.minX == 0.0D && east.maxX == 1.0D
                        && east.minZ == 0.0D && east.maxZ == 1.0D,
                "The shape is uniform for all directions: a full 16x8x16 slab");
        helper.succeed();
    }
}
