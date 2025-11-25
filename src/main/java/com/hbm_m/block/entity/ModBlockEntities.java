package com.hbm_m.block.entity;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.hbm_m.block.entity.IronCrateBlockEntity;
import com.hbm_m.block.entity.SteelCrateBlockEntity;
import com.hbm_m.block.entity.machine.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.block.entity.machine.MachineAssemblerBlockEntity;
import com.hbm_m.block.entity.machine.MachineBatteryBlockEntity;
import com.hbm_m.block.entity.machine.MachinePressBlockEntity;
import com.hbm_m.block.entity.machine.MachineShredderBlockEntity;
import com.hbm_m.block.entity.machine.MachineWoodBurnerBlockEntity;
import com.hbm_m.block.entity.machine.UniversalMachinePartBlockEntity;
import com.hbm_m.block.entity.DeshCrateBlockEntity;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
		DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RefStrings.MODID);

    public static final RegistryObject<BlockEntityType<GeigerCounterBlockEntity>> GEIGER_COUNTER_BE =
		BLOCK_ENTITIES.register("geiger_counter_be", () ->
			BlockEntityType.Builder.<GeigerCounterBlockEntity>of(GeigerCounterBlockEntity::new, ModBlocks.GEIGER_COUNTER_BLOCK.get())
				.build(null));

    public static final RegistryObject<BlockEntityType<MachineAssemblerBlockEntity>> MACHINE_ASSEMBLER_BE =
		BLOCK_ENTITIES.register("machine_assembler_be", () ->
			BlockEntityType.Builder.<MachineAssemblerBlockEntity>of(MachineAssemblerBlockEntity::new, ModBlocks.MACHINE_ASSEMBLER.get())
				.build(null));

    public static final RegistryObject<BlockEntityType<MachineAdvancedAssemblerBlockEntity>> ADVANCED_ASSEMBLY_MACHINE_BE =
		BLOCK_ENTITIES.register("advanced_assembly_machine_be", () ->
			BlockEntityType.Builder.<MachineAdvancedAssemblerBlockEntity>of(MachineAdvancedAssemblerBlockEntity::new, ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get())
				.build(null));

    public static final RegistryObject<BlockEntityType<MachineBatteryBlockEntity>> MACHINE_BATTERY_BE =
            BLOCK_ENTITIES.register("machine_battery_be", () -> {
                // Превращаем список RegistryObject в массив Block[]
                Block[] validBlocks = ModBlocks.BATTERY_BLOCKS.stream()
                        .map(RegistryObject::get)
                        .toArray(Block[]::new);

                return BlockEntityType.Builder.<MachineBatteryBlockEntity>of(MachineBatteryBlockEntity::new, validBlocks)
                        .build(null);
            });

    public static final RegistryObject<BlockEntityType<AnvilBlockEntity>> ANVIL_BE =
        BLOCK_ENTITIES.register("anvil_be", () ->
            BlockEntityType.Builder.<AnvilBlockEntity>of(AnvilBlockEntity::new,
                    ModBlocks.ANVIL_BLOCK.get(),
                    ModBlocks.ANVIL_IRON.get(),
                    ModBlocks.ANVIL_LEAD.get(),
                    ModBlocks.ANVIL_STEEL.get(),
                    ModBlocks.ANVIL_DESH.get(),
                    ModBlocks.ANVIL_FERROURANIUM.get(),
                    ModBlocks.ANVIL_SATURNITE.get(),
                    ModBlocks.ANVIL_BISMUTH_BRONZE.get(),
                    ModBlocks.ANVIL_ARSENIC_BRONZE.get(),
                    ModBlocks.ANVIL_SCHRABIDATE.get(),
                    ModBlocks.ANVIL_DNT.get(),
                    ModBlocks.ANVIL_OSMIRIDIUM.get(),
                    ModBlocks.ANVIL_MURKY.get()).build(null));

    public static final RegistryObject<BlockEntityType<MineBlockEntity>> MINE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("mine_block_entity", () ->
                    BlockEntityType.Builder.of(MineBlockEntity::new, ModBlocks.MINE_AP.get())
                            .build(null)
            );

    public static final RegistryObject<BlockEntityType<MineBlockEntity>> MINE_NUKE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("mine_nuke_block_entity", () ->
                    BlockEntityType.Builder.of(MineBlockEntity::new, ModBlocks.MINE_FAT.get())
                            .build(null)
            );
    public static final RegistryObject<BlockEntityType<MachineShredderBlockEntity>> SHREDDER =
            BLOCK_ENTITIES.register("shredder", () ->
                    BlockEntityType.Builder.of(MachineShredderBlockEntity::new,
                            ModBlocks.SHREDDER.get()).build(null));

    public static final RegistryObject<BlockEntityType<UniversalMachinePartBlockEntity>> UNIVERSAL_MACHINE_PART_BE =
        BLOCK_ENTITIES.register("universal_machine_part_be", () ->
			BlockEntityType.Builder.<UniversalMachinePartBlockEntity>of(UniversalMachinePartBlockEntity::new, ModBlocks.UNIVERSAL_MACHINE_PART.get())
				.build(null));

	public static final RegistryObject<BlockEntityType<WireBlockEntity>> WIRE_BE =
		BLOCK_ENTITIES.register("wire_be", () ->
			BlockEntityType.Builder.<WireBlockEntity>of(WireBlockEntity::new, ModBlocks.WIRE_COATED.get())
				.build(null));


    public static final RegistryObject<BlockEntityType<SwitchBlockEntity>> SWITCH_BE =
            BLOCK_ENTITIES.register("switch_be", () ->
                    BlockEntityType.Builder.of(SwitchBlockEntity::new, ModBlocks.SWITCH.get())
                            .build(null));

	public static final RegistryObject<BlockEntityType<BlastFurnaceBlockEntity>> BLAST_FURNACE_BE =
			BLOCK_ENTITIES.register("blast_furnace_be", () ->
					BlockEntityType.Builder.of(BlastFurnaceBlockEntity::new,
							ModBlocks.BLAST_FURNACE.get()).build(null));

	public static final RegistryObject<BlockEntityType<MachinePressBlockEntity>> PRESS_BE =
			BLOCK_ENTITIES.register("press_be", () ->
					BlockEntityType.Builder.of(MachinePressBlockEntity::new,
							ModBlocks.PRESS.get()).build(null));

	public static final RegistryObject<BlockEntityType<MachineWoodBurnerBlockEntity>> WOOD_BURNER_BE =
			BLOCK_ENTITIES.register("wood_burner_be", () ->
					BlockEntityType.Builder.of(MachineWoodBurnerBlockEntity::new,
							ModBlocks.WOOD_BURNER.get()).build(null));

    // ДВЕРИ

    public static final RegistryObject<BlockEntityType<DoorBlockEntity>> DOOR_ENTITY = 
        BLOCK_ENTITIES.register("door", () -> 
                BlockEntityType.Builder.of(DoorBlockEntity::new,
                // Все блоки дверей, которые используют этот BlockEntity
                        ModBlocks.LARGE_VEHICLE_DOOR.get(),
                        ModBlocks.ROUND_AIRLOCK_DOOR.get(),
                        ModBlocks.TRANSITION_SEAL.get(),
                        ModBlocks.FIRE_DOOR.get(),
                        ModBlocks.SLIDE_DOOR.get(),
                        ModBlocks.SLIDING_SEAL_DOOR.get(),
                        ModBlocks.SECURE_ACCESS_DOOR.get(),
                        ModBlocks.QE_SLIDING.get(),
                        ModBlocks.QE_CONTAINMENT.get(),
                        ModBlocks.WATER_DOOR.get(),
                        ModBlocks.SILO_HATCH.get(),
                        ModBlocks.SILO_HATCH_LARGE.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<IronCrateBlockEntity>> IRON_CRATE_BE =
            BLOCK_ENTITIES.register("iron_crate_be", () ->
                    BlockEntityType.Builder.<IronCrateBlockEntity>of(
                            IronCrateBlockEntity::new,
                            ModBlocks.CRATE_IRON.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<SteelCrateBlockEntity>> STEEL_CRATE_BE =
            BLOCK_ENTITIES.register("steel_crate_be", () ->
                    BlockEntityType.Builder.<SteelCrateBlockEntity>of(
                            SteelCrateBlockEntity::new,
                            ModBlocks.CRATE_STEEL.get()
                    ).build(null));
    public static final RegistryObject<BlockEntityType<DeshCrateBlockEntity>> DESH_CRATE_BE =
            BLOCK_ENTITIES.register("desh_crate_be", () ->
                    BlockEntityType.Builder.<DeshCrateBlockEntity>of(
                            DeshCrateBlockEntity::new,
                            ModBlocks.CRATE_DESH.get()
                    ).build(null));
        

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}