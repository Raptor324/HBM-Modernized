package com.hbm_m.block.entity;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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

    public static final RegistryObject<BlockEntityType<MachineAssemblerPartBlockEntity>> MACHINE_ASSEMBLER_PART_BE =
		BLOCK_ENTITIES.register("machine_assembler_part_be", () ->
			BlockEntityType.Builder.<MachineAssemblerPartBlockEntity>of(MachineAssemblerPartBlockEntity::new, ModBlocks.MACHINE_ASSEMBLER_PART.get())
				.build(null));

    public static final RegistryObject<BlockEntityType<AdvancedAssemblyMachineBlockEntity>> ADVANCED_ASSEMBLY_MACHINE =
		BLOCK_ENTITIES.register("advanced_assembly_machine_be", () ->
			BlockEntityType.Builder.<AdvancedAssemblyMachineBlockEntity>of(AdvancedAssemblyMachineBlockEntity::new, ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get())
				.build(null));

    public static final RegistryObject<BlockEntityType<AdvancedAssemblyMachinePartBlockEntity>> ADVANCED_ASSEMBLY_MACHINE_PART =
        BLOCK_ENTITIES.register("advanced_assembly_machine_part_be", () ->
			BlockEntityType.Builder.<AdvancedAssemblyMachinePartBlockEntity>of(AdvancedAssemblyMachinePartBlockEntity::new, ModBlocks.ADVANCED_ASSEMBLY_MACHINE_PART.get())
				.build(null));

     public static final RegistryObject<BlockEntityType<WireBlockEntity>> WIRE_BE =
		BLOCK_ENTITIES.register("wire_be", () ->
			BlockEntityType.Builder.<WireBlockEntity>of(WireBlockEntity::new, ModBlocks.WIRE_COATED.get())
				.build(null));

    public static final RegistryObject<BlockEntityType<MachineBatteryBlockEntity>> MACHINE_BATTERY_BE =
		BLOCK_ENTITIES.register("machine_battery_be", () ->
			BlockEntityType.Builder.<MachineBatteryBlockEntity>of(MachineBatteryBlockEntity::new, ModBlocks.MACHINE_BATTERY.get())
				.build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}