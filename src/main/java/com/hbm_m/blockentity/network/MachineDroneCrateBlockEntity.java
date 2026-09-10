package com.hbm_m.blockentity.network;

import java.util.List;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.drone.EntityDeliveryDrone;
import com.hbm_m.entity.drone.EntityDroneBase;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineDroneCrateMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * <p>Im Fluessigkeitsbetrieb haengt die Kiste wie im Original am <b>Rohrnetz</b>: im Sendebetrieb
 * zieht sie sich voll, im Empfangsbetrieb schiebt sie ab, was die Drohnen abgeladen haben. Erst
 * damit laesst sich eine Drohnenstrecke ohne Handarbeit betreiben - man haengt sie an beiden Enden
 * an Rohre und laesst laufen.</p>
 */
public class MachineDroneCrateBlockEntity extends BaseMachineBlockEntity
        implements IDroneLinkable, com.hbm_m.api.fluids.IFluidStandardTransceiverMK2 {

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

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    /** Nur im Fluessigkeitsbetrieb haengt die Kiste am Rohrnetz - als Kiste hat sie nichts zu melden. */
    @Override
    public FluidTank[] getAllTanks() {
        return itemType ? new FluidTank[0] : new FluidTank[] { fluidTank };
    }

    /** Im Sendebetrieb zieht sie sich voll: dann nimmt sie an. */
    @Override
    public FluidTank[] getReceivingTanks() {
        return !itemType && sendingMode ? new FluidTank[] { fluidTank } : new FluidTank[0];
    }

    /** Im Empfangsbetrieb gibt sie ab, was die Drohnen gebracht haben. */
    @Override
    public FluidTank[] getSendingTanks() {
        return !itemType && !sendingMode ? new FluidTank[] { fluidTank } : new FluidTank[0];
    }

    private void serverTick(Level level, BlockPos pos) {
        applyFluidIdentifier();

        // 1:1: die Rohranbindung laeuft nur im Fluessigkeitsbetrieb.
        if (!itemType && level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                if (sendingMode) {
                    trySubscribe(fluidTank.getTankType(), level, pos.relative(dir), dir);
                } else if (fluidTank.getFill() > 0) {
                    tryProvide(fluidTank, level, pos.relative(dir), dir);
                }
            }
        }

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
            } else if (com.hbm_m.platform.PlatformHooks.isSameItemSameTags(current, stack)) {
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
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("sendingMode", sendingMode);
        tag.putBoolean("itemType", itemType);
        if (nextTarget != null) tag.putLong("nextTarget", nextTarget.asLong());
        tag.put("fluidTank", fluidTank.writeNBT(new CompoundTag()));
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
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
