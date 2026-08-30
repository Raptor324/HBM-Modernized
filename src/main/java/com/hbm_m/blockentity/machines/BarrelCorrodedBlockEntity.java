package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.FluidTankMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Corroded Barrel BlockEntity - only ever exists as the runtime result of an Iron Barrel decaying
 * (see {@link BarrelIronBlockEntity#handleIncompatibleFluid}), not as something you can craft or
 * place directly (matches the original 1.7.10 {@code BlockFluidBarrel}, whose
 * {@code createNewTileEntity} is hard-coded to return {@code null} specifically for
 * {@code barrel_corroded} - i.e. a freshly-placed corroded barrel there is purely decorative).
 * <p>
 * Reuses the base tank's generic "exploded" leak system ({@link MachineFluidTankBlockEntity#hasExploded})
 * permanently switched on, with a faster material-specific decay rate: roughly a third of the
 * remaining fluid is lost every second, plus a small per-second chance of the barrel being destroyed
 * outright (no drops, no explosion effect) - both 1:1 with the original's
 * {@code TileEntityBarrel#checkFluidInteraction}'s {@code barrel_corroded} branch.
 */
public class BarrelCorrodedBlockEntity extends MachineFluidTankBlockEntity {

    /** Matches the iron barrel it decays from. */
    public static final int CAPACITY = BarrelIronBlockEntity.CAPACITY;

    /** Average real-time interval between destruction rolls, in seconds (matches the original's
     *  {@code nextInt(3 * 60 * 20)} per-tick roll, adapted to our once-per-second decay cadence). */
    private static final int DESTROY_CHANCE_SECONDS = 180;

    public BarrelCorrodedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BARREL_CORRODED_BE.get(), pos, state, CAPACITY);
        this.hasExploded = true;
    }

    @Override
    protected int calculateLeakAmount() {
        if (level == null || level.getGameTime() % 20 != 0) return 0;
        return Math.max(1, getFluidTank().getFill() / 3);
    }

    @Override
    protected void updateLeak(int amount) {
        super.updateLeak(amount);
        if (level == null || level.isClientSide || level.getGameTime() % 20 != 0) return;
        if (level.random.nextInt(DESTROY_CHANCE_SECONDS) == 0) {
            level.destroyBlock(worldPosition, false);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(ModBlocks.BARREL_CORRODED.get().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FluidTankMenu(id, inventory, this, this.data);
    }
}
