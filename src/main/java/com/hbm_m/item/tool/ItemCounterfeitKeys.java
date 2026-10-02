package com.hbm_m.item.tool;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.tile.ILockableTile;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ItemKeyPin;
import com.hbm_m.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code com.hbm.items.tool.ItemCounterfeitKeys} ({@code key_kit}): an einem verschlossenen Behaelter, dessen
 * Schloss nachgemacht werden kann ({@code cheesable}), entstehen zwei {@code key_fake} mit dessen Pins.
 */
public class ItemCounterfeitKeys extends Item implements ITooltipProvider {

    public ItemCounterfeitKeys(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockEntity te = world.getBlockEntity(ctx.getClickedPos());

        if (te instanceof ILockableTile locked) {

            if (locked.isLocked() && locked.isCheesable()) {
                ItemStack st = new ItemStack(ModItems.KEY_FAKE.get());
                ItemKeyPin.setCode(st, locked.getPins());
                player.getInventory().setItem(player.getInventory().selected, st.copy());

                if (!player.getInventory().add(st.copy())) {
                    player.drop(st.copy(), false);
                }

                player.inventoryMenu.broadcastChanges();
                player.swing(ctx.getHand());
                return InteractionResult.SUCCESS;

            } else if (!locked.isCheesable()) {
                if (!world.isClientSide) {
                    player.sendSystemMessage(Component.literal("This lock is too elaborate for a counterfeit key to be made").withStyle(ChatFormatting.LIGHT_PURPLE));
                    player.sendSystemMessage(Component.literal("Perhaps there is another way around here to unlock it").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Use on a locked container to create two counterfeit keys!"));
    }
}
