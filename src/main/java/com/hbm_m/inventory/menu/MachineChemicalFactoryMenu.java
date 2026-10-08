package com.hbm_m.inventory.menu;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineChemicalFactoryBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.industrial.ItemBlueprints;
import com.hbm_m.item.industrial.ItemMachineUpgrade;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code ContainerMachineChemicalFactory}: Menue-Slotindex == Tile-Slotindex (32), dann Spielerinventar bei (26,134).
 */
public class MachineChemicalFactoryMenu extends AbstractContainerMenu {

    private static final int LANE_COUNT = MachineChemicalFactoryBlockEntity.LANE_COUNT;
    private static final int TE_SLOT_COUNT = MachineChemicalFactoryBlockEntity.SLOT_COUNT;

    private final MachineChemicalFactoryBlockEntity blockEntity;
    private final ContainerData data;

    public MachineChemicalFactoryMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(LANE_COUNT * 2 + 5));
    }

    public MachineChemicalFactoryMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.CHEMICAL_FACTORY_MENU.get(), id);
        this.blockEntity = (MachineChemicalFactoryBlockEntity) entity;
        this.data = data;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        // Battery
        addSlot(new Slot(container, 0, 224, 88));
        // Upgrades
        for (int row = 0; row < 3; row++) addSlot(new Slot(container, 1 + row, 206, 125 + row * 18));

        for (int i = 0; i < LANE_COUNT; i++) {
            // Template
            addSlot(new Slot(container, 4 + i * 7, 93, 20 + i * 22));
            // Solid Input
            for (int col = 0; col < 3; col++) addSlot(new Slot(container, 5 + i * 7 + col, 10 + col * 16, 20 + i * 22));
            // Solid Output
            for (int col = 0; col < 3; col++) addSlot(new Slot(container, 8 + i * 7 + col, 139 + col * 16, 20 + i * 22) {
                @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 26 + col * 18, 134 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 26 + col * 18, 192));
        }

        addDataSlots(data);
    }

    public MachineChemicalFactoryBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getLaneProgress(int lane) { return data.get(lane); }
    public int getLaneMaxProgress(int lane) { return data.get(LANE_COUNT + lane); }

    public long getEnergyStored() {
        return ((long) data.get(LANE_COUNT * 2 + 1) << 32) | (data.get(LANE_COUNT * 2) & 0xFFFFFFFFL);
    }

    public long getMaxEnergyStored() {
        return ((long) data.get(LANE_COUNT * 2 + 3) << 32) | (data.get(LANE_COUNT * 2 + 2) & 0xFFFFFFFFL);
    }

    public boolean getLaneDidProcess(int lane) {
        return (data.get(LANE_COUNT * 2 + 4) & (1 << lane)) != 0;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack slotOriginal = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return slotOriginal;

        ItemStack slotStack = slot.getItem();
        slotOriginal = slotStack.copy();

        if (index <= TE_SLOT_COUNT - 1) {
            if (!moveItemStackTo(slotStack, TE_SLOT_COUNT, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(slotOriginal).isPresent()
                    || com.hbm_m.api.energy.ItemEnergyAccess.getHbmReceiver(slotOriginal).isPresent()
                    || slotOriginal.getItem() instanceof ItemCreativeBattery) {
                if (!moveItemStackTo(slotStack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (slotOriginal.getItem() instanceof ItemBlueprints) {
                // Original: nur die Mappe des ersten Moduls (die drei weiteren Zweige sind dort unerreichbar)
                if (!moveItemStackTo(slotStack, 4, 5, false)) return ItemStack.EMPTY;
            } else if (slotOriginal.getItem() instanceof ItemMachineUpgrade) {
                if (!moveItemStackTo(slotStack, 1, 4, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(slotStack, 5, 8, false)
                        && !moveItemStackTo(slotStack, 12, 15, false)
                        && !moveItemStackTo(slotStack, 19, 22, false)
                        && !moveItemStackTo(slotStack, 26, 29, false)) return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, slotStack);
        return slotOriginal;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        // w16b: Original isUseableByPlayer (TileEntityMachineBase) = 128 vom Kern, dazu Huelle der Maschine (MultiblockMenuReach)
        return MultiblockMenuReach.stillValidCore(blockEntity, player, 128.0D);
    }
}
