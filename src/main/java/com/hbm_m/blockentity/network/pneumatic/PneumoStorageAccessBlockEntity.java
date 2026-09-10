package com.hbm_m.blockentity.network.pneumatic;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.PneumoStorageAccessMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code TileEntityPneumoStorageAccess} (1.7.10): das Zugangsterminal.
 *
 * <p>Es lagert selbst gar nichts. Es haelt nur ein {@link com.hbm_m.api.pneumatic.StackCache
 * Verzeichnis} ueber alles, was im Rohrnetz in Reichweite liegt, und zeigt das als eine einzige
 * durchsuchbare Liste - Plaetze gleicher Art aus verschiedenen Lagern erscheinen darin als ein
 * Eintrag mit der Gesamtmenge.</p>
 *
 * <p>Wieviel es sieht, bestimmen die Lager: jedes von ihnen hat eine eigene Druckstufe und damit
 * eine eigene Reichweite. Ein Lager ohne Druckluft ist fuer das Terminal unsichtbar.</p>
 */
public class PneumoStorageAccessBlockEntity extends PneumaticMachineBlockEntity {

    public PneumoStorageAccessBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PNEUMO_STORAGE_ACCESS_BE.get(), pos, state, 0);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PneumoStorageAccessBlockEntity be) {
        if (level.isClientSide()) return;
        be.tickNetwork(level, pos);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.pneumatic_storage_access");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PneumoStorageAccessMenu(id, inv, this);
    }
}
