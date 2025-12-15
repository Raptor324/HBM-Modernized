package com.hbm_m.network;

import com.hbm_m.sound.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import com.hbm_m.item.custom.grenades_and_activators.MultiDetonatorItem;
import com.hbm_m.item.custom.grenades_and_activators.MultiDetonatorItem.PointData;
import com.hbm_m.block.custom.explosives.IDetonatable;
import java.util.function.Supplier;

public class DetonateAllPacket {

    private CompoundTag tag;

    public DetonateAllPacket(CompoundTag tag) {
        this.tag = tag;
    }

    public DetonateAllPacket() {
        this.tag = new CompoundTag();
    }

    public static void encode(DetonateAllPacket msg, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeNbt(msg.tag);
    }

    public static DetonateAllPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new DetonateAllPacket(buf.readNbt());
    }

    public static boolean handle(DetonateAllPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                handleDetonation(player);
            }
        });
        return true;
    }

    private static void handleDetonation(ServerPlayer player) {
        Level level = player.serverLevel();
        if (level == null) return;

        // Получаем предмет из руки
        net.minecraft.world.item.ItemStack mainItem = player.getMainHandItem();
        net.minecraft.world.item.ItemStack offItem = player.getOffhandItem();
        net.minecraft.world.item.ItemStack detonatorStack = net.minecraft.world.item.ItemStack.EMPTY;

        if (mainItem.getItem() instanceof MultiDetonatorItem) {
            detonatorStack = mainItem;
        } else if (offItem.getItem() instanceof MultiDetonatorItem) {
            detonatorStack = offItem;
        }

        if (detonatorStack.isEmpty()) {
            player.displayClientMessage(
                    Component.literal("Multi-Detonator не найден!").withStyle(ChatFormatting.RED),
                    false
            );
            return;
        }

        MultiDetonatorItem detonatorItem = (MultiDetonatorItem) detonatorStack.getItem();
        int successCount = 0;
        final int POINTS_COUNT = detonatorItem.getMaxPoints();

        for (int i = 0; i < POINTS_COUNT; i++) {
            PointData pointData = detonatorItem.getPointData(detonatorStack, i);
            if (pointData == null || !pointData.hasTarget) {
                continue;
            }

            BlockPos targetPos = new BlockPos(pointData.x, pointData.y, pointData.z);

            if (!level.isLoaded(targetPos)) {
                player.displayClientMessage(
                        Component.literal(pointData.name + " ❌ Позиция не загружена").withStyle(ChatFormatting.RED),
                        false
                );
                continue;
            }

            BlockState state = level.getBlockState(targetPos);
            Block block = state.getBlock();

            if (block instanceof IDetonatable) {
                IDetonatable detonatable = (IDetonatable) block;
                try {
                    boolean success = detonatable.onDetonate(level, targetPos, state, player);
                    if (success) {
                        player.displayClientMessage(
                                Component.literal(pointData.name + " Успешно активировано").withStyle(ChatFormatting.GREEN),
                                false
                        );
                        if (ModSounds.TOOL_TECH_BLEEP.isPresent()) {
                            SoundEvent soundEvent = ModSounds.TOOL_TECH_BLEEP.get();
                            level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                        }
                        successCount++;
                    } else {
                        player.displayClientMessage(
                                Component.literal(pointData.name + " Активация не удалась").withStyle(ChatFormatting.RED),
                                false
                        );
                    }
                } catch (Exception e) {
                    player.displayClientMessage(
                            Component.literal(pointData.name + " Ошибка при активации").withStyle(ChatFormatting.RED),
                            false
                    );
                    e.printStackTrace();
                }
            } else {
                player.displayClientMessage(
                        Component.literal(pointData.name + " Блок несовместим").withStyle(ChatFormatting.RED),
                        false
                );
            }
        }

        player.displayClientMessage(
                Component.literal("Успешно активировано: " + successCount + "/" + POINTS_COUNT)
                        .withStyle(successCount == POINTS_COUNT ? ChatFormatting.GREEN : ChatFormatting.YELLOW),
                false
        );
    }
}