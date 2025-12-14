package com.hbm_m.item.custom.grenades_and_activators;

import com.hbm_m.entity.grenades.AirstrikeAgentEntity;
import com.hbm_m.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AirstrikeAgentItem extends Item {

    private static final int MAX_RANGE = 256;

    public AirstrikeAgentItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos targetPos = hitResult.getBlockPos();

                if (!level.isLoaded(targetPos)) {
                    player.displayClientMessage(
                            Component.literal("Целевой чанк не загружен!").withStyle(ChatFormatting.RED),
                            true
                    );
                    return InteractionResultHolder.fail(stack);
                }

                // Создаем сущность самолета
                AirstrikeAgentEntity airplane = new AirstrikeAgentEntity(level, player, targetPos);
                level.addFreshEntity(airplane);

                player.displayClientMessage(
                        Component.literal("Авиаудар вызван на координатах: " + targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ())
                                .withStyle(ChatFormatting.GREEN),
                        true
                );

                ModSounds.TOOL_TECH_BLEEP.ifPresent(soundEvent ->
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                soundEvent, player.getSoundSource(), 1.0F, 1.0F)
                );

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());

            } else {
                player.displayClientMessage(
                        Component.literal("Нет целевого блока в видимости!").withStyle(ChatFormatting.RED),
                        true
                );
                return InteractionResultHolder.fail(stack);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("Вызывает авиаудар в целевую точку").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Химическое оружие 'Agent Orange'").withStyle(ChatFormatting.GRAY));
    }

    // Вспомогательный метод для трассировки луча, идентичный player.pick
    protected static BlockHitResult getPlayerPOVHitResult(Level pLevel, Player pPlayer, ClipContext.Fluid pFluid) {
        float f = pPlayer.getXRot();
        float f1 = pPlayer.getYRot();
        double d0 = pPlayer.getX();
        double d1 = pPlayer.getEyeY();
        double d2 = pPlayer.getZ();
        double d3 = MAX_RANGE;
        return pLevel.clip(new ClipContext(
                pPlayer.getEyePosition(),
                pPlayer.getEyePosition().add(pPlayer.getViewVector(1.0F).scale(d3)),
                ClipContext.Block.OUTLINE, pFluid, pPlayer));
    }
}