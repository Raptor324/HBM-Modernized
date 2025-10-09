package com.hbm_m.menu;

// Меню для продвинутой сборочной машины.
// Имеет слоты для батареи, улучшений, схемы, ввода и вывода.
// Содержит логику для обработки Shift-клика и отображения прогресса сборки.

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.item.ItemBlueprintFolder;
import com.hbm_m.item.ItemTemplateFolder;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

public class MachineAdvancedAssemblerMenu extends AbstractContainerMenu {

    private final MachineAdvancedAssemblerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    private int lastProgress = 0;
    private int lastMaxProgress = 0;
    private int lastEnergy = 0;
    private int lastMaxEnergy = 0;

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
            this.addSlot(new SlotItemHandler(handler, 0, 152, 81) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    // Проверяем наличие энергетической capability
                    return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
                }
                
                @Override
                public int getMaxStackSize() {
                    return 1; // Батареи не стакаются
                }
            });

            // Слот 1: Папка шаблонов
            this.addSlot(new SlotItemHandler(handler, 1, 34, 126) {
                @Override
                public boolean mayPlace(@Nonnull ItemStack stack) {
                    return stack.getItem() instanceof ItemBlueprintFolder;
                }
                
                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
            
            // Слоты 2-3: Улучшения
            this.addSlot(new SlotItemHandler(handler, 2, 152, 108));
            this.addSlot(new SlotItemHandler(handler, 3, 152, 126));
            
            // Слоты 4-15: Входные ресурсы (сетка 4x3)
            for (int i = 0; i < 4; ++i) {
                for (int j = 0; j < 3; ++j) {
                    this.addSlot(new SlotItemHandler(handler, 4 + (i * 3) + j, 8 + j * 18, 18 + i * 18));
                }
            }

            // Слот 16: Выход
            this.addSlot(new SlotItemHandler(handler, 16, 98, 45){
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false; // Запрещаем размещение предметов игроком
                }
            });
        });

        addDataSlots(pData);
    }

    // Конструктор, вызываемый с клиента
    public MachineAdvancedAssemblerMenu(int pContainerId, Inventory pPlayerInventory, FriendlyByteBuf pExtraData) {
        this(pContainerId, pPlayerInventory, getBlockEntity(pPlayerInventory, pExtraData), new SimpleContainerData(5));
    }

    private static MachineAdvancedAssemblerBlockEntity getBlockEntity(final Inventory playerInventory, final FriendlyByteBuf data) {
        if (playerInventory.player.level().getBlockEntity(data.readBlockPos()) instanceof MachineAdvancedAssemblerBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("BlockEntity не найден!");
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
    }

    // --- Логика для GUI ---
    public boolean isCrafting() {
        return data.get(0) > 0;
    }

    public int getEnergyDelta() {
        return this.data.get(4);
    }

    public MachineAdvancedAssemblerBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public int getScaledProgress() {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);
        int progressArrowSize = 70;
        return maxProgress != 0 && progress != 0 ? progress * progressArrowSize / maxProgress : 0;
    }

    public int getProgress() {
        return this.data.get(0);
    }

    public int getMaxProgress() {
        return this.data.get(1);
    }

    public int getEnergy() {
        return this.data.get(2);
    }

    public int getMaxEnergy() {
        return this.data.get(3);
    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), 
                        pPlayer, ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get());
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
            // Из инвентаря игрока в машину
            
            // Умная логика: батареи в слот 0, папки шаблонов в слот 1
            if (sourceStack.getCapability(ForgeCapabilities.ENERGY).isPresent()) {
                // Это батарея - в слот 0
                if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (sourceStack.getItem() instanceof ItemTemplateFolder) {
                // Это папка шаблонов - в слот 1
                if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX + 1, TE_INVENTORY_FIRST_SLOT_INDEX + 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Остальное - в входные слоты (4-15) и слоты улучшений (2-3)
                if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX + 2, 
                                    TE_INVENTORY_FIRST_SLOT_INDEX + 16, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // Из машины в инвентарь игрока
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, 
                                VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
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