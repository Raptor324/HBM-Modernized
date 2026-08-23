package com.hbm_m.test;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
 *///?}

/**
 * Guard test for the wood barrier ensuring no recursion in collision shapes.
 *
 * <p>Historical problem: adjacent barriers could endlessly re-enter each other's
 * {@code getCollisionShape} and crash the server with a StackOverflowError.
 * In the current implementation the block is a simple full cube, so its shape is
 * trivially stable; this test pins that invariant so porting a specialized
 * barrier class cannot reintroduce the regression.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class LegacyWoodBarrierGameTest {

    private LegacyWoodBarrierGameTest() {
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    /** {@link VoxelShape} has no value comparison — compare via symmetric difference. */
    private static boolean sameShape(VoxelShape first, VoxelShape second) {
        return !Shapes.joinIsNotEmpty(first, second, BooleanOp.NOT_SAME);
    }

    private static VoxelShape collision(Level level, BlockPos absolute) {
        BlockState state = level.getBlockState(absolute);
        return state.getCollisionShape(level, absolute, CollisionContext.empty());
    }

    @GameTest(template = "empty5x5x5", batch = "wood_barrier", timeoutTicks = 100)
    public static void adjacentBarriersDoNotRecurse(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos first = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.WOOD_BARRIER.get());
        helper.setBlock(new BlockPos(3, 1, 2), ModBlocks.WOOD_BARRIER.get());
        helper.setBlock(new BlockPos(4, 1, 2), ModBlocks.WOOD_BARRIER.get());
        helper.setBlock(new BlockPos(2, 1, 3), ModBlocks.WOOD_BARRIER.get());

        // Before the fix, an equivalent call would never return due to recursion.
        collision(level, first);
        collision(level, first.east());
        collision(level, first.south());

        check(sameShape(collision(level, first.east()), collision(level, first.east())),
                "A barrier surrounded by barriers must yield a stable collision shape without recursion");

        // Cleanup.
        for (BlockPos p : new BlockPos[]{first, first.east(), first.east().east(), first.south()}) {
            level.removeBlock(p, false);
        }
        helper.succeed();
    }
}
