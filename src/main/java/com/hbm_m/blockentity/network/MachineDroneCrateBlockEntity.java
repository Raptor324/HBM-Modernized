package com.hbm_m.blockentity.network;

import java.util.List;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.drone.EntityDeliveryDrone;
import com.hbm_m.entity.drone.EntityDroneBase;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineDroneCrateMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Crane Drone Crate - Port von {@code TileEntityDroneCrate} (1.7.10 Original, Pipeline A). 18
 * Item-Slots ODER ein {@link FluidTank} (per {@code itemType}-Flag), dazu ein {@code sendingMode}-
 * Flag (senden/empfangen). Erkennt untaetige {@link EntityDeliveryDrone}s direkt ueber sich und
 * laedt/entlaedt sie komplett in einem Rutsch - kein Filter/Whitelist, reiner Bulk-Transfer, exakt
 * wie im Original.
 * <p>
 * SCOPE-Vereinfachung: Das Original integriert den Fluid-Modus zusaetzlich in das MK2-Rohrnetzwerk
 * ({@code IFluidStandardTransceiver}) fuer automatisches Befuellen/Entleeren durch Nachbarrohre.
 * Hier: reiner manueller Tank (befuellbar wie andere einfache Tanks per Fluid-Identifier-Item),
 * ohne Netzwerk-Anbindung - der Kernmechanismus (Drohnen laden/entladen) ist vollstaendig erhalten,
 * nur die Pipe-Netzwerk-Automatik entfaellt angesichts des Aufwands fuer die volle MK2-Integration.
 */
public class MachineDroneCrateBlockEntity extends BaseMachineBlockEntity implements IDroneLinkable {

    public static final int INVENTORY_SIZE = 18;
    public static final int SLOT_FLUID_ID = 18;
    public static final int TOTAL_SLOTS = 19;

    private final FluidTank fluidTank = new FluidTank(64_000);
    private boolean sendingMode = true;
    private boolean itemType = true;
    private BlockPos nextTarget = null;

    public MachineDroneCrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_CRATE_BE.get(), pos, state, TOTAL_SLOTS, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineDroneCrateBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick(level, pos);
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        applyFluidIdentifier();
        BlockPos point = getDronePoint();
        AABB box = new AABB(point).inflate(0.4);
        List<EntityDeliveryDrone> drones = level.getEntitiesOfClass(EntityDeliveryDrone.class, box);

        for (EntityDeliveryDrone drone : drones) {
            if (!drone.isIdle()) continue;

            if (sendingMode) {
                if (drone.getAppearance() != EntityDroneBase.APPEARANCE_EMPTY) continue;
                loadDrone(drone);
            } else {
                int expected = itemType ? EntityDroneBase.APPEARANCE_CRATE : EntityDroneBase.APPEARANCE_BARREL;
                if (drone.getAppearance() != expected) continue;
                unloadDrone(drone);
            }

            if (nextTarget != null) {
                drone.setTarget(nextTarget.getX() + 0.5, nextTarget.getY() + 1.0, nextTarget.getZ() + 0.5);
            }
        }
    }

    private void loadDrone(EntityDeliveryDrone drone) {
        if (itemType) {
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                drone.setItem(i, inventory.getStackInSlot(i));
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
            drone.setAppearance(EntityDroneBase.APPEARANCE_CRATE);
        } else {
            if (fluidTank.getFill() <= 0) return;
            drone.getFluidTank().setTankType(fluidTank.getTankType());
            drone.getFluidTank().fill(fluidTank.getFill());
            fluidTank.fill(0);
            drone.setAppearance(EntityDroneBase.APPEARANCE_BARREL);
        }
        setChanged();
    }

    private void unloadDrone(EntityDeliveryDrone drone) {
        if (itemType) {
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                ItemStack cargo = drone.getItem(i);
                if (cargo.isEmpty()) continue;
                ItemStack remainder = insertIntoSlots(cargo);
                drone.setItem(i, remainder);
            }
            if (drone.isCargoEmpty()) drone.setAppearance(EntityDroneBase.APPEARANCE_EMPTY);
        } else {
            FluidTank droneTank = drone.getFluidTank();
            if (droneTank.getFill() <= 0) return;
            if (fluidTank.getFill() <= 0 || fluidTank.getTankType() == droneTank.getTankType()) {
                fluidTank.setTankType(droneTank.getTankType());
                int space = fluidTank.getMaxFill() - fluidTank.getFill();
                int toMove = Math.min(space, droneTank.getFill());
                fluidTank.fill(fluidTank.getFill() + toMove);
                droneTank.fill(droneTank.getFill() - toMove);
            }
            if (droneTank.getFill() <= 0) drone.setAppearance(EntityDroneBase.APPEARANCE_EMPTY);
        }
        setChanged();
    }

    private ItemStack insertIntoSlots(ItemStack stack) {
        for (int i = 0; i < INVENTORY_SIZE && !stack.isEmpty(); i++) {
            ItemStack current = inventory.getStackInSlot(i);
            if (current.isEmpty()) {
                inventory.setStackInSlot(i, stack);
                return ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameTags(current, stack)) {
                int space = current.getMaxStackSize() - current.getCount();
                if (space > 0) {
                    int toMove = Math.min(space, stack.getCount());
                    current.grow(toMove);
                    stack.shrink(toMove);
                }
            }
        }
        return stack;
    }

    private void applyFluidIdentifier() {
        ItemStack idStack = inventory.getStackInSlot(SLOT_FLUID_ID);
        if (idStack.isEmpty()) return;
        if (idStack.getItem() instanceof com.hbm_m.item.liquids.FluidIdentifierItem) {
            var resolved = com.hbm_m.item.liquids.FluidIdentifierItem.resolvePrimaryForTank(idStack);
            if (resolved != null && resolved != fluidTank.getTankType()) {
                fluidTank.assignTypeAndZeroFluid(resolved);
                setChanged();
            }
        }
    }

    // ── IDroneLinkable ──────────────────────────────────────────────────────

    @Override
    public BlockPos getDronePoint() {
        return worldPosition.above();
    }

    @Override
    public void setNextTarget(BlockPos target) {
        this.nextTarget = target;
        setChanged();
    }

    public BlockPos getNextTarget() { return nextTarget; }

    // ── Toggles ──────────────────────────────────────────────────────────────

    public boolean isSendingMode() { return sendingMode; }
    public void toggleSendingMode() { sendingMode = !sendingMode; setChanged(); }
    public boolean isItemType() { return itemType; }
    public void toggleItemType() { itemType = !itemType; setChanged(); }
    public FluidTank getFluidTank() { return fluidTank; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("sendingMode", sendingMode);
        tag.putBoolean("itemType", itemType);
        if (nextTarget != null) tag.putLong("nextTarget", nextTarget.asLong());
        tag.put("fluidTank", fluidTank.writeNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        sendingMode = tag.getBoolean("sendingMode");
        itemType = tag.getBoolean("itemType");
        nextTarget = tag.contains("nextTarget") ? BlockPos.of(tag.getLong("nextTarget")) : null;
        if (tag.contains("fluidTank")) fluidTank.readNBT(tag.getCompound("fluidTank"));
    }

    // ── Slot validation / Menu ─────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.drone_crate");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineDroneCrateMenu.create(id, inventory, this);
    }
}
