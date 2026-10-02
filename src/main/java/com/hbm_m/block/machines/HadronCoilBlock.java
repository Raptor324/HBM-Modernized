package com.hbm_m.block.machines;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;

/** 1:1 {@code BlockHadronCoil}: Spulenblock mit Verbundtextur zu allen Spulen und der Spulenstaerke im Tooltip. */
public class HadronCoilBlock extends Block {

    public final int factor;

    public HadronCoilBlock(Properties p, int factor) {
        super(p);
        this.factor = factor;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal(Component.translatable("info.coil").getString() + ": " + String.format(Locale.US, "%,d", factor)));
    }
}
