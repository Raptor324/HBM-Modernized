package com.hbm_m.event;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModIngots;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Mod.EventBusSubscriber(modid = "hbm_m", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CrateBreaker {

    private static final Random RANDOM = new Random();

    private static final List<RegistryObject<Block>> BREAKABLE_CRATES = List.of(
            ModBlocks.CRATE,
            ModBlocks.CRATE_CONSERVE,
            ModBlocks.CRATE_LEAD,
            ModBlocks.CRATE_WEAPON,
            ModBlocks.CRATE_METAL
    );

    private static final List<RegistryObject<?>> CRACK_SOUNDS = List.of(
            ModSounds.CRATEBREAK1,
            ModSounds.CRATEBREAK2,
            ModSounds.CRATEBREAK3,
            ModSounds.CRATEBREAK4,
            ModSounds.CRATEBREAK5
    );

    // Дроп с шансом для каждого ящика: список (предмет, шанс выпадения)
    private static final Map<RegistryObject<Block>, List<DropChance>> CRATE_DROPS = Map.of(
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
                    new DropChance(ModItems.RADAWAY, 0.1)
            ),
            ModBlocks.CRATE_METAL, List.of(
                    new DropChance(ModItems.PCB, 0.4),
                    new DropChance(ModItems.VACUUM_TUBE, 0.4),
                    new DropChance(ModItems.CAPACITOR, 0.4),
                    new DropChance(ModItems.SILICON_CIRCUIT, 0.4),
                    new DropChance(ModItems.MICROCHIP, 0.2),
                    new DropChance(ModItems.ANALOG_CIRCUIT, 0.1),
                    new DropChance(ModItems.MOTOR_DESH, 0.1),
                    new DropChance(ModItems.MOTOR, 0.1)
            ),
            ModBlocks.CRATE_WEAPON, List.of(
                    new DropChance(ModItems.DETONATOR, 0.1),
                    new DropChance(ModItems.RADAWAY, 0.1),
                    new DropChance(ModItems.GRENADE, 0.2),
                    new DropChance(ModItems.GRENADEFIRE, 0.2),
                    new DropChance(ModItems.GRENADESLIME, 0.2),
                    new DropChance(ModItems.GRENADE_IF, 0.2),
                    new DropChance(ModItems.GRENADE_IF_FIRE, 0.2),
                    new DropChance(ModItems.GRENADE_IF_SLIME, 0.2),
                    new DropChance(ModItems.GRENADE_IF_HE, 0.2),
                    new DropChance(ModItems.GRENADESMART, 0.1),
                    new DropChance(ModBlocks.DET_MINER, 0.4),
                    new DropChance(ModBlocks.EXPLOSIVE_CHARGE, 0.1),
                    new DropChance(ModItems.RANGE_DETONATOR, 0.1),
                    new DropChance(ModItems.MULTI_DETONATOR, 0.1),
                    new DropChance(ModBlocks.C4, 0.1),
                    new DropChance(ModBlocks.SMOKE_BOMB, 0.1),
                    new DropChance(ModBlocks.NUCLEAR_CHARGE, 0.001)
            ),
            ModBlocks.CRATE_LEAD, List.of(
                    new DropChance(ModItems.BLADE_TEST, 0.1),
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
                    new DropChance(ModItems.PAINT_CLADDING, 0.05),
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

    private record DropChance(RegistryObject<?> item, double chance) {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;

        ItemStack held = player.getItemInHand(event.getHand());
        if (!held.is(ModItems.CROWBAR.get())) return;

        BlockPos pos = event.getPos();
        Block block = level.getBlockState(pos).getBlock();

        RegistryObject<Block> matchedCrate = null;
        for (RegistryObject<Block> crate : BREAKABLE_CRATES) {
            Block b = crate.orElse(null);
            if (b != null && b == block) {
                matchedCrate = crate;
                break;
            }
        }
        if (matchedCrate == null) return;

        event.setCanceled(true);

        // Отправляем анимацию руки на клиент
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.swing(event.getHand(), true);
        }

        level.destroyBlock(pos, false);

        RegistryObject<?> soundObj = CRACK_SOUNDS.get(RANDOM.nextInt(CRACK_SOUNDS.size()));
        if (soundObj != null) {
            var soundEvent = soundObj.get();
            if (soundEvent instanceof net.minecraft.sounds.SoundEvent se) {
                level.playSound(null, pos, se, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }

        List<DropChance> dropChances = CRATE_DROPS.get(matchedCrate);
        if (dropChances == null) return;

        // Выполняем 4 ролла для дропа с шансом
        for (int i = 0; i < 4; i++) {
            dropChances.stream()
                    .filter(dc -> RANDOM.nextDouble() <= dc.chance)
                    .findAny()
                    .ifPresent(dc -> dropItem(dc, level, pos));
        }
    }

    private static void dropItem(DropChance dc, Level level, BlockPos pos) {
        var obj = dc.item.get();
        Item itemToDrop = null;

        // Если это предмет напрямую
        if (obj instanceof Item item) {
            itemToDrop = item;
        }
        // Если это блок - конвертируем в item-версию
        else if (obj instanceof Block block) {
            itemToDrop = Item.byBlock(block);
        }

        if (itemToDrop != null && itemToDrop != Items.AIR) {
            ItemStack dropStack = new ItemStack(itemToDrop);
            ItemEntity dropEntity = new ItemEntity(level,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    dropStack);
            dropEntity.setDeltaMovement(
                    (RANDOM.nextDouble() - 0.5) * 0.5,
                    RANDOM.nextDouble() * 0.3 + 0.1,
                    (RANDOM.nextDouble() - 0.5) * 0.5);
            level.addFreshEntity(dropEntity);
        }
    }
}
