package com.hbm_m.wiaj.cannery.rbmk;

import com.hbm_m.api.energy.WireBlock;
import com.hbm_m.api.energy.WireCenterVisual;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.FluidDuctBlock;
import com.hbm_m.block.machines.PipeRenderShape;
import com.hbm_m.blockentity.machines.FluidDuctBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKAbsorberBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKBlankBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKBoilerBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKControlAutoBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKControlBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKControlManualBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKCoolerBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKModeratorBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKOutgasserBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKReflectorBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKRodBlockEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.wiaj.WiajScene;
import com.hbm_m.wiaj.action.ActionCreateActor;
import com.hbm_m.wiaj.action.ActionSetBlock;
import com.hbm_m.wiaj.action.ActionSetTile;
import com.hbm_m.wiaj.actor.ActorBase;
import com.hbm_m.wiaj.actor.ActorTileEntity;
import com.hbm_m.wiaj.cannery.Dummies;
import com.hbm_m.wiaj.cannery.rbmk.RbmkWiajActors.ActorRbmkMachine;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * Shared helpers, semantic palettes, Actor ID ranges, and block/duct/cable factories
 * for RBMK WIAJ 3D demos.
 */
public final class RbmkWiajHelpers {

	private RbmkWiajHelpers() { }

	public static final int[] COLOR_GREEN  = new int[] { 0xFFB8FFB8, 0xFF4CD964, 0xFF1F7A33, 0xFF1A1F22 };
	public static final int[] COLOR_RED    = new int[] { 0xFFFFB0B0, 0xFFE53935, 0xFF8E1B1B, 0xFF1A1F22 };
	public static final int[] COLOR_CYAN   = new int[] { 0xFFB0F4FF, 0xFF26C6DA, 0xFF007C91, 0xFF1A1F22 };
	public static final int[] COLOR_PURPLE = new int[] { 0xFFE3B8FF, 0xFFAB47BC, 0xFF6A1B9A, 0xFF1A1F22 };

	// Reserved non-overlapping Actor ID ranges
	public static final int ID_COLUMN_BASE    = 0;    // 0..149:  RBMK columns & machines (turbines, cooling tower, console)
	public static final int ID_FLUX_BASE      = 150;  // 150..169: neutron pulses / steam & water flow markers
	public static final int ID_HIGHLIGHT_BASE = 170;  // 170..189: 3D block/zone highlights
	public static final int ID_MELTDOWN_FX    = 190;  // 190..194: overheat & meltdown effects
	public static final int ID_GUI_OVERLAY    = 200;  // 2D GUI previews (console, rods, curves, lifecycle)
	public static final int ID_PANEL_MAIN     = 210;  // Primary text panel
	public static final int ID_PANEL_ICON     = 211;  // Secondary icon panel

	public enum RbmkColType {
		FUEL, FUEL_MOD, MODERATOR, REFLECTOR, ABSORBER,
		CONTROL, CONTROL_AUTO, BOILER, OUTGASSER, COOLER, BLANK,
		DEBRIS_SMOLDERING
	}

	public static String tr(String key) {
		return I18n.get(key);
	}

	public static Block blockForColumn(RbmkColType type) {
		return switch (type) {
			case FUEL              -> ModBlocks.RBMK_ROD.get();
			case FUEL_MOD          -> ModBlocks.RBMK_ROD_MOD.get();
			case MODERATOR         -> ModBlocks.RBMK_MODERATOR.get();
			case REFLECTOR         -> ModBlocks.RBMK_REFLECTOR.get();
			case ABSORBER          -> ModBlocks.RBMK_ABSORBER.get();
			case CONTROL           -> ModBlocks.RBMK_CONTROL.get();
			case CONTROL_AUTO      -> ModBlocks.RBMK_CONTROL_AUTO.get();
			case BOILER            -> ModBlocks.RBMK_BOILER.get();
			case OUTGASSER         -> ModBlocks.RBMK_OUTGASSER.get();
			case COOLER            -> ModBlocks.RBMK_COOLER.get();
			case BLANK             -> ModBlocks.RBMK_BLANK.get();
			case DEBRIS_SMOLDERING -> ModBlocks.RBMK_DEBRIS_BURNING.get();
		};
	}

