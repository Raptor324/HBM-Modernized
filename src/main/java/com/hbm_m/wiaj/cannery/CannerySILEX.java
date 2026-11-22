package com.hbm_m.wiaj.cannery;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.BarrelTcalloyBlockEntity;
import com.hbm_m.blockentity.machines.FluidDuctBlockEntity;
import com.hbm_m.blockentity.machines.MachineFelBlockEntity;
import com.hbm_m.blockentity.machines.MachineSilexBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;
import com.hbm_m.wiaj.WiajScript;
import com.hbm_m.wiaj.action.ActionCreateActor;
import com.hbm_m.wiaj.action.ActionRemoveActor;
import com.hbm_m.wiaj.action.ActionSetBlock;
import com.hbm_m.wiaj.action.ActionSetTile;
import com.hbm_m.wiaj.action.ActionSetZoom;
import com.hbm_m.wiaj.action.ActionUpdateActor;
import com.hbm_m.wiaj.action.ActionWait;
import com.hbm_m.wiaj.actor.ActorFancyPanel;
import com.hbm_m.wiaj.actor.ActorTileEntity;
import com.hbm_m.wiaj.actor.ITileActorRenderer;
import com.hbm_m.wiaj.actor.ActorFancyPanel.Orientation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * FEL & SILEX demo.
 * <p>
 * Port of {@code com.hbm.wiaj.cannery.CannerySILEX} (1.7.10). The original actors rendered
 * the machine models directly via {@code ResourceManager}; here the machines render through
 * their registered Nucleus {@code MachineBer} (per-BE VBO tier inside the demo screen), so
 * the FEL beam and any machine animations come straight from the in-world render spec.
 * <p>
 * The original wave {@code mode} indices 1-5 map to {IR, VISIBLE, UV, GAMMA, DRX}; the modern
 * {@link EnumWavelengths} has an extra {@code XRAY} entry between UV and GAMMA, hence the
 * explicit {@link ActorFEL#MODE_MAP}.
 */
public class CannerySILEX extends CanneryBase {

	@Override
	public ItemStack getIcon() {
		return new ItemStack(ModItems.SILEX.get());
	}

	@Override
	public String getName() {
		return "cannery.silex";
	}

	public WiajScript createScript() {
		VirtualWorld world = new VirtualWorld(17, 5, 5);
		WiajScript script = new WiajScript(world);

		WiajScene scene0 = new WiajScene(script);

		scene0.add(new ActionSetZoom(2, 0));

		for(int x = world.sizeX - 1; x >= 0; x--) {
			for(int z = 0; z < world.sizeZ; z++) {
				scene0.add(new ActionSetBlock(x, 0, z, net.minecraft.world.level.block.Blocks.BRICKS));
			}

			scene0.add(new ActionWait(1));
		}

		scene0.add(new ActionWait(9));

		CompoundTag fel = new CompoundTag();
		fel.putDouble("x", 11D);
		fel.putDouble("y", 1D);
		fel.putDouble("z", 2D);
		fel.putInt("rotation", 5);
		fel.putInt("length", 11);
		scene0.add(new ActionCreateActor(0, new ActorTileEntity(new ActorFEL(), fel).smoothPlace()));

		scene0.add(new ActionCreateActor(3, new ActorFancyPanel(Minecraft.getInstance().font, 15, -5, new Object[][] {{resolveKey("cannery.silex.0")}}, 100)
				.setColors(colorCopper).setOrientation(Orientation.LEFT)));

		scene0.add(new ActionWait(80));
		scene0.add(new ActionRemoveActor(3));
		scene0.add(new ActionWait(10));

		scene0.add(new ActionCreateActor(3, new ActorFancyPanel(Minecraft.getInstance().font, -7, -15, new Object[][] {{new ItemStack(ModItems.LASER_CRYSTAL_CO2.get())}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionUpdateActor(0, "mode", 1));
		scene0.add(new ActionWait(20));
		scene0.add(new ActionCreateActor(3, new ActorFancyPanel(Minecraft.getInstance().font, -7, -15, new Object[][] {{new ItemStack(ModItems.LASER_CRYSTAL_BISMUTH.get())}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionUpdateActor(0, "mode", 2));
		scene0.add(new ActionWait(20));
		scene0.add(new ActionCreateActor(3, new ActorFancyPanel(Minecraft.getInstance().font, -7, -15, new Object[][] {{new ItemStack(ModItems.LASER_CRYSTAL_CMB.get())}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionUpdateActor(0, "mode", 3));
		scene0.add(new ActionWait(20));
		scene0.add(new ActionCreateActor(3, new ActorFancyPanel(Minecraft.getInstance().font, -7, -15, new Object[][] {{new ItemStack(ModItems.LASER_CRYSTAL_DNT.get())}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionUpdateActor(0, "mode", 4));
		scene0.add(new ActionWait(20));
		scene0.add(new ActionCreateActor(3, new ActorFancyPanel(Minecraft.getInstance().font, -7, -15, new Object[][] {{new ItemStack(ModItems.LASER_CRYSTAL_DIGAMMA.get())}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionUpdateActor(0, "mode", 5));
		scene0.add(new ActionWait(20));

		scene0.add(new ActionRemoveActor(3));
		scene0.add(new ActionUpdateActor(0, "mode", 0));
		scene0.add(new ActionWait(20));

		for(int y = 1; y < 4; y++) {
			for(int z = 1; z < 4; z++) {
				scene0.add(new ActionSetBlock(5, y, z, net.minecraft.world.level.block.Blocks.STONE));
			}
			scene0.add(new ActionWait(5));
		}

		scene0.add(new ActionCreateActor(3, new ActorFancyPanel(Minecraft.getInstance().font, -7, -15, new Object[][] {{resolveKey("cannery.silex.1")}}, 150)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionUpdateActor(0, "mode", 3));
		scene0.add(new ActionUpdateActor(0, "length", 4));

		scene0.add(new ActionWait(40));
		scene0.add(new ActionUpdateActor(0, "length", 11));
		scene0.add(new ActionSetBlock(5, 2, 2, net.minecraft.world.level.block.Blocks.FIRE));
		scene0.add(new ActionWait(10));
		scene0.add(new ActionSetBlock(5, 2, 2, net.minecraft.world.level.block.Blocks.AIR));
		scene0.add(new ActionWait(40));

		for(int y = 1; y < 4; y++) {
			for(int z = 1; z < 4; z++) {
				scene0.add(new ActionSetBlock(5, y, z, ModBlocks.BRICK_CONCRETE.get()));
			}

			if(y == 2) {
				scene0.add(new ActionUpdateActor(0, "length", 4));
			}

			scene0.add(new ActionWait(5));
		}

		scene0.add(new ActionCreateActor(3, new ActorFancyPanel(Minecraft.getInstance().font, -7, -15, new Object[][] {{resolveKey("cannery.silex.2")}}, 150)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionWait(40));
		scene0.add(new ActionRemoveActor(3));

		for(int y = 3; y > 0; y--) {
			for(int z = 1; z < 4; z++) {
				scene0.add(new ActionSetBlock(5, y, z, net.minecraft.world.level.block.Blocks.AIR));
			}

			if(y == 2) {
				scene0.add(new ActionUpdateActor(0, "length", 11));
			}

			scene0.add(new ActionWait(5));
		}

		scene0.add(new ActionUpdateActor(0, "mode", 0));

		WiajScene scene1 = new WiajScene(script);

		CompoundTag silex0 = new CompoundTag();
		silex0.putDouble("x", 5D);
		silex0.putDouble("y", 1D);
		silex0.putDouble("z", 2D);
		silex0.putInt("rotation", 2);
		scene1.add(new ActionCreateActor(1, new ActorTileEntity(new ActorSILEX(), silex0).smoothPlace()));

		scene1.add(new ActionWait(20));

		scene1.add(new ActionCreateActor(3, new ActorFancyPanel(Minecraft.getInstance().font, 42, -18, new Object[][] {{resolveKey("cannery.silex.3")}}, 150)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene1.add(new ActionWait(80));

		scene1.add(new ActionCreateActor(3, new ActorFancyPanel(Minecraft.getInstance().font, 60, 32, new Object[][] {{resolveKey("cannery.silex.4")}}, 150)
				.setColors(colorCopper).setOrientation(Orientation.RIGHT)));

		scene1.add(new ActionWait(60));

		scene1.add(new ActionCreateActor(3, new ActorFancyPanel(Minecraft.getInstance().font, 12, 32, new Object[][] {{resolveKey("cannery.silex.5")}}, 150)
				.setColors(colorCopper).setOrientation(Orientation.RIGHT)));

		scene1.add(new ActionWait(60));
		scene1.add(new ActionRemoveActor(3));
		scene1.add(new ActionWait(10));

		// Duct runs with explicit connection states - the original placed a peroxide pipe
		// TileEntity on every duct cell (the TE carried the fluid the ISBRH renderer tinted the
		// overlay pass with) plus invisible JarDummyConnectors so the pipes joined up and reached
		// into the barrel; modern ducts carry connections in the BlockState instead
		// and VirtualWorld applies no neighbor updates, so every arm is set explicitly and
		// reciprocally (the joint toward the west run, the vertical joints and the two
		// +z connector stubs mirror the original dummy-connector positions). The tint TE maps to
		// a real FluidDuctBlockEntity with the same fluid type (see CanneryCentrifuge) - one BE
		// per cell, since the modern BE's pos must match its cell.
		// WEST of (5,1,0) is false here: in the original the west arm did not exist yet, the
		// (4,1,0) duct only appeared in the 2nd-run loop below.
		BlockState duct520 = duct(false, true, false, false, false, true);
		FluidDuctBlockEntity tile520 = new FluidDuctBlockEntity(new BlockPos(5, 2, 0), duct520);
		tile520.setFluidTypeSilent(ModFluids.PEROXIDE.getSource());
		scene1.add(new ActionSetTile(5, 2, 0, tile520));
		BlockState duct510 = duct(false, false, true, false, true, false);
		FluidDuctBlockEntity tile510 = new FluidDuctBlockEntity(new BlockPos(5, 1, 0), duct510);
		tile510.setFluidTypeSilent(ModFluids.PEROXIDE.getSource());
		scene1.add(new ActionSetTile(5, 1, 0, tile510));
		BlockState duct610 = duct(false, false, true, true, false, false);
		FluidDuctBlockEntity tile610 = new FluidDuctBlockEntity(new BlockPos(6, 1, 0), duct610);
		tile610.setFluidTypeSilent(ModFluids.PEROXIDE.getSource());
		scene1.add(new ActionSetTile(6, 1, 0, tile610));
		scene1.add(new ActionSetBlock(5, 2, 0, duct520));
		scene1.add(new ActionSetBlock(5, 1, 0, duct510));
		scene1.add(new ActionSetBlock(6, 1, 0, duct610));
		CompoundTag barrel = new CompoundTag();
		barrel.putDouble("x", 7D);
		barrel.putDouble("y", 1D);
		barrel.putDouble("z", 0D);
		// id 11, NOT 1: the original 1.7.10 scene had no barrel actor at all (its barrel is part
		// of the block/tile pass) and id 1 belongs to the first SILEX for the whole scene - the
		// port's barrel-as-actor inherited id 1 and replaced that SILEX the moment it appeared
		scene1.add(new ActionCreateActor(11, new ActorTileEntity(new ActorBarrel(), barrel).smoothPlace()));

		scene1.add(new ActionWait(20));
		scene1.add(new ActionUpdateActor(0, "mode", 3));
		scene1.add(new ActionWait(10));

		scene1.add(new ActionCreateActor(3, new ActorFancyPanel(Minecraft.getInstance().font, 42, -18, new Object[][] {{resolveKey("cannery.silex.6")}}, 150)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene1.add(new ActionWait(80));

		scene1.add(new ActionCreateActor(3, new ActorFancyPanel(Minecraft.getInstance().font, 42, -18, new Object[][] {{resolveKey("cannery.silex.7")}}, 150)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene1.add(new ActionWait(60));

		scene1.add(new ActionCreateActor(3, new ActorFancyPanel(Minecraft.getInstance().font, -7, -15, new Object[][] {{resolveKey("cannery.silex.8")}}, 150)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene1.add(new ActionWait(60));
		scene1.add(new ActionRemoveActor(3));
		scene1.add(new ActionWait(10));

		CompoundTag silex1 = new CompoundTag();
		silex1.putDouble("x", 1D);
		silex1.putDouble("y", 1D);
		silex1.putDouble("z", 2D);
		silex1.putInt("rotation", 2);
		scene1.add(new ActionCreateActor(2, new ActorTileEntity(new ActorSILEX(), silex1).smoothPlace()));
		scene1.add(new ActionWait(10));

		for(int i = 1; i < 5; i++) {
			// i == 1: no west neighbor (original drew no stub into air) but UP to (1,2,0)
			BlockState ductState = i == 1 ? duct(false, false, true, false, true, false) : duct(false, false, true, true, false, false);
			FluidDuctBlockEntity ductTile = new FluidDuctBlockEntity(new BlockPos(i, 1, 0), ductState);
			ductTile.setFluidTypeSilent(ModFluids.PEROXIDE.getSource());
			scene1.add(new ActionSetTile(i, 1, 0, ductTile));
			scene1.add(new ActionSetBlock(i, 1, 0, ductState));
		}
		// SOUTH stub mirrors the original JarDummyConnector at (1,2,1); DOWN joins (1,1,0) whose UP arm is set above
		BlockState duct120 = duct(false, true, false, false, false, true);
		FluidDuctBlockEntity tile120 = new FluidDuctBlockEntity(new BlockPos(1, 2, 0), duct120);
		tile120.setFluidTypeSilent(ModFluids.PEROXIDE.getSource());
		scene1.add(new ActionSetTile(1, 2, 0, tile120));
		scene1.add(new ActionSetBlock(1, 2, 0, duct120));
		// The original recomputed connections every render (Library.canConnectFluid on live
		// neighbors), so once the (4,1,0) duct above appeared, the (5,1,0) duct grew its west
		// arm. All ActionSetBlock actions are 0-duration and chain in the same tick
		// (WiajScene.tick), so the west arm becomes visible in exactly the same tick the
		// (4,1,0) block appears - the original's per-render visibility boundary.
		scene1.add(new ActionSetBlock(5, 1, 0, duct(false, false, true, true, true, false)));

		scene1.add(new ActionWait(20));

		script.addScene(scene0).addScene(scene1);
		return script;
	}

	/** Original {@code I18nUtil.resolveKey}: resolves the key immediately for the panel lines. */
	private static String resolveKey(String key) {
		return net.minecraft.client.resources.language.I18n.get(key);
	}

	/** A fluid duct state with explicit connections and the matching render shape. */
	private static net.minecraft.world.level.block.state.BlockState duct(boolean north, boolean south, boolean east, boolean west, boolean up, boolean down) {
		return ModBlocks.FLUID_DUCT.get().defaultBlockState()
				.setValue(com.hbm_m.block.machines.FluidDuctBlock.NORTH, north)
				.setValue(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, south)
				.setValue(com.hbm_m.block.machines.FluidDuctBlock.EAST, east)
				.setValue(com.hbm_m.block.machines.FluidDuctBlock.WEST, west)
				.setValue(com.hbm_m.block.machines.FluidDuctBlock.UP, up)
				.setValue(com.hbm_m.block.machines.FluidDuctBlock.DOWN, down)
				.setValue(com.hbm_m.block.machines.FluidDuctBlock.SHAPE,
						com.hbm_m.block.machines.PipeRenderShape.fromConnections(north, south, east, west, up, down));
	}

	/**
	 * Renders the FEL through its registered Nucleus {@code MachineBer}: the machine model, the
	 * {@code Cube.018} parts and the coherent beam hook are the same code path as in-world.
	 * <p>
	 * The pose is block-corner based (translate(x, y, z) with no manual rotation) so the
	 * spec's blockTransform applies the {@code FACING} rotation and centering exactly like
	 * the in-world dispatcher does - the beam origin then matches the original demo actor.
	 * The fake BE lives at virtual coordinates offset far beyond the world border, so its
	 * {@code BlockPos.asLong()} roster/cache keys can never collide with a real machine.
	 */
	public static class ActorFEL implements ITileActorRenderer {

		/** Original wave index (1-5) → modern {@link EnumWavelengths} (XRAY inserted at 4). */
		static final EnumWavelengths[] MODE_MAP = {
				EnumWavelengths.NULL,
				EnumWavelengths.IR,
				EnumWavelengths.VISIBLE,
				EnumWavelengths.UV,
				EnumWavelengths.GAMMA,
				EnumWavelengths.DRX
		};

		/** Original rotation code (3:0°, 5:90°, 2:180°, 4:270°) → FACING reproducing the same net yaw via {@code applyFelTransform}. */
		static final net.minecraft.core.Direction[] FACING_MAP = {
				net.minecraft.core.Direction.NORTH,
				net.minecraft.core.Direction.NORTH,
				net.minecraft.core.Direction.SOUTH,
				net.minecraft.core.Direction.NORTH,
				net.minecraft.core.Direction.EAST,
				net.minecraft.core.Direction.WEST
		};

		/** X offset isolating the fake BE's pos-keyed Nucleus state (roster slots, caches) from real machines. */
		static final int GUI_POS_OFFSET = 30_000_000;

		private MachineFelBlockEntity fel;
		private int lastRotation = -1;

		@Override
		public void renderActor(GuiGraphics gui, VirtualWorld world, int ticks, float interp, CompoundTag data) {
			double x = data.getDouble("x");
			double y = data.getDouble("y");
			double z = data.getDouble("z");
			int rotation = data.getInt("rotation");
			int mode = data.getInt("mode");
			int length = data.getInt("length");

			gui.pose().pushPose();
			gui.pose().translate(x, y, z);

			if(this.fel == null || this.lastRotation != rotation) {
				this.lastRotation = rotation;
				this.fel = new MachineFelBlockEntity(
						new BlockPos((int) x + GUI_POS_OFFSET, (int) y, (int) z),
						ModBlocks.FEL.get().defaultBlockState().setValue(com.hbm_m.block.machines.MachineFelBlock.FACING,
								FACING_MAP[Math.min(rotation, FACING_MAP.length - 1)]));
			}
			// Original guard: mode > 0 && mode < 6 renders a beam, anything else none.
			this.fel.mode = (mode > 0 && mode < MODE_MAP.length) ? MODE_MAP[mode] : EnumWavelengths.NULL;
			this.fel.isOn = this.fel.mode != EnumWavelengths.NULL;
			// The FEL hook derives the beam length as distance - 3, exactly like in-world.
			this.fel.distance = length + 3;
			this.fel.setEnergyStored(MachineFelBlockEntity.maxPower);

			BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
			BlockEntityRenderer<BlockEntity> ber = dispatcher.getRenderer(this.fel);
			if(ber != null) {
				ber.render(this.fel, interp, gui.pose(), gui.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			}

			gui.pose().popPose();
		}

		@Override
		public void updateActor(int ticks, CompoundTag data) { }
	}

	/**
	 * Renders the SILEX through its registered Nucleus {@code MachineBer}, corner pose + FACING
	 * (see {@link ActorFEL}). Rotation codes map through {@code applySilexTransform} (E:0°, N:90°,
	 * W:180°, S:270°) to reproduce the original net yaw per code.
	 */
	public static class ActorSILEX implements ITileActorRenderer {

		static final net.minecraft.core.Direction[] FACING_MAP = {
				net.minecraft.core.Direction.NORTH,
				net.minecraft.core.Direction.NORTH,
				net.minecraft.core.Direction.WEST,
				net.minecraft.core.Direction.EAST,
				net.minecraft.core.Direction.SOUTH,
				net.minecraft.core.Direction.NORTH
		};

		private MachineSilexBlockEntity silex;
		private int lastRotation = -1;

		@Override
		public void renderActor(GuiGraphics gui, VirtualWorld world, int ticks, float interp, CompoundTag data) {
			double x = data.getDouble("x");
			double y = data.getDouble("y");
			double z = data.getDouble("z");
			int rotation = data.getInt("rotation");

			gui.pose().pushPose();
			gui.pose().translate(x, y, z);

			if(this.silex == null || this.lastRotation != rotation) {
				this.lastRotation = rotation;
				this.silex = new MachineSilexBlockEntity(
						new BlockPos((int) x + ActorFEL.GUI_POS_OFFSET, (int) y, (int) z),
						ModBlocks.SILEX.get().defaultBlockState().setValue(com.hbm_m.block.machines.DummyableMachineBlock.FACING,
								FACING_MAP[Math.min(rotation, FACING_MAP.length - 1)]));
			}

			BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
			BlockEntityRenderer<BlockEntity> ber = dispatcher.getRenderer(this.silex);
			if(ber != null) {
				ber.render(this.silex, interp, gui.pose(), gui.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			}

			gui.pose().popPose();
		}

		@Override
		public void updateActor(int ticks, CompoundTag data) { }
	}

	/**
	 * Renders the Technetium Steel Barrel through its registered Nucleus {@code MachineBer}:
	 * block-corner pose with the spec's blockTransform applying the centering T(0.5, 0, 0.5),
	 * so the barrel aligns precisely with the duct at (6, 1, 0) instead of being shifted
	 * diagonally by the uncentered block model.
	 */
	public static class ActorBarrel implements ITileActorRenderer {

		private final net.minecraft.world.level.material.Fluid fluid;
		private BarrelTcalloyBlockEntity barrel;

		public ActorBarrel() {
			this(ModFluids.PEROXIDE.getSource());
		}

		public ActorBarrel(net.minecraft.world.level.material.Fluid fluid) {
			this.fluid = fluid;
		}

		@Override
		public void renderActor(GuiGraphics gui, VirtualWorld world, int ticks, float interp, CompoundTag data) {
			double x = data.getDouble("x");
			double y = data.getDouble("y");
			double z = data.getDouble("z");

			gui.pose().pushPose();
			gui.pose().translate(x, y, z);

			if (this.barrel == null) {
				this.barrel = new BarrelTcalloyBlockEntity(
						new BlockPos((int) x + ActorFEL.GUI_POS_OFFSET, (int) y, (int) z),
						ModBlocks.BARREL_TCALLOY.get().defaultBlockState());
				if (this.fluid != null) {
					this.barrel.getFluidTank().setTankType(this.fluid);
					this.barrel.getFluidTank().setFill(1000);
				}
			}

			BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
			BlockEntityRenderer<BlockEntity> ber = dispatcher.getRenderer(this.barrel);
			if (ber != null) {
				ber.render(this.barrel, interp, gui.pose(), gui.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			}

			gui.pose().popPose();
		}

		@Override
		public void updateActor(int ticks, CompoundTag data) { }
	}
}
