package com.hbm_m.block.entity.machines;

import java.util.ArrayList;
import java.util.List;

import api.hbm.entity.IRadarDetectable;
import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineRadarMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MachineRadarBlockEntity extends BaseMachineBlockEntity {

    public static final int RADAR_RANGE = 256;
    public static final int RADAR_BUFFER = 0;
    public static final int RADAR_ALTITUDE = 64;
    private static final long ENERGY_DRAIN_PER_TICK = 500L;
    private static final int MAX_CONTACTS = 64;
    private static final int DEFAULT_MAX_PROGRESS = 200;

    private int progress = 0;
    private int maxProgress = DEFAULT_MAX_PROGRESS;
    private boolean active = false;

    public boolean scanMissiles = true;
    public boolean scanPlayers = false;
    public boolean smartMode = true;
    public boolean redMode = true;
    public boolean jammed = false;

    private int lastRedPower = 0;
    private final List<Entity> trackedEntities = new ArrayList<>();
    public final List<int[]> nearbyMissiles = new ArrayList<>();

    public MachineRadarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR_BE.get(), pos, state, 0, 250_000L, 2_500L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineRadarBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.serverTick();
        }
    }

    private void serverTick() {
        ensureNetworkInitialized();

        if (worldPosition.getY() < RADAR_ALTITUDE) {
            clearScanData();
            if (lastRedPower != 0) {
                lastRedPower = 0;
                notifyRedstoneNeighbors();
            }
            return;
        }

        boolean wasActive = active;
        active = getEnergyStored() > 0;

        if (active) {
            performRadarScan();

            long afterDrain = getEnergyStored() - ENERGY_DRAIN_PER_TICK;
            setEnergyStored(Math.max(0L, afterDrain));
        } else {
            clearScanData();
        }

        progress = (progress + 1) % Math.max(1, maxProgress);

        int redPower = getRedPower();
        if (redPower != lastRedPower) {
            lastRedPower = redPower;
            notifyRedstoneNeighbors();
        }

        if (wasActive != active || active || progress == 0) {
            setChanged();
            sendUpdateToClient();
        }
    }

    private void performRadarScan() {
        if (level == null || level.isClientSide) {
            return;
        }

        nearbyMissiles.clear();
        trackedEntities.clear();
        jammed = false;

        AABB area = new AABB(
                worldPosition.getX() + 0.5D - RADAR_RANGE,
                level.getMinBuildHeight(),
                worldPosition.getZ() + 0.5D - RADAR_RANGE,
                worldPosition.getX() + 0.5D + RADAR_RANGE,
                level.getMaxBuildHeight(),
                worldPosition.getZ() + 0.5D + RADAR_RANGE
        );

        List<Entity> entities = level.getEntities((Entity) null, area, Entity::isAlive);

        for (Entity entity : entities) {
            if (nearbyMissiles.size() >= MAX_CONTACTS) {
                break;
            }
            if (entity.getY() < worldPosition.getY() + RADAR_BUFFER) {
                continue;
            }

            if (isJammingEntity(entity)) {
                jammed = true;
                nearbyMissiles.clear();
                trackedEntities.clear();
                return;
            }

            if (entity instanceof Player && scanPlayers) {
                nearbyMissiles.add(createContact(entity, getTargetTypeIndex(entity)));
                trackedEntities.add(entity);
                continue;
            }

            if (scanMissiles && isMissileLike(entity)) {
                int type = getTargetTypeIndex(entity);
                nearbyMissiles.add(createContact(entity, type));

                if (smartMode) {
                    Vec3 motion = entity.getDeltaMovement();
                    if (motion.y <= 0.0D && isEntityApproaching(entity)) {
                        trackedEntities.add(entity);
                    }
                } else {
                    trackedEntities.add(entity);
                }
            }
        }
    }

    private boolean isMissileLike(Entity entity) {
        String name = entity.getType().toString().toLowerCase();
        return name.contains("missile")
                || name.contains("rocket")
                || name.contains("airstrike")
                || name.contains("bomb")
                || name.contains("nuke");
    }

    private boolean isJammingEntity(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }

        String typeName = living.getType().toString().toLowerCase();
        String displayName = living.getName().getString().toLowerCase();
        return typeName.contains("digamma")
                || typeName.contains("jam")
                || displayName.contains("digamma")
                || displayName.contains("jam");
    }

    private int[] createContact(Entity entity, int type) {
        return new int[] {
                (int) entity.getX(),
                (int) entity.getY(),
                (int) entity.getZ(),
                getVelocity(entity),
                type
        };
    }

    private int getVelocity(Entity entity) {
        Vec3 motion = entity.getDeltaMovement();
        return (int) (Math.sqrt(motion.x * motion.x + motion.y * motion.y + motion.z * motion.z) * 20.0D);
    }

    private boolean isEntityApproaching(Entity entity) {
        Vec3 motion = entity.getDeltaMovement();
        boolean xAxisApproaching = (worldPosition.getX() < entity.getX() && motion.x < 0.0D)
                || (worldPosition.getX() > entity.getX() && motion.x > 0.0D);
        boolean zAxisApproaching = (worldPosition.getZ() < entity.getZ() && motion.z < 0.0D)
                || (worldPosition.getZ() > entity.getZ() && motion.z > 0.0D);
        return xAxisApproaching && zAxisApproaching;
    }

    private int getTargetTypeIndex(Entity entity) {
        if (entity instanceof IRadarDetectable detectable) {
            return detectable.getTargetType().ordinal();
        }
        if (entity instanceof Player) {
            return IRadarDetectable.RadarTargetType.PLAYER.ordinal();
        }
        return 0;
    }

    public String getTargetTypeName(int index) {
        IRadarDetectable.RadarTargetType[] values = IRadarDetectable.RadarTargetType.values();
        if (index < 0 || index >= values.length) {
            return IRadarDetectable.RadarTargetType.MISSILE_TIER0.name;
        }
        return values[index].name;
    }

    public long getPowerScaled(int scale) {
        if (getMaxEnergyStored() <= 0) {
            return 0;
        }
        return getEnergyStored() * scale / getMaxEnergyStored();
    }

    public void handleButtonPress(int buttonId) {
        switch (buttonId) {
            case 0 -> scanMissiles = !scanMissiles;
            case 1 -> scanPlayers = !scanPlayers;
            case 2 -> smartMode = !smartMode;
            case 3 -> redMode = !redMode;
            default -> {
                return;
            }
        }

        setChanged();
        sendUpdateToClient();
    }

    public int getRedPower() {
        if (trackedEntities.isEmpty()) {
            return 0;
        }

        if (redMode) {
            double maxRange = RADAR_RANGE * Math.sqrt(2.0D);
            int powerOut = 0;

            for (Entity entity : trackedEntities) {
                double dx = entity.getX() - worldPosition.getX();
                double dz = entity.getZ() - worldPosition.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);

                int p = 15 - (int) Math.floor(dist / maxRange * 15.0D);
                if (p > powerOut) {
                    powerOut = p;
                }
            }

            return Math.max(0, Math.min(15, powerOut));
        }

        int powerOut = 0;
        for (int[] contact : nearbyMissiles) {
            if (contact == null || contact.length < 5) {
                continue;
            }
            powerOut = Math.max(powerOut, contact[4] + 1);
        }
        return Math.max(0, Math.min(15, powerOut));
    }

    private void clearScanData() {
        jammed = false;
        nearbyMissiles.clear();
        trackedEntities.clear();
    }

    private void notifyRedstoneNeighbors() {
        if (level != null && !level.isClientSide) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    public int getProgressScaled(int scale) {
        if (maxProgress <= 0) {
            return 0;
        }
        return progress * scale / maxProgress;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("progress", progress);
        tag.putInt("max_progress", maxProgress);
        tag.putBoolean("active", active);
        tag.putBoolean("scan_missiles", scanMissiles);
        tag.putBoolean("scan_players", scanPlayers);
        tag.putBoolean("smart_mode", smartMode);
        tag.putBoolean("red_mode", redMode);
        tag.putBoolean("jammed", jammed);

        ListTag contacts = new ListTag();
        for (int[] entry : nearbyMissiles) {
            if (entry == null || entry.length < 5) {
                continue;
            }
            CompoundTag contactTag = new CompoundTag();
            contactTag.putInt("x", entry[0]);
            contactTag.putInt("y", entry[1]);
            contactTag.putInt("z", entry[2]);
            contactTag.putInt("v", entry[3]);
            contactTag.putInt("t", entry[4]);
            contacts.add(contactTag);
        }
        tag.put("contacts", contacts);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("max_progress");
        if (maxProgress <= 0) {
            maxProgress = DEFAULT_MAX_PROGRESS;
        }
        active = tag.getBoolean("active");
        scanMissiles = !tag.contains("scan_missiles") || tag.getBoolean("scan_missiles");
        scanPlayers = tag.getBoolean("scan_players");
        smartMode = tag.getBoolean("smart_mode");
        redMode = tag.getBoolean("red_mode");
        jammed = tag.getBoolean("jammed");

        nearbyMissiles.clear();
        ListTag contacts = tag.getList("contacts", Tag.TAG_COMPOUND);
        for (int i = 0; i < contacts.size(); i++) {
            CompoundTag contactTag = contacts.getCompound(i);
            nearbyMissiles.add(new int[] {
                    contactTag.getInt("x"),
                    contactTag.getInt("y"),
                    contactTag.getInt("z"),
                    contactTag.getInt("v"),
                    contactTag.getInt("t")
            });
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.radar");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineRadarMenu.create(id, inventory, this);
    }
}