	public static Block steamConnectorBlock() {
		return ModBlocks.RBMK_LOADER.get();
	}

	public static Block spentFuelDrumBlock() {
		return ModBlocks.MACHINE_STORAGE_DRUM.get();
	}

	public static Item coverItem(boolean glassRadiativeSafe) {
		return glassRadiativeSafe ? ModItems.RBMK_LID_GLASS.get() : ModItems.RBMK_LID.get();
	}

	/**
	 * Creates an {@link ActorRbmkMachine} renderer for the given RBMK column type,
	 * syncing {@code lidState} (0=none, 1=concrete, 2=glass) and {@code rodLevel} (0.0..1.0)
	 * from actor NBT data.
	 */
	public static ActorRbmkMachine<BlockEntity> columnRenderer(RbmkColType type) {
		BlockState state = blockForColumn(type).defaultBlockState();
		return new ActorRbmkMachine<>((p, f) -> switch (type) {
			case FUEL, FUEL_MOD -> new RBMKRodBlockEntity(p, state);
			case MODERATOR      -> new RBMKModeratorBlockEntity(p, state);
			case REFLECTOR      -> new RBMKReflectorBlockEntity(p, state);
			case ABSORBER       -> new RBMKAbsorberBlockEntity(p, state);
			case CONTROL        -> new RBMKControlManualBlockEntity(p, state);
			case CONTROL_AUTO   -> new RBMKControlAutoBlockEntity(p, state);
			case BOILER         -> new RBMKBoilerBlockEntity(p, state);
			case OUTGASSER      -> new RBMKOutgasserBlockEntity(p, state);
			case COOLER         -> new RBMKCoolerBlockEntity(p, state);
			case BLANK, DEBRIS_SMOLDERING -> new RBMKBlankBlockEntity(p, state);
		}, (be, data) -> {
			if (be instanceof RBMKColumnBlockEntity col && data.contains("lidState")) {
				col.setLidState(data.getInt("lidState"));
			}
			if (be instanceof RBMKControlBlockEntity ctrl && data.contains("rodLevel")) {
				double lvl = data.getDouble("rodLevel");
				ctrl.level = lvl;
				ctrl.lastLevel = lvl;
			}
		});
	}

	public static void placeColumn(WiajScene scene, int actorId, int x, int baseY, int z,
			RbmkColType type, int lidState, double rodLevel, boolean smooth) {
		CompoundTag tag = ActorRbmkMachine.posTag(x, baseY, z, Direction.NORTH);
		tag.putInt("lidState", lidState);
		tag.putDouble("rodLevel", rodLevel);
		ActorBase actor = new ActorTileEntity(columnRenderer(type), tag);
		if (smooth) {
			actor.smoothPlace();
		}
		scene.add(new ActionCreateActor(actorId, actor));
	}

	public static void placeColumnBlocks(WiajScene scene, int x, int baseY, int z,
			int colHeight, RbmkColType type) {
		Block b = blockForColumn(type);
		for (int dy = 0; dy < colHeight; dy++) {
			scene.add(new ActionSetBlock(x, baseY + dy, z, b));
		}
	}

	// =========================================================================
	// Fluid ducts, cables, and scene geometry
	// =========================================================================

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
			placeDuct(scene, x, y, z, fluid, false, false, (x < max) || capEnd, (x > min) || capStart, false, false);
		}
	}

	public static void placeDuctLineZ(WiajScene scene, int x, int y, int z1, int z2, Fluid fluid,
			boolean capStart, boolean capEnd) {
		int min = Math.min(z1, z2);
		int max = Math.max(z1, z2);
		for (int z = min; z <= max; z++) {
			placeDuct(scene, x, y, z, fluid, (z > min) || capStart, (z < max) || capEnd, false, false, false, false);
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

	public static void buildIndustrialFloor(WiajScene scene, int sizeX, int sizeZ) {
		for (int x = sizeX - 1; x >= 0; x--) {
			for (int z = 0; z < sizeZ; z++) {
				boolean border = (x == 0 || x == sizeX - 1 || z == 0 || z == sizeZ - 1);
				scene.add(new ActionSetBlock(x, 0, z,
						border ? ModBlocks.CONCRETE_HAZARD.get() : ModBlocks.CONCRETE_SMOOTH.get()));
			}
		}
	}
}
