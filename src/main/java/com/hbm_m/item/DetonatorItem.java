package com.hbm_m.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm_m.block.IDetonatable;

public class DetonatorItem extends Item {

    private static final String NBT_POS_X = "DetPosX";
    private static final String NBT_POS_Y = "DetPosY";
    private static final String NBT_POS_Z = "DetPosZ";
    private static final String NBT_HAS_TARGET = "HasTarget";

    public DetonatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) {
            return InteractionResult.PASS;
        }

        // Если игрок присел - сохраняем позицию
        if (player.isCrouching()) {
            if (!stack.hasTag()) {
                stack.setTag(new CompoundTag());
            }

            CompoundTag nbt = stack.getTag();
            nbt.putInt(NBT_POS_X, pos.getX());
            nbt.putInt(NBT_POS_Y, pos.getY());
            nbt.putInt(NBT_POS_Z, pos.getZ());
            nbt.putBoolean(NBT_HAS_TARGET, true);

            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.literal("Позиция сохранена: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                                .withStyle(ChatFormatting.GREEN),
                        true
                );
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Если игрок НЕ присел - активируем сохраненную позицию
        if (!player.isCrouching()) {
            if (!stack.hasTag()) {
                if (!level.isClientSide) {
                    player.displayClientMessage(
                            Component.literal("Нет сохраненной позиции!")
                                    .withStyle(ChatFormatting.RED),
                            true
                    );
                }
                return InteractionResultHolder.fail(stack);
            }

            CompoundTag nbt = stack.getTag();

            if (!nbt.contains(NBT_HAS_TARGET) || !nbt.getBoolean(NBT_HAS_TARGET)) {
                if (!level.isClientSide) {
                    player.displayClientMessage(
                            Component.literal("Нет сохраненной позиции!")
                                    .withStyle(ChatFormatting.RED),
                            true
                    );
                }
                return InteractionResultHolder.fail(stack);
            }

            int x = nbt.getInt(NBT_POS_X);
            int y = nbt.getInt(NBT_POS_Y);
            int z = nbt.getInt(NBT_POS_Z);
            BlockPos targetPos = new BlockPos(x, y, z);

            if (!level.isClientSide) {
                // Проверяем, загружен ли чанк
                if (!level.isLoaded(targetPos)) {
                    player.displayClientMessage(
                            Component.literal("Позиция не совместима или не прогружена")
                                    .withStyle(ChatFormatting.RED),
                            true
                    );
                    return InteractionResultHolder.fail(stack);
                }

                BlockState state = level.getBlockState(targetPos);
                Block block = state.getBlock();

                // Проверяем, поддерживает ли блок детонацию
                if (block instanceof IDetonatable) {
                    IDetonatable detonatable = (IDetonatable) block;
                    boolean success = detonatable.onDetonate(level, targetPos, state, player);

                    if (success) {
                        player.displayClientMessage(
                                Component.literal("Успешно активировано")
                                        .withStyle(ChatFormatting.GREEN),
                                true
                        );
                        return InteractionResultHolder.success(stack);
                    } else {
                        player.displayClientMessage(
                                Component.literal("Позиция не совместима или не прогружена")
                                        .withStyle(ChatFormatting.RED),
                                true
                        );
                        return InteractionResultHolder.fail(stack);
                    }
                } else {
                    player.displayClientMessage(
                            Component.literal("Позиция не совместима или не прогружена")
                                    .withStyle(ChatFormatting.RED),
                            true
                    );
                    return InteractionResultHolder.fail(stack);
                }
            }

            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }
}