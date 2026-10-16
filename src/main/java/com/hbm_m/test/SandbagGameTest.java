package com.hbm_m.test;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
 *///?}

/**
 * Contract of the Sandbags block in its current implementation.
 *
 * <p>The block is registered as a simple full cube with copied stone
 * properties. The original behavior (blast resistance 18 and shape
 * merging with neighboring blocks) has not been ported yet — the tests
 * pin down the actual contract so that the future port of the
 * specialized block cannot go unnoticed.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class SandbagGameTest {

    private SandbagGameTest() {
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "sandbags", timeoutTicks = 100)
    public static void sandbagsKeepFullCubeShape(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SANDBAGS.get());
        AABB bounds = helper.getBlockState(pos).getShape(level, helper.absolutePos(pos)).bounds();
        // An isolated block is still a full cube 0..1 on all axes for now
        // (the original 0.25..0.75 contract will come with the specialized block).
        check(bounds.minX == 0.0D && bounds.maxX == 1.0D
                        && bounds.minY == 0.0D && bounds.maxY == 1.0D
                        && bounds.minZ == 0.0D && bounds.maxZ == 1.0D,
                "Sandbags are a full cube in the current implementation");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "sandbags", timeoutTicks = 100)
    public static void sandbagsResistanceMatchesRegistration(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SANDBAGS.get());

        // Properties are copied from stone; the original blast resistance 18 is not set yet.
        float sandbags = ModBlocks.SANDBAGS.get().getExplosionResistance();
        float stone = Blocks.STONE.getExplosionResistance();
        check(sandbags == stone,
                "Sandbags resistance equals stone (properties copied at registration)");
        helper.succeed();
    }
}
