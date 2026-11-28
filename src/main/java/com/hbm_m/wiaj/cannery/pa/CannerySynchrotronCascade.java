package com.hbm_m.wiaj.cannery.pa;

import static com.hbm_m.wiaj.cannery.pa.PaWiajHelpers.*;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.radio.RadioTorchBaseBlock;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;

/**
 * Dual-Ring Cascade (Ring #1 Gold 15x15 + Ring #2 NbTi 21x21) & 3x Speed Upgrade:
 * - Scene 0: Full Ring #1 + Redstone-over-Radio receivers under Dipoles #3 & #4
 * - Scene 1: Full Ring #2 (NbTi, 21-block sides, threshold 6,500) + cascade transfer
 * - Scene 2: 3x RF Cavities + 3x Quadrupoles speed upgrade + final checklist
 */
public class CannerySynchrotronCascade extends CanneryBase {

	// Dedicated non-overlapping machine actor ID ranges (0..29 Ring 1, 30..69 Ring 2)
	private static final int ID_R1_BASE = 0;
	private static final int ID_R2_BASE = 30;

	@Override
	public ItemStack getIcon() {
		return new ItemStack(ModItems.PA_COIL_NIOBIUM.get());
	}

	@Override
	public String getName() {
		return "cannery.pa_cascade";
	}

	@Override
	public CanneryBase[] seeAlso() {
		return new CanneryBase[] {
				new CanneryParticleAccelerator(),
				new CanneryParticleCoils(),
				new CannerySynchrotron()
		};
	}

