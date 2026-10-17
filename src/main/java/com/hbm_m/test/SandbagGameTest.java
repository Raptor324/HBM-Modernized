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
 * Vertrag des Sandsack-Blocks nach dem 1:1-Port von {@code BlockSandbags} (R6d).
 *
 * <p>Original: allein stehend 0.25..0.75 auf X/Z (Hoehe voll), Seiten wachsen
 * zu Nachbar-Sandsaecken bzw. festen Bloecken auf 0/1; {@code setResistance(30F)}
 * entspricht 1.7.10-intern 90/5 = 18 Explosionsresistenz.
 * (Die alten Tests pinnten noch den Platzhalter-Vollwuerfel mit Stein-Werten.)
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class SandbagGameTest {

    private static final double EPS = 1.0E-6D;

    private SandbagGameTest() {
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static boolean eq(double a, double b) {
        return Math.abs(a - b) < EPS;
    }

    @GameTest(template = "empty3x3x3", batch = "sandbags", timeoutTicks = 100)
    public static void sandbagsIsolatedShapeMatchesOriginal(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SANDBAGS.get());
        AABB bounds = helper.getBlockState(pos).getShape(level, helper.absolutePos(pos)).bounds();
        // Original setBlockBoundsBasedOnState ohne Nachbarn: 0.25..0.75 auf X/Z, 0..1 auf Y.
        check(eq(bounds.minX, 0.25D) && eq(bounds.maxX, 0.75D)
                        && eq(bounds.minY, 0.0D) && eq(bounds.maxY, 1.0D)
                        && eq(bounds.minZ, 0.25D) && eq(bounds.maxZ, 0.75D),
                "Isolated sandbags are 0.25..0.75 on X/Z like the original, got " + bounds);
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "sandbags", timeoutTicks = 100)
    public static void sandbagsConnectToNeighbourSandbags(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockPos east = pos.east();
        helper.setBlock(pos, ModBlocks.SANDBAGS.get());
        // Nachbar danach setzen: updateShape des ersten Sandsacks muss die Ostseite auf 1 ziehen.
        helper.setBlock(east, ModBlocks.SANDBAGS.get());
        AABB bounds = helper.getBlockState(pos).getShape(level, helper.absolutePos(pos)).bounds();
        check(eq(bounds.maxX, 1.0D) && eq(bounds.minX, 0.25D)
                        && eq(bounds.minZ, 0.25D) && eq(bounds.maxZ, 0.75D),
                "Sandbags extend towards a neighbouring sandbag (east side 1.0), got " + bounds);
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "sandbags", timeoutTicks = 100)
    public static void sandbagsResistanceMatchesOriginal(GameTestHelper helper) {
        float sandbags = ModBlocks.SANDBAGS.get().getExplosionResistance();
        // Original: setResistance(30F) -> 1.7.10 speichert 90, getExplosionResistance = 90/5 = 18.
        check(Math.abs(sandbags - 18.0F) < 1.0E-4F,
                "Sandbags explosion resistance is 18 like the original, got " + sandbags);
        helper.succeed();
    }
}
