package com.hbm_m.item.tool;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.items.tool.ItemSwordMeteorite}: unzerstoerbar, kursive Beschreibung aus {@code .desc}. */
public class ItemSwordMeteorite extends ItemSwordAbility {

    public static final List<ItemSwordMeteorite> swords = new ArrayList<>();

    public ItemSwordMeteorite(float damage, double movement, Tier material) {
        // setMaxDamage(0)
        super(damage, movement, material, new Properties().stacksTo(1), true);
        swords.add(this);
        //? if >= 1.21.1 {
        /*com.hbm_m.platform.ItemComponentHooks.deferUnbreakable(this);
        *///?}
    }

    //? if < 1.21.1 {
    @Override
    public boolean canBeDepleted() {
        return false;
    }
    //?}

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        super.appendHbmTooltip(stack, level, list, flag);

        for (String line : Component.translatable(this.getDescriptionId() + ".desc").getString().split("\\$")) {
            list.add(Component.literal(line).withStyle(ChatFormatting.ITALIC));
        }
    }
}
