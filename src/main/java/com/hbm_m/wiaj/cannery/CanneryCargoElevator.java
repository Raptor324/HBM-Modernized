package com.hbm_m.wiaj.cannery;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.radio.RadioTorchBaseBlock;
import com.hbm_m.blockentity.machines.CargoElevatorBlockEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;
import com.hbm_m.wiaj.WiajScript;
import com.hbm_m.wiaj.action.ActionCreateActor;
import com.hbm_m.wiaj.action.ActionOffsetBy;
import com.hbm_m.wiaj.action.ActionRemoveActor;
import com.hbm_m.wiaj.action.ActionRotateBy;
import com.hbm_m.wiaj.action.ActionSetBlock;
import com.hbm_m.wiaj.action.ActionSetZoom;
import com.hbm_m.wiaj.action.ActionUpdateActor;
import com.hbm_m.wiaj.action.ActionWait;
import com.hbm_m.wiaj.actor.ActorFancyPanel;
import com.hbm_m.wiaj.actor.ActorFancyPanel.Orientation;
import com.hbm_m.wiaj.actor.ActorPlayer;
import com.hbm_m.wiaj.actor.ActorTileEntity;
import com.hbm_m.wiaj.actor.ITileActorRenderer;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 3D WIAJ demonstration for the Cargo Elevator.
 * <p>
 * Coordinate & layout reference:
 * <ul>
 *   <li>Elevator center is at (6, 0, 6), occupying the 3x3 shaft x: 5..7, z: 5..7</li>
 *   <li>Platform top surface is at Y = elevY (0.0) + 1.0 + extension</li>
 *   <li>Floor 1 (Ground): slab at y = 0 (walking surface Y = 1.0, extension = 0.0)</li>
 *   <li>Floor 2 (Mezzanine, +Z back-right): slab at y = 5, x: 3..10, z: 8..11 (walking surface Y = 6.0, extension = 5.0)</li>
 *   <li>Floor 3 (Upper Deck, +X back-left): slab at y = 10, x: 8..11, z: 3..10 (walking surface Y = 11.0, extension = 10.0)</li>
 * </ul>
 */
public class CanneryCargoElevator extends CanneryBase {

	// Panel positions docked to the left side of the screen to never overlap the central elevator shaft
	private static final int PANEL_X = -105;
	private static final int PANEL_Y = -10;
	private static final int PANEL_WRAP = 135;
	private static final int ICON_Y = -40;
	private static final int TEXT_BELOW_ICON_Y = 12;

	@Override
	public ItemStack getIcon() {
		return new ItemStack(ModBlocks.CARGO_ELEVATOR.get());
	}

	@Override
	public String getName() {
		return "cannery.cargo_elevator";
	}

