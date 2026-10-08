package com.hbm_m.inventory.menu;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.item.IDesignatorItem;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.SoyuzLauncherBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.platform.DummyItemStackHandler;

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
        this(id, inv, getBlockEntity(inv, extraData), new SimpleContainerData(5));
    }

    private static SoyuzLauncherBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf extraData) {
        BlockEntity blockEntity = inv.player.level().getBlockEntity(extraData.readBlockPos());
        if (blockEntity instanceof SoyuzLauncherBlockEntity launcher) return launcher;
        // На клиенте тайл может отсутствовать (реплей Flashback) — возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (inv.player.level().isClientSide) return null;
        throw new IllegalStateException("BlockEntity is not a SoyuzLauncherBlockEntity");
    }

    public SoyuzLauncherMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.SOYUZ_LAUNCHER_MENU.get(), id);
        this.blockEntity = entity instanceof SoyuzLauncherBlockEntity launcher ? launcher : null;
        this.data = data;

        // тайл может отсутствовать на клиенте (реплей Flashback) — подставляем пустую заглушку
        var container = this.blockEntity != null
                ? new ModItemStackHandlerContainer(this.blockEntity.getInventory(), this.blockEntity::setChanged)
                : new ModItemStackHandlerContainer(new DummyItemStackHandler(SoyuzLauncherBlockEntity.SLOT_COUNT), () -> {});

        // Rocket
        addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_ROCKET, 98, 80) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return (stack.getItem() instanceof com.hbm_m.item.special.ItemSoyuz);
            }
        });
        // Designator
        addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_DESIGNATOR, 80, 80) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof IDesignatorItem;
            }
        });
        // Satellite payload
        addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_SATELLITE, 98, 26));
        // Landing module
        addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_LANDER, 80, 26) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModItems.MISSILE_SOYUZ_LANDER.get());
            }
        });
        // Kerosene in/out
        addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_FUEL_IN, 152, 98));
        addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_FUEL_OUT, 152, 116));
        // Oxygen in/out
        addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_OXIDIZER_IN, 170, 98));
        addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_OXIDIZER_OUT, 170, 116));
        // Battery
        addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_BATTERY, 134, 98) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return isEnergyProviderItem(stack) || stack.getItem() instanceof ItemCreativeBattery;
            }
        });
        // Fracht: drei Spalten zu sechs, von rechts nach links (Original ContainerSoyuzLauncher)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 6; j++) {
                addSlot(new Slot(container, j + i * 6 + SoyuzLauncherBlockEntity.CARGO_START, 44 - i * 18, 26 + j * 18));
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(inv, j + i * 9 + 9, 17 + j * 18, 162 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inv, i, 17 + i * 18, 220));
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
        // тайл может отсутствовать на клиенте (реплей Flashback)
        return blockEntity != null ? blockEntity.getMaxEnergyStored() : 0L;
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
        } else if (!moveItemStackTo(stack, SoyuzLauncherBlockEntity.SLOT_ROCKET, SoyuzLauncherBlockEntity.SLOT_ROCKET + 1, false)) {
            // Original: Shift-Klick aus dem Inventar geht nur in den Raketenplatz
            return ItemStack.EMPTY;
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
        // тайл может отсутствовать на клиенте (реплей Flashback)
        if (blockEntity == null) {
            return false;
        }
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.SOYUZ_LAUNCHER.get());
    }
}
