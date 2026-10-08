package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineSatLinkerMenu;
import com.hbm_m.item.ISatChip;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.satellite.SatelliteManager;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of legacy {@code TileEntityMachineSatLinker}: slot 0+1 copies slot 0's satellite chip
 * frequency onto slot 1 (so a designator can be matched to a payload before launch); slot 2
 * assigns a fresh random unused frequency to whatever chip is placed there.
 */
public class MachineSatLinkerBlockEntity extends BaseHbmBlockEntity implements MenuProvider {

    public static final int SLOT_COPY_SOURCE = 0;
    public static final int SLOT_COPY_TARGET = 1;
    public static final int SLOT_RANDOMIZE = 2;
    public static final int SLOT_COUNT = 3;

    private final ModItemStackHandler inventory = new ModItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public MachineSatLinkerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_SATLINKER_BE.get(), pos, state);
    }

    public ModItemStackHandler getInventory() {
        return inventory;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineSatLinkerBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        ItemStack source = be.inventory.getStackInSlot(SLOT_COPY_SOURCE);
        ItemStack target = be.inventory.getStackInSlot(SLOT_COPY_TARGET);
        if (source.getItem() instanceof ISatChip && target.getItem() instanceof ISatChip) {
            ISatChip.setFreqS(target, ISatChip.getFreqS(source));
        }

        ItemStack toRandomize = be.inventory.getStackInSlot(SLOT_RANDOMIZE);
        if (toRandomize.getItem() instanceof ISatChip) {
            SatelliteManager manager = SatelliteManager.get(server);
            int freq = server.getRandom().nextInt(100_000);
            if (!manager.isFreqTaken(freq)) {
                ISatChip.setFreqS(toRandomize, freq);
            }
        }
    }

    
    // (устраняет вложенный stonecutter-баг в load())
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.put("inventory", com.hbm_m.platform.ItemStackSerialization.serialize(inventory, registries));
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("inventory")) {
            com.hbm_m.platform.ItemStackSerialization.deserialize(inventory, tag.getCompound("inventory"), registries);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.machine_satlinker");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new MachineSatLinkerMenu(containerId, playerInventory, this);
    }

    //? if forge {
    /** Original {@code ISidedInventory}: unten {1}, oben {0}, Seiten {2}; nichts hinein, alles heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return side == net.minecraft.core.Direction.DOWN ? new int[] { 1 } : side == net.minecraft.core.Direction.UP ? new int[] { 0 } : new int[] { 2 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return false; }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return true; }
            });

    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@org.jetbrains.annotations.NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sidedItems.invalidate();
    }
    //?} elif neoforge {
    /*/^* Original {@code ISidedInventory}: unten {1}, oben {0}, Seiten {2}; nichts hinein, alles heraus. ^/
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return side == net.minecraft.core.Direction.DOWN ? new int[] { 1 } : side == net.minecraft.core.Direction.UP ? new int[] { 0 } : new int[] { 2 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return false; }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return true; }
            });

    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        sidedItems.invalidate();
    }
    *///?}
}
