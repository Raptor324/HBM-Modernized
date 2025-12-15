package com.hbm_m.item.custom.grenades_and_activators;

import com.hbm_m.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm_m.block.custom.explosives.IDetonatable;

import javax.annotation.Nullable;
import java.util.List;

public class DetonatorItem extends Item {

    private static final String NBT_POS_X = "DetPosX";
    private static final String NBT_POS_Y = "DetPosY";
    private static final String NBT_POS_Z = "DetPosZ";
    private static final String NBT_HAS_TARGET = "HasTarget";

    public DetonatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }


    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        if (stack.hasTag()) {
            CompoundTag nbt = stack.getTag();
            if (nbt != null && nbt.contains("HasTarget") && nbt.getBoolean("HasTarget")) {
                int x = nbt.getInt("DetPosX");
                int y = nbt.getInt("DetPosY");
                int z = nbt.getInt("DetPosZ");

                tooltip.add(Component.translatable("tooltip.hbm_m.detonator.target")
                        .append(Component.literal(x + ", " + y + ", " + z))
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

                tooltip.add(Component.translatable("tooltip.hbm_m.detonator.right_click")
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.hbm_m.detonator.shift_right_click")
                        .withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("tooltip.hbm_m.detonator.no_target")
                        .withStyle(ChatFormatting.RED));
                tooltip.add(Component.translatable("tooltip.hbm_m.detonator.shift_right_click")
                        .withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.hbm_m.detonator.no_target")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("tooltip.hbm_m.detonator.shift_right_click")
                    .withStyle(ChatFormatting.GRAY));
        }
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
                if (ModSounds.TOOL_TECH_BOOP.isPresent()) {
                    SoundEvent soundEvent = ModSounds.TOOL_TECH_BOOP.get();
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                }
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
                    if (ModSounds.TOOL_TECH_BLEEP.isPresent()) {
                        SoundEvent soundEvent = ModSounds.TOOL_TECH_BLEEP.get();
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                    }
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
                    if (ModSounds.TOOL_TECH_BLEEP.isPresent()) {
                        SoundEvent soundEvent = ModSounds.TOOL_TECH_BLEEP.get();
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                    }

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

                    if (ModSounds.TOOL_TECH_BLEEP.isPresent()) {
                        SoundEvent soundEvent = ModSounds.TOOL_TECH_BLEEP.get();
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                    }
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
                        if (ModSounds.TOOL_TECH_BLEEP.isPresent()) {
                            SoundEvent soundEvent = ModSounds.TOOL_TECH_BLEEP.get();
                            level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                        }

                        return InteractionResultHolder.success(stack);
                    } else {
                        player.displayClientMessage(
                                Component.literal("Позиция не совместима или не прогружена")
                                        .withStyle(ChatFormatting.RED),
                                true
                        );
                        if (ModSounds.TOOL_TECH_BLEEP.isPresent()) {
                            SoundEvent soundEvent = ModSounds.TOOL_TECH_BLEEP.get();
                            level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                        }

                        return InteractionResultHolder.fail(stack);
                    }


                } else {
                    player.displayClientMessage(
                            Component.literal("Позиция не совместима или не прогружена")
                                    .withStyle(ChatFormatting.RED),
                            true
                    );
                    if (ModSounds.TOOL_TECH_BLEEP.isPresent()) {
                        SoundEvent soundEvent = ModSounds.TOOL_TECH_BLEEP.get();
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                    }

                    return InteractionResultHolder.fail(stack);
                }
            }

            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }
}