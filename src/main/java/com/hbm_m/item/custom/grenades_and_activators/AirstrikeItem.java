package com.hbm_m.item.custom.grenades_and_activators;

import com.hbm_m.entity.grenades.AirstrikeAgentEntity;
import com.hbm_m.entity.grenades.AirstrikeEntity;
import com.hbm_m.entity.grenades.AirstrikeHeavyEntity;
import com.hbm_m.entity.grenades.AirstrikeNukeEntity;
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

public class AirstrikeItem extends Item {

    private static final int MAX_RANGE = 256;

    public enum AirstrikeType {
        NORMAL("normal", AirstrikeEntity::new),
        HEAVY("heavy", AirstrikeHeavyEntity::new),
        AGENT("agent", AirstrikeAgentEntity::new),
        NUKE("nuke", AirstrikeNukeEntity::new);

        private final String name;
        private final AirstrikeEntityFactory factory;

        AirstrikeType(String name, AirstrikeEntityFactory factory) {
            this.name = name;
            this.factory = factory;
        }

        public String getName() {
            return name;
        }

        public AirstrikeEntityFactory getFactory() {
            return factory;
        }
    }

    @FunctionalInterface
    public interface AirstrikeEntityFactory {
        Object create(Level level, Player player, BlockPos pos);
    }

    private final AirstrikeType type;

    public AirstrikeItem(Properties properties, AirstrikeType type) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return type == AirstrikeType.NUKE;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
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
                            Component.translatable("message.hbm_m.airstrike.not_loaded").withStyle(ChatFormatting.RED),
                            true
                    );
                    return InteractionResultHolder.fail(stack);
                }

                // Создаем сущность самолета через фабрику
                Object airplane = type.getFactory().create(level, player, targetPos);
                if (airplane instanceof net.minecraft.world.entity.Entity entity) {
                    level.addFreshEntity(entity);
                }

                player.displayClientMessage(
                        Component.translatable("message.hbm_m.airstrike.called", targetPos.getX(), targetPos.getY(), targetPos.getZ())
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
                        Component.translatable("message.hbm_m.airstrike.no_target").withStyle(ChatFormatting.RED),
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
        tooltip.add(Component.translatable("tooltip.hbm_m.airstrike.common").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hbm_m.airstrike." + type.getName()).withStyle(ChatFormatting.GRAY));
    }

    protected static BlockHitResult getPlayerPOVHitResult(Level pLevel, Player pPlayer, ClipContext.Fluid pFluid) {
        return pLevel.clip(new ClipContext(
                pPlayer.getEyePosition(),
                pPlayer.getEyePosition().add(pPlayer.getViewVector(1.0F).scale(MAX_RANGE)),
                ClipContext.Block.OUTLINE, pFluid, pPlayer));
    }
}
