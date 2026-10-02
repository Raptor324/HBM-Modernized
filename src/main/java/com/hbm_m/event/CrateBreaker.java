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

    /** Nur noch die Port-eigene Konservenkiste; die Original-Kisten hebeln sich 1:1 selbst auf (CrateBlocks). */
    private static final List<RegistrySupplier<Block>> BREAKABLE_CRATES = List.of(
            ModBlocks.CRATE_CONSERVE
    );

    private static final List<RegistrySupplier<net.minecraft.sounds.SoundEvent>> CRACK_SOUNDS = List.of(
            ModSounds.CRATEBREAK1,
            ModSounds.CRATEBREAK2,
            ModSounds.CRATEBREAK3,
            ModSounds.CRATEBREAK4,
            ModSounds.CRATEBREAK5
    );

    private static final Map<RegistrySupplier<Block>, List<DropChance>> CRATE_DROPS = Map.of();

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