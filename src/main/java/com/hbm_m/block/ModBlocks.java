package com.hbm_m.block;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.block.entity.DoorDecl;
import com.hbm_m.item.ModItems;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.util.valueproviders.UniformInt;

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

    public static final RegistryObject<Block> ANVIL = registerBlock("anvil",
            () -> new AnvilBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL)));

    public static final RegistryObject<Block> BLAST_FURNACE = registerBlock("blast_furnace",
            () -> new BlastFurnaceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL)));

    public static final RegistryObject<Block> PRESS = registerBlockWithoutItem("press",
            () -> new MachinePressBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistryObject<Block> WOOD_BURNER = registerBlockWithoutItem("wood_burner",
            () -> new MachineWoodBurnerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

	public static final RegistryObject<Block> ARMOR_TABLE = registerBlock("armor_table",
            () -> new ArmorTableBlock(TABLE_PROPERTIES));

    public static final RegistryObject<Block> SHREDDER = registerBlock("shredder",
            () -> new ShredderBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));


    public static final RegistryObject<Block> MACHINE_ASSEMBLER = registerBlockWithoutItem("machine_assembler",
        () -> new MachineAssemblerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f).noOcclusion()));

    public static final RegistryObject<Block> ADVANCED_ASSEMBLY_MACHINE = registerBlockWithoutItem("advanced_assembly_machine",
        () -> new MachineAdvancedAssemblerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f).noOcclusion()));

    public static final RegistryObject<Block> UNIVERSAL_MACHINE_PART = registerBlockWithoutItem("universal_machine_part",
        () -> new UniversalMachinePartBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f).noOcclusion().noParticlesOnBreak()));

    public static final RegistryObject<Block> MACHINE_BATTERY = registerBlockWithoutItem("machine_battery",
        () -> new MachineBatteryBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f)));

    // public static final RegistryObject<Block> FLUID_TANK = registerBlockWithoutItem("fluid_tank",
    //     () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    //---------------------------<БЛОКИ>-------------------------------------
    public static final RegistryObject<Block> REINFORCED_STONE = registerBlock("reinforced_stone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> REINFORCED_GLASS = registerBlock("reinforced_glass",
            () -> new GlassBlock(BlockBehaviour.Properties.copy(Blocks.GLASS).strength(4.0F, 12.0F)));

    public static final RegistryObject<Block> CRATE = registerBlock("crate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD).strength(1.0f, 1.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CRATE_LEAD = registerBlock("crate_lead",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD).strength(1.0f, 1.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CRATE_METAL = registerBlock("crate_metal",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD).strength(1.0f, 1.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CRATE_WEAPON = registerBlock("crate_weapon",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD).strength(1.0f, 1.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> CONCRETE_HAZARD = registerBlock("concrete_hazard",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CONCRETE_HAZARD_STAIRS = registerBlock("concrete_hazard_stairs",
            () -> new StairBlock(() -> ModBlocks.CONCRETE_HAZARD.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistryObject<Block> CONCRETE_HAZARD_SLAB = registerBlock("concrete_hazard_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistryObject<Block> BRICK_CONCRETE = registerBlock("brick_concrete",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> BRICK_CONCRETE_STAIRS = registerBlock("brick_concrete_stairs",
            () -> new StairBlock(() -> ModBlocks.BRICK_CONCRETE.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistryObject<Block> BRICK_CONCRETE_SLAB = registerBlock("brick_concrete_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistryObject<Block> CONCRETE_MOSSY = registerBlock("concrete_mossy",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CONCRETE_MOSSY_STAIRS = registerBlock("concrete_mossy_stairs",
            () -> new StairBlock(() -> ModBlocks.CONCRETE_MOSSY.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistryObject<Block> CONCRETE_MOSSY_SLAB = registerBlock("concrete_mossy_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistryObject<Block> CONCRETE  = registerBlock("concrete",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CONCRETE_STAIRS = registerBlock("concrete_stairs",
            () -> new StairBlock(() -> ModBlocks.CONCRETE.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistryObject<Block> CONCRETE_SLAB = registerBlock("concrete_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistryObject<Block> CONCRETE_CRACKED  = registerBlock("concrete_cracked",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CONCRETE_CRACKED_STAIRS = registerBlock("concrete_cracked_stairs",
            () -> new StairBlock(() -> ModBlocks.CONCRETE_CRACKED.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistryObject<Block> CONCRETE_CRACKED_SLAB = registerBlock("concrete_cracked_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistryObject<Block> CONCRETE_VENT  = registerBlock("concrete_vent",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> DET_MINER = registerBlock("det_miner",
            () -> new DetMinerBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F) // Прочность блока (как у камня или земли)
                    .sound(SoundType.STONE) // Звук при разрушении
                    .requiresCorrectToolForDrops() // Требует правильного инструмента для лута (как руды)
                    // .noOcclusion() // <--- ЭТО ОЧЕНЬ ВАЖНО! НЕ ИСПОЛЬЗУЙТЕ ЭТО ДЛЯ БЛОКОВ, КОТОРЫЕ ДОЛЖНЫ РЕАГИРОВАТЬ НА РЕДСТОУН
                    //                    // noOcclusion делает блок "неполным" или "прозрачным" для редстоуна.
                    //                    // Если ваш блок должен быть твердым и проводить редстоун, то эти свойства должны быть по умолчанию.
                    // .isRedstoneConductor((state, level, pos) -> true) // Можно явно указать, что блок проводит редстоун
                    // .isViewBlocking((state, level, pos) -> true) // Блок блокирует обзор
            ));

    //public static final RegistryObject<Block> DEMON_LAMP  = registerBlock("demon_lamp",
           // () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> DOOR_BUNKER = registerBlock("door_bunker",
            () -> new net.minecraft.world.level.block.DoorBlock(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK).sound(SoundType.NETHERITE_BLOCK).noOcclusion(), BlockSetType.STONE));

    public static final RegistryObject<Block> DOOR_OFFICE = registerBlock("door_office",
            () -> new net.minecraft.world.level.block.DoorBlock(BlockBehaviour.Properties.copy(Blocks.CHERRY_WOOD).sound(SoundType.CHERRY_WOOD).noOcclusion(), BlockSetType.CHERRY));

    public static final RegistryObject<Block> METAL_DOOR = registerBlock("metal_door",
            () -> new net.minecraft.world.level.block.DoorBlock(BlockBehaviour.Properties.copy(Blocks.CHAIN).sound(SoundType.CHAIN).noOcclusion(), BlockSetType.BIRCH));

    public static final RegistryObject<Block> EXPLOSIVE_CHARGE  = registerBlock("explosive_charge",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> CONCRETE_FAN  = registerBlock("concrete_fan",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> CONCRETE_MARKED  = registerBlock("concrete_marked",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> BRICK_CONCRETE_BROKEN = registerBlock("brick_concrete_broken",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> BRICK_CONCRETE_BROKEN_STAIRS = registerBlock("brick_concrete_broken_stairs",
            () -> new StairBlock(() -> ModBlocks.BRICK_CONCRETE_BROKEN.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));
    public static final RegistryObject<Block> BRICK_CONCRETE_BROKEN_SLAB = registerBlock("brick_concrete_broken_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));

    public static final RegistryObject<Block> BRICK_CONCRETE_CRACKED = registerBlock("brick_concrete_cracked",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> BRICK_CONCRETE_CRACKED_STAIRS = registerBlock("brick_concrete_cracked_stairs",
            () -> new StairBlock(() -> ModBlocks.BRICK_CONCRETE_CRACKED.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));
    public static final RegistryObject<Block> BRICK_CONCRETE_CRACKED_SLAB = registerBlock("brick_concrete_cracked_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));

    public static final RegistryObject<Block> BRICK_CONCRETE_MOSSY = registerBlock("brick_concrete_mossy",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> BRICK_CONCRETE_MOSSY_STAIRS = registerBlock("brick_concrete_mossy_stairs",
            () -> new StairBlock(() -> ModBlocks.BRICK_CONCRETE_MOSSY.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));
    public static final RegistryObject<Block> BRICK_CONCRETE_MOSSY_SLAB = registerBlock("brick_concrete_mossy_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));

    public static final RegistryObject<Block> BRICK_CONCRETE_MARKED = registerBlock("brick_concrete_marked",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> REINFORCED_STONE_STAIRS = registerBlock("reinforced_stone_stairs",
            () -> new StairBlock(() -> ModBlocks.REINFORCED_STONE.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));
    public static final RegistryObject<Block> REINFORCED_STONE_SLAB = registerBlock("reinforced_stone_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));

    // -----------------------<РАСТЕНИЯ>-----------------------------
    public static final RegistryObject<Block> STRAWBERRY_BUSH = registerBlock("strawberry_bush",
            () -> new FlowerBlock(() -> MobEffects.LUCK, 5,
                    BlockBehaviour.Properties.copy(Blocks.ALLIUM).noOcclusion().noCollission()));


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

    public static final RegistryObject<Block> COBALT_ORE_DEEPSLATE = registerBlock("cobalt_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> CINNABAR_ORE_DEEPSLATE = registerBlock("cinnabar_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    // ДВЕРИ
    
    public static final RegistryObject<DoorBlock> LARGE_VEHICLE_DOOR =
    registerBlockWithoutItem("large_vehicle_door",
        () -> new DoorBlock(
            BlockBehaviour.Properties.of()
                .strength(10.0F, 1000.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .noOcclusion(),
            DoorDecl.LARGE_VEHICLE_DOOR
        ));
    
    // ==================== Helper Methods ====================
    
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