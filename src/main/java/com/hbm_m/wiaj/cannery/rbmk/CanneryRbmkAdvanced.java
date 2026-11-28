package com.hbm_m.wiaj.cannery.rbmk;

import static com.hbm_m.wiaj.cannery.rbmk.RbmkWiajHelpers.*;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;
import com.hbm_m.wiaj.WiajScript;
import com.hbm_m.wiaj.action.ActionCreateActor;
import com.hbm_m.wiaj.action.ActionRemoveActor;
import com.hbm_m.wiaj.action.ActionSetBlock;
import com.hbm_m.wiaj.action.ActionSetZoom;
import com.hbm_m.wiaj.action.ActionWait;
import com.hbm_m.wiaj.actor.ActorFancyPanel;
import com.hbm_m.wiaj.actor.ActorFancyPanel.Orientation;
import com.hbm_m.wiaj.cannery.CanneryBase;
import com.hbm_m.wiaj.cannery.CannerySILEX;
import com.hbm_m.wiaj.cannery.rbmk.RbmkWiajActors.ActorRbmkGuiPreview;
import com.hbm_m.wiaj.cannery.rbmk.RbmkWiajActors.ActorRbmkGuiPreview.Mode;
import com.hbm_m.wiaj.cannery.rbmk.RbmkWiajActors.ActorWorldHighlight;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Advanced RBMK and Fuel Cycle (3 scenes):
 * 0) 16-rod safe MEP-239 reactor (11x11 diamond, ~30 MHE/s, ~1000°C)
 * 1) Spent Fuel Pool Drum (water on all 4 sides) + SILEX pellet reprocessing
 * 2) Engineer pre-startup checklist
 */
public class CanneryRbmkAdvanced extends CanneryBase {

	private static final int CX = 11;
	private static final int CZ = 10;

	private int nextColId;

	@Override
	public ItemStack getIcon() {
		return new ItemStack(spentFuelDrumBlock());
	}

	@Override
	public String getName() {
		return "cannery.rbmk_advanced";
	}

	@Override
	public CanneryBase[] seeAlso() {
		return new CanneryBase[] {
				new CanneryRbmkIntro(),
				new CanneryRbmkBasics(),
				new CanneryRbmkStarter(),
				new CannerySILEX()
		};
	}

