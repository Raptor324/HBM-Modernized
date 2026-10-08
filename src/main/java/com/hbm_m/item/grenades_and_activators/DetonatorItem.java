package com.hbm_m.item.grenades_and_activators;

import com.hbm_m.item.ITooltipProvider;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.interfaces.IDetonatable;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
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

    private static final String NBT_POS_X = "DetPosX";
    private static final String NBT_POS_Y = "DetPosY";
    private static final String NBT_POS_Z = "DetPosZ";
    private static final String NBT_HAS_TARGET = "HasTarget";

    public DetonatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }


    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // 1:1 Original ItemDetonator.addInformation (NBT-Schluessel des Ports)
        tooltip.add(com.hbm_m.util.TooltipUtil.gray("Shift right-click to set position,"));
        tooltip.add(com.hbm_m.util.TooltipUtil.gray("right-click to detonate!"));
        CompoundTag nbt = PlatformHooks.hasItemTag(stack) ? PlatformHooks.getItemTag(stack) : null;
        if (nbt == null || !nbt.getBoolean("HasTarget")) {
            tooltip.add(Component.literal("No position set!").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal("Linked to " + nbt.getInt("DetPosX") + ", " + nbt.getInt("DetPosY") + ", " + nbt.getInt("DetPosZ")).withStyle(ChatFormatting.YELLOW));
        }
    }





    /** Original "[Name] "-Praefix in Dunkeltuerkis. */
    private Component prefix() {
        return Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA));
    }

    /** 1:1 Original onItemUse: Schleich-Rechtsklick auf einen Block merkt die Position. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player != null && player.isShiftKeyDown()) {
            PlatformHooks.editItemTag(stack, nbt -> {
                nbt.putInt(NBT_POS_X, pos.getX());
                nbt.putInt(NBT_POS_Y, pos.getY());
                nbt.putInt(NBT_POS_Z, pos.getZ());
                nbt.putBoolean(NBT_HAS_TARGET, true);
            });

            if (!level.isClientSide) {
                player.sendSystemMessage(prefix().copy().append(Component.literal("Position set!").withStyle(ChatFormatting.GREEN)));
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TOOL_TECH_BOOP.get(), player.getSoundSource(), 2.0F, 1.0F);

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    /** 1:1 Original onItemRightClick: zuendet die gemerkte Bombe ({@code IBomb}), Port-Sprengkoerper ueber {@link IDetonatable}. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag nbt = PlatformHooks.hasItemTag(stack) ? PlatformHooks.getItemTag(stack) : null;

        if (nbt == null || !nbt.getBoolean(NBT_HAS_TARGET)) {
            if (!level.isClientSide) {
                player.sendSystemMessage(prefix().copy().append(Component.literal("No position set!").withStyle(ChatFormatting.RED)));
            }
        } else {
            int x = nbt.getInt(NBT_POS_X);
            int y = nbt.getInt(NBT_POS_Y);
            int z = nbt.getInt(NBT_POS_Z);
            BlockPos targetPos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(targetPos);
            Block block = state.getBlock();

            if (block instanceof com.hbm_m.api.bomb.IBomb || block instanceof IDetonatable) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TOOL_TECH_BLEEP.get(), player.getSoundSource(), 1.0F, 1.0F);
                if (!level.isClientSide) {
                    com.hbm_m.api.bomb.IBomb.BombReturnCode ret;
                    if (block instanceof com.hbm_m.api.bomb.IBomb bomb) {
                        ret = bomb.explode(level, targetPos);
                    } else {
                        ret = ((IDetonatable) block).onDetonate(level, targetPos, state, player)
                                ? com.hbm_m.api.bomb.IBomb.BombReturnCode.DETONATED : com.hbm_m.api.bomb.IBomb.BombReturnCode.ERROR_INCOMPATIBLE;
                    }

                    if (com.hbm_m.config.ModClothConfig.get().enableExtendedLogging)
                        com.hbm_m.main.MainRegistry.LOGGER.info("[DET] Tried to detonate block at " + x + " / " + y + " / " + z + " by " + player.getName().getString() + "!");

                    player.sendSystemMessage(prefix().copy().append(Component.translatable(ret.getUnlocalizedMessage())
                            .withStyle(ret.wasSuccessful() ? ChatFormatting.YELLOW : ChatFormatting.RED)));
                }

            } else {
                if (!level.isClientSide) {
                    player.sendSystemMessage(prefix().copy().append(Component.translatable(com.hbm_m.api.bomb.IBomb.BombReturnCode.ERROR_NO_BOMB.getUnlocalizedMessage())
                            .withStyle(ChatFormatting.RED)));
                }
            }
        }

        return InteractionResultHolder.pass(stack);
    }
}
