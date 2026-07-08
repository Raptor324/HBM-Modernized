package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Solar Mirror BlockEntity - a passive single-block "marker" that contributes to
 * nearby Solar Boilers when it has direct sky access.
 *
 * The actual heat contribution is computed by the Solar Boiler itself, which scans
 * its surroundings for Solar Mirror blocks with sky access. This block entity has no
 * inventory, no GUI, and only tracks its own sky-access state for visual/debug purposes.
 */
public class MachineSolarMirrorsBlockEntity extends BaseMachineBlockEntity {

    /** How often (in ticks) the sky-access state is re-checked. */
    private static final int CHECK_INTERVAL = 20;

    private boolean hasSkyAccess = false;

    public MachineSolarMirrorsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_MIRRORS_BE.get(), pos, state, 0, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSolarMirrorsBlockEntity be) {
        if (level.isClientSide()) return;

        // Lightweight periodic sky-access check; the boiler does the actual scanning.
        if (level.getGameTime() % CHECK_INTERVAL != 0) return;

        boolean nowSkyAccess = level.canSeeSky(pos);
        if (nowSkyAccess != be.hasSkyAccess) {
            be.hasSkyAccess = nowSkyAccess;
            be.setChanged();
        }
    }

    public boolean hasSkyAccess() {
        return hasSkyAccess;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("HasSkyAccess", hasSkyAccess);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        hasSkyAccess = tag.getBoolean("HasSkyAccess");
    }

    @Override public Component getDisplayName() { return Component.translatable("container.hbm_m.solar_mirrors"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; }
    @Override protected Component getDefaultName() { return Component.translatable("container.hbm_m.solar_mirrors"); }
    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }
}
