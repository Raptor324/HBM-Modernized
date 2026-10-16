package com.hbm_m.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

/**
 * Порт «undefined» (item 4993, ItemCustomLore в 1.7.10): лор-тултип показывает
 * случайное имя предмета из реестра (меняется каждые 500 мс), с шансом 1/10 —
 * тёмно-красную строку «UNDEFINED», при неудачном выборе — красный «ERROR #r».
 */
public class UndefinedItem extends Item implements ITooltipProvider {

    /** Размер реестра предметов (ориг. Item.itemRegistry.getKeys().size()). */
    private static int setSize = 0;

    public UndefinedItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // Порт ModItems.undefined (ItemCustomLore.addInformation): в оригинале ветка 1/10
        // использует worldObj.rand (мигает), ветка имени — Random, засеянный временем/500
        // (имя держится полсекунды). Любое исключение -> тёмно-красное «UNDEFINED».
        try {
            Random flicker = new Random((level != null ? level.getGameTime() : 0) + stack.hashCode());
            if (flicker.nextInt(10) == 0) {
                tooltip.add(Component.literal("UNDEFINED").withStyle(ChatFormatting.DARK_RED));
            } else {
                Random rand = new Random(System.currentTimeMillis() / 500);

                if (setSize == 0) {
                    setSize = BuiltInRegistries.ITEM.keySet().size();
                }

                int r = rand.nextInt(setSize);
                var key = BuiltInRegistries.ITEM.keySet().stream().skip(r).findFirst().orElse(null);

                if (key != null) {
                    Item item = BuiltInRegistries.ITEM.get(key);
                    tooltip.add(new ItemStack(item).getHoverName());
                } else {
                    tooltip.add(Component.literal("ERROR #" + r).withStyle(ChatFormatting.RED));
                }
            }
        } catch (Exception ex) {
            tooltip.add(Component.literal("UNDEFINED").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
