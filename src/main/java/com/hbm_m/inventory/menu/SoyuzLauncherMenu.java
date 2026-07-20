package com.hbm_m.inventory.menu;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.item.IDesignatorItem;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.SoyuzLauncherBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;

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
 * Menu for the Soyuz Launcher - slot layout is a direct port of legacy
 * {@code ContainerSoyuzLauncher} (same pixel coordinates).
 */
public class SoyuzLauncherMenu extends AbstractContainerMenu {

    private static final int TE_SLOT_COUNT = SoyuzLauncherBlockEntity.SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = TE_SLOT_COUNT;
    private static final int VANILLA_SLOT_COUNT = 36;

    private final SoyuzLauncherBlockEntity blockEntity;
    private final ContainerData data;

    public SoyuzLauncherMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, (SoyuzLauncherBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(5));
    }

    public SoyuzLauncherMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.SOYUZ_LAUNCHER_MENU.get(), id);
        this.blockEntity = (SoyuzLauncherBlockEntity) entity;
        this.data = data;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        // Rocket
        addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_ROCKET, 62, 18) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(SoyuzLauncherBlockEntity.rocketItem());
            }
        });
        // Designator
        addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_DESIGNATOR, 62, 36) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof IDesignatorItem;
            }
        });
        // Satellite payload
        addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_SATELLITE, 116, 18));
        // Landing module
        addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_LANDER, 116, 36) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModItems.MISSILE_SOYUZ_LANDER.get());
            }
        });
        // Kerosene in/out
        addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_FUEL_IN, 8, 90));
        addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_FUEL_OUT, 8, 108));
        // Oxygen in/out
        addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_OXIDIZER_IN, 26, 90));
        addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_OXIDIZER_OUT, 26, 108));
        // Battery
        addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_BATTERY, 44, 108) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return isEnergyProviderItem(stack) || stack.getItem() instanceof ItemCreativeBattery;
            }
        });
        // Cargo grid (18 slots, 6x3)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                int idx = SoyuzLauncherBlockEntity.CARGO_START + col + row * 6;
                addSlot(new Slot(container, idx, 62 + col * 18, 72 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 198));
        }

        addDataSlots(data);
    }

    private static boolean isEnergyProviderItem(ItemStack stack) {
        return com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(stack).isPresent();
    }

    public SoyuzLauncherBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public long getEnergyStored() {
        return ((long) data.get(1) << 32) | (data.get(0) & 0xFFFFFFFFL);
    }

    public long getMaxEnergyStored() {
        return blockEntity.getMaxEnergyStored();
    }

    public int getMode() {
        return data.get(2);
    }

    public int getCountdown() {
        return data.get(3);
    }

    public boolean isStarting() {
        return data.get(4) != 0;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < TE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (stack.is(SoyuzLauncherBlockEntity.rocketItem())) {
                if (!moveItemStackTo(stack, SoyuzLauncherBlockEntity.SLOT_ROCKET, SoyuzLauncherBlockEntity.SLOT_ROCKET + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.getItem() instanceof IDesignatorItem) {
                if (!moveItemStackTo(stack, SoyuzLauncherBlockEntity.SLOT_DESIGNATOR, SoyuzLauncherBlockEntity.SLOT_DESIGNATOR + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(ModItems.MISSILE_SOYUZ_LANDER.get())) {
                if (!moveItemStackTo(stack, SoyuzLauncherBlockEntity.SLOT_LANDER, SoyuzLauncherBlockEntity.SLOT_LANDER + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (isEnergyProviderItem(stack) || stack.getItem() instanceof ItemCreativeBattery) {
                if (!moveItemStackTo(stack, SoyuzLauncherBlockEntity.SLOT_BATTERY, SoyuzLauncherBlockEntity.SLOT_BATTERY + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, SoyuzLauncherBlockEntity.CARGO_START, SoyuzLauncherBlockEntity.CARGO_END + 1, false)) {
                if (!moveItemStackTo(stack, SoyuzLauncherBlockEntity.SLOT_SATELLITE, SoyuzLauncherBlockEntity.SLOT_SATELLITE + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.SOYUZ_LAUNCHER.get());
    }
}
