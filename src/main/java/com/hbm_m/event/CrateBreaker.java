package com.hbm_m.event;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.ModSounds;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class CrateBreaker {

    private static final Random RANDOM = new Random();

    private static final List<RegistrySupplier<Block>> BREAKABLE_CRATES = List.of(
            ModBlocks.CRATE,
            ModBlocks.CRATE_CONSERVE,
            ModBlocks.CRATE_LEAD,
            ModBlocks.CRATE_WEAPON,
            ModBlocks.CRATE_METAL
    );

    private static final List<RegistrySupplier<net.minecraft.sounds.SoundEvent>> CRACK_SOUNDS = List.of(
            ModSounds.CRATEBREAK1,
            ModSounds.CRATEBREAK2,
            ModSounds.CRATEBREAK3,
            ModSounds.CRATEBREAK4,
            ModSounds.CRATEBREAK5
    );

    // Дроп с шансом для каждого ящика: список (предмет, шанс выпадения)
    private static final Map<RegistrySupplier<Block>, List<DropChance>> CRATE_DROPS = Map.of(
            ModBlocks.CRATE, List.of(
                    new DropChance(ModBlocks.CRT_BROKEN, 0.1),
                    new DropChance(ModBlocks.BARREL_CORRODED, 0.1),
                    new DropChance(ModBlocks.BARREL_IRON, 0.1),
                    new DropChance(ModBlocks.TAPE_RECORDER, 0.1),
                    new DropChance(ModBlocks.TOASTER, 0.1),
                    new DropChance(ModBlocks.FILE_CABINET, 0.1),
                    new DropChance(ModBlocks.CRT_CLEAN, 0.1),
                    new DropChance(ModBlocks.CRT_BSOD, 0.1),
                    new DropChance(ModBlocks.B29, 0.01),
                    new DropChance(ModBlocks.DORNIER, 0.01),

                    // Новые блоки из MainRegistry, по 10 штук каждый
                    new DropChance(ModBlocks.FLOOD_LAMP, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_ASBESTOS, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_COLORED_SAND, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_BLACK, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_BLUE, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_BROWN, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_COLORED_INDIGO, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_COLORED_PINK, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_COLORED_PURPLE, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_CYAN, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_GRAY, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_GREEN, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_LIGHT_BLUE, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_LIME, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_MAGENTA, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_ORANGE, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_PINK, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_PURPLE, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_RED, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_YELLOW, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_HAZARD, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_SILVER, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_WHITE, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_SUPER, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_SUPER_M0, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_SUPER_M1, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_SUPER_M2, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_SUPER_M3, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_SUPER_BROKEN, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_REBAR, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_REBAR_ALT, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_FLAT, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_TILE, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_VENT, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_FAN, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_TILE_TREFOIL, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_MOSSY, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_CRACKED, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_MARKED, 0.1, 10),
                    new DropChance(ModBlocks.BRICK_CONCRETE, 0.1, 10),
                    new DropChance(ModBlocks.BRICK_CONCRETE_MOSSY, 0.1, 10),
                    new DropChance(ModBlocks.BRICK_CONCRETE_CRACKED, 0.1, 10),
                    new DropChance(ModBlocks.BRICK_CONCRETE_BROKEN, 0.1, 10),
                    new DropChance(ModBlocks.BRICK_CONCRETE_MARKED, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_PILLAR, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_COLORED_MACHINE, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_COLORED_MACHINE_STRIPE, 0.1, 10),
                    new DropChance(ModBlocks.CONCRETE_COLORED_BRONZE, 0.1, 10),
                    new DropChance(ModBlocks.METEOR_POLISHED, 0.1, 10),
                    new DropChance(ModBlocks.METEOR_BRICK, 0.1, 10),
                    new DropChance(ModBlocks.METEOR_BRICK_CRACKED, 0.1, 10),
                    new DropChance(ModBlocks.METEOR_BRICK_MOSSY, 0.1, 10),
                    new DropChance(ModBlocks.METEOR_BRICK_CHISELED, 0.1, 10),
                    new DropChance(ModBlocks.METEOR_PILLAR, 0.1, 10),
                    new DropChance(ModBlocks.DEPTH_BRICK, 0.1, 10),
                    new DropChance(ModBlocks.DEPTH_TILES, 0.1, 10),
                    new DropChance(ModBlocks.DEPTH_NETHER_BRICK, 0.1, 10),
                    new DropChance(ModBlocks.DEPTH_NETHER_TILES, 0.1, 10),
                    new DropChance(ModBlocks.GNEISS_TILE, 0.1, 10),
                    new DropChance(ModBlocks.GNEISS_BRICK, 0.1, 10),
                    new DropChance(ModBlocks.GNEISS_CHISELED, 0.1, 10),
                    new DropChance(ModBlocks.BRICK_BASE, 0.1, 10),
                    new DropChance(ModBlocks.BRICK_LIGHT, 0.1, 10),
                    new DropChance(ModBlocks.BARRICADE, 0.1, 10),
                    new DropChance(ModBlocks.BRICK_FIRE, 0.1, 10),
                    new DropChance(ModBlocks.BRICK_OBSIDIAN, 0.1, 10),
                    new DropChance(ModBlocks.VINYL_TILE, 0.1, 10),
                    new DropChance(ModBlocks.VINYL_TILE_SMALL, 0.1, 10),
                    new DropChance(ModBlocks.REINFORCED_STONE, 0.1, 10),
                    new DropChance(ModBlocks.BRICK_DUCRETE, 0.1, 10),
                    new DropChance(ModBlocks.ASPHALT, 0.1, 10),
                    new DropChance(ModBlocks.BASALT_POLISHED, 0.1, 10),
                    new DropChance(ModBlocks.BASALT_BRICK, 0.1, 10),

                    new DropChance(ModItems.RADAWAY, 0.1)
            ),
            ModBlocks.CRATE_METAL, List.of(
                    new DropChance(ModItems.PCB, 0.4),
                    new DropChance(ModItems.VACUUM_TUBE, 0.4),
                    new DropChance(ModItems.CAPACITOR, 0.4),
                    new DropChance(ModItems.SILICON_CIRCUIT, 0.4),
                    new DropChance(ModItems.MICROCHIP, 0.2),
                    new DropChance(ModItems.CRT_DISPLAY, 0.4),
                    new DropChance(ModItems.INSULATOR, 0.4),
                    new DropChance(ModItems.ANALOG_CIRCUIT, 0.1),
                    new DropChance(ModItems.CAPACITOR_BOARD, 0.1),
                    new DropChance(ModItems.INTEGRATED_CIRCUIT, 0.1),
                    new DropChance(ModItems.ADVANCED_CIRCUIT, 0.01),
                    new DropChance(ModItems.MOTOR_DESH, 0.1),
                    new DropChance(ModItems.MOTOR, 0.2)
            ),
            ModBlocks.CRATE_WEAPON, List.of(
                    new DropChance(ModItems.DETONATOR, 0.05),
                    new DropChance(ModItems.RADAWAY, 0.1),
                    new DropChance(ModItems.GRENADE, 0.3),
                    new DropChance(ModItems.GRENADEFIRE, 0.3),
                    new DropChance(ModItems.GRENADESLIME, 0.3),
                    new DropChance(ModItems.GRENADE_IF, 0.3),
                    new DropChance(ModItems.GRENADE_IF_FIRE, 0.3),
                    new DropChance(ModItems.GRENADE_IF_SLIME, 0.3),
                    new DropChance(ModItems.GRENADE_IF_HE, 0.3),
                    new DropChance(ModItems.GRENADESMART, 0.2),
                    new DropChance(ModBlocks.DET_MINER, 0.3),
                    new DropChance(ModBlocks.EXPLOSIVE_CHARGE, 0.2),
                    new DropChance(ModItems.RANGE_DETONATOR, 0.01),
                    new DropChance(ModItems.MULTI_DETONATOR, 0.01),
                    new DropChance(ModBlocks.C4, 0.1),
                    new DropChance(ModBlocks.SMOKE_BOMB, 0.1),
                    new DropChance(ModBlocks.GIGA_DET, 0.1),
                    new DropChance(ModBlocks.WASTE_CHARGE, 0.1),
                    new DropChance(ModItems.LIQUIDATOR_BOOTS, 0.05),
                    new DropChance(ModItems.LIQUIDATOR_CHESTPLATE, 0.05),
                    new DropChance(ModItems.LIQUIDATOR_HELMET, 0.05),
                    new DropChance(ModItems.LIQUIDATOR_LEGGINGS, 0.05),
                    new DropChance(ModItems.STARMETAL_PICKAXE, 0.05),
                    new DropChance(ModItems.STARMETAL_SWORD, 0.05),
                    new DropChance(ModItems.STARMETAL_HELMET, 0.05),
                    new DropChance(ModItems.STARMETAL_LEGGINGS, 0.05),
                    new DropChance(ModItems.STARMETAL_CHESTPLATE, 0.05),
                    new DropChance(ModItems.STARMETAL_BOOTS, 0.05),
                    new DropChance(ModItems.STARMETAL_SHOVEL, 0.05),
                    new DropChance(ModItems.STARMETAL_HOE, 0.1),
                    new DropChance(ModBlocks.NUCLEAR_CHARGE, 0.001)
            ),
            ModBlocks.CRATE_LEAD, List.of(
                    new DropChance(ModItems.BLADE_TEST, 0.01),
                    new DropChance(ModItems.BLADE_ALLOY, 0.05),
                    new DropChance(ModBlocks.PRESS, 0.05),
                    new DropChance(ModItems.BLADE_STEEL, 0.2),
                    new DropChance(ModItems.BLADE_TITANIUM, 0.2),
                    new DropChance(ModItems.RADAWAY, 0.2),
                    new DropChance(ModItems.STAMP_OBSIDIAN_FLAT, 0.05),
                    new DropChance(ModItems.STAMP_STEEL_FLAT, 0.1),
                    new DropChance(ModItems.STAMP_IRON_FLAT, 0.2),
                    new DropChance(ModItems.BLUEPRINT_FOLDER, 0.1),
                    new DropChance(ModBlocks.BLAST_FURNACE_EXTENSION, 0.05),
                    new DropChance(ModBlocks.FREAKY_ALIEN_BLOCK, 0.1),
                    new DropChance(ModItems.STAMP_DESH_FLAT, 0.01),
                    new DropChance(ModItems.DEFUSER, 0.05),
                    new DropChance(ModItems.RUBBER_CLADDING, 0.05),
                    new DropChance(ModItems.LEAD_CLADDING, 0.05),
                    new DropChance(ModItems.PAINT_CLADDING, 0.1),
                    new DropChance(ModBlocks.ANVIL_DNT, 0.001),
                    new DropChance(ModBlocks.ANVIL_DESH, 0.01),
                    new DropChance(ModBlocks.ANVIL_STEEL, 0.05),
                    new DropChance(ModBlocks.ANVIL_IRON, 0.05),
                    new DropChance(ModItems.OIL_DETECTOR, 0.05)
            ),
            ModBlocks.CRATE_CONSERVE, List.of(
                    new DropChance(ModItems.CANNED_ASBESTOS, 0.1),
                    new DropChance(ModItems.RADAWAY, 0.1),
                    new DropChance(ModItems.CANNED_ASS, 0.1),
                    new DropChance(ModItems.CANNED_BARK, 0.1),
                    new DropChance(ModItems.CANNED_BEEF, 0.1),
                    new DropChance(ModItems.CANNED_BHOLE, 0.1),
                    new DropChance(ModItems.CANNED_CHEESE, 0.1),
                    new DropChance(ModItems.CANNED_CHINESE, 0.1),
                    new DropChance(ModItems.CANNED_DIESEL, 0.1),
                    new DropChance(ModItems.CANNED_FIST, 0.1),
                    new DropChance(ModItems.CANNED_FRIED, 0.1),
                    new DropChance(ModItems.CANNED_HOTDOGS, 0.1),
                    new DropChance(ModItems.CANNED_JIZZ, 0.1),
                    new DropChance(ModItems.CANNED_KEROSENE, 0.1),
                    new DropChance(ModItems.CANNED_LEFTOVERS, 0.1),
                    new DropChance(ModItems.CANNED_MILK, 0.1),
                    new DropChance(ModItems.CANNED_MYSTERY, 0.1),
                    new DropChance(ModItems.CANNED_NAPALM, 0.1),
                    new DropChance(ModItems.CANNED_OIL, 0.1),
                    new DropChance(ModItems.CANNED_PASHTET, 0.1),
                    new DropChance(ModItems.CANNED_PIZZA, 0.1),
                    new DropChance(ModItems.CANNED_RECURSION, 0.1),
                    new DropChance(ModItems.CANNED_SPAM, 0.1),
                    new DropChance(ModItems.CANNED_STEW, 0.1),
                    new DropChance(ModItems.CANNED_TOMATO, 0.1),
                    new DropChance(ModItems.CANNED_TUNA, 0.1),
                    new DropChance(ModItems.CANNED_TUBE, 0.1),
                    new DropChance(ModItems.CANNED_YOGURT, 0.1),

                    new DropChance(ModItems.CAN_BEPIS, 0.1),
                    new DropChance(ModItems.CAN_BREEN, 0.1),
                    new DropChance(ModItems.CAN_CREATURE, 0.1),
                    new DropChance(ModItems.CAN_EMPTY, 0.1),
                    new DropChance(ModItems.CAN_KEY, 0.1),
                    new DropChance(ModItems.CAN_LUNA, 0.1),
                    new DropChance(ModItems.CAN_MRSUGAR, 0.1),
                    new DropChance(ModItems.CAN_MUG, 0.1),
                    new DropChance(ModItems.CAN_OVERCHARGE, 0.1),
                    new DropChance(ModItems.CAN_REDBOMB, 0.1),
                    new DropChance(ModItems.CAN_SMART, 0.1)
            )

    );

    private record DropChance(RegistrySupplier<?> item, double chance, int count) { public DropChance(RegistrySupplier<?> item, double chance) {
        this(item, chance, 1); // если количество не указано, будет 1
    }
    }

    /**
     * Регистрация обработчика события.
     * Вызывается один раз при инициализации мода.
     */
    public static void init() {
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) -> {
            Level level = player.level();
            if (level.isClientSide) return EventResult.pass();

            ItemStack held = player.getItemInHand(hand);
            if (!held.is(ModItems.CROWBAR.get())) return EventResult.pass();

            Block block = level.getBlockState(pos).getBlock();

            RegistrySupplier<Block> matchedCrate = null;
            for (RegistrySupplier<Block> crate : BREAKABLE_CRATES) {
                Block b = crate.getOrNull();
                if (b != null && b == block) {
                    matchedCrate = crate;
                    break;
                }
            }
            if (matchedCrate == null) return EventResult.pass();

            // Анимация руки
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.swing(hand, true);
            }

            // Ломаем блок без ванильного дропа
            level.destroyBlock(pos, false);

            // Случайный звук треска
            RegistrySupplier<SoundEvent> soundObj = CRACK_SOUNDS.get(RANDOM.nextInt(CRACK_SOUNDS.size()));
            if (soundObj != null) {
                SoundEvent se = soundObj.get();
                if (se != null) {
                    level.playSound(null, pos, se, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }

            // Дропы для конкретного ящика
            List<DropChance> dropChances = CRATE_DROPS.get(matchedCrate);
            if (dropChances != null) {
                // 4 независимых ролла
                for (int i = 0; i < 4; i++) {
                    dropChances.stream()
                            .filter(dc -> RANDOM.nextDouble() <= dc.chance())
                            .findAny()
                            .ifPresent(dc -> spawnDrop(level, pos, dc));
                }
            }

            return EventResult.interruptTrue();
        });
    }

    private static void spawnDrop(Level level, BlockPos pos, DropChance dc) {
        var obj = dc.item().get();
        Item itemToDrop = null;

        if (obj instanceof Item it) {
            itemToDrop = it;
        } else if (obj instanceof Block bl) {
            itemToDrop = Item.byBlock(bl);
        }

        if (itemToDrop == null || itemToDrop == Items.AIR) return;

        int count = dc.count();

        ItemStack stack = new ItemStack(itemToDrop, count);
        ItemEntity dropEntity = new ItemEntity(
                level,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                stack
        );
        dropEntity.setDeltaMovement(
                (RANDOM.nextDouble() - 0.5) * 0.5,
                RANDOM.nextDouble() * 0.3 + 0.1,
                (RANDOM.nextDouble() - 0.5) * 0.5
        );
        level.addFreshEntity(dropEntity);
    }
}