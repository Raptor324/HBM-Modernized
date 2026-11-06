package com.hbm_m.wiaj.cannery;

import com.hbm_m.api.energy.WireBlock;
import com.hbm_m.api.energy.WireCenterVisual;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.block.network.ConveyorBend;
import com.hbm_m.block.network.ConveyorBendableBlock;
import com.hbm_m.block.network.ConveyorBlockBase;
import com.hbm_m.blockentity.machines.MachineConveyorPressBlockEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;
import com.hbm_m.wiaj.WiajScript;
import com.hbm_m.wiaj.action.ActionCreateActor;
import com.hbm_m.wiaj.action.ActionRemoveActor;
import com.hbm_m.wiaj.action.ActionSetBlock;
import com.hbm_m.wiaj.action.ActionSetZoom;
import com.hbm_m.wiaj.action.ActionUpdateActor;
import com.hbm_m.wiaj.action.ActionWait;
import com.hbm_m.wiaj.actor.ActorFancyPanel;
import com.hbm_m.wiaj.actor.ActorFancyPanel.Orientation;
import com.hbm_m.wiaj.actor.ActorTileEntity;
import com.hbm_m.wiaj.actor.ITileActorRenderer;
import com.hbm_m.wiaj.cannery.CanneryConveyor.ActorMovingItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * World In A Jar demonstration for the Conveyor Press.
 * <p>
 * Subset and specialized companion to {@link CanneryConveyor}, demonstrating:
 * <ul>
 *   <li>In-line conveyor integration: 3-block tall industrial multiblock machine</li>
 *   <li>Continuous flow: middle deck serves as an active conveyor belt without hoppers</li>
 *   <li>Stamp installation and Screwdriver removal/swapping mechanics</li>
 *   <li>Live stamping: HE powered hydraulic piston stamping ingots into plates on the fly</li>
 * </ul>
 */
public class CanneryConveyorPress extends CanneryBase {

	@Override
	public ItemStack getIcon() {
		return new ItemStack(ModBlocks.CONVEYOR_PRESS.get());
	}

	@Override
	public String getName() {
		return "cannery.conveyor_press";
	}

	@Override
	public CanneryBase[] seeAlso() {
		return new CanneryBase[] {
				new CanneryConveyor()
		};
	}

