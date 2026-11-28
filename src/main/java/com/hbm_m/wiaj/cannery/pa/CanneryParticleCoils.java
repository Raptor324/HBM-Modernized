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
import com.hbm_m.wiaj.actor.ActorTileEntity;
import com.hbm_m.wiaj.cannery.CanneryBase;
import com.hbm_m.wiaj.cannery.pa.PaWiajActors.*;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/**
 * Large Coils, x10 energy penalties, and particle crash causes demo:
 * - Scene 0: 4 coil tiers (Gold, NbTi, BSCCO, Chlorophyte)
 * - Scene 1: Short side penalty (9 < 15 -> x10 HE)
 * - Scene 2: Double penalty (x100 -> CRASH) & max momentum exceeded (>2200 -> CRASH)
 */
public class CanneryParticleCoils extends CanneryBase {

	@Override
	public ItemStack getIcon() {
		return new ItemStack(ModItems.PA_COIL_GOLD.get());
	}

	@Override
	public String getName() {
		return "cannery.pa_coils";
	}

	@Override
	public CanneryBase[] seeAlso() {
		return new CanneryBase[] {
				new CanneryParticleAccelerator(),
				new CannerySynchrotron(),
				new CannerySynchrotronCascade()
		};
	}

	@Override
	public WiajScript createScript() {
		VirtualWorld world = new VirtualWorld(29, 7, 13);
		WiajScript script = new WiajScript(world);
		var font = Minecraft.getInstance().font;

		// =====================================================================
		// SCENE 0: 4 Large Coil Tiers (floor y=0, dipoles at y=2)
		// =====================================================================
		WiajScene scene0 = new WiajScene(script);
		scene0.add(new ActionSetZoom(1.15, 0));
		fillBox(scene0, 0, 0, 0, 28, 0, 12, ModBlocks.CONCRETE_SMOOTH.get());

		int[] xs = { 23, 17, 11, 5 };
		for (int i = 0; i < 4; i++) {
			scene0.add(new ActionCreateActor(i, new ActorTileEntity(
					CanneryParticleAccelerator.paDipole(),
					ActorNucleusMachine.posTagForBeam(xs[i], 2, 6, Direction.WEST)).smoothPlace()));
			scene0.add(new ActionWait(6));
		}

		scene0.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, -68,
				new Object[][] {
					{ new ItemStack(ModItems.PA_COIL_GOLD.get()),        " " + tr("cannery.pa.coils.tier.gold") },
					{ new ItemStack(ModItems.PA_COIL_NIOBIUM.get()),     " " + tr("cannery.pa.coils.tier.nbti") },
					{ new ItemStack(ModItems.PA_COIL_BSCCO.get()),       " " + tr("cannery.pa.coils.tier.bscco") },
					{ new ItemStack(ModItems.PA_COIL_CHLOROPHYTE.get()), " " + tr("cannery.pa.coils.tier.chlorophyte") }
				}, 0).setColors(colorBlue).setOrientation(Orientation.BOTTOM)));
		scene0.add(new ActionWait(120));
		scene0.add(new ActionRemoveActor(ID_PANEL_MAIN));
		for (int i = 0; i < 4; i++) scene0.add(new ActionRemoveActor(i));

		// =====================================================================
		// SCENE 1: x10 Penalty (short side 9 < 15)
		// =====================================================================
		WiajScene scene1 = new WiajScene(script);

		scene1.add(new ActionCreateActor(0, new ActorTileEntity(CanneryParticleAccelerator.paDipole(), ActorNucleusMachine.posTagForBeam(20, 2, 6, Direction.WEST)).smoothPlace()));
		scene1.add(new ActionCreateActor(1, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(17, 2, 6, Direction.WEST)).smoothPlace()));
		scene1.add(new ActionCreateActor(2, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(14, 2, 6, Direction.WEST)).smoothPlace()));
		scene1.add(new ActionCreateActor(3, new ActorTileEntity(CanneryParticleAccelerator.paBeamline(), ActorNucleusMachine.posTagForBeam(11, 2, 6, Direction.WEST)).smoothPlace()));
		scene1.add(new ActionCreateActor(4, new ActorTileEntity(CanneryParticleAccelerator.paDipole(), ActorNucleusMachine.posTagForBeam(8, 2, 6, Direction.WEST)).smoothPlace()));

		scene1.add(new ActionCreateActor(ID_HIGHLIGHT_BASE, new ActorWorldHighlight(9.5, 2.0, 6.0, 18.5, 3.0, 7.0, 0x88FAD64A)));
		scene1.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, 70,
				new Object[][] { { tr("cannery.pa.coils.penalty_length") } }, 230)
				.setColors(colorGold).setOrientation(Orientation.TOP)));

		ActorParticleBeam p1 = new ActorParticleBeam(0.35, false,
				new double[] { 20.5, 2.5, 6.5 }, new double[] { 8.5, 2.5, 6.5 });
		scene1.add(new ActionCreateActor(ID_PARTICLE, p1));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "momentum", 400));
		scene1.add(new ActionWait(34));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "penalty", true));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "statusNote", "cannery.pa.note.penalty_len"));
		scene1.add(new ActionWait(45));
		scene1.add(new ActionRemoveActor(ID_PARTICLE));

		// Penalty #2: Low momentum (< 1,500 in NbTi) -> x10 HE draw
		scene1.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, 70,
				new Object[][] { { tr("cannery.pa.coils.penalty_momentum") } }, 230)
				.setColors(colorGold).setOrientation(Orientation.TOP)));
		ActorParticleBeam p1b = new ActorParticleBeam(0.35, false,
				new double[] { 20.5, 2.5, 6.5 }, new double[] { 8.5, 2.5, 6.5 });
		scene1.add(new ActionCreateActor(ID_PARTICLE, p1b));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "momentum", 400));
		scene1.add(new ActionWait(34));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "penalty", true));
		scene1.add(new ActionUpdateActor(ID_PARTICLE, "statusNote", "cannery.pa.note.penalty_mom"));
		scene1.add(new ActionWait(45));
		scene1.add(new ActionRemoveActor(ID_PARTICLE));
		scene1.add(new ActionRemoveActor(ID_PANEL_MAIN));

		// =====================================================================
		// SCENE 2: Double Penalty (x100 = CRASH) & Max Exceeded (>2200 = CRASH)
		// =====================================================================
		WiajScene scene2 = new WiajScene(script);

		scene2.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, 70,
				new Object[][] {
					{ new ItemStack(ModItems.PA_COIL_NIOBIUM.get()), " §c9 < 21 (x10) + 400 < 1,500 (x10) = x100!" },
					{ tr("cannery.pa.coils.crash_double") }
				}, 235).setColors(COLOR_RED).setOrientation(Orientation.TOP)));

		ActorParticleBeam p2 = new ActorParticleBeam(0.35, false,
				new double[] { 20.5, 2.5, 6.5 }, new double[] { 8.5, 2.5, 6.5 });
		scene2.add(new ActionCreateActor(ID_PARTICLE, p2));
		scene2.add(new ActionUpdateActor(ID_PARTICLE, "momentum", 400));
		scene2.add(new ActionWait(34));
		scene2.add(new ActionUpdateActor(ID_PARTICLE, "crashed", true));
		scene2.add(new ActionUpdateActor(ID_PARTICLE, "statusNote", "cannery.pa.note.crash_double"));
		scene2.add(new ActionWait(60));
		scene2.add(new ActionRemoveActor(ID_PARTICLE));

		scene2.add(new ActionRemoveActor(ID_HIGHLIGHT_BASE));
		scene2.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, 70,
				new Object[][] {
					{ new ItemStack(ModItems.PA_COIL_GOLD.get()), " §cM: 2,300 > MAX 2,200!" },
					{ tr("cannery.pa.coils.crash_max") }
				}, 235).setColors(COLOR_RED).setOrientation(Orientation.TOP)));

		ActorParticleBeam p3 = new ActorParticleBeam(0.45, false,
				new double[] { 20.5, 2.5, 6.5 }, new double[] { 8.5, 2.5, 6.5 });
		scene2.add(new ActionCreateActor(ID_PARTICLE, p3));
		scene2.add(new ActionUpdateActor(ID_PARTICLE, "momentum", 2300));
		scene2.add(new ActionWait(27));
		scene2.add(new ActionUpdateActor(ID_PARTICLE, "crashed", true));
		scene2.add(new ActionUpdateActor(ID_PARTICLE, "statusNote", "cannery.pa.note.crash_max"));
		scene2.add(new ActionWait(70));
		scene2.add(new ActionRemoveActor(ID_PARTICLE));

		// 2C) Defocus >= 1,000 (or D > 0 at Detector) -> CRASH!
		scene2.add(new ActionCreateActor(ID_PANEL_MAIN, new ActorFancyPanel(font, 0, 70,
				new Object[][] { { tr("cannery.pa.coils.crash_defocus") } }, 235)
				.setColors(COLOR_RED).setOrientation(Orientation.TOP)));
		ActorParticleBeam p4 = new ActorParticleBeam(0.45, false,
				new double[] { 20.5, 2.5, 6.5 }, new double[] { 8.5, 2.5, 6.5 });
		scene2.add(new ActionCreateActor(ID_PARTICLE, p4));
		scene2.add(new ActionUpdateActor(ID_PARTICLE, "momentum", 1000));
		scene2.add(new ActionUpdateActor(ID_PARTICLE, "defocus", 900));
		scene2.add(new ActionWait(20));
		scene2.add(new ActionUpdateActor(ID_PARTICLE, "defocus", 1000));
		scene2.add(new ActionUpdateActor(ID_PARTICLE, "crashed", true));
		scene2.add(new ActionUpdateActor(ID_PARTICLE, "statusNote", "cannery.pa.note.crash_defocus"));
		scene2.add(new ActionWait(65));

		scene2.add(new ActionRemoveActor(ID_PARTICLE));
		scene2.add(new ActionRemoveActor(ID_PANEL_MAIN));

		script.addScene(scene0).addScene(scene1).addScene(scene2);
		return script;
	}
}
