package com.hbm_m.test;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.extprop.HbmLivingProps;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Blocks;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
 *///?}

/**
 * Behavior tests for the Demon Core Lamp ({@code lamp_demon}).
 *
 * <p>The lamp's server tick irradiates every living entity within 25 blocks
 * (dose = 100000 / blast-resistance-sum / distance²) and sets entities closer
 * than 2 blocks on fire (100 damage). These tests verify the three observable
 * behaviors headlessly: lethal burn at contact, quadratic dose at range, and
 * blast-resistance shielding of an obsidian wall. Each test gets its own batch
 * - gametest batches run sequentially, so the lamps of sibling tests cannot
 * contaminate each other's assertions (the 25-block scan reaches far beyond a
 * 5x5x5 structure).
 *
 * <p>Run: {@code ./gradlew :1.20.1-forge:runGameTestServer} (and likewise for neoforge).
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class DemonLampGameTest {

    private DemonLampGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    /** Frozen (no-AI) pig at the center of the given structure-relative cell, added to the level. */
    private static Pig spawnPig(GameTestHelper helper, int x, int y, int z) {
        Pig pig = EntityType.PIG.create(helper.getLevel());
        check(pig != null, "Pig must be creatable");
        BlockPos abs = helper.absolutePos(new BlockPos(x, y, z));
        pig.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        pig.setNoAi(true);
        helper.getLevel().addFreshEntity(pig);
        return pig;
    }

    private static Cow spawnCow(GameTestHelper helper, int x, int y, int z) {
        Cow cow = EntityType.COW.create(helper.getLevel());
        check(cow != null, "Cow must be creatable");
        BlockPos abs = helper.absolutePos(new BlockPos(x, y, z));
        cow.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        cow.setNoAi(true);
        helper.getLevel().addFreshEntity(cow);
        return cow;
    }

    /**
     * Entity ~1.3 blocks from the lamp center: inside BURN_RANGE (2) → 100 in-fire
     * damage, i.e. dead on the first lamp tick.
     */
    @GameTest(template = "empty5x5x5", batch = "demon_lamp_burn", timeoutTicks = 100)
    public static void lamp_burnsCloseEntities(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.LAMP_DEMON.get().defaultBlockState());
        Cow cow = spawnCow(helper, 1, 1, 2);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    check(!cow.isAlive(),
                            "Cow ~1.3 blocks from the lamp must die from the 100 dmg burn tick");
                })
                .thenSucceed();
    }

    /**
     * Entity ~2.2 blocks from the lamp center: no burn, but one tick of unshielded
     * dose = 100000 / len² ≈ 21600 rads (HbmLivingProps caps at 2500).
     */
    @GameTest(template = "empty5x5x5", batch = "demon_lamp_dose", timeoutTicks = 100)
    public static void lamp_radiatesEntitiesAtRange(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.LAMP_DEMON.get().defaultBlockState());
        Pig pig = spawnPig(helper, 4, 1, 2);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    check(pig.isAlive(), "Pig beyond BURN_RANGE must not burn");
                    check(HbmLivingProps.getRadiation(pig) > 1000f,
                            "Unshielded pig at ~2.2 blocks must receive >1000 rad in one tick, got "
                                    + HbmLivingProps.getRadiation(pig));
                })
                .thenSucceed();
    }

    /**
     * Same geometry as the dose test but one extra block of distance (len ≈ 3.1) and
     * a 2x2 obsidian wall (resistance 1200 each) across the ray: dose ≈ 4.3/tick.
     * Two wall blocks actually intersect the marched ray; the wall is built 2 wide
     * and 2 tall so the exact eye-height trajectory stays covered.
     */
    @GameTest(template = "empty5x5x5", batch = "demon_lamp_shield", timeoutTicks = 100)
    public static void lamp_radiationShieldedByWall(GameTestHelper helper) {
        // Lamp at (1,1,1), pig at (4.5,1,1.5): the ray passes through (2,1,1) and (3,2,1).
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.LAMP_DEMON.get().defaultBlockState());
        for (int x = 2; x <= 3; x++) {
            for (int y = 1; y <= 2; y++) {
                helper.setBlock(new BlockPos(x, y, 1), Blocks.OBSIDIAN);
            }
        }
        Pig pig = spawnPig(helper, 4, 1, 1);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    float rad = HbmLivingProps.getRadiation(pig);
                    check(rad < 100f,
                            "Obsidian-shielded pig must stay below 100 rad in one tick, got " + rad);
                })
                .thenSucceed();
    }
}
