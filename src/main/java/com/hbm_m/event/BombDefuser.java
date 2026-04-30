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

/**
 * Разминирование (дизарм) мин/блоков-бомб при клике дефьюзером.
 *
 * Мультилоадерная реализация на Architectury events (без Forge event bus).
 */
public class BombDefuser {

    private static final Random RANDOM = new Random();

    private static final List<RegistrySupplier<Block>> BOMBS = List.of(
            ModBlocks.MINE_AP,
            ModBlocks.MINE_FAT,
            ModBlocks.DUD_CONVENTIONAL,
            ModBlocks.DUD_SALTED,
            ModBlocks.DUD_NUKE
    );

    private static final List<RegistrySupplier<SoundEvent>> DEFUSE_SOUNDS = List.of(
            ModSounds.CLICK
    );

    private record DropAmount(RegistrySupplier<?> item, int amount) {}

    private static final Map<RegistrySupplier<Block>, List<DropAmount>> BOMB_DROPS = Map.of(
            ModBlocks.MINE_FAT, List.of(
                    new DropAmount(ModItems.BILLET_PLUTONIUM, 1),
                    new DropAmount(ModItems.PLATE_STEEL, 3),
                    new DropAmount(ModItems.BALL_TNT, 1)
            ),
            ModBlocks.MINE_AP, List.of(
                    new DropAmount(ModItems.PLATE_STEEL, 3),
                    new DropAmount(ModItems.BALL_TNT, 2)
            ),
            ModBlocks.DUD_CONVENTIONAL, List.of(
                    new DropAmount(ModItems.PLATE_STEEL, 8),
                    new DropAmount(ModItems.BALL_TNT, 16)
            ),
            ModBlocks.DUD_SALTED, List.of(
                    new DropAmount(ModItems.BILLET_PLUTONIUM, 2),
                    new DropAmount(ModItems.BALL_TNT, 8),
                    new DropAmount(ModItems.COBALT_RAW, 8)
            ),
            ModBlocks.DUD_NUKE, List.of(
                    new DropAmount(ModItems.BALL_TNT, 8),
                    new DropAmount(ModItems.BILLET_PLUTONIUM, 4)
            )
    );

    /**
     * Регистрация обработчика события.
     * Вызывается один раз при инициализации мода.
     */
    public static void init() {
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) -> {
            Level level = player.level();
            if (level.isClientSide) return EventResult.pass();

            ItemStack held = player.getItemInHand(hand);
            if (!held.is(ModItems.DEFUSER.get())) return EventResult.pass();

            RegistrySupplier<Block> matchedBomb = findBomb(level, pos);
            if (matchedBomb == null) return EventResult.pass();

            // Анимация руки
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.swing(hand, true);
            }

            // Ломаем блок без ванильного дропа
            level.destroyBlock(pos, false);

            // Звук
            playDefuseSound(level, pos);

            // Фиксированный дроп
            List<DropAmount> drops = BOMB_DROPS.get(matchedBomb);
            if (drops != null) {
                for (DropAmount drop : drops) {
                    spawnDrop(level, pos, drop);
                }
            }

            return EventResult.interruptTrue();
        });
    }

    private static RegistrySupplier<Block> findBomb(Level level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        for (RegistrySupplier<Block> bomb : BOMBS) {
            Block b = bomb.getOrNull();
            if (b != null && b == block) {
                return bomb;
            }
        }
        return null;
    }

    private static void playDefuseSound(Level level, BlockPos pos) {
        if (DEFUSE_SOUNDS.isEmpty()) return;
        RegistrySupplier<SoundEvent> soundObj = DEFUSE_SOUNDS.get(RANDOM.nextInt(DEFUSE_SOUNDS.size()));
        if (soundObj == null) return;
        SoundEvent se = soundObj.getOrNull();
        if (se == null) return;
        level.playSound(null, pos, se, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static void spawnDrop(Level level, BlockPos pos, DropAmount drop) {
        var obj = drop.item.get();
        Item itemToDrop = null;

        if (obj instanceof Item item) {
            itemToDrop = item;
        } else if (obj instanceof Block block) {
            itemToDrop = Item.byBlock(block);
        }

        if (itemToDrop == null || itemToDrop == Items.AIR) return;

        ItemStack dropStack = new ItemStack(itemToDrop, drop.amount);
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
