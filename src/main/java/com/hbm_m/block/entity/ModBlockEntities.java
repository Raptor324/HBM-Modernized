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

    public static final RegistryObject<BlockEntityType<ArmorTableBlockEntity>> ARMOR_TABLE_BE =
            BLOCK_ENTITIES.register("armor_table_be", () ->
                    BlockEntityType.Builder.of(ArmorTableBlockEntity::new, ModBlocks.ARMOR_TABLE.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<GeigerCounterBlockEntity>> GEIGER_COUNTER_BE =
            BLOCK_ENTITIES.register("geiger_counter_be", () ->
                    BlockEntityType.Builder.of(GeigerCounterBlockEntity::new, ModBlocks.GEIGER_COUNTER_BLOCK.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<MachineAssemblerBlockEntity>> MACHINE_ASSEMBLER_BE =
            BLOCK_ENTITIES.register("machine_assembler_be", () ->
                    BlockEntityType.Builder.of(MachineAssemblerBlockEntity::new, ModBlocks.MACHINE_ASSEMBLER.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<MachineAssemblerPartBlockEntity>> MACHINE_ASSEMBLER_PART_BE =
            BLOCK_ENTITIES.register("machine_assembler_part_be", () ->
                    BlockEntityType.Builder.of(MachineAssemblerPartBlockEntity::new, ModBlocks.MACHINE_ASSEMBLER_PART.get())
                            .build(null));

    // Статический блок инициализации, который выполнится после создания всех RegistryObject'ов
    static {
        // "Инъекция" поставщиков в классы BlockEntity.
        // Это происходит ПОСЛЕ того, как все поля выше созданы, но ДО того, как игра их использует.
        ArmorTableBlockEntity.TYPE_SUPPLIER = ARMOR_TABLE_BE;
        GeigerCounterBlockEntity.TYPE_SUPPLIER = GEIGER_COUNTER_BE;
        MachineAssemblerBlockEntity.TYPE_SUPPLIER = MACHINE_ASSEMBLER_BE;
        MachineAssemblerPartBlockEntity.TYPE_SUPPLIER = MACHINE_ASSEMBLER_PART_BE;
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}