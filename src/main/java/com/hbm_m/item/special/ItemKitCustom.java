package com.hbm_m.item.special;

import com.hbm_m.platform.StackNbt;

import com.hbm_m.inventory.HeldItemInventory;
import com.hbm_m.item.ModItems;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ItemKitCustom}: wie {@link ItemKitNBT}, aber mit zwei per NBT ({@code color1}/{@code color2})
 * eingefaerbten Overlay-Schichten ({@code kit_1}/{@code kit_2}), eigenem Namen und Lore. Nicht im Kreativmenue.
 */
public class ItemKitCustom extends ItemKitNBT {

    public ItemKitCustom(Properties properties) {
        super(properties);
    }

    /** Original {@code getColorFromItemStack}: Pass 1/2 aus dem NBT, Grundschicht weiss. */
    public static int getRenderColor(ItemStack stack, int pass) {
        if (pass == 1) return getColor(stack, 1);
        if (pass == 2) return getColor(stack, 2);
        return 0xffffff;
    }

    public static ItemStack create(String name, String lore, int color1, int color2, ItemStack... contents) {
        ItemStack stack = new ItemStack(ModItems.KIT_CUSTOM.get());

        StackNbt.orCreate(stack);

        setColor(stack, color1, 1);
        setColor(stack, color2, 2);

        if (lore != null) addTooltipToStack(stack, lore.split("\\$"));
        // EnumChatFormatting.RESET: Name ohne die Kursivschrift umbenannter Gegenstaende
        StackNbt.setCustomName(stack, Component.literal(name).withStyle(s -> s.withItalic(false)));
        HeldItemInventory.addStacksToNBT(stack, contents);

        return stack;
    }

    /** {@code ItemStackUtil.addTooltipToStack}: haengt Zeilen an {@code display.Lore} an. */
    public static void addTooltipToStack(ItemStack stack, String... lines) {
        //? if < 1.21.1 {
        var display = StackNbt.orCreateElement(stack, "display");
        ListTag lore = display.getList("Lore", 8);
        for (String line : lines) lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line))));
        display.put("Lore", lore);
        //?} else {
        /*// 1.21.1: Lore ist die Komponente LORE (gleicher Stil wie display.Lore)
        for (String line : lines)
            stack.update(net.minecraft.core.component.DataComponents.LORE, net.minecraft.world.item.component.ItemLore.EMPTY,
                    l -> l.withLineAdded(Component.literal(line)));
        *///?}
    }

    public static void setColor(ItemStack stack, int color, int index) {
        StackNbt.orCreate(stack).putInt("color" + index, color);
    }

    public static int getColor(ItemStack stack, int index) {
        if (!StackNbt.has(stack)) return 0;
        return StackNbt.read(stack).getInt("color" + index);
    }
}
