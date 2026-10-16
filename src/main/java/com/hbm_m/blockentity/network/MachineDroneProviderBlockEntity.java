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
    public ItemStack extractMatching(ItemStack pattern, int amount) {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty() || !com.hbm_m.platform.PlatformHooks.isSameItemSameTags(stack, pattern)) continue;

            int toTake = Math.min(amount, stack.getCount());
            ItemStack taken = stack.copy();
            taken.setCount(toTake);
            stack.shrink(toTake);
            if (stack.isEmpty()) inventory.setStackInSlot(i, ItemStack.EMPTY);
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
}
