package com.hbm_m.wiaj.cannery;

import java.util.HashMap;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.PartTabMetaItems;

import net.minecraft.world.item.Item;

/**
 * Registry of demo scripts keyed by their trigger item.
 * <p>
 * Port of {@code com.hbm.wiaj.cannery.Jars} (1.7.10). The original keyed by
 * {@code ComparableStack} (item + meta); in modern Minecraft items have no meta, so the
 * key is the plain {@link Item}. The original meta-differentiated triggers resolve to
 * dedicated items here: {@code plant_item[MUSTARDWILLOW]} is the standalone
 * {@code plant_item_mustardwillow} item and {@code plant_flower[CD0]} is the
 * {@code plant_flower_cd0} block's item. Blocks without an explicit {@code ModItems}
 * field (stirling, stirling_steel, foundry_channel, plant_flower_cd0) get their
 * {@code ModBlocks.registerBlock} auto-registered BlockItem via {@code Block#asItem()}.
 */
public class Jars {

	public static HashMap<Item, CanneryBase> canneries = new HashMap<>();

	public static void initJars() {
		canneries.put(ModItems.FIREBOX.get(), new CanneryFirebox());
		canneries.put(ModBlocks.STIRLING.get().asItem(), new CanneryStirling());
		canneries.put(ModBlocks.STIRLING_STEEL.get().asItem(), new CanneryStirling());
		canneries.put(ModItems.MACHINE_GASCENT.get(), new CanneryCentrifuge());
		canneries.put(ModItems.FEL.get(), new CannerySILEX());
		canneries.put(ModItems.SILEX.get(), new CannerySILEX());
		canneries.put(ModBlocks.FOUNDRY_CHANNEL.get().asItem(), new CanneryFoundryChannel());
		canneries.put(ModItems.CRUCIBLE.get(), new CanneryCrucible());
		canneries.put(ModBlocks.CARGO_ELEVATOR.get().asItem(), new CanneryCargoElevator());

		CanneryBase conveyor = new com.hbm_m.wiaj.cannery.conveyor.CanneryConveyor();
		canneries.put(ModItems.CONVEYOR_WAND_REGULAR.get(), conveyor);
		canneries.put(ModItems.CONVEYOR_WAND_EXPRESS.get(), conveyor);
		canneries.put(ModItems.CONVEYOR_WAND_DOUBLE.get(), conveyor);
		canneries.put(ModItems.CONVEYOR_WAND_TRIPLE.get(), conveyor);
		canneries.put(ModBlocks.CRANE_EXTRACTOR.get().asItem(), conveyor);
		canneries.put(ModBlocks.CRANE_INSERTER.get().asItem(), conveyor);
		canneries.put(ModBlocks.CRANE_SPLITTER.get().asItem(), conveyor);
		canneries.put(ModBlocks.CRANE_BOXER.get().asItem(), conveyor);
		canneries.put(ModBlocks.CRANE_UNBOXER.get().asItem(), conveyor);
		canneries.put(ModBlocks.CRANE_ROUTER.get().asItem(), conveyor);
		canneries.put(ModBlocks.CONVEYOR_PRESS.get().asItem(), new CanneryConveyorPress());

		canneries.put(PartTabMetaItems.get("plant_item_mustardwillow").get(), new CanneryWillow());
		canneries.put(ModBlocks.PLANT_FLOWER_CD0.get().asItem(), new CanneryWillow());

		CanneryBase paBasics = new com.hbm_m.wiaj.cannery.pa.CanneryParticleAccelerator();
		CanneryBase paCoils  = new com.hbm_m.wiaj.cannery.pa.CanneryParticleCoils();
		CanneryBase paSync   = new com.hbm_m.wiaj.cannery.pa.CannerySynchrotron();

		canneries.put(ModBlocks.PA_SOURCE.get().asItem(),   paBasics);
		canneries.put(ModBlocks.PA_DETECTOR.get().asItem(), paBasics);
		canneries.put(ModBlocks.RFC.get().asItem(),         paBasics);
		canneries.put(ModBlocks.QUADRUPOLE.get().asItem(),  paBasics);
		canneries.put(ModBlocks.BEAMLINE.get().asItem(),    paBasics);

		canneries.put(ModItems.PA_COIL_GOLD.get(),        paCoils);
		canneries.put(ModItems.PA_COIL_NIOBIUM.get(),     paCoils);
		canneries.put(ModItems.PA_COIL_BSCCO.get(),       paCoils);
		canneries.put(ModItems.PA_COIL_CHLOROPHYTE.get(), paCoils);

		canneries.put(ModBlocks.DIPOLE.get().asItem(), paSync);

		CanneryBase rbmkIntro    = new com.hbm_m.wiaj.cannery.rbmk.CanneryRbmkIntro();
		CanneryBase rbmkBasics   = new com.hbm_m.wiaj.cannery.rbmk.CanneryRbmkBasics();
		CanneryBase rbmkStarter  = new com.hbm_m.wiaj.cannery.rbmk.CanneryRbmkStarter();
		CanneryBase rbmkAdvanced = new com.hbm_m.wiaj.cannery.rbmk.CanneryRbmkAdvanced();

		canneries.put(ModBlocks.RBMK_ROD_MOD.get().asItem(),       rbmkIntro);
		canneries.put(ModBlocks.RBMK_COOLER.get().asItem(),        rbmkIntro);
		canneries.put(ModBlocks.RBMK_ROD.get().asItem(),           rbmkBasics);
		canneries.put(ModBlocks.RBMK_MODERATOR.get().asItem(),     rbmkBasics);
		canneries.put(ModBlocks.RBMK_REFLECTOR.get().asItem(),     rbmkBasics);
		canneries.put(ModBlocks.RBMK_ABSORBER.get().asItem(),      rbmkBasics);
		canneries.put(ModBlocks.RBMK_CONTROL.get().asItem(),       rbmkBasics);
		canneries.put(ModBlocks.RBMK_CONTROL_AUTO.get().asItem(),  rbmkBasics);
		canneries.put(ModBlocks.RBMK_OUTGASSER.get().asItem(),     rbmkBasics);
		canneries.put(ModItems.RBMK_LID.get(),                     rbmkBasics);
		canneries.put(ModItems.RBMK_LID_GLASS.get(),               rbmkBasics);

		canneries.put(ModBlocks.RBMK_BOILER.get().asItem(),        rbmkStarter);
		canneries.put(ModBlocks.RBMK_STEAM_INLET.get().asItem(),   rbmkStarter);
		canneries.put(ModBlocks.RBMK_STEAM_OUTLET.get().asItem(),  rbmkStarter);
		canneries.put(ModBlocks.RBMK_LOADER.get().asItem(),        rbmkStarter);
		canneries.put(ModItems.RBMK_CONSOLE.get(),                 rbmkStarter);
		canneries.put(ModItems.LINKER.get(),                       rbmkStarter);
		canneries.put(ModBlocks.RBMK_CRANE_CONSOLE.get().asItem(), rbmkStarter);

		canneries.put(ModBlocks.RBMK_BLANK.get().asItem(),         rbmkAdvanced);
		canneries.put(ModBlocks.MACHINE_STORAGE_DRUM.get().asItem(), rbmkAdvanced);
		canneries.put(ModItems.RBMK_FUEL_MEP.get(),                rbmkAdvanced);
		canneries.put(ModItems.RBMK_FUEL_MEU.get(),                rbmkAdvanced);
		canneries.put(ModItems.RBMK_FUEL_MOX.get(),                rbmkAdvanced);
		canneries.put(ModItems.RBMK_FUEL_HEU235.get(),             rbmkAdvanced);
		canneries.put(ModItems.RBMK_FUEL_LEP.get(),                rbmkAdvanced);
		canneries.put(ModItems.RBMK_FUEL_HEP.get(),                rbmkAdvanced);
		canneries.put(ModItems.RBMK_FUEL_RA226BE.get(),            rbmkAdvanced);
		canneries.put(ModItems.RBMK_FUEL_DRX.get(),                rbmkAdvanced);
	}
}
