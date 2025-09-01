package com.hbm_m.block;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.item.ModItems;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LeavesBlock;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, RefStrings.MODID);

    public static final RegistryObject<Block> GEIGER_COUNTER_BLOCK = registerBlock("geiger_counter_block",
            () -> new GeigerCounterBlock(Block.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));
            
    private static final BlockBehaviour.Properties TABLE_PROPERTIES =
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
            
    public static final RegistryObject<Block> URANIUM_BLOCK = registerBlock("uranium_block",
            () -> new RadioactiveBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL)));

    public static final RegistryObject<Block> POLONIUM210_BLOCK = registerBlock("polonium210_block",
            () -> new RadioactiveBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL)));

    public static final RegistryObject<Block> PLUTONIUM_BLOCK = registerBlock("plutonium_block",
            () -> new RadioactiveBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL)));

    public static final RegistryObject<Block> PLUTONIUM_FUEL_BLOCK = registerBlock("plutonium_fuel_block",
            () -> new RadioactiveBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL)));

    public static final RegistryObject<Block> ARMOR_TABLE = registerBlock("armor_table",
            () -> new ArmorTableBlock(TABLE_PROPERTIES));

    public static final RegistryObject<Block> MACHINE_ASSEMBLER = registerBlockWithoutItem("machine_assembler",
        () -> new MachineAssemblerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f).noOcclusion()));

    public static final RegistryObject<Block> MACHINE_ASSEMBLER_PART = registerBlockWithoutItem("machine_assembler_part",
            () -> new MachineAssemblerPartBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f).noOcclusion().noParticlesOnBreak()));

    public static final RegistryObject<Block> ADVANCED_ASSEMBLY_MACHINE = BLOCKS.register("advanced_assembly_machine",
        () -> new AdvancedAssemblyMachineBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f).noOcclusion()));

    public static final RegistryObject<Block> ADVANCED_ASSEMBLY_MACHINE_PART = BLOCKS.register("advanced_assembly_machine_part",
        () -> new AdvancedAssemblyMachinePartBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f).noOcclusion().noParticlesOnBreak()));

    public static final RegistryObject<Block> MACHINE_BATTERY = registerBlockWithoutItem("machine_battery",
        () -> new MachineBatteryBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f).noOcclusion()));

    public static final RegistryObject<Block> URANIUM_ORE = registerBlock("uranium_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(3.0F, 3.0F).requiresCorrectToolForDrops(),
                    UniformInt.of(2, 5))); // Будет выпадать 2-5 единиц опыта

    public static final RegistryObject<Block> WASTE_GRASS = registerBlock("waste_grass",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIRT).sound(SoundType.GRAVEL)));
            
    public static final RegistryObject<Block> WASTE_LEAVES = registerBlock("waste_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES).noOcclusion()));

    public static final RegistryObject<Block> WIRE_COATED = registerBlock("wire_coated",
            () -> new WireBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));


    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }
    private static <T extends Block> RegistryObject<T> registerBlockWithoutItem(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}