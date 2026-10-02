package com.hbm_m.item.tool;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.blockentity.network.radio.RTTYNetwork;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm_m.interfaces.IItemControlReceiver;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.network.InfoToastPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.tool.ItemRTTYPager}: zeigt das Signal eines Funkkanals an (ID_PAGER_DYN + Platz, 5 s);
 * das Signal "selfdestruct" sprengt den Pager.
 */
public class ItemRTTYPager extends Item implements IItemControlReceiver, ITooltipProvider {

    public static final String KEY_CHANNEL = "chan";
    /** Original {@code ServerProxy.ID_PAGER_DYN}. */
    public static final int ID_PAGER_DYN = 1000;

    public ItemRTTYPager(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean held) {
        if (!stack.hasTag() || !stack.getTag().contains(KEY_CHANNEL)) return;
        if (!(entity instanceof ServerPlayer player) || world.isClientSide) return;

        String channelFreq = stack.getTag().getString(KEY_CHANNEL);
        RTTYNetwork.RttyChannel chan = RTTYNetwork.listen(world, channelFreq);

        if (chan != null && chan.timeStamp >= world.getGameTime() - 1) {
            int alive = entity.tickCount % 1000;
            Component message = Component.literal("[ " + channelFreq + " (" + alive + ") ] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(String.valueOf(chan.signal)).withStyle(ChatFormatting.YELLOW));

            if ("selfdestruct".equals(chan.signal + "")) {
                ExplosionVNT vnt = new ExplosionVNT(world, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 5, null);
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 50).setupPiercing(5F, 0.5F));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
                vnt.explode();
                stack.shrink(1);
                return;
            }

            InfoToastPacket.sendTo(player, message, 100, ID_PAGER_DYN + slot, 0xFFFFFF);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world.isClientSide) com.hbm_m.inventory.gui.GUIScreenPager.open(stack);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (!stack.hasTag() || !stack.getTag().contains(KEY_CHANNEL) || stack.getTag().getString(KEY_CHANNEL).isEmpty()) {
            list.add(Component.literal("No channel set!").withStyle(ChatFormatting.RED));
        } else {
            list.add(Component.literal("Channel: " + stack.getTag().getString(KEY_CHANNEL)).withStyle(ChatFormatting.YELLOW));
        }
    }

    @Override
    public void receiveControl(ItemStack stack, CompoundTag data) {
        if (data.contains("chan")) {
            if (!stack.hasTag()) stack.setTag(new CompoundTag());
            stack.getTag().putString(KEY_CHANNEL, data.getString(KEY_CHANNEL));
        }
    }
}
