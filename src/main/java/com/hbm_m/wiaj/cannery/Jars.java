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

		canneries.put(ModItems.CONVEYOR_WAND_REGULAR.get(), new CanneryConveyor());
		canneries.put(ModItems.CONVEYOR_WAND_EXPRESS.get(), new CanneryConveyor());
		canneries.put(ModItems.CONVEYOR_WAND_DOUBLE.get(), new CanneryConveyor());
		canneries.put(ModItems.CONVEYOR_WAND_TRIPLE.get(), new CanneryConveyor());
		canneries.put(ModBlocks.CONVEYOR_PRESS.get().asItem(), new CanneryConveyorPress());

		canneries.put(PartTabMetaItems.get("plant_item_mustardwillow").get(), new CanneryWillow());
		canneries.put(ModBlocks.PLANT_FLOWER_CD0.get().asItem(), new CanneryWillow());
	}
}
