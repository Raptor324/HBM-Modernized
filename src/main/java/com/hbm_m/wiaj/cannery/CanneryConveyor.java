package com.hbm_m.wiaj.cannery;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.network.ConveyorBend;
import com.hbm_m.block.network.ConveyorBendableBlock;
import com.hbm_m.block.network.ConveyorBlockBase;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.util.BobMathUtil;
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
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * World In A Jar tutorial demonstration for Conveyors and Logistics.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Basic belt transport, lane snapping, and Screwdriver 90° rotation / bends</li>
 *   <li>Speed tiers (Express 3x speed) and parallel multi-lane belts (Double, Triple)</li>
 *   <li>Vertical logistics: Conveyor Lift (vertical ascent) and Chute (rapid drop)</li>
 *   <li>Automated route planning and line-wide upgrades via Conveyor Wands</li>
 * </ul>
 */
public class CanneryConveyor extends CanneryBase {

	@Override
	public ItemStack getIcon() {
		return new ItemStack(ModItems.CONVEYOR_WAND_REGULAR.get());
	}

	@Override
	public String getName() {
		return "cannery.conveyor";
	}

	@Override
	public CanneryBase[] seeAlso() {
		return new CanneryBase[] {
				new CanneryConveyorPress()
		};
	}

	@Override
	public WiajScript createScript() {
		VirtualWorld world = new VirtualWorld(8, 7, 8);
		WiajScript script = new WiajScript(world);

		// ====================================================================
		// Scene 0: Conveyor Basics, Movement & Screwdriver Bends
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

		// Straight conveyor belt heading towards +X (FACING = WEST)
		for (int x = 1; x <= 5; x++) {
			scene0.add(new ActionSetBlock(x, 1, 2, conveyor(Direction.WEST, ConveyorBend.STRAIGHT)));
		}

		// Panel 0: Basics & lane snapping
		scene0.add(new ActionCreateActor(10, new ActorFancyPanel(
				Minecraft.getInstance().font, 0, -50,
				new Object[][] { { resolveKey("cannery.conveyor.0") } }, 185)
				.setColors(colorBlue).setOrientation(Orientation.BOTTOM)));

		// Moving item: Iron Ingot along straight belt
		CompoundTag itemTag0 = new CompoundTag();
		scene0.add(new ActionCreateActor(1, new ActorTileEntity(
				new ActorMovingItem(new ItemStack(Items.IRON_INGOT), 0.08, true,
						new double[] {1.2, 1.25, 2.5},
						new double[] {5.5, 1.25, 2.5}), itemTag0)));

		scene0.add(new ActionWait(65));
		scene0.add(new ActionRemoveActor(10));

		// Panel 1: Screwdriver rotation & bends
		scene0.add(new ActionCreateActor(11, new ActorFancyPanel(
				Minecraft.getInstance().font, 0, 40,
				new Object[][] {
					{ new ItemStack(ModItems.SCREWDRIVER.get()) },
					{ resolveKey("cannery.conveyor.1") }
				}, 190)
				.setColors(colorGold).setOrientation(Orientation.TOP)));

		scene0.add(new ActionWait(15));

		// Turn conveyor at (5, 1, 2) to bend RIGHT (turns from +X towards +Z)
		scene0.add(new ActionSetBlock(5, 1, 2, conveyor(Direction.WEST, ConveyorBend.RIGHT)));

		// Continuing conveyor line heading towards +Z (FACING = NORTH)
		scene0.add(new ActionSetBlock(5, 1, 3, conveyor(Direction.NORTH, ConveyorBend.STRAIGHT)));
		scene0.add(new ActionSetBlock(5, 1, 4, conveyor(Direction.NORTH, ConveyorBend.STRAIGHT)));
		scene0.add(new ActionSetBlock(5, 1, 5, conveyor(Direction.NORTH, ConveyorBend.STRAIGHT)));
		scene0.add(new ActionSetBlock(5, 1, 6, Blocks.CHEST.defaultBlockState()));

		// Switch item to the new bent path
		scene0.add(new ActionRemoveActor(1));
		CompoundTag itemTag0b = new CompoundTag();
		scene0.add(new ActionCreateActor(1, new ActorTileEntity(
				new ActorMovingItem(new ItemStack(Items.IRON_INGOT), 0.08, true,
						new double[] {1.2, 1.25, 2.5},
						new double[] {5.5, 1.25, 2.5},
						new double[] {5.5, 1.25, 5.8}), itemTag0b)));

		scene0.add(new ActionWait(85));
		scene0.add(new ActionRemoveActor(11));
		scene0.add(new ActionRemoveActor(1));

		// ====================================================================
		// Scene 1: Speed Tiers & Multi-Lane Belts
		// ====================================================================
		WiajScene scene1 = new WiajScene(script);

		// Clear previous tracks
		clearVolume(scene1, 1, 1, 2, 6, 1, 6);

		// Track 1: Standard Conveyor at z = 2
		for (int x = 1; x <= 6; x++) {
			scene1.add(new ActionSetBlock(x, 1, 2, ModBlocks.CONVEYOR.get().defaultBlockState()
					.setValue(ConveyorBlockBase.FACING, Direction.WEST)));
		}

		// Track 2: Express Conveyor at z = 5
		for (int x = 1; x <= 6; x++) {
			scene1.add(new ActionSetBlock(x, 1, 5, ModBlocks.CONVEYOR_EXPRESS.get().defaultBlockState()
					.setValue(ConveyorBlockBase.FACING, Direction.WEST)));
		}

		// Panel 2: Express speed comparison
		scene1.add(new ActionCreateActor(10, new ActorFancyPanel(
				Minecraft.getInstance().font, -20, -50,
				new Object[][] {
					{ new ItemStack(ModBlocks.CONVEYOR_EXPRESS.get()) },
					{ resolveKey("cannery.conveyor.2") }
				}, 195)
				.setColors(colorBlue).setOrientation(Orientation.BOTTOM)));

		// Copper ingot on standard track (speed 0.06)
		CompoundTag copperTag = new CompoundTag();
		scene1.add(new ActionCreateActor(1, new ActorTileEntity(
				new ActorMovingItem(new ItemStack(Items.COPPER_INGOT), 0.06, true,
						new double[] {1.2, 1.25, 2.5},
						new double[] {6.5, 1.25, 2.5}), copperTag)));

		// Gold ingot on express track (speed 0.18 - 3x faster)
		CompoundTag goldTag = new CompoundTag();
		scene1.add(new ActionCreateActor(2, new ActorTileEntity(
				new ActorMovingItem(new ItemStack(Items.GOLD_INGOT), 0.18, true,
						new double[] {1.2, 1.25, 5.5},
						new double[] {6.5, 1.25, 5.5}), goldTag)));

		scene1.add(new ActionWait(80));
		scene1.add(new ActionRemoveActor(10));
		scene1.add(new ActionRemoveActor(1));
		scene1.add(new ActionRemoveActor(2));

		// Replace track 2 at z = 5 with Triple Conveyor
		for (int x = 1; x <= 6; x++) {
			scene1.add(new ActionSetBlock(x, 1, 5, ModBlocks.CONVEYOR_TRIPLE.get().defaultBlockState()
					.setValue(ConveyorBlockBase.FACING, Direction.WEST)));
		}

		// Panel 3: Double and Triple parallel lanes
		scene1.add(new ActionCreateActor(11, new ActorFancyPanel(
				Minecraft.getInstance().font, 0, 40,
				new Object[][] {
					{ new ItemStack(ModBlocks.CONVEYOR_TRIPLE.get()) },
					{ resolveKey("cannery.conveyor.3") }
				}, 195)
				.setColors(colorCopper).setOrientation(Orientation.TOP)));

		// 3 parallel item streams along the 3 separate lanes
		CompoundTag lane0 = new CompoundTag();
		scene1.add(new ActionCreateActor(3, new ActorTileEntity(
				new ActorMovingItem(new ItemStack(Items.IRON_INGOT), 0.08, true,
						new double[] {1.2, 1.25, 5.22},
						new double[] {6.5, 1.25, 5.22}), lane0)));

		CompoundTag lane1 = new CompoundTag();
		scene1.add(new ActionCreateActor(4, new ActorTileEntity(
				new ActorMovingItem(new ItemStack(Items.GOLD_INGOT), 0.08, true,
						new double[] {1.2, 1.25, 5.50},
						new double[] {6.5, 1.25, 5.50}), lane1)));

		CompoundTag lane2 = new CompoundTag();
		scene1.add(new ActionCreateActor(5, new ActorTileEntity(
				new ActorMovingItem(new ItemStack(Items.COAL), 0.08, true,
						new double[] {1.2, 1.25, 5.78},
						new double[] {6.5, 1.25, 5.78}), lane2)));

		scene1.add(new ActionWait(85));
		scene1.add(new ActionRemoveActor(11));
		scene1.add(new ActionRemoveActor(3));
		scene1.add(new ActionRemoveActor(4));
		scene1.add(new ActionRemoveActor(5));

		// ====================================================================
		// Scene 2: Vertical Logistics (Conveyor Lift & Chute)
		// ====================================================================
		WiajScene scene2 = new WiajScene(script);

		clearVolume(scene2, 1, 1, 2, 6, 1, 6);

		// Ground entry
		scene2.add(new ActionSetBlock(1, 1, 3, conveyor(Direction.WEST, ConveyorBend.STRAIGHT)));

		// Conveyor Lift climbing vertically from y = 1 to y = 3
		scene2.add(new ActionSetBlock(2, 1, 3, ModBlocks.CONVEYOR_LIFT.get().defaultBlockState()
				.setValue(ConveyorBlockBase.FACING, Direction.WEST)));
		scene2.add(new ActionSetBlock(2, 2, 3, ModBlocks.CONVEYOR_LIFT.get().defaultBlockState()
				.setValue(ConveyorBlockBase.FACING, Direction.WEST)));
		scene2.add(new ActionSetBlock(2, 3, 3, ModBlocks.CONVEYOR_LIFT.get().defaultBlockState()
				.setValue(ConveyorBlockBase.FACING, Direction.WEST)));

		// Upper bridge at y = 4
		scene2.add(new ActionSetBlock(2, 4, 3, conveyor(Direction.WEST, ConveyorBend.STRAIGHT)));
		scene2.add(new ActionSetBlock(3, 4, 3, conveyor(Direction.WEST, ConveyorBend.STRAIGHT)));
		scene2.add(new ActionSetBlock(4, 4, 3, conveyor(Direction.WEST, ConveyorBend.STRAIGHT)));

		// Support columns
		for (int y = 1; y <= 3; y++) {
			scene2.add(new ActionSetBlock(3, y, 3, Blocks.IRON_BARS.defaultBlockState()));
			scene2.add(new ActionSetBlock(4, y, 3, Blocks.IRON_BARS.defaultBlockState()));
		}

		// Conveyor Chute dropping vertically from y = 4 to y = 2
		scene2.add(new ActionSetBlock(5, 4, 3, ModBlocks.CONVEYOR_CHUTE.get().defaultBlockState()
				.setValue(ConveyorBlockBase.FACING, Direction.WEST)));
		scene2.add(new ActionSetBlock(5, 3, 3, ModBlocks.CONVEYOR_CHUTE.get().defaultBlockState()
				.setValue(ConveyorBlockBase.FACING, Direction.WEST)));
		scene2.add(new ActionSetBlock(5, 2, 3, ModBlocks.CONVEYOR_CHUTE.get().defaultBlockState()
				.setValue(ConveyorBlockBase.FACING, Direction.WEST)));

		// Ground exit
		scene2.add(new ActionSetBlock(5, 1, 3, conveyor(Direction.WEST, ConveyorBend.STRAIGHT)));
		scene2.add(new ActionSetBlock(6, 1, 3, conveyor(Direction.WEST, ConveyorBend.STRAIGHT)));
		scene2.add(new ActionSetBlock(7, 1, 3, Blocks.CHEST.defaultBlockState()));

		// Panel 4: Vertical transport
		scene2.add(new ActionCreateActor(10, new ActorFancyPanel(
				Minecraft.getInstance().font, -30, -50,
				new Object[][] {
					{ new ItemStack(ModBlocks.CONVEYOR_LIFT.get()) },
					{ resolveKey("cannery.conveyor.4") }
				}, 195)
				.setColors(colorBlue).setOrientation(Orientation.BOTTOM)));

		// Moving item ascending the lift and dropping down the chute
		CompoundTag verticalTag = new CompoundTag();
		scene2.add(new ActionCreateActor(1, new ActorTileEntity(
				new ActorMovingItem(new ItemStack(ModBlocks.CRATE.get()), 0.12, true,
						new double[] {1.2, 1.25, 3.5},
						new double[] {2.5, 1.25, 3.5},
						new double[] {2.5, 4.25, 3.5},
						new double[] {5.5, 4.25, 3.5},
						new double[] {5.5, 1.25, 3.5},
						new double[] {6.8, 1.25, 3.5}), verticalTag)));

		scene2.add(new ActionWait(75));
		scene2.add(new ActionRemoveActor(10));

		// Panel 5: Screwdriver conversion cycle
		scene2.add(new ActionCreateActor(11, new ActorFancyPanel(
				Minecraft.getInstance().font, 10, 35,
				new Object[][] {
					{ new ItemStack(ModItems.SCREWDRIVER.get()) },
					{ resolveKey("cannery.conveyor.5") }
				}, 190)
				.setColors(colorGold).setOrientation(Orientation.TOP)));

		scene2.add(new ActionWait(80));
		scene2.add(new ActionRemoveActor(11));
		scene2.add(new ActionRemoveActor(1));

		// ====================================================================
		// Scene 3: Conveyor Wand (Auto-Routing & Mass Upgrade)
		// ====================================================================
		WiajScene scene3 = new WiajScene(script);

		clearVolume(scene3, 1, 1, 2, 7, 4, 6);

		// Markers for start and end
		scene3.add(new ActionSetBlock(1, 0, 2, ModBlocks.CONCRETE_HAZARD.get()));
		scene3.add(new ActionSetBlock(6, 0, 5, ModBlocks.CONCRETE_HAZARD.get()));

		// Panel 6: Auto-routing with Wand
		scene3.add(new ActionCreateActor(10, new ActorFancyPanel(
				Minecraft.getInstance().font, 0, -50,
				new Object[][] {
					{ new ItemStack(ModItems.CONVEYOR_WAND_REGULAR.get()) },
					{ resolveKey("cannery.conveyor.6") }
				}, 190)
				.setColors(colorBlue).setOrientation(Orientation.BOTTOM)));

		scene3.add(new ActionWait(15));

		// Build auto-routed conveyor line
		scene3.add(new ActionSetBlock(1, 1, 2, conveyor(Direction.WEST, ConveyorBend.STRAIGHT)));
		scene3.add(new ActionWait(3));
		scene3.add(new ActionSetBlock(2, 1, 2, conveyor(Direction.WEST, ConveyorBend.STRAIGHT)));
		scene3.add(new ActionWait(3));
		scene3.add(new ActionSetBlock(3, 1, 2, conveyor(Direction.WEST, ConveyorBend.RIGHT)));
		scene3.add(new ActionWait(3));
		scene3.add(new ActionSetBlock(3, 1, 3, conveyor(Direction.NORTH, ConveyorBend.STRAIGHT)));
		scene3.add(new ActionWait(3));
		scene3.add(new ActionSetBlock(3, 1, 4, conveyor(Direction.NORTH, ConveyorBend.LEFT)));
		scene3.add(new ActionWait(3));
		scene3.add(new ActionSetBlock(4, 1, 4, conveyor(Direction.WEST, ConveyorBend.STRAIGHT)));
		scene3.add(new ActionWait(3));
		scene3.add(new ActionSetBlock(5, 1, 4, conveyor(Direction.WEST, ConveyorBend.RIGHT)));
		scene3.add(new ActionWait(3));
		scene3.add(new ActionSetBlock(5, 1, 5, conveyor(Direction.NORTH, ConveyorBend.LEFT)));
		scene3.add(new ActionWait(3));
		scene3.add(new ActionSetBlock(6, 1, 5, conveyor(Direction.WEST, ConveyorBend.STRAIGHT)));

		// Item travels along auto-routed line
		CompoundTag wandItemTag = new CompoundTag();
		scene3.add(new ActionCreateActor(1, new ActorTileEntity(
				new ActorMovingItem(new ItemStack(Items.IRON_INGOT), 0.08, true,
						new double[] {1.2, 1.25, 2.5},
						new double[] {3.5, 1.25, 2.5},
						new double[] {3.5, 1.25, 4.5},
						new double[] {5.5, 1.25, 4.5},
						new double[] {5.5, 1.25, 5.5},
						new double[] {6.8, 1.25, 5.5}), wandItemTag)));

		scene3.add(new ActionWait(60));
		scene3.add(new ActionRemoveActor(10));

		// Panel 7: Upgraded wand line conversion
		scene3.add(new ActionCreateActor(11, new ActorFancyPanel(
				Minecraft.getInstance().font, 0, 40,
				new Object[][] {
					{ new ItemStack(ModItems.CONVEYOR_WAND_EXPRESS.get()) },
					{ resolveKey("cannery.conveyor.7") }
				}, 190)
				.setColors(colorGold).setOrientation(Orientation.TOP)));

		scene3.add(new ActionWait(20));

		// Instant whole-line upgrade to Express Conveyor!
		scene3.add(new ActionSetBlock(1, 1, 2, express(Direction.WEST, ConveyorBend.STRAIGHT)));
		scene3.add(new ActionSetBlock(2, 1, 2, express(Direction.WEST, ConveyorBend.STRAIGHT)));
		scene3.add(new ActionSetBlock(3, 1, 2, express(Direction.WEST, ConveyorBend.RIGHT)));
		scene3.add(new ActionSetBlock(3, 1, 3, express(Direction.NORTH, ConveyorBend.STRAIGHT)));
		scene3.add(new ActionSetBlock(3, 1, 4, express(Direction.NORTH, ConveyorBend.LEFT)));
		scene3.add(new ActionSetBlock(4, 1, 4, express(Direction.WEST, ConveyorBend.STRAIGHT)));
		scene3.add(new ActionSetBlock(5, 1, 4, express(Direction.WEST, ConveyorBend.RIGHT)));
		scene3.add(new ActionSetBlock(5, 1, 5, express(Direction.NORTH, ConveyorBend.LEFT)));
		scene3.add(new ActionSetBlock(6, 1, 5, express(Direction.WEST, ConveyorBend.STRAIGHT)));

		// Speed up item to match express line
		scene3.add(new ActionUpdateActor(1, "speed", 0.20));

		scene3.add(new ActionWait(75));
		scene3.add(new ActionRemoveActor(11));
		scene3.add(new ActionRemoveActor(1));

		script.addScene(scene0).addScene(scene1).addScene(scene2).addScene(scene3);
		return script;
	}

