package com.hbm_m.item.tool;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.handler.BossSpawnHandler;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.sound.ModSounds;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.tool.ItemMeteorRemote}: ruft einen Meteor auf den Spieler herab (wie ein natuerlicher
 * Einschlag im Umkreis von 100 Bloecken). Zwei Benutzungen, nicht reparierbar.
 */
public class ItemMeteorRemote extends Item implements ITooltipProvider {

    public ItemMeteorRemote(Properties properties) {
        super(properties.durability(2).setNoRepair());
    }

    @Override
    public void appendHbmTooltip(ItemStack itemstack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Right click to summon a meteorite!"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));

        if (!world.isClientSide) {
            BossSpawnHandler.spawnMeteorAtPlayer(player, false);
            player.sendSystemMessage(Component.literal("Watch your head!"));
            world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TOOL_TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        player.swing(hand);
        return InteractionResultHolder.success(stack);
    }
}
