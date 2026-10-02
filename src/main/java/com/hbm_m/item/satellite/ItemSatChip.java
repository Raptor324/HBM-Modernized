package com.hbm_m.item.satellite;

import java.util.List;

import com.hbm_m.item.ISatChip;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.machine.ItemSatChip}: Satellitenchip mit Frequenz (NBT "freq"); die Frequenz
 * setzt der Satelliten-Verknuepfer. Der fruehere Port-eigene Schleichen+Rechtsklick-Zaehler ist entfernt.
 */
public class ItemSatChip extends Item implements ISatChip, ITooltipProvider {

    public ItemSatChip(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHbmTooltip(ItemStack itemstack, Level level, List<Component> list, TooltipFlag flag) {

        list.add(Component.translatable("satchip.frequency").append(": " + getFreq(itemstack)));

        if (this == ModItems.SAT_FOEQ.get())
            list.add(Component.translatable("satchip.foeq"));

        if (this == ModItems.SAT_GERALD.get()) {
            for (String line : Component.translatable("satchip.gerald.desc").getString().split("\\$")) {
                list.add(Component.literal(line));
            }
        }

        if (this == ModItems.SAT_LASER.get())
            list.add(Component.translatable("satchip.laser"));

        if (this == ModItems.SAT_MAPPER.get())
            list.add(Component.translatable("satchip.mapper"));

        if (this == ModItems.SAT_MINER.get())
            list.add(Component.translatable("satchip.miner"));

        if (this == ModItems.SAT_LUNAR_MINER.get())
            list.add(Component.translatable("satchip.lunar_miner"));

        if (this == ModItems.SAT_RADAR.get())
            list.add(Component.translatable("satchip.radar"));

        if (this == ModItems.SAT_RESONATOR.get())
            list.add(Component.translatable("satchip.resonator"));

        if (this == ModItems.SAT_SCANNER.get())
            list.add(Component.translatable("satchip.scanner"));
    }
}
