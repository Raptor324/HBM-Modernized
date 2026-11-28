package com.hbm_m.wiaj.cannery.conveyor;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.MachineCraneSplitterBlock;
import com.hbm_m.block.network.ConveyorBend;
import com.hbm_m.block.network.ConveyorBendableBlock;
import com.hbm_m.block.network.ConveyorBlockBase;
import com.hbm_m.block.network.CraneBaseBlock;
import com.hbm_m.wiaj.WiajScene;
import com.hbm_m.wiaj.action.ActionSetBlock;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared helpers, semantic palettes, and Actor ID constants for Conveyor & Logistics WIAJ demos.
 * <p>
 * Conveyor direction convention (see {@link ConveyorBlockBase}):
 * {@code FACING = WEST} -> items move toward {@code +X} (EAST)
 * {@code FACING = EAST} -> items move toward {@code -X} (WEST)
 * {@code FACING = NORTH} -> items move toward {@code +Z} (SOUTH)
 * {@code FACING = SOUTH} -> items move toward {@code -Z} (NORTH)
 */
public final class ConveyorWiajHelpers {

	private ConveyorWiajHelpers() { }

	public static final int[] COLOR_GREEN = new int[] { 0xFFB8FFB8, 0xFF4CD964, 0xFF1F7A33, 0xFF1A1F22 };
	public static final int[] COLOR_RED   = new int[] { 0xFFFFB0B0, 0xFFE53935, 0xFF8E1B1B, 0xFF1A1F22 };
	public static final int[] COLOR_CYAN  = new int[] { 0xFFB0F4FF, 0xFF26C6DA, 0xFF007C91, 0xFF1A1F22 };

	// Reserved Actor ID ranges (non-overlapping)
	public static final int ID_STREAM_BASE    = 1;   // 1..29: item streams
	public static final int ID_HIGHLIGHT_BASE = 40;  // 40..59: 3D world highlights
	public static final int ID_GUI_OVERLAY    = 70;  // 2D sorter GUI preview
	public static final int ID_PANEL_MAIN     = 80;  // Main info panel
	public static final int ID_PANEL_ICON     = 81;  // Extra icon panel

	// Panel docking coordinates (kept away from center 3D scene)
	public static final int PANEL_TOP_Y       = -82;
	public static final int PANEL_BOT_Y       = 72;
	public static final int PANEL_LEFT_X      = -105;

	public static String tr(String key) {
		return I18n.get(key);
	}

	/** Converts desired item travel direction to conveyor {@code FACING}. */
	public static Direction facingForMove(Direction moveDir) {
		return moveDir.getOpposite();
	}

	public static BlockState beltRegular(Direction moveDir, ConveyorBend bend) {
		return ModBlocks.CONVEYOR.get().defaultBlockState()
				.setValue(ConveyorBlockBase.FACING, facingForMove(moveDir))
				.setValue(ConveyorBendableBlock.BEND, bend);
	}

	public static BlockState beltExpress(Direction moveDir, ConveyorBend bend) {
		return ModBlocks.CONVEYOR_EXPRESS.get().defaultBlockState()
				.setValue(ConveyorBlockBase.FACING, facingForMove(moveDir))
				.setValue(ConveyorBendableBlock.BEND, bend);
	}

	public static BlockState beltDouble(Direction moveDir) {
		return ModBlocks.CONVEYOR_DOUBLE.get().defaultBlockState()
				.setValue(ConveyorBlockBase.FACING, facingForMove(moveDir));
	}

	public static BlockState beltTriple(Direction moveDir) {
		return ModBlocks.CONVEYOR_TRIPLE.get().defaultBlockState()
				.setValue(ConveyorBlockBase.FACING, facingForMove(moveDir));
	}

	public static BlockState beltLift(Direction moveDir) {
		return ModBlocks.CONVEYOR_LIFT.get().defaultBlockState()
				.setValue(ConveyorBlockBase.FACING, facingForMove(moveDir));
	}

	public static BlockState beltChute(Direction moveDir) {
		return ModBlocks.CONVEYOR_CHUTE.get().defaultBlockState()
				.setValue(ConveyorBlockBase.FACING, facingForMove(moveDir));
	}

	// --- Crane / Logistics blocks ---

	/** Extractor (Ejector): pulls from inventory at OUTPUT and places onto belt at FACING. */
	public static BlockState beltEjector(Direction outMoveDir) {
		return ModBlocks.CRANE_EXTRACTOR.get().defaultBlockState()
				.setValue(CraneBaseBlock.FACING, outMoveDir)
				.setValue(CraneBaseBlock.OUTPUT, outMoveDir.getOpposite());
	}

	/** Inserter: accepts items from belt at FACING and inserts into inventory at OUTPUT. */
	public static BlockState beltInserter(Direction inMoveDir) {
		return ModBlocks.CRANE_INSERTER.get().defaultBlockState()
				.setValue(CraneBaseBlock.FACING, inMoveDir.getOpposite())
				.setValue(CraneBaseBlock.OUTPUT, inMoveDir);
	}

	/** Splitter: 2-block multiblock (LEFT/RIGHT), items travel opposite to FACING. */
	public static BlockState beltSplitter(Direction moveDir, MachineCraneSplitterBlock.Half half) {
		return ModBlocks.CRANE_SPLITTER.get().defaultBlockState()
				.setValue(MachineCraneSplitterBlock.FACING, facingForMove(moveDir))
				.setValue(MachineCraneSplitterBlock.HALF, half);
	}

	/** Boxer: accepts items at FACING and outputs packages at OUTPUT. */
	public static BlockState beltBoxer(Direction moveDir) {
		return ModBlocks.CRANE_BOXER.get().defaultBlockState()
				.setValue(CraneBaseBlock.FACING, moveDir.getOpposite())
				.setValue(CraneBaseBlock.OUTPUT, moveDir);
	}

	/** Unboxer: accepts packages at OUTPUT and outputs unpacked items at FACING. */
	public static BlockState beltUnboxer(Direction moveDir) {
		return ModBlocks.CRANE_UNBOXER.get().defaultBlockState()
				.setValue(CraneBaseBlock.FACING, moveDir)
				.setValue(CraneBaseBlock.OUTPUT, moveDir.getOpposite());
	}

	/** Router (Sorter): routes items by 6 color-coded face filters. */
	public static BlockState beltSorter() {
		return ModBlocks.CRANE_ROUTER.get().defaultBlockState();
	}

	// --- Scene geometry helpers ---

	public static void fillBox(WiajScene scene, int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
			for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
				for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
					scene.add(new ActionSetBlock(x, y, z, block));
				}
			}
		}
	}

	/** Builds a smooth concrete floor with industrial hazard border. */
	public static void buildIndustrialFloor(WiajScene scene, int sizeX, int sizeZ) {
		for (int x = sizeX - 1; x >= 0; x--) {
			for (int z = 0; z < sizeZ; z++) {
				boolean border = (x == 0 || x == sizeX - 1 || z == 0 || z == sizeZ - 1);
				Block b = border ? ModBlocks.CONCRETE_HAZARD.get() : ModBlocks.CONCRETE_SMOOTH.get();
				scene.add(new ActionSetBlock(x, 0, z, b));
			}
		}
	}

	/** Clears everything above the floor (y >= 1) between scenes. */
	public static void clearStageAboveFloor(WiajScene scene, int sizeX, int sizeY, int sizeZ) {
		fillBox(scene, 0, 1, 0, sizeX - 1, sizeY - 1, sizeZ - 1, Blocks.AIR);
	}
}
