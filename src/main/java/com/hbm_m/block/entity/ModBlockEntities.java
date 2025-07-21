package com.hbm_m.block.entity;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
}