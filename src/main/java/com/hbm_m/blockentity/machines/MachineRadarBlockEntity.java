package com.hbm_m.blockentity.machines;

import java.util.ArrayList;
import java.util.List;

import api.hbm.entity.IRadarDetectable;
import com.hbm_m.api.energy.ItemEnergyAccess;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.inventory.menu.MachineRadarMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.entity.missile.MissileBaseEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?}

public class MachineRadarBlockEntity extends BaseMachineBlockEntity {

    /** Слоты как в {@code TileEntityMachineRadarNT}: 0 и 9 — батареи. */
    private static final int SLOT_COUNT = 10;
    private static final int SLOT_BATTERY_PRIMARY = 0;
    private static final int SLOT_BATTERY_SECONDARY = 9;

    public static final int RADAR_RANGE = 256;
    public static final int RADAR_LARGE_RANGE = 3_000;
    public static final int RADAR_BUFFER = 0;
    public static final int RADAR_ALTITUDE = 64;
    private static final long ENERGY_DRAIN_PER_TICK = 500L;
    private static final int MAX_CONTACTS = 64;
    private static final int DEFAULT_MAX_PROGRESS = 200;
    private static final int SONAR_PING_INTERVAL = 80;

    private int progress = 0;
    private int maxProgress = DEFAULT_MAX_PROGRESS;
    private int pingTimer = 0;
    private boolean active = false;

    /** Клиентская анимация тарелки (порт {@code TileEntityMachineRadarNT}). */
    public float prevRotation;
    public float rotation;

    public boolean scanMissiles = true;
    public boolean scanPlayers = false;
    public boolean smartMode = true;
    public boolean redMode = true;
    public boolean jammed = false;

    private int lastRedPower = 0;
    private final List<Entity> trackedEntities = new ArrayList<>();
    public final List<int[]> nearbyMissiles = new ArrayList<>();

