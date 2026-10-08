package com.hbm_m.blockentity.network;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.request.RequestNetwork.OfferNode;
import com.hbm_m.blockentity.network.request.RequestNetworkParticipant;
import com.hbm_m.inventory.menu.MachineDroneProviderMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Drone Provider - Port von {@code TileEntityDroneProvider} (1.7.10 Original, Pipeline B). Reiner
 * Item-Vorrat: 9 Slots, kein Filter, kein Hopper-Auszug ({@code canExtractItem=false} im Original -
 * nur {@link com.hbm_m.entity.drone.EntityRequestDrone}s duerfen programmatisch entnehmen).
 */
public class MachineDroneProviderBlockEntity extends BaseMachineBlockEntity {

    public static final int INVENTORY_SIZE = 9;

    private final RequestNetworkParticipant network = new RequestNetworkParticipant(this::createOfferNode);

    public MachineDroneProviderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_PROVIDER_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineDroneProviderBlockEntity be) {
        if (level.isClientSide) return;
        if (level.getGameTime() % 20 != 0) return;
        be.network.tick(level, pos.above(), level.hasNeighborSignal(pos));
    }

    private OfferNode createOfferNode(BlockPos pos) {
        List<ItemStack> offer = new ArrayList<>();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) offer.add(stack.copy());
        }
        return new OfferNode(pos, network.reachableNodes, offer);
    }

    public RequestNetworkParticipant getNetwork() { return network; }

    /** Pulls up to {@code amount} of the first slot matching {@code pattern} (used by EntityRequestDrone's pickup step). */
    public ItemStack extractMatching(com.hbm_m.blockentity.network.request.RequestNetwork.RequestStack pattern) {
        // Original: der ganze passende Stapel wird mitgenommen
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty() || !pattern.matches(stack)) continue;

            ItemStack taken = stack.copy();
            inventory.setStackInSlot(i, ItemStack.EMPTY);
            setChanged();
            return taken;
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.drone_crate_provider");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineDroneProviderMenu.create(id, inventory, this);
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slots {0-8}; alles hinein, nichts heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return com.hbm_m.blockentity.SidedItemAccess.range(0, 8); }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return true; }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return false; }
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
    //?}
}