	private static BlockState conveyor(Direction facing, ConveyorBend bend) {
		return ModBlocks.CONVEYOR.get().defaultBlockState()
				.setValue(ConveyorBlockBase.FACING, facing)
				.setValue(ConveyorBendableBlock.BEND, bend);
	}

	private static BlockState express(Direction facing, ConveyorBend bend) {
		return ModBlocks.CONVEYOR_EXPRESS.get().defaultBlockState()
				.setValue(ConveyorBlockBase.FACING, facing)
				.setValue(ConveyorBendableBlock.BEND, bend);
	}

	private static void clearVolume(WiajScene scene, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					scene.add(new ActionSetBlock(x, y, z, Blocks.AIR));
				}
			}
		}
	}

	private static String resolveKey(String key) {
		return I18n.get(key);
	}

	/**
	 * Renders and animates an item traveling smoothly along conveyor waypoints with
	 * sub-tick interpolation and support for dynamic transformations (ingot -> plate).
	 */
	public static class ActorMovingItem implements ITileActorRenderer {

		private final ItemStack defaultStack;
		private final double[][] waypoints;
		private final double defaultSpeed;
		private final boolean loop;

		public ActorMovingItem(ItemStack stack, double speed, boolean loop, double[]... waypoints) {
			this.defaultStack = stack;
			this.defaultSpeed = speed;
			this.loop = loop;
			this.waypoints = waypoints;
		}

		@Override
		public void renderActor(GuiGraphics gui, VirtualWorld world, int ticks, float interp, CompoundTag data) {
			if (data.getBoolean("hidden") || this.waypoints == null || this.waypoints.length < 2) return;

			double curX = data.getDouble("curX");
			double curY = data.getDouble("curY");
			double curZ = data.getDouble("curZ");
			double prevX = data.contains("prevX") ? data.getDouble("prevX") : curX;
			double prevY = data.contains("prevY") ? data.getDouble("prevY") : curY;
			double prevZ = data.contains("prevZ") ? data.getDouble("prevZ") : curZ;

			double rx = BobMathUtil.interp(prevX, curX, interp);
			double ry = BobMathUtil.interp(prevY, curY, interp);
			double rz = BobMathUtil.interp(prevZ, curZ, interp);

			ItemStack stack = this.defaultStack;
			if (data.contains("item")) {
				String itemKey = data.getString("item");
				if ("iron_plate".equals(itemKey)) {
					stack = ModMaterialItems.stack(ModMaterials.IRON, MaterialShape.PLATE, 1);
				} else if ("iron_ingot".equals(itemKey)) {
					stack = new ItemStack(Items.IRON_INGOT);
				} else if ("gold_ingot".equals(itemKey)) {
					stack = new ItemStack(Items.GOLD_INGOT);
				} else if ("copper_ingot".equals(itemKey)) {
					stack = new ItemStack(Items.COPPER_INGOT);
				} else if ("coal".equals(itemKey)) {
					stack = new ItemStack(Items.COAL);
				} else if ("crate".equals(itemKey)) {
					stack = new ItemStack(ModBlocks.CRATE.get());
				}
			}

			gui.pose().pushPose();
			gui.pose().translate(rx, ry, rz);

			ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
			BakedModel model = itemRenderer.getModel(stack, null, null, 0);
			if (!model.isGui3d()) {
				gui.pose().mulPose(Axis.XP.rotationDegrees(90F));
				gui.pose().translate(0.0, -0.1875, 0.0);
			} else {
				float s = 0.5F;
				gui.pose().scale(s, s, s);
			}

			itemRenderer.render(stack, ItemDisplayContext.FIXED, false, gui.pose(), gui.bufferSource(),
					LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
			gui.pose().popPose();
		}

		@Override
		public void updateActor(int ticks, CompoundTag data) {
			if (this.waypoints == null || this.waypoints.length < 2) return;

			if (!data.contains("initialized")) {
				data.putBoolean("initialized", true);
				data.putInt("seg", 0);
				data.putDouble("progress", 0.0);
				data.putDouble("curX", waypoints[0][0]);
				data.putDouble("curY", waypoints[0][1]);
				data.putDouble("curZ", waypoints[0][2]);
				data.putDouble("prevX", waypoints[0][0]);
				data.putDouble("prevY", waypoints[0][1]);
				data.putDouble("prevZ", waypoints[0][2]);
			}

			if (data.getBoolean("paused")) {
				data.putDouble("prevX", data.getDouble("curX"));
				data.putDouble("prevY", data.getDouble("curY"));
				data.putDouble("prevZ", data.getDouble("curZ"));
				return;
			}

			data.putDouble("prevX", data.getDouble("curX"));
			data.putDouble("prevY", data.getDouble("curY"));
			data.putDouble("prevZ", data.getDouble("curZ"));

			int seg = data.getInt("seg");
			double progress = data.getDouble("progress");
			double speed = data.contains("speed") ? data.getDouble("speed") : this.defaultSpeed;

			if (seg >= waypoints.length - 1) {
				if (this.loop) {
					seg = 0;
					progress = 0.0;
					data.putDouble("prevX", waypoints[0][0]);
					data.putDouble("prevY", waypoints[0][1]);
					data.putDouble("prevZ", waypoints[0][2]);
					data.putDouble("curX", waypoints[0][0]);
					data.putDouble("curY", waypoints[0][1]);
					data.putDouble("curZ", waypoints[0][2]);
				} else {
					return;
				}
			}

			double[] p0 = waypoints[seg];
			double[] p1 = waypoints[seg + 1];
			double dx = p1[0] - p0[0];
			double dy = p1[1] - p0[1];
			double dz = p1[2] - p0[2];
			double len = Math.sqrt(dx * dx + dy * dy + dz * dz);

			progress += speed;

			while (len > 1.0e-5 && progress >= len && seg < waypoints.length - 1) {
				progress -= len;
				seg++;
				if (seg < waypoints.length - 1) {
					p0 = waypoints[seg];
					p1 = waypoints[seg + 1];
					dx = p1[0] - p0[0];
					dy = p1[1] - p0[1];
					dz = p1[2] - p0[2];
					len = Math.sqrt(dx * dx + dy * dy + dz * dz);
				}
			}

			if (seg >= waypoints.length - 1) {
				if (this.loop) {
					seg = 0;
					progress = 0.0;
					p0 = waypoints[0];
					p1 = waypoints[1];
					dx = p1[0] - p0[0];
					dy = p1[1] - p0[1];
					dz = p1[2] - p0[2];
					len = Math.sqrt(dx * dx + dy * dy + dz * dz);
				} else {
					seg = waypoints.length - 1;
					data.putInt("seg", seg);
					data.putDouble("progress", 0.0);
					data.putDouble("curX", waypoints[seg][0]);
					data.putDouble("curY", waypoints[seg][1]);
					data.putDouble("curZ", waypoints[seg][2]);
					return;
				}
			}

			double t = len > 1.0e-5 ? (progress / len) : 0.0;
			data.putInt("seg", seg);
			data.putDouble("progress", progress);
			data.putDouble("curX", p0[0] + dx * t);
			data.putDouble("curY", p0[1] + dy * t);
			data.putDouble("curZ", p0[2] + dz * t);
		}
	}
}
