package com.hbm_m.item.crates;

import com.hbm_m.item.ITooltipProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.tooltip.CrateContentsTooltipComponent;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import com.hbm_m.platform.ModItemStackHandler;

/**
 * Базовый Item для ящиков HBM с отображением содержимого в тултипе.
 * Показывает первые 10 предметов + индикатор заполненности.
 */
public class CrateItem extends BlockItem implements ITooltipProvider {

    private static final int PREVIEW_LIMIT = 10;
    private final int totalSlots;

    public CrateItem(Block block, Properties properties, int totalSlots) {
        super(block, properties);
        this.totalSlots = totalSlots;
    }

    /** Platzzahl der Kiste (ContainerUpgradeRecipe kuerzt uebernommene Inhalte darauf). */
    public int getTotalSlots() {
        return totalSlots;
    }

    // ---- 1:1 ItemBlockStorageCrate: Kiste aus der Hand oeffnen (ServerConfig.CRATE_OPEN_HELD) ----

    /** Original: nur Eisen-, Stahl-, Desh-, Wolframkiste und Tresor sind {@code ItemBlockStorageCrate}. */
    private boolean opensHeld() {
        Block b = getBlock();
        return b == com.hbm_m.block.ModBlocks.CRATE_IRON.get() || b == com.hbm_m.block.ModBlocks.CRATE_STEEL.get()
                || b == com.hbm_m.block.ModBlocks.CRATE_DESH.get() || b == com.hbm_m.block.ModBlocks.CRATE_TUNGSTEN.get()
                || b == com.hbm_m.block.ModBlocks.SAFE.get();
    }

    /** Original {@code onItemUse}: mit CRATE_OPEN_HELD setzt man die Kiste nur schleichend. */
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext ctx) {
        if (com.hbm_m.config.ModClothConfig.get().crateOpenHeld && opensHeld() && ctx.getPlayer() != null && !ctx.getPlayer().isShiftKeyDown()) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        return super.useOn(ctx);
    }

    /** Original {@code onItemRightClick}. */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!com.hbm_m.config.ModClothConfig.get().crateOpenHeld || !opensHeld() || hand != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return super.use(level, player, hand);
        }

        if (!level.isClientSide && stack.getCount() == 1 && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            HeldCrate.tryOpen(sp, stack, getBlock());
        }

        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {

        // 1:1 BlockStorageCrate.addInformation: Spinnen und Schloss verbergen den Inhalt
        CompoundTag be = com.hbm_m.platform.BlockEntityItemData.read(stack);
        if (be != null) {
            boolean locked = be.getBoolean("isLocked");
            if (be.getBoolean("spiders")) {
                if (locked) tooltip.add(Component.literal("This container is locked.").withStyle(ChatFormatting.RED));
                tooltip.add(Component.literal("Skittering emanates from within...").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                return;
            }
            if (locked) {
                tooltip.add(Component.literal("This container is locked.").withStyle(ChatFormatting.RED));
                CrateTooltipData d = readTooltipData(stack);
                tooltip.add(Component.literal(d != null && d.occupiedSlots() > 0 ? "It feels heavy..." : "It feels empty...").withStyle(ChatFormatting.YELLOW));
                return;
            }
        }

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
        CompoundTag be = com.hbm_m.platform.BlockEntityItemData.read(stack);
        if (be != null) {
            if (be.getBoolean("isLocked") || be.getBoolean("spiders")) return Optional.empty();
        }
        CrateTooltipData data = readTooltipData(stack);
        if (data == null || data.previewEntries().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CrateContentsTooltipComponent(data.previewEntries()));
    }

    private @Nullable CrateTooltipData readTooltipData(ItemStack stack) {
        CompoundTag beTag = com.hbm_m.platform.BlockEntityItemData.read(stack);
        if (beTag == null) return null;
        if (!beTag.contains("inventory")) return null;

        CompoundTag inventoryTag = beTag.getCompound("inventory");
        ModItemStackHandler handler = new ModItemStackHandler(totalSlots) {
            @Override
            protected void onContentsChanged(int slot) {}
        };
        //? if < 1.21.1 {
        handler.deserializeNBT(inventoryTag);
        //?} else {
        /*// Tooltip-path — клиентский; провайдер из клиентского Level.
        // ItemStackHandler.deserializeNBT в 1.21.1 требует HolderLookup.Provider.
        handler.deserializeNBT(PlatformHooks.clientProvider(), inventoryTag);
        *///?}

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
        //? if < 1.21.1 {
        CompoundTag keyTag = stack.copy().save(new CompoundTag());
        //?} else {
        /*// Tooltip-path — клиентский; провайдер из клиентского Level (1.21.1 требует Provider для save).
        CompoundTag keyTag = PlatformHooks.safeItemSave(stack.copy(), PlatformHooks.clientProvider());
        *///?}
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
