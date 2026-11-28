package com.hbm_m.wiaj.cannery.rbmk;

import static com.hbm_m.wiaj.cannery.rbmk.RbmkWiajHelpers.*;

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
import com.hbm_m.wiaj.cannery.CanneryBase;
import com.hbm_m.wiaj.cannery.rbmk.RbmkWiajActors.ActorMeltdownEffect;
import com.hbm_m.wiaj.cannery.rbmk.RbmkWiajActors.ActorNeutronFlux;
import com.hbm_m.wiaj.cannery.rbmk.RbmkWiajActors.ActorRbmkGuiPreview;
import com.hbm_m.wiaj.cannery.rbmk.RbmkWiajActors.ActorRbmkGuiPreview.Mode;
import com.hbm_m.wiaj.cannery.rbmk.RbmkWiajActors.ActorWorldHighlight;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * RBMK Basics (4 scenes):
 * 0) Fuel column and 4 cardinal neutron directions (5-block range)
 * 1) Moderator (fast -> slow), Reflector (bounce), Absorber, and Outgasser (irradiation)
 * 2) Control rods (manual + auto with Save button) and RBMK lids
 * 3) Fuel reactivity curves and Meltdown (>1500°C / skin overheat)
 */
public class CanneryRbmkBasics extends CanneryBase {

	private static final int FAST_COL = 0xEEFF8F00; // Orange: fast neutrons
	private static final int SLOW_COL = 0xEE26C6DA; // Cyan: slow (thermal) neutrons

	@Override
	public ItemStack getIcon() {
		return new ItemStack(blockForColumn(RbmkColType.FUEL));
	}

	@Override
	public String getName() {
		return "cannery.rbmk_basics";
	}

	@Override
	public CanneryBase[] seeAlso() {
		return new CanneryBase[] {
				new CanneryRbmkIntro(),
				new CanneryRbmkStarter(),
				new CanneryRbmkAdvanced()
		};
	}

