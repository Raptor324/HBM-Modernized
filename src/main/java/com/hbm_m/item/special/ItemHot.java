package com.hbm_m.item.special;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.special.ItemHot}: glueht nach dem Erhitzen ("heat" im NBT) und kuehlt pro Tick im
 * Inventar um 1 ab; bei 0 wird das NBT wie im Original komplett entfernt. Im Inventar liegt das "_hot"-Symbol mit
 * Deckkraft = Hitze darueber ({@code ItemRendererHot}, Port: {@code HotItemDecorator}).
 *
 * <p>Im Original ist {@code heat} statisch und wird von jedem Konstruktor ueberschrieben; der zuletzt gebaute
 * ItemHot ist {@code ingot_chainsteel} (100). Damit gilt fuer alle heissen Gegenstaende die Maximalhitze 100 -
 * der Port legt diesen wirksamen Wert fest, statt von der Registrierungsreihenfolge abzuhaengen. Der
 * Konstruktorwert bleibt als Originalangabe erhalten.</p>
 */
public class ItemHot extends Item {

    protected static final int heat = 100;

    /** Textur des gluehenden Zustands ({@code getIconString() + "_hot"}). */
    public final ResourceLocation hotTexture;

    public ItemHot(int heat, ResourceLocation hotTexture, Properties properties) {
        super(properties);
        this.hotTexture = hotTexture;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {

        if (!world.isClientSide && stack.hasTag()) {

            int h = stack.getTag().getInt("heat");

            if (h > 0) {
                stack.getTag().putInt("heat", h - 1);
            } else {
                stack.setTag(null);
            }
        }
    }

    public static ItemStack heatUp(ItemStack stack) {

        if (!(stack.getItem() instanceof ItemHot))
            return stack;

        if (!stack.hasTag())
            stack.setTag(new CompoundTag());

        stack.getTag().putInt("heat", getMaxHeat(stack));
        return stack;
    }

    public static ItemStack heatUp(ItemStack stack, double d) {

        if (!(stack.getItem() instanceof ItemHot))
            return stack;

        if (!stack.hasTag())
            stack.setTag(new CompoundTag());

        stack.getTag().putInt("heat", (int) (d * getMaxHeat(stack)));
        return stack;
    }

    public static double getHeat(ItemStack stack) {

        if (!(stack.getItem() instanceof ItemHot))
            return 0;

        if (!stack.hasTag())
            return 0;

        int h = stack.getTag().getInt("heat");

        return (double) h / (double) heat;
    }

    public static int getMaxHeat(ItemStack stack) {
        return heat;
    }

    /** Maximalhitze fuer Datagen (Ofenrezepte liefern das heisse Ergebnis). */
    public static int maxHeat() {
        return heat;
    }
}
