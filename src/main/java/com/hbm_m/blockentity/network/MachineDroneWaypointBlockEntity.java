package com.hbm_m.blockentity.network;

import java.util.List;

import com.hbm_m.block.machines.MachineDroneWaypointBlock;
import com.hbm_m.entity.drone.EntityDeliveryDrone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Drone Waypoint - Port von {@code TileEntityDroneWaypoint} (1.7.10 Original, Pipeline A). Reiner
 * Durchgangspunkt: erkennt untaetige {@link EntityDeliveryDrone}s am projizierten Punkt (Mount-
 * Richtung * {@link #height}) und setzt sie einfach auf {@link #nextTarget} um - keine Fracht-
 * Interaktion, im Gegensatz zu {@link MachineDroneCrateBlockEntity}.
 */
public class MachineDroneWaypointBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements IDroneLinkable {

    public static final int MIN_HEIGHT = 1;
    public static final int MAX_HEIGHT = 15;
    public static final int DEFAULT_HEIGHT = 5;

    private int height = DEFAULT_HEIGHT;
    private BlockPos nextTarget = null;

    public MachineDroneWaypointBlockEntity(BlockPos pos, BlockState state) {
        super(com.hbm_m.blockentity.ModBlockEntities.DRONE_WAYPOINT_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineDroneWaypointBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick(level, pos);
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        if (nextTarget == null) return;

        BlockPos point = getDronePoint();
        AABB box = new AABB(point).inflate(0.4);
        List<EntityDeliveryDrone> drones = level.getEntitiesOfClass(EntityDeliveryDrone.class, box);

        for (EntityDeliveryDrone drone : drones) {
            if (!drone.isIdle()) continue;
            drone.setTarget(nextTarget.getX() + 0.5, nextTarget.getY() + 1.0, nextTarget.getZ() + 0.5);
        }
    }

    public int getHeight() { return height; }

    public void adjustHeight(boolean increase) {
        height = increase ? Math.min(MAX_HEIGHT, height + 1) : Math.max(MIN_HEIGHT, height - 1);
        setChanged();
    }

    // ── IDroneLinkable ──────────────────────────────────────────────────────

    @Override
    public BlockPos getDronePoint() {
        Direction mount = getBlockState().getValue(MachineDroneWaypointBlock.FACING);
        return worldPosition.relative(mount, height);
    }

    @Override
    public void setNextTarget(BlockPos target) {
        this.nextTarget = target;
        setChanged();
    }

    public BlockPos getNextTarget() { return nextTarget; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putInt("height", height);
        if (nextTarget != null) tag.putLong("nextTarget", nextTarget.asLong());
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        height = tag.contains("height") ? tag.getInt("height") : DEFAULT_HEIGHT;
        nextTarget = tag.contains("nextTarget") ? BlockPos.of(tag.getLong("nextTarget")) : null;
    }
}
