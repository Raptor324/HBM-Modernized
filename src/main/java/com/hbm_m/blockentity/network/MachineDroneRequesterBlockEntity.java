package com.hbm_m.blockentity.network;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.request.RequestNetwork.RequestNode;
import com.hbm_m.blockentity.network.request.RequestNetworkParticipant;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.menu.MachineDroneRequesterMenu;

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
 * Drone Requester - Port von {@code TileEntityDroneRequester} (1.7.10 Original, Pipeline B). 9
 * Filter-Slots (0-8) + 9 Lager-Slots (9-17), {@link ModulePatternMatcher} entscheidet pro Filter,
 * ob der zugehoerige Lager-Slot als "ausreichend befuellt" gilt - falls nicht, wird das Filter-Item
 * dem {@link RequestNode}s Wunschzettel hinzugefuegt.
 * Wunschzettel wie im Original als {@code AStack} (RequestNetwork.RequestStack): genau, Wildcard
 * oder Ore-Dictionary-Schluessel je nach Filtermodus.
 */
public class MachineDroneRequesterBlockEntity extends BaseMachineBlockEntity {

    public static final int FILTER_START = 0;
    public static final int FILTER_END = 8;
    public static final int STOCK_START = 9;
    public static final int STOCK_END = 17;
    public static final int INVENTORY_SIZE = 18;

    private final ModulePatternMatcher matcher = new ModulePatternMatcher(9);
    private final RequestNetworkParticipant network = new RequestNetworkParticipant(this::createRequestNode);

    public MachineDroneRequesterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_REQUESTER_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineDroneRequesterBlockEntity be) {
        if (level.isClientSide) return;
        if (level.getGameTime() % 20 != 0) return;
        be.network.tick(level, pos.above(), level.hasNeighborSignal(pos));
    }

    private RequestNode createRequestNode(BlockPos pos) {
        List<com.hbm_m.blockentity.network.request.RequestNetwork.RequestStack> request = new ArrayList<>();
        for (int i = FILTER_START; i <= FILTER_END; i++) {
            ItemStack filter = inventory.getStackInSlot(i);
            ItemStack stock = inventory.getStackInSlot(i + STOCK_START);
            if (filter.isEmpty()) continue;
            String mode = matcher.getMode(i);
            com.hbm_m.blockentity.network.request.RequestNetwork.RequestStack aStack = null;

            if (ModulePatternMatcher.MODE_EXACT.equals(mode)) {
                aStack = com.hbm_m.blockentity.network.request.RequestNetwork.RequestStack.comp(filter, false);
            } else if (ModulePatternMatcher.MODE_WILDCARD.equals(mode)) {
                aStack = com.hbm_m.blockentity.network.request.RequestNetwork.RequestStack.comp(filter, true);
            } else if (mode != null) {
                aStack = com.hbm_m.blockentity.network.request.RequestNetwork.RequestStack.dict(mode);
            }

            if (aStack == null) continue;

            if (stock.isEmpty() || !matcher.isValidForFilter(filter, i, stock)) request.add(aStack);
        }
        return new RequestNode(pos, network.reachableNodes, request);
    }

    public RequestNetworkParticipant getNetwork() { return network; }
    public ModulePatternMatcher getMatcher() { return matcher; }

    /** Merges cargo into the stock slot matching {@code index}'s filter (used by EntityRequestDrone's unload step). */
    public ItemStack depositStock(ItemStack cargo) {
        // Original UNLOAD: erst gleiche Stapel (Item + Schaden) in 9-17 aufstocken, Rest in den ersten leeren Platz
        for (int i = STOCK_START; i <= STOCK_END; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == cargo.getItem() && stack.getDamageValue() == cargo.getDamageValue()) {
                int toTransfer = Math.min(stack.getMaxStackSize() - stack.getCount(), cargo.getCount());
                stack.grow(toTransfer);
                cargo.shrink(toTransfer);
            }
        }

        if (cargo.getCount() <= 0) cargo = ItemStack.EMPTY;

        if (!cargo.isEmpty()) for (int i = STOCK_START; i <= STOCK_END; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                inventory.setStackInSlot(i, cargo.copy());
                cargo = ItemStack.EMPTY;
                break;
            }
        }
        setChanged();
        return cargo;
    }

    public void nextFilterMode(int index) {
        matcher.nextMode(index, inventory.getStackInSlot(index));
        setChanged();
    }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        matcher.writeToNBT(tag);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        matcher.readFromNBT(tag);
    }

    // ── Slot validation / Menu ─────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot >= FILTER_START && slot <= FILTER_END;
    }

    public void setFilterSlot(int index, ItemStack stack) {
        inventory.setStackInSlot(index, stack);
        matcher.initPattern(index, stack);
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.drone_crate_requester");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineDroneRequesterMenu.create(id, inventory, this);
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slots {9-17}; nichts hinein, alles heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return com.hbm_m.blockentity.SidedItemAccess.range(9, 17); }
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
    /*/^* Original {@code ISidedInventory}: Slots {9-17}; nichts hinein, alles heraus. ^/
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return com.hbm_m.blockentity.SidedItemAccess.range(9, 17); }
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
