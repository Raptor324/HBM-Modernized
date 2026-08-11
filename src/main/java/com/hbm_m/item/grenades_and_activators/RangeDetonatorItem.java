package com.hbm_m.item.grenades_and_activators;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.interfaces.IDetonatable;
import com.hbm_m.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RangeDetonatorItem extends Item implements ITooltipProvider {

    private static final int MAX_RANGE = 256;

    public RangeDetonatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    //? if forge {
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.render.item.ItemRenderDetonatorLaser.INSTANCE;
            }
        });
    }
    //?}

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hitResult = (BlockHitResult) player.pick(MAX_RANGE, 1.0F, false);
        Vec3 target = hitResult.getType() == HitResult.Type.BLOCK
                ? Vec3.atCenterOf(hitResult.getBlockPos())
                : hitResult.getLocation();

        if (level.isClientSide) {
            spawnLaserBeam(level, player, target);
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

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

        return InteractionResultHolder.pass(stack);
    }

    /** Луч redstone dust — 1.7.10 {@code ItemLaserDetonator} / {@code reddust}, только на клиенте. */
    private static void spawnLaserBeam(Level level, Player player, Vec3 target) {
        Vec3 vec = new Vec3(
                target.x - player.getX(),
                target.y - player.getEyeY(),
                target.z - player.getZ());
        double len = Math.min(vec.length(), 15.0D);
        if (len < 1.0E-4D) {
            return;
        }
        vec = vec.scale(1.0D / len);

        DustParticleOptions dust = new DustParticleOptions(DustParticleOptions.REDSTONE_PARTICLE_COLOR, 1.0F);
        for (int i = 0; i < len; i++) {
            double rand = level.random.nextDouble() * len + 3.0D;
            level.addParticle(
                    dust,
                    player.getX() + vec.x * rand,
                    player.getEyeY() + vec.y * rand,
                    player.getZ() + vec.z * rand,
                    0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.range_detonator.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hbm_m.range_detonator.hint")
                .withStyle(ChatFormatting.GRAY));
    }
}
