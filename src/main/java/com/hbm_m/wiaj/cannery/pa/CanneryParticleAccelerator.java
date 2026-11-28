package com.hbm_m.wiaj.cannery.pa;

import static com.hbm_m.wiaj.cannery.pa.PaWiajHelpers.*;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.block.machines.MachineMixerBlock;
import com.hbm_m.block.machines.albion.PAMultiblockBlock;
import com.hbm_m.blockentity.machines.MachineCompressorBlockEntity;
import com.hbm_m.blockentity.machines.MachineMixerBlockEntity;
import com.hbm_m.blockentity.machines.albion.*;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;
import com.hbm_m.wiaj.WiajScript;
import com.hbm_m.wiaj.action.*;
import com.hbm_m.wiaj.actor.ActorFancyPanel;
import com.hbm_m.wiaj.actor.ActorFancyPanel.Orientation;
import com.hbm_m.wiaj.actor.ActorTileEntity;
import com.hbm_m.wiaj.cannery.CanneryBase;
import com.hbm_m.wiaj.cannery.pa.PaWiajActors.*;
import com.hbm_m.wiaj.cannery.pa.PaWiajActors.ActorGuiPreview.Mode;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Main Particle Accelerator demo:
 * - Scene Intro: Showcase each of the 6 PA machines one by one
 * - Scene 0: Geometry, Y+1 beam axis, and 9x5x1 pit for Detector (with 90 deg rotation)
 * - Scene 1: Linear run (Momentum +100, Defocus +100 / -100)
 * - Scene 2: Industrial Mixer -> 2 Compressors behind the line + non-overlapping y=7 power cables
 * - Scene 3: Source & Detector GUI walkthrough
 */
public class CanneryParticleAccelerator extends CanneryBase {

	@Override
	public ItemStack getIcon() {
		return new ItemStack(ModBlocks.PA_SOURCE.get());
	}

	@Override
	public String getName() {
		return "cannery.pa_basics";
	}

	@Override
	public CanneryBase[] seeAlso() {
		return new CanneryBase[] {
				new CanneryParticleCoils(),
				new CannerySynchrotron(),
				new CannerySynchrotronCascade()
		};
	}

	static ActorNucleusMachine<BlockEntity> paSource() {
		return new ActorNucleusMachine<>((p, f) -> new PASourceBlockEntity(p, ModBlocks.PA_SOURCE.get().defaultBlockState().setValue(PAMultiblockBlock.FACING, f)));
	}
	static ActorNucleusMachine<BlockEntity> paBeamline() {
		return new ActorNucleusMachine<>((p, f) -> new PABeamlineBlockEntity(p, ModBlocks.BEAMLINE.get().defaultBlockState().setValue(PAMultiblockBlock.FACING, f)));
	}
	static ActorNucleusMachine<BlockEntity> paRfc() {
		return new ActorNucleusMachine<>((p, f) -> new PARFCBlockEntity(p, ModBlocks.RFC.get().defaultBlockState().setValue(PAMultiblockBlock.FACING, f)));
	}
	static ActorNucleusMachine<BlockEntity> paQuadrupole() {
		return new ActorNucleusMachine<>((p, f) -> new PAQuadrupoleBlockEntity(p, ModBlocks.QUADRUPOLE.get().defaultBlockState().setValue(PAMultiblockBlock.FACING, f)));
	}
	static ActorNucleusMachine<BlockEntity> paDetector() {
		return new ActorNucleusMachine<>((p, f) -> new PADetectorBlockEntity(p, ModBlocks.PA_DETECTOR.get().defaultBlockState().setValue(PAMultiblockBlock.FACING, f)));
	}
	static ActorNucleusMachine<BlockEntity> paDipole() {
		return new ActorNucleusMachine<>((p, f) -> new PADipoleBlockEntity(p, ModBlocks.DIPOLE.get().defaultBlockState().setValue(PAMultiblockBlock.FACING, f)));
	}
	static ActorNucleusMachine<BlockEntity> compressor() {
		return new ActorNucleusMachine<>((p, f) -> new MachineCompressorBlockEntity(p, ModBlocks.COMPRESSOR.get().defaultBlockState().setValue(DummyableMachineBlock.FACING, f)));
	}
	static ActorNucleusMachine<BlockEntity> mixer() {
		return new ActorNucleusMachine<>((p, f) -> new MachineMixerBlockEntity(p, ModBlocks.MIXER.get().defaultBlockState().setValue(MachineMixerBlock.FACING, f)));
	}

