package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineTapeDriveBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineTapeDriveMenu;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.satellite.Satellite;
import com.hbm_m.satellite.SatelliteManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityMachineTapeDrive} (Laufwerksgehaeuse): zwoelf Laufwerksplaetze (je ein Stueck). Alle 10 Ticks
 * fragt es den Satelliten-Uplink an seiner Rueckseite; hat dessen Satellit Daten, wird das erste passende leere
 * Laufwerk beschrieben. Leere Laufwerke lassen sich nicht automatisch entnehmen.
 */
public class MachineTapeDriveBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_COUNT = 12;

    public static final byte SLOT_EMPTY = 0;
    public static final byte SLOT_ANY = 1;
    public static final byte SLOT_EMPTY_TAPE = 2;
    public static final byte SLOT_FILLED_TAPE = 3;

    public MachineTapeDriveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TAPE_DRIVE_BE.get(), pos, state, SLOT_COUNT, 0L, 0L, 0L);
    }

    @Override
    protected ModItemStackHandler createInventoryHandler(int size) {
        return new ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                sendUpdateToClient();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return isItemValidForSlot(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
    }

    public static boolean isDrive(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().startsWith("drive_");
    }

    private static String path(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineTapeDriveBlockEntity be) {
        if (level instanceof ServerLevel server && server.getGameTime() % 10 == 0) be.update(server);
    }

    private void update(ServerLevel world) {
        Direction facing = getBlockState().hasProperty(MachineTapeDriveBlock.FACING) ? getBlockState().getValue(MachineTapeDriveBlock.FACING) : Direction.NORTH;
        BlockEntity connected = world.getBlockEntity(worldPosition.relative(facing.getOpposite()));

        if (connected instanceof com.hbm_m.interfaces.IMultiblockPart part && part.getControllerPos() != null) {
            connected = world.getBlockEntity(part.getControllerPos());
        }

        if (!(connected instanceof MachineSatLinkBlockEntity link) || !link.connected) return;

        SatelliteManager data = SatelliteManager.get(world);
        Satellite satellite = data.getSatFromFreq(link.freq);
        if (satellite == null || !satellite.hasData(world)) {
            if (satellite != null && satellite.isDirty) data.setDirty();
            return;
        }

        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!isDrive(stack)) continue;
            Item ret = satellite.getOutputData(stack);
            if (ret != null) {
                satellite.consumeData();
                inventory.setStackInSlot(i, new ItemStack(ret));
                data.setDirty();
                break;
            }
        }
        if (satellite.isDirty) data.setDirty();
    }

    /** Original {@code serialize}: Zustand je Platz fuer die Lampen am Gehaeuse. */
    public byte getTapeState(int i) {
        ItemStack stack = inventory.getStackInSlot(i);
        if (stack.isEmpty()) return SLOT_EMPTY;
        if (!isDrive(stack)) return SLOT_ANY;
        String p = path(stack);
        if (p.equals("drive_disk_empty") || p.equals("drive_flash_empty")) return SLOT_EMPTY_TAPE;
        if (p.equals("drive_disk_broken") || p.equals("drive_flash_broken")) return SLOT_ANY;
        return SLOT_FILLED_TAPE;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return isDrive(stack);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_tape_drive");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineTapeDriveMenu(containerId, playerInventory, this);
    }

    //? if forge {
    /** Original {@code ISidedInventory}: alle Plaetze; leere Laufwerke bleiben drin. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(Direction side) { return com.hbm_m.blockentity.SidedItemAccess.range(0, SLOT_COUNT - 1); }
                @Override public boolean canInsert(int slot, ItemStack stack, Direction side) { return isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, ItemStack stack, Direction side) {
                    String p = path(stack);
                    return !p.equals("drive_disk_empty") && !p.equals("drive_flash_empty");
                }
            });

    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sidedItems.invalidate();
    }
    //?}
}
