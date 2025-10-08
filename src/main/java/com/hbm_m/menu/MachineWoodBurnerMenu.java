package com.hbm_m.menu;
// Меню для дровяной печи.
// Имеет слоты для топлива и пепла. 

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.MachineWoodBurnerBlockEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.main.MainRegistry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

import static net.minecraft.tags.BlockTags.PLANKS;
import static net.minecraft.tags.ItemTags.SAPLINGS;

public class MachineWoodBurnerMenu extends AbstractContainerMenu {
    private final MachineWoodBurnerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public MachineWoodBurnerMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(5));
    }

    public MachineWoodBurnerMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.WOOD_BURNER_MENU.get(), pContainerId);
        checkContainerSize(inv, 2);
        this.blockEntity = (MachineWoodBurnerBlockEntity) entity;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            // Слот для топлива (0)
            this.addSlot(new SlotItemHandler(handler, 0, 26, 18) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    // Проверяем, является ли предмет топливом
                    return isFuel(stack);
                }
            });

            // Слот для пепла (1) - только вывод
            this.addSlot(new SlotItemHandler(handler, 1, 26, 54) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false; // Запрещаем помещать что-либо в слот пепла
                }
            });
        });

        addDataSlots(data);
    }

    private boolean isFuel(ItemStack stack) {
        Item item = stack.getItem();

        // КАСТОМНОЕ ТОПЛИВО
        if (item == ModItems.LIGNITE.get()) return true;

        // УГОЛЬ
        if (item == Items.COAL || item == Items.CHARCOAL) return true;

        // ТЕГИ - автоматически покрывают все типы дерева
        if (stack.is(ItemTags.LOGS) || stack.is(ItemTags.LOGS_THAT_BURN)) return true;
        if (stack.is(ItemTags.PLANKS)) return true;
        if (stack.is(ItemTags.WOODEN_STAIRS)) return true;

        if (stack.is(ItemTags.WOODEN_SLABS)) return true;
        if (stack.is(ItemTags.WOODEN_FENCES)) return true;
        if (stack.is(ItemTags.WOODEN_TRAPDOORS)) return true;
        if (stack.is(ItemTags.WOODEN_DOORS)) return true;
        if (stack.is(ItemTags.WOODEN_BUTTONS)) return true;
        if (stack.is(ItemTags.WOODEN_PRESSURE_PLATES)) return true;
        if (stack.is(ItemTags.SIGNS)) return true;
        if (stack.is(ItemTags.HANGING_SIGNS)) return true;
        if (stack.is(ItemTags.BOATS)) return true;
        if (stack.is(ItemTags.CHEST_BOATS)) return true;

        // МЕЛОЧИ
        if (item == Items.STICK || item == Items.BOWL || item == Items.BAMBOO) return true;

        // ДЕРЕВЯННЫЕ ИНСТРУМЕНТЫ
        if (item == Items.WOODEN_SWORD || item == Items.WOODEN_PICKAXE ||
                item == Items.WOODEN_AXE || item == Items.WOODEN_SHOVEL ||
                item == Items.WOODEN_HOE) return true;

        // ПРОЧИЕ ДЕРЕВЯННЫЕ БЛОКИ
        if (item == Items.CRAFTING_TABLE || item == Items.CARTOGRAPHY_TABLE ||
                item == Items.FLETCHING_TABLE || item == Items.SMITHING_TABLE ||
                item == Items.LOOM || item == Items.BARREL || item == Items.COMPOSTER ||
                item == Items.CHEST || item == Items.TRAPPED_CHEST ||
                item == Items.BOOKSHELF || item == Items.CHISELED_BOOKSHELF ||
                item == Items.LECTERN || item == Items.NOTE_BLOCK ||
                item == Items.JUKEBOX || item == Items.DAYLIGHT_DETECTOR ||
                item == Items.LADDER) return true;

        // ПРОЧЕЕ ТОПЛИВО
        if (item == Items.BLAZE_ROD || item == Items.DRIED_KELP_BLOCK) return true;

        return false;
    }

    public int getEnergy() {
        return this.data.get(0);
    }

    public int getMaxEnergy() {
        return this.data.get(1);
    }

    public int getBurnTime() {
        return this.data.get(2);
    }

    public int getMaxBurnTime() {
        return this.data.get(3);
    }

    public boolean isLit() {
        return this.data.get(4) != 0;
    }

    public int getBurnTimeScaled(int scale) {
        int maxBurnTime = this.data.get(3);
        int burnTime = this.data.get(2);

        return maxBurnTime != 0 ? burnTime * scale / maxBurnTime : 0;
    }

    public int getEnergyScaled(int scale) {
        int energy = this.data.get(0);
        int maxEnergy = this.data.get(1);

        return maxEnergy != 0 ? energy * scale / maxEnergy : 0;
    }

    // Константы для quickMove логики
    private static final int VANILLA_SLOT_COUNT = 36;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = 36;
    private static final int TE_INVENTORY_SLOT_COUNT = 2;

    @Override
    public @NotNull ItemStack quickMoveStack(@Nonnull Player playerIn, int pIndex) {
        Slot sourceSlot = this.slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // Перемещение из инвентаря игрока в машину
            boolean moved = false;

            // Если это топливо, пытаемся поместить в слот топлива (0)
            if (isFuel(sourceStack)) {
                moved = this.moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + 1, false);
            }

            if (!moved) {
                return ItemStack.EMPTY;
            }
        } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // Перемещение из машины в инвентарь игрока
            if (!this.moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            MainRegistry.LOGGER.debug("Invalid slotIndex:" + pIndex);
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(@Nonnull Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                pPlayer, ModBlocks.WOOD_BURNER.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                // ИЗМЕНЕНО: было 84 + i * 18, стало 120 + i * 18 (опустили на 36 пикселей)
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 104 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            // ИЗМЕНЕНО: было 142, стало 178 (опустили на 36 пикселей)
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 162));
        }
    }
}