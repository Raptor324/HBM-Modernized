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


    //---------------------------<СТАНКИ>-------------------------------------
    public static final RegistryObject<Block> BLAST_FURNACE = registerBlock("blast_furnace",
            () -> new BlastFurnaceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL)));

    public static final RegistryObject<Block> PRESS = registerBlock("press",
            () -> new PressBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL)));

    public static final RegistryObject<Block> WOOD_BURNER = registerBlock("wood_burner",
            () -> new WoodBurnerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL)));

    // -----------------------<РУДЫ>-----------------------------
    public static final RegistryObject<Block> LIGNITE_ORE = registerBlock("lignite_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> ALUMINUM_ORE = registerBlock("aluminum_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> URANIUM_ORE_H = registerBlock("uranium_ore_h",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> LEAD_ORE = registerBlock("lead_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> RAREGROUND_ORE = registerBlock("rareground_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> FLUORITE_ORE = registerBlock("fluorite_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> BERYLLIUM_ORE = registerBlock("beryllium_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> ASBESTOS_ORE = registerBlock("asbestos_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> CINNABAR_ORE = registerBlock("cinnabar_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> COBALT_ORE = registerBlock("cobalt_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> TUNGSTEN_ORE = registerBlock("tungsten_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> THORIUM_ORE = registerBlock("thorium_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> FREAKY_ALIEN_BLOCK = registerBlock("freaky_alien_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> TITANIUM_ORE = registerBlock("titanium_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> SULFUR_ORE = registerBlock("sulfur_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    // Дипслейт руды
    public static final RegistryObject<Block> URANIUM_ORE_DEEPSLATE = registerBlock("uranium_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> BERYLLIUM_ORE_DEEPSLATE = registerBlock("beryllium_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> TITANIUM_ORE_DEEPSLATE = registerBlock("titanium_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> LEAD_ORE_DEEPSLATE = registerBlock("lead_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> RAREGROUND_ORE_DEEPSLATE = registerBlock("rareground_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> THORIUM_ORE_DEEPSLATE = registerBlock("thorium_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> ALUMINUM_ORE_DEEPSLATE = registerBlock("aluminum_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));


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