package com.hbm_m.event;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Mod.EventBusSubscriber(modid = "hbm_m", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BombDefuser {

    private static final Random RANDOM = new Random();

    private static final List<RegistryObject<Block>> BOMBS = List.of(
            ModBlocks.MINE_AP,
            ModBlocks.MINE_FAT,
            ModBlocks.DUD_FUGAS_TONG,
            ModBlocks.DUD_SALTED,
            ModBlocks.DUD_NUKE
    );

    private static final List<RegistryObject<?>> DEFUSE_SOUNDS = List.of(
            ModSounds.CLICK
    );

    // Новый класс для фиксированного количества дропа
    private record DropAmount(RegistryObject<?> item, int amount) {}

    // Убираем "шансы", теперь фиксированное количество дропа для каждого типа
    private static final Map<RegistryObject<Block>, List<DropAmount>> BOMB_DROPS = Map.of(
            ModBlocks.MINE_FAT, List.of(
                    new DropAmount(ModItems.BILLET_PLUTONIUM, 1),
                    new DropAmount(ModItems.PLATE_STEEL, 3),
                    new DropAmount(ModItems.BALL_TNT, 1)
            ),
            ModBlocks.MINE_AP, List.of(
                    new DropAmount(ModItems.PLATE_STEEL, 3),
                    new DropAmount(ModItems.BALL_TNT, 2)
            ),
            ModBlocks.DUD_FUGAS_TONG, List.of(
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

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;

        ItemStack held = player.getItemInHand(event.getHand());
        if (!held.is(ModItems.DEFUSER.get())) return;

        BlockPos pos = event.getPos();
        Block block = level.getBlockState(pos).getBlock();

        RegistryObject<Block> matchedCrate = null;
        for (RegistryObject<Block> crate : BOMBS) {
            Block b = crate.orElse(null);
            if (b != null && b == block) {
                matchedCrate = crate;
                break;
            }
        }
        if (matchedCrate == null) return;

        event.setCanceled(true);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.swing(event.getHand(), true);
        }

        level.destroyBlock(pos, false);

        RegistryObject<?> soundObj = DEFUSE_SOUNDS.get(RANDOM.nextInt(DEFUSE_SOUNDS.size()));
        if (soundObj != null) {
            var soundEvent = soundObj.get();
            if (soundEvent instanceof net.minecraft.sounds.SoundEvent se) {
                level.playSound(null, pos, se, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }

        List<DropAmount> drops = BOMB_DROPS.get(matchedCrate);
        if (drops == null) return;

        // Для каждого предмета из списка создаём ровно amount предметов
        for (DropAmount drop : drops) {
            dropItem(drop, level, pos);
        }
    }

    private static void dropItem(DropAmount drop, Level level, BlockPos pos) {
        var obj = drop.item.get();
        Item itemToDrop = null;

        if (obj instanceof Item item) {
            itemToDrop = item;
        } else if (obj instanceof Block block) {
            itemToDrop = Item.byBlock(block);
        }

        if (itemToDrop != null && itemToDrop != Items.AIR) {
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
}
