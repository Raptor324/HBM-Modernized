package com.hbm_m.item.special;

import com.hbm_m.advancement.ModAdvancements;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 1:1 port of {@code ItemCigarette}, covering both the cigarette and the crackpipe.
 *
 * <p>A cigarette costs 2000 black lung, 2000 asbestos and 100 RAD. The crackpipe is cheaper on the
 * lungs, makes you dizzy, and heals ten. Both make you cough.</p>
 *
 * <p>Smoking a cigarette while wearing a No. 9 on your head is the original's {@code achNo9} - it
 * is a hat, not a fishing joke.</p>
 */
public class ItemCigarette extends Item {

    /** {@code getMaxItemUseDuration} is 30 ticks for both. */
    private static final int USE_TICKS = 30;

    private final boolean crackpipe;

    public ItemCigarette(boolean crackpipe, Properties properties) {
        super(properties);
        this.crackpipe = crackpipe;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        // EnumAction.bow in the original: a hold, not a chew.
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return USE_TICKS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level,
                                                           @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level,
                                              @NotNull LivingEntity entity) {
        if (!(entity instanceof Player player)) return stack;

        if (!player.isCreative()) stack.shrink(1);

        if (!level.isClientSide) {
            if (!crackpipe) {
                HbmLivingProps.incrementBlackLung(player, 2000);
                HbmLivingProps.incrementAsbestos(player, 2000);
                HbmLivingProps.incrementRadiation(player, 100F);

                if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.NO9.get())) {
                    ModAdvancements.grant(player, ModAdvancements.NO9);
                }
            } else {
                HbmLivingProps.incrementBlackLung(player, 500);
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
                player.heal(10F);
            }

            // The original plays hbm:player.cough and throws a burst of smoke; the port has no
            // cough sample, so this borrows the closest vanilla one.
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 1.0F, 1.0F);

            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SMOKE,
                        player.getX(), player.getEyeY(), player.getZ(),
                        30, 0.25D, 0.25D, 0.25D, 0.02D);
            }
        }

        return stack;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (!crackpipe) {
            tooltip.add(Component.literal("✓ Asbestos filter").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("✓ High in tar").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("✓ Tobacco contains 100% Polonium-210").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("✓ Yum").withStyle(ChatFormatting.RED));
        } else {
            // The original cycles the last word through eight colours on a two-second loop.
            ChatFormatting[] colors = {
                    ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN,
                    ChatFormatting.AQUA, ChatFormatting.BLUE, ChatFormatting.DARK_PURPLE, ChatFormatting.LIGHT_PURPLE
            };
            int len = 2000;
            int index = (int) (System.currentTimeMillis() % len * colors.length / len);
            tooltip.add(Component.literal("This can't be good for me, but I feel ")
                    .append(Component.literal("GREAT").withStyle(colors[index])));
        }
    }
}
