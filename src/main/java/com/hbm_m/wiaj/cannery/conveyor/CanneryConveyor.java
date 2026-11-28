package com.hbm_m.wiaj.cannery.conveyor;

import static com.hbm_m.wiaj.cannery.conveyor.ConveyorWiajHelpers.*;

import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.MachineCraneSplitterBlock;
import com.hbm_m.block.network.ConveyorBend;
import com.hbm_m.item.ModItems;
import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;
import com.hbm_m.wiaj.WiajScript;
import com.hbm_m.wiaj.action.*;
import com.hbm_m.wiaj.actor.ActorFancyPanel;
import com.hbm_m.wiaj.actor.ActorFancyPanel.Orientation;
import com.hbm_m.wiaj.cannery.CanneryBase;
import com.hbm_m.wiaj.cannery.CanneryConveyorPress;
import com.hbm_m.wiaj.cannery.conveyor.ConveyorWiajActors.*;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 3D WIAJ demonstration for Conveyors and Item Logistics (6 scenes):
 * 0) 4 belt tiers side by side (Regular, Double, Triple, Express + parabolic launch)
 * 1) Screwdriver bends and Conveyor Wand auto-routing / line-wide upgrade
 * 2) Vertical logistics (Chain Lift & Chute) with two-tier concrete overpass
 * 3) I/O & Splitting (Ejector, Inserter, Round-Robin Splitter)
 * 4) Packaging (Boxer & Unboxer) to prevent factory congestion
 * 5) Conveyor Sorter with 2D color-coded face filter overlay
 */
public class CanneryConveyor extends CanneryBase {

	private static final int SIZE_X = 13;
	private static final int SIZE_Y = 7;
	private static final int SIZE_Z = 11;

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
		VirtualWorld world = new VirtualWorld(SIZE_X, SIZE_Y, SIZE_Z);
		WiajScript script = new WiajScript(world);
		var font = Minecraft.getInstance().font;

		// ====================================================================
		// SCENE 0: 4 basic belt tiers side by side
		// ====================================================================
		WiajScene scene0 = new WiajScene(script);
		scene0.add(new ActionSetZoom(1.65, 0));
		buildIndustrialFloor(scene0, SIZE_X, SIZE_Z);
		scene0.add(new ActionWait(6));

