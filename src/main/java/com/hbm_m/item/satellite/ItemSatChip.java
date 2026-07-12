package com.hbm_m.item.satellite;

import java.util.List;

import com.hbm_m.item.ISatChip;
import com.hbm_m.sound.ModSounds;

import net.minecraft.ChatFormatting;
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
 * Generic satellite payload chip (e.g. {@code sat_gerald}): carries a "freq" tag so it can be
 * matched to an {@link com.hbm_m.item.designator.ItemSatDesignator} remote after launch.
 * <p>
 * The legacy mod set frequency via a dedicated GUI keypad ({@code GUIScreenSatCoord}); that
 * screen isn't ported (out of scope - see Satellite Manager plan), so frequency is set with a
 * simple sneak + right-click cycle instead.
 */
public class ItemSatChip extends Item implements ISatChip {

    private static final int MAX_FREQ = 1000;

    public ItemSatChip(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        int next = (getFreq(stack) + 1) % MAX_FREQ;
        setFreq(stack, next);

        if (level.isClientSide()) {
            player.displayClientMessage(Component.translatable("message.hbm_m.satchip.freq_set", next), true);
        }
        level.playSound(player, player.blockPosition(), ModSounds.TOOL_TECH_BLEEP.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.satchip.freq", getFreq(stack)).withStyle(ChatFormatting.GRAY));
    }
}
