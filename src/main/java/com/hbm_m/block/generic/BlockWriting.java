package com.hbm_m.block.generic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@link com.hbm.blocks.generic.BlockWriting} ({@code brick_concrete_marked}): Rechtsklick (nicht schleichend)
 * zeigt die Warnbotschaft fuer Atommuell-Endlager (WIPP-Text) in Rot.
 */
public class BlockWriting extends Block {

    public BlockWriting(Properties properties) {
        super(properties);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    //?} else {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack hbmHeld, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return com.hbm_m.platform.BlockUseHooks.item(hbmUse(state, world, pos, player, hand, hit));
    }
    private InteractionResult hbmUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    *///?}
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        } else if (!player.isShiftKeyDown()) {
            String[] lines = {
                    "You should not have come here.",
                    "This is not a place of honor. No great deed is commemorated here.",
                    "Nothing of value is here.",
                    "What is here is dangerous and repulsive.",
                    "We considered ourselves a powerful culture. We harnessed the hidden fire, and used it for our own purposes.",
                    "Then we saw the fire could burn within living things, unnoticed until it destroyed them.",
                    "And we were afraid.",
                    "We built great tombs to hold the fire for one hundred thousand years, after which it would no longer kill.",
                    "If this place is opened, the fire will not be isolated from the world, and we will have failed to protect you.",
                    "Leave this place and never come back."
            };
            for (String l : lines) player.sendSystemMessage(Component.literal(l).withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }
}
