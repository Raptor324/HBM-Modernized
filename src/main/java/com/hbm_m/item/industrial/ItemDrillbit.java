package com.hbm_m.item.industrial;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code ItemDrillbit}: Bohrkoepfe fuer den grossen Bergbaubohrer. Das Original ist ein Metadaten-Item ueber
 * {@link EnumDrillType}; im Port ist jede Sorte ein eigenes Item mit ihrem Typ.
 */
public class ItemDrillbit extends Item {

    public enum EnumDrillType {
        STEEL           (1.0D, 1, 0, false, false),
        STEEL_DIAMOND   (1.0D, 1, 2, false, true),
        HSS             (1.2D, 2, 0, true, false),
        HSS_DIAMOND     (1.2D, 2, 3, true, true),
        DESH            (1.5D, 3, 1, true, true),
        DESH_DIAMOND    (1.5D, 3, 4, true, true),
        TCALLOY         (2.0D, 4, 1, true, true),
        TCALLOY_DIAMOND (2.0D, 4, 4, true, true),
        FERRO           (2.5D, 5, 1, true, true),
        FERRO_DIAMOND   (2.5D, 5, 4, true, true);

        public final double speed;
        public final int tier;
        public final int fortune;
        public final boolean vein;
        public final boolean silk;

        EnumDrillType(double speed, int tier, int fortune, boolean vein, boolean silk) {
            this.speed = speed;
            this.tier = tier;
            this.fortune = fortune;
            this.vein = vein;
            this.silk = silk;
        }
    }

    public final EnumDrillType type;

    public ItemDrillbit(Properties properties, EnumDrillType type) {
        super(properties);
        this.type = type;
    }

    @Nullable
    public static EnumDrillType typeOf(ItemStack stack) {
        return stack.getItem() instanceof ItemDrillbit bit ? bit.type : null;
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        list.add(Component.literal("Speed: " + ((int) (type.speed * 100)) + "%").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("Tier: " + type.tier).withStyle(ChatFormatting.YELLOW));
        if (type.fortune > 0) list.add(Component.literal("Fortune " + type.fortune).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (type.vein) list.add(Component.literal("Vein miner").withStyle(ChatFormatting.GREEN));
        if (type.silk) list.add(Component.literal("Silk touch").withStyle(ChatFormatting.GREEN));
    }
}
