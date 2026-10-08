package com.hbm_m.item.tool;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.items.tool.ModSword}: Brecheisen, Bleirohr und Reer Graar. */
public class ModSword extends SwordItem implements ITooltipProvider {

    public ModSword(Tier mat) {
        //? if < 1.21.1 {
        super(mat, 4, -2.4F, new Properties());
        //?} else {
        /*super(mat, new Properties().attributes(SwordItem.createAttributes(mat, 4, -2.4F)));
        *///?}
    }

    @Override
    public void appendHbmTooltip(ItemStack itemstack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (this == ModItems.WEAPON_PIPE_LEAD.get())
            list.add(Component.literal("I'm going to attempt a manual override on this wall."));

        if (this == ModItems.REER_GRAAR.get()) {
            list.add(Component.literal("Call now!"));
            list.add(Component.literal("555-10-3728-ZX7-INFINITE"));
        }
    }
}
