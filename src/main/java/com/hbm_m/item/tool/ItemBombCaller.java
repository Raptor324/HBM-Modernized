package com.hbm_m.item.tool;

import com.hbm_m.entity.logic.EntityBomber;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 1:1 port of {@code ItemBombCaller} - right-click a point up to 500 blocks away and a bomber
 * flies in and levels it. The caller is consumed.
 *
 * <p>The original is one item with eight damage values; the port registers one item per loadout
 * instead, since 1.20 has no item damage variants for this sort of thing. Only the four
 * bomblet-carrying loadouts exist - the rest need entities that are not ported.</p>
 */
public class ItemBombCaller extends Item {

    /** The payload this caller brings in. */
    public enum Strike {
        CARPET("Carpet bombing"),
        NAPALM("Napalm"),
        CHLORINE("Poison gas"),
        ATOMIC("Atomic bomb");

        final String label;

        Strike(String label) {
            this.label = label;
        }
    }

    /** {@code Library.rayTrace(player, 500, 1)}: it can be called in from a long way off. */
    private static final double RANGE = 500D;

    private final Strike strike;

    public ItemBombCaller(Strike strike, Properties properties) {
        super(properties);
        this.strike = strike;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level,
                                                           @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        Vec3 eye = player.getEyePosition();
        Vec3 target = eye.add(player.getViewVector(1F).scale(RANGE));
        HitResult hit = level.clip(new ClipContext(eye, target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (!(hit instanceof BlockHitResult block) || hit.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            double x = block.getBlockPos().getX();
            double y = block.getBlockPos().getY();
            double z = block.getBlockPos().getZ();

            EntityBomber bomber = switch (this.strike) {
                case NAPALM   -> EntityBomber.napalm(level, x, y, z);
                case CHLORINE -> EntityBomber.chlorine(level, x, y, z);
                case ATOMIC   -> EntityBomber.aBomb(level, x, y, z);
                default       -> EntityBomber.carpet(level, x, y, z);
            };
            level.addFreshEntity(bomber);

            player.displayClientMessage(Component.literal("Called in airstrike!"), false);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.hbm_m.sound.ModSounds.TOOL_TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        if (!player.isCreative()) stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    //? if < 1.21.1 {
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
    //?} else {
    /*@Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
    *///?}
        tooltip.add(Component.literal("Type: " + this.strike.label).withStyle(ChatFormatting.GRAY));
    }
}
