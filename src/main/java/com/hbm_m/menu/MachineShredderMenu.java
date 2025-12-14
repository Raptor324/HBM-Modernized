package com.hbm_m.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.custom.machines.MachineShredderBlockEntity;
import com.hbm_m.item.custom.industrial.ItemBlades;
import com.hbm_m.util.LongDataPacker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class MachineShredderMenu extends AbstractContainerMenu {

    private final MachineShredderBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // Индексы слотов (как в оригинале: 0-8 вход, 9-26 выход, 27-28 лезвия, 29 батарея)
    private static final int INPUT_SLOTS = 9;
    private static final int OUTPUT_SLOTS = 18;
    private static final int BLADE_SLOTS = 2;
    private static final int BATTERY_SLOT = 29;
    private static final int BLADE_LEFT = 27;
    private static final int BLADE_RIGHT = 28;

    public MachineShredderMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData), new SimpleContainerData(8));
    }

    public MachineShredderMenu(int containerId, Inventory playerInventory, MachineShredderBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity, blockEntity.getContainerData());
    }

    public MachineShredderMenu(int containerId, Inventory playerInventory, MachineShredderBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.SHREDDER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.level = playerInventory.player.level();
        this.data = data;
        addDataSlots(data);

        ItemStackHandler itemHandler = this.blockEntity.getInventory();

        // Входные слоты (3x3 сетка) - верхняя левая часть GUI (0-8)
        int startX = 44;
        int startY = 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                this.addSlot(new SlotItemHandler(itemHandler, index,
                        startX + col * 18, startY + row * 18));
            }
        }

        // Выходные слоты (3x6 сетка) - справа (9-26)
        int outputX = 116;
        int outputY = 18;
        for (int col = 0; col < 6; col++) {
            for (int row = 0; row < 3; row++) {
                int index = col * 3 + row;
                this.addSlot(new SlotItemHandler(itemHandler, INPUT_SLOTS + index,
                        outputX + row * 18, outputY + col * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false; // Нельзя помещать предметы в выходные слоты
                    }
                });
            }
        }

        // Слоты для лезвий (2 слота) - внизу слева (27-28)
        int bladeX = 44;
        int bladeY = 108;
        this.addSlot(new SlotItemHandler(itemHandler, BLADE_LEFT, bladeX, bladeY));
        this.addSlot(new SlotItemHandler(itemHandler, BLADE_RIGHT, bladeX + 36, bladeY));

        // Слот для батареи (29) - внизу слева, под лезвиями
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, 8, 108) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // Разрешаем класть только предметы с энергетической capability
                return stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).isPresent();
            }
        });

        // Инвентарь игрока
        int playerInvX = 8;
        int playerInvY = 151; // Стандартное положение относительно верха GUI

        // Основной инвентарь (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        playerInvX + col * 18, playerInvY + row * 18));
            }
        }

        // Хотбар (1x9)
        int hotbarY = 209; // Стандартное положение относительно верха GUI
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                    playerInvX + col * 18, hotbarY));
        }
    }

    private static MachineShredderBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf data) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(data.readBlockPos());
        if (blockEntity instanceof MachineShredderBlockEntity shredder) {
            return shredder;
        }
        throw new IllegalStateException("BlockEntity is not a MachineShredder");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack slotStack = slot.getItem();
        itemstack = slotStack.copy();

        int containerSlots = INPUT_SLOTS + OUTPUT_SLOTS + BLADE_SLOTS + 1; // +1 для батареи
        int playerInventoryStart = containerSlots;
        int playerInventoryEnd = this.slots.size();

        // Из контейнера в инвентарь игрока
        if (index < containerSlots) {
            if (!this.moveItemStackTo(slotStack, playerInventoryStart, playerInventoryEnd, true)) {
                return ItemStack.EMPTY;
            }
        }
        // Из инвентаря игрока в контейнер
        else if (index >= playerInventoryStart && index < playerInventoryEnd) {
            // Проверяем тип предмета
            boolean isBlade = slotStack.getItem() instanceof ItemBlades;
            boolean isBattery = slotStack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).isPresent();

            boolean moved = false;
            
            if (isBattery) {
                // Пытаемся поместить в слот для батареи
                moved = this.moveItemStackTo(slotStack, BATTERY_SLOT, BATTERY_SLOT + 1, false);
            }
            
            if (!moved && isBlade) {
                // Пытаемся поместить в слоты для лезвий
                moved = this.moveItemStackTo(slotStack, BLADE_LEFT, BLADE_RIGHT + 1, false);
            }
            
            if (!moved) {
                // Пытаемся поместить во входные слоты (разрешаем все предметы, кроме лезвий)
                moved = this.moveItemStackTo(slotStack, 0, INPUT_SLOTS, false);
            }
            
            if (!moved) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, slotStack);
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.SHREDDER.get());
    }

    public MachineShredderBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public boolean isCrafting() {
        return data.get(0) > 0;
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getMaxProgress() {
        return data.get(1);
    }

    public int getScaledProgress(int width) {
        int progress = getProgress();
        int maxProgress = getMaxProgress();
        return maxProgress == 0 ? 0 : progress * width / maxProgress;
    }

    public long getEnergyLong() {
        return LongDataPacker.unpack(data.get(2), data.get(3));
    }

    public long getMaxEnergyLong() {
        return LongDataPacker.unpack(data.get(4), data.get(5));
    }

    public long getEnergyDeltaLong() {
        return LongDataPacker.unpack(data.get(6), data.get(7));
    }
}