	@Override
	public WiajScript createScript() {
		VirtualWorld world = new VirtualWorld(23, 9, 21);
		WiajScript script = new WiajScript(world);
		var font = Minecraft.getInstance().font;

		// =====================================================================
		// SCENE 0: 16-rod MEP-239 11x11 diamond (layered build)
		// =====================================================================
		WiajScene scene0 = new WiajScene(script);
		scene0.add(new ActionSetZoom(0.85, 0));
		buildIndustrialFloor(scene0, 23, 21);

		nextColId = ID_COLUMN_BASE;

		// 1) Center moderator + 4 inner fuel rods
		placeColumn(scene0, nextColId++, CX, 1, CZ, RbmkColType.MODERATOR, 1, 0.0, true);
		placeSymCardinal(scene0, 1, RbmkColType.FUEL, 2);
		scene0.add(new ActionWait(10));

		// 2) 4 corner moderators + 8 mid fuel rods (=12)
		placeSymDiag(scene0, 1, 1, RbmkColType.MODERATOR, 1);
		placeSymDiag(scene0, 2, 1, RbmkColType.FUEL, 2);
		placeSymDiag(scene0, 1, 2, RbmkColType.FUEL, 2);
		scene0.add(new ActionWait(10));

		// 3) 4 outer moderators + 4 outer fuel rods (=16 MEP-239 rods)
		placeSymCardinal(scene0, 2, RbmkColType.MODERATOR, 1);
		placeSymCardinal(scene0, 3, RbmkColType.FUEL, 2);
		scene0.add(new ActionWait(12));

		// 4) 12 steam boilers along diagonal edges
		placeSymDiag(scene0, 2, 2, RbmkColType.BOILER, 1);
		placeSymDiag(scene0, 3, 1, RbmkColType.BOILER, 1);
		placeSymDiag(scene0, 1, 3, RbmkColType.BOILER, 1);
		scene0.add(new ActionWait(12));

		// 5) Outer moderator ring + closed reflector perimeter
		placeSymCardinal(scene0, 4, RbmkColType.MODERATOR, 1);
		placeSymDiag(scene0, 3, 2, RbmkColType.MODERATOR, 1);
		placeSymDiag(scene0, 2, 3, RbmkColType.MODERATOR, 1);

		placeSymCardinal(scene0, 5, RbmkColType.REFLECTOR, 1);
		placeSymDiag(scene0, 4, 1, RbmkColType.REFLECTOR, 1);
		placeSymDiag(scene0, 1, 4, RbmkColType.REFLECTOR, 1);
		placeSymDiag(scene0, 4, 2, RbmkColType.REFLECTOR, 1);
		placeSymDiag(scene0, 2, 4, RbmkColType.REFLECTOR, 1);
		placeSymDiag(scene0, 3, 3, RbmkColType.REFLECTOR, 1);

		scene0.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, -78,
				new Object[][] { { tr("cannery.rbmk.adv.0.mep16") } }, 240)
				.setColors(COLOR_GREEN).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionWait(110));
		scene0.add(new ActionRemoveActor(ID_PANEL_MAIN));

		// =====================================================================
		// SCENE 1: Spent Fuel Pool Drum + SILEX reprocessing
		// =====================================================================
		WiajScene scene1 = new WiajScene(script);
		for (int id = ID_COLUMN_BASE; id < nextColId; id++) {
			scene1.add(new ActionRemoveActor(id));
		}
		clearBox(scene1, 1, 1, 1, 21, 7, 19);
		scene1.add(new ActionSetZoom(0.65, 20));

		// 5x5 concrete basin with water on all 4 sides around Spent Fuel Pool Drum
		fillBox(scene1, 9, 1, 8, 13, 2, 12, ModBlocks.CONCRETE_SMOOTH.get());
		clearBox(scene1, 10, 1, 9, 12, 2, 11);
		fillBox(scene1, 10, 1, 9, 12, 1, 11, Blocks.WATER);
		scene1.add(new ActionSetBlock(11, 1, 10, spentFuelDrumBlock()));

		scene1.add(new ActionCreateActor(ID_HIGHLIGHT_BASE, new ActorWorldHighlight(10, 1, 9, 13, 2, 12, 0x8826C6DA)));
		scene1.add(new ActionCreateActor(ID_GUI_OVERLAY, new ActorRbmkGuiPreview(Mode.FUEL_LIFECYCLE, 102, -5)));
		scene1.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, -95, -5,
				new Object[][] { { tr("cannery.rbmk.adv.1.spent_pool") } }, 175)
				.setColors(COLOR_CYAN).setOrientation(Orientation.RIGHT)));
		scene1.add(new ActionWait(105));

		scene1.add(new ActionRemoveActor(ID_HIGHLIGHT_BASE));
		scene1.add(new ActionRemoveActor(ID_GUI_OVERLAY));
		scene1.add(new ActionRemoveActor(ID_PANEL_MAIN));

		// =====================================================================
		// SCENE 2: Engineer pre-startup checklist
		// =====================================================================
		WiajScene scene2 = new WiajScene(script);
		scene2.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, 0,
				new Object[][] { { tr("cannery.rbmk.adv.2.checklist") } }, 245)
				.setColors(colorGold).setOrientation(Orientation.CENTER)));
		scene2.add(new ActionWait(110));
		scene2.add(new ActionRemoveActor(ID_PANEL_MAIN));

		script.addScene(scene0).addScene(scene1).addScene(scene2);
		return script;
	}

	private void placeSymCardinal(WiajScene scene, int d, RbmkColType type, int lidState) {
		placeColumn(scene, nextColId++, CX + d, 1, CZ, type, lidState, 0.0, true);
		placeColumn(scene, nextColId++, CX - d, 1, CZ, type, lidState, 0.0, true);
		placeColumn(scene, nextColId++, CX, 1, CZ + d, type, lidState, 0.0, true);
		placeColumn(scene, nextColId++, CX, 1, CZ - d, type, lidState, 0.0, true);
	}

	private void placeSymDiag(WiajScene scene, int dx, int dz, RbmkColType type, int lidState) {
		placeColumn(scene, nextColId++, CX + dx, 1, CZ + dz, type, lidState, 0.0, true);
		placeColumn(scene, nextColId++, CX - dx, 1, CZ + dz, type, lidState, 0.0, true);
		placeColumn(scene, nextColId++, CX + dx, 1, CZ - dz, type, lidState, 0.0, true);
		placeColumn(scene, nextColId++, CX - dx, 1, CZ - dz, type, lidState, 0.0, true);
	}
}
