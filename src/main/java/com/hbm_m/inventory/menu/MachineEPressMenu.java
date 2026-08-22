package com.hbm_m.inventory.menu;

import com.hbm_m.api.energy.ItemEnergyAccess;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineEPressBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.industrial.ItemStamp;
import com.hbm_m.main.MainRegistry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?}

public class MachineEPressMenu extends AbstractContainerMenu {

    public final MachineEPressBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public MachineEPressMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(6));
    }

    public MachineEPressMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.EPRESS_MENU.get(), containerId);
        checkContainerSize(inv, 4);
        blockEntity = ((MachineEPressBlockEntity) entity);
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        var handler = this.blockEntity.getInventory();
        var container = new ModItemStackHandlerContainer(handler, this.blockEntity::setChanged);
        this.addSlot(new BatterySlot(container, 0, 26, 53));
        this.addSlot(new StampSlot(container, 1, 80, 17));
        this.addSlot(new MaterialSlot(container, 2, 80, 53));
        this.addSlot(new OutputSlot(container, 3, 139, 34));
        // Upgrade slot - the original has one (ContainerMachineEPress, SlotUpgrade); placed to fit
        // this port's own GUI layout rather than the original's coordinates.
        this.addSlot(new Slot(container, 4, 139, 53) {
            @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return stack.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade;
            }
            @Override public int getMaxStackSize() { return 1; }
        });

        addDataSlots(data);
    }

    public int getPress() {
        return this.data.get(0);
    }

    public int getMaxPress() {
        return this.data.get(1);
    }

    public int getEnergyStored() {
        return this.data.get(2);
    }

    public int getMaxEnergyStored() {
        return this.data.get(3);
    }

    public int getPressPosition() {
        return this.data.get(4);
    }

    public boolean isRetracting() {
        return this.data.get(5) == 1;
    }

    public boolean isCrafting() {
        return getPress() > 0 || isRetracting();
    }

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT = 5;

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX
                    + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            MainRegistry.LOGGER.debug("Invalid slotIndex:" + index);
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
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.EPRESS.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    private static class BatterySlot extends Slot {
        public BatterySlot(net.minecraft.world.Container itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (ItemEnergyAccess.getHbmProvider(stack).isPresent()) return true;
            //? if forge {
            return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
            //?}
            //? if fabric {
            /*return false;
            *///?}
        }
    }

    private static class StampSlot extends Slot {
        public StampSlot(net.minecraft.world.Container itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (stack.getItem() instanceof ItemStamp) {
                return true;
            }
            return stack.getItem().toString().contains("stamp");
        }
    }

    private static class MaterialSlot extends Slot {
        public MaterialSlot(net.minecraft.world.Container itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            String itemName = stack.getItem().toString();
            return itemName.contains("ingot") || itemName.contains("metal");
        }
    }

    private static class OutputSlot extends Slot {
        public OutputSlot(net.minecraft.world.Container itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
