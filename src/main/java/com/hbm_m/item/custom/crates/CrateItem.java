package com.hbm_m.item.custom.crates;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.tooltip.CrateContentsTooltipComponent;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Базовый Item для ящиков HBM с отображением содержимого в тултипе.
 * Показывает первые 10 предметов + индикатор заполненности.
 */
public class CrateItem extends BlockItem {

    private static final int PREVIEW_LIMIT = 10;
    private final int totalSlots;

    public CrateItem(Block block, Properties properties, int totalSlots) {
        super(block, properties);
        this.totalSlots = totalSlots;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        CrateTooltipData data = readTooltipData(stack);
        if (data == null) return;

        int occupiedSlots = data.occupiedSlots();

        if (data.totalRows() > PREVIEW_LIMIT) {
            int remaining = data.totalRows() - PREVIEW_LIMIT;
            tooltip.add(Component.literal(" ...and " + remaining + " more")
                    .withStyle(getFillColor(occupiedSlots)));
        }

        if (occupiedSlots == 0) {
            tooltip.add(Component.literal(" [Empty]")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.literal(" " + occupiedSlots + "/" + totalSlots + " slots used")
                    .withStyle(getFillColor(occupiedSlots)));
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        CrateTooltipData data = readTooltipData(stack);
        if (data == null || data.previewEntries().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CrateContentsTooltipComponent(data.previewEntries()));
    }

    private @Nullable CrateTooltipData readTooltipData(ItemStack stack) {
        if (!stack.hasTag()) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("BlockEntityTag")) return null;

        CompoundTag beTag = tag.getCompound("BlockEntityTag");
        if (!beTag.contains("inventory")) return null;

        CompoundTag inventoryTag = beTag.getCompound("inventory");
        ItemStackHandler handler = new ItemStackHandler(totalSlots);
        handler.deserializeNBT(inventoryTag);

        Map<String, GroupData> groups = new LinkedHashMap<>();
        int occupiedSlots = 0;

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack item = handler.getStackInSlot(i);
            if (!item.isEmpty()) {
                occupiedSlots++;
                String key = makeGroupingKey(item);
                GroupData group = groups.computeIfAbsent(key, k -> new GroupData(item.copy()));
                group.totalCount += item.getCount();
            }
        }

        List<CrateContentsTooltipComponent.Entry> allRows = new ArrayList<>();
        for (GroupData group : groups.values()) {
            // Всегда одна строка на уникальный предмет (сумма всех стаков).
            ItemStack representative = group.representative.copy();
            representative.setCount(1);
            allRows.add(new CrateContentsTooltipComponent.Entry(representative, group.totalCount));
        }
        allRows.sort(
                Comparator
                        .comparingInt(CrateContentsTooltipComponent.Entry::totalCount)
                        .reversed()
                        .thenComparing(entry -> entry.stack().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER)
        );

        int totalRows = allRows.size();
        List<CrateContentsTooltipComponent.Entry> previewEntries =
                totalRows > PREVIEW_LIMIT ? allRows.subList(0, PREVIEW_LIMIT) : allRows;

        return new CrateTooltipData(occupiedSlots, totalRows, previewEntries);
    }

    private static String makeGroupingKey(ItemStack stack) {
        CompoundTag keyTag = stack.copy().save(new CompoundTag());
        keyTag.remove("Count");
        return keyTag.toString();
    }

    private static final class GroupData {
        private final ItemStack representative;
        private int totalCount = 0;

        private GroupData(ItemStack representative) {
            this.representative = representative;
        }
    }

    private record CrateTooltipData(int occupiedSlots, int totalRows,
                                    List<CrateContentsTooltipComponent.Entry> previewEntries) {}

    private ChatFormatting getFillColor(int totalItems) {
        float pct = (float) totalItems / totalSlots * 100;
        if (pct <= 33.0f) return ChatFormatting.GREEN;
        if (pct <= 66.0f) return ChatFormatting.YELLOW;
        return ChatFormatting.RED;
    }
}
