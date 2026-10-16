package com.hbm_m.test;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.icf.ICFLaserPart;
import com.hbm_m.blockentity.machines.UniversalMachinePartBlockEntity;
import com.hbm_m.blockentity.machines.icf.ICFPhantomBlockEntity;
import com.hbm_m.blockentity.machines.pile.PileBaseBlockEntity;
import com.hbm_m.interfaces.IRelocatable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
 *///?}

/**
 * What a contraption engine (Create, Sable) needs from the mod's blocks when it moves them:
 * {@code BlockState.rotate}/{@code mirror} must turn the FACING of blocks that never override
 * them ({@code BlockFacingRotationMixin}), and block entities holding absolute positions must
 * remap them through the engine's transform ({@link IRelocatable}).
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class ContraptionMoveGameTest {

    private ContraptionMoveGameTest() {}

    private static final String BATCH = "contraptionmove";

    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    private static Direction facing(BlockState state) {
        return state.getValue(HorizontalDirectionalBlock.FACING);
    }

    @GameTest(template = "empty3x3x3", batch = BATCH, timeoutTicks = 100)
    public static void rotate_controllerWithoutOverride_turnsFacing(GameTestHelper helper) {
        // BaseEntityBlock + own FACING: nothing in the class hierarchy rotates it.
        BlockState assembler = ModBlocks.MACHINE_ASSEMBLER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        check(facing(assembler.rotate(Rotation.CLOCKWISE_90)) == Direction.EAST, "assembler CW90: NORTH -> EAST");
        check(facing(assembler.rotate(Rotation.CLOCKWISE_180)) == Direction.SOUTH, "assembler CW180: NORTH -> SOUTH");
        check(facing(assembler.rotate(Rotation.COUNTERCLOCKWISE_90)) == Direction.WEST, "assembler CCW90: NORTH -> WEST");
        check(facing(assembler.rotate(Rotation.NONE)) == Direction.NORTH, "assembler NONE is identity");

        BlockState torus = ModBlocks.TORUS.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST);
        check(facing(torus.rotate(Rotation.CLOCKWISE_90)) == Direction.NORTH, "torus CW90: WEST -> NORTH");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = BATCH, timeoutTicks = 100)
    public static void rotate_machinePart_turnsFacing(GameTestHelper helper) {
        // Parts derive their controller from their own FACING, so they must turn with the ship.
        BlockState part = ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
        check(facing(part.rotate(Rotation.CLOCKWISE_90)) == Direction.WEST, "part CW90: SOUTH -> WEST");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = BATCH, timeoutTicks = 100)
    public static void mirror_controllerWithoutOverride_flipsFacing(GameTestHelper helper) {
        BlockState north = ModBlocks.MACHINE_ASSEMBLER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        BlockState east = north.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
        // Vanilla semantics (HorizontalDirectionalBlock.mirror): LEFT_RIGHT flips the Z axis only.
        check(facing(north.mirror(Mirror.LEFT_RIGHT)) == Direction.SOUTH, "LEFT_RIGHT: NORTH -> SOUTH");
        check(facing(east.mirror(Mirror.LEFT_RIGHT)) == Direction.EAST, "LEFT_RIGHT leaves EAST");
        check(facing(east.mirror(Mirror.FRONT_BACK)) == Direction.WEST, "FRONT_BACK: EAST -> WEST");
        check(facing(north.mirror(Mirror.NONE)) == Direction.NORTH, "NONE is identity");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = BATCH, timeoutTicks = 100)
    public static void relocate_machinePart_movesControllerPos(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        BlockEntity be = helper.getBlockEntity(at);
        check(be instanceof UniversalMachinePartBlockEntity, "part block entity expected");
        UniversalMachinePartBlockEntity part = (UniversalMachinePartBlockEntity) be;
        BlockPos controller = helper.absolutePos(new BlockPos(0, 1, 1));
        part.setControllerPos(controller);

        ((IRelocatable) part).relocate(pos -> pos.offset(10, 20, 30));
        check(controller.offset(10, 20, 30).equals(part.getControllerPos()), "controller position must follow the transform");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = BATCH, timeoutTicks = 100)
    public static void relocate_icfPhantom_movesCorePos(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, ModBlocks.ICF_BLOCK.get().defaultBlockState());
        BlockEntity be = helper.getBlockEntity(at);
        check(be instanceof ICFPhantomBlockEntity, "ICF phantom block entity expected");
        ICFPhantomBlockEntity phantom = (ICFPhantomBlockEntity) be;
        BlockPos core = helper.absolutePos(new BlockPos(0, 1, 0));
        phantom.setup(ICFLaserPart.CASING, core);

        ((IRelocatable) phantom).relocate(pos -> pos.offset(-5, 0, 7));
        check(core.offset(-5, 0, 7).equals(phantom.getCorePos()), "core position must follow the transform");
        check(phantom.getPart() == ICFLaserPart.CASING, "the remembered part is untouched");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = BATCH, timeoutTicks = 100)
    public static void relocate_pileBrick_movesCorePos(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, ModBlocks.PILE_BLOCK.get().defaultBlockState());
        BlockEntity be = helper.getBlockEntity(at);
        check(be instanceof PileBaseBlockEntity, "pile block entity expected");
        PileBaseBlockEntity pile = (PileBaseBlockEntity) be;
        BlockPos core = helper.absolutePos(new BlockPos(2, 1, 2));
        pile.setCore(core);

        ((IRelocatable) pile).relocate(pos -> pos.offset(3, 0, 0));
        check(core.offset(3, 0, 0).equals(pile.getCorePos()), "core position must follow the transform");
        helper.succeed();
    }
}
