package com.hbm_m.wiaj.cannery;

import com.hbm_m.api.energy.WireBlock;
import com.hbm_m.api.energy.WireCenterVisual;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.block.machines.MachineFireboxBlock;
import com.hbm_m.block.machines.PowerDetectorBlock;
import com.hbm_m.blockentity.machines.MachineFireboxBlockEntity;
import com.hbm_m.blockentity.machines.MachineStirlingBlockEntity;
import com.hbm_m.item.ModItems;
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
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Firebox & stirling engine demo.
 * <p>
 * Port of {@code com.hbm.wiaj.cannery.CanneryFirebox} (1.7.10). The original actors rendered the
 * machine OBJs directly via {@code ResourceManager}; here both machines render through their
 * registered Nucleus {@code MachineBer} ({@code HeaterRenderers} spec "firebox", {@code
 * MachineEngineRenderer} spec "stirling"), which drive the door swing, the burning inner and the
 * cog rotation off the fake BlockEntities - the same fields the in-world machines use.
 * <p>
 * The original {@code red_cable} connections were drawn by the ISBRH cable renderer probing
 * neighbor connector TileEntities (the four {@code JarDummyConnector}s below); modern
 * {@link WireBlock} carries its connections in the BlockState and {@code VirtualWorld} runs no
 * neighbor updates (and {@code getConnectionState} is private), so every arm is set explicitly -
 * the arms mirror exactly what the original renderer drew toward the dummy/machine positions.
 * The detector's meta 0/1 becomes {@link PowerDetectorBlock#POWERED}.
 */
public class CanneryFirebox extends CanneryBase {

	@Override
	public ItemStack getIcon() {
		return new ItemStack(ModItems.FIREBOX.get());
	}

	@Override
	public String getName() {
		return "cannery.firebox";
	}

