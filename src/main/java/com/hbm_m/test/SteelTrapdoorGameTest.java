package com.hbm_m.test;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
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
 * Contract of the Steel Trapdoor block in its current implementation.
 *
 * <p>The block is registered as a simple full cube with copied stone
 * properties: TrapDoorBlock behavior (hand opening, ladder attachment,
 * 2-pixel-thick slab) and the "oak trapdoor + steel ingot" recipe have
 * not yet been ported. These tests pin down the actual contract until
 * the specialized block is ported.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class SteelTrapdoorGameTest {

    private SteelTrapdoorGameTest() {
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "steel_trapdoor", timeoutTicks = 100)
    public static void steelTrapdoorKeepsFullCubeShape(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.TRAPDOOR_STEEL.get());
        AABB bounds = helper.getBlockState(pos).getShape(level, helper.absolutePos(pos)).bounds();
        // Full cube until the real TrapDoorBlock is ported (slab 14/16..1 on Z).
        check(bounds.minX == 0.0D && bounds.maxX == 1.0D
                        && bounds.minY == 0.0D && bounds.maxY == 1.0D
                        && bounds.minZ == 0.0D && bounds.maxZ == 1.0D,
                "Steel Trapdoor is a full cube in the current implementation");

        // State cleanup.
        level.removeBlock(helper.absolutePos(pos), false);
        helper.succeed();
    }
}
