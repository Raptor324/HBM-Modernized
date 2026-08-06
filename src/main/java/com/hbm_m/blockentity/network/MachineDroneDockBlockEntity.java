package com.hbm_m.blockentity.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.request.RequestNetwork;
import com.hbm_m.blockentity.network.request.RequestNetwork.OfferNode;
import com.hbm_m.blockentity.network.request.RequestNetwork.PathNode;
import com.hbm_m.blockentity.network.request.RequestNetwork.RequestNode;
import com.hbm_m.blockentity.network.request.RequestNetworkParticipant;
import com.hbm_m.entity.drone.EntityRequestDrone;
import com.hbm_m.entity.drone.EntityRequestDrone.ProgramStep;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Drone Dock - Port von {@code TileEntityDroneDock} (1.7.10 Original, Pipeline B). Die eigentliche
 * "Dispatch-Zentrale": haelt einen Vorrat an Request-Drohnen-Items (9 Slots), sucht einmal pro
 * Sekunde nach einem unerfuellten {@link RequestNode} mit passendem {@link OfferNode} im 5-Chunk-
 * Umkreis, pathfindet per Breitensuche (Tiefe 10) dock-&gt;offer-&gt;request-&gt;dock und spawnt bei
 * Erfolg eine {@link EntityRequestDrone} mit dem kompilierten Wegpunkt-/Aktions-Programm.
 */
public class MachineDroneDockBlockEntity extends BaseMachineBlockEntity {

    public static final int INVENTORY_SIZE = 9;
    public static final int PATHING_DEPTH = 10;

    private final RequestNetworkParticipant network = new RequestNetworkParticipant(pos -> new PathNode(pos, Set.of()));