	public WiajScript createScript() {
		VirtualWorld world = new VirtualWorld(5, 5, 5);
		WiajScript script = new WiajScript(world);

		WiajScene scene0 = new WiajScene(script);

		scene0.add(new ActionSetZoom(3, 0));

		for(int x = world.sizeX - 1; x >= 0; x--) {
			for(int z = 0; z < world.sizeZ; z++) {
				scene0.add(new ActionSetBlock(x, 0, z, net.minecraft.world.level.block.Blocks.BRICKS));
			}

			scene0.add(new ActionWait(2));
		}

		scene0.add(new ActionWait(8));

		CompoundTag firebox = new CompoundTag();
		firebox.putDouble("x", 2D);
		firebox.putDouble("y", 1D);
		firebox.putDouble("z", 2D);
		firebox.putInt("rotation", 5);
		scene0.add(new ActionCreateActor(0, new ActorTileEntity(new ActorFirebox(), firebox).smoothPlace()));

		scene0.add(new ActionWait(10));

		scene0.add(new ActionCreateActor(1, new ActorFancyPanel(Minecraft.getInstance().font, 0, -10, new Object[][] {{resolveKey("cannery.firebox.0")}}, 150)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene0.add(new ActionWait(60));

		scene0.add(new ActionCreateActor(1, new ActorFancyPanel(Minecraft.getInstance().font, 0, -10, new Object[][] {{resolveKey("cannery.firebox.1")}}, 250)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene0.add(new ActionWait(60));
		scene0.add(new ActionRemoveActor(1));
		scene0.add(new ActionWait(5));
		scene0.add(new ActionUpdateActor(0, "open", true));
		scene0.add(new ActionWait(30));

		scene0.add(new ActionUpdateActor(0, "isOn", true));

		scene0.add(new ActionCreateActor(1, new ActorFancyPanel(Minecraft.getInstance().font, -50, 40, new Object[][] {{new ItemStack(Items.COAL)}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.RIGHT)));
		scene0.add(new ActionWait(10));
		scene0.add(new ActionCreateActor(1, new ActorFancyPanel(Minecraft.getInstance().font, -50, 40, new Object[][] {{new ItemStack(ModItems.COAL_COKE.get())}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.RIGHT)));
		scene0.add(new ActionWait(10));
		scene0.add(new ActionCreateActor(1, new ActorFancyPanel(Minecraft.getInstance().font, -50, 40, new Object[][] {{new ItemStack(ModItems.SOLID_FUEL.get())}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.RIGHT)));
		scene0.add(new ActionWait(10));
		scene0.add(new ActionCreateActor(1, new ActorFancyPanel(Minecraft.getInstance().font, -50, 40, new Object[][] {{new ItemStack(ModItems.ROCKET_FUEL.get())}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.RIGHT)));
		scene0.add(new ActionWait(10));
		scene0.add(new ActionCreateActor(1, new ActorFancyPanel(Minecraft.getInstance().font, -50, 40, new Object[][] {{new ItemStack(ModItems.SOLID_FUEL_BF.get())}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.RIGHT)));
		scene0.add(new ActionWait(10));
		scene0.add(new ActionRemoveActor(1));
		scene0.add(new ActionWait(10));
		scene0.add(new ActionUpdateActor(0, "open", false));
		scene0.add(new ActionWait(30));

		scene0.add(new ActionCreateActor(1, new ActorFancyPanel(Minecraft.getInstance().font, 0, -10, new Object[][] {{resolveKey("cannery.firebox.2")}}, 250)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene0.add(new ActionWait(80));

		scene0.add(new ActionCreateActor(1, new ActorFancyPanel(Minecraft.getInstance().font, 0, -10, new Object[][] {{resolveKey("cannery.firebox.3")}}, 250)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene0.add(new ActionWait(60));
		scene0.add(new ActionRemoveActor(1));
		scene0.add(new ActionWait(10));

		WiajScene scene1 = new WiajScene(script);

		CompoundTag stirling = new CompoundTag();
		stirling.putDouble("x", 2D);
		stirling.putDouble("y", 2D);
		stirling.putDouble("z", 2D);
		stirling.putInt("rotation", 2);
		stirling.putBoolean("hasCog", true);
		scene1.add(new ActionCreateActor(1, new ActorTileEntity(new ActorStirling(), stirling).smoothPlace()));
		scene1.add(new ActionUpdateActor(1, "speed", 0F));

		scene1.add(new ActionWait(10));
		scene1.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, 0, -45, new Object[][] {{resolveKey("cannery.firebox.4")}}, 250)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene1.add(new ActionWait(60));
		scene1.add(new ActionRemoveActor(2));
		scene1.add(new ActionWait(10));

		for(int i = 0; i < 60; i++) {
			scene1.add(new ActionUpdateActor(1, "speed", i / 5F));
			scene1.add(new ActionWait(1));
		}

		// The four invisible connector dummies are kept 1:1 from the original: the cable's
		// visual arms are set explicitly below (the modern cable decides connections in its
		// BlockState and VirtualWorld applies no neighbor updates), but the dummies at the
		// stirling and machine positions still satisfy every renderer that probes neighbor
		// BlockEntities directly.
		scene1.add(new ActionSetTile(1, 2, 2, new Dummies.JarDummyConnector()));
		scene1.add(new ActionSetTile(0, 2, 2, new Dummies.JarDummyConnector()));
		scene1.add(new ActionSetTile(0, 1, 2, new Dummies.JarDummyConnector()));
		scene1.add(new ActionSetTile(0, 1, 3, new Dummies.JarDummyConnector()));
		// (0,2,2): EAST arm into the dummy at (1,2,2), DOWN into the lower cable - the arms the
		// original ISBRH renderer drew. (0,1,2): UP into the upper cable, SOUTH into the detector.
		scene1.add(new ActionSetBlock(0, 2, 2, cable(false, false, true, false, false, true)));
		scene1.add(new ActionSetBlock(0, 1, 2, cable(false, true, false, false, true, false)));
		scene1.add(new ActionSetBlock(0, 1, 3, detector(false)));
		scene1.add(new ActionWait(10));
		scene1.add(new ActionSetBlock(0, 1, 3, detector(true)));
		scene1.add(new ActionWait(100));

		script.addScene(scene0).addScene(scene1);
		return script;
	}

	/** Original {@code I18nUtil.resolveKey}: resolves the key immediately for the panel lines. */
	private static String resolveKey(String key) {
		return net.minecraft.client.resources.language.I18n.get(key);
	}

	/** A red cable state with explicit connections and the matching center visual (see {@code CannerySILEX#duct}). */
	private static BlockState cable(boolean north, boolean south, boolean east, boolean west, boolean up, boolean down) {
		return ModBlocks.RED_CABLE.get().defaultBlockState()
				.setValue(WireBlock.NORTH, north)
				.setValue(WireBlock.SOUTH, south)
				.setValue(WireBlock.EAST, east)
				.setValue(WireBlock.WEST, west)
				.setValue(WireBlock.UP, up)
				.setValue(WireBlock.DOWN, down)
				.setValue(WireBlock.CENTER, WireCenterVisual.JUNCTION);
	}

	/** Original {@code machine_detector} meta 0/1: the modern detector's {@code POWERED} state. */
	private static BlockState detector(boolean powered) {
		return ModBlocks.DETECTOR.get().defaultBlockState().setValue(PowerDetectorBlock.POWERED, powered);
	}

	/**
	 * Renders the firebox through its registered Nucleus {@code MachineBer} ({@code HeaterRenderers}
	 * spec "firebox"): the machine body, the door swinging at the hinge (1.375, 0, 0.375) and the
	 * InnerEmpty/InnerBurning dynamic inner are the same code path as in-world.
	 * <p>
	 * The pose is block-corner based (translate(x, y, z) with no manual rotation) so the spec's
	 * blockTransform applies the {@code FACING} rotation and centering exactly like the in-world
	 * dispatcher does. The door math itself ({@code swingSpeed = angle / 10 + 3}, clamp 0..135) runs
	 * here in {@link #updateActor} exactly like the original actor, and the resulting angles are
	 * copied onto the fake BE so the spec animates the door off
	 * {@link MachineFireboxBaseBlockEntity#getInterpolatedDoorAngle}.
	 * <p>
	 * The fake BE lives at virtual coordinates offset far beyond the world border, so its
	 * {@code BlockPos.asLong()} roster/cache keys can never collide with a real machine.
	 */
	public static class ActorFirebox implements ITileActorRenderer {

		/** X offset isolating the fake BE's pos-keyed Nucleus state (roster slots, caches) from real machines. */
		static final int GUI_POS_OFFSET = 30_000_000;

		/**
		 * Original rotation code (3:0°, 5:90°, 2:180°, 4:270°) → FACING reproducing the same net yaw
		 * via the Nucleus blockTransform (CardinalOrientation: E:0°, N:90°, W:180°, S:270°).
		 */
		static final Direction[] FACING_MAP = {
				Direction.NORTH,
				Direction.NORTH,
				Direction.WEST,
				Direction.EAST,
				Direction.SOUTH,
				Direction.NORTH
		};

		private MachineFireboxBlockEntity firebox;
		private int lastRotation = -1;

		@Override
		public void renderActor(GuiGraphics gui, VirtualWorld world, int ticks, float interp, CompoundTag data) {
			double x = data.getDouble("x");
			double y = data.getDouble("y");
			double z = data.getDouble("z");
			int rotation = data.getInt("rotation");
			boolean isOn = data.getBoolean("isOn");
			float doorAngle = data.getFloat("angle");
			float prevDoorAngle = data.getFloat("lastAngle");

			gui.pose().pushPose();
			gui.pose().translate(x, y, z);

			if(this.firebox == null || this.lastRotation != rotation) {
				this.lastRotation = rotation;
				this.firebox = new MachineFireboxBlockEntity(
						new BlockPos((int) x + GUI_POS_OFFSET, (int) y, (int) z),
						ModBlocks.FIREBOX.get().defaultBlockState().setValue(MachineFireboxBlock.FACING,
								FACING_MAP[Math.min(rotation, FACING_MAP.length - 1)]));
			}
			// The spec reads the swing and the burning state off the fake BE - the same fields the
			// in-world firebox drives, so the door and inner render exactly like the original actor.
			this.firebox.prevDoorAngle = prevDoorAngle;
			this.firebox.doorAngle = doorAngle;
			this.firebox.wasOn = isOn;

			BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
			BlockEntityRenderer<BlockEntity> ber = dispatcher.getRenderer(this.firebox);
			if(ber != null) {
				ber.render(this.firebox, interp, gui.pose(), gui.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			}

			gui.pose().popPose();
		}

		@Override
		public void updateActor(int ticks, CompoundTag data) {

			boolean open = data.getBoolean("open");
			float doorAngle = data.getFloat("angle");
			data.putFloat("lastAngle", doorAngle);

			float swingSpeed = (doorAngle / 10F) + 3;

			if(open) {
				doorAngle += swingSpeed;
			} else {
				doorAngle -= swingSpeed;
			}

			doorAngle = Mth.clamp(doorAngle, 0F, 135F);
			data.putFloat("angle", doorAngle);
		}
	}

	/**
	 * Renders the stirling engine through its registered Nucleus {@code MachineBer}
	 * ({@code MachineEngineRenderer} spec "stirling"): base, cog (only with {@code hasCog}, rotating
	 * with {@code -rot}), small cog ({@code rot * 2 + 3}) and the piston - the same code path as
	 * in-world, driven off the fake BE's public {@code spin}/{@code lastSpin} fields exactly like
	 * the original {@code RenderStirling} TESR branch.
	 * <p>
	 * The spin integration stays in the actor data tag ({@code speed} → {@code spin}/{@code
	 * lastSpin}), so rewinding re-seeds it like the original. The original {@code type} 0/1
	 * (normal/steel) maps to two separate blocks in Modernized; this scene only uses the normal
	 * {@code STIRLING} block ({@code getGeatMeta()} 0).
	 */
	public static class ActorStirling implements ITileActorRenderer {

		private MachineStirlingBlockEntity stirling;
		private int lastRotation = -1;

		@Override
		public void renderActor(GuiGraphics gui, VirtualWorld world, int ticks, float interp, CompoundTag data) {
			double x = data.getDouble("x");
			double y = data.getDouble("y");
			double z = data.getDouble("z");
			int rotation = data.getInt("rotation");

			gui.pose().pushPose();
			gui.pose().translate(x, y, z);

			if(this.stirling == null || this.lastRotation != rotation) {
				this.lastRotation = rotation;
				this.stirling = new MachineStirlingBlockEntity(
						new BlockPos((int) x + ActorFirebox.GUI_POS_OFFSET, (int) y, (int) z),
						ModBlocks.STIRLING.get().defaultBlockState().setValue(DummyableMachineBlock.FACING,
								ActorFirebox.FACING_MAP[Math.min(rotation, ActorFirebox.FACING_MAP.length - 1)]));
				this.stirling.setHasCog(data.getBoolean("hasCog"));
			}
			// The spec lerps the cog/piston animation off lastSpin/spin exactly like the original
			// renderCommon(lastSpin + (spin - lastSpin) * interp, ...).
			this.stirling.lastSpin = data.getFloat("lastSpin");
			this.stirling.spin = data.getFloat("spin");

			BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
			BlockEntityRenderer<BlockEntity> ber = dispatcher.getRenderer(this.stirling);
			if(ber != null) {
				ber.render(this.stirling, interp, gui.pose(), gui.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			}

			gui.pose().popPose();
		}

		@Override
		public void updateActor(int ticks, CompoundTag data) {

			float lastSpin = 0;
			float spin = data.getFloat("spin");
			float speed = data.getFloat("speed");

			lastSpin = spin;
			spin += speed;

			if(spin >= 360) {
				lastSpin -= 360;
				spin -= 360;
			}

			data.putFloat("lastSpin", lastSpin);
			data.putFloat("spin", spin);
		}
	}
}