	@Override
	public WiajScript createScript() {
		VirtualWorld world = new VirtualWorld(15, 9, 15);
		WiajScript script = new WiajScript(world);
		var font = Minecraft.getInstance().font;

		// =====================================================================
		// SCENE 0: Fuel column and 4 neutron beams (5-block range)
		// =====================================================================
		WiajScene scene0 = new WiajScene(script);
		scene0.add(new ActionSetZoom(1.25, 0));
		buildIndustrialFloor(scene0, 15, 15);

		// 5-block cross markers from center (7, 0, 7)
		for (int d = 1; d <= 5; d++) {
			var marker = (d % 2 == 0) ? Blocks.WHITE_CONCRETE : Blocks.BLACK_CONCRETE;
			scene0.add(new ActionSetBlock(7 + d, 0, 7, marker));
			scene0.add(new ActionSetBlock(7 - d, 0, 7, marker));
			scene0.add(new ActionSetBlock(7, 0, 7 + d, marker));
			scene0.add(new ActionSetBlock(7, 0, 7 - d, marker));
		}
		scene0.add(new ActionWait(6));

		placeColumn(scene0, ID_COLUMN_BASE, 7, 1, 7, RbmkColType.FUEL, 0, 0.0, true);

		scene0.add(new ActionCreateActor(ID_FLUX_BASE,     new ActorNeutronFlux(0.28, true, new double[][] {{7.5, 3.0, 7.5}, {12.5, 3.0, 7.5}}, new int[] { FAST_COL })));
		scene0.add(new ActionCreateActor(ID_FLUX_BASE + 1, new ActorNeutronFlux(0.28, true, new double[][] {{7.5, 3.0, 7.5}, { 2.5, 3.0, 7.5}}, new int[] { FAST_COL })));
		scene0.add(new ActionCreateActor(ID_FLUX_BASE + 2, new ActorNeutronFlux(0.28, true, new double[][] {{7.5, 3.0, 7.5}, { 7.5, 3.0, 12.5}}, new int[] { FAST_COL })));
		scene0.add(new ActionCreateActor(ID_FLUX_BASE + 3, new ActorNeutronFlux(0.28, true, new double[][] {{7.5, 3.0, 7.5}, { 7.5, 3.0,  2.5}}, new int[] { FAST_COL })));

		scene0.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, -76,
				new Object[][] { { tr("cannery.rbmk.basics.0.neutrons") } }, 235)
				.setColors(colorBlue).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionWait(95));

		for (int i = 0; i < 4; i++) scene0.add(new ActionRemoveActor(ID_FLUX_BASE + i));
		scene0.add(new ActionRemoveActor(ID_PANEL_MAIN));

		// =====================================================================
		// SCENE 1: Moderator + Reflector + Absorber + Outgasser
		// =====================================================================
		WiajScene scene1 = new WiajScene(script);

		placeColumn(scene1, ID_COLUMN_BASE + 1,  9, 1, 7, RbmkColType.MODERATOR, 0, 0.0, true);
		placeColumn(scene1, ID_COLUMN_BASE + 2, 11, 1, 7, RbmkColType.REFLECTOR, 0, 0.0, true);
		placeColumn(scene1, ID_COLUMN_BASE + 3,  5, 1, 7, RbmkColType.ABSORBER,  0, 0.0, true);
		placeColumn(scene1, ID_COLUMN_BASE + 4,  7, 1, 5, RbmkColType.OUTGASSER, 0, 0.0, true);

		// +X beam: Fast -> (Moderator) -> Slow -> (Reflector) -> Bounce back to fuel
		ActorNeutronFlux bounceFlux = new ActorNeutronFlux(0.25, true,
				new double[][] { {7.5, 3.0, 7.5}, {9.5, 3.0, 7.5}, {11.3, 3.0, 7.5}, {7.5, 3.0, 7.5} },
				new int[] { FAST_COL, SLOW_COL, SLOW_COL }).withHud();
		scene1.add(new ActionCreateActor(ID_FLUX_BASE, bounceFlux));
		scene1.add(new ActionUpdateActor(ID_FLUX_BASE, "flux", 120));
		scene1.add(new ActionUpdateActor(ID_FLUX_BASE, "tempC", 420));
		scene1.add(new ActionUpdateActor(ID_FLUX_BASE, "statusNote", "cannery.rbmk.note.mod_refl"));

		// -X beam absorbed
		scene1.add(new ActionCreateActor(ID_FLUX_BASE + 1, new ActorNeutronFlux(0.25, true,
				new double[][] { {7.5, 3.0, 7.5}, {5.7, 3.0, 7.5} }, new int[] { FAST_COL })));

		scene1.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, 72,
				new Object[][] { { tr("cannery.rbmk.basics.1.mod_refl_abs") } }, 240)
				.setColors(COLOR_CYAN).setOrientation(Orientation.TOP)));
		scene1.add(new ActionWait(105));

		scene1.add(new ActionRemoveActor(ID_FLUX_BASE));
		scene1.add(new ActionRemoveActor(ID_FLUX_BASE + 1));
		scene1.add(new ActionRemoveActor(ID_PANEL_MAIN));

		// =====================================================================
		// SCENE 2: Control rods (manual & auto) + RBMK lids
		// =====================================================================
		WiajScene scene2 = new WiajScene(script);
		for (int i = 0; i <= 4; i++) scene2.add(new ActionRemoveActor(ID_COLUMN_BASE + i));

		placeColumn(scene2, ID_COLUMN_BASE,     5, 1, 7, RbmkColType.FUEL,    0, 0.0, true);
		placeColumn(scene2, ID_COLUMN_BASE + 1, 7, 1, 7, RbmkColType.CONTROL, 0, 0.0, true);
		placeColumn(scene2, ID_COLUMN_BASE + 2, 9, 1, 7, RbmkColType.FUEL,    0, 0.0, true);

		// 2A) Manual control rod: 0% -> 100%
		scene2.add(new ActionCreateActor(ID_GUI_OVERLAY, new ActorRbmkGuiPreview(Mode.CONTROL_ROD, 102, -5)));
		scene2.add(new ActionUpdateActor(ID_GUI_OVERLAY, "rodLevel", 0));
		scene2.add(new ActionUpdateActor(ID_GUI_OVERLAY, "highlight", "level"));
		scene2.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, -90, -5,
				new Object[][] { { tr("cannery.rbmk.basics.2.control_manual") } }, 175)
				.setColors(colorGold).setOrientation(Orientation.RIGHT)));
		scene2.add(new ActionWait(45));

		// Withdraw rod to 100% — both 3D cap and 2D GUI rise, flux passes between fuel channels
		scene2.add(new ActionUpdateActor(ID_COLUMN_BASE + 1, "rodLevel", 1.0));
		scene2.add(new ActionUpdateActor(ID_GUI_OVERLAY, "rodLevel", 100));
		scene2.add(new ActionCreateActor(ID_FLUX_BASE, new ActorNeutronFlux(0.28, true,
				new double[][] { {5.5, 3.0, 7.5}, {9.5, 3.0, 7.5} }, new int[] { SLOW_COL })));
		scene2.add(new ActionWait(45));

		// 2B) Auto control rod (highlight Save button)
		scene2.add(new ActionCreateActor(ID_GUI_OVERLAY, new ActorRbmkGuiPreview(Mode.AUTO_CONTROL, 102, -5)));
		scene2.add(new ActionUpdateActor(ID_GUI_OVERLAY, "highlight", "save"));
		scene2.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, -90, -5,
				new Object[][] { { tr("cannery.rbmk.basics.2.control_auto") } }, 175)
				.setColors(colorBlue).setOrientation(Orientation.RIGHT)));
		scene2.add(new ActionWait(70));
		scene2.add(new ActionRemoveActor(ID_GUI_OVERLAY));

		// 2C) Lids on top (2 = glass lid on fuel, 1 = concrete lid)
		scene2.add(new ActionUpdateActor(ID_COLUMN_BASE,     "lidState", 2));
		scene2.add(new ActionUpdateActor(ID_COLUMN_BASE + 2, "lidState", 2));
		scene2.add(new ActionCreateActor(ID_HIGHLIGHT_BASE, new ActorWorldHighlight(5, 5, 7, 10, 5.4, 8, 0xAA4CD964)));
		scene2.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, -76,
				new Object[][] { { tr("cannery.rbmk.basics.2.covers") } }, 235)
				.setColors(COLOR_GREEN).setOrientation(Orientation.BOTTOM)));
		scene2.add(new ActionWait(75));

		scene2.add(new ActionRemoveActor(ID_FLUX_BASE));
		scene2.add(new ActionRemoveActor(ID_HIGHLIGHT_BASE));
		scene2.add(new ActionRemoveActor(ID_PANEL_MAIN));

		// =====================================================================
		// SCENE 3: Fuel reactivity curves and Meltdown (>1500°C)
		// =====================================================================
		WiajScene scene3 = new WiajScene(script);

		scene3.add(new ActionCreateActor(ID_GUI_OVERLAY, new ActorRbmkGuiPreview(Mode.FLUX_FUNCTIONS, 98, -5)));
		scene3.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, -92, -5,
				new Object[][] { { tr("cannery.rbmk.basics.3.functions") } }, 175)
				.setColors(COLOR_PURPLE).setOrientation(Orientation.RIGHT)));
		scene3.add(new ActionWait(95));
		scene3.add(new ActionRemoveActor(ID_GUI_OVERLAY));

		ActorNeutronFlux overheatHud = new ActorNeutronFlux(0.0, false,
				new double[][] { {7.5, 3.0, 7.5}, {7.5, 3.0, 7.5} }, new int[] { 0x00000000 }).withHud();
		scene3.add(new ActionCreateActor(ID_FLUX_BASE, overheatHud));
		scene3.add(new ActionCreateActor(ID_MELTDOWN_FX, new ActorMeltdownEffect(7.5, 3.0, 7.5)));
		scene3.add(new ActionCreateActor(ID_HIGHLIGHT_BASE, new ActorWorldHighlight(5, 1, 7, 10, 5, 8, 0xAAE53935)));

		for (int t = 900; t <= 1550; t += 130) {
			scene3.add(new ActionUpdateActor(ID_FLUX_BASE, "flux", 500 + t));
			scene3.add(new ActionUpdateActor(ID_FLUX_BASE, "tempC", t));
			scene3.add(new ActionWait(6));
		}

		// Meltdown: replace columns with smoldering debris
		scene3.add(new ActionUpdateActor(ID_MELTDOWN_FX, "active", true));
		scene3.add(new ActionUpdateActor(ID_FLUX_BASE, "statusNote", "cannery.rbmk.note.meltdown"));
		for (int i = 0; i <= 2; i++) scene3.add(new ActionRemoveActor(ID_COLUMN_BASE + i));
		placeColumnBlocks(scene3, 5, 1, 7, 2, RbmkColType.DEBRIS_SMOLDERING);
		placeColumnBlocks(scene3, 7, 1, 7, 1, RbmkColType.DEBRIS_SMOLDERING);
		placeColumnBlocks(scene3, 9, 1, 7, 2, RbmkColType.DEBRIS_SMOLDERING);

		scene3.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, 72,
				new Object[][] { { tr("cannery.rbmk.basics.3.meltdown") } }, 240)
				.setColors(COLOR_RED).setOrientation(Orientation.TOP)));
		scene3.add(new ActionWait(95));

		scene3.add(new ActionRemoveActor(ID_FLUX_BASE));
		scene3.add(new ActionRemoveActor(ID_MELTDOWN_FX));
		scene3.add(new ActionRemoveActor(ID_HIGHLIGHT_BASE));
		scene3.add(new ActionRemoveActor(ID_PANEL_MAIN));

		script.addScene(scene0).addScene(scene1).addScene(scene2).addScene(scene3);
		return script;
	}
}
