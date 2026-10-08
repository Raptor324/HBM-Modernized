package com.hbm_m.item.industrial;

import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ItemArcElectrode}: Elektrode des Lichtbogenofens, je Werkstoff ein eigenes Item (Original: Meta). Jeder
 * Schmelzvorgang erhoeht {@code durability} im NBT; ist die Haltbarkeit des Typs erreicht, wird sie zur
 * {@link ItemArcElectrodeBurnt geschmolzenen Elektrode}.
 */
public class ItemArcElectrode extends Item {

    public enum EnumElectrodeType {
        GRAPHITE(10),
        LANTHANIUM(100),
        DESH(500),
        SATURNITE(1500);

        public final int durability;

        EnumElectrodeType(int dura) {
            this.durability = dura;
        }
    }

    public final EnumElectrodeType type;

    public ItemArcElectrode(Properties properties, EnumElectrodeType type) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    public static int getDurability(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 0;
        return tag.getInt("durability");
    }

    /** Original {@code damage}: true, wenn die Elektrode damit verbraucht ist. */
    public static boolean damage(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemArcElectrode)) return false;
        CompoundTag tag = stack.getOrCreateTag();
        int durability = tag.getInt("durability");
        durability++;
        tag.putInt("durability", durability);
        return durability >= getMaxDurability(stack);
    }

    public static int getMaxDurability(ItemStack stack) {
        return stack.getItem() instanceof ItemArcElectrode e ? e.type.durability : 1;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return getDurability(stack) > 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return Math.round(13.0F - 13.0F * getDurability(stack) / (float) getMaxDurability(stack));
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        float f = Math.max(0.0F, 1.0F - getDurability(stack) / (float) getMaxDurability(stack));
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }
}
