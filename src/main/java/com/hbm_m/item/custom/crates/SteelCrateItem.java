package com.hbm_m.item.custom.crates;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SteelCrateItem extends BlockItem {

    private static final int TOTAL_SLOTS = 54;

    public SteelCrateItem(net.minecraft.world.level.block.Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("BlockEntityTag")) {
                CompoundTag beTag = tag.getCompound("BlockEntityTag");

                if (beTag.contains("inventory")) {
                    CompoundTag inventoryTag = beTag.getCompound("inventory");
                    ItemStackHandler handler = new ItemStackHandler(TOTAL_SLOTS);
                    handler.deserializeNBT(inventoryTag);

                    int itemsShown = 0;
                    int totalItems = 0;

                    // Список всегда голубой
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack item = handler.getStackInSlot(i);
                        if (!item.isEmpty()) {
                            totalItems++;

                            if (itemsShown < 10) {
                                String itemName = item.getHoverName().getString();
                                int count = item.getCount();
                                tooltip.add(Component.literal("  " + itemName + " ×" + count)
                                        .withStyle(ChatFormatting.AQUA)); // Голубой цвет
                                itemsShown++;
                            }
                        }
                    }

                    // Динамичная последняя строка
                    ChatFormatting fillColor = getFillColor(totalItems, TOTAL_SLOTS);

                    if (totalItems > 10) {
                        int remaining = totalItems - 10;
                        tooltip.add(Component.literal("  and " + remaining + " more.../54")
                                .withStyle(fillColor)); // Цвет зависит от заполненности
                    }

                    if (totalItems == 0) {
                        tooltip.add(Component.literal("  [Empty]").withStyle(fillColor));
                    }
                }
            }
        }
    }

    private ChatFormatting getFillColor(int totalItems, int totalSlots) {
        float fillPercentage = (float) totalItems / totalSlots * 100;

        if (fillPercentage <= 33.0f) {
            return ChatFormatting.GREEN;
        } else if (fillPercentage <= 66.0f) {
            return ChatFormatting.YELLOW;
        } else {
            return ChatFormatting.RED;
        }
    }
}
