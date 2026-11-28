package com.hbm_m.wiaj.cannery.pa;

import com.hbm_m.api.energy.WireBlock;
import com.hbm_m.api.energy.WireCenterVisual;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.FluidDuctBlock;
import com.hbm_m.block.machines.PipeRenderShape;
import com.hbm_m.blockentity.machines.FluidDuctBlockEntity;
import com.hbm_m.wiaj.WiajScene;
import com.hbm_m.wiaj.action.ActionSetBlock;
import com.hbm_m.wiaj.action.ActionSetTile;
import com.hbm_m.wiaj.cannery.Dummies;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * Shared helpers, semantic palettes, and utility builders for Particle Accelerator WIAJ demos.
 */
public final class PaWiajHelpers {

	private PaWiajHelpers() { }

	public static final int[] COLOR_GREEN = new int[] { 0xFFB8FFB8, 0xFF4CD964, 0xFF1F7A33, 0xFF1A1F22 };
	public static final int[] COLOR_RED   = new int[] { 0xFFFFB0B0, 0xFFE53935, 0xFF8E1B1B, 0xFF1A1F22 };
	public static final int[] COLOR_CYAN  = new int[] { 0xFFB0F4FF, 0xFF26C6DA, 0xFF007C91, 0xFF1A1F22 };

	// Reserved Actor ID ranges (0..89 for machines, 90+ for beams/overlays/panels)
	public static final int ID_MACHINE_BASE   = 0;
	public static final int ID_PARTICLE       = 90;
	public static final int ID_PLAYER         = 95;
	public static final int ID_HIGHLIGHT_BASE = 100;
	public static final int ID_GUI_OVERLAY    = 110;
	public static final int ID_PANEL_MAIN     = 120;
	public static final int ID_PANEL_ICON     = 121;
	public static final int ID_PANEL_EXTRA    = 122;

	public static String tr(String key) {
		return I18n.get(key);
	}

	/**
	 * Converts desired beam travel direction to block FACING.
	 * PAOrientation.beamAxis(state) = facing.getCounterClockWise(), so facing = beamDir.getClockWise().
	 */
	public static Direction facingForBeam(Direction beamDir) {
		return beamDir.getClockWise();
	}

	public static BlockState ductState(boolean n, boolean s, boolean e, boolean w, boolean u, boolean d) {
		return ModBlocks.FLUID_DUCT.get().defaultBlockState()
				.setValue(FluidDuctBlock.NORTH, n)
				.setValue(FluidDuctBlock.SOUTH, s)
				.setValue(FluidDuctBlock.EAST, e)
				.setValue(FluidDuctBlock.WEST, w)
				.setValue(FluidDuctBlock.UP, u)
				.setValue(FluidDuctBlock.DOWN, d)
				.setValue(FluidDuctBlock.SHAPE, PipeRenderShape.fromConnections(n, s, e, w, u, d));
	}

	public static void placeDuct(WiajScene scene, int x, int y, int z, Fluid fluid,
			boolean n, boolean s, boolean e, boolean w, boolean u, boolean d) {
		BlockState state = ductState(n, s, e, w, u, d);
		FluidDuctBlockEntity be = new FluidDuctBlockEntity(new BlockPos(x, y, z), state);
		if (fluid != null) {
			be.setFluidTypeSilent(fluid);
		}
		scene.add(new ActionSetTile(x, y, z, be));
		scene.add(new ActionSetBlock(x, y, z, state));
	}

	public static void placeDuctLineX(WiajScene scene, int x1, int x2, int y, int z, Fluid fluid,
			boolean capStart, boolean capEnd) {
		int min = Math.min(x1, x2);
		int max = Math.max(x1, x2);
		for (int x = min; x <= max; x++) {
			boolean w = (x > min) || capStart;
			boolean e = (x < max) || capEnd;
			placeDuct(scene, x, y, z, fluid, false, false, e, w, false, false);
		}
	}

	public static void placeDuctLineZ(WiajScene scene, int x, int y, int z1, int z2, Fluid fluid,
			boolean capStart, boolean capEnd) {
		int min = Math.min(z1, z2);
		int max = Math.max(z1, z2);
		for (int z = min; z <= max; z++) {
			boolean n = (z > min) || capStart;
			boolean s = (z < max) || capEnd;
			placeDuct(scene, x, y, z, fluid, n, s, false, false, false, false);
		}
	}

	public static BlockState cableState(boolean n, boolean s, boolean e, boolean w, boolean u, boolean d) {
		WireCenterVisual center =
				(u && d && !n && !s && !e && !w) ? WireCenterVisual.STRAIGHT_Y :
				(e && w && !n && !s && !u && !d) ? WireCenterVisual.STRAIGHT_X :
				(n && s && !e && !w && !u && !d) ? WireCenterVisual.STRAIGHT_Z :
				WireCenterVisual.JUNCTION;
		return ModBlocks.RED_CABLE.get().defaultBlockState()
				.setValue(WireBlock.NORTH, n)
				.setValue(WireBlock.SOUTH, s)
				.setValue(WireBlock.EAST, e)
				.setValue(WireBlock.WEST, w)
				.setValue(WireBlock.UP, u)
				.setValue(WireBlock.DOWN, d)
				.setValue(WireBlock.CENTER, center);
	}

	public static void placeCable(WiajScene scene, int x, int y, int z,
			boolean n, boolean s, boolean e, boolean w, boolean u, boolean d) {
		scene.add(new ActionSetBlock(x, y, z, cableState(n, s, e, w, u, d)));
	}

	public static void placeDummyConnector(WiajScene scene, int x, int y, int z) {
		scene.add(new ActionSetTile(x, y, z, new Dummies.JarDummyConnector()));
	}

	public static void fillBox(WiajScene scene, int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
			for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
				for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
					scene.add(new ActionSetBlock(x, y, z, block));
				}
			}
		}
	}

	public static void clearBox(WiajScene scene, int x1, int y1, int z1, int x2, int y2, int z2) {
		fillBox(scene, x1, y1, z1, x2, y2, z2, Blocks.AIR);
	}

	public static void digDetectorPit(WiajScene scene, int minX, int maxX, int minZ, int maxZ) {
		clearBox(scene, minX, 1, minZ, maxX, 1, maxZ);
		fillBox(scene, minX, 0, minZ, maxX, 0, maxZ, ModBlocks.CONCRETE_SMOOTH.get());
	}

	public static void buildHollowRingFloor(WiajScene scene, int minX, int maxX, int minZ, int maxZ,
			int y, int margin, Block block) {
		fillBox(scene, minX - margin, y, minZ - margin, maxX + margin, y, minZ + margin, block);
		fillBox(scene, minX - margin, y, maxZ - margin, maxX + margin, y, maxZ + margin, block);
		fillBox(scene, minX - margin, y, minZ - margin, minX + margin, y, maxZ + margin, block);
		fillBox(scene, maxX - margin, y, minZ - margin, maxX + margin, y, maxZ + margin, block);
	}
}
