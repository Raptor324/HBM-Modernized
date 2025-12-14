package com.hbm_m.item.custom.grenades_and_activators;

import com.hbm_m.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import com.hbm_m.block.custom.explosives.IDetonatable;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RangeDetonatorItem extends Item {

    private static final int MAX_RANGE = 256;

    public RangeDetonatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // Используем встроенную функцию игрока для трассировки луча
            BlockHitResult hitResult = (BlockHitResult) player.pick(MAX_RANGE, 1.0F, false);

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos targetPos = hitResult.getBlockPos();

                // Проверяем, загружен ли чанк
                if (!level.isLoaded(targetPos)) {
                    player.displayClientMessage(
                            Component.translatable("message.hbm_m.range_detonator.pos_not_loaded")
                                    .withStyle(ChatFormatting.RED),
                            true
                    );
                    return InteractionResultHolder.fail(stack);
                }

                // Спавним частицу (лазерная точка)
                spawnRedstoneParticles(level, targetPos);

                BlockState state = level.getBlockState(targetPos);
                Block block = state.getBlock();

                // Проверяем, поддерживает ли блок детонацию
                if (block instanceof IDetonatable detonatable) {
                    boolean success = detonatable.onDetonate(level, targetPos, state, player);

                    if (success) {
                        player.displayClientMessage(
                                Component.translatable("message.hbm_m.range_detonator.activated")
                                        .withStyle(ChatFormatting.GREEN),
                                true
                        );
                        if (ModSounds.TOOL_TECH_BLEEP.isPresent()) {
                            SoundEvent soundEvent = ModSounds.TOOL_TECH_BLEEP.get();
                            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                    soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                        }
                        return InteractionResultHolder.success(stack);
                    } else {
                        player.displayClientMessage(
                                Component.translatable("message.hbm_m.range_detonator.pos_not_loaded")
                                        .withStyle(ChatFormatting.RED),
                                true
                        );
                        if (ModSounds.TOOL_TECH_BOOP.isPresent()) {
                            SoundEvent soundEvent = ModSounds.TOOL_TECH_BOOP.get();
                            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                    soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                        }
                        return InteractionResultHolder.fail(stack);
                    }
                } else {
                    player.displayClientMessage(
                            Component.translatable("message.hbm_m.range_detonator.pos_not_loaded")
                                    .withStyle(ChatFormatting.RED),
                            true
                    );
                    if (ModSounds.TOOL_TECH_BOOP.isPresent()) {
                        SoundEvent soundEvent = ModSounds.TOOL_TECH_BOOP.get();
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                soundEvent, player.getSoundSource(), 1.0F, 1.0F);
                    }
                    return InteractionResultHolder.fail(stack);
                }
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    /**
     * Спавнит облако частиц красного камня на целевой позиции
     */
    private void spawnRedstoneParticles(Level level, BlockPos pos) {
        for (int i = 0; i < 8; i++) {
            double offsetX = (Math.random() - 0.5) * 0.2;
            double offsetY = (Math.random() - 0.5) * 0.2;
            double offsetZ = (Math.random() - 0.5) * 0.2;

            level.addParticle(
                    ParticleTypes.FLASH,
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() + 0.5 + offsetY,
                    pos.getZ() + 0.5 + offsetZ,
                    0.0, 0.0, 0.0
            );
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.hbm_m.range_detonator.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hbm_m.range_detonator.hint")
                .withStyle(ChatFormatting.GRAY));
    }
}
