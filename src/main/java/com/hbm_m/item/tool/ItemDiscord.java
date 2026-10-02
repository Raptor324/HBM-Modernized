package com.hbm_m.item.tool;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.item.ITooltipProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code com.hbm.items.tool.ItemDiscord} ({@code rod_of_discord}): teleportiert bis 100 Bloecke zum angeblickten Block. */
public class ItemDiscord extends Item implements ITooltipProvider {

    public ItemDiscord(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Library.rayTrace(player, 100, 1)
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(100));
        BlockHitResult pos = world.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        if (pos.getType() == HitResult.Type.BLOCK) {

            if (!world.isClientSide) {

                if (player.isPassenger())
                    player.stopRiding();

                Direction dir = pos.getDirection();
                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.teleportTo(pos.getLocation().x + dir.getStepX(), pos.getLocation().y + dir.getStepY() - 1, pos.getLocation().z + dir.getStepZ());
                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.fallDistance = 0.0F;
            }

            for (int i = 0; i < 32; ++i)
                world.addParticle(ParticleTypes.PORTAL, player.getX(), player.getY() + player.getRandom().nextDouble() * 2.0D, player.getZ(), player.getRandom().nextGaussian(), 0.0D, player.getRandom().nextGaussian());
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHbmTooltip(ItemStack itemstack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("I've seen the Rod of Discord and honestly").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        list.add(Component.literal("it's not as amazing as people say.").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        list.add(Component.literal(""));
        list.add(Component.literal("Rod of Discord is crucial in so many boss fights.").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        list.add(Component.literal("Imagine getting coiled by worm bosses.").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        list.add(Component.literal(""));
        list.add(Component.literal("Oh, you mean the Terraria item.").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        list.add(Component.literal("Idk about that thing.").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
    }
}