		for (int x = 2; x <= 8; x++) {
			scene0.add(new ActionSetBlock(x, 1, 2, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT)));
			scene0.add(new ActionSetBlock(x, 1, 4, beltDouble(Direction.EAST)));
			scene0.add(new ActionSetBlock(x, 1, 6, beltTriple(Direction.EAST)));
			scene0.add(new ActionSetBlock(x, 1, 8, beltExpress(Direction.EAST, ConveyorBend.STRAIGHT)));
			scene0.add(new ActionWait(2));
		}

		// Stream 1: Regular (1 lane)
		scene0.add(new ActionCreateActor(ID_STREAM_BASE, new ActorItemStream(
				new ItemStack(Items.IRON_INGOT), 0.08, 2.2,
				new double[] { 2.2, 1.25, 2.5 }, new double[] { 8.9, 1.25, 2.5 })));

		// Stream 2: Double (2 lanes: +/- 0.25)
		scene0.add(new ActionCreateActor(ID_STREAM_BASE + 1, new ActorItemStream(
				new ItemStack(Items.COPPER_INGOT), 0.08, 2.2,
				new double[] { 2.2, 1.25, 4.25 }, new double[] { 8.9, 1.25, 4.25 })));
		scene0.add(new ActionCreateActor(ID_STREAM_BASE + 2, new ActorItemStream(
				List.of(new ItemStack(Items.COPPER_INGOT)), 0.08, 2.2, 1.1, 0.0,
				new double[] { 2.2, 1.25, 4.75 }, new double[] { 8.9, 1.25, 4.75 })));

		// Stream 3: Triple (3 lanes: +/- 0.3125)
		scene0.add(new ActionCreateActor(ID_STREAM_BASE + 3, new ActorItemStream(
				new ItemStack(Items.COAL), 0.08, 2.2,
				new double[] { 2.2, 1.25, 6.1875 }, new double[] { 8.9, 1.25, 6.1875 })));
		scene0.add(new ActionCreateActor(ID_STREAM_BASE + 4, new ActorItemStream(
				List.of(new ItemStack(Items.IRON_INGOT)), 0.08, 2.2, 0.7, 0.0,
				new double[] { 2.2, 1.25, 6.50 }, new double[] { 8.9, 1.25, 6.50 })));
		scene0.add(new ActionCreateActor(ID_STREAM_BASE + 5, new ActorItemStream(
				List.of(new ItemStack(Items.GOLD_INGOT)), 0.08, 2.2, 1.4, 0.0,
				new double[] { 2.2, 1.25, 6.8125 }, new double[] { 8.9, 1.25, 6.8125 })));

		// Stream 4: Express (3x speed + parabolic launch off the end)
		scene0.add(new ActionCreateActor(ID_STREAM_BASE + 6, new ActorItemStream(
				List.of(new ItemStack(Items.GOLD_INGOT)), 0.22, 2.8, 0.0, 0.55,
				new double[] { 2.2, 1.25, 8.5 },
				new double[] { 8.9, 1.25, 8.5 },
				new double[] { 10.8, 1.08, 8.5 })));

		scene0.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, PANEL_TOP_Y,
				new Object[][] {
					{ new ItemStack(ModItems.CONVEYOR_WAND_REGULAR.get()), new ItemStack(ModItems.CONVEYOR_WAND_TRIPLE.get()), new ItemStack(ModItems.CONVEYOR_WAND_EXPRESS.get()) },
					{ tr("cannery.conveyor.0.tiers") }
				}, 235).setColors(colorBlue).setOrientation(Orientation.BOTTOM)));

		scene0.add(new ActionWait(95));
		for (int i = 0; i <= 6; i++) scene0.add(new ActionRemoveActor(ID_STREAM_BASE + i));
		scene0.add(new ActionRemoveActor(ID_PANEL_MAIN));

		// ====================================================================
		// SCENE 1: Screwdriver bends & Conveyor Wand routing/upgrading
		// ====================================================================
		WiajScene scene1 = new WiajScene(script);
		clearStageAboveFloor(scene1, SIZE_X, SIZE_Y, SIZE_Z);

		scene1.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, PANEL_TOP_Y,
				new Object[][] {
					{ new ItemStack(ModItems.SCREWDRIVER.get()), new ItemStack(ModItems.CONVEYOR_WAND_REGULAR.get()) },
					{ tr("cannery.conveyor.1.wand_route") }
				}, 230).setColors(colorGold).setOrientation(Orientation.BOTTOM)));

		scene1.add(new ActionSetBlock(2, 1, 3, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT))); scene1.add(new ActionWait(3));
		scene1.add(new ActionSetBlock(3, 1, 3, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT))); scene1.add(new ActionWait(3));
		scene1.add(new ActionSetBlock(4, 1, 3, beltRegular(Direction.EAST, ConveyorBend.RIGHT)));    scene1.add(new ActionWait(3));
		scene1.add(new ActionSetBlock(4, 1, 4, beltRegular(Direction.SOUTH, ConveyorBend.STRAIGHT))); scene1.add(new ActionWait(3));
		scene1.add(new ActionSetBlock(4, 1, 5, beltRegular(Direction.SOUTH, ConveyorBend.STRAIGHT))); scene1.add(new ActionWait(3));
		scene1.add(new ActionSetBlock(4, 1, 6, beltRegular(Direction.SOUTH, ConveyorBend.LEFT)));     scene1.add(new ActionWait(3));
		scene1.add(new ActionSetBlock(5, 1, 6, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT))); scene1.add(new ActionWait(3));
		scene1.add(new ActionSetBlock(6, 1, 6, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT))); scene1.add(new ActionWait(3));
		scene1.add(new ActionSetBlock(7, 1, 6, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT))); scene1.add(new ActionWait(3));
		scene1.add(new ActionSetBlock(8, 1, 6, beltInserter(Direction.EAST)));
		scene1.add(new ActionSetBlock(9, 1, 6, ModBlocks.CRATE.get()));

		scene1.add(new ActionCreateActor(ID_HIGHLIGHT_BASE, new ActorWorldHighlight(4, 1, 3, 5, 1.35, 4, 0xAAFAD64A)));

		scene1.add(new ActionCreateActor(ID_STREAM_BASE, new ActorItemStream(
				new ItemStack(Items.IRON_INGOT), 0.08, 2.0,
				new double[] { 2.2, 1.25, 3.5 },
				new double[] { 4.5, 1.25, 3.5 },
				new double[] { 4.5, 1.25, 6.5 },
				new double[] { 8.3, 1.25, 6.5 })));
		scene1.add(new ActionWait(55));
		scene1.add(new ActionRemoveActor(ID_HIGHLIGHT_BASE));
		scene1.add(new ActionRemoveActor(ID_PANEL_MAIN));

		// Line-wide upgrade with Express Wand
		scene1.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, PANEL_TOP_Y,
				new Object[][] {
					{ new ItemStack(ModItems.CONVEYOR_WAND_EXPRESS.get()) },
					{ tr("cannery.conveyor.1.wand_upgrade") }
				}, 230).setColors(COLOR_GREEN).setOrientation(Orientation.BOTTOM)));

		scene1.add(new ActionSetBlock(2, 1, 3, beltExpress(Direction.EAST, ConveyorBend.STRAIGHT)));
		scene1.add(new ActionSetBlock(3, 1, 3, beltExpress(Direction.EAST, ConveyorBend.STRAIGHT)));
		scene1.add(new ActionSetBlock(4, 1, 3, beltExpress(Direction.EAST, ConveyorBend.RIGHT)));
		scene1.add(new ActionSetBlock(4, 1, 4, beltExpress(Direction.SOUTH, ConveyorBend.STRAIGHT)));
		scene1.add(new ActionSetBlock(4, 1, 5, beltExpress(Direction.SOUTH, ConveyorBend.STRAIGHT)));
		scene1.add(new ActionSetBlock(4, 1, 6, beltExpress(Direction.SOUTH, ConveyorBend.LEFT)));
		scene1.add(new ActionSetBlock(5, 1, 6, beltExpress(Direction.EAST, ConveyorBend.STRAIGHT)));
		scene1.add(new ActionSetBlock(6, 1, 6, beltExpress(Direction.EAST, ConveyorBend.STRAIGHT)));
		scene1.add(new ActionSetBlock(7, 1, 6, beltExpress(Direction.EAST, ConveyorBend.STRAIGHT)));
		scene1.add(new ActionUpdateActor(ID_STREAM_BASE, "speed", 0.20));

		scene1.add(new ActionWait(65));
		scene1.add(new ActionRemoveActor(ID_STREAM_BASE));
		scene1.add(new ActionRemoveActor(ID_PANEL_MAIN));

		// ====================================================================
		// SCENE 2: Vertical Logistics (Chain Lift & Chute + camera orbit)
		// ====================================================================
		WiajScene scene2 = new WiajScene(script);
		clearStageAboveFloor(scene2, SIZE_X, SIZE_Y, SIZE_Z);

		fillBox(scene2, 4, 1, 5, 8, 3, 5, ModBlocks.CONCRETE_SMOOTH.get());
		scene2.add(new ActionSetBlock(4, 3, 5, ModBlocks.CONCRETE_HAZARD.get()));
		scene2.add(new ActionSetBlock(8, 3, 5, ModBlocks.CONCRETE_HAZARD.get()));

		scene2.add(new ActionSetBlock(2, 1, 5, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT)));
		for (int y = 1; y <= 3; y++) scene2.add(new ActionSetBlock(3, y, 5, beltLift(Direction.EAST)));
		for (int x = 3; x <= 8; x++) scene2.add(new ActionSetBlock(x, 4, 5, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT)));
		for (int y = 4; y >= 2; y--) scene2.add(new ActionSetBlock(9, y, 5, beltChute(Direction.EAST)));
		scene2.add(new ActionSetBlock(9, 1, 5, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT)));
		scene2.add(new ActionSetBlock(10, 1, 5, beltInserter(Direction.EAST)));
		scene2.add(new ActionSetBlock(11, 1, 5, ModBlocks.CRATE.get()));

		scene2.add(new ActionCreateActor(ID_STREAM_BASE, new ActorItemStream(
				new ItemStack(ModBlocks.CRATE.get()), 0.14, 3.0,
				new double[] { 2.2, 1.25, 5.5 },
				new double[] { 3.5, 1.25, 5.5 },
				new double[] { 3.5, 4.25, 5.5 },
				new double[] { 9.5, 4.25, 5.5 },
				new double[] { 9.5, 1.25, 5.5 },
				new double[] { 10.3, 1.25, 5.5 })));

		scene2.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, PANEL_TOP_Y,
				new Object[][] {
					{ tr("cannery.conveyor.2.vertical") }
				}, 235).setColors(COLOR_CYAN).setOrientation(Orientation.BOTTOM)));

		scene2.add(new ActionRotateBy(35, -6, 25));
		scene2.add(new ActionWait(55));
		scene2.add(new ActionRotateBy(-35, 6, 25));
		scene2.add(new ActionRemoveActor(ID_STREAM_BASE));
		scene2.add(new ActionRemoveActor(ID_PANEL_MAIN));

		// ====================================================================
		// SCENE 3: I/O (Ejector / Inserter) & Round-Robin Splitter
		// ====================================================================
		WiajScene scene3 = new WiajScene(script);
		clearStageAboveFloor(scene3, SIZE_X, SIZE_Y, SIZE_Z);

		scene3.add(new ActionSetBlock(2, 1, 4, ModBlocks.CRATE.get()));
		scene3.add(new ActionSetBlock(3, 1, 4, beltEjector(Direction.EAST)));
		scene3.add(new ActionSetBlock(4, 1, 4, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT)));
		scene3.add(new ActionSetBlock(5, 1, 4, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT)));

		// 2-block Splitter (LEFT at z=4, RIGHT at z=5 for EAST travel)
		scene3.add(new ActionSetBlock(6, 1, 4, beltSplitter(Direction.EAST, MachineCraneSplitterBlock.Half.LEFT)));
		scene3.add(new ActionSetBlock(6, 1, 5, beltSplitter(Direction.EAST, MachineCraneSplitterBlock.Half.RIGHT)));

		for (int x = 7; x <= 9; x++) {
			scene3.add(new ActionSetBlock(x, 1, 4, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT)));
			scene3.add(new ActionSetBlock(x, 1, 5, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT)));
		}
		scene3.add(new ActionSetBlock(10, 1, 4, beltInserter(Direction.EAST)));
		scene3.add(new ActionSetBlock(10, 1, 5, beltInserter(Direction.EAST)));
		scene3.add(new ActionSetBlock(11, 1, 4, ModBlocks.CRATE.get()));
		scene3.add(new ActionSetBlock(11, 1, 5, ModBlocks.CRATE.get()));

		// Highlights: red = Ejector, green = Inserters
		scene3.add(new ActionCreateActor(ID_HIGHLIGHT_BASE,     new ActorWorldHighlight(3, 1, 4, 4, 2, 5, 0x88E53935)));
		scene3.add(new ActionCreateActor(ID_HIGHLIGHT_BASE + 1, new ActorWorldHighlight(10, 1, 4, 11, 2, 5, 0x884CD964)));
		scene3.add(new ActionCreateActor(ID_HIGHLIGHT_BASE + 2, new ActorWorldHighlight(10, 1, 5, 11, 2, 6, 0x884CD964)));

		// Dense input stream -> two alternating round-robin output lanes
		scene3.add(new ActionCreateActor(ID_STREAM_BASE, new ActorItemStream(
				new ItemStack(Items.IRON_INGOT), 0.10, 1.5,
				new double[] { 3.5, 1.25, 4.5 }, new double[] { 6.2, 1.25, 4.5 })));
		scene3.add(new ActionCreateActor(ID_STREAM_BASE + 1, new ActorItemStream(
				List.of(new ItemStack(Items.IRON_INGOT)), 0.10, 3.0, 2.7, 0.0,
				new double[] { 6.5, 1.25, 4.5 }, new double[] { 10.3, 1.25, 4.5 })));
		scene3.add(new ActionCreateActor(ID_STREAM_BASE + 2, new ActorItemStream(
				List.of(new ItemStack(Items.IRON_INGOT)), 0.10, 3.0, 4.2, 0.0,
				new double[] { 6.5, 1.25, 5.5 }, new double[] { 10.3, 1.25, 5.5 })));

		scene3.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, PANEL_TOP_Y,
				new Object[][] {
					{ new ItemStack(ModBlocks.CRANE_EXTRACTOR.get()), new ItemStack(ModBlocks.CRANE_SPLITTER.get()), new ItemStack(ModBlocks.CRANE_INSERTER.get()) },
					{ tr("cannery.conveyor.3.io_splitter") }
				}, 240).setColors(colorGold).setOrientation(Orientation.BOTTOM)));

		scene3.add(new ActionWait(95));
		for (int i = 0; i < 3; i++) {
			scene3.add(new ActionRemoveActor(ID_STREAM_BASE + i));
			scene3.add(new ActionRemoveActor(ID_HIGHLIGHT_BASE + i));
		}
		scene3.add(new ActionRemoveActor(ID_PANEL_MAIN));

		// ====================================================================
		// SCENE 4: Packaging & Unpackaging (Boxer & Unboxer)
		// ====================================================================
		WiajScene scene4 = new WiajScene(script);
		clearStageAboveFloor(scene4, SIZE_X, SIZE_Y, SIZE_Z);

		scene4.add(new ActionSetBlock(1, 1, 5, ModBlocks.CRATE.get()));
		scene4.add(new ActionSetBlock(2, 1, 5, beltEjector(Direction.EAST)));
		scene4.add(new ActionSetBlock(3, 1, 5, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT)));
		scene4.add(new ActionSetBlock(4, 1, 5, beltBoxer(Direction.EAST)));
		scene4.add(new ActionSetBlock(5, 1, 5, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT)));
		scene4.add(new ActionSetBlock(6, 1, 5, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT)));
		scene4.add(new ActionSetBlock(7, 1, 5, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT)));
		scene4.add(new ActionSetBlock(8, 1, 5, beltUnboxer(Direction.EAST)));
		scene4.add(new ActionSetBlock(9, 1, 5, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT)));
		scene4.add(new ActionSetBlock(10, 1, 5, beltInserter(Direction.EAST)));
		scene4.add(new ActionSetBlock(11, 1, 5, ModBlocks.CRATE.get()));

		scene4.add(new ActionCreateActor(ID_STREAM_BASE, new ActorItemStream(
				new ItemStack(Items.SANDSTONE), 0.10, 0.75,
				new double[] { 2.5, 1.25, 5.5 }, new double[] { 4.2, 1.25, 5.5 })));
		scene4.add(new ActionCreateActor(ID_STREAM_BASE + 1, new ActorItemStream(
				List.of(new ItemStack(ModBlocks.CRATE.get())), 0.10, 3.2, 1.8, 0.0,
				new double[] { 4.6, 1.35, 5.5 }, new double[] { 8.3, 1.35, 5.5 })));
		scene4.add(new ActionCreateActor(ID_STREAM_BASE + 2, new ActorItemStream(
				List.of(new ItemStack(Items.SANDSTONE)), 0.10, 0.75, 5.4, 0.0,
				new double[] { 8.6, 1.25, 5.5 }, new double[] { 10.3, 1.25, 5.5 })));

		scene4.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, PANEL_TOP_Y,
				new Object[][] {
					{ new ItemStack(ModBlocks.CRANE_BOXER.get()), new ItemStack(ModBlocks.CRANE_UNBOXER.get()) },
					{ tr("cannery.conveyor.4.boxer") }
				}, 240).setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

		scene4.add(new ActionWait(95));
		for (int i = 0; i < 3; i++) scene4.add(new ActionRemoveActor(ID_STREAM_BASE + i));
		scene4.add(new ActionRemoveActor(ID_PANEL_MAIN));

		// ====================================================================
		// SCENE 5: Smart Sorting (Conveyor Sorter + 2D filter overlay)
		// ====================================================================
		WiajScene scene5 = new WiajScene(script);
		clearStageAboveFloor(scene5, SIZE_X, SIZE_Y, SIZE_Z);
		scene5.add(new ActionOffsetBy(-1.8, 0.0, 0.0, 20));

		scene5.add(new ActionSetBlock(2, 1, 5, ModBlocks.CRATE.get()));
		scene5.add(new ActionSetBlock(3, 1, 5, beltEjector(Direction.EAST)));
		scene5.add(new ActionSetBlock(4, 1, 5, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT)));
		scene5.add(new ActionSetBlock(5, 1, 5, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT)));
		scene5.add(new ActionSetBlock(6, 1, 5, beltSorter()));

		// 3 output branches: North (-Z, Iron), East (+X, Coal), South (+Z, Gold)
		scene5.add(new ActionSetBlock(6, 1, 4, beltRegular(Direction.NORTH, ConveyorBend.STRAIGHT)));
		scene5.add(new ActionSetBlock(6, 1, 3, beltRegular(Direction.NORTH, ConveyorBend.STRAIGHT)));
		scene5.add(new ActionSetBlock(6, 1, 2, beltInserter(Direction.NORTH)));
		scene5.add(new ActionSetBlock(6, 1, 1, ModBlocks.CRATE.get()));

		scene5.add(new ActionSetBlock(7, 1, 5, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT)));
		scene5.add(new ActionSetBlock(8, 1, 5, beltRegular(Direction.EAST, ConveyorBend.STRAIGHT)));
		scene5.add(new ActionSetBlock(9, 1, 5, beltInserter(Direction.EAST)));
		scene5.add(new ActionSetBlock(10, 1, 5, ModBlocks.CRATE.get()));

		scene5.add(new ActionSetBlock(6, 1, 6, beltRegular(Direction.SOUTH, ConveyorBend.STRAIGHT)));
		scene5.add(new ActionSetBlock(6, 1, 7, beltRegular(Direction.SOUTH, ConveyorBend.STRAIGHT)));
		scene5.add(new ActionSetBlock(6, 1, 8, beltInserter(Direction.SOUTH)));
		scene5.add(new ActionSetBlock(6, 1, 9, ModBlocks.CRATE.get()));

		ItemStack iron = new ItemStack(Items.IRON_INGOT);
		ItemStack coal = new ItemStack(Items.COAL);
		ItemStack gold = new ItemStack(Items.GOLD_INGOT);

		scene5.add(new ActionCreateActor(ID_STREAM_BASE, new ActorItemStream(
				List.of(iron, coal, gold), 0.10, 1.5, 0.0, 0.0,
				new double[] { 3.5, 1.25, 5.5 }, new double[] { 6.3, 1.25, 5.5 })));

		scene5.add(new ActionCreateActor(ID_STREAM_BASE + 1, new ActorItemStream(
				List.of(iron), 0.10, 4.5, 2.8, 0.0,
				new double[] { 6.5, 1.25, 5.0 }, new double[] { 6.5, 1.25, 2.2 })));
		scene5.add(new ActionCreateActor(ID_STREAM_BASE + 2, new ActorItemStream(
				List.of(coal), 0.10, 4.5, 4.3, 0.0,
				new double[] { 6.9, 1.25, 5.5 }, new double[] { 9.3, 1.25, 5.5 })));
		scene5.add(new ActionCreateActor(ID_STREAM_BASE + 3, new ActorItemStream(
				List.of(gold), 0.10, 4.5, 5.8, 0.0,
				new double[] { 6.5, 1.25, 6.0 }, new double[] { 6.5, 1.25, 8.3 })));

		scene5.add(new ActionCreateActor(ID_GUI_OVERLAY, new ActorSorterGuiPreview(112, -5, iron, coal, gold)));

		scene5.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, -55, PANEL_BOT_Y,
				new Object[][] {
					{ new ItemStack(ModBlocks.CRANE_ROUTER.get()) },
					{ tr("cannery.conveyor.5.sorter") }
				}, 215).setColors(COLOR_GREEN).setOrientation(Orientation.TOP)));

		scene5.add(new ActionWait(110));
		for (int i = 0; i < 4; i++) scene5.add(new ActionRemoveActor(ID_STREAM_BASE + i));
		scene5.add(new ActionRemoveActor(ID_GUI_OVERLAY));
		scene5.add(new ActionRemoveActor(ID_PANEL_MAIN));
		scene5.add(new ActionOffsetBy(1.8, 0.0, 0.0, 20));

		script.addScene(scene0).addScene(scene1).addScene(scene2)
			  .addScene(scene3).addScene(scene4).addScene(scene5);
		return script;
	}
}