	@Override
	public WiajScript createScript() {
		VirtualWorld world = new VirtualWorld(13, 16, 13);
		WiajScript script = new WiajScript(world);

		// ====================================================================
		// Scene 0: Introduction, 3x3 Footprint & Base Assembly
		// ====================================================================
		WiajScene scene0 = new WiajScene(script);
		scene0.add(new ActionSetZoom(2.0, 0));
		scene0.add(new ActionOffsetBy(0, 3.5, 0, 0));

		// Concrete foundation across y = 0, leaving the 3x3 elevator pit (x: 5..7, z: 5..7) open
		for (int x = world.sizeX - 1; x >= 0; x--) {
			for (int z = 0; z < world.sizeZ; z++) {
				if (x >= 5 && x <= 7 && z >= 5 && z <= 7) {
					continue;
				}
				scene0.add(new ActionSetBlock(x, 0, z, ModBlocks.CONCRETE_SMOOTH.get()));
			}
			scene0.add(new ActionWait(1));
		}

		// Hazard stripes bordering the 3x3 shaft footprint at ground level (y = 0)
		for (int z = 4; z <= 8; z++) {
			scene0.add(new ActionSetBlock(4, 0, z, ModBlocks.CONCRETE_HAZARD.get()));
			scene0.add(new ActionSetBlock(8, 0, z, ModBlocks.CONCRETE_HAZARD.get()));
		}
		for (int x = 5; x <= 7; x++) {
			scene0.add(new ActionSetBlock(x, 0, 4, ModBlocks.CONCRETE_HAZARD.get()));
			scene0.add(new ActionSetBlock(x, 0, 8, ModBlocks.CONCRETE_HAZARD.get()));
		}

		scene0.add(new ActionWait(8));

		// Spawn elevator base in the 3x3 pit at (6, 0, 6) so platform top (Y = 1.0) is flush with ground floor
		CompoundTag elevTag0 = new CompoundTag();
		elevTag0.putDouble("x", 6D);
		elevTag0.putDouble("y", 0D);
		elevTag0.putDouble("z", 6D);
		elevTag0.putInt("height", 0);
		elevTag0.putDouble("extension", 0D);
		elevTag0.putDouble("prevExtension", 0D);
		elevTag0.putDouble("speed", 0.15D);
		elevTag0.putBoolean("renderPlatform", true);
		scene0.add(new ActionCreateActor(0, new ActorTileEntity(new ActorCargoElevator(), elevTag0).smoothPlace()));
		scene0.add(new ActionUpdateActor(0, "height", 0));
		scene0.add(new ActionUpdateActor(0, "extension", 0.0D));
		scene0.add(new ActionUpdateActor(0, "prevExtension", 0.0D));
		scene0.add(new ActionUpdateActor(0, "moving", false));

		scene0.add(new ActionWait(10));

		// Overview panel
		scene0.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, PANEL_X, PANEL_Y,
				new Object[][] {{resolveKey("cannery.cargo_elevator.0")}}, PANEL_WRAP)
				.setColors(colorGold).setOrientation(Orientation.CENTER)));
		scene0.add(new ActionWait(70));

		// Structure detail panel
		scene0.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, PANEL_X, PANEL_Y,
				new Object[][] {{resolveKey("cannery.cargo_elevator.1")}}, PANEL_WRAP)
				.setColors(colorGold).setOrientation(Orientation.CENTER)));
		scene0.add(new ActionWait(70));
		scene0.add(new ActionRemoveActor(2));
		scene0.add(new ActionWait(10));

		// ====================================================================
		// Scene 1: Self-stacking & Multi-Floor Construction (Cinematic Zoom-out)
		// ====================================================================
		WiajScene scene1 = new WiajScene(script);

		// Show elevator item icon panel above text
		scene1.add(new ActionCreateActor(3, new ActorFancyPanel(Minecraft.getInstance().font, PANEL_X, ICON_Y,
				new Object[][] {{new ItemStack(ModBlocks.CARGO_ELEVATOR.get())}}, 0)
				.setColors(colorGold).setOrientation(Orientation.CENTER)));

		scene1.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, PANEL_X, TEXT_BELOW_ICON_Y,
				new Object[][] {{resolveKey("cannery.cargo_elevator.2")}}, PANEL_WRAP)
				.setColors(colorGold).setOrientation(Orientation.CENTER)));

		scene1.add(new ActionWait(20));

		// Zoom out and pan up to frame the full multi-story shaft (zoom: 2.0 -> 1.35, offsetY: 3.5 -> 0.5)
		scene1.add(new ActionSetZoom(-0.65, 45));
		scene1.add(new ActionOffsetBy(0, -3.0, 0, 45));

		// Start from base height 0, then expand shaft upwards
		scene1.add(new ActionUpdateActor(0, "height", 0));
		scene1.add(new ActionUpdateActor(0, "extension", 0.0D));
		scene1.add(new ActionUpdateActor(0, "prevExtension", 0.0D));
		scene1.add(new ActionUpdateActor(0, "moving", false));

		// Grow up to Floor 2 (height = 5)
		scene1.add(new ActionUpdateActor(0, "height", 2));
		scene1.add(new ActionWait(8));
		scene1.add(new ActionUpdateActor(0, "height", 3));
		scene1.add(new ActionWait(8));
		scene1.add(new ActionUpdateActor(0, "height", 5));

		// Build Floor 2 on the back-right (+Z side: z = 8..11, y = 5, walking surface Y = 6.0)
		buildFloor2(scene1);

		scene1.add(new ActionWait(15));

		// Grow up to Floor 3 (height = 10)
		scene1.add(new ActionUpdateActor(0, "height", 7));
		scene1.add(new ActionWait(8));
		scene1.add(new ActionUpdateActor(0, "height", 9));
		scene1.add(new ActionWait(8));
		scene1.add(new ActionUpdateActor(0, "height", 10));

		// Build Floor 3 on the back-left (+X side: x = 8..11, y = 10, walking surface Y = 11.0)
		buildFloor3(scene1);

		scene1.add(new ActionWait(20));
		scene1.add(new ActionRemoveActor(3));

		scene1.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, PANEL_X, PANEL_Y,
				new Object[][] {{resolveKey("cannery.cargo_elevator.3")}}, PANEL_WRAP)
				.setColors(colorGold).setOrientation(Orientation.CENTER)));
		scene1.add(new ActionWait(70));
		scene1.add(new ActionRemoveActor(2));
		scene1.add(new ActionWait(10));

		// ====================================================================
		// Scene 2: Platform Lift & Player Riding (Floor 1 -> Floor 3)
		// ====================================================================
		WiajScene scene2 = new WiajScene(script);

		scene2.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, PANEL_X, PANEL_Y,
				new Object[][] {{resolveKey("cannery.cargo_elevator.4")}}, PANEL_WRAP)
				.setColors(colorGold).setOrientation(Orientation.CENTER)));

		// Spawn player on the open front walkway (z = 2.5) facing South (+Z, yaw = 0F) toward the elevator
		CompoundTag playerTag = new CompoundTag();
		playerTag.putDouble("x", 6.5D);
		playerTag.putDouble("y", 1.0D);
		playerTag.putDouble("z", 2.5D);
		playerTag.putDouble("prevX", 6.5D);
		playerTag.putDouble("prevY", 1.0D);
		playerTag.putDouble("prevZ", 2.5D);
		playerTag.putFloat("yaw", 0F);
		playerTag.putFloat("prevYaw", 0F);
		playerTag.putFloat("pitch", 0F);
		playerTag.putBoolean("visible", true);
		scene2.add(new ActionCreateActor(1, new ActorPlayer(playerTag)));

		scene2.add(new ActionWait(15));

		// Player walks forward (+Z, yaw = 0F) onto the center of the elevator platform (z: 2.5 -> 6.5)
		for (int step = 1; step <= 16; step++) {
			double z = 2.5D + (4.0D * step / 16.0D);
			scene2.add(new ActionUpdateActor(1, "z", z));
			scene2.add(new ActionWait(1));
		}

		// Turn toward Floor 3 exit (+X, yaw = -90F)
		scene2.add(new ActionUpdateActor(1, "yaw", -90F));
		scene2.add(new ActionWait(10));

		// Activate elevator lift to Floor 3 (targetExtension = 10.0, ridingOffsetY = 1.0 so player stands on platform)
		scene2.add(new ActionUpdateActor(1, "ridingElevatorId", 0));
		scene2.add(new ActionUpdateActor(1, "ridingOffsetY", 1.0D));
		scene2.add(new ActionUpdateActor(1, "riding", true));

		scene2.add(new ActionUpdateActor(0, "targetExtension", 10.0D));
		scene2.add(new ActionUpdateActor(0, "moving", true));

		scene2.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, PANEL_X, PANEL_Y,
				new Object[][] {{resolveKey("cannery.cargo_elevator.5")}}, PANEL_WRAP)
				.setColors(colorGold).setOrientation(Orientation.CENTER)));

		// Camera tracks upward along the shaft (10.0 / 0.15 = 67 ticks travel time)
		scene2.add(new ActionOffsetBy(0, -1.5, 0, 67));
		scene2.add(new ActionWait(6));

		// Player reaches 3rd floor (Y = 11.0) and walks forward (+X, yaw = -90F) onto the upper deck (x: 6.5 -> 9.5)
		scene2.add(new ActionUpdateActor(1, "riding", false));
		scene2.add(new ActionUpdateActor(1, "y", 11.0D));
		scene2.add(new ActionUpdateActor(1, "yaw", -90F));

		for (int step = 1; step <= 14; step++) {
			double x = 6.5D + (3.0D * step / 14.0D);
			scene2.add(new ActionUpdateActor(1, "x", x));
			scene2.add(new ActionWait(1));
		}

		// Turn player around to watch the elevator shaft (-X, yaw = 90F)
		scene2.add(new ActionUpdateActor(1, "yaw", 90F));

		scene2.add(new ActionWait(40));
		scene2.add(new ActionRemoveActor(2));
		scene2.add(new ActionWait(10));

		// ====================================================================
		// Scene 3: Mid-travel Reversal with Camera Rotation & Screwdriver Dismantle
		// ====================================================================
		WiajScene scene3 = new WiajScene(script);

		// Platform starts descending toward 0.0
		scene3.add(new ActionUpdateActor(0, "targetExtension", 0.0D));
		scene3.add(new ActionUpdateActor(0, "moving", true));

		// Halfway down (~32 ticks at 0.15 speed -> extension ~ 5.2)
		scene3.add(new ActionWait(32));

		// Mid-travel reverse click back to 10.0!
		scene3.add(new ActionUpdateActor(0, "targetExtension", 10.0D));
		scene3.add(new ActionUpdateActor(0, "moving", true));

		scene3.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, PANEL_X, PANEL_Y,
				new Object[][] {{resolveKey("cannery.cargo_elevator.6")}}, PANEL_WRAP)
				.setColors(colorGold).setOrientation(Orientation.CENTER)));

		// Smooth camera swing to highlight the mid-travel reversal, then return to exact baseline angle
		scene3.add(new ActionRotateBy(25, -5, 30));
		scene3.add(new ActionWait(15));
		scene3.add(new ActionRotateBy(-25, 5, 30));
		scene3.add(new ActionWait(10));
		scene3.add(new ActionRemoveActor(2));
		scene3.add(new ActionWait(10));

		// Screwdriver dismantle demonstration
		scene3.add(new ActionCreateActor(3, new ActorFancyPanel(Minecraft.getInstance().font, PANEL_X, ICON_Y,
				new Object[][] {{new ItemStack(ModItems.SCREWDRIVER.get())}}, 0)
				.setColors(colorCopper).setOrientation(Orientation.CENTER)));

		scene3.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, PANEL_X, TEXT_BELOW_ICON_Y,
				new Object[][] {{resolveKey("cannery.cargo_elevator.7")}}, PANEL_WRAP)
				.setColors(colorCopper).setOrientation(Orientation.CENTER)));

		scene3.add(new ActionWait(25));

		// Dismantle top floor: height 10 -> 9
		scene3.add(new ActionUpdateActor(0, "height", 9));
		scene3.add(new ActionUpdateActor(0, "targetExtension", 9.0D));
		scene3.add(new ActionUpdateActor(0, "extension", 9.0D));
		scene3.add(new ActionUpdateActor(0, "prevExtension", 9.0D));

		scene3.add(new ActionWait(30));
		scene3.add(new ActionRemoveActor(3));

		// Restore height to 10 for ROR demo
		scene3.add(new ActionUpdateActor(0, "height", 10));
		scene3.add(new ActionUpdateActor(0, "targetExtension", 10.0D));
		scene3.add(new ActionUpdateActor(0, "extension", 10.0D));
		scene3.add(new ActionUpdateActor(0, "prevExtension", 10.0D));

		// Restore camera offset back to +0.5
		scene3.add(new ActionOffsetBy(0, 1.5, 0, 30));

		scene3.add(new ActionWait(30));
		scene3.add(new ActionRemoveActor(2));
		scene3.add(new ActionWait(10));

		// ====================================================================
		// Scene 4: ROR (Redstone Over Radio) & Intermediate Floor Stop
		// ====================================================================
		WiajScene scene4 = new WiajScene(script);

		// Mount Radio Torch Controller on Floor 2 deck at (4, 6, 9)
		scene4.add(new ActionSetBlock(4, 6, 9, ModBlocks.RADIO_TORCH_CONTROLLER.get().defaultBlockState()
				.setValue(RadioTorchBaseBlock.FACING, Direction.UP)));

		// Reset platform to ground floor (extension = 0.0) and position player at ground approach
		scene4.add(new ActionUpdateActor(0, "targetExtension", 0.0D));
		scene4.add(new ActionUpdateActor(0, "extension", 0.0D));
		scene4.add(new ActionUpdateActor(0, "prevExtension", 0.0D));

		scene4.add(new ActionUpdateActor(1, "x", 6.5D));
		scene4.add(new ActionUpdateActor(1, "y", 1.0D));
		scene4.add(new ActionUpdateActor(1, "z", 2.5D));
		scene4.add(new ActionUpdateActor(1, "prevX", 6.5D));
		scene4.add(new ActionUpdateActor(1, "prevY", 1.0D));
		scene4.add(new ActionUpdateActor(1, "prevZ", 2.5D));
		scene4.add(new ActionUpdateActor(1, "yaw", 0F));
		scene4.add(new ActionUpdateActor(1, "prevYaw", 0F));

		// Explain limitation of standard right-click
		scene4.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, PANEL_X, PANEL_Y,
				new Object[][] {{resolveKey("cannery.cargo_elevator.ror.0")}}, PANEL_WRAP)
				.setColors(colorGold).setOrientation(Orientation.CENTER)));
		scene4.add(new ActionWait(65));

		// Show Radio Torch Controller icon
		scene4.add(new ActionCreateActor(3, new ActorFancyPanel(Minecraft.getInstance().font, PANEL_X, ICON_Y,
				new Object[][] {{new ItemStack(ModBlocks.RADIO_TORCH_CONTROLLER.get())}}, 0)
				.setColors(colorBlue).setOrientation(Orientation.CENTER)));

		scene4.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, PANEL_X, TEXT_BELOW_ICON_Y,
				new Object[][] {{resolveKey("cannery.cargo_elevator.ror.1")}}, PANEL_WRAP)
				.setColors(colorBlue).setOrientation(Orientation.CENTER)));
		scene4.add(new ActionWait(65));
		scene4.add(new ActionRemoveActor(3));

		// Player walks forward (+Z, yaw = 0F) onto the ground floor platform (z: 2.5 -> 6.5)
		for (int step = 1; step <= 16; step++) {
			double z = 2.5D + (4.0D * step / 16.0D);
			scene4.add(new ActionUpdateActor(1, "z", z));
			scene4.add(new ActionWait(1));
		}

		scene4.add(new ActionWait(10));

		// ROR Command 1: setextension!5 -> stops at 2nd floor (intermediate mezzanine, Y = 6.0)
		scene4.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, PANEL_X, PANEL_Y,
				new Object[][] {
						{"§b[ RTTY / ROR: \"setextension!5\" ]"},
						{resolveKey("cannery.cargo_elevator.ror.2")}
				}, PANEL_WRAP).setColors(colorBlue).setOrientation(Orientation.CENTER)));

		scene4.add(new ActionUpdateActor(1, "ridingElevatorId", 0));
		scene4.add(new ActionUpdateActor(1, "ridingOffsetY", 1.0D));
		scene4.add(new ActionUpdateActor(1, "riding", true));

		scene4.add(new ActionUpdateActor(0, "targetExtension", 5.0D));
		scene4.add(new ActionUpdateActor(0, "moving", true));

		// Zoom in slightly to highlight the intermediate Floor 2 alignment (5.0 / 0.15 = 34 ticks)
		scene4.add(new ActionSetZoom(0.45, 25));
		scene4.add(new ActionOffsetBy(0, 1.0, 0, 25));
		scene4.add(new ActionWait(12));

		// Player steps off forward (+Z, yaw = 0F) onto the 2nd Floor walkway at Y = 6.0 (z: 6.5 -> 9.5)
		scene4.add(new ActionUpdateActor(1, "riding", false));
		scene4.add(new ActionUpdateActor(1, "y", 6.0D));
		scene4.add(new ActionUpdateActor(1, "yaw", 0F));

		for (int step = 1; step <= 14; step++) {
			double z = 6.5D + (3.0D * step / 14.0D);
			scene4.add(new ActionUpdateActor(1, "z", z));
			scene4.add(new ActionWait(1));
		}

		scene4.add(new ActionWait(35));

		// Player turns around (-Z, yaw = 180F) and walks forward back onto the platform (z: 9.5 -> 6.5)
		scene4.add(new ActionUpdateActor(1, "yaw", 180F));
		scene4.add(new ActionWait(5));
		for (int step = 1; step <= 14; step++) {
			double z = 9.5D - (3.0D * step / 14.0D);
			scene4.add(new ActionUpdateActor(1, "z", z));
			scene4.add(new ActionWait(1));
		}

		// Face toward Floor 3 exit (+X, yaw = -90F)
		scene4.add(new ActionUpdateActor(1, "yaw", -90F));
		scene4.add(new ActionWait(10));

		// ROR Command 2: setextension!10 -> sends up to 3rd floor (top floor, Y = 11.0)
		scene4.add(new ActionCreateActor(2, new ActorFancyPanel(Minecraft.getInstance().font, PANEL_X, PANEL_Y,
				new Object[][] {
						{"§b[ RTTY / ROR: \"setextension!10\" ]"},
						{resolveKey("cannery.cargo_elevator.ror.3")}
				}, PANEL_WRAP).setColors(colorBlue).setOrientation(Orientation.CENTER)));

		scene4.add(new ActionUpdateActor(1, "riding", true));
		scene4.add(new ActionUpdateActor(0, "targetExtension", 10.0D));
		scene4.add(new ActionUpdateActor(0, "moving", true));

		// Zoom out for full facility view
		scene4.add(new ActionSetZoom(-0.45, 25));
		scene4.add(new ActionOffsetBy(0, -1.0, 0, 25));
		scene4.add(new ActionWait(12));

		// Player exits forward (+X, yaw = -90F) onto 3rd floor walkway at Y = 11.0 (x: 6.5 -> 9.5)
		scene4.add(new ActionUpdateActor(1, "riding", false));
		scene4.add(new ActionUpdateActor(1, "y", 11.0D));
		scene4.add(new ActionUpdateActor(1, "yaw", -90F));

		for (int step = 1; step <= 14; step++) {
			double x = 6.5D + (3.0D * step / 14.0D);
			scene4.add(new ActionUpdateActor(1, "x", x));
			scene4.add(new ActionWait(1));
		}

		scene4.add(new ActionWait(70));
		scene4.add(new ActionRemoveActor(2));
		scene4.add(new ActionWait(10));

		script.addScene(scene0).addScene(scene1).addScene(scene2).addScene(scene3).addScene(scene4);
		return script;
	}

	/**
	 * Builds Floor 2 (Mezzanine at Y = 5 slab, Y = 6.0 walking surface) on the back-right (+Z) side of the shaft.
	 * The elevator shaft occupies x: 5..7, z: 5..7, so this deck starts cleanly at z = 8.
	 */
	private static void buildFloor2(WiajScene scene) {
		for (int x = 3; x <= 10; x++) {
			for (int z = 8; z <= 11; z++) {
				if (z == 8 && x >= 5 && x <= 7) {
					scene.add(new ActionSetBlock(x, 5, z, ModBlocks.CONCRETE_HAZARD.get()));
				} else {
					scene.add(new ActionSetBlock(x, 5, z, ModBlocks.CONCRETE_SMOOTH.get()));
				}
			}
		}

		Set<BlockPos> railingPositions = new HashSet<>();
		// West edge (x = 3)
		for (int z = 8; z <= 11; z++) railingPositions.add(new BlockPos(3, 6, z));
		// South edge (z = 11)
		for (int x = 3; x <= 10; x++) railingPositions.add(new BlockPos(x, 6, 11));
		// East edge (x = 10)
		for (int z = 8; z <= 11; z++) railingPositions.add(new BlockPos(10, 6, z));
		// North threshold facing shaft at z = 8 (leaving x: 5..7 open for the elevator entrance)
		for (int x = 3; x <= 4; x++) railingPositions.add(new BlockPos(x, 6, 8));
		for (int x = 8; x <= 10; x++) railingPositions.add(new BlockPos(x, 6, 8));

		buildConnectedRailings(scene, railingPositions, Collections.emptySet());
	}

	/**
	 * Builds Floor 3 (Upper Deck at Y = 10 slab, Y = 11.0 walking surface) on the back-left (+X) side of the shaft.
	 * The elevator shaft occupies x: 5..7, z: 5..7, so this deck starts cleanly at x = 8.
	 */
	private static void buildFloor3(WiajScene scene) {
		for (int x = 8; x <= 11; x++) {
			for (int z = 3; z <= 10; z++) {
				if (x == 8 && z >= 5 && z <= 7) {
					scene.add(new ActionSetBlock(x, 10, z, ModBlocks.CONCRETE_HAZARD.get()));
				} else {
					scene.add(new ActionSetBlock(x, 10, z, ModBlocks.CONCRETE_SMOOTH.get()));
				}
			}
		}

		Set<BlockPos> railingPositions = new HashSet<>();
		// North edge (z = 3)
		for (int x = 8; x <= 11; x++) railingPositions.add(new BlockPos(x, 11, 3));
		// East edge (x = 11)
		for (int z = 3; z <= 10; z++) railingPositions.add(new BlockPos(11, 11, z));
		// South edge (z = 10)
		for (int x = 8; x <= 11; x++) railingPositions.add(new BlockPos(x, 11, 10));
		// West threshold facing shaft at x = 8 (leaving z: 5..7 open for the elevator entrance)
		for (int z = 3; z <= 4; z++) railingPositions.add(new BlockPos(8, 11, z));
		for (int z = 8; z <= 10; z++) railingPositions.add(new BlockPos(8, 11, z));

		buildConnectedRailings(scene, railingPositions, Collections.emptySet());
	}

	/**
	 * Places iron bars with explicit directional connection properties so that adjacent
	 * railings seamlessly connect without isolated posts.
	 */
	private static void buildConnectedRailings(WiajScene scene, Set<BlockPos> railingPositions, Set<BlockPos> solidPositions) {
		for (BlockPos pos : railingPositions) {
			boolean north = railingPositions.contains(pos.north()) || solidPositions.contains(pos.north());
			boolean south = railingPositions.contains(pos.south()) || solidPositions.contains(pos.south());
			boolean east = railingPositions.contains(pos.east()) || solidPositions.contains(pos.east());
			boolean west = railingPositions.contains(pos.west()) || solidPositions.contains(pos.west());
			scene.add(new ActionSetBlock(pos.getX(), pos.getY(), pos.getZ(), ironBars(north, south, east, west)));
		}
	}

	/** Creates an IronBars BlockState with explicit connections. */
	private static BlockState ironBars(boolean north, boolean south, boolean east, boolean west) {
		return Blocks.IRON_BARS.defaultBlockState()
				.setValue(IronBarsBlock.NORTH, north)
				.setValue(IronBarsBlock.SOUTH, south)
				.setValue(IronBarsBlock.EAST, east)
				.setValue(IronBarsBlock.WEST, west);
	}

	private static String resolveKey(String key) {
		return I18n.get(key);
	}

	/**
	 * Renders the Cargo Elevator multiblock via {@link MachineCargoElevatorRenderer} with animated extension,
	 * repeated guides per floor, and telescoping piston segments.
	 */
	public static class ActorCargoElevator implements ITileActorRenderer {

		static final int GUI_POS_OFFSET = 30_000_000;
		private CargoElevatorBlockEntity elevator;

		@Override
		public void renderActor(GuiGraphics gui, VirtualWorld world, int ticks, float interp, CompoundTag data) {
			double x = data.getDouble("x");
			double y = data.getDouble("y");
			double z = data.getDouble("z");
			int height = data.getInt("height");
			double ext = data.getDouble("extension");
			double prevExt = data.contains("prevExtension") ? data.getDouble("prevExtension") : ext;

			gui.pose().pushPose();
			gui.pose().translate(x, y, z);

			if (this.elevator == null) {
				this.elevator = new CargoElevatorBlockEntity(
						new BlockPos((int) x + GUI_POS_OFFSET, (int) y, (int) z),
						ModBlocks.CARGO_ELEVATOR.get().defaultBlockState());
			}

			this.elevator.height = height;
			this.elevator.extension = ext;
			this.elevator.prevExtension = prevExt;
			this.elevator.renderPlatform = true;

			BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
			BlockEntityRenderer<BlockEntity> ber = dispatcher.getRenderer(this.elevator);
			if (ber != null) {
				ber.render(this.elevator, interp, gui.pose(), gui.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			}

			gui.pose().popPose();
		}

		@Override
		public void updateActor(int ticks, CompoundTag data) {
			double ext = data.getDouble("extension");
			data.putDouble("prevExtension", ext);

			if (data.getBoolean("moving") && data.contains("targetExtension")) {
				double target = data.getDouble("targetExtension");
				double speed = data.contains("speed") ? data.getDouble("speed") : CargoElevatorBlockEntity.SPEED;
				if (ext < target) {
					ext = Math.min(target, ext + speed);
				} else if (ext > target) {
					ext = Math.max(target, ext - speed);
				}
				data.putDouble("extension", ext);
				if (Math.abs(ext - target) < 1.0e-5) {
					data.putBoolean("moving", false);
				}
			}
		}
	}
}