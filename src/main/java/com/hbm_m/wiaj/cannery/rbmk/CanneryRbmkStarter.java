package com.hbm_m.wiaj.cannery.rbmk;

import static com.hbm_m.wiaj.cannery.rbmk.RbmkWiajHelpers.*;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.MachineIndustrialTurbineBlock;
import com.hbm_m.block.machines.MachineRbmkConsoleBlock;
import com.hbm_m.blockentity.machines.MachineCoolingTowerBlockEntity;
import com.hbm_m.blockentity.machines.MachineIndustrialTurbineBlockEntity;
import com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
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
import com.hbm_m.wiaj.actor.ActorTileEntity;
import com.hbm_m.wiaj.cannery.CanneryBase;
import com.hbm_m.wiaj.cannery.rbmk.RbmkWiajActors.ActorNeutronFlux;
import com.hbm_m.wiaj.cannery.rbmk.RbmkWiajActors.ActorRbmkGuiPreview;
import com.hbm_m.wiaj.cannery.rbmk.RbmkWiajActors.ActorRbmkGuiPreview.Mode;
import com.hbm_m.wiaj.cannery.rbmk.RbmkWiajActors.ActorRbmkMachine;
import com.hbm_m.wiaj.cannery.rbmk.RbmkWiajActors.ActorWorldHighlight;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;

/**
 * First working 4-rod RBMK reactor (4 scenes):
 * 0) Step-by-step "#" core assembly (4 fuel, 4 boiler, 4 control, moderators, reflectors, lids)
 * 1) Bottom plumbing: Water (y=3) + Steam Connector (y=2) + Steam (y=1)
 * 2) Adjacent turbine cascade -> Cooling Tower -> closed water loop + red cables
 * 3) Console linking (15x15, AZ-5) and Ra-226-Be neutron source kickstart
 */
public class CanneryRbmkStarter extends CanneryBase {

	private static final int CX = 6;
	private static final int CZ = 8;
	private static final int COL_Y = 4;

	@Override
	public ItemStack getIcon() {
		return new ItemStack(blockForColumn(RbmkColType.BOILER));
	}

	@Override
	public String getName() {
		return "cannery.rbmk_starter";
	}

	@Override
	public CanneryBase[] seeAlso() {
		return new CanneryBase[] {
				new CanneryRbmkIntro(),
				new CanneryRbmkBasics(),
				new CanneryRbmkAdvanced()
		};
	}

