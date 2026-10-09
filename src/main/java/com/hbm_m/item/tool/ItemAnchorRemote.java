package com.hbm_m.item.tool;

import com.hbm_m.platform.StackNbt;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.fekal_electric.ModBatteryItem;
import com.hbm_m.util.BobMathUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.tool.ItemAnchorRemote}: Akku (1 MHE, 10 kHE/t Laden) - verknuepft sich mit einem
 * Teleportanker und teleportiert fuer 10 kHE dorthin (kleine Explosion am Ziel). Ohne NBT ist er wie im Original voll.
 */
public class ItemAnchorRemote extends ModBatteryItem {

    public ItemAnchorRemote(Properties properties) {
        super(properties, 1_000_000, 10_000, 0);
    }

    private long charge(ItemStack stack) {
        if (!StackNbt.has(stack) || !StackNbt.read(stack).contains("energy")) return getCapacity();
        return getEnergy(stack);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Energy stored: " + BobMathUtil.getShortNumber(charge(stack)) + "/" + BobMathUtil.getShortNumber(getCapacity()) + "HE"));
        list.add(Component.literal("Charge rate: " + BobMathUtil.getShortNumber(getMaxReceive()) + "HE/t"));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        if (ctx.getLevel().getBlockState(pos).is(ModBlocks.TELEANCHOR.get())) {
            ItemStack stack = ctx.getItemInHand();
            long charge = charge(stack);
            if (!StackNbt.has(stack)) StackNbt.set(stack, new CompoundTag());
            setEnergy(stack, charge);
            StackNbt.tag(stack).putInt("x", pos.getX());
            StackNbt.tag(stack).putInt("y", pos.getY());
            StackNbt.tag(stack).putInt("z", pos.getZ());
            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown() || world.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        if (!StackNbt.has(stack) || !StackNbt.read(stack).contains("x")) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, 0.75F);
            return InteractionResultHolder.pass(stack);
        }

        if (charge(stack) < 10_000) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, 0.75F);
            return InteractionResultHolder.pass(stack);
        }

        int x = StackNbt.read(stack).getInt("x");
        int y = StackNbt.read(stack).getInt("y");
        int z = StackNbt.read(stack).getInt("z");

        world.getChunk(x >> 4, z >> 4);

        if (world.getBlockState(new BlockPos(x, y, z)).is(ModBlocks.TELEANCHOR.get())) {

            if (player.isPassenger()) {
                player.stopRiding();
            }

            world.explode(player, x + 0.5, y + 1 + player.getBbHeight() / 2, z + 0.5, 2F, Level.ExplosionInteraction.NONE);
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.teleportTo(x + 0.5, y + 1, z + 0.5);
            player.fallDistance = 0.0F;

            if (world instanceof net.minecraft.server.level.ServerLevel server) {
                for (int i = 0; i < 32; ++i) {
                    server.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + player.getRandom().nextDouble() * 2.0D, player.getZ(), 1,
                            player.getRandom().nextGaussian(), 0.0D, player.getRandom().nextGaussian(), 0);
                }
            }

            setEnergy(stack, charge(stack) - 10_000);
        } else {
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, 0.75F);
        }

        return InteractionResultHolder.pass(stack);
    }
}
