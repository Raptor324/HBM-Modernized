package com.hbm_m.item.industrial;

import com.hbm_m.item.ITooltipProvider;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.platform.PlatformHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ZirnoxRodItem extends Item implements ITooltipProvider {

    private static final String NBT_LIFE = "life";

    private final int maxLife;
    private final int heat;
    private final boolean breeding;

    public ZirnoxRodItem(Properties properties, int maxLife, int heat, boolean breeding) {
        super(properties.stacksTo(1));
        this.maxLife = maxLife;
        this.heat = heat;
        this.breeding = breeding;
    }

    public int getMaxLife() {
        return maxLife;
    }

    public int getHeat() {
        return heat;
    }

    public boolean isBreeding() {
        return breeding;
    }

    public static int getLifeTime(ItemStack stack) {
        CompoundTag tag = PlatformHooks.getItemTag(stack);
        return tag == null ? 0 : tag.getInt(NBT_LIFE);
    }

    public static void setLifeTime(ItemStack stack, int time) {
        PlatformHooks.editItemTag(stack, t -> t.putInt(NBT_LIFE, Math.max(0, time)));
    }

    public static void incrementLifeTime(ItemStack stack) {
        setLifeTime(stack, getLifeTime(stack) + 1);
    }

    private static int getMaxLife(ItemStack stack) {
        if (stack.getItem() instanceof ZirnoxRodItem rodItem) {
            return rodItem.getMaxLife();
        }
        return 1;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getLifeTime(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int max = Math.max(1, getMaxLife(stack));
        int life = Math.min(getLifeTime(stack), max);
        float remaining = 1.0F - (life / (float) max);
        return Math.max(0, Math.round(13.0F * remaining));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xE5C14B;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // 1:1 Original ItemZirnoxRod.addInformation: gelbe Abbrandzeile, dann desc.item.zirnox(Breeding)Rod ($-getrennt, Farben aus dem Lang-Text)
        tooltip.add(Component.translatable("trait.rbmk.depletion", ((int) ((((double) getLifeTime(stack)) / (double) maxLife) * 100000)) / 1000D + "%").withStyle(ChatFormatting.YELLOW));
        // Original reicht fuer %d einen getShortNumber-String durch (ergibt dort "Format error"); hier die gemeinten Zahlen.
        String raw = net.minecraft.locale.Language.getInstance().getOrDefault(breeding ? "desc.item.zirnoxBreedingRod" : "desc.item.zirnoxRod");
        String text;
        try {
            text = breeding ? String.format(java.util.Locale.US, raw, maxLife) : String.format(java.util.Locale.US, raw, heat, maxLife);
        } catch (java.util.IllegalFormatException e) {
            text = raw;
        }
        for (String s : text.split("\\$")) {
            tooltip.add(Component.literal(s));
        }
    }
}