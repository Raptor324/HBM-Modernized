package com.hbm_m.menu;

// Меню для продвинутой сборочной машины.
// Имеет слоты для батареи, улучшений, схемы, ввода и вывода.
// Содержит логику для обработки Shift-клика и отображения прогресса сборки.

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.MachineAdvancedAssemblerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class MachineAdvancedAssemblerMenu extends AbstractContainerMenu {

    private final MachineAdvancedAssemblerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // Конструктор, вызываемый с сервера
    public MachineAdvancedAssemblerMenu(int pContainerId, Inventory pPlayerInventory, MachineAdvancedAssemblerBlockEntity pBlockEntity, ContainerData pData) {
        super(ModMenuTypes.ADVANCED_ASSEMBLY_MACHINE_MENU.get(), pContainerId);
        checkContainerSize(pPlayerInventory, 17);
        this.blockEntity = pBlockEntity;
        this.level = pPlayerInventory.player.level();
        this.data = pData;

        addPlayerInventory(pPlayerInventory);
        addPlayerHotbar(pPlayerInventory);

        // Добавляем слоты машины
        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            // Слот 0: Батарея
            this.addSlot(new SlotItemHandler(handler, 0, 152, 82));
            // Слот 1: Чертеж
            this.addSlot(new SlotItemHandler(handler, 1, 34, 126));
            // Слоты 2-3: Улучшения
            this.addSlot(new SlotItemHandler(handler, 2, 8, 18));
            this.addSlot(new SlotItemHandler(handler, 3, 8, 36));
            
            // Слоты 4-15: Входные ресурсы (сетка 3x4)
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 4; ++j) {
                    this.addSlot(new SlotItemHandler(handler, 4 + (i * 4) + j, 34 + j * 18, 18 + i * 18));
                }
            }

            // Слот 16: Выход
            this.addSlot(new SlotItemHandler(handler, 16, 124, 36));
        });

        addDataSlots(pData);
    }

    // Конструктор, вызываемый с клиента
    public MachineAdvancedAssemblerMenu(int pContainerId, Inventory pPlayerInventory, FriendlyByteBuf pExtraData) {
        this(pContainerId, pPlayerInventory, getBlockEntity(pPlayerInventory, pExtraData), new SimpleContainerData(4));
    }

    private static MachineAdvancedAssemblerBlockEntity getBlockEntity(final Inventory playerInventory, final FriendlyByteBuf data) {
        if (playerInventory.player.level().getBlockEntity(data.readBlockPos()) instanceof MachineAdvancedAssemblerBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("BlockEntity не найден!");
    }

    // --- Логика для GUI ---
    public boolean isCrafting() {
        return data.get(0) > 0;
    }
    
    public MachineAdvancedAssemblerBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public int getScaledProgress() {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);
        int progressArrowSize = 70; // Ширина полосы прогресса в пикселях

        return maxProgress != 0 && progress != 0 ? progress * progressArrowSize / maxProgress : 0;
    }

    // --- Стандартные методы ---

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), pPlayer, ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get());
    }
    
    // Логика Shift-клика
    private static final int TE_INVENTORY_SLOT_COUNT = 17;
    private static final int VANILLA_SLOT_COUNT = 36;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player playerIn, int pIndex) {
        Slot sourceSlot = this.slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (pIndex < VANILLA_SLOT_COUNT) {
            // Из инвентаря игрока в Tile Entity
            // TODO: Добавьте здесь более умную логику
            // Например, проверять, является ли предмет батареей, и перемещать его в слот 0, и т.д.
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // Из Tile Entity в инвентарь игрока
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
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


    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                // Y = 174 основано на ySize = 256 и стандартном смещении
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 174 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 232));
        }
    }
}