    public MachineDroneDockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_DOCK_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineDroneDockBlockEntity be) {
        if (level.isClientSide) return;
        if (level.getGameTime() % 20 != 0) return;

        BlockPos nodePos = pos.above();
        be.network.tick(level, nodePos, level.hasNeighborSignal(pos));

        if (!be.hasSpareDrone()) return;

        Set<PathNode> localNodes = RequestNetwork.getAllLocalNodes(level, pos.getX(), pos.getZ(), 5);
        List<RequestNode> requests = new ArrayList<>();
        List<OfferNode> offers = new ArrayList<>();
        for (PathNode node : localNodes) {
            if (node instanceof RequestNode r) requests.add(r);
            if (node instanceof OfferNode o) offers.add(o);
        }

        PathNode own = new PathNode(nodePos, be.network.reachableNodes);

        for (int attempt = 0; attempt < 5; attempt++) {
            shuffle(requests, level.random);
            shuffle(offers, level.random);

            RequestNode firstRequest = null;
            for (RequestNode request : requests) {
                if (request.active && !request.request.isEmpty()) {
                    firstRequest = request;
                    break;
                }
            }
            if (firstRequest == null) continue;

            ItemStack wanted = firstRequest.request.get(level.random.nextInt(firstRequest.request.size()));

            for (OfferNode offer : offers) {
                if (!offer.active) continue;
                boolean matches = offer.offer.stream().anyMatch(stack -> ItemStack.isSameItemSameTags(stack, wanted));
                if (!matches) continue;

                if (be.tryEmbark(level, own, firstRequest, offer, wanted, localNodes)) return;
                break;
            }
        }
    }

    private boolean tryEmbark(Level level, PathNode dock, RequestNode request, OfferNode offer, ItemStack wanted, Set<PathNode> localNodes) {
        List<PathNode> dockToOffer = generatePath(dock, offer, localNodes);
        if (dockToOffer == null) return false;
        List<PathNode> offerToRequest = generatePath(offer, request, localNodes);
        if (offerToRequest == null) return false;
        List<PathNode> requestToDock = generatePath(request, dock, localNodes);
        if (requestToDock == null) return false;

        int droneSlot = findSpareDroneSlot();
        if (droneSlot < 0) return false;
        inventory.getStackInSlot(droneSlot).shrink(1);
        if (inventory.getStackInSlot(droneSlot).isEmpty()) inventory.setStackInSlot(droneSlot, ItemStack.EMPTY);

        List<Object> program = new ArrayList<>();
        for (PathNode node : dockToOffer) program.add(node.pos);
        program.add(offer.pos);
        program.add(wanted);
        for (PathNode node : offerToRequest) program.add(node.pos);
        program.add(request.pos);
        program.add(ProgramStep.UNLOAD);
        for (PathNode node : requestToDock) program.add(node.pos);
        program.add(dock.pos);
        program.add(ProgramStep.DOCK);

        EntityRequestDrone drone = EntityRequestDrone.create(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, program);
        level.addFreshEntity(drone);
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.BARREL_OPEN, net.minecraft.sounds.SoundSource.BLOCKS, 2.0F, 1.0F);
        setChanged();
        return true;
    }

    private static List<PathNode> generatePath(PathNode start, PathNode end, Set<PathNode> localNodes) {
        List<List<PathNode>> paths = new ArrayList<>();
        List<PathNode> init = new ArrayList<>();
        init.add(start);
        paths.add(init);

        for (int depth = 0; depth < PATHING_DEPTH; depth++) {
            int iterationBrake = 1000;
            List<List<PathNode>> newPaths = new ArrayList<>();

            depthLoop:
            for (List<PathNode> oldPath : paths) {
                for (PathNode connectedUnsafe : oldPath.get(oldPath.size() - 1).reachableNodes) {
                    PathNode connectedSafe = lookup(localNodes, connectedUnsafe);
                    if (connectedSafe != null) {
                        if (connectedSafe.equals(end)) {
                            List<PathNode> result = new ArrayList<>(oldPath);
                            result.remove(0);
                            return result;
                        }

                        List<PathNode> newPath = new ArrayList<>(oldPath);
                        newPath.add(connectedSafe);
                        newPaths.add(newPath);
                    }

                    iterationBrake--;
                    if (iterationBrake <= 0) continue depthLoop;
                }
            }

            paths = newPaths;
        }
        return null;
    }

    private static <T> void shuffle(List<T> list, net.minecraft.util.RandomSource random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }

    private static PathNode lookup(Set<PathNode> localNodes, PathNode key) {
        for (PathNode n : localNodes) if (n.equals(key)) return n;
        return null;
    }

    private boolean hasSpareDrone() {
        return findSpareDroneSlot() >= 0;
    }

    private int findSpareDroneSlot() {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == ModItems.DRONE_REQUEST.get()) return i;
        }
        return -1;
    }

    /** Called by {@link EntityRequestDrone} on its final DOCK step. Returns true if it successfully re-docked. */
    public boolean dockDrone(ItemStack heldItem) {
        ItemStack droneStack = new ItemStack(ModItems.DRONE_REQUEST.get());

        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                if (!heldItem.isEmpty() && i + 1 < INVENTORY_SIZE && inventory.getStackInSlot(i + 1).isEmpty()) {
                    inventory.setStackInSlot(i + 1, heldItem.copy());
                }
                inventory.setStackInSlot(i, droneStack);
                setChanged();
                return true;
            } else if (ItemStack.isSameItem(stack, droneStack) && stack.getCount() < 64) {
                if (!heldItem.isEmpty() && i + 1 < INVENTORY_SIZE && inventory.getStackInSlot(i + 1).isEmpty()) {
                    inventory.setStackInSlot(i + 1, heldItem.copy());
                }
                stack.grow(1);
                setChanged();
                return true;
            }
        }
        return false;
    }

    public RequestNetworkParticipant getNetwork() { return network; }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack.getItem() == ModItems.DRONE_REQUEST.get();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.drone_dock");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return com.hbm_m.inventory.menu.MachineDroneDockMenu.create(id, inventory, this);
    }
}