    public MachineRadarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR_BE.get(), pos, state, SLOT_COUNT, 250_000L, 2_500L, 0L);
    }

    public boolean isLargeRadar() {
        return getBlockState().is(ModBlocks.LARGE_RADAR.get());
    }

    public int getRange() {
        return isLargeRadar() ? RADAR_LARGE_RANGE : RADAR_RANGE;
    }

    @Override
    public AABB getRenderBoundingBox() {
        if (isLargeRadar()) {
            // Порт TileEntityMachineRadarLarge.getRenderBoundingBox()
            return new AABB(
                    worldPosition.getX() - 5,
                    worldPosition.getY(),
                    worldPosition.getZ() - 5,
                    worldPosition.getX() + 6,
                    worldPosition.getY() + 10,
                    worldPosition.getZ() + 6
            );
        }
        return super.getRenderBoundingBox();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineRadarBlockEntity blockEntity) {
        if (level.isClientSide) {
            blockEntity.clientTick();
            return;
        }
        blockEntity.serverTick();
    }

    private void clientTick() {
        prevRotation = rotation;
        if (active || getEnergyStored() > 0) {
            rotation += 5.0F;
            if (rotation >= 360.0F) {
                rotation -= 360.0F;
                prevRotation -= 360.0F;
            }
        }
    }

    private void serverTick() {
        ensureNetworkInitialized();
        chargeFromBatterySlot(SLOT_BATTERY_SECONDARY);
        chargeFromBatterySlot(SLOT_BATTERY_PRIMARY);
        chargeFromAdjacentBlocks();

        boolean wasActive = active;
        active = getEnergyStored() > 0;

        if (worldPosition.getY() < RADAR_ALTITUDE) {
            clearScanData();
            if (lastRedPower != 0) {
                lastRedPower = 0;
                notifyRedstoneNeighbors();
            }
        } else if (active) {
            performRadarScan();
            setEnergyStored(Math.max(0L, getEnergyStored() - ENERGY_DRAIN_PER_TICK));
            active = getEnergyStored() > 0;
        } else {
            clearScanData();
        }

        progress = (progress + 1) % Math.max(1, maxProgress);

        pingTimer++;
        if (getEnergyStored() > 0 && pingTimer >= SONAR_PING_INTERVAL) {
            level.playSound(null, worldPosition, ModSounds.SONAR_PING.get(), SoundSource.BLOCKS, 5.0F, 1.0F);
            pingTimer = 0;
        }

        int redPower = getRedPower();
        if (redPower != lastRedPower) {
            lastRedPower = redPower;
            notifyRedstoneNeighbors();
        }

        if (wasActive != active || active || progress == 0 || level.getGameTime() % 20 == 0) {
            setChanged();
            sendUpdateToClient();
        }
    }

    /**
     * Точки подключения кабеля (порт {@code TileEntityMachineRadarNT.getConPos()} /
     * {@code TileEntityMachineRadarLarge.getConPos()}).
     */
    private BlockPos[] getConnectionPositions() {
        int offset = isLargeRadar() ? 2 : 1;
        return new BlockPos[] {
                worldPosition.offset(offset, 0, 0),
                worldPosition.offset(-offset, 0, 0),
                worldPosition.offset(0, 0, offset),
                worldPosition.offset(0, 0, -offset),
        };
    }

    private void chargeFromAdjacentBlocks() {
        if (level == null || !canReceive()) {
            return;
        }

        for (Direction dir : Direction.values()) {
            tryPullEnergyFromPos(worldPosition.relative(dir), dir.getOpposite());
            if (!canReceive()) {
                return;
            }
        }

        if (level.getGameTime() % 20 != 0) {
            return;
        }

        for (BlockPos connPos : getConnectionPositions()) {
            for (Direction dir : Direction.values()) {
                tryPullEnergyFromPos(connPos, dir);
                if (!canReceive()) {
                    return;
                }
            }
        }
    }

    private void tryPullEnergyFromPos(BlockPos sourcePos, Direction side) {
        if (level == null || !canReceive()) {
            return;
        }

        BlockEntity source = level.getBlockEntity(sourcePos);
        if (source == null) {
            return;
        }

        long needed = Math.min(getMaxEnergyStored() - getEnergyStored(), getReceiveSpeed());
        if (needed <= 0) {
            return;
        }
        final long pullLimit = needed;

        //? if forge {
        source.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER, side).ifPresent(provider -> {
            if (!provider.canExtract()) {
                return;
            }
            long toPull = Math.min(pullLimit, provider.getProvideSpeed());
            long extracted = provider.extractEnergy(toPull, false);
            if (extracted > 0) {
                receiveEnergy(extracted, false);
            }
        });

        if (!canReceive()) {
            return;
        }
        long feNeeded = Math.min(getMaxEnergyStored() - getEnergyStored(), getReceiveSpeed());
        if (feNeeded <= 0) {
            return;
        }
        final int fePullLimit = (int) Math.min(Integer.MAX_VALUE, feNeeded);

        source.getCapability(ForgeCapabilities.ENERGY, side).ifPresent(fe -> {
            if (!fe.canExtract()) {
                return;
            }
            if (fePullLimit <= 0) {
                return;
            }
            int extracted = fe.extractEnergy(fePullLimit, false);
            if (extracted > 0) {
                receiveEnergy(extracted, false);
            }
        });
        //?}
    }

    private void performRadarScan() {
        if (level == null || level.isClientSide) {
            return;
        }

        nearbyMissiles.clear();
        trackedEntities.clear();
        jammed = false;

        AABB area = new AABB(
                worldPosition.getX() + 0.5D - getRange(),
                level.getMinBuildHeight(),
                worldPosition.getZ() + 0.5D - getRange(),
                worldPosition.getX() + 0.5D + getRange(),
                level.getMaxBuildHeight(),
                worldPosition.getZ() + 0.5D + getRange()
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

            if (scanPlayers && isTrackableMob(entity)) {
                nearbyMissiles.add(createContact(entity, IRadarDetectable.RadarTargetType.PLAYER.ordinal(), true));
                trackedEntities.add(entity);
                continue;
            }

            if (scanMissiles && isMissileLike(entity)) {
                if (entity instanceof com.hbm_m.entity.missile.MissileBaseEntity missile
                        && !missile.canBeDetectedByRadar()) {
                    continue;
                }
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
        if (entity instanceof MissileBaseEntity) {
            return true;
        }
        if (entity instanceof IRadarDetectable detectable) {
            return detectable.getTargetType() != IRadarDetectable.RadarTargetType.PLAYER;
        }
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

    private boolean isTrackableMob(Entity entity) {
        return entity instanceof LivingEntity && !(entity instanceof Player);
    }

    private int[] createContact(Entity entity, int type) {
        return createContact(entity, type, false);
    }

    private int[] createContact(Entity entity, int type, boolean mobContact) {
        return new int[] {
                (int) entity.getX(),
                (int) entity.getY(),
                (int) entity.getZ(),
                getVelocity(entity),
                type,
                mobContact ? 1 : 0
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
            double maxRange = getRange() * Math.sqrt(2.0D);
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
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = getBlockState();
        level.updateNeighborsAt(worldPosition, state.getBlock());
        level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());

        if (!isLargeRadar() || !(state.getBlock() instanceof IMultiblockController controller)) {
            return;
        }

        MultiblockStructureHelper helper = controller.getStructureHelper();
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);

        for (BlockPos localPos : helper.getStructureMap().keySet()) {
            if (helper.resolvePartRole(localPos, controller) != PartRole.ENERGY_CONNECTOR) {
                continue;
            }
            BlockPos worldPos = helper.getRotatedPos(worldPosition, localPos, facing);
            level.updateNeighborsAt(worldPos, ModBlocks.UNIVERSAL_MACHINE_PART.get());
            level.updateNeighbourForOutputSignal(worldPos, ModBlocks.UNIVERSAL_MACHINE_PART.get());
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
            contactTag.putInt("m", entry.length >= 6 ? entry[5] : 0);
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
                    contactTag.getInt("t"),
                    contactTag.getInt("m")
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
        if (slot != SLOT_BATTERY_PRIMARY && slot != SLOT_BATTERY_SECONDARY) {
            return false;
        }
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof ItemCreativeBattery) {
            return true;
        }
        if (ItemEnergyAccess.getHbmProvider(stack).isPresent()) {
            return true;
        }
        //? if forge {
        return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
        //?}
        //? if fabric {
        /*return teamreborn.energy.api.EnergyStorage.ITEM.find(stack, null) != null;
        *///?}
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineRadarMenu.create(id, inventory, this);
    }
}
