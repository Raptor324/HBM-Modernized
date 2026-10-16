package com.hbm_m.item.grenades_and_activators;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.api.bomb.IBomb.BombReturnCode;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.interfaces.IDetonatable;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

public class DetonatorItem extends Item implements ITooltipProvider {

    public static final String NBT_POS_X = "DetPosX";
    public static final String NBT_POS_Y = "DetPosY";
    public static final String NBT_POS_Z = "DetPosZ";
    public static final String NBT_HAS_TARGET = "HasTarget";

    public DetonatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * Trigger detonation on a target block. Loads the target chunk safely on the server
     * using temporary chunk loading via PlatformHooks.
     * Supports both IBomb and IDetonatable blocks.
     */
    public static BombReturnCode triggerDetonation(Level level, BlockPos pos, @Nullable Player player) {
        if (!PlatformHooks.loadChunkTemporary(level, pos)) {
            return BombReturnCode.ERROR_NO_BOMB;
        }
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (level.getBlockEntity(pos) instanceof com.hbm_m.interfaces.IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                pos = controllerPos;
                if (!PlatformHooks.loadChunkTemporary(level, pos)) {
                    return BombReturnCode.ERROR_NO_BOMB;
                }
                state = level.getBlockState(pos);
                block = state.getBlock();
            }
        }
        if (block instanceof IBomb bomb) {
            return bomb.explode(level, pos);
        } else if (block instanceof IDetonatable detonatable) {
            boolean success = detonatable.onDetonate(level, pos, state, player);
            return success ? BombReturnCode.TRIGGERED : BombReturnCode.ERROR_INCOMPATIBLE;
        }
        return BombReturnCode.ERROR_NO_BOMB;
    }

    /**
     * Format a message with 1.7.10 style prefix [Detonator] (DARK_AQUA).
     */
    public static Component formatDetonatorMessage(Item item, Component message) {
        return Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.translatable(item.getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA))
                .append(message);
    }

    /**
     * Reads saved position supporting both 1.7.10 tags (x, y, z) and HBM-Modernized tags.
     */
    @Nullable
    public static BlockPos getSavedPosition(ItemStack stack) {
        if (!PlatformHooks.hasItemTag(stack)) {
            return null;
        }
        CompoundTag nbt = PlatformHooks.getItemTag(stack);
        if (nbt == null) {
            return null;
        }
        if (nbt.contains(NBT_HAS_TARGET) && !nbt.getBoolean(NBT_HAS_TARGET)) {
            return null;
        }
        if (nbt.contains("x") && nbt.contains("y") && nbt.contains("z")) {
            return new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
        }
        if (nbt.contains(NBT_POS_X) && nbt.contains(NBT_POS_Y) && nbt.contains(NBT_POS_Z)) {
            return new BlockPos(nbt.getInt(NBT_POS_X), nbt.getInt(NBT_POS_Y), nbt.getInt(NBT_POS_Z));
        }
        return null;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        BlockPos targetPos = getSavedPosition(stack);
        if (targetPos != null) {
            tooltip.add(Component.translatable("tooltip.hbm_m.detonator.target")
                    .append(Component.literal(targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ()))
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

        // Shift right-click saves position
        if (player.isCrouching()) {
            PlatformHooks.editItemTag(stack, nbt -> {
                // 1.7.10 tag format
                nbt.putInt("x", pos.getX());
                nbt.putInt("y", pos.getY());
                nbt.putInt("z", pos.getZ());
                // Backwards compatibility for existing Modernized saves
                nbt.putInt(NBT_POS_X, pos.getX());
                nbt.putInt(NBT_POS_Y, pos.getY());
                nbt.putInt(NBT_POS_Z, pos.getZ());
                nbt.putBoolean(NBT_HAS_TARGET, true);
            });

            if (!level.isClientSide) {
                player.sendSystemMessage(formatDetonatorMessage(this,
                        Component.translatable("desc.misc.posSet").withStyle(ChatFormatting.GREEN)));

                ModSounds.TOOL_TECH_BOOP.ifPresent(sound ->
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, player.getSoundSource(), 2.0F, 1.0F));
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        BlockPos targetPos = getSavedPosition(stack);
        if (targetPos == null) {
            if (!level.isClientSide) {
                player.sendSystemMessage(formatDetonatorMessage(this,
                        Component.translatable("desc.misc.noPos").withStyle(ChatFormatting.RED)));
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            BombReturnCode ret = triggerDetonation(level, targetPos, player);

            if (ret != BombReturnCode.ERROR_NO_BOMB && ret != BombReturnCode.UNDEFINED) {
                ModSounds.TOOL_TECH_BLEEP.ifPresent(sound ->
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, player.getSoundSource(), 1.0F, 1.0F));

                if (ModClothConfig.get().enableExtendedLogging) {
                    MainRegistry.LOGGER.info("[DET] Tried to detonate block at {} / {} / {} by {}!",
                            targetPos.getX(), targetPos.getY(), targetPos.getZ(), player.getName().getString());
                }

                player.sendSystemMessage(formatDetonatorMessage(this,
                        Component.translatable(ret.getUnlocalizedMessage())
                                .withStyle(ret.wasSuccessful() ? ChatFormatting.YELLOW : ChatFormatting.RED)));

                return InteractionResultHolder.success(stack);
            } else {
                player.sendSystemMessage(formatDetonatorMessage(this,
                        Component.translatable(BombReturnCode.ERROR_NO_BOMB.getUnlocalizedMessage())
                                .withStyle(ChatFormatting.RED)));

                return InteractionResultHolder.fail(stack);
            }
        }

        return InteractionResultHolder.success(stack);
    }
}