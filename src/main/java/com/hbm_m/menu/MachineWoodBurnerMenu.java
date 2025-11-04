package com.hbm_m.menu;
// Меню для дровяной печи.
// Имеет слоты для топлива и пепла. 

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.MachineWoodBurnerBlockEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.energy.LongDataPacker;

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
        // [ИСПРАВЛЕНО] Убедимся, что размер 8
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(8));
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
            // (Слоты остались на тех же координатах, как в старом файле)
            // Слот для топлива (0)
            this.addSlot(new SlotItemHandler(handler, 0, 26, 18) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return isFuel(stack);
                }
            });

            // Слот для пепла (1)
            this.addSlot(new SlotItemHandler(handler, 1, 26, 54) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
        });

        addDataSlots(data);
    }

    private boolean isFuel(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.LAVA_BUCKET) return false;
        if (item == ModItems.LIGNITE.get()) return true;
        return net.minecraftforge.common.ForgeHooks.getBurnTime(stack, null) > 0;
    }

    // --- [НАЧАЛО ИСПРАВЛЕНИЙ] ---

    public long getEnergyLong() {
        // (ПРАВИЛЬНО) Читаем 0 и 1
        int high = this.data.get(0);
        int low = this.data.get(1);
        return LongDataPacker.unpack(high, low);
    }

    public long getMaxEnergyLong() {
        // (ПРАВИЛЬНО) Читаем 2 и 3
        int high = this.data.get(2);
        int low = this.data.get(3);
        return LongDataPacker.unpack(high, low);
    }

    public int getBurnTime() {
        // [ИСПРАВЛЕНО] BlockEntity пишет в ячейку 4
        return this.data.get(4);
    }

    public int getMaxBurnTime() {
        // [ИСПРАВЛЕНО] BlockEntity пишет в ячейку 5
        return this.data.get(5);
    }

    public boolean isLit() {
        // [ИСПРАВЛЕНО] BlockEntity пишет в ячейку 6
        return this.data.get(6) != 0;
    }

    public void toggleEnabled() {
        // [ИСПРАВЛЕНО] BlockEntity слушает ячейку 7
        this.data.set(7, -1);
    }

    public boolean isEnabled() {
        // [ИСПРАВЛЕНО] BlockEntity пишет в ячейку 7
        return data.get(7) != 0;
    }

    public int getBurnTimeScaled(int scale) {
        // [ИСПРАВЛЕНО] Используем исправленные геттеры
        int maxBurnTime = this.getMaxBurnTime();
        int burnTime = this.getBurnTime();

        return maxBurnTime != 0 ? burnTime * scale / maxBurnTime : 0;
    }

    public int getEnergyScaled(int scale) {
        // [ИСПРАВЛЕНО] Используем long-геттеры для long-логики
        long energy = this.getEnergyLong();
        long maxEnergy = this.getMaxEnergyLong();

        // Используем double, чтобы избежать переполнения и получить точный int для GUI
        return maxEnergy != 0 ? (int) ((double)energy / maxEnergy * scale) : 0;
    }

    // --- [КОНЕЦ ИСПРАВЛЕНИЙ] ---


    // (Логика quickMoveStack, stillValid и addPlayer... не изменилась)
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
            boolean moved = false;
            if (isFuel(sourceStack)) {
                moved = this.moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + 1, false);
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
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
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 104 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 162));
        }
    }

    public MachineWoodBurnerBlockEntity getBlockEntity() {
        return this.blockEntity;
    }
}