	@Override
	public WiajScript createScript() {
		VirtualWorld world = new VirtualWorld(38, 9, 13);
		WiajScript script = new WiajScript(world);
		var font = Minecraft.getInstance().font;

		// =====================================================================
		// SCENE INTRO (0..5): Present each of the 6 PA components as separate scenes
		// so Back / Next buttons step between individual machines
		// =====================================================================
		Object[][] showcase = {
				{ paSource(),     "cannery.pa.intro.source" },
				{ paBeamline(),   "cannery.pa.intro.beamline" },
				{ paRfc(),        "cannery.pa.intro.rfc" },
				{ paQuadrupole(), "cannery.pa.intro.quadrupole" },
				{ paDipole(),     "cannery.pa.intro.dipole" },
				{ paDetector(),   "cannery.pa.intro.detector" }
		};
		java.util.List<WiajScene> introScenes = new java.util.ArrayList<>(showcase.length);
		for (int i = 0; i < showcase.length; i++) {
			WiajScene s = new WiajScene(script);
			if (i == 0) {
				s.add(new ActionSetZoom(1.05, 0));
				s.add(new ActionOffsetBy(0, 1.0, 0, 0));
				fillBox(s, 0, 0, 0, 37, 0, 12, ModBlocks.CONCRETE_SMOOTH.get());
				fillBox(s, 0, 1, 0, 37, 1, 12, ModBlocks.CONCRETE_SMOOTH.get());
				s.add(new ActionWait(6));
			}
			@SuppressWarnings("unchecked")
			ActorNucleusMachine<BlockEntity> renderer = (ActorNucleusMachine<BlockEntity>) showcase[i][0];
			String key = (String) showcase[i][1];
			int showcaseY = (i == showcase.length - 1) ? 4 : 3; // Detector is 5 tall (offset -2), so y=4 sits flush on y=1 floor
			s.add(new ActionCreateActor(0, new ActorTileEntity(renderer, ActorNucleusMachine.posTagForBeam(18, showcaseY, 6, Direction.WEST)).smoothPlace()));
			if (i == 1) {
				s.add(new ActionCreateActor(ID_HIGHLIGHT_BASE, new ActorWorldHighlight(17, 2, 6, 20, 3, 7, 0xAAFFD600)));
			}
			s.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, -75,
					new Object[][] { { tr(key) } }, 220)
					.setColors(colorBlue).setOrientation(Orientation.BOTTOM)));
			s.add(new ActionWait(55));
			if (i == 1) {
				s.add(new ActionRemoveActor(ID_HIGHLIGHT_BASE));
			}
			s.add(new ActionRemoveActor(0));
			if (i == showcase.length - 1) {
				s.add(new ActionRemoveActor(ID_PANEL_MAIN));
			}
			introScenes.add(s);
		}

		// =====================================================================
		// SCENE 0: Geometry, Y+1 beam axis, and 9x5x1 Detector pit
		// Source at x=33 (29..37) -> flush with Beamline #1 at x=27 (26..28)
		// =====================================================================
		WiajScene scene0 = new WiajScene(script);
		scene0.add(new ActionSetZoom(-0.10, 15));

		scene0.add(new ActionCreateActor(0, new ActorTileEntity(paSource(), ActorNucleusMachine.posTagForBeam(33, 3, 5, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, -70,
				new Object[][] { { tr("cannery.pa.0.intro") } }, 210)
				.setColors(colorBlue).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionWait(70));

		scene0.add(new ActionCreateActor(1, new ActorTileEntity(paBeamline(), ActorNucleusMachine.posTagForBeam(27, 3, 5, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(ID_HIGHLIGHT_BASE, new ActorWorldHighlight(26, 2, 5, 29, 3, 6, 0xAAFFD600)));
		scene0.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, -20, -65,
				new Object[][] { { tr("cannery.pa.0.beamline_height") } }, 215)
				.setColors(colorGold).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionWait(80));
		scene0.add(new ActionRemoveActor(ID_HIGHLIGHT_BASE));

		scene0.add(new ActionCreateActor(2, new ActorTileEntity(paRfc(), ActorNucleusMachine.posTagForBeam(21, 3, 5, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionWait(12));
		scene0.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, -70,
				new Object[][] { { tr("cannery.pa.0.rfc") } }, 220)
				.setColors(colorBlue).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionWait(75));

		scene0.add(new ActionCreateActor(3, new ActorTileEntity(paQuadrupole(), ActorNucleusMachine.posTagForBeam(15, 3, 5, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(4, new ActorTileEntity(paBeamline(), ActorNucleusMachine.posTagForBeam(12, 3, 5, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 15, -70,
				new Object[][] { { tr("cannery.pa.0.quadrupole") } }, 220)
				.setColors(COLOR_GREEN).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionWait(80));

		// Rotate 90 deg to show Detector height mismatch clearly
		scene0.add(new ActionRotateBy(90, 0, 20));
		scene0.add(new ActionOffsetBy(7.0, 0.0, 0.0, 20));
		scene0.add(new ActionSetZoom(0.45, 20));
		scene0.add(new ActionCreateActor(5, new ActorTileEntity(paDetector(), ActorNucleusMachine.posTagForBeam(6, 4, 5, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(ID_HIGHLIGHT_BASE, new ActorWorldHighlight(10.5, 3, 4.5, 11.5, 5, 6.5, 0xCCE53935)));
		scene0.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, -70,
				new Object[][] { { tr("cannery.pa.0.detector_mismatch") } }, 220)
				.setColors(COLOR_RED).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionWait(75));

		scene0.add(new ActionRemoveActor(5));
		scene0.add(new ActionRemoveActor(ID_HIGHLIGHT_BASE));
		digDetectorPit(scene0, 2, 10, 3, 7);
		scene0.add(new ActionCreateActor(ID_HIGHLIGHT_BASE, new ActorWorldHighlight(2, 1, 3, 11, 2, 8, 0xAA4CD964)));
		scene0.add(new ActionWait(12));
		scene0.add(new ActionCreateActor(5, new ActorTileEntity(paDetector(), ActorNucleusMachine.posTagForBeam(6, 3, 5, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, -70,
				new Object[][] { { tr("cannery.pa.0.detector_pit") } }, 220)
				.setColors(COLOR_GREEN).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionWait(80));
		scene0.add(new ActionRemoveActor(ID_HIGHLIGHT_BASE));
		scene0.add(new ActionRemoveActor(ID_PANEL_MAIN));
		scene0.add(new ActionRotateBy(-90, 0, 20));
		scene0.add(new ActionOffsetBy(-7.0, 0.0, 0.0, 20));
		scene0.add(new ActionSetZoom(-0.45, 20));

		// =====================================================================
		// SCENE 1: Live Linear Run (Momentum +100, Defocus +100 / -100)
		// =====================================================================
		WiajScene scene1 = new WiajScene(script);

		scene1.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, 75,
				new Object[][] { { tr("cannery.pa.1.linear_run") } }, 230)
				.setColors(colorBlue).setOrientation(Orientation.TOP)));

		ActorParticleBeam beam = new ActorParticleBeam(0.35, false,
				new double[] { 29.0, 3.5, 5.5 },
				new double[] { 21.0, 3.5, 5.5 },
				new double[] { 15.0, 3.5, 5.5 },
				new double[] { 10.5, 3.5, 5.5 }
		);
		scene1.add(new ActionCreateActor(ID_PARTICLE, beam));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "statusNote", "cannery.pa.note.src_to_rfc"));

		scene1.add(new ActionWait(23));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "momentum", 100));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "defocus", 100));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "statusNote", "cannery.pa.note.rfc_gain"));

		scene1.add(new ActionWait(17));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "defocus", 0));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "statusNote", "cannery.pa.note.quad_focus"));

		scene1.add(new ActionWait(14));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "statusNote", "cannery.pa.note.det_success"));
		scene1.add(new ActionWait(45));

		scene1.add(new ActionRemoveActor(ID_PARTICLE));
		scene1.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, 75,
				new Object[][] { { tr("cannery.pa.1.linear_limit") } }, 230)
				.setColors(colorGold).setOrientation(Orientation.TOP)));
		scene1.add(new ActionWait(85));
		scene1.add(new ActionRemoveActor(ID_PANEL_MAIN));

		// =====================================================================
		// SCENE 2: Industrial Mixer -> 2 Compressors (behind line at z=9) + y=7 Power Cables
		// =====================================================================
		WiajScene scene2 = new WiajScene(script);

		// Rotate camera slightly so Mixer & Compressors behind the line are clearly visible
		scene2.add(new ActionRotateBy(-55, 0, 20));
		scene2.add(new ActionCreateActor(6, new ActorTileEntity(mixer(), ActorNucleusMachine.posTag(26, 2, 9, Direction.NORTH)).smoothPlace()));
		scene2.add(new ActionCreateActor(7, new ActorTileEntity(compressor(), ActorNucleusMachine.posTag(22, 2, 9, Direction.NORTH)).smoothPlace()));
		scene2.add(new ActionCreateActor(8, new ActorTileEntity(compressor(), ActorNucleusMachine.posTag(18, 2, 9, Direction.NORTH)).smoothPlace()));

		scene2.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, -40, 75,
				new Object[][] { { tr("cannery.pa.2.coolant_chain") } }, 230)
				.setColors(COLOR_CYAN).setOrientation(Orientation.TOP)));
		scene2.add(new ActionWait(90));

		var coldFluid = ModFluids.PERFLUOROMETHYL_COLD.getSource();
		var warmFluid = ModFluids.PERFLUOROMETHYL.getSource();

		// Fluid trunks at y = 6 (z=4 cold, z=6 warm)
		for (int x = 1; x <= 33; x++) {
			boolean w = x > 1, e = x < 33;
			boolean coldS = (x == 18) || (x == 18);
			boolean coldD = (x == 33 || x == 15 || x == 1);
			placeDuct(scene2, x, 6, 4, coldFluid, false, coldS, e, w, false, coldD);

			boolean warmS = (x == 22);
			boolean warmN = (x == 24);
			boolean warmD = (x == 33 || x == 15 || x == 1);
			placeDuct(scene2, x, 6, 6, warmFluid, warmN, warmS, e, w, false, warmD);
		}

		// Power Cable trunk on dedicated y = 7, z = 5 (never crosses fluid pipes!)
		for (int x = 1; x <= 33; x++) {
			boolean w = x > 1, e = x < 33;
			boolean cabD = (x == 33 || x == 21 || x == 15 || x == 1);
			placeCable(scene2, x, 7, 5, false, false, e, w, false, cabD);
		}

		// Source (x=33) and Quadrupole (x=15) drops
		for (int px : new int[] { 33, 15 }) {
			placeDuct(scene2, px, 5, 4, coldFluid, false, false, false, false, true, true);
			placeCable(scene2, px, 6, 5, false, false, false, false, true, true);
			placeCable(scene2, px, 5, 5, false, false, false, false, true, true);
			placeDuct(scene2, px, 5, 6, warmFluid, false, false, false, false, true, true);
			placeDummyConnector(scene2, px, 4, 4);
			placeDummyConnector(scene2, px, 4, 5);
			placeDummyConnector(scene2, px, 4, 6);
		}

		// RF Cavity (x=21, 3 roof posts along z=5 at x=18, 21, 24)
		placeDuct(scene2, 18, 6, 5, coldFluid, true, false, false, false, false, true);
		placeDuct(scene2, 18, 5, 5, coldFluid, false, false, false, false, true, true);
		placeDummyConnector(scene2, 18, 4, 5);
		placeCable(scene2, 21, 6, 5, false, false, false, false, true, true);
		placeCable(scene2, 21, 5, 5, false, false, false, false, true, true);
		placeDummyConnector(scene2, 21, 4, 5);
		placeDuct(scene2, 24, 6, 5, warmFluid, false, true, false, false, false, true);
		placeDuct(scene2, 24, 5, 5, warmFluid, false, false, false, false, true, true);
		placeDummyConnector(scene2, 24, 4, 5);

		// Detector rear wall (x=1) drops to y=3
		placeCable(scene2, 1, 6, 5, false, false, false, false, true, true);
		for (int y = 5; y >= 4; y--) {
			placeDuct(scene2, 1, y, 4, coldFluid, false, false, false, false, true, true);
			placeCable(scene2, 1, y, 5, false, false, false, false, true, true);
			placeDuct(scene2, 1, y, 6, warmFluid, false, false, false, false, true, true);
		}
		placeDuct(scene2, 1, 3, 4, coldFluid, false, false, true, false, true, false);
		placeCable(scene2, 1, 3, 5, false, false, true, false, true, false);
		placeDuct(scene2, 1, 3, 6, warmFluid, false, false, true, false, true, false);
		placeDummyConnector(scene2, 2, 3, 4);
		placeDummyConnector(scene2, 2, 3, 5);
		placeDummyConnector(scene2, 2, 3, 6);

		// Mixer (26,2,9) -> Comp #1 (22,2,9) -> Comp #2 (18,2,9)
		placeDuctLineX(scene2, 23, 25, 2, 9, warmFluid, true, true);
		placeDuctLineX(scene2, 19, 21, 2, 9, warmFluid, true, true);

		// Warm return from (22,6,6) -> (22,6,9) -> down into Comp #1
		for (int z = 7; z <= 8; z++) {
			placeDuct(scene2, 22, 6, z, warmFluid, true, true, false, false, false, false);
		}
		placeDuct(scene2, 22, 6, 9, warmFluid, true, false, false, false, false, true);
		for (int y = 5; y >= 3; y--) {
			placeDuct(scene2, 22, y, 9, warmFluid, false, false, false, false, true, true);
		}

		// Cold supply rising from Comp #2 (18,2,9) -> (18,6,9) -> (18,6,4)
		for (int y = 3; y <= 5; y++) {
			placeDuct(scene2, 18, y, 9, coldFluid, false, false, false, false, true, true);
		}
		placeDuct(scene2, 18, 6, 9, coldFluid, true, false, false, false, false, true);
		for (int z = 8; z >= 5; z--) {
			placeDuct(scene2, 18, 6, z, coldFluid, true, true, false, false, false, false);
		}

		scene2.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, -75,
				new Object[][] { { tr("cannery.pa.2.ports_and_draw") } }, 235)
				.setColors(colorGold).setOrientation(Orientation.BOTTOM)));
		scene2.add(new ActionWait(95));
		scene2.add(new ActionRemoveActor(ID_PANEL_MAIN));
		scene2.add(new ActionRotateBy(55, 0, 20));

		// =====================================================================
		// SCENE 3: Source & Detector GUI Walkthrough
		// =====================================================================
		WiajScene scene3 = new WiajScene(script);

		ActorGuiPreview srcGui = new ActorGuiPreview(Mode.SOURCE, 95, -5)
				.withSlots(
						new ItemStack(ModItems.PARTICLE_COPPER.get()),
						new ItemStack(ModItems.PARTICLE_HYDROGEN.get()),
						new ItemStack(ModItems.PARTICLE_EMPTY.get()),
						new ItemStack(ModItems.PARTICLE_EMPTY.get())
				);
		scene3.add(new ActionCreateActor(ID_GUI_OVERLAY, srcGui));
		scene3.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, -95, -5,
				new Object[][] { { tr("cannery.pa.3.cooling_leds") } }, 175)
				.setColors(COLOR_CYAN).setOrientation(Orientation.RIGHT)));

		scene3.add(new ActionUpdateActor(ID_GUI_OVERLAY, "highlight", "temp"));
		for (int t = 293; t >= 123; t -= 10) {
			scene3.add(new ActionUpdateActor(ID_GUI_OVERLAY, "tempK", Math.max(123, t)));
			scene3.add(new ActionWait(2));
		}
		scene3.add(new ActionUpdateActor(ID_GUI_OVERLAY, "powered", true));
		scene3.add(new ActionUpdateActor(ID_GUI_OVERLAY, "highlight", "power"));
		scene3.add(new ActionWait(40));

		scene3.add(new ActionUpdateActor(ID_GUI_OVERLAY, "status", "Running"));
		scene3.add(new ActionUpdateActor(ID_GUI_OVERLAY, "highlight", "botSlots"));
		scene3.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, -95, -5,
				new Object[][] { { tr("cannery.pa.3.source_slots") } }, 175)
				.setColors(colorGold).setOrientation(Orientation.RIGHT)));
		scene3.add(new ActionWait(75));

		scene3.add(new ActionUpdateActor(ID_GUI_OVERLAY, "highlight", "infoBtn"));
		scene3.add(new ActionUpdateActor(ID_GUI_OVERLAY, "tooltip", "cannery.pa.tip.last_momentum_300"));
		scene3.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, -95, -5,
				new Object[][] { { tr("cannery.pa.3.source_buttons") } }, 175)
				.setColors(colorBlue).setOrientation(Orientation.RIGHT)));
		scene3.add(new ActionWait(80));
		scene3.add(new ActionRemoveActor(ID_GUI_OVERLAY));

		ActorGuiPreview detGui = new ActorGuiPreview(Mode.DETECTOR, 95, -5)
				.withSlots(
						new ItemStack(ModItems.PARTICLE_EMPTY.get(), 64),
						new ItemStack(ModItems.PARTICLE_EMPTY.get(), 64),
						new ItemStack(ModItems.PARTICLE_AMAT.get()),
						ItemStack.EMPTY
				);
		scene3.add(new ActionCreateActor(ID_GUI_OVERLAY, detGui));
		scene3.add(new ActionUpdateActor(ID_GUI_OVERLAY, "tempK", 123));
		scene3.add(new ActionUpdateActor(ID_GUI_OVERLAY, "powered", true));
		scene3.add(new ActionUpdateActor(ID_GUI_OVERLAY, "highlight", "topSlots"));
		scene3.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, -95, -5,
				new Object[][] { { tr("cannery.pa.3.detector_capsules") } }, 180)
				.setColors(COLOR_RED).setOrientation(Orientation.RIGHT)));
		scene3.add(new ActionWait(95));

		scene3.add(new ActionRemoveActor(ID_GUI_OVERLAY));
		scene3.add(new ActionRemoveActor(ID_PANEL_MAIN));

		for (WiajScene intro : introScenes) {
			script.addScene(intro);
		}
		script.addScene(scene0).addScene(scene1).addScene(scene2).addScene(scene3);
		return script;
	}
}
