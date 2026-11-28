package com.hbm_m.wiaj.cannery.pa;

import static com.hbm_m.wiaj.cannery.pa.PaWiajHelpers.*;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;
import com.hbm_m.wiaj.WiajScript;
import com.hbm_m.wiaj.action.*;
import com.hbm_m.wiaj.actor.ActorFancyPanel;
import com.hbm_m.wiaj.actor.ActorFancyPanel.Orientation;
import com.hbm_m.wiaj.actor.ActorPlayer;
import com.hbm_m.wiaj.actor.ActorTileEntity;
import com.hbm_m.wiaj.cannery.CanneryBase;
import com.hbm_m.wiaj.cannery.pa.PaWiajActors.*;
import com.hbm_m.wiaj.cannery.pa.PaWiajActors.ActorGuiPreview.Mode;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Ring Synchrotron (Ring #1 on Gold coils, side = 15 blocks):
 * - Scene 0: Step-by-step 15x15 ring + Detector after Quadrupole
 * - Scene 1: Dipole GUI (relative compass with ActorPlayer + threshold 400)
 * - Scene 2: Live 4-lap Antimatter run (300) at threshold 400
 */
public class CannerySynchrotron extends CanneryBase {

	@Override
	public ItemStack getIcon() {
		return new ItemStack(ModBlocks.DIPOLE.get());
	}

	@Override
	public String getName() {
		return "cannery.pa_synchrotron";
	}

	@Override
	public CanneryBase[] seeAlso() {
		return new CanneryBase[] {
				new CanneryParticleAccelerator(),
				new CanneryParticleCoils(),
				new CannerySynchrotronCascade()
		};
	}

	@Override
	public WiajScript createScript() {
		VirtualWorld world = new VirtualWorld(35, 8, 39);
		WiajScript script = new WiajScript(world);
		var font = Minecraft.getInstance().font;

		// =====================================================================
		// SCENE 0: Ring #1 Assembly (Gold, 15-block sides, beam axis y=3)
		// =====================================================================
		WiajScene scene0 = new WiajScene(script);
		scene0.add(new ActionSetZoom(0.68, 0));

		buildHollowRingFloor(scene0, 6, 24, 6, 24, 1, 2, ModBlocks.CONCRETE_SMOOTH.get());
		fillBox(scene0, 26, 1, 4, 34, 1, 8, ModBlocks.CONCRETE_SMOOTH.get());
		fillBox(scene0, 3, 1, 26, 9, 1, 38, ModBlocks.CONCRETE_SMOOTH.get());
		digDetectorPit(scene0, 4, 8, 29, 37);
		scene0.add(new ActionWait(6));

		int id = 0;
		scene0.add(new ActionCreateActor(id++, new ActorTileEntity(CanneryParticleAccelerator.paSource(), ActorNucleusMachine.posTagForBeam(30, 3, 6, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(id++, new ActorTileEntity(CanneryParticleAccelerator.paDipole(), ActorNucleusMachine.posTagForBeam(24, 3, 6, Direction.SOUTH)).smoothPlace()));
		scene0.add(new ActionWait(8));

		// Side 1 (+Z, 15 blocks): Beam + RFC(9) + Beam -> Dipole 2
		scene0.add(new ActionCreateActor(id++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(24, 3, 9, Direction.SOUTH)).smoothPlace()));
		scene0.add(new ActionCreateActor(id++, new ActorTileEntity(CanneryParticleAccelerator.paRfc(), ActorNucleusMachine.posTagForBeam(24, 3, 15, Direction.SOUTH)).smoothPlace()));
		scene0.add(new ActionCreateActor(id++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(24, 3, 21, Direction.SOUTH)).smoothPlace()));
		scene0.add(new ActionCreateActor(id++, new ActorTileEntity(CanneryParticleAccelerator.paDipole(), ActorNucleusMachine.posTagForBeam(24, 3, 24, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionWait(10));

		// Side 2 (-X, 15 blocks): 2x Beam + Quad(3) + 2x Beam -> Dipole 3
		scene0.add(new ActionCreateActor(id++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(21, 3, 24, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(id++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(18, 3, 24, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(id++, new ActorTileEntity(CanneryParticleAccelerator.paQuadrupole(), ActorNucleusMachine.posTagForBeam(15, 3, 24, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(id++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(12, 3, 24, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(id++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(9, 3, 24, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(id++, new ActorTileEntity(CanneryParticleAccelerator.paDipole(), ActorNucleusMachine.posTagForBeam(6, 3, 24, Direction.NORTH)).smoothPlace()));
		scene0.add(new ActionWait(10));

		// Detector 1 branch (+Z, after Quadrupole)
		scene0.add(new ActionCreateActor(id++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(6, 3, 27, Direction.SOUTH)).smoothPlace()));
		scene0.add(new ActionCreateActor(id++, new ActorTileEntity(CanneryParticleAccelerator.paDetector(), ActorNucleusMachine.posTagForBeam(6, 3, 33, Direction.SOUTH)).smoothPlace()));
		scene0.add(new ActionWait(8));

		// Side 3 (-Z, 5x Beam) -> Dipole 4 -> Side 4 (+X, 5x Beam)
		for (int z = 21; z >= 9; z -= 3) {
			scene0.add(new ActionCreateActor(id++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(6, 3, z, Direction.NORTH)).smoothPlace()));
		}
		scene0.add(new ActionCreateActor(id++, new ActorTileEntity(CanneryParticleAccelerator.paDipole(), ActorNucleusMachine.posTagForBeam(6, 3, 6, Direction.EAST)).smoothPlace()));
		for (int x = 9; x <= 21; x += 3) {
			scene0.add(new ActionCreateActor(id++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(x, 3, 6, Direction.EAST)).smoothPlace()));
		}

		scene0.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, -80,
				new Object[][] { { tr("cannery.pa.sync.0.ring_math") } }, 240)
				.setColors(colorBlue).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionWait(95));
		scene0.add(new ActionRemoveActor(ID_PANEL_MAIN));

		// =====================================================================
		// SCENE 1: Dipole #3 GUI + Relative Compass with ActorPlayer
		// =====================================================================
		WiajScene scene1 = new WiajScene(script);

		scene1.add(new ActionSetZoom(0.65, 25));
		scene1.add(new ActionOffsetBy(10.0, 0.0, -5.0, 25));

		CompoundTag pTag = new CompoundTag();
		pTag.putDouble("x", 6.5); pTag.putDouble("y", 2.0); pTag.putDouble("z", 21.5);
		pTag.putDouble("prevX", 6.5); pTag.putDouble("prevY", 2.0); pTag.putDouble("prevZ", 21.5);
		pTag.putFloat("yaw", 0F); pTag.putFloat("prevYaw", 0F);
		pTag.putBoolean("visible", true);
		scene1.add(new ActionCreateActor(ID_PLAYER, new ActorPlayer(pTag)));

		ActorGuiPreview dipGui = new ActorGuiPreview(Mode.DIPOLE, 100, -5)
				.withCoil(new ItemStack(ModItems.PA_COIL_GOLD.get()));
		scene1.add(new ActionCreateActor(ID_GUI_OVERLAY, dipGui));
		scene1.add(new ActionUpdateActor(ID_GUI_OVERLAY, "tempK", 123));
		scene1.add(new ActionUpdateActor(ID_GUI_OVERLAY, "powered", true));
		scene1.add(new ActionUpdateActor(ID_GUI_OVERLAY, "hasCoil", true));
		scene1.add(new ActionUpdateActor(ID_GUI_OVERLAY, "threshold", 400));
		scene1.add(new ActionUpdateActor(ID_GUI_OVERLAY, "dirLow", "N"));
		scene1.add(new ActionUpdateActor(ID_GUI_OVERLAY, "dirHigh", "S"));
		scene1.add(new ActionUpdateActor(ID_GUI_OVERLAY, "dirRedstone", "S"));
		scene1.add(new ActionUpdateActor(ID_GUI_OVERLAY, "playerDir", "S"));
		scene1.add(new ActionUpdateActor(ID_GUI_OVERLAY, "highlight", "row1"));
		scene1.add(new ActionUpdateActor(ID_GUI_OVERLAY, "tooltip", "cannery.pa.tip.player_s"));

		scene1.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, -95, -10,
				new Object[][] { { tr("cannery.pa.sync.1.dipole_compass") } }, 175)
				.setColors(colorGold).setOrientation(Orientation.RIGHT)));
		scene1.add(new ActionWait(55));

		scene1.add(new ActionUpdateActor(ID_PLAYER, "yaw", -90F));
		scene1.add(new ActionUpdateActor(ID_GUI_OVERLAY, "playerDir", "E"));
		scene1.add(new ActionUpdateActor(ID_GUI_OVERLAY, "tooltip", "cannery.pa.tip.player_e"));
		scene1.add(new ActionWait(55));

		scene1.add(new ActionUpdateActor(ID_GUI_OVERLAY, "highlight", "row2"));
		scene1.add(new ActionUpdateActor(ID_GUI_OVERLAY, "tooltip", "cannery.pa.tip.dipole_400"));
		scene1.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, -95, -10,
				new Object[][] { { tr("cannery.pa.sync.1.dipole_threshold") } }, 175)
				.setColors(COLOR_GREEN).setOrientation(Orientation.RIGHT)));
		scene1.add(new ActionWait(80));

		scene1.add(new ActionRemoveActor(ID_PLAYER));
		scene1.add(new ActionRemoveActor(ID_GUI_OVERLAY));
		scene1.add(new ActionRemoveActor(ID_PANEL_MAIN));
		scene1.add(new ActionSetZoom(-0.65, 25));
		scene1.add(new ActionOffsetBy(-10.0, 0.0, 5.0, 25));

		// =====================================================================
		// SCENE 2: Live 4-Lap Antimatter Run (M: 0 -> 400 -> Detector)
		// =====================================================================
		WiajScene scene2 = new WiajScene(script);

		scene2.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, 80,
				new Object[][] { { tr("cannery.pa.sync.2.four_laps") } }, 235)
				.setColors(COLOR_GREEN).setOrientation(Orientation.TOP)));

		ActorParticleBeam ringBeam = new ActorParticleBeam(1.2, false,
				new double[] { 26.0, 3.5, 6.5 },
				new double[] { 24.5, 3.5, 6.5 }, new double[] { 24.5, 3.5, 24.5 }, new double[] { 6.5, 3.5, 24.5 }, new double[] { 6.5, 3.5, 6.5 },
				new double[] { 24.5, 3.5, 6.5 }, new double[] { 24.5, 3.5, 24.5 }, new double[] { 6.5, 3.5, 24.5 }, new double[] { 6.5, 3.5, 6.5 },
				new double[] { 24.5, 3.5, 6.5 }, new double[] { 24.5, 3.5, 24.5 }, new double[] { 6.5, 3.5, 24.5 }, new double[] { 6.5, 3.5, 6.5 },
				new double[] { 24.5, 3.5, 6.5 }, new double[] { 24.5, 3.5, 24.5 }, new double[] { 6.5, 3.5, 24.5 }, new double[] { 6.5, 3.5, 29.5 }
		);
		scene2.add(new ActionCreateActor(ID_PARTICLE, ringBeam));

		for (int lap = 1; lap <= 4; lap++) {
			scene2.add(new ActionWait(10));
			scene2.add(new ActionUpdateActor(ID_PARTICLE, "momentum", lap * 100));
			scene2.add(new ActionUpdateActor(ID_PARTICLE, "defocus", 100));
			scene2.add(new ActionWait(15));
			scene2.add(new ActionUpdateActor(ID_PARTICLE, "defocus", 0));
			if (lap < 4) {
				scene2.add(new ActionUpdateActor(ID_PARTICLE, "statusNote", "cannery.pa.note.dipole3_loop"));
				scene2.add(new ActionWait(35));
			} else {
				scene2.add(new ActionUpdateActor(ID_PARTICLE, "statusNote", "cannery.pa.note.dipole3_det"));
				scene2.add(new ActionWait(30));
			}
		}

		scene2.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, 80,
				new Object[][] { { tr("cannery.pa.sync.2.threshold_tip") } }, 235)
				.setColors(colorGold).setOrientation(Orientation.TOP)));
		scene2.add(new ActionWait(90));

		scene2.add(new ActionRemoveActor(ID_PARTICLE));
		scene2.add(new ActionRemoveActor(ID_PANEL_MAIN));

		script.addScene(scene0).addScene(scene1).addScene(scene2);
		return script;
	}
}