	@Override
	public WiajScript createScript() {
		VirtualWorld world = new VirtualWorld(34, 14, 17);
		WiajScript script = new WiajScript(world);
		var font = Minecraft.getInstance().font;

		// =====================================================================
		// SCENE 0: Step-by-step 4-rod core assembly
		// =====================================================================
		WiajScene scene0 = new WiajScene(script);
		scene0.add(new ActionSetZoom(0.82, 0));
		buildIndustrialFloor(scene0, 34, 17);

		int colId = ID_COLUMN_BASE;

		// 1) 4 fuel channels spaced 3 blocks apart
		int[][] fuels = { {CX-2, CZ-2}, {CX+2, CZ-2}, {CX-2, CZ+2}, {CX+2, CZ+2} };
		for (int[] p : fuels) {
			placeColumn(scene0, colId++, p[0], COL_Y, p[1], RbmkColType.FUEL, 2, 0.0, true);
		}
		scene0.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, -78,
				new Object[][] { { tr("cannery.rbmk.starter.0.core_layout") } }, 235)
				.setColors(colorBlue).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionWait(35));

		// 2) Between fuel rods: Moderator + Control + Boiler
		int[][] mods    = { {CX, CZ-2}, {CX, CZ+2}, {CX-2, CZ}, {CX+2, CZ} };
		int[][] ctrls   = { {CX-1, CZ-2}, {CX-1, CZ+2}, {CX-2, CZ-1}, {CX+2, CZ-1} };
		int[][] boilers = { {CX+1, CZ-2}, {CX+1, CZ+2}, {CX-2, CZ+1}, {CX+2, CZ+1} };
		for (int[] p : mods)    placeColumn(scene0, colId++, p[0], COL_Y, p[1], RbmkColType.MODERATOR, 1, 0.0, true);
		for (int[] p : ctrls)   placeColumn(scene0, colId++, p[0], COL_Y, p[1], RbmkColType.CONTROL,   0, 0.0, true);
		for (int[] p : boilers) placeColumn(scene0, colId++, p[0], COL_Y, p[1], RbmkColType.BOILER,    1, 0.0, true);
		scene0.add(new ActionWait(25));

		// 3) Outer moderators + perimeter reflectors
		int[][] outerMods = {
				{CX-3, CZ-2}, {CX+3, CZ-2}, {CX-3, CZ+2}, {CX+3, CZ+2},
				{CX-2, CZ-3}, {CX+2, CZ-3}, {CX-2, CZ+3}, {CX+2, CZ+3}
		};
		int[][] outerRefl = {
				{CX-4, CZ-2}, {CX+4, CZ-2}, {CX-4, CZ+2}, {CX+4, CZ+2},
				{CX-2, CZ-4}, {CX+2, CZ-4}, {CX-2, CZ+4}, {CX+2, CZ+4}
		};
		for (int[] p : outerMods) placeColumn(scene0, colId++, p[0], COL_Y, p[1], RbmkColType.MODERATOR, 1, 0.0, true);
		for (int[] p : outerRefl) placeColumn(scene0, colId++, p[0], COL_Y, p[1], RbmkColType.REFLECTOR, 1, 0.0, true);

		scene0.add(new ActionWait(55));
		scene0.add(new ActionRemoveActor(ID_PANEL_MAIN));

		// =====================================================================
		// SCENE 1: Bottom plumbing (Water y=3 + Steam Connector y=2 + Steam y=1)
		// =====================================================================
		WiajScene scene1 = new WiajScene(script);
		scene1.add(new ActionRotateBy(35, 12, 20));
		scene1.add(new ActionSetZoom(0.35, 20));

		var waterFluid = Fluids.WATER;
		var steamFluid = ModFluids.ULTRAHOTSTEAM.getSource();

		for (int[] b : boilers) {
			// y = 3: Water duct into boiler bottom
			placeDuct(scene1, b[0], 3, b[1], waterFluid, false, false, true, false, true, false);
			// y = 2: RBMK Steam Connector (2 blocks below boiler base)
			scene1.add(new ActionSetBlock(b[0], 2, b[1], steamConnectorBlock()));
			// y = 1: Steam duct from bottom of Steam Connector toward turbine hall
			placeDuct(scene1, b[0], 1, b[1], steamFluid, false, false, true, false, true, false);
		}

		placeDuctLineX(scene1, CX + 2, 11, 1, CZ + 1, steamFluid, true, true);
		placeDuct(scene1, 11, 1, CZ, steamFluid, true, true, false, false, false, false);
		placeDuct(scene1, 11, 1, CZ - 1, steamFluid, false, true, true, false, false, false);
		placeDuctLineX(scene1, CX + 2, 11, 3, CZ + 2, waterFluid, true, true);

		scene1.add(new ActionCreateActor(ID_HIGHLIGHT_BASE, new ActorWorldHighlight(CX + 1, 2, CZ + 1, CX + 3, 3, CZ + 3, 0xAAFAD64A).xray()));
		scene1.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, -78,
				new Object[][] { { tr("cannery.rbmk.starter.1.steam_connector") } }, 240)
				.setColors(COLOR_CYAN).setOrientation(Orientation.BOTTOM)));
		scene1.add(new ActionWait(95));

		scene1.add(new ActionRemoveActor(ID_HIGHLIGHT_BASE));
		scene1.add(new ActionRemoveActor(ID_PANEL_MAIN));
		scene1.add(new ActionRotateBy(-35, -12, 20));
		scene1.add(new ActionSetZoom(-0.35, 20));

		// =====================================================================
		// SCENE 2: Turbine cascade -> Cooling Tower -> Closed water loop
		// =====================================================================
		WiajScene scene2 = new WiajScene(script);
		scene2.add(new ActionOffsetBy(-8.0, 0.0, 0.0, 20));
		scene2.add(new ActionSetZoom(-0.15, 20));

		ActorRbmkMachine<BlockEntity> turbineRenderer = new ActorRbmkMachine<>(
				(p, f) -> new MachineIndustrialTurbineBlockEntity(p,
						ModBlocks.INDUSTRIAL_TURBINE.get().defaultBlockState().setValue(MachineIndustrialTurbineBlock.FACING, f)));
		ActorRbmkMachine<BlockEntity> towerRenderer = new ActorRbmkMachine<>(
				(p, f) -> new MachineCoolingTowerBlockEntity(p, ModBlocks.COOLING_TOWER.get().defaultBlockState()));

		// 4 turbines side-by-side along X (each 3 wide x 7 long, facing SOUTH: x=12..23, z=5..11)
		int[] turbineXs = { 13, 16, 19, 22 };
		for (int tx : turbineXs) {
			scene2.add(new ActionCreateActor(colId++,
					new ActorTileEntity(turbineRenderer, ActorRbmkMachine.posTag(tx, 1, 8, Direction.SOUTH)).smoothPlace()));
		}
		// Cooling Tower (9x9 footprint at x=25..33, z=4..12 — 1-block gap from turbine #4)
		scene2.add(new ActionCreateActor(colId++,
				new ActorTileEntity(towerRenderer, ActorRbmkMachine.posTag(29, 1, 8, Direction.NORTH)).smoothPlace()));

		// Spent steam duct: Turbine #4 side port (23, 1, 11) -> Cooling Tower port (25, 1, 11)
		placeDuct(scene2, 24, 1, 11, ModFluids.SPENTSTEAM.getSource(), false, false, true, true, false, false);

		// Water return line: Cooling Tower south port (29, 1, 12) -> z=13 -> reactor water manifold (8, 3, 10)
		placeDuct(scene2, 29, 1, 13, waterFluid, true, false, false, true, false, false);
		placeDuctLineX(scene2, 9, 28, 1, 13, waterFluid, true, true);
		placeDuct(scene2, 8, 1, 13, waterFluid, false, false, true, false, true, false);
		placeDuct(scene2, 8, 2, 13, waterFluid, false, false, false, false, true, true);
		placeDuct(scene2, 8, 3, 13, waterFluid, true, false, false, false, false, true);
		placeDuct(scene2, 8, 3, 12, waterFluid, true, true, false, false, false, false);
		placeDuct(scene2, 8, 3, 11, waterFluid, true, true, false, false, false, false);

		// Red power cables along z=4 at y=2, tapping into each turbine's rear terminal at (tx, 2, 5)
		for (int x = 13; x <= 22; x++) {
			boolean isTerminal = (x == 13 || x == 16 || x == 19 || x == 22);
			placeCable(scene2, x, 2, 4, false, isTerminal, x < 22, x > 13, false, false);
		}

		// Animated closed-loop flow markers: Steam -> Turbines -> Cooling Tower -> Water -> Reactor
		scene2.add(new ActionCreateActor(ID_FLUX_BASE, new ActorNeutronFlux(0.38, true,
				new double[][] {
						{ 8.5, 1.5, 9.5 }, { 11.5, 1.5, 7.5 }, { 22.5, 1.5, 11.5 },
						{ 29.5, 1.5, 13.5 }, { 8.5, 3.5, 13.5 }, { 8.5, 3.5, 10.5 }
				},
				new int[] { 0xEEFF80AB, 0xEEFFFFFF, 0xEEFFFFFF, 0xEE40C4FF, 0xEE2979FF })));

		scene2.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, -78,
				new Object[][] { { tr("cannery.rbmk.starter.2.turbine_loop") } }, 240)
				.setColors(COLOR_GREEN).setOrientation(Orientation.BOTTOM)));
		scene2.add(new ActionWait(95));

		scene2.add(new ActionRemoveActor(ID_FLUX_BASE));
		scene2.add(new ActionRemoveActor(ID_PANEL_MAIN));
		scene2.add(new ActionSetZoom(0.15, 20));
		scene2.add(new ActionOffsetBy(8.0, 0.0, 0.0, 20));

		// =====================================================================
		// SCENE 3: RBMK Console linking and Ra-226-Be kickstart
		// =====================================================================
		WiajScene scene3 = new WiajScene(script);

		ActorRbmkMachine<BlockEntity> consoleRenderer = new ActorRbmkMachine<>(
				(p, f) -> new MachineRbmkConsoleBlockEntity(p,
						ModBlocks.RBMK_CONSOLE.get().defaultBlockState().setValue(MachineRbmkConsoleBlock.FACING, f)));
		scene3.add(new ActionCreateActor(colId++,
				new ActorTileEntity(consoleRenderer, ActorRbmkMachine.posTag(CX, 1, 2, Direction.NORTH)).smoothPlace()));

		scene3.add(new ActionCreateActor(ID_GUI_OVERLAY, new ActorRbmkGuiPreview(Mode.CONSOLE_15X15, 100, -5)));
		scene3.add(new ActionUpdateActor(ID_GUI_OVERLAY, "rodLevel", 0));

		scene3.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, -95, -5,
				new Object[][] { { tr("cannery.rbmk.starter.3.console_link") } }, 175)
				.setColors(colorGold).setOrientation(Orientation.RIGHT)));
		scene3.add(new ActionWait(60));

		scene3.add(new ActionUpdateActor(ID_GUI_OVERLAY, "highlight", "az5"));
		scene3.add(new ActionWait(35));
		scene3.add(new ActionUpdateActor(ID_GUI_OVERLAY, "highlight", ""));

		ActorNeutronFlux startupHud = new ActorNeutronFlux(0.25, true,
				new double[][] { {CX - 1.5, 6.0, CZ - 1.5}, {CX + 2.5, 6.0, CZ - 1.5} },
				new int[] { 0xEE26C6DA }).withHud();
		scene3.add(new ActionCreateActor(ID_FLUX_BASE, startupHud));
		scene3.add(new ActionUpdateActor(ID_FLUX_BASE, "flux", 0));
		scene3.add(new ActionUpdateActor(ID_FLUX_BASE, "tempC", 20));
		scene3.add(new ActionUpdateActor(ID_FLUX_BASE, "statusNote", "cannery.rbmk.note.need_source"));

		scene3.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, -95, -5,
				new Object[][] { { tr("cannery.rbmk.starter.3.kickstart") } }, 175)
				.setColors(COLOR_GREEN).setOrientation(Orientation.RIGHT)));
		scene3.add(new ActionWait(35));

		// Withdraw control rods to 20% -> flux & temp climb
		scene3.add(new ActionUpdateActor(ID_GUI_OVERLAY, "rodLevel", 20));
		scene3.add(new ActionUpdateActor(ID_FLUX_BASE, "statusNote", "cannery.rbmk.note.running"));
		for (int step = 1; step <= 5; step++) {
			scene3.add(new ActionUpdateActor(ID_FLUX_BASE, "flux", step * 85));
			scene3.add(new ActionUpdateActor(ID_FLUX_BASE, "tempC", 150 + step * 140));
			scene3.add(new ActionWait(10));
		}

		scene3.add(new ActionWait(35));
		scene3.add(new ActionRemoveActor(ID_FLUX_BASE));
		scene3.add(new ActionRemoveActor(ID_GUI_OVERLAY));
		scene3.add(new ActionRemoveActor(ID_PANEL_MAIN));

		script.addScene(scene0).addScene(scene1).addScene(scene2).addScene(scene3);
		return script;
	}
}
