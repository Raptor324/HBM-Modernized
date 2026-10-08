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

    /** Original {@code Library.rayTrace(player, 500, 1)}. */
    private static final int MAX_RANGE = 500;

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
    //?} elif neoforge {
    /*@Override
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.render.item.ItemRenderDetonatorLaser.INSTANCE;
            }
        });
    }
    *///?}

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Original Library.rayTrace(player, 500, 1) mit mopOnMiss: auch ohne Treffer zaehlt der Block am Strahlende
        BlockHitResult hitResult = (BlockHitResult) player.pick(MAX_RANGE, 1.0F, false);
        BlockPos targetPos = hitResult.getBlockPos();

        if (!level.isClientSide) {
            BlockState state = level.getBlockState(targetPos);
            Block block = state.getBlock();

            if (block instanceof com.hbm_m.api.bomb.IBomb bomb) {
                com.hbm_m.api.bomb.IBomb.BombReturnCode ret = bomb.explode(level, targetPos);

                if (com.hbm_m.config.ModClothConfig.get().enableExtendedLogging)
                    com.hbm_m.main.MainRegistry.LOGGER.info("[DET] Tried to detonate block at " + targetPos.getX() + " / " + targetPos.getY() + " / " + targetPos.getZ() + " by " + player.getName().getString() + "!");

                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TOOL_TECH_BLEEP.get(), player.getSoundSource(), 1.0F, 1.0F);
                inform(player, ret.getUnlocalizedMessage(), ret.wasSuccessful());

            } else if (block instanceof IDetonatable detonatable) {
                // Port: Sprengkoerper, die im Port ueber IDetonatable statt IBomb laufen
                boolean success = detonatable.onDetonate(level, targetPos, state, player);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), success ? ModSounds.TOOL_TECH_BLEEP.get() : ModSounds.TOOL_TECH_BOOP.get(), player.getSoundSource(), 1.0F, 1.0F);
                inform(player, (success ? com.hbm_m.api.bomb.IBomb.BombReturnCode.DETONATED : com.hbm_m.api.bomb.IBomb.BombReturnCode.ERROR_INCOMPATIBLE).getUnlocalizedMessage(), success);

            } else {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TOOL_TECH_BOOP.get(), player.getSoundSource(), 1.0F, 1.0F);
                inform(player, com.hbm_m.api.bomb.IBomb.BombReturnCode.ERROR_NO_BOMB.getUnlocalizedMessage(), false);
            }
        } else {
            spawnLaserBeam(level, player, Vec3.atCenterOf(targetPos));
        }

        return InteractionResultHolder.pass(stack);
    }

    /** Original {@code PlayerInformPacket(..., ID_DETONATOR)}: gelb bei Erfolg, sonst rot. */
    private static void inform(Player player, String key, boolean success) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.hbm_m.network.InfoToastPacket.sendTo(sp, Component.translatable(key).withStyle(success ? ChatFormatting.YELLOW : ChatFormatting.RED),
                    60, com.hbm_m.client.overlay.OverlayInfoToast.ID_DETONATOR, success ? 0xFFFF55 : 0xFF5555);
        }
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
        vec = vec.normalize();

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
