package com.hbm_m.inventory.fluid.trait;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Joke trait of {@code unstable_sanity} ("Concentrate REN-TV", no 1.7.10 analog): the hover line
 * "(hmm, smells like piss, weird)".
 */
public class FT_Sanity extends FluidTrait {

    @Override
    public void addInfo(List<Component> info) {
        info.add(Component.translatable("hbmfluid.trait.sanity").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
    }
}
