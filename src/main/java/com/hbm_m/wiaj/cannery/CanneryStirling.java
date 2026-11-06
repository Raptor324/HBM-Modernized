package com.hbm_m.wiaj.cannery;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.api.energy.WireBlock;
import com.hbm_m.api.energy.WireCenterVisual;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.block.machines.PowerDetectorBlock;
import com.hbm_m.blockentity.machines.MachineOilburnerBlockEntity;
import com.hbm_m.blockentity.machines.MachineStirlingBlockEntity;
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
import com.hbm_m.wiaj.cannery.CanneryFirebox.ActorFirebox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stirling Engine demo.
 * <p>
 * Port of {@code com.hbm.wiaj.cannery.CanneryStirling} (1.7.10). The original actors rendered
 * the machine models directly via {@code ResourceManager} ({@code RenderStirling} as an
 * {@code ITileActorRenderer}, and the local {@code ActorBurner} for the oil burner); here the
 * Stirling renders through its registered Nucleus {@code MachineBer} by driving the fake
 * {@link MachineStirlingBlockEntity}'s {@code spin}/{@code lastSpin}/{@code hasCog} exactly
 * like the original data flags, and the oil burner - which has no BlockEntityRenderer in
 * Modernized, its visuals are the baked blockstate model - is drawn via
 * {@code BlockRenderDispatcher#renderSingleBlock}, the modern equivalent of
 * {@code ResourceManager.heater_oilburner.renderAll()}.
 * <p>
 * The original {@code type} 0/1 (normal/steel) was one block's meta; Modernized ships two
 * separate blocks ({@code STIRLING}/{@code STIRLING_STEEL}), so the actor recreates the fake
 * BlockEntity with the matching block state.
 * <p>
 * The red cable run mirrors the original dummy-connector layout 1:1, but modern
 * {@code WireBlock} carries its visual connections in the BlockState and VirtualWorld applies
 * no neighbor updates, so every arm is set explicitly via {@link #cable} (same mechanism
 * change as the ducts in {@code CannerySILEX}); scene1 additionally re-states the middle
 * cable whose arms change when the run moves up a block - the 1.7.10 renderer recomputed
 * connections every frame, a static BlockState cannot.
 */
public class CanneryStirling extends CanneryBase {

	@Override
	public ItemStack getIcon() {
		return new ItemStack(ModBlocks.STIRLING.get());
	}

	@Override
	public String getName() {
		return "cannery.stirling";
	}

	public CanneryBase[] seeAlso() {
		return new CanneryBase[] {
				new CanneryFirebox()
		};
	}

	@Override
	public WiajScript createScript() {

		VirtualWorld world = new VirtualWorld(5, 5, 5);
		WiajScript script = new WiajScript(world);

		WiajScene scene0 = new WiajScene(script);

		scene0.add(new ActionSetZoom(3, 0));

		for(int x = world.sizeX - 1; x >= 0; x--) {
			for(int z = 0; z < world.sizeZ; z++) {
				scene0.add(new ActionSetBlock(x, 0, z, Blocks.BRICKS));
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

		CompoundTag stirling = new CompoundTag();
		stirling.putDouble("x", 2D);
		stirling.putDouble("z", 2D);
		stirling.putInt("rotation", 2);
		scene0.add(new ActionCreateActor(1, new ActorTileEntity(new ActorStirling(), stirling).smoothPlace()));
		/*
		 * When rewinding, the NBT tag persists. We have to manually set all variable values with UpdateActor.
		 */
		scene0.add(new ActionUpdateActor(1, "speed", 0F));
		scene0.add(new ActionUpdateActor(1, "y", 2D));
		scene0.add(new ActionUpdateActor(1, "type", 0));
		scene0.add(new ActionUpdateActor(1, "hasCog", true));
		scene0.add(new ActionUpdateActor(1, "spin", 0F));
		scene0.add(new ActionUpdateActor(1, "lastSpin", 0F));

		scene0.add(new ActionWait(10));

		scene0.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, 0, -45, new Object[][] {{resolveKey("cannery.stirling.0")}}, 250)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene0.add(new ActionWait(60));

		scene0.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, 0, 40, new Object[][] {{resolveKey("cannery.stirling.1")}}, 250)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene0.add(new ActionWait(60));
		scene0.add(new ActionRemoveActor(2));
		scene0.add(new ActionWait(5));
		scene0.add(new ActionUpdateActor(0, "open", true));
		scene0.add(new ActionWait(30));
		scene0.add(new ActionUpdateActor(0, "isOn", true));
		scene0.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, -50, 40, new Object[][] {{new ItemStack(net.minecraft.world.item.Items.COAL)}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.RIGHT)));
		scene0.add(new ActionWait(20));
		scene0.add(new ActionRemoveActor(2));
		scene0.add(new ActionWait(10));
		scene0.add(new ActionUpdateActor(0, "open", false));
		scene0.add(new ActionWait(30));

		for(int i = 0; i < 60; i++) {
			scene0.add(new ActionUpdateActor(1, "speed", i / 5F));
			scene0.add(new ActionWait(1));
		}

		scene0.add(new ActionWait(20));

		// Cable run with explicit connection states - the original placed invisible
		// JarDummyConnectors so the 1.7.10 ISBRH cable renderer joined the run across air
		// and into the machines; modern cables carry connections in the BlockState instead
		// and VirtualWorld applies no neighbor updates, so every arm is set explicitly
		// (the dummy positions themselves are mirrored 1:1 below).
		scene0.add(new ActionSetTile(1, 2, 2, new Dummies.JarDummyConnector()));
		scene0.add(new ActionSetTile(0, 2, 2, new Dummies.JarDummyConnector()));
		scene0.add(new ActionSetTile(0, 1, 2, new Dummies.JarDummyConnector()));
		scene0.add(new ActionSetTile(0, 1, 3, new Dummies.JarDummyConnector()));
		// (0,2,2): DOWN to (0,1,2), EAST to the invisible dummy at (1,2,2) next to the engine
		scene0.add(new ActionSetBlock(0, 2, 2, cable(false, false, true, false, false, true)));
		// (0,1,2): UP to (0,2,2), SOUTH into the detector at (0,1,3)
		scene0.add(new ActionSetBlock(0, 1, 2, cable(false, true, false, false, true, false)));
		// Original meta 0 = off
		scene0.add(new ActionSetBlock(0, 1, 3, detector(false)));
		scene0.add(new ActionWait(10));
		// Original meta 1 = on
		scene0.add(new ActionSetBlock(0, 1, 3, detector(true)));
		scene0.add(new ActionWait(40));

		WiajScene scene1 = new WiajScene(script);

		scene1.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, 0, -45, new Object[][] {{resolveKey("cannery.stirling.2")}}, 250)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene1.add(new ActionWait(60));
		scene1.add(new ActionRemoveActor(2));

		CompoundTag burner = new CompoundTag();
		burner.putDouble("x", 2D);
		burner.putDouble("y", 1D);
		burner.putDouble("z", 2D);
		burner.putInt("rotation", 5);
		scene1.add(new ActionCreateActor(0, new ActorTileEntity(new ActorBurner(), burner).smoothPlace()));
		scene1.add(new ActionUpdateActor(1, "y", 3D));
		scene1.add(new ActionSetTile(1, 2, 2, null));
		scene1.add(new ActionSetTile(1, 3, 2, new Dummies.JarDummyConnector()));
		scene1.add(new ActionSetTile(0, 3, 2, new Dummies.JarDummyConnector()));
		// (0,3,2): DOWN to (0,2,2), EAST to the invisible dummy at (1,3,2) next to the engine
		scene1.add(new ActionSetBlock(0, 3, 2, cable(false, false, true, false, false, true)));
		// The 1.7.10 renderer recomputed the middle cable's arms every frame - after the dummy
		// at (1,2,2) is removed and (0,3,2) appears, (0,2,2) runs straight UP+DOWN; a static
		// BlockState has to be re-stated explicitly.
		scene1.add(new ActionSetBlock(0, 2, 2, cable(false, false, false, false, true, true)));

		for(int i = 0; i < 100; i++) {
			scene1.add(new ActionUpdateActor(1, "speed", (i + 60) / 5F));
			scene1.add(new ActionWait(1));
		}

		scene1.add(new ActionWait(20));
		scene1.add(new ActionUpdateActor(1, "hasCog", false));
		scene1.add(new ActionSetBlock(0, 1, 3, detector(false)));

		for(int i = 0; i < 160; i += 10) {
			scene1.add(new ActionUpdateActor(1, "speed", (160 - i) / 5F));
			scene1.add(new ActionWait(1));
		}
		scene1.add(new ActionUpdateActor(1, "speed", 0F));

		scene1.add(new ActionWait(20));

		scene1.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, 0, -45, new Object[][] {{resolveKey("cannery.stirling.3")}}, 250)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
		scene1.add(new ActionWait(60));
		scene1.add(new ActionRemoveActor(2));
		scene1.add(new ActionWait(20));
		scene1.add(new ActionUpdateActor(1, "hasCog", true));
		// Original type 1 (steel) is the separate STIRLING_STEEL block in Modernized -
		// the actor recreates its fake BE from the block state.
		scene1.add(new ActionUpdateActor(1, "type", 1));
		scene1.add(new ActionWait(20));

		for(int i = 0; i < 60; i++) {
			scene1.add(new ActionUpdateActor(1, "speed", i / 5F));
			scene1.add(new ActionWait(1));
		}

		scene1.add(new ActionSetBlock(0, 1, 3, detector(true)));
		scene1.add(new ActionWait(100));

		script.addScene(scene0).addScene(scene1);
		return script;
	}

	/** Original {@code I18nUtil.resolveKey}: resolves the key immediately for the panel lines. */
	private static String resolveKey(String key) {
		return net.minecraft.client.resources.language.I18n.get(key);
	}

	/**
	 * A red cable state with explicit connections and the matching center visual -
	 * {@code WireBlock#getConnectionState} is private and needs a LevelAccessor, and
	 * VirtualWorld applies no neighbor updates, so the demo sets the arms by hand.
	 */
	private static BlockState cable(boolean north, boolean south, boolean east, boolean west, boolean up, boolean down) {
		WireCenterVisual center = up && down && !north && !south && !east && !west ? WireCenterVisual.STRAIGHT_Y
				: east && west && !north && !south && !up && !down ? WireCenterVisual.STRAIGHT_X
				: north && south && !east && !west && !up && !down ? WireCenterVisual.STRAIGHT_Z
				: WireCenterVisual.JUNCTION;
		return ModBlocks.RED_CABLE.get().defaultBlockState()
				.setValue(WireBlock.NORTH, north)
				.setValue(WireBlock.SOUTH, south)
				.setValue(WireBlock.EAST, east)
				.setValue(WireBlock.WEST, west)
				.setValue(WireBlock.UP, up)
				.setValue(WireBlock.DOWN, down)
				.setValue(WireBlock.CENTER, center);
	}

	/** Original {@code machine_detector} meta 0 (off) / 1 (on) - a single block with {@code POWERED} in Modernized. */
	private static BlockState detector(boolean powered) {
		return ModBlocks.DETECTOR.get().defaultBlockState().setValue(PowerDetectorBlock.POWERED, powered);
	}

	/**
	 * Renders the Stirling engine through its registered Nucleus {@code MachineBer}: the cog,
	 * small cog and piston animation read the fake BE's public {@code spin}/{@code lastSpin}
	 * and {@code hasCog()} - the same values the original {@code RenderStirling} actor kept in
	 * its data tag, so the speed ramps translate 1:1.
	 * <p>
	 * The pose is block-corner based (translate(x, y, z) with no manual rotation) so the
	 * spec's facing transform applies the {@code FACING} rotation exactly like the in-world
	 * dispatcher does. Original rotation codes map through {@code ObjBerHelper.meta}
	 * (2:NORTH, 3:SOUTH, 4:WEST, 5:EAST) to reproduce the original net yaw per code.
	 * The fake BE lives at virtual coordinates offset far beyond the world border, so its
	 * {@code BlockPos.asLong()} roster/cache keys can never collide with a real machine.
	 */
	public static class ActorStirling implements ITileActorRenderer {

		/** Original rotation code (3:0°, 5:90°, 2:180°, 4:270°) → FACING reproducing the same net yaw via {@code ObjBerHelper.meta}. */
		static final Direction[] FACING_MAP = {
				Direction.NORTH,
				Direction.NORTH,
				Direction.NORTH,
				Direction.SOUTH,
				Direction.WEST,
				Direction.EAST
		};

		/** X offset isolating the fake BE's pos-keyed Nucleus state (roster slots, caches) from real machines. */
		static final int GUI_POS_OFFSET = 30_000_000;

		private MachineStirlingBlockEntity stirling;
		private int lastRotation = -1;
		private int lastType = -1;

		@Override
		public void renderActor(GuiGraphics gui, VirtualWorld world, int ticks, float interp, CompoundTag data) {
			double x = data.getDouble("x");
			double y = data.getDouble("y");
			double z = data.getDouble("z");
			int rotation = data.getInt("rotation");
			// Original type 0/1 was one block's meta; Modernized ships two blocks, so the fake
			// BE is recreated with the matching block state (texture and gear follow getGeatMeta).
			int type = data.getInt("type");
			boolean hasCog = data.getBoolean("hasCog");

			gui.pose().pushPose();
			gui.pose().translate(x, y, z);

			if(this.stirling == null || this.lastRotation != rotation || this.lastType != type) {
				this.lastRotation = rotation;
				this.lastType = type;
				this.stirling = new MachineStirlingBlockEntity(
						new BlockPos((int) x + GUI_POS_OFFSET, (int) y, (int) z),
						(type == 1 ? ModBlocks.STIRLING_STEEL : ModBlocks.STIRLING).get().defaultBlockState()
								.setValue(DummyableMachineBlock.FACING, FACING_MAP[Math.min(rotation, FACING_MAP.length - 1)]));
			}

			this.stirling.setHasCog(hasCog);
			this.stirling.lastSpin = data.getFloat("lastSpin");
			this.stirling.spin = data.getFloat("spin");

			BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
			BlockEntityRenderer<BlockEntity> ber = dispatcher.getRenderer(this.stirling);
			if(ber != null) {
				ber.render(this.stirling, interp, gui.pose(), gui.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			}

			gui.pose().popPose();
		}

		/** Original {@code RenderStirling.updateActor} 1:1: integrate {@code speed} into {@code spin}, wrapping at 360. */
		@Override
		public void updateActor(int ticks, CompoundTag data) {

			float lastSpin = data.getFloat("spin");
			float spin = lastSpin + data.getFloat("speed");

			if(spin >= 360F) {
				lastSpin -= 360F;
				spin -= 360F;
			}

			data.putFloat("lastSpin", lastSpin);
			data.putFloat("spin", spin);
		}
	}

	/**
	 * Renders the oil burner. There is no dedicated oil burner BlockEntityRenderer in
	 * Modernized - the machine's visuals are its baked blockstate model (the chunk-mesh path),
	 * which is the modern equivalent of the original {@code ResourceManager.heater_oilburner
	 * .renderAll()} - so the actor draws the fake BE's block state via
	 * {@code BlockRenderDispatcher#renderSingleBlock}.
	 * <p>
	 * The original actor never read the {@code rotation} code: {@code ActorBurner.renderActor}
	 * (original CanneryStirling.java:185-196) only takes x/y/z and calls
	 * {@code ResourceManager.heater_oilburner.renderAll()} with no rotation switch, so the
	 * burner draws unrotated here too - {@code defaultBlockState()} is {@code facing=north},
	 * which the blockstate file maps to y = 0.
	 */
	public static class ActorBurner implements ITileActorRenderer {

		/** X offset isolating the fake BE's pos-keyed state from real machines (same convention as {@link ActorStirling}). */
		static final int GUI_POS_OFFSET = 30_000_000;

		private MachineOilburnerBlockEntity burner;
		private int lastRotation = -1;

		@Override
		public void renderActor(GuiGraphics gui, VirtualWorld world, int ticks, float interp, CompoundTag data) {
			double x = data.getDouble("x");
			double y = data.getDouble("y");
			double z = data.getDouble("z");
			int rotation = data.getInt("rotation");

			gui.pose().pushPose();
			gui.pose().translate(x, y, z);

			if(this.burner == null || this.lastRotation != rotation) {
				this.lastRotation = rotation;
				// Original renders the burner model unrotated regardless of the rotation code -
				// defaultBlockState() is facing=north, the blockstate file maps that to y = 0.
				this.burner = new MachineOilburnerBlockEntity(
						new BlockPos((int) x + GUI_POS_OFFSET, (int) y, (int) z),
						ModBlocks.OILBURNER.get().defaultBlockState());
			}

			Minecraft.getInstance().getBlockRenderer().renderSingleBlock(this.burner.getBlockState(),
					gui.pose(), gui.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

			gui.pose().popPose();
		}

		@Override
		public void updateActor(int ticks, CompoundTag data) { }
	}
}
