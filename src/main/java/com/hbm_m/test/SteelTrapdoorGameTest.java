package com.hbm_m.test;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Vertrag der Stahlfalltuer nach dem 1:1-Port von {@code BlockNTMTrapdoor} (R6d).
 *
 * <p>Original erbt von {@code BlockTrapDoor}: geschlossen eine 3/16 dicke Platte,
 * {@code setResistance(8F)} entspricht 1.7.10-intern 24/5 = 4.8.
 * (Die alten Tests pinnten noch den Platzhalter-Vollwuerfel.)
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class SteelTrapdoorGameTest {

    private static final double EPS = 1.0E-6D;

    private SteelTrapdoorGameTest() {
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "steel_trapdoor", timeoutTicks = 100)
    public static void steelTrapdoorIsTrapdoorSlab(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.TRAPDOOR_STEEL.get());
        check(ModBlocks.TRAPDOOR_STEEL.get() instanceof TrapDoorBlock,
                "Steel Trapdoor is a TrapDoorBlock like the original BlockTrapDoor subclass");
        check(!helper.getBlockState(pos).getValue(TrapDoorBlock.OPEN), "Steel Trapdoor starts closed");
        AABB bounds = helper.getBlockState(pos).getShape(level, helper.absolutePos(pos)).bounds();
        // Geschlossen, untere Haelfte: Platte 0..3/16 auf Y, voll auf X/Z.
        check(Math.abs(bounds.minY) < EPS && Math.abs(bounds.maxY - 0.1875D) < EPS
                        && Math.abs(bounds.minX) < EPS && Math.abs(bounds.maxX - 1.0D) < EPS
                        && Math.abs(bounds.minZ) < EPS && Math.abs(bounds.maxZ - 1.0D) < EPS,
                "Closed Steel Trapdoor is a 3/16 slab, got " + bounds);

        float res = ModBlocks.TRAPDOOR_STEEL.get().getExplosionResistance();
        check(Math.abs(res - 4.8F) < 1.0E-4F, "Steel Trapdoor explosion resistance is 4.8 like the original, got " + res);

        // State cleanup.
        level.removeBlock(helper.absolutePos(pos), false);
        helper.succeed();
    }
}
