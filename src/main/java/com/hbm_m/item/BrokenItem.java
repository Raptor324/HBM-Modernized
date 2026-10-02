package com.hbm_m.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1:1 {@code com.hbm.items.BrokenItem}: "Broken %s" eines anderen Gegenstands (NBT itemID/itemMeta). Das Symbol ist
 * das des anderen Gegenstands mit dem Riss darueber (Port: {@code client.BrokenItemDecorator}). Die Meta-Angabe ist
 * im Port bedeutungslos und wird nur mitgeschrieben.
 */
public class BrokenItem extends Item {

    public BrokenItem(Properties properties) {
        super(properties);
    }

    /** Der gebrochene Gegenstand, oder leer. */
    public static ItemStack getBrokenStack(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return ItemStack.EMPTY;
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("itemID"));
        if (id == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item);
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemStack sta = getBrokenStack(stack);
        if (sta.isEmpty()) return super.getName(stack);
        return Component.translatable(this.getDescriptionId(stack) + ".prefix", sta.getHoverName());
    }

    public static ItemStack make(ItemStack stack) { return make(stack.getItem(), stack.getCount(), 0); }
    public static ItemStack make(Item item) { return make(item, 1, 0); }
    public static ItemStack make(Item item, int meta) { return make(item, 1, meta); }

    public static ItemStack make(Item item, int stacksize, int meta) {
        ItemStack stack = new ItemStack(ModItems.BROKEN_ITEM.get(), stacksize);
        CompoundTag nbt = new CompoundTag();
        nbt.putString("itemID", BuiltInRegistries.ITEM.getKey(item).toString());
        nbt.putInt("itemMeta", meta);
        stack.setTag(nbt);
        return stack;
    }
}