	@Override
	public WiajScript createScript() {
		VirtualWorld world = new VirtualWorld(7, 6, 7);
		WiajScript script = new WiajScript(world);

		// ====================================================================
		// Scene 0: Conveyor Line Integration & Multiblock Form
		// ====================================================================
		WiajScene scene0 = new WiajScene(script);
		scene0.add(new ActionSetZoom(2.2, 0));

		// Smooth concrete floor at y = 0
		for (int x = world.sizeX - 1; x >= 0; x--) {
			for (int z = 0; z < world.sizeZ; z++) {
				scene0.add(new ActionSetBlock(x, 0, z, ModBlocks.CONCRETE_SMOOTH.get()));
			}
			scene0.add(new ActionWait(1));
		}
		scene0.add(new ActionWait(4));

		// Incoming conveyor line heading towards +X (FACING = WEST)
		scene0.add(new ActionSetBlock(1, 1, 3, conveyor(Direction.WEST)));
		scene0.add(new ActionSetBlock(2, 1, 3, conveyor(Direction.WEST)));

		// Conveyor Press multiblock placed at x = 3, z = 3
		// Controller at y = 0, machine dummy parts at y = 1 and y = 2
		scene0.add(new ActionSetBlock(3, 0, 3, ModBlocks.CONVEYOR_PRESS.get().defaultBlockState()
				.setValue(DummyableMachineBlock.FACING, Direction.NORTH)));
		scene0.add(new ActionSetBlock(3, 1, 3, ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState()));
		scene0.add(new ActionSetBlock(3, 2, 3, ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState()));

		// Outgoing conveyor line continuing towards +X
		scene0.add(new ActionSetBlock(4, 1, 3, conveyor(Direction.WEST)));
		scene0.add(new ActionSetBlock(5, 1, 3, conveyor(Direction.WEST)));
		scene0.add(new ActionSetBlock(6, 1, 3, ModBlocks.CRATE.get().defaultBlockState()));

		// Heavy red cable power supply into controller base
		scene0.add(new ActionSetBlock(3, 0, 2, cable(false, true, false, false, false, false)));
		scene0.add(new ActionSetBlock(3, 0, 1, cable(false, true, false, false, false, false)));

		// Register Conveyor Press BER Actor
		CompoundTag pressData0 = new CompoundTag();
		pressData0.putDouble("x", 3D);
		pressData0.putDouble("y", 0D);
		pressData0.putDouble("z", 3D);
		pressData0.putBoolean("hasStamp", false);
		pressData0.putDouble("press", 0D);
		scene0.add(new ActionCreateActor(0, new ActorTileEntity(new ActorConveyorPress(Direction.NORTH), pressData0).smoothPlace()));

		// Panel 0: Multiblock introduction
		scene0.add(new ActionCreateActor(10, new ActorFancyPanel(
				Minecraft.getInstance().font, 0, -50,
				new Object[][] {
					{ new ItemStack(ModBlocks.CONVEYOR_PRESS.get()) },
					{ resolveKey("cannery.conveyor_press.0") }
				}, 190)
				.setColors(colorGrey).setOrientation(Orientation.BOTTOM)));

		scene0.add(new ActionWait(60));
		scene0.add(new ActionRemoveActor(10));

		// Panel 1: Through-flow conveyor bed
		scene0.add(new ActionCreateActor(11, new ActorFancyPanel(
				Minecraft.getInstance().font, 0, 40,
				new Object[][] { { resolveKey("cannery.conveyor_press.1") } }, 190)
				.setColors(colorBlue).setOrientation(Orientation.TOP)));

		scene0.add(new ActionWait(70));
		scene0.add(new ActionRemoveActor(11));

		// ====================================================================
		// Scene 1: Stamps, Screwdriver & In-Line Stamping
		// ====================================================================
		WiajScene scene1 = new WiajScene(script);

		// Retain foundation, tracks, and press
		for (int x = world.sizeX - 1; x >= 0; x--) {
			for (int z = 0; z < world.sizeZ; z++) {
				scene1.add(new ActionSetBlock(x, 0, z, ModBlocks.CONCRETE_SMOOTH.get()));
			}
		}
		scene1.add(new ActionSetBlock(1, 1, 3, conveyor(Direction.WEST)));
		scene1.add(new ActionSetBlock(2, 1, 3, conveyor(Direction.WEST)));
		scene1.add(new ActionSetBlock(3, 0, 3, ModBlocks.CONVEYOR_PRESS.get().defaultBlockState()
				.setValue(DummyableMachineBlock.FACING, Direction.NORTH)));
		scene1.add(new ActionSetBlock(3, 1, 3, ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState()));
		scene1.add(new ActionSetBlock(3, 2, 3, ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState()));
		scene1.add(new ActionSetBlock(4, 1, 3, conveyor(Direction.WEST)));
		scene1.add(new ActionSetBlock(5, 1, 3, conveyor(Direction.WEST)));
		scene1.add(new ActionSetBlock(6, 1, 3, ModBlocks.CRATE.get().defaultBlockState()));
		scene1.add(new ActionSetBlock(3, 0, 2, cable(false, true, false, false, false, false)));
		scene1.add(new ActionSetBlock(3, 0, 1, cable(false, true, false, false, false, false)));

		// Recreate press actor in active state
		CompoundTag pressData1 = new CompoundTag();
		pressData1.putDouble("x", 3D);
		pressData1.putDouble("y", 0D);
		pressData1.putDouble("z", 3D);
		pressData1.putBoolean("hasStamp", false);
		pressData1.putDouble("press", 0D);
		scene1.add(new ActionCreateActor(0, new ActorTileEntity(new ActorConveyorPress(Direction.NORTH), pressData1).smoothPlace()));

		// Panel 2: Installing stamp
		scene1.add(new ActionCreateActor(10, new ActorFancyPanel(
				Minecraft.getInstance().font, -20, -50,
				new Object[][] {
					{ new ItemStack(ModItems.STAMP_IRON_PLATE.get()) },
					{ resolveKey("cannery.conveyor_press.2") }
				}, 190)
				.setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene1.add(new ActionWait(30));

		// Stamp installed -> stamper piston appears!
		scene1.add(new ActionUpdateActor(0, "hasStamp", true));

		scene1.add(new ActionWait(50));
		scene1.add(new ActionRemoveActor(10));

		// Panel 3: Screwdriver removal
		scene1.add(new ActionCreateActor(11, new ActorFancyPanel(
				Minecraft.getInstance().font, 0, 40,
				new Object[][] {
					{ new ItemStack(ModItems.SCREWDRIVER.get()) },
					{ resolveKey("cannery.conveyor_press.3") }
				}, 190)
				.setColors(colorGold).setOrientation(Orientation.TOP)));

		scene1.add(new ActionWait(60));
		scene1.add(new ActionRemoveActor(11));

		// Panel 4: Live stamping in action
		scene1.add(new ActionCreateActor(12, new ActorFancyPanel(
				Minecraft.getInstance().font, 0, -50,
				new Object[][] { { resolveKey("cannery.conveyor_press.4") } }, 190)
				.setColors(colorBlue).setOrientation(Orientation.BOTTOM)));

		// Loop 3 consecutive live stamping cycles
		for (int cycle = 0; cycle < 3; cycle++) {
			int actorId = 1 + cycle;
			CompoundTag ingotTag = new CompoundTag();
			// Spawn Iron Ingot at conveyor start
			scene1.add(new ActionCreateActor(actorId, new ActorTileEntity(
					new ActorMovingItem(new ItemStack(Items.IRON_INGOT), 0.08, false,
							new double[] {1.2, 1.25, 3.5},
							new double[] {3.5, 1.25, 3.5},
							new double[] {5.8, 1.25, 3.5}), ingotTag)));

			// Ingot arrives at the center of the press at tick ~28
			scene1.add(new ActionWait(28));

			// Stamp strikes down!
			scene1.add(new ActionUpdateActor(0, "stamping", true));
			scene1.add(new ActionWait(5));

			// Transform ingot into stamped plate at the bottom of the stroke!
			scene1.add(new ActionUpdateActor(actorId, "item", "iron_plate"));
			scene1.add(new ActionWait(5));

			// Plate rolls out to the collection crate
			scene1.add(new ActionWait(28));
			scene1.add(new ActionRemoveActor(actorId));
			scene1.add(new ActionWait(10));
		}

		scene1.add(new ActionWait(40));
		scene1.add(new ActionRemoveActor(12));
		scene1.add(new ActionRemoveActor(0));

		script.addScene(scene0).addScene(scene1);
		return script;
	}

	private static BlockState conveyor(Direction facing) {
		return ModBlocks.CONVEYOR.get().defaultBlockState()
				.setValue(ConveyorBlockBase.FACING, facing)
				.setValue(ConveyorBendableBlock.BEND, ConveyorBend.STRAIGHT);
	}

	private static BlockState cable(boolean north, boolean south, boolean east, boolean west, boolean up, boolean down) {
		return ModBlocks.RED_WIRE_COATED.get().defaultBlockState()
				.setValue(WireBlock.NORTH, north)
				.setValue(WireBlock.SOUTH, south)
				.setValue(WireBlock.EAST, east)
				.setValue(WireBlock.WEST, west)
				.setValue(WireBlock.UP, up)
				.setValue(WireBlock.DOWN, down)
				.setValue(WireBlock.CENTER, WireCenterVisual.JUNCTION);
	}

	private static String resolveKey(String key) {
		return I18n.get(key);
	}

	/**
	 * Renders the Conveyor Press multiblock through {@link MachinePressGroupRenderer}
	 * with live piston stroke animations and reactive stamp heads.
	 */
	public static class ActorConveyorPress implements ITileActorRenderer {

		static final int GUI_POS_OFFSET = 30_000_000;
		private MachineConveyorPressBlockEntity press;
		private final Direction facing;

		public ActorConveyorPress(Direction facing) {
			this.facing = facing;
		}

		@Override
		public void renderActor(GuiGraphics gui, VirtualWorld world, int ticks, float interp, CompoundTag data) {
			double x = data.getDouble("x");
			double y = data.getDouble("y");
			double z = data.getDouble("z");

			gui.pose().pushPose();
			gui.pose().translate(x, y, z);

			if (this.press == null) {
				BlockState state = ModBlocks.CONVEYOR_PRESS.get().defaultBlockState()
						.setValue(DummyableMachineBlock.FACING, this.facing != null ? this.facing : Direction.NORTH);
				this.press = new MachineConveyorPressBlockEntity(
						new BlockPos((int) x + GUI_POS_OFFSET, (int) y, (int) z), state);
			}

			if (data.getBoolean("hasStamp")) {
				if (this.press.getStampStack().isEmpty()) {
					this.press.tryInsertStamp(null, new ItemStack(ModItems.STAMP_IRON_PLATE.get()));
				}
			} else {
				this.press.removeStamp();
			}

			double p = data.getDouble("press");
			double lastP = data.contains("lastPress") ? data.getDouble("lastPress") : p;
			this.press.renderPress = p;
			this.press.lastPress = lastP;

			BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
			BlockEntityRenderer<BlockEntity> ber = dispatcher.getRenderer(this.press);
			if (ber != null) {
				ber.render(this.press, interp, gui.pose(), gui.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			}

			gui.pose().popPose();
		}

		@Override
		public void updateActor(int ticks, CompoundTag data) {
			double p = data.getDouble("press");
			data.putDouble("lastPress", p);

			if (data.getBoolean("stamping")) {
				boolean retracting = data.getBoolean("retracting");
				double speed = 0.2;
				if (!retracting) {
					p += speed;
					if (p >= 1.0) {
						p = 1.0;
						data.putBoolean("retracting", true);
					}
				} else {
					p -= speed;
					if (p <= 0.0) {
						p = 0.0;
						data.putBoolean("retracting", false);
						data.putBoolean("stamping", false);
					}
				}
				data.putDouble("press", p);
			}
		}
	}
}
