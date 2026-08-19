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
 * <p>
 * SCOPE-Vereinfachung: Das Original nutzt {@code AStack}/{@code ComparableStack}/{@code OreDictStack}
 * fuer den Wunschzettel-Eintrag. Hier: einfache {@link ItemStack}-Repraesentanten (1 Stueck), Matching
 * erfolgt weiterhin ueber {@link ModulePatternMatcher} beim Provider-Pairing in
 * {@link MachineDroneDockBlockEntity} - der Kernmechanismus (Filter-Modus-abhaengiges Matching)
 * bleibt vollstaendig erhalten, nur die AStack-Zwischenschicht entfaellt.
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
        List<ItemStack> request = new ArrayList<>();
        for (int i = FILTER_START; i <= FILTER_END; i++) {
            ItemStack filter = inventory.getStackInSlot(i);
            if (filter.isEmpty()) continue;
            String mode = matcher.getMode(i);
            if (mode == null) continue;

            ItemStack stock = inventory.getStackInSlot(i + STOCK_START);
            boolean sufficient = !stock.isEmpty() && matcher.isValidForFilter(filter, i, stock)
                    && stock.getCount() >= filter.getCount();
            if (!sufficient) {
                ItemStack representative = filter.copy();
                representative.setCount(1);
                request.add(representative);
            }
        }
        return new RequestNode(pos, network.reachableNodes, request);
    }

    public RequestNetworkParticipant getNetwork() { return network; }
    public ModulePatternMatcher getMatcher() { return matcher; }

    /** Merges cargo into the stock slot matching {@code index}'s filter (used by EntityRequestDrone's unload step). */
    public ItemStack depositStock(ItemStack cargo) {
        for (int i = FILTER_START; i <= FILTER_END && !cargo.isEmpty(); i++) {
            ItemStack filter = inventory.getStackInSlot(i);
            if (filter.isEmpty() || !matcher.isValidForFilter(filter, i, cargo)) continue;

            int stockSlot = i + STOCK_START;
            ItemStack stock = inventory.getStackInSlot(stockSlot);
            if (stock.isEmpty()) {
                inventory.setStackInSlot(stockSlot, cargo.copy());
                cargo = ItemStack.EMPTY;
            } else if (com.hbm_m.platform.PlatformHooks.isSameItemSameTags(stock, cargo)) {
                int space = stock.getMaxStackSize() - stock.getCount();
                int toMove = Math.min(space, cargo.getCount());
                if (toMove > 0) {
                    stock.grow(toMove);
                    cargo.shrink(toMove);
                }
            }
        }
        setChanged();
        return cargo;
    }

    public void nextFilterMode(int index) {
        matcher.nextMode(index);
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
}
