package com.hbm_m.block.machines;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;

/**
 * 1:1 die PWR-Bauteile des Originals: {@code BlockGenericTooltip} (Waermetauscher, Kuehlkoerper, Neutronenquelle,
 * Reflektor, Huelle, Anschluss) bzw. {@code BlockPillarPWR} (Brennstab, Steuerstab, Kuehlkanal) - einfache Bloecke mit
 * Beschreibung. Beim Zusammenbau setzt der Controller an ihre Stelle den Traeger {@code pwr_block}
 * ({@link PWRBlock}), der sich das Bauteil merkt.
 */
public class PWRPartBlock extends Block implements PWRPart {

    private final Kind kind;

    public PWRPartBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override public Kind getKind() { return kind; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    /** 1:1 {@code BlockPillarPWR}. */
    public static class Pillar extends RotatedPillarBlock implements PWRPart {
        private final Kind kind;

        public Pillar(Kind kind, Properties properties) {
            super(properties);
            this.kind = kind;
        }

        @Override public Kind getKind() { return kind; }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
            com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
        }
    }
}
