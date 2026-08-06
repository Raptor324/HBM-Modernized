package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.OrbusMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityMachineOrbus} (1.7.10 Original) - a large fluid tank
 * ({@code TileEntityBarrel} subclass in the original, 512,000 mB). Identical logic to
 * {@link MachineFluidTankBlockEntity} (fill/drain, mode, explosion, MK2 network participation);
 * only the capacity and registration differ, same pattern as {@link Bat9000BlockEntity}.
 * <p>
 * SCOPE-Vereinfachung: Das Original ist ein {@code BlockDummyable}-Multiblock ({@code {4,0,2,1,2,1}}) -
 * hier reines Einzelblock ueber {@link com.hbm_m.block.machines.BarrelTankBlock} (wie bereits bei
 * Iron/Steel-Fass dieses Ports), statt der originalen mehrblockigen Struktur.
 */
public class OrbusBlockEntity extends MachineFluidTankBlockEntity {

    public static final int CAPACITY = 512_000;

    public OrbusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORBUS_BE.get(), pos, state, CAPACITY);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(ModBlocks.ORBUS.get().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new OrbusMenu(id, inventory, this, this.data);
    }
}
