package com.hbm_m.test;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.SealBlocks;
import com.hbm_m.blockentity.machines.HatchBlockEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.util.CompletionStatus;
import com.hbm_m.util.CompletionTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * GameTest-верификация паритета Seal Controller, Seal Frame и Seal Hatch с 1.7.10 (M3: seal_controller).
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class SealControllerGameTest {

    private static final float EPS = 1.0E-4F;

    private SealControllerGameTest() {
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "seal_controller", timeoutTicks = 100)
    public static void sealControllerPropertiesAndParity(GameTestHelper helper) {
        // 1. Controller & Frame: Hardness 10.0F, Resistance 100.0F
        float ctrlRes = ModBlocks.SEAL_CONTROLLER.get().getExplosionResistance();
        check(Math.abs(ctrlRes - 100.0F) < EPS,
                "Seal Controller blast resistance must be 100.0F, got " + ctrlRes);

        float ctrlHardness = ModBlocks.SEAL_CONTROLLER.get().defaultDestroyTime();
        check(Math.abs(ctrlHardness - 10.0F) < EPS,
                "Seal Controller hardness must be 10.0F, got " + ctrlHardness);

        float frameRes = ModBlocks.SEAL_FRAME.get().getExplosionResistance();
        check(Math.abs(frameRes - 100.0F) < EPS,
                "Seal Frame blast resistance must be 100.0F, got " + frameRes);

        float frameHardness = ModBlocks.SEAL_FRAME.get().defaultDestroyTime();
        check(Math.abs(frameHardness - 10.0F) < EPS,
                "Seal Frame hardness must be 10.0F, got " + frameHardness);

        // 2. Hatch: Unbreakable (-1.0F), Infinite blast resistance (>= 3600000.0F / Float.POSITIVE_INFINITY)
        float hatchRes = ModBlocks.SEAL_HATCH.get().getExplosionResistance();
        check(Float.isInfinite(hatchRes) || hatchRes >= 3600000.0F,
                "Seal Hatch blast resistance must be infinite, got " + hatchRes);

        float hatchHardness = ModBlocks.SEAL_HATCH.get().defaultDestroyTime();
        check(hatchHardness == -1.0F,
                "Seal Hatch hardness must be -1.0F (unbreakable), got " + hatchHardness);

        // 3. Completion status COMPLETE in ModBlocks and ModItems
        check(CompletionTracker.getStatus(ModBlocks.SEAL_CONTROLLER.getId()) == CompletionStatus.COMPLETE,
                "ModBlocks.SEAL_CONTROLLER must be marked CompletionStatus.COMPLETE");
        check(CompletionTracker.getStatus(ModBlocks.SEAL_FRAME.getId()) == CompletionStatus.COMPLETE,
                "ModBlocks.SEAL_FRAME must be marked CompletionStatus.COMPLETE");
        check(CompletionTracker.getStatus(ModBlocks.SEAL_HATCH.getId()) == CompletionStatus.COMPLETE,
                "ModBlocks.SEAL_HATCH must be marked CompletionStatus.COMPLETE");

        check(CompletionTracker.getStatus(ModItems.SEAL_CONTROLLER.getId()) == CompletionStatus.COMPLETE,
                "ModItems.SEAL_CONTROLLER must be marked CompletionStatus.COMPLETE");
        check(CompletionTracker.getStatus(ModItems.SEAL_FRAME.getId()) == CompletionStatus.COMPLETE,
                "ModItems.SEAL_FRAME must be marked CompletionStatus.COMPLETE");
        check(CompletionTracker.getStatus(ModItems.SEAL_HATCH.getId()) == CompletionStatus.COMPLETE,
                "ModItems.SEAL_HATCH must be marked CompletionStatus.COMPLETE");

        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "seal_controller", timeoutTicks = 100)
    public static void sealControllerScanningAndOperation(GameTestHelper helper) {
        Level level = helper.getLevel();

        // Build a 3x3 horizontal frame at relative y = 1 (size 1, interior aperture 1x1 at (1, 1, 1))
        // Controller at relative (1, 1, 0) facing NORTH (so behind is SOUTH, i.e. +Z towards z = 1 and z = 2)
        BlockPos ctrlPos = helper.absolutePos(new BlockPos(1, 1, 0));
        BlockState ctrlState = ModBlocks.SEAL_CONTROLLER.get().defaultBlockState()
                .setValue(SealBlocks.Controller.FACING, Direction.NORTH);
        level.setBlockAndUpdate(ctrlPos, ctrlState);

        // Place frame blocks around the 3x3 perimeter
        // North edge: (0, 1, 0), (1, 1, 0) is controller, (2, 1, 0)
        level.setBlockAndUpdate(helper.absolutePos(new BlockPos(0, 1, 0)), ModBlocks.SEAL_FRAME.get().defaultBlockState());
        level.setBlockAndUpdate(helper.absolutePos(new BlockPos(2, 1, 0)), ModBlocks.SEAL_FRAME.get().defaultBlockState());

        // West edge: (0, 1, 1)
        level.setBlockAndUpdate(helper.absolutePos(new BlockPos(0, 1, 1)), ModBlocks.SEAL_FRAME.get().defaultBlockState());
        // East edge: (2, 1, 1)
        level.setBlockAndUpdate(helper.absolutePos(new BlockPos(2, 1, 1)), ModBlocks.SEAL_FRAME.get().defaultBlockState());

        // South edge: (0, 1, 2), (1, 1, 2), (2, 1, 2)
        level.setBlockAndUpdate(helper.absolutePos(new BlockPos(0, 1, 2)), ModBlocks.SEAL_FRAME.get().defaultBlockState());
        level.setBlockAndUpdate(helper.absolutePos(new BlockPos(1, 1, 2)), ModBlocks.SEAL_FRAME.get().defaultBlockState());
        level.setBlockAndUpdate(helper.absolutePos(new BlockPos(2, 1, 2)), ModBlocks.SEAL_FRAME.get().defaultBlockState());

        // Interior aperture at (1, 1, 1) should initially be air
        BlockPos hatchPos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(hatchPos, Blocks.AIR.defaultBlockState());

        // 1. Verify frame scanning
        int size = SealBlocks.Controller.getFrameSize(level, ctrlPos);
        check(size == 1, "Expected frame size 1 for 3x3 frame, got " + size);

        // 2. Verify seal is initially open
        check(!SealBlocks.Controller.isSealClosed(level, ctrlPos, size), "Seal should initially be open");

        // 3. Close seal -> fills aperture with seal_hatch
        SealBlocks.Controller.closeSeal(level, ctrlPos, size);
        check(level.getBlockState(hatchPos).is(ModBlocks.SEAL_HATCH.get()), "Aperture must contain seal_hatch after closing");
        check(SealBlocks.Controller.isSealClosed(level, ctrlPos, size), "Seal should now be closed");

        // Verify HatchBlockEntity has controller pos set
        if (level.getBlockEntity(hatchPos) instanceof HatchBlockEntity hatchTe) {
            check(ctrlPos.equals(hatchTe.getControllerPos()), "HatchBlockEntity must have controller pos set to " + ctrlPos);
        } else {
            throw new GameTestAssertException("Aperture must contain HatchBlockEntity");
        }

        // 4. Remote detonator (IBomb.explode) toggles seal: closed -> open
        if (ctrlState.getBlock() instanceof IBomb bomb) {
            IBomb.BombReturnCode code = bomb.explode(level, ctrlPos);
            check(code == IBomb.BombReturnCode.TRIGGERED, "Detonator explode must return TRIGGERED, got " + code);
            check(level.getBlockState(hatchPos).isAir(), "Aperture must be air after opening via detonator");
            check(!SealBlocks.Controller.isSealClosed(level, ctrlPos, size), "Seal should now be open");

            // Toggling again via detonator: open -> closed
            code = bomb.explode(level, ctrlPos);
            check(code == IBomb.BombReturnCode.TRIGGERED, "Detonator explode must return TRIGGERED on re-close");
            check(level.getBlockState(hatchPos).is(ModBlocks.SEAL_HATCH.get()), "Aperture must be hatch after re-closing");
        } else {
            throw new GameTestAssertException("Controller must implement IBomb");
        }

        // 5. Invalidation / Hatch liveness: breaking the controller dissolves the hatch
        level.setBlockAndUpdate(ctrlPos, Blocks.AIR.defaultBlockState());
        if (level.getBlockEntity(hatchPos) instanceof HatchBlockEntity hatchTeAfterBreak) {
            HatchBlockEntity.serverTick(level, hatchPos, level.getBlockState(hatchPos), hatchTeAfterBreak);
            check(level.getBlockState(hatchPos).isAir(), "Hatch must dissolve to air when controller is destroyed");
        }

        helper.succeed();
    }
}
