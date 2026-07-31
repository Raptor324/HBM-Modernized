package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
public class MachineSatLinkerBlockEntity extends BlockEntity implements MenuProvider {

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

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(tag.getCompound("inventory"));
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
}