	@Override
	public WiajScript createScript() {
		VirtualWorld world = new VirtualWorld(62, 8, 45);
		WiajScript script = new WiajScript(world);
		var font = Minecraft.getInstance().font;

		// =====================================================================
		// SCENE 0: Full Ring #1 + Redstone-over-Radio under Dipoles #3 and #4
		// =====================================================================
		WiajScene scene0 = new WiajScene(script);
		scene0.add(new ActionSetZoom(0.54, 0));
		scene0.add(new ActionOffsetBy(-11.0, 0.0, 0.0, 0));

		buildHollowRingFloor(scene0, 32, 50, 6, 24, 1, 2, ModBlocks.CONCRETE_SMOOTH.get());
		fillBox(scene0, 52, 1, 4, 60, 1, 8, ModBlocks.CONCRETE_SMOOTH.get());
		fillBox(scene0, 29, 1, 26, 35, 1, 38, ModBlocks.CONCRETE_SMOOTH.get());
		digDetectorPit(scene0, 30, 34, 29, 37);

		buildHollowRingFloor(scene0, 2, 26, 6, 30, 1, 2, ModBlocks.CONCRETE_SMOOTH.get());
		fillBox(scene0, 0, 1, 32, 5, 1, 44, ModBlocks.CONCRETE_SMOOTH.get());
		digDetectorPit(scene0, 0, 4, 35, 43);

		int id1 = ID_R1_BASE;
		// Ring 1: Source (x=56, 52..60) + 4 Dipoles + Side 1 (RFC) + Side 2 (Quad) + Detector 1 + Sides 3 & 4
		scene0.add(new ActionCreateActor(id1++, new ActorTileEntity(CanneryParticleAccelerator.paSource(), ActorNucleusMachine.posTagForBeam(56, 3, 6, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(id1++, new ActorTileEntity(CanneryParticleAccelerator.paDipole(), ActorNucleusMachine.posTagForBeam(50, 3, 6, Direction.SOUTH)).smoothPlace()));
		scene0.add(new ActionCreateActor(id1++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(50, 3, 9, Direction.SOUTH)).smoothPlace()));
		scene0.add(new ActionCreateActor(id1++, new ActorTileEntity(CanneryParticleAccelerator.paRfc(), ActorNucleusMachine.posTagForBeam(50, 3, 15, Direction.SOUTH)).smoothPlace()));
		scene0.add(new ActionCreateActor(id1++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(50, 3, 21, Direction.SOUTH)).smoothPlace()));
		scene0.add(new ActionCreateActor(id1++, new ActorTileEntity(CanneryParticleAccelerator.paDipole(), ActorNucleusMachine.posTagForBeam(50, 3, 24, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(id1++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(47, 3, 24, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(id1++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(44, 3, 24, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(id1++, new ActorTileEntity(CanneryParticleAccelerator.paQuadrupole(), ActorNucleusMachine.posTagForBeam(41, 3, 24, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(id1++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(38, 3, 24, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(id1++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(35, 3, 24, Direction.WEST)).smoothPlace()));
		scene0.add(new ActionCreateActor(id1++, new ActorTileEntity(CanneryParticleAccelerator.paDipole(), ActorNucleusMachine.posTagForBeam(32, 3, 24, Direction.NORTH)).smoothPlace()));
		scene0.add(new ActionCreateActor(id1++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(32, 3, 27, Direction.SOUTH)).smoothPlace()));
		scene0.add(new ActionCreateActor(id1++, new ActorTileEntity(CanneryParticleAccelerator.paDetector(), ActorNucleusMachine.posTagForBeam(32, 3, 33, Direction.SOUTH)).smoothPlace()));
		for (int z = 21; z >= 9; z -= 3) {
			scene0.add(new ActionCreateActor(id1++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(32, 3, z, Direction.NORTH)).smoothPlace()));
		}
		scene0.add(new ActionCreateActor(id1++, new ActorTileEntity(CanneryParticleAccelerator.paDipole(), ActorNucleusMachine.posTagForBeam(32, 3, 6, Direction.EAST)).smoothPlace()));
		for (int x = 35; x <= 47; x += 3) {
			scene0.add(new ActionCreateActor(id1++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(x, 3, 6, Direction.EAST)).smoothPlace()));
		}

		// ROR receivers under Dipole #3 (32,1,24) and Dipole #4 (32,1,6), plus Lever + Sender
		scene0.add(new ActionSetBlock(32, 1, 24, ModBlocks.RADIO_TORCH_RECEIVER.get().defaultBlockState()
				.setValue(RadioTorchBaseBlock.FACING, Direction.UP)));
		scene0.add(new ActionSetBlock(32, 1, 6, ModBlocks.RADIO_TORCH_RECEIVER.get().defaultBlockState()
				.setValue(RadioTorchBaseBlock.FACING, Direction.UP)));
		scene0.add(new ActionSetBlock(42, 2, 2, ModBlocks.CONCRETE_HAZARD.get()));
		scene0.add(new ActionSetBlock(42, 3, 2, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.POWERED, false)));
		scene0.add(new ActionSetBlock(41, 2, 2, ModBlocks.RADIO_TORCH_SENDER.get().defaultBlockState()
				.setValue(RadioTorchBaseBlock.FACING, Direction.UP)));
		scene0.add(new ActionCreateActor(ID_HIGHLIGHT_BASE,     new ActorWorldHighlight(31.5, 1, 23.5, 33.5, 2.2, 25.5, 0xCCE53935).xray()));
		scene0.add(new ActionCreateActor(ID_HIGHLIGHT_BASE + 1, new ActorWorldHighlight(31.5, 1,  5.5, 33.5, 2.2,  7.5, 0xCCE53935).xray()));

		ActorGuiPreview dip4Gui = new ActorGuiPreview(Mode.DIPOLE, 105, -5)
				.withCoil(new ItemStack(ModItems.PA_COIL_GOLD.get()));
		scene0.add(new ActionCreateActor(ID_GUI_OVERLAY, dip4Gui));
		scene0.add(new ActionUpdateActor(ID_GUI_OVERLAY, "tempK", 123));
		scene0.add(new ActionUpdateActor(ID_GUI_OVERLAY, "powered", true));
		scene0.add(new ActionUpdateActor(ID_GUI_OVERLAY, "hasCoil", true));
		scene0.add(new ActionUpdateActor(ID_GUI_OVERLAY, "threshold", 1500));
		scene0.add(new ActionUpdateActor(ID_GUI_OVERLAY, "dirLow", "E"));
		scene0.add(new ActionUpdateActor(ID_GUI_OVERLAY, "dirHigh", "E"));
		scene0.add(new ActionUpdateActor(ID_GUI_OVERLAY, "dirRedstone", "W"));
		scene0.add(new ActionUpdateActor(ID_GUI_OVERLAY, "highlight", "row3"));
		scene0.add(new ActionUpdateActor(ID_GUI_OVERLAY, "tooltip", "cannery.pa.tip.cascade_1500"));

		scene0.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, -95, -10,
				new Object[][] { { tr("cannery.pa.cascade.0.redstone_switch") } }, 185)
				.setColors(colorBlue).setOrientation(Orientation.RIGHT)));
		scene0.add(new ActionWait(95));
		scene0.add(new ActionRemoveActor(ID_GUI_OVERLAY));
		scene0.add(new ActionRemoveActor(ID_HIGHLIGHT_BASE));
		scene0.add(new ActionRemoveActor(ID_HIGHLIGHT_BASE + 1));
		scene0.add(new ActionRemoveActor(ID_PANEL_MAIN));

		// =====================================================================
		// SCENE 1: Full Ring #2 (NbTi, 21-block sides, threshold 6,500) + Cascade
		// =====================================================================
		WiajScene scene1 = new WiajScene(script);
		scene1.add(new ActionOffsetBy(11.0, 0.0, 0.0, 25));
		scene1.add(new ActionSetZoom(-0.08, 25));

		int id2 = ID_R2_BASE;
		// Bridge + 4 Dipoles of Ring #2
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(29, 3, 6, Direction.WEST)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paDipole(), ActorNucleusMachine.posTagForBeam(26, 3, 6, Direction.SOUTH)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paDipole(), ActorNucleusMachine.posTagForBeam(26, 3, 30, Direction.WEST)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paDipole(), ActorNucleusMachine.posTagForBeam(2, 3, 30, Direction.NORTH)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paDipole(), ActorNucleusMachine.posTagForBeam(2, 3, 6, Direction.EAST)).smoothPlace()));

		// Side 5 (+Z, 21 blocks): 2xBeam + RF(9) + 2xBeam
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(26, 3, 9, Direction.SOUTH)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(26, 3, 12, Direction.SOUTH)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paRfc(), ActorNucleusMachine.posTagForBeam(26, 3, 18, Direction.SOUTH)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(26, 3, 24, Direction.SOUTH)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(26, 3, 27, Direction.SOUTH)).smoothPlace()));

