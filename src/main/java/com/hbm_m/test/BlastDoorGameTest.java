package com.hbm_m.test;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.doors.BlastDoorDecl;
import com.hbm_m.item.ModItems;
import com.hbm_m.util.CompletionStatus;
import com.hbm_m.util.CompletionTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * GameTest-верификация паритета Blast Door с 1.7.10 (M1: blast_door).
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class BlastDoorGameTest {

    private static final float EPS = 1.0E-4F;

    private BlastDoorGameTest() {
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "blast_door", timeoutTicks = 100)
    public static void blastDoorPropertiesAndParity(GameTestHelper helper) {
        // 1. Hardness 10.0F, Resistance 1000.0F
        float resistance = ModBlocks.BLAST_DOOR.get().getExplosionResistance();
        check(Math.abs(resistance - 1000.0F) < EPS,
                "Blast Door resistance must be 1000.0F like in 1.7.10, got " + resistance);

        float destroySpeed = ModBlocks.BLAST_DOOR.get().defaultDestroyTime();
        check(Math.abs(destroySpeed - 10.0F) < EPS,
                "Blast Door destroy speed must be 10.0F like in 1.7.10, got " + destroySpeed);

        // 2. Completion status COMPLETE
        check(CompletionTracker.getStatus(ModBlocks.BLAST_DOOR.getId()) == CompletionStatus.COMPLETE,
                "ModBlocks.BLAST_DOOR must be marked CompletionStatus.COMPLETE");
        check(CompletionTracker.getStatus(ModItems.BLAST_DOOR.getId()) == CompletionStatus.COMPLETE,
                "ModItems.BLAST_DOOR must be marked CompletionStatus.COMPLETE");

        // 3. BlastDoorDecl 1x7 column and dynamic thresholds
        BlastDoorDecl decl = BlastDoorDecl.INSTANCE;
        check(decl.getOpenTime() == 100, "Blast Door open time must be 100 ticks");

        // Progressive retraction check: y+1 to y+5 removed progressively
        // j=0 (y=1): threshold = 0.0
        // j=1 (y=2): threshold = 0.2
        // j=2 (y=3): threshold = 0.4
        // j=3 (y=4): threshold = 0.6
        // j=4 (y=5): threshold = 0.8
        check(Math.abs(decl.getRetractionThreshold(0, 5, 0) - 0.0F) < EPS, "Threshold for y+1 must be 0.0");
        check(Math.abs(decl.getRetractionThreshold(1, 5, 0) - 0.2F) < EPS, "Threshold for y+2 must be 0.2");
        check(Math.abs(decl.getRetractionThreshold(2, 5, 0) - 0.4F) < EPS, "Threshold for y+3 must be 0.4");
        check(Math.abs(decl.getRetractionThreshold(3, 5, 0) - 0.6F) < EPS, "Threshold for y+4 must be 0.6");
        check(Math.abs(decl.getRetractionThreshold(4, 5, 0) - 0.8F) < EPS, "Threshold for y+5 must be 0.8");

        // Progressive dummy retraction behavior:
        BlockPos y1 = new BlockPos(0, 1, 0);
        BlockPos y2 = new BlockPos(0, 2, 0);
        BlockPos y3 = new BlockPos(0, 3, 0);
        BlockPos y4 = new BlockPos(0, 4, 0);
        BlockPos y5 = new BlockPos(0, 5, 0);
        BlockPos y6 = new BlockPos(0, 6, 0); // Frame: never retracted

        // Fully closed (progress = 0): nothing retracted
        check(!decl.isPartRetracted(y1, 0.0F), "y+1 solid at progress 0.0");
        check(!decl.isPartRetracted(y5, 0.0F), "y+5 solid at progress 0.0");

        // Progress 0.1: y+1 retracted, others solid
        check(decl.isPartRetracted(y1, 0.1F), "y+1 retracted at progress 0.1");
        check(!decl.isPartRetracted(y2, 0.1F), "y+2 solid at progress 0.1");

        // Progress 0.3: y+1 and y+2 retracted
        check(decl.isPartRetracted(y2, 0.3F), "y+2 retracted at progress 0.3");
        check(!decl.isPartRetracted(y3, 0.3F), "y+3 solid at progress 0.3");

        // Progress 0.5: y+1..y+3 retracted
        check(decl.isPartRetracted(y3, 0.5F), "y+3 retracted at progress 0.5");
        check(!decl.isPartRetracted(y4, 0.5F), "y+4 solid at progress 0.5");

        // Progress 0.7: y+1..y+4 retracted
        check(decl.isPartRetracted(y4, 0.7F), "y+4 retracted at progress 0.7");
        check(!decl.isPartRetracted(y5, 0.7F), "y+5 solid at progress 0.7");

        // Progress 0.9: y+1..y+5 retracted
        check(decl.isPartRetracted(y5, 0.9F), "y+5 retracted at progress 0.9");

        // Housing at y+6 is NEVER retracted
        check(!decl.isPartRetracted(y6, 0.5F), "Housing y+6 never retracted at progress 0.5");

        helper.succeed();
    }
}