		// Side 6 (-X, 21 blocks): 3xBeam + Quad(3) + 3xBeam
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(23, 3, 30, Direction.WEST)).smoothPlace()));
		final int idS6QuadA = id2++;
		scene1.add(new ActionCreateActor(idS6QuadA, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(20, 3, 30, Direction.WEST)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(17, 3, 30, Direction.WEST)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paQuadrupole(), ActorNucleusMachine.posTagForBeam(14, 3, 30, Direction.WEST)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(11, 3, 30, Direction.WEST)).smoothPlace()));
		final int idS6QuadB = id2++;
		scene1.add(new ActionCreateActor(idS6QuadB, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(8, 3, 30, Direction.WEST)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(5, 3, 30, Direction.WEST)).smoothPlace()));

		// Detector 2 branch (+Z, after Side 6 Quadrupole)
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(2, 3, 33, Direction.SOUTH)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paDetector(), ActorNucleusMachine.posTagForBeam(2, 3, 39, Direction.SOUTH)).smoothPlace()));

		// Side 7 (-Z, 21 blocks): 7xBeam (middle 3 saved for RFC replacement in Scene 2)
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(2, 3, 27, Direction.NORTH)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(2, 3, 24, Direction.NORTH)).smoothPlace()));
		final int idS7Mid0 = id2++;
		scene1.add(new ActionCreateActor(idS7Mid0, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(2, 3, 21, Direction.NORTH)).smoothPlace()));
		final int idS7Mid1 = id2++;
		scene1.add(new ActionCreateActor(idS7Mid1, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(2, 3, 18, Direction.NORTH)).smoothPlace()));
		final int idS7Mid2 = id2++;
		scene1.add(new ActionCreateActor(idS7Mid2, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(2, 3, 15, Direction.NORTH)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(2, 3, 12, Direction.NORTH)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(2, 3, 9, Direction.NORTH)).smoothPlace()));

		// Side 8 (+X, 21 blocks): 7xBeam (middle 3 saved for RFC replacement in Scene 2)
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(5, 3, 6, Direction.EAST)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(8, 3, 6, Direction.EAST)).smoothPlace()));
		final int idS8Mid0 = id2++;
		scene1.add(new ActionCreateActor(idS8Mid0, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(11, 3, 6, Direction.EAST)).smoothPlace()));
		final int idS8Mid1 = id2++;
		scene1.add(new ActionCreateActor(idS8Mid1, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(14, 3, 6, Direction.EAST)).smoothPlace()));
		final int idS8Mid2 = id2++;
		scene1.add(new ActionCreateActor(idS8Mid2, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(17, 3, 6, Direction.EAST)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(20, 3, 6, Direction.EAST)).smoothPlace()));
		scene1.add(new ActionCreateActor(id2++, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(23, 3, 6, Direction.EAST)).smoothPlace()));

		scene1.add(new ActionSetBlock(42, 3, 2, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.POWERED, true)));
		scene1.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, 82,
				new Object[][] { { tr("cannery.pa.cascade.1.lever_on") } }, 240)
				.setColors(COLOR_GREEN).setOrientation(Orientation.TOP)));

		ActorParticleBeam cascadeBeam = new ActorParticleBeam(1.8, false,
				new double[] { 50.5, 3.5, 6.5 }, new double[] { 50.5, 3.5, 24.5 },
				new double[] { 32.5, 3.5, 24.5 }, new double[] { 32.5, 3.5, 6.5 },
				new double[] { 26.5, 3.5, 6.5 }, new double[] { 26.5, 3.5, 30.5 },
				new double[] {  2.5, 3.5, 30.5 }, new double[] {  2.5, 3.5, 6.5 },
				new double[] { 26.5, 3.5, 6.5 }, new double[] { 26.5, 3.5, 30.5 },
				new double[] {  2.5, 3.5, 30.5 }, new double[] {  2.5, 3.5, 35.5 }
		);
		scene1.add(new ActionCreateActor(ID_PARTICLE, cascadeBeam));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "momentum", 1400));
		scene1.add(new ActionWait(20));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "momentum", 1500));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "statusNote", "cannery.pa.note.cascade_enter"));
		scene1.add(new ActionWait(35));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "momentum", 4200));
		scene1.add(new ActionWait(45));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "momentum", 6500));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "statusNote", "cannery.pa.note.cascade_det2"));
		scene1.add(new ActionWait(55));
		scene1.add(new ActionRemoveActor(ID_PARTICLE));
		scene1.add(new ActionRemoveActor(ID_PANEL_MAIN));

		// =====================================================================
		// SCENE 2: 3x Speed Upgrade (3x RFC + 3x Quadrupole) + Checklist
		// =====================================================================
		WiajScene scene2 = new WiajScene(script);
		scene2.add(new ActionOffsetBy(14.0, 0.0, -4.0, 25));
		scene2.add(new ActionSetZoom(0.35, 25));

		// Replace 2 beamlines on Side 6 with Quadrupoles
		scene2.add(new ActionCreateActor(idS6QuadA, new ActorTileEntity(CanneryParticleAccelerator.paQuadrupole(), ActorNucleusMachine.posTagForBeam(20, 3, 30, Direction.WEST)).smoothPlace()));
		scene2.add(new ActionCreateActor(idS6QuadB, new ActorTileEntity(CanneryParticleAccelerator.paQuadrupole(), ActorNucleusMachine.posTagForBeam(8, 3, 30, Direction.WEST)).smoothPlace()));

		// Remove middle 3 beamlines on Side 7 & Side 8 before placing 9-block RF Cavities
		scene2.add(new ActionRemoveActor(idS7Mid0));
		scene2.add(new ActionRemoveActor(idS7Mid1));
		scene2.add(new ActionRemoveActor(idS7Mid2));
		scene2.add(new ActionCreateActor(idS7Mid1, new ActorTileEntity(CanneryParticleAccelerator.paRfc(), ActorNucleusMachine.posTagForBeam(2, 3, 18, Direction.NORTH)).smoothPlace()));

		scene2.add(new ActionRemoveActor(idS8Mid0));
		scene2.add(new ActionRemoveActor(idS8Mid1));
		scene2.add(new ActionRemoveActor(idS8Mid2));
		scene2.add(new ActionCreateActor(idS8Mid1, new ActorTileEntity(CanneryParticleAccelerator.paRfc(), ActorNucleusMachine.posTagForBeam(14, 3, 6, Direction.EAST)).smoothPlace()));

		scene2.add(new ActionCreateActor(ID_HIGHLIGHT_BASE, new ActorWorldHighlight(6.5, 2, 28.5, 21.5, 5.2, 31.5, 0x884CD964)));
		scene2.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, -78,
				new Object[][] { { tr("cannery.pa.cascade.2.upgrade_3x") } }, 240)
				.setColors(COLOR_GREEN).setOrientation(Orientation.BOTTOM)));
		scene2.add(new ActionWait(95));
		scene2.add(new ActionRemoveActor(ID_HIGHLIGHT_BASE));

		scene2.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, 0,
				new Object[][] { { tr("cannery.pa.cascade.2.checklist") } }, 245)
				.setColors(colorGold).setOrientation(Orientation.CENTER)));
		scene2.add(new ActionWait(120));
		scene2.add(new ActionRemoveActor(ID_PANEL_MAIN));

		script.addScene(scene0).addScene(scene1).addScene(scene2);
		return script;
	}